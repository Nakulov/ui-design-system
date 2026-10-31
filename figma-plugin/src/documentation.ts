// Страницы документации: описание системы, страницы foundation, core и компонентов, Release Checklist (figma-documentation.md, разделы 3, 5).
import type { Bundle, ChangelogEntry, Contract, Element, Id, ManifestExample, ManifestSection, Source } from "./bundle";
import { type Auto, AUTO_KEY, AutoChecks, type Check, type Checks, CHECKS_KEY, oldest, readJson, scope, stale } from "./checklist";
import { isMain, mark, ref } from "./ids";
import { readImports } from "./imports";
import { Renderer, Tokens } from "./render";
import { Report } from "./report";
import { fill } from "./samples";

const ELEMENTS = "figma/documentation/elements/";
/** Превью величин foundation лежит в сущности: `foundation/spacing/preview` (bundle-format.md, раздел 3.7). */
const PREVIEW = "preview";
const OVERVIEW = "docs/overview";
const USAGE = "Рекомендации по использованию";
const GAP = 120;
const PLATFORMS: [string, string][] = [["android", "Android"], ["ios", "iOS"], ["figma", "Figma"]];
const KINDS: Record<ChangelogEntry["kind"], string> = { added: "Добавлено", changed: "Изменено", removed: "Удалено", renamed: "Переименовано" };

type Span = { text: { value: string; typography?: string; color?: string; decoration?: string } };
type Model = Record<string, unknown>;
type Release = Record<string, { status: string; version?: string }>;

/** Итоговые статусы уровня: свои поверх родительских; версия — своя или ближайшего предка (schema-format.md, раздел 8.5). */
function inherit(parent: Release, own: unknown): Release {
  const result = { ...parent };
  for (const [key, value] of Object.entries((own ?? {}) as Record<string, unknown>)) {
    const { status, version } = (typeof value === "string" ? { status: value } : value) as { status: string; version?: string };
    result[key] = { status, version: version ?? parent[key]?.version };
  }
  return result;
}

/** Версия `a` позже `b`; уровень без версии — раньше любой версии. */
function later(a: string | undefined, b: string | undefined): boolean {
  if (!a) return false;
  if (!b) return true;
  const [x, y] = [a, b].map((v) => v.split(".").map(Number));
  for (let i = 0; i < Math.max(x.length, y.length); i++) {
    if ((x[i] ?? 0) !== (y[i] ?? 0)) return (x[i] ?? 0) > (y[i] ?? 0);
  }
  return false;
}

/** Фрагменты строки по подмножеству Markdown: код, жирный, ссылка (schema-format.md, раздел 11.2). */
function inline(text: string): Span[] {
  const spans: Span[] = [];
  const pattern = /`([^`]+)`|\*\*([^*]+)\*\*|\[([^\]]+)\]\([^)]+\)/g;
  let last = 0;
  for (const match of text.matchAll(pattern)) {
    if (match.index! > last) spans.push({ text: { value: text.slice(last, match.index) } });
    if (match[1] !== undefined) spans.push({ text: { value: match[1], typography: "code" } });
    else if (match[2] !== undefined) spans.push({ text: { value: match[2], typography: "bodyMediumEmphasized" } });
    else spans.push({ text: { value: match[3], color: "primary", decoration: "underline" } });
    last = match.index! + match[0].length;
  }
  if (last < text.length) spans.push({ text: { value: text.slice(last) } });
  return spans;
}

/** Блоки раздела: абзацы и пункты списков; продолжение пункта — строки с отступом. */
function blocks(markdown: string): { marker?: string; text: string }[] {
  const result: { marker?: string; text: string }[] = [];
  let open = false;
  for (const line of markdown.split("\n")) {
    const item = line.match(/^(-|\d+\.)\s+(.*)$/);
    if (!line.trim()) open = false;
    else if (item) {
      result.push({ marker: item[1] === "-" ? "•" : item[1], text: item[2] });
      open = true;
    } else if (open) result[result.length - 1].text += ` ${line.trim()}`;
    else {
      result.push({ text: line.trim() });
      open = true;
    }
  }
  return result;
}

export class DocumentationBuilder {
  private readonly report = new Report();
  private tokens!: Tokens;
  private checks!: AutoChecks;

  constructor(private readonly bundle: Bundle) {}

  async build(): Promise<Report> {
    this.tokens = await Tokens.load(this.bundle);
    await figma.loadAllPagesAsync();
    this.checks = new AutoChecks(this.bundle, readImports());
    const foundation = Object.entries(this.bundle.contracts)
      .filter(([id]) => id.startsWith("foundation/") && isMain(id) && this.bundle.values[id])
      .sort(([, a], [, b]) => a.name.localeCompare(b.name));
    // Страницы core и компонентов — у сущностей с манифестом (documentation-manifest.md, раздел 2).
    const manifested = (layer: string) => Object.entries(this.bundle.contracts)
      .filter(([id]) => id.startsWith(layer) && this.bundle.manifests[id])
      .sort(([, a], [, b]) => a.name.localeCompare(b.name));
    const core = manifested("core/");
    const components = manifested("components/");
    const overview = await this.page((p) => ref(p, "role") === "overview", "Overview", (p) => mark(p, "overview", "role"));
    await this.draw(overview, "overview", this.overview(foundation.map(([id]) => id), core.map(([id]) => id), components.map(([id]) => id)));
    const pages = async (entities: [Id, Contract][], build: (id: Id) => Model) => {
      const result: PageNode[] = [];
      for (const [id, contract] of entities) {
        const page = await this.page((p) => ref(p) === id, contract.name, (p) => mark(p, id));
        const warnings = this.report.data().warnings.length;
        await this.draw(page, id, build(id));
        await this.presets(page, id);
        if (scope(id)) await this.checklist(page, id, this.report.data().warnings.length - warnings);
        result.push(page);
      }
      return result;
    };
    const foundationPages = await pages(foundation, (id) => this.entity(id));
    const corePages = await pages(core, (id) => this.manifest(id));
    const componentPages = await pages(components, (id) => this.manifest(id));
    this.order(overview, foundationPages, corePages, componentPages);
    await figma.setCurrentPageAsync(overview);
    return this.report;
  }

  private async page(find: (page: PageNode) => boolean, name: string, init: (page: PageNode) => void): Promise<PageNode> {
    let page = figma.root.children.find(find);
    if (!page) {
      page = figma.createPage();
      page.name = name;
      init(page);
      this.report.created("страницы");
    } else this.report.updated("страницы");
    return page;
  }

  /** Модель элемента оформления или превью с данными дырок (figma-documentation.md, раздел 5). */
  private element(id: Id, values: Record<string, unknown>): Model {
    const element = this.bundle.documentation[id] as Element | undefined;
    if (!element) throw new Error(`нет элемента ${id}`);
    return fill(element.model, values) as Model;
  }

  private markdown(text: string | undefined): Model[] {
    return blocks(text ?? "").map((block) => block.marker
      ? this.element(`${ELEMENTS}listItem`, { marker: block.marker, text: inline(block.text) })
      : this.element(`${ELEMENTS}paragraph`, { text: inline(block.text) }));
  }

  private section(title: string, content: Model[]): Model {
    return this.element(`${ELEMENTS}section`, { title, content });
  }

  /** Описание системы: разделы `docs/overview.md` и оглавления foundation, core и компонентов. */
  private overview(foundation: Id[], core: Id[], components: Id[]): Model {
    const sections = Object.entries(this.bundle.texts[OVERVIEW] ?? {}).map(([title, text]) => this.section(title, this.markdown(text)));
    const contents = (ids: Id[]) => ids.map((id) => this.element(`${ELEMENTS}listItem`, {
      marker: "•", text: [{ text: { value: this.bundle.contracts[id].name, typography: "code" } }, { text: { value: ` — ${this.bundle.contracts[id].description}` } }],
    }));
    return this.element(`${ELEMENTS}page`, {
      sections: [
        this.element(`${ELEMENTS}heading`, { text: "UI Design System" }), ...sections,
        this.section("Foundation", contents(foundation)), ...(core.length ? [this.section("Core", contents(core))] : []),
        ...(components.length ? [this.section("Components", contents(components))] : []),
      ],
    });
  }

  /** Страница core или компонента по манифесту: Overview, Properties, примеры, Usage, Platform Support, History (figma-documentation.md, разделы 3.3, 3.4). */
  private manifest(id: Id): Model {
    const contract = this.bundle.contracts[id];
    return this.element(`${ELEMENTS}page`, {
      sections: this.bundle.manifests[id].sections.flatMap((section) => this.manifestSection(id, contract, section)),
    });
  }

  private manifestSection(id: Id, contract: Contract, section: ManifestSection): Model[] {
    const title = section.title ?? "";
    switch (section.type) {
      case "overview":
        // Основной пример — только у компонентов.
        return [this.element(`${ELEMENTS}heading`, { text: contract.name }), this.element(`${ELEMENTS}description`, { text: contract.description }),
          ...(section.examples ? [this.grid(section.examples)] : [])];
      case "properties":
        return [this.section(title, [this.element(`${ELEMENTS}table`, { rows: [...this.properties(id, contract), ...this.base(contract)] })])];
      case "usage": {
        const usage = this.markdown(this.bundle.texts[id]?.[USAGE]);
        return usage.length ? [this.section(title, usage)] : [];
      }
      case "platformSupport":
        return [this.platformSupport(id, contract, title)];
      case "history":
        return [this.history(id, title)];
      default:
        return [this.section(title, [this.grid(section.examples ?? [])])];
    }
  }

  /** Примеры: превью — демонстрация core или образец из `samples/` компонента, подпись — `caption`. Модель с шириной
   *  `fill` рисуется в карточке `exampleWide` ширины карточки Samples: у `example` её ширину задала бы подпись. */
  private grid(examples: ManifestExample[]): Model {
    const items = examples.map((example) => {
      const preview = (example.demo ? this.element(example.demo, {}) : this.bundle.samples[example.sample!]) as Model;
      const root = Object.values(preview)[0] as { layout?: { width?: { sizing?: unknown } } };
      const element = root.layout?.width?.sizing === "fill" ? "exampleWide" : "example";
      return this.element(`${ELEMENTS}${element}`, { preview, caption: example.caption ?? "" });
    });
    return this.element(`${ELEMENTS}grid`, { items });
  }

  /** Строка `Component` у компонента: поля базовой модели, у `appearance` — контексты (documentation-manifest.md, раздел 4.2). */
  private base(contract: Contract): Model[] {
    const base = contract.extends ? this.bundle.contracts[contract.extends] : undefined;
    if (!base) return [];
    const context = (contract.appearanceContext ?? []) as string[];
    const fields = (base.properties as Record<string, unknown>[]).map((prop) =>
      prop.name === "appearance" && context.length ? `appearance (${context.join(", ")})` : String(prop.name));
    return [this.element(`${ELEMENTS}propertyRow`, {
      name: base.name, type: base.name, description: `Базовая модель: ${fields.join(", ")}`,
      platforms: this.platforms(inherit({}, base.releaseVersions)),
    })];
  }

  /** Строки Properties: поля объекта или варианты `oneOf`; платформы — по наследованию `releaseVersions`. */
  private properties(id: Id, contract: Contract): Model[] {
    const release = inherit({}, contract.releaseVersions);
    const row = (name: string, type: string, description: string, own: unknown) => this.element(`${ELEMENTS}propertyRow`, {
      name, type, description, platforms: this.platforms(inherit(release, own)),
    });
    const target = (ref: unknown) => (typeof ref === "string" ? this.bundle.contracts[ref] : undefined);
    if (contract.type === "object") {
      return (contract.properties as Record<string, unknown>[]).map((prop) => {
        const items = prop.items as Record<string, unknown> | undefined;
        const type = prop.type === "array" ? `${target(items?.$ref)?.name ?? items?.type}[]` : target(prop.$ref)?.name ?? String(prop.type);
        return row(String(prop.name), type, String(prop.description ?? ""), prop.releaseVersions);
      });
    }
    // У элемента oneOf своего описания нет: берётся описание вспомогательного определения сущности, а у общего
    // типа (например, `Spacing` у `padding.all`) оно говорит о типе, а не о варианте, и не выводится.
    const directory = id.slice(0, id.lastIndexOf("/") + 1);
    return (contract.oneOf as Record<string, unknown>[]).map((element) => {
      const kind = target(element.$ref);
      const name = String(element.name ?? (kind ? kind.name[0].toLowerCase() + kind.name.slice(1) : ""));
      const own = typeof element.$ref === "string" && element.$ref.startsWith(directory);
      return row(name, kind?.name ?? String(element.type ?? "—"), own ? kind?.description ?? "—" : "—",
        element.releaseVersions ?? kind?.releaseVersions);
    });
  }

  /** Выпуски по платформам: первой строкой — контракт, ниже — уровни, с которых начинается более поздняя версия,
   *  по возрастанию версий (figma-documentation.md, раздел 3.3). */
  private platformSupport(id: Id, contract: Contract, title: string): Model {
    const release = inherit({}, contract.releaseVersions);
    const levels = this.levels(id.slice(0, id.lastIndexOf("/") + 1), contract, "", release, new Set());
    const rows = PLATFORMS.flatMap(([key, name]) => {
      const own = release[key];
      if (own?.status !== "released") return [this.releaseRow(name, "—", "не выпущено")];
      const groups = new Map<string, string[]>();
      for (const level of levels) {
        const version = level.release[key]?.version;
        if (level.release[key]?.status !== "released" || !later(version, level.parent[key]?.version)) continue;
        groups.set(version!, [...(groups.get(version!) ?? []), level.path]);
      }
      const versions = [...groups.keys()].sort((a, b) => (later(a, b) ? 1 : later(b, a) ? -1 : 0));
      return [this.releaseRow(name, own.version ?? "✓", contract.name),
        ...versions.map((version) => this.releaseRow("", version, groups.get(version)!.join(", ")))];
    });
    return this.section(title, [this.element(`${ELEMENTS}table`, { rows })]);
  }

  private releaseRow(platform: string, version: string, targets: string): Model {
    return this.element(`${ELEMENTS}releaseRow`, { platform, version, targets });
  }

  /** Уровни контракта с путями в формате `target` журнала: свойства, варианты `oneOf` и значения по ссылкам внутри
   *  каталога (schema-format.md, раздел 11.4, правило 2); статусы — итоговые, `parent` — итоговые статусы родителя. */
  private levels(directory: string, contract: Contract, prefix: string, release: Release, seen: Set<Contract>): { path: string; release: Release; parent: Release }[] {
    if (seen.has(contract)) return [];
    seen = new Set([...seen, contract]);
    const inside = (ref: unknown) => (typeof ref === "string" && ref.startsWith(directory) ? this.bundle.contracts[ref] : undefined);
    const result: { path: string; release: Release; parent: Release }[] = [];
    const level = (name: string, node: Record<string, unknown>, nested?: Contract) => {
      const own = inherit(release, node.releaseVersions);
      result.push({ path: prefix + name, release: own, parent: release });
      if (nested) result.push(...this.levels(directory, nested, `${prefix}${name}.`, own, seen));
    };
    for (const prop of (contract.properties ?? []) as Record<string, unknown>[]) {
      level(String(prop.name), prop, inside(prop.$ref ?? (prop.items as Record<string, unknown> | undefined)?.$ref));
    }
    for (const value of (contract.values ?? []) as Record<string, unknown>[]) level(String(value.name), value);
    for (const element of (contract.oneOf ?? []) as Record<string, unknown>[]) {
      const target = typeof element.$ref === "string" ? this.bundle.contracts[element.$ref] : undefined;
      if (element.inline && target) {
        // Развёрнутые значения входят в путь без имени элемента; сравниваются с контрактом, содержащим `oneOf`.
        const own = inherit(release, element.releaseVersions);
        for (const value of (target.values ?? []) as Record<string, unknown>[]) {
          result.push({ path: prefix + String(value.name), release: inherit(own, value.releaseVersions), parent: release });
        }
        continue;
      }
      const name = String(element.name ?? (target ? target.name[0].toLowerCase() + target.name.slice(1) : ""));
      level(name, element, inside(element.$ref));
    }
    return result;
  }

  /** История изменений: строки журнала `changelog.json` от новых к старым (figma-documentation.md, раздел 3). */
  private history(id: Id, title: string): Model {
    const rows = (this.bundle.changelogs[id]?.entries ?? []).map((entry) => this.element(`${ELEMENTS}historyRow`, {
      date: entry.issue ? `${entry.date} · #${entry.issue}` : entry.date,
      kind: KINDS[entry.kind],
      target: entry.kind === "renamed" ? `${entry.from} → ${entry.target}` : entry.target ?? "—",
      description: inline(entry.description),
    }));
    return this.section(title, [this.element(`${ELEMENTS}table`, { rows })]);
  }

  /** Выпущенные платформы с версией через « · »; если нигде не выпущено — «не выпущено». */
  private platforms(release: Release): string {
    const released = PLATFORMS.filter(([key]) => release[key]?.status === "released")
      .map(([key, name]) => (release[key].version ? `${name} ${release[key].version}` : name));
    return released.length ? released.join(" · ") : "не выпущено";
  }

  /** Страница foundation по правилу: Overview, Values, Usage, History (figma-documentation.md, раздел 3). */
  private entity(id: Id): Model {
    const contract = this.bundle.contracts[id];
    const values = this.bundle.values[id];
    const preview = `${id.split("/").slice(0, 2).join("/")}/${PREVIEW}`;
    const rows = Object.keys(values.modes[0].values).map((name) => this.element(`${ELEMENTS}valueRow`, {
      name, value: this.value(values.modes.map((mode) => mode.values[name])), sample: this.element(preview, { value: name }),
    }));
    const usage = this.markdown(this.bundle.texts[id]?.[USAGE]);
    return this.element(`${ELEMENTS}page`, {
      sections: [
        this.element(`${ELEMENTS}heading`, { text: contract.name }),
        this.element(`${ELEMENTS}description`, { text: contract.description }),
        this.section("Values", [this.element(`${ELEMENTS}table`, { rows })]),
        ...(usage.length ? [this.section("Usage", usage)] : []),
        this.history(id, "History"),
      ],
    });
  }

  /** Величина по режимам: разные — через « / », одинаковые — одна. */
  private value(perMode: unknown[]): string {
    const text = [...new Set(perMode.map((v) => this.format(v)))];
    return text.join(" / ");
  }

  private format(value: unknown): string {
    if (typeof value === "number" || typeof value === "string") return String(value);
    const v = value as Record<string, unknown>;
    if (typeof v.alias === "string") {
      const [target, name] = v.alias.split("#");
      return this.format(this.bundle.values[target]?.modes[0].values[name]);
    }
    if ("fontSize" in v) return `${v.fontSize} / ${v.lineHeight} · ${v.fontWeight}`;
    if ("blur" in v) return `y ${v.offsetY} · blur ${v.blur}`;
    return "—";
  }

  /**
   * Фрейм Presets на странице корня композиции: для каждого пресета — имя, описание, таблица осей и сетка экземпляров
   * примеров с подписями значений осей; у компонента без пресетов фрейма нет (раздел 3.4).
   */
  private async presets(page: PageNode, id: Id) {
    const presets = Object.entries(this.bundle.presets).filter(([, preset]) => preset.root === id)
      .sort(([, a], [, b]) => a.name.localeCompare(b.name));
    if (!presets.length) {
      page.children.find((n) => ref(n, "role") === "presets")?.remove();
      return;
    }
    const sections = presets.map(([key, preset]) => this.section(preset.name, [
      this.element(`${ELEMENTS}description`, { text: preset.description }),
      this.element(`${ELEMENTS}table`, { rows: [
        ...Object.entries(preset.axes).map(([axis, values]) => this.element(`${ELEMENTS}axisRow`, { axis, values: values.join(", ") })),
        this.element(`${ELEMENTS}axisRow`, { axis: "вариантов", values: String(preset.variants.length) }),
      ] }),
      this.element(`${ELEMENTS}grid`, {
        // Подпись — значения осей в порядке описания (figma-mapping.md, раздел 11.2, правило 7).
        items: (preset.examples ?? preset.variants.map((variant) => variant.axes)).map((axes) => this.element(`${ELEMENTS}example`, {
          preview: { preset: { ref: key, axes } }, caption: Object.keys(preset.axes).map((axis) => axes[axis]).join(" · "),
        })),
      }),
    ]));
    await this.area(page, id, "presets", "Presets", this.element(`${ELEMENTS}page`, {
      sections: [this.element(`${ELEMENTS}heading`, { text: "Presets" }), ...sections],
    }));
    this.place(page);
  }

  /** Generated Documentation пересобирается целиком (раздел 3). */
  private async draw(page: PageNode, id: Id, model: Model) {
    await this.area(page, id, "generated", "Generated Documentation", model);
    this.place(page);
  }

  /** Область плагина на странице: фрейм с ролью, перерисованный по модели. */
  private async area(page: PageNode, id: Id, role: string, name: string, model: Model): Promise<FrameNode> {
    await figma.setCurrentPageAsync(page);
    let frame = page.children.find((n) => ref(n, "role") === role) as FrameNode | undefined;
    if (!frame) {
      frame = figma.createFrame();
      page.appendChild(frame);
      mark(frame, role, "role");
      mark(frame, id);
    }
    frame.name = name;
    frame.layoutMode = "VERTICAL";
    frame.primaryAxisSizingMode = "AUTO";
    frame.counterAxisSizingMode = "AUTO";
    frame.fills = [];
    frame.clipsContent = false;
    const reuse = new Map<string, SceneNode>();
    for (const node of [...frame.children]) {
      if (ref(node, "slot")) reuse.set(ref(node, "slot"), node);
      else node.remove();
    }
    await new Renderer(this.tokens, this.report, page.name).draw(model, frame, reuse);
    for (const node of reuse.values()) node.remove();
    return frame;
  }

  /**
   * Generated Documentation — в начале координат или слева от других узлов страницы (на `Icon` — от компонентов
   * символов), Release Checklist — слева от неё, Presets — под ней; прежние фреймы Custom Documentation не учитываются
   * (раздел 3.2).
   */
  private place(page: PageNode) {
    const own = ["generated", "checklist", "presets", "custom"];
    const generated = page.children.find((n) => ref(n, "role") === "generated");
    const checklist = page.children.find((n) => ref(n, "role") === "checklist");
    const presets = page.children.find((n) => ref(n, "role") === "presets");
    const others = page.children.filter((n) => !own.includes(ref(n, "role")));
    if (generated) {
      generated.x = others.length ? Math.min(...others.map((n) => n.x)) - generated.width - GAP : 0;
      generated.y = 0;
    }
    if (checklist) {
      checklist.x = (generated?.x ?? 0) - checklist.width - GAP;
      checklist.y = 0;
    }
    if (presets) {
      presets.x = generated?.x ?? 0;
      presets.y = generated ? generated.y + generated.height + GAP : 0;
    }
  }

  /** Release Checklist сущности: автоматические пункты по проверкам, ручные отметки сохраняются (раздел 3.5). */
  private async checklist(page: PageNode, id: Id, warnings: number) {
    const auto: Auto = {};
    for (const [number, reason] of Object.entries(await this.checks.run(id, scope(id)!, warnings))) {
      auto[number] = reason === null;
      if (reason) this.report.warn(`${page.name} › Release Checklist, пункт ${number}: ${reason}`);
    }
    const frame = page.children.find((n) => ref(n, "role") === "checklist");
    await this.drawChecklist(page, id, auto, frame ? readJson<Checks>(frame, CHECKS_KEY) : {}, this.bundle.source);
  }

  /** Ручные отметки открытой страницы: меняются и перерисовываются без повторных проверок (раздел 3.5, правила 5, 8). */
  async updateChecks(page: PageNode, change: (checks: Checks, current: Check) => Checks) {
    const frame = page.children.find((n) => ref(n, "role") === "checklist");
    if (!frame) throw new Error("на странице нет Release Checklist");
    this.tokens = await Tokens.load(this.bundle);
    const current = readImports().documentation?.source ?? this.bundle.source;
    const checks = change(readJson<Checks>(frame, CHECKS_KEY), { commit: current.commit, committedAt: current.committedAt });
    await this.drawChecklist(page, ref(frame), readJson<Auto>(frame, AUTO_KEY), checks, current);
  }

  /** Фрейм чеклиста: статус и счёт, сборка отметок, автоматические и ручные пункты; current — сборка документации в файле. */
  private async drawChecklist(page: PageNode, id: Id, auto: Auto, checks: Checks, current: Source) {
    const items = this.bundle.checklist.items.filter((item) => item.appliesTo.includes(scope(id)!));
    const automatic = items.filter((item) => item.kind === "auto");
    const manual = items.filter((item) => item.kind === "manual");
    const status = inherit({}, this.bundle.contracts[id].releaseVersions).figma?.status ?? "—";
    const passed = automatic.filter((item) => auto[item.number]).length;
    const checked = manual.filter((item) => checks[item.number]).length;
    const rows = (list: typeof items, mark: (number: number) => string) => list.map((item) => this.element(`${ELEMENTS}checklistRow`, {
      mark: mark(item.number), number: String(item.number), text: inline(item.text),
    }));
    const first = oldest(checks);
    const build = this.element(`${ELEMENTS}description`, {
      text: first ? `Отметки на сборке ${first.commit?.slice(0, 7) ?? "вне Git"} · ${first.committedAt?.slice(0, 10) ?? "без даты"}` : "Отметок нет",
    });
    if (first && stale(checks, current)) (build.Text as Model).color = { solid: "error" };
    const model = this.element(`${ELEMENTS}page`, {
      sections: [
        this.element(`${ELEMENTS}heading`, { text: "Release Checklist" }),
        this.element(`${ELEMENTS}description`, { text: `Figma: ${status} · авто ${passed}/${automatic.length} · ручные ${checked}/${manual.length}` }),
        build,
        this.section("Автоматические", [this.element(`${ELEMENTS}table`, { rows: rows(automatic, (n) => (auto[n] ? "✓" : "✗")) })]),
        this.section("Ручные", [this.element(`${ELEMENTS}table`, { rows: rows(manual, (n) => (checks[n] ? "✓" : "—")) })]),
      ],
    });
    const frame = await this.area(page, id, "checklist", "Release Checklist", model);
    mark(frame, JSON.stringify(auto), AUTO_KEY);
    mark(frame, JSON.stringify(checks), CHECKS_KEY);
    this.place(page);
  }

  /** Порядок страниц: Overview, Foundation, Core, Components, затем библиотека (раздел 3.2). */
  private order(overview: PageNode, foundation: PageNode[], core: PageNode[], components: PageNode[]) {
    const separator = (role: string, name: string) => {
      let page = figma.root.children.find((p) => ref(p, "role") === role);
      if (!page) {
        page = figma.createPage();
        mark(page, role, "role");
      }
      page.name = name;
      return page;
    };
    const library = ["components", "presets", "samples", "screens"].map((role) => figma.root.children.find((p) => ref(p, "role") === role)).filter((p): p is PageNode => !!p);
    const ordered = [overview, separator("separator:foundation", "——— Foundation ———"), ...foundation,
      ...(core.length ? [separator("separator:core", "——— Core ———"), ...core] : []),
      ...(components.length ? [separator("separator:components", "——— Components ———"), ...components] : []),
      separator("separator:library", "——— Library ———"), ...library];
    ordered.forEach((page, index) => figma.root.insertChild(index, page));
  }
}
