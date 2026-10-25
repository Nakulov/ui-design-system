// Страницы документации: описание системы, страницы foundation и core (figma-documentation.md, разделы 3, 5).
import type { Bundle, Contract, Element, Id, ManifestSection } from "./bundle";
import { mark, ref } from "./ids";
import { Renderer, Tokens } from "./render";
import { Report } from "./report";
import { fill } from "./samples";

const ELEMENTS = "figma/documentation/elements/";
const PREVIEWS = "figma/documentation/previews/";
const OVERVIEW = "docs/overview";
const USAGE = "Рекомендации по использованию";
const GAP = 120;
const PLATFORMS: [string, string][] = [["android", "Android"], ["ios", "iOS"], ["figma", "Figma"]];

type Span = { text: { value: string; typography?: string; color?: string; decoration?: string } };
type Model = Record<string, unknown>;

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

  constructor(private readonly bundle: Bundle) {}

  async build(): Promise<Report> {
    this.tokens = await Tokens.load(this.bundle);
    await figma.loadAllPagesAsync();
    const foundation = Object.entries(this.bundle.contracts)
      .filter(([id]) => id.startsWith("schemas/foundation/") && id.split("/").length === 4 && this.bundle.values[id])
      .sort(([, a], [, b]) => a.name.localeCompare(b.name));
    // Страницы core — у сущностей с манифестом (documentation-manifest.md, раздел 2).
    const core = Object.entries(this.bundle.contracts)
      .filter(([id]) => id.startsWith("schemas/core/") && this.bundle.manifests[id])
      .sort(([, a], [, b]) => a.name.localeCompare(b.name));
    const overview = await this.page((p) => ref(p, "role") === "overview", "Overview", (p) => mark(p, "overview", "role"));
    await this.draw(overview, "overview", this.overview(foundation.map(([id]) => id), core.map(([id]) => id)));
    const pages = async (entities: [Id, Contract][], build: (id: Id) => Model) => {
      const result: PageNode[] = [];
      for (const [id, contract] of entities) {
        const page = await this.page((p) => ref(p) === id, contract.name, (p) => mark(p, id));
        await this.draw(page, id, build(id));
        result.push(page);
      }
      return result;
    };
    const foundationPages = await pages(foundation, (id) => this.entity(id));
    const corePages = await pages(core, (id) => this.core(id));
    this.order(overview, foundationPages, corePages);
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

  /** Описание системы: разделы `docs/overview.md` и оглавления foundation и core. */
  private overview(foundation: Id[], core: Id[]): Model {
    const sections = Object.entries(this.bundle.texts[OVERVIEW] ?? {}).map(([title, text]) => this.section(title, this.markdown(text)));
    const contents = (ids: Id[]) => ids.map((id) => this.element(`${ELEMENTS}listItem`, {
      marker: "•", text: [{ text: { value: this.bundle.contracts[id].name, typography: "code" } }, { text: { value: ` — ${this.bundle.contracts[id].description}` } }],
    }));
    return this.element(`${ELEMENTS}page`, {
      sections: [
        this.element(`${ELEMENTS}heading`, { text: "UI Design System" }), ...sections,
        this.section("Foundation", contents(foundation)), ...(core.length ? [this.section("Core", contents(core))] : []),
      ],
    });
  }

  /** Страница core по манифесту: Overview, Properties, демонстрации, Usage (figma-documentation.md, раздел 3.3). */
  private core(id: Id): Model {
    const contract = this.bundle.contracts[id];
    return this.element(`${ELEMENTS}page`, {
      sections: this.bundle.manifests[id].sections.flatMap((section) => this.manifestSection(id, contract, section)),
    });
  }

  private manifestSection(id: Id, contract: Contract, section: ManifestSection): Model[] {
    const title = section.title ?? "";
    switch (section.type) {
      case "overview":
        return [this.element(`${ELEMENTS}heading`, { text: contract.name }), this.element(`${ELEMENTS}description`, { text: contract.description })];
      case "properties":
        return [this.section(title, [this.element(`${ELEMENTS}table`, { rows: this.properties(id, contract) })])];
      case "usage": {
        const usage = this.markdown(this.bundle.texts[id]?.[USAGE]);
        return usage.length ? [this.section(title, usage)] : [];
      }
      default: {
        const items = (section.examples ?? []).map((example) => this.element(`${ELEMENTS}example`, {
          preview: this.element(example.demo, {}), caption: example.caption ?? "",
        }));
        return [this.section(title, [this.element(`${ELEMENTS}grid`, { items })])];
      }
    }
  }

  /** Строки Properties: поля объекта или варианты `oneOf`; платформы — по наследованию `releaseVersions`. */
  private properties(id: Id, contract: Contract): Model[] {
    const release = (contract.releaseVersions ?? {}) as Record<string, string>;
    const row = (name: string, type: string, description: string, own: unknown) => this.element(`${ELEMENTS}propertyRow`, {
      name, type, description, platforms: this.platforms({ ...release, ...((own ?? {}) as Record<string, string>) }),
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

  /** Выпущенные платформы через « · »; если нигде не выпущено — «не выпущено». */
  private platforms(release: Record<string, string>): string {
    const released = PLATFORMS.filter(([key]) => release[key] === "released").map(([, name]) => name);
    return released.length ? released.join(" · ") : "не выпущено";
  }

  /** Страница foundation по правилу: Overview, Values, Usage (figma-documentation.md, раздел 3). */
  private entity(id: Id): Model {
    const contract = this.bundle.contracts[id];
    const values = this.bundle.values[id];
    const preview = `${PREVIEWS}${id.split("/")[2]}`;
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

  /** Область Generated Documentation пересобирается, Custom Documentation создаётся один раз (раздел 3). */
  private async draw(page: PageNode, id: Id, model: Model) {
    await figma.setCurrentPageAsync(page);
    let generated = page.children.find((n) => ref(n, "role") === "generated") as FrameNode | undefined;
    if (!generated) {
      generated = figma.createFrame();
      page.appendChild(generated);
      mark(generated, "generated", "role");
      mark(generated, id);
    }
    generated.name = "Generated Documentation";
    generated.layoutMode = "VERTICAL";
    generated.primaryAxisSizingMode = "AUTO";
    generated.counterAxisSizingMode = "AUTO";
    generated.fills = [];
    generated.clipsContent = false;
    const reuse = new Map<string, SceneNode>();
    for (const node of [...generated.children]) {
      if (ref(node, "slot")) reuse.set(ref(node, "slot"), node);
      else node.remove();
    }
    await new Renderer(this.tokens, this.report, page.name).draw(model, generated, reuse);
    for (const node of reuse.values()) node.remove();
    // На странице символов документация стоит слева от компонентов `icon/<имя>`.
    const others = page.children.filter((n) => n !== generated && ref(n, "role") !== "custom");
    generated.x = others.length ? Math.min(...others.map((n) => n.x)) - generated.width - GAP : 0;
    generated.y = 0;
    if (!page.children.some((n) => ref(n, "role") === "custom")) {
      const custom = figma.createFrame();
      page.appendChild(custom);
      mark(custom, "custom", "role");
      mark(custom, id);
      custom.name = "Custom Documentation";
      custom.resize(600, 400);
      custom.x = generated.x;
      custom.y = generated.y + generated.height + GAP;
    }
  }

  /** Порядок страниц: Overview, Foundation, Core, затем библиотека (раздел 3.2). */
  private order(overview: PageNode, foundation: PageNode[], core: PageNode[]) {
    const separator = (role: string, name: string) => {
      let page = figma.root.children.find((p) => ref(p, "role") === role);
      if (!page) {
        page = figma.createPage();
        mark(page, role, "role");
      }
      page.name = name;
      return page;
    };
    const library = ["components", "samples"].map((role) => figma.root.children.find((p) => ref(p, "role") === role)).filter((p): p is PageNode => !!p);
    const ordered = [overview, separator("separator:foundation", "——— Foundation ———"), ...foundation,
      ...(core.length ? [separator("separator:core", "——— Core ———"), ...core] : []), separator("separator:library", "——— Library ———"), ...library];
    ordered.forEach((page, index) => figma.root.insertChild(index, page));
  }
}
