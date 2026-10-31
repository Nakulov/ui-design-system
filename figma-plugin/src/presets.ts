// Страница Presets: Component Set пресетов из описаний (figma-mapping.md, раздел 11).
import type { Bundle, Id, Preset, PresetLayout } from "./bundle";
import { removeVariant } from "./components";
import { COLOR, CORNER_RADIUS, mark, ref } from "./ids";
import { Renderer, Tokens } from "./render";
import type { Report } from "./report";

const ROLE = "presets";
/** Подписи значений осей рядом с Component Set: перерисовываются и удаляются вместе с пресетом (раздел 11.3, правило 5). */
const LABELS = "presetLabels";
const GAP = 96;
const CELL_GAP = 32;
const BLOCK_GAP = 48;
const LABEL_WIDTH = 200;
const LABEL_HEIGHT = 40;
const LABEL_STYLE = "labelMedium";
const LABEL_COLOR = "onSurfaceMuted";
const TITLE_HEIGHT = 28;
const BOX_COLOR = "outlineStrong";
const BOX_RADIUS = "l";

type Values = Record<string, string>;
/** Подпись или рамка блока витрины. */
type Mark = { text: string; x: number; y: number } | { box: { x: number; y: number; width: number; height: number } };
type Model = Record<string, { id?: string; [field: string]: unknown }>;

/** Оси поля `layout`: одна ось или список, первая — внешняя. */
function axesOf(value: string | string[] | undefined): string[] {
  return value === undefined ? [] : typeof value === "string" ? [value] : value;
}

/** Все сочетания значений осей по порядку: первая ось — внешняя. */
function combinations(axes: string[], preset: Preset): Values[] {
  return axes.reduce<Values[]>((result, axis) => result.flatMap((c) => preset.axes[axis].map((v) => ({ ...c, [axis]: v }))), [{}]);
}

/** Имя variant: `ось=значение` через запятую в порядке осей описания. */
export function variantName(preset: Preset, values: Values): string {
  return Object.keys(preset.axes).map((axis) => `${axis}=${values[axis]}`).join(", ");
}

/** Ссылки пресета на другие пресеты: узлы `preset` моделей и значения `swaps` (раздел 11.1, правило 7). */
function references(preset: Preset): Id[] {
  const result = new Set<Id>();
  const walk = (node: unknown) => {
    if (Array.isArray(node)) node.forEach(walk);
    else if (node && typeof node === "object") {
      const entries = Object.entries(node);
      if (entries.length === 1 && entries[0][0] === "preset") {
        const target = (entries[0][1] as { ref?: unknown }).ref;
        if (typeof target === "string") result.add(target);
        return;
      }
      for (const [, value] of entries) walk(value);
    }
  };
  for (const variant of preset.variants) walk(variant.model);
  for (const values of Object.values(preset.swaps ?? {})) for (const value of values) if (value.startsWith("figma/presets/")) result.add(value);
  return [...result];
}

/** Слои Figma-компонента без слоёв внутри экземпляров: сами экземпляры входят. */
function ownLayers(node: ComponentNode): SceneNode[] {
  const result: SceneNode[] = [];
  const visit = (layer: SceneNode) => {
    result.push(layer);
    if (layer.type !== "INSTANCE" && "children" in layer) for (const child of layer.children) visit(child);
  };
  for (const child of node.children) visit(child);
  return result;
}

export class PresetsBuilder {
  constructor(private readonly bundle: Bundle, private readonly report: Report) {}

  /** Пресеты строятся после компонентов той же сборкой (раздел 11.3, правило 2). */
  async build() {
    const tokens = await Tokens.load(this.bundle);
    const page = await this.page();
    const ids = Object.keys(this.bundle.presets);
    for (const node of [...page.children]) {
      if (ref(node) && !ids.includes(ref(node))) await this.remove(node);
    }
    let y = 0;
    const built = new Map<Id, ComponentSetNode>();
    for (const id of this.order()) {
      const set = await this.preset(page, id, tokens);
      if (set) built.set(id, set);
    }
    // На странице — по алфавиту сверху вниз, подписи — рядом со своим набором.
    for (const id of [...built.keys()].sort((a, b) => this.bundle.presets[a].name.localeCompare(this.bundle.presets[b].name))) {
      const set = built.get(id)!;
      const labels = page.children.find((n) => ref(n) === id && ref(n, "role") === LABELS);
      set.x = LABEL_WIDTH;
      set.y = y + LABEL_HEIGHT;
      if (labels) {
        labels.x = set.x;
        labels.y = set.y;
      }
      y = set.y + set.height + GAP;
    }
  }

  private async page(): Promise<PageNode> {
    await figma.loadAllPagesAsync();
    let page = figma.root.children.find((p) => ref(p, "role") === ROLE);
    if (!page) {
      page = figma.createPage();
      page.name = "Presets";
      mark(page, ROLE, "role");
    }
    // Сразу после Components (figma-documentation.md, раздел 3.2); полный порядок ставит сборка документации.
    const components = figma.root.children.find((p) => ref(p, "role") === "components");
    if (components) figma.root.insertChild(figma.root.children.indexOf(components) + 1, page);
    await figma.setCurrentPageAsync(page);
    return page;
  }

  /** Порядок сборки по ссылкам: сначала те, на которые ссылаются; независимые — по алфавиту (раздел 11.3, правило 3). */
  private order(): Id[] {
    const result: Id[] = [];
    const visit = (id: Id, from: Id[]) => {
      if (result.includes(id) || from.includes(id)) return;
      const preset = this.bundle.presets[id];
      for (const target of references(preset).sort()) {
        if (!this.bundle.presets[target]) this.report.warn(`${preset.name}: ссылка на пресет ${target}, которого нет в наборе`);
        else visit(target, [...from, id]);
      }
      result.push(id);
    };
    for (const id of Object.keys(this.bundle.presets).sort((a, b) => this.bundle.presets[a].name.localeCompare(this.bundle.presets[b].name))) visit(id, []);
    return result;
  }

  private async remove(node: SceneNode) {
    if (node.type === "COMPONENT_SET") {
      for (const variant of [...node.children] as ComponentNode[]) await removeVariant(variant, node.name, this.report);
      this.report.deleted("пресеты");
    }
    if (!node.removed) node.remove();
  }

  /**
   * Component Set пресета по идентификатору, варианты — по значениям осей; тело перерисовывается на месте (раздел 11.3,
   * правило 4).
   */
  private async preset(page: PageNode, id: Id, tokens: Tokens): Promise<ComponentSetNode | undefined> {
    const preset = this.bundle.presets[id];
    if (!preset.variants.length) return undefined;
    const existing = page.children.find((n) => ref(n) === id && ref(n, "role") !== LABELS);
    const set = existing?.type === "COMPONENT_SET" ? existing : undefined;
    if (existing && !set) existing.remove();
    if (set) this.report.updated("пресеты");
    else this.report.created("пресеты");
    const old = set ? await this.migrate(set, preset) : new Map<string, ComponentNode>();
    const nodes = new Map<string, ComponentNode>();
    // Каждая правка слоя внутри варианта заставляет Figma перепроверять весь набор, на сотнях вариантов это в сотню раз
    // дороже. Вариант перерисовывается во временном фрейме на странице и возвращается в набор: id и ключ сохраняются.
    const holder = figma.createFrame();
    holder.name = `${preset.name} (temp)`;
    holder.fills = [];
    page.appendChild(holder);
    try {
      for (const [index, variant] of preset.variants.entries()) {
        const name = variantName(preset, variant.axes);
        const node = old.get(name) ?? figma.createComponent();
        old.delete(name);
        holder.appendChild(node);
        // Привязки к свойствам набора снимаются до отрисовки и ставятся заново в `swaps`; слои внутри экземпляров — не
        // свои, их привязки задаёт их Figma-компонент.
        for (const layer of ownLayers(node)) if (Object.keys(layer.componentPropertyReferences ?? {}).length) layer.componentPropertyReferences = {};
        const model = variant.model as Model;
        const slot = Object.values(model)[0].id ?? "root";
        const label = `${preset.name} (${Object.values(variant.axes).join(", ")})`;
        const drawn = await new Renderer(tokens, this.report, label, false, true).draw(model, holder, new Map([[slot, node]]), slot);
        if ((drawn as SceneNode | undefined) !== node) this.report.warn(`${label}: вариант не отрисован в Figma-компонент`);
        // В набор вариант возвращается уже с именем по осям: вариант с другим именем ломает набор.
        node.name = name;
        (set ?? page).appendChild(node);
        this.expose(node, label);
        nodes.set(name, node);
        // Прогресс в окне: на сотнях вариантов сборка идёт минуты.
        if ((index + 1) % 10 === 0) figma.ui.postMessage({ type: "busy", message: `${preset.name}: ${index + 1}/${preset.variants.length}` });
      }
    } finally {
      // Вариант, вынесенный перед ошибкой, возвращается в набор: иначе он остался бы отдельным компонентом.
      for (const child of [...holder.children]) (set ?? page).appendChild(child);
      holder.remove();
    }
    for (const node of old.values()) await removeVariant(node, preset.name, this.report);
    const result = set ?? figma.combineAsVariants([...nodes.values()], page);
    result.name = preset.name;
    mark(result, id);
    tokens.register(id, result);
    this.orderValues(result, preset);
    await this.swaps(result, preset, tokens);
    await this.showcase(page, result, preset, id, nodes, tokens);
    return result;
  }

  /**
   * Существующие варианты по именам с осями описания. Добавленная ось получает первое значение; при удалённой оси
   * остаётся вариант с её первым значением, остальные удаляются (раздел 11.3, правило 4).
   */
  private async migrate(set: ComponentSetNode, preset: Preset): Promise<Map<string, ComponentNode>> {
    // Вариант с именем не по осям (например, после прерванной сборки) ломает набор: Figma не отдаёт его свойства.
    for (const node of [...set.children] as ComponentNode[]) {
      if (!node.name.split(", ").every((pair) => /^[^=]+=[^=]+$/.test(pair))) await removeVariant(node, preset.name, this.report);
    }
    const axes = Object.keys(preset.axes);
    const removed = Object.entries(set.componentPropertyDefinitions)
      .filter(([axis, definition]) => definition.type === "VARIANT" && !axes.includes(axis))
      .map(([axis, definition]) => [axis, definition.variantOptions?.[0]] as const);
    const result = new Map<string, ComponentNode>();
    for (const node of [...set.children] as ComponentNode[]) {
      const own = node.variantProperties ?? {};
      const name = variantName(preset, Object.fromEntries(axes.map((axis) => [axis, own[axis] ?? preset.axes[axis][0]])));
      if (removed.some(([axis, first]) => own[axis] !== first) || result.has(name)) await removeVariant(node, preset.name, this.report);
      else result.set(name, node);
    }
    return result;
  }

  /** Экземпляры с `id` выставляются наружу (exposed nested instance), без `id` — нет (раздел 11.1, правило 6). */
  private expose(node: ComponentNode, label: string) {
    const visit = (layer: SceneNode) => {
      if (layer.type === "INSTANCE") {
        const exposed = ref(layer, "id") !== "";
        try {
          if (layer.isExposedInstance !== exposed) layer.isExposedInstance = exposed;
        } catch (e) {
          this.report.warn(`${label}: ${layer.name} не выставлен наружу: ${(e as Error).message}`);
        }
        return;
      }
      if ("children" in layer) for (const child of layer.children) visit(child);
    };
    for (const child of node.children) visit(child);
  }

  /**
   * Значения осей в порядке описания (раздел 11.3, правило 4). Figma хранит порядок значений в наборе и ставит значение,
   * которое исчезло и появилось снова, в конец: варианты значений, начиная с первого не на своём месте, выносятся на
   * страницу и возвращаются по порядку. id и ключи вариантов сохраняются.
   */
  private orderValues(set: ComponentSetNode, preset: Preset) {
    for (const [axis, values] of Object.entries(preset.axes)) {
      const current = set.componentPropertyDefinitions[axis]?.variantOptions ?? [];
      const first = values.findIndex((value, index) => current[index] !== value);
      if (first < 0) continue;
      for (const value of values.slice(first)) {
        const nodes = (set.children as ComponentNode[]).filter((node) => node.variantProperties?.[axis] === value);
        try {
          for (const node of nodes) set.parent!.appendChild(node);
        } finally {
          for (const node of nodes) set.appendChild(node);
        }
      }
    }
  }

  /**
   * Места `swaps` — instance swap-свойства с предпочтительными значениями (раздел 11.2, правило 3). Значение свойства одно
   * на весь Component Set: вариант с другим содержимым места получает своё свойство `<место> (<значения осей>)`.
   */
  private async swaps(set: ComponentSetNode, preset: Preset, tokens: Tokens) {
    for (const [place, values] of Object.entries(preset.swaps ?? {})) {
      const preferred: InstanceSwapPreferredValue[] = values.flatMap((value) => {
        const node = tokens.library(value);
        return node ? [{ type: node.type === "COMPONENT_SET" ? "COMPONENT_SET" : "COMPONENT", key: node.key } as InstanceSwapPreferredValue] : [];
      });
      let first: string | undefined;
      for (const variant of set.children as ComponentNode[]) {
        const layer = (ownLayers(variant).find((n) => n.type === "INSTANCE" && ref(n, "id") === place) ?? null) as InstanceNode | null;
        const main = layer && await layer.getMainComponentAsync();
        if (!layer || !main) continue;
        first ??= main.id;
        const name = main.id === first ? place : `${place} (${Object.values(variant.variantProperties ?? {}).join(", ")})`;
        const key = Object.entries(set.componentPropertyDefinitions).find(([k, d]) => k.split("#")[0] === name && d.type === "INSTANCE_SWAP")?.[0];
        const property = key ?? set.addComponentProperty(name, "INSTANCE_SWAP", main.id, { preferredValues: preferred });
        if (key) set.editComponentProperty(key, { defaultValue: main.id, preferredValues: preferred });
        layer.componentPropertyReferences = { mainComponent: property };
      }
    }
    const used = new Set(set.findAll((n) => !!n.componentPropertyReferences).flatMap((n) => Object.values(n.componentPropertyReferences ?? {})));
    for (const [key, definition] of Object.entries(set.componentPropertyDefinitions)) {
      if (definition.type !== "VARIANT" && !used.has(key)) set.deleteComponentProperty(key);
    }
  }

  /**
   * Витрина по `layout`: блоки друг под другом, каждый в рамке с заголовком, в блоке строки и столбцы; без `layout` —
   * одна строка в порядке описания. Клетки без варианта пустые (раздел 11.3, правило 5).
   */
  private async showcase(page: PageNode, set: ComponentSetNode, preset: Preset, id: Id, nodes: Map<string, ComponentNode>, tokens: Tokens) {
    const layout: PresetLayout | undefined = preset.layout;
    const blocks = combinations(axesOf(layout?.blocks), preset);
    const rows = layout ? combinations(axesOf(layout.rows), preset) : [{}];
    const columns = layout ? combinations(axesOf(layout.columns), preset) : preset.variants.map((v) => v.axes);
    const framed = blocks.length > 1 || Object.keys(blocks[0]).length > 0;
    const at = (block: Values, row: Values, column: Values) => nodes.get(variantName(preset, { ...block, ...row, ...column }));
    const widths = columns.map((column) => Math.max(0, ...blocks.flatMap((block) => rows.map((row) => at(block, row, column)?.width ?? 0))));
    // Клетки отступают от рамки блока на CELL_GAP, рамка — от края набора на половину CELL_GAP.
    const edge = CELL_GAP / 2;
    const left = framed ? edge + CELL_GAP : CELL_GAP;
    const right = left + widths.reduce((sum, width) => sum + width + CELL_GAP, 0) - CELL_GAP;
    const marks: Mark[] = [];
    let y = CELL_GAP;
    for (const block of blocks) {
      const cells = rows.map((row) => columns.map((column) => at(block, row, column)));
      if (!cells.flat().some(Boolean)) continue;
      const top = y + (framed ? TITLE_HEIGHT : 0);
      if (framed) {
        marks.push({ text: Object.entries(block).map(([axis, value]) => `${axis}: ${value}`).join(" · "), x: edge, y });
        y = top + CELL_GAP;
      }
      for (const [index, row] of rows.entries()) {
        if (!cells[index].some(Boolean)) continue;
        const height = Math.max(...cells[index].map((n) => n?.height ?? 0));
        if (Object.keys(row).length) marks.push({ text: Object.values(row).join(" · "), x: -LABEL_WIDTH + 16, y: y + height / 2 - 8 });
        let x = left;
        for (const [column, node] of cells[index].entries()) {
          if (node) {
            node.x = x;
            node.y = y;
          }
          x += widths[column] + CELL_GAP;
        }
        y += height + CELL_GAP;
      }
      if (framed) {
        marks.push({ box: { x: edge, y: top, width: right + CELL_GAP - edge, height: y - top } });
        y += BLOCK_GAP;
      }
    }
    let x = left;
    for (const [index, column] of columns.entries()) {
      if (layout) marks.push({ text: Object.values(column).join(" · "), x, y: -LABEL_HEIGHT + 8 });
      x += widths[index] + CELL_GAP;
    }
    set.resize(right + (framed ? CELL_GAP + edge : CELL_GAP), Math.max(framed ? y - BLOCK_GAP + edge : y, 1));
    await this.labels(page, id, marks, tokens);
  }

  /**
   * Фрейм подписей значений осей и рамок блоков поверх набора; координаты — от левого верхнего угла набора. Рамки без
   * заливки и лежат в промежутках между клетками, поэтому варианты под ними видны.
   */
  private async labels(page: PageNode, id: Id, marks: Mark[], tokens: Tokens) {
    let frame = page.children.find((n) => ref(n) === id && ref(n, "role") === LABELS) as FrameNode | undefined;
    if (!frame) {
      frame = figma.createFrame();
      page.appendChild(frame);
      mark(frame, id);
      mark(frame, LABELS, "role");
    }
    frame.name = `${this.bundle.presets[id].name} labels`;
    frame.fills = [];
    frame.clipsContent = false;
    frame.locked = true;
    frame.resize(1, 1);
    for (const child of [...frame.children]) child.remove();
    const style = tokens.textStyle(LABEL_STYLE);
    const font = style?.fontName ?? { family: "Inter", style: "Regular" };
    await figma.loadFontAsync(font);
    const paint = (role: string): Paint[] => {
      const variable = tokens.variable(COLOR, role);
      return variable ? [figma.variables.setBoundVariableForPaint({ type: "SOLID", color: { r: 0, g: 0, b: 0 } }, "color", variable)] : [];
    };
    const radius = tokens.variable(CORNER_RADIUS, BOX_RADIUS);
    for (const item of marks) {
      if ("box" in item) {
        const box = figma.createRectangle();
        frame.appendChild(box);
        box.name = "block";
        box.x = item.box.x;
        box.y = item.box.y;
        box.resize(item.box.width, item.box.height);
        box.fills = [];
        box.strokes = paint(BOX_COLOR);
        box.strokeWeight = 1;
        box.dashPattern = [6, 4];
        if (radius) for (const field of ["topLeftRadius", "topRightRadius", "bottomLeftRadius", "bottomRightRadius"] as const) box.setBoundVariable(field, radius);
        continue;
      }
      const text = figma.createText();
      frame.appendChild(text);
      text.fontName = font;
      if (style) await text.setTextStyleIdAsync(style.id);
      text.characters = item.text;
      text.fills = paint(LABEL_COLOR);
      text.x = item.x;
      text.y = item.y;
    }
  }
}
