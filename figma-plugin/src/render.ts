// Отрисовка модели слоями Figma в режиме тела (specifications/figma-mapping.md, раздел 10).
import type { Bundle, Id } from "./bundle";
import {
  BORDER_WIDTH, COLOR, CORNER_RADIUS, ELEVATION, ICON, SIZE, SPACING, TYPOGRAPHY, mark, prefix, ref, rgba,
} from "./ids";
import type { Report } from "./report";

/** Величина foundation-сущности: вариант шкалы или вариант со свойствами (`custom`). */
type Token = string | { custom: { value: number } };
type Sizing = "hug" | "fill" | { fixed: Token };
type Dimension = { sizing: Sizing; min?: Token; max?: Token };
type Pin = Record<string, { offset: Token } | Record<string, Token>>;
type Pinned = { horizontal: Pin; vertical: Pin; relativeTo?: "edge" | "content" };
type Layout = { width: Dimension; height: Dimension; position: "flow" | { pinned: Pinned } };
type Fill = { solid: string };
type ElevationStyle = { offsetX: number; offsetY: number; blur: number; spread?: number; color: string };
type Plain = {
  background?: Fill;
  shadow?: string | { custom: ElevationStyle };
  cornerRadius?: string;
  stroke?: { width: Token; color: string };
  overlay?: Fill;
  clip?: boolean;
};
type Base = {
  id?: string;
  layout: Layout;
  padding: Record<string, unknown>;
  appearance: Record<string, unknown>;
  indicators?: unknown[];
  selected?: boolean;
  [field: string]: unknown;
};
/** Модель компонента: объект с одним ключом — именем компонента (schema-format.md, раздел 4.5). */
type Entry = Record<string, Base>;
type Indicator = { anchor: string; content: Entry; gap?: Token };
type Resource = { icon: { name: string; color: string } };
type Remote = { origin: { url: string }; scale: "fit" | "crop"; alignment: string; placeholder?: Resource; error?: Resource };
type Span = { text: { value: string; typography?: string; color?: string; decoration?: string } } | { icon: { name: string; typography?: string; color?: string } };

const DEFAULT_FONT: FontName = { family: "Inter", style: "Regular" };

const ALIGN: Record<string, "MIN" | "CENTER" | "MAX"> = { start: "MIN", top: "MIN", center: "CENTER", end: "MAX", bottom: "MAX" };
const TEXT_ALIGN: Record<string, "LEFT" | "CENTER" | "RIGHT"> = { start: "LEFT", center: "CENTER", end: "RIGHT" };
const DECORATION: Record<string, TextDecoration> = { underline: "UNDERLINE", strikethrough: "STRIKETHROUGH" };
/** Доли смещения картинки по осям для `ImageAlignment`. */
const IMAGE_ALIGN: Record<string, [number, number]> = {
  topStart: [0, 0], topCenter: [0.5, 0], topEnd: [1, 0],
  centerStart: [0, 0.5], center: [0.5, 0.5], centerEnd: [1, 0.5],
  bottomStart: [0, 1], bottomCenter: [0.5, 1], bottomEnd: [1, 1],
};

const BOUND_FIELDS: VariableBindableNodeField[] = [
  "width", "height", "itemSpacing", "counterAxisSpacing", "paddingLeft", "paddingRight", "paddingTop", "paddingBottom",
  "topLeftRadius", "topRightRadius", "bottomLeftRadius", "bottomRightRadius", "minWidth", "maxWidth", "minHeight",
  "maxHeight", "strokeWeight",
];

/** Родитель с Auto Layout: дети в потоке. */
function isAuto(parent: BaseNode): boolean {
  return "layoutMode" in parent && parent.layoutMode !== "NONE";
}

/** Имя слоя модели: `<Компонент> · <id>`, без `id` — `<Компонент>` (раздел 10.2). */
function layerName(name: string, id: string | undefined): string {
  return id ? `${name} · ${id}` : name;
}

/** Вариант значения `oneOf` или `enum`: имя и значение (`schema-format.md`, раздел 4.5). */
function variant<T = unknown>(value: unknown): [string, T] {
  if (typeof value === "string") return [value, undefined as T];
  const [name, inner] = Object.entries(value as Record<string, unknown>)[0];
  return [name, inner as T];
}

/** Переменные, стили и символы foundation, построенные плагином (раздел 3.1). */
export class Tokens {
  private constructor(
    private readonly bundle: Bundle,
    private readonly variables: Map<Id, Variable>,
    private readonly textStyles: Map<Id, TextStyle>,
    private readonly effectStyles: Map<Id, EffectStyle>,
    private readonly icons: Map<Id, ComponentNode>,
    private readonly components: Map<Id, ComponentNode | ComponentSetNode>,
  ) {}

  /** Компоненты без Figma-компонента в библиотеке, о которых уже предупредили за этот прогон. */
  readonly missing = new Set<Id>();

  /** Величины из файла; если foundation не построен — ошибка. */
  static async load(bundle: Bundle): Promise<Tokens> {
    const byRef = <T extends { getSharedPluginData(namespace: string, key: string): string }>(items: T[]) =>
      new Map(items.filter((item) => ref(item)).map((item) => [ref(item), item]));
    const variables = byRef(await figma.variables.getLocalVariablesAsync());
    if (![...variables.keys()].some((id) => id.startsWith(`${COLOR}#`))) {
      throw new Error("В файле нет переменных foundation: сначала постройте foundation");
    }
    await figma.loadAllPagesAsync();
    const iconPage = figma.root.children.find((page) => ref(page) === ICON);
    const componentsPage = figma.root.children.find((page) => ref(page, "role") === "components");
    return new Tokens(
      bundle, variables,
      byRef(await figma.getLocalTextStylesAsync()),
      byRef(await figma.getLocalEffectStylesAsync()),
      byRef(iconPage?.findAllWithCriteria({ types: ["COMPONENT"] }) ?? []),
      byRef((componentsPage?.children ?? []).filter((n): n is ComponentNode | ComponentSetNode => n.type === "COMPONENT" || n.type === "COMPONENT_SET")),
    );
  }

  variable(contract: Id, name: string): Variable | undefined {
    return this.variables.get(`${contract}#${name}`);
  }

  textStyle(role: string): TextStyle | undefined {
    return this.textStyles.get(`${TYPOGRAPHY}#${role}`);
  }

  effectStyle(level: string): EffectStyle | undefined {
    return this.effectStyles.get(`${ELEVATION}#${level}`);
  }

  icon(name: string): ComponentNode | undefined {
    return this.icons.get(`${ICON}#${name}`);
  }

  /** Figma-компонент библиотеки по идентификатору (раздел 7.1); удалённый вручную не учитывается. */
  library(id: Id): ComponentNode | ComponentSetNode | undefined {
    const node = this.components.get(id);
    return node && !node.removed ? node : undefined;
  }

  /** Число величины: вариант шкалы — из первого режима темы, `custom` — его значение. */
  number(contract: Id, token: Token): number {
    if (typeof token !== "string") return token.custom.value;
    return this.bundle.values[contract].modes[0].values[token] as number;
  }

  /** Идентификатор публичного контракта компонента по имени. */
  component(name: string): Id | undefined {
    return Object.keys(this.bundle.contracts).find((id) => id.startsWith("components/") && this.bundle.contracts[id].name === name);
  }
}

/** Слои, найденные при сбросе: ключ — место слоя в модели (`slot`). */
type Kept = Map<string, SceneNode>;

/**
 * Отрисовка одной модели. Слои строятся сразу; раскладка `overlay`, координаты `pinned`, слой наложения и вписывание
 * картинок требуют готовых размеров и выполняются после.
 *
 * Каждый слой помечен местом в модели (`sharedPluginData`, ключ `slot`): `id` модели или путь (`children[1]`, `text`,
 * `icon`). Повторная отрисовка обновляет слой с тем же местом и видом, а не пересоздаёт его: правки в экземплярах
 * Figma-компонентов привязаны к слоям (раздел 7, правило 8).
 */
export class Renderer {
  private readonly layouts: (() => void)[] = [];
  private readonly placements: (() => void)[] = [];
  private readonly fonts = new Set<string>();

  /** `library` — тело образца библиотеки: картинки по адресу не загружаются (раздел 4.1, правило 12). */
  constructor(
    private readonly tokens: Tokens, private readonly report: Report, private readonly label: string,
    private readonly library = false,
  ) {}

  /**
   * Рисует модель последним ребёнком `parent`. Корень из `reuse` с местом `slot` (или `id` модели) обновляется на
   * месте; у образца библиотеки это сам Figma-компонент.
   */
  async draw(entry: unknown, parent: BaseNode & ChildrenMixin, reuse: Kept, slot = "root"): Promise<FrameNode | undefined> {
    const node = await this.component(entry as Entry, parent as FrameNode, reuse, "", slot);
    // Размеры вложенных контейнеров overlay нужны родителям, а размеры родителей — растянутым детям.
    for (const layout of [...this.layouts].reverse()) layout();
    for (const layout of this.layouts) layout();
    for (const place of this.placements) place();
    if (node) await this.resolvePaints(node);
    return node;
  }

  /**
   * Заливки с переменными цвета получают значение переменной для своего слоя и привязываются заново. Figma Desktop не
   * пересчитывает значение у текстов и фигур, привязанных через API внутри фрейма с явным режимом коллекции, и рисует
   * заглушку, пока привязку не обновят.
   */
  private async resolvePaints(root: SceneNode) {
    const variables = new Map<string, Variable | null>();
    const resolve = async (paints: readonly Paint[], node: SceneNode): Promise<Paint[] | undefined> => {
      let changed = false;
      const result: Paint[] = [];
      for (const paint of paints) {
        const id = paint.type === "SOLID" ? paint.boundVariables?.color?.id : undefined;
        if (!id) {
          result.push(paint);
          continue;
        }
        if (!variables.has(id)) variables.set(id, await figma.variables.getVariableByIdAsync(id));
        const variable = variables.get(id);
        const value = variable?.resolveForConsumer(node).value as RGBA | undefined;
        if (!variable || !value || typeof value !== "object" || !("r" in value)) {
          result.push(paint);
          continue;
        }
        const solid: SolidPaint = { type: "SOLID", color: { r: value.r, g: value.g, b: value.b }, opacity: value.a ?? 1 };
        result.push(figma.variables.setBoundVariableForPaint(solid, "color", variable));
        changed = true;
      }
      return changed ? result : undefined;
    };
    const nodes = "findAll" in root ? [root, ...root.findAll()] : [root];
    for (const node of nodes) {
      if (node.type === "INSTANCE" || (node.parent && "type" in node.parent && node.parent.type === "BOOLEAN_OPERATION")) continue;
      if (node.type === "TEXT") {
        for (const segment of node.getStyledTextSegments(["fills"])) {
          const paints = await resolve(segment.fills, node);
          if (paints) node.setRangeFills(segment.start, segment.end, paints);
        }
      } else if ("fills" in node && Array.isArray(node.fills)) {
        const paints = await resolve(node.fills, node);
        if (paints) node.fills = paints;
      }
    }
  }

  private warn(at: string, message: string) {
    this.report.warn(`${this.label} › ${at}: ${message}`);
  }

  private async component(entry: Entry, parent: FrameNode, reuse: Kept, path: string, slot: string): Promise<FrameNode | undefined> {
    const [name, model] = Object.entries(entry)[0];
    const at = path ? `${path}/${name}` : name;
    const contract = this.tokens.component(name);
    if (!contract || !["Container", "Text", "Image", "Badge"].includes(name)) {
      this.warn(at, "компонент не поддерживается и не отрисован");
      return undefined;
    }
    const key = model.id ?? slot;
    const old = reuse.get(key);
    reuse.delete(key);
    if (model.indicators?.length) return this.indicated(name, model, parent, old, path, at, key);
    if (!this.library && name !== "Container") {
      const node = await this.instance(name, model, contract, parent, old, key, at);
      if (node) return node;
    }
    let frame: FrameNode;
    // Figma-компонент библиотеки — корень своего образца (раздел 6.2), в том числе `text/icon` с другим `ref`.
    if (old?.type === "COMPONENT" || (old?.type === "FRAME" && ref(old) === contract)) frame = old as FrameNode;
    else {
      if (old && !old.removed) old.remove();
      frame = figma.createFrame();
    }
    const kept = await this.reset(frame);
    parent.appendChild(frame);
    if (frame.type === "FRAME") frame.name = layerName(name, model.id);
    mark(frame, contract);
    mark(frame, model.id ?? "", "id");
    mark(frame, key, "slot");

    if (name === "Container") this.arrangement(frame, model, at);
    else {
      frame.layoutMode = "HORIZONTAL";
      frame.primaryAxisAlignItems = name === "Text" ? "MIN" : "CENTER";
      frame.counterAxisAlignItems = name === "Text" ? "MIN" : "CENTER";
    }
    this.layout(frame, model.layout, isAuto(parent), at);
    this.padding(frame, model.padding, at);
    const plain = this.plain(model);
    await this.appearance(frame, plain, at);

    if (name === "Container") await this.children(frame, model, kept, at);
    else if (name === "Text") await this.text(frame, model, kept, at);
    else if (name === "Image") await this.source(frame, model.source, model.layout, true, kept, at);
    else if (name === "Badge") await this.badge(frame, model, kept, at);
    if (plain.overlay) this.overlay(frame, plain, kept, at);
    for (const leftover of kept.values()) leftover.remove();
    this.collapseEmpty(frame, model.layout);
    return frame;
  }

  /**
   * Экземпляр Figma-компонента библиотеки (раздел 10.1, правило 1): вариант по значениям модели, значения — свойствами
   * и переопределениями слоёв, поля базовой модели — свойствами корня. Если компонента нет или модель требует того,
   * чего в нём нет, возвращает `undefined`, и рисуется тело.
   */
  private async instance(name: string, model: Base, contract: Id, parent: FrameNode, old: SceneNode | undefined, key: string, at: string): Promise<FrameNode | undefined> {
    const [id, values] = this.target(name, model, contract);
    const library = this.tokens.library(id);
    if (!library) {
      if (!this.tokens.missing.has(id)) {
        const figmaName = id.endsWith("#icon") ? "text/icon" : id.split("/").pop();
        this.warn(at, `нет Figma-компонента ${figmaName} на странице Components, все такие элементы нарисованы телом`);
      }
      this.tokens.missing.add(id);
      return undefined;
    }
    if (library.type === "COMPONENT_SET" && "state" in library.componentPropertyDefinitions) values.state = "default";
    const main = library.type === "COMPONENT" ? library : library.children.find((c) =>
      c.type === "COMPONENT" && Object.entries(values).every(([k, v]) => c.variantProperties?.[k] === v)) as ComponentNode | undefined;
    const plain = this.plain(model);
    const reason = !main ? `нет варианта ${Object.entries(values).map(([k, v]) => `${k}=${v}`).join(", ")}`
      : plain.overlay && !main.children.some((n) => ref(n, "slot") === "overlay") ? "наложение — нет слоя в компоненте"
      : name === "Badge" && model.content !== undefined && variant(model.content)[0] === "image" && variant(variant(model.content)[1])[0] === "remote" ? "картинка по адресу в бейдже"
      : undefined;
    if (!main || reason) {
      this.warn(at, `${reason}, нарисовано телом (раздел 10.1, правило 5)`);
      return undefined;
    }
    let node: InstanceNode;
    if (old?.type === "INSTANCE") {
      node = old;
      if ((await node.getMainComponentAsync())?.id !== main.id) node.swapComponent(main);
    } else node = main.createInstance();
    try {
      parent.appendChild(node);
      node.name = layerName(name, model.id);
      mark(node, contract);
      mark(node, model.id ?? "", "id");
      mark(node, key, "slot");
      const frame = node as unknown as FrameNode;
      await this.clear(frame);
      this.layout(frame, model.layout, isAuto(parent), at);
      this.padding(frame, model.padding, at);
      await this.appearance(frame, plain, at);
      const kept: Kept = new Map(node.children.filter((n) => ref(n, "slot")).map((n) => [ref(n, "slot"), n]));
      if (name === "Text") await this.text(frame, model, kept, at);
      else if (name === "Image") await this.source(frame, model.source, model.layout, true, kept, at);
      else if (name === "Badge") await this.badge(frame, model, kept, at);
      if (plain.overlay) this.overlay(frame, plain, kept, at);
      // Слои компонента, которых нет в модели, скрываются: менять состав экземпляра Figma не даёт.
      for (const leftover of kept.values()) leftover.visible = false;
      await this.fixProperties(node);
      this.collapseEmpty(frame, model.layout);
      if (old && old !== node && !old.removed) old.remove();
      return frame;
    } catch (e) {
      node.remove();
      this.warn(at, `экземпляр не построен, нарисовано телом: ${(e as Error).message}`);
      return undefined;
    }
  }

  /**
   * Instance swap- и boolean-свойства задаются экземпляру явно: значение слоя, совпавшее со значением компонента, иначе
   * не закрепляется и после правки образца следует за компонентом. Text-свойство не задаётся: оно сбрасывает стили
   * диапазонов (раздел 7, правило 9).
   */
  private async fixProperties(node: InstanceNode) {
    const values: { [key: string]: string | boolean } = {};
    for (const layer of node.children) {
      const references = layer.componentPropertyReferences ?? {};
      if (references.mainComponent && layer.type === "INSTANCE") {
        const main = await layer.getMainComponentAsync();
        if (main) values[references.mainComponent] = main.id;
      }
      if (references.visible) values[references.visible] = layer.visible;
    }
    if (Object.keys(values).length) node.setProperties(values);
  }

  /** Figma-компонент и значения его variant-свойств для модели (разделы 4.1, правила 12–13, и 10.5). */
  private target(name: string, model: Base, contract: Id): [Id, Record<string, string>] {
    if (name === "Image") return [contract, { source: variant(model.source)[0] }];
    if (name === "Badge") return [contract, { content: model.content === undefined ? "none" : variant(model.content)[0] }];
    const [kind, value] = variant<{ spans: Span[] }>(model.content);
    const spans = kind === "rich" ? value.spans : [];
    const edge = spans.length > 1 && ("icon" in spans[0] || "icon" in spans[spans.length - 1]);
    return [edge ? `${contract}#icon` : contract, {}];
  }

  /** Пустой фрейм Auto Layout в Figma не сжимается: при `hug` его размер — сумма отступов, как на платформах. */
  private collapseEmpty(frame: FrameNode, layout: Layout) {
    if (frame.layoutMode === "NONE" || frame.children.some((n) => !("layoutPositioning" in n) || n.layoutPositioning === "AUTO")) return;
    const width = layout.width.sizing === "hug" ? Math.max(frame.paddingLeft + frame.paddingRight, 0.01) : frame.width;
    const height = layout.height.sizing === "hug" ? Math.max(frame.paddingTop + frame.paddingBottom, 0.01) : frame.height;
    if (width === frame.width && height === frame.height) return;
    const sizing = [frame.layoutSizingHorizontal, frame.layoutSizingVertical] as const;
    frame.resize(width, height);
    // Растянутое измерение остаётся растянутым.
    if (sizing[0] === "FILL") frame.layoutSizingHorizontal = "FILL";
    if (sizing[1] === "FILL") frame.layoutSizingVertical = "FILL";
  }

  /** Сбрасывает свойства слоя; возвращает дочерние слои по месту в модели, слои без места удаляет. */
  private async reset(frame: FrameNode): Promise<Kept> {
    const kept: Kept = new Map();
    for (const child of [...frame.children]) {
      const slot = ref(child, "slot");
      if (slot && !kept.has(slot)) kept.set(slot, child);
      else child.remove();
    }
    await this.clear(frame);
    frame.layoutMode = "NONE";
    return kept;
  }

  /** Сбрасывает оформление и ограничения размера корня: значения задаёт модель. */
  private async clear(frame: FrameNode) {
    for (const field of BOUND_FIELDS) {
      if (frame.boundVariables?.[field as keyof typeof frame.boundVariables]) frame.setBoundVariable(field, null);
    }
    await frame.setEffectStyleIdAsync("");
    frame.fills = [];
    frame.strokes = [];
    frame.effects = [];
    frame.cornerRadius = 0;
    frame.clipsContent = false;
    if (frame.layoutMode !== "NONE") frame.minWidth = frame.maxWidth = frame.minHeight = frame.maxHeight = null;
  }

  /** Добавляет слой в корень; у экземпляра слои уже на месте, а менять их порядок Figma не даёт. */
  private attach(frame: FrameNode, node: SceneNode, first = false) {
    if ((frame as SceneNode).type === "INSTANCE") return;
    if (first) frame.insertChild(0, node);
    else frame.appendChild(node);
  }

  /** Слой с местом `slot`: прежний того же вида или новый (раздел 7, правило 8). */
  private take<T extends SceneNode>(kept: Kept, slot: string, type: T["type"], create: () => T): T {
    const old = kept.get(slot);
    kept.delete(slot);
    let node: T;
    if (old?.type === type) node = old as T;
    else {
      old?.remove();
      node = create();
    }
    mark(node, slot, "slot");
    node.visible = true;
    return node;
  }

  /** Число величины и привязка поля слоя к переменной варианта шкалы (раздел 10.1, правило 3). */
  private assign(node: SceneNode, field: VariableBindableNodeField, contract: Id, token: Token, at: string) {
    const value = this.tokens.number(contract, token);
    if (field === "width") (node as FrameNode).resize(value, (node as FrameNode).height);
    else if (field === "height") (node as FrameNode).resize((node as FrameNode).width, value);
    else (node as unknown as Record<string, number>)[field] = value;
    if (typeof token !== "string") return;
    const variable = this.tokens.variable(contract, token);
    if (variable) node.setBoundVariable(field, variable);
    else this.warn(at, `нет переменной ${contract.split("/").pop()}#${token}`);
  }

  private paint(color: string, at: string): SolidPaint {
    const paint: SolidPaint = { type: "SOLID", color: { r: 0.5, g: 0.5, b: 0.5 } };
    const variable = this.tokens.variable(COLOR, color);
    if (variable) return figma.variables.setBoundVariableForPaint(paint, "color", variable);
    this.warn(at, `нет переменной color/${color}`);
    return paint;
  }

  // Базовая модель (раздел 10.2).

  private layout(frame: FrameNode, layout: Layout, parentAuto: boolean, at: string) {
    const flow = layout.position === "flow";
    for (const [axis, dimension] of [["Horizontal", layout.width], ["Vertical", layout.height]] as const) {
      const sizing = `layoutSizing${axis}` as const;
      const [kind, token] = variant<Token>(dimension.sizing);
      if (kind === "fixed") {
        if (frame.layoutMode !== "NONE" || parentAuto) frame[sizing] = "FIXED";
        this.assign(frame, axis === "Horizontal" ? "width" : "height", SIZE, token, at);
      } else if (kind === "hug") {
        if (frame.layoutMode !== "NONE") frame[sizing] = "HUG";
      } else if (parentAuto && flow) {
        frame[sizing] = "FILL";
      }
      for (const bound of ["min", "max"] as const) {
        const value = dimension[bound];
        if (value === undefined) continue;
        const field = `${bound}${axis === "Horizontal" ? "Width" : "Height"}` as const;
        try {
          this.assign(frame, field, SIZE, value, at);
        } catch (e) {
          this.warn(at, `${field} не задан: ${(e as Error).message}`);
        }
      }
    }
    if (parentAuto) frame.layoutPositioning = flow ? "AUTO" : "ABSOLUTE";
    if (typeof layout.position === "object") {
      const pinned = layout.position.pinned;
      this.placements.push(() => this.pin(frame, pinned));
    }
  }

  /** Координаты закреплённого слоя по краям родителя (раздел 10.2, правило 1). */
  private pin(frame: FrameNode, pinned: Pinned) {
    const parent = frame.parent as FrameNode;
    const content = pinned.relativeTo === "content";
    // У родителя overlay (без Auto Layout) отступы не в полях фрейма, а в метке `padding` (раздел 10.2).
    const marked = parent.layoutMode === "NONE" ? JSON.parse(ref(parent, "padding") || "{}") as Record<string, number> : undefined;
    const pad = !content ? { start: 0, end: 0, top: 0, bottom: 0 }
      : marked ? { start: marked.start ?? 0, end: marked.end ?? 0, top: marked.top ?? 0, bottom: marked.bottom ?? 0 }
      : { start: parent.paddingLeft, end: parent.paddingRight, top: parent.paddingTop, bottom: parent.paddingBottom };
    const axis = (pin: Pin, size: number, length: number, near: "start" | "top", far: "end" | "bottom") => {
      const [kind, value] = variant<Record<string, Token>>(pin);
      const offset = (token: Token) => this.tokens.number(SPACING, token);
      if (kind === "stretch") {
        const start = pad[near] + offset(value[near]);
        return { position: start, length: Math.max(size - start - pad[far] - offset(value[far]), 0.01), constraint: "STRETCH" as const };
      }
      const shift = offset(value.offset);
      if (kind === near) return { position: pad[near] + shift, length, constraint: "MIN" as const };
      if (kind === far) return { position: size - pad[far] - shift - length, length, constraint: "MAX" as const };
      return { position: (size - length) / 2 + shift, length, constraint: "CENTER" as const };
    };
    const h = axis(pinned.horizontal, parent.width, frame.width, "start", "end");
    const v = axis(pinned.vertical, parent.height, frame.height, "top", "bottom");
    if (h.constraint === "STRETCH" || v.constraint === "STRETCH") frame.resize(h.length, v.length);
    frame.x = h.position;
    frame.y = v.position;
    frame.constraints = { horizontal: h.constraint, vertical: v.constraint };
  }

  private padding(frame: FrameNode, padding: Record<string, unknown>, at: string) {
    const [kind, value] = variant<Record<string, Token> | Token>(padding);
    const sides: Record<string, Token> =
      kind === "all" ? { start: value as Token, top: value as Token, end: value as Token, bottom: value as Token }
      : kind === "symmetric" ? (({ horizontal, vertical }) => ({ start: horizontal, end: horizontal, top: vertical, bottom: vertical }))(value as Record<string, Token>)
      : value as Record<string, Token>;
    if (frame.layoutMode === "NONE") {
      // У фрейма без Auto Layout поля не действуют: их учитывает раскладка overlay.
      mark(frame, JSON.stringify(Object.fromEntries(Object.entries(sides).map(([side, token]) => [side, this.tokens.number(SPACING, token)]))), "padding");
      return;
    }
    this.assign(frame, "paddingLeft", SPACING, sides.start, at);
    this.assign(frame, "paddingRight", SPACING, sides.end, at);
    this.assign(frame, "paddingTop", SPACING, sides.top, at);
    this.assign(frame, "paddingBottom", SPACING, sides.bottom, at);
  }

  /** Оформление показанного состояния: `default`, для `selectable` — по `selected` (раздел 10.1, правило 2). */
  private plain(model: Base): Plain {
    const [kind, value] = variant<Record<string, Record<string, Plain>>>(model.appearance);
    if (kind === "plain") return value as unknown as Plain;
    if (kind === "selectable") return (model.selected ? value.selected : value.unselected).default;
    return value.default as unknown as Plain;
  }

  private async appearance(frame: FrameNode, plain: Plain, at: string) {
    if (plain.background) frame.fills = [this.paint(plain.background.solid, at)];
    if (plain.shadow) {
      const [kind, value] = variant<ElevationStyle>(plain.shadow);
      if (kind === "custom") {
        frame.effects = [{
          type: "DROP_SHADOW", visible: true, blendMode: "NORMAL", showShadowBehindNode: false, color: rgba(value.color),
          offset: { x: value.offsetX, y: value.offsetY }, radius: value.blur, spread: value.spread ?? 0,
        }];
      } else {
        const style = this.tokens.effectStyle(kind);
        if (style) await frame.setEffectStyleIdAsync(style.id);
        else this.warn(at, `нет стиля elevation/${kind}`);
      }
    }
    if (plain.cornerRadius) this.radius(frame, plain.cornerRadius, at);
    if (plain.stroke) {
      frame.strokes = [this.paint(plain.stroke.color, at)];
      frame.strokeAlign = "INSIDE";
      this.assign(frame, "strokeWeight", BORDER_WIDTH, plain.stroke.width, at);
    }
    frame.clipsContent = plain.clip === true;
  }

  private radius(node: FrameNode | RectangleNode, token: string, at: string) {
    for (const field of ["topLeftRadius", "topRightRadius", "bottomLeftRadius", "bottomRightRadius"] as const) {
      this.assign(node, field, CORNER_RADIUS, token, at);
    }
  }

  /** Наложение — верхний слой корня поверх детей (раздел 10.2, правило 3). */
  private overlay(frame: FrameNode, plain: Plain, kept: Kept, at: string) {
    const layer = this.take(kept, "overlay", "RECTANGLE", () => figma.createRectangle());
    this.attach(frame, layer);
    layer.name = "overlay";
    mark(layer, "overlay", "role");
    if (frame.layoutMode !== "NONE") layer.layoutPositioning = "ABSOLUTE";
    layer.fills = [this.paint(plain.overlay!.solid, at)];
    if (plain.cornerRadius) this.radius(layer, plain.cornerRadius, at);
    this.placements.push(() => {
      layer.x = 0;
      layer.y = 0;
      layer.resize(Math.max(frame.width, 0.01), Math.max(frame.height, 0.01));
      layer.constraints = { horizontal: "STRETCH", vertical: "STRETCH" };
    });
  }

  /**
   * Носитель и индикаторы во фрейме-обёртке с Auto Layout: обёртка повторяет размер носителя и занимает его место у
   * родителя, индикаторы и маска выреза лежат в ней с абсолютной позицией. Носитель — во фрейме `carrier` без
   * обрезки: маска Figma не режет фрейм с `clipsContent` (раздел 10.2, правило 4).
   */
  private async indicated(name: string, model: Base, parent: FrameNode, old: SceneNode | undefined, path: string, at: string, key: string): Promise<FrameNode | undefined> {
    let wrapper: FrameNode;
    if (old?.type === "FRAME" && ref(old, "role") === "indicators") wrapper = old;
    else {
      // Носитель, нарисованный раньше без индикаторов, переиспользуется внутри обёртки.
      wrapper = figma.createFrame();
      if (old) {
        const carrier = figma.createFrame();
        wrapper.appendChild(carrier);
        mark(carrier, "carrier", "slot");
        carrier.appendChild(old);
      }
    }
    const kept = await this.reset(wrapper);
    parent.appendChild(wrapper);
    wrapper.name = `${name} indicators`;
    mark(wrapper, "indicators", "role");
    mark(wrapper, model.id ?? "", "id");
    mark(wrapper, key, "slot");
    const flow: Layout = { ...model.layout, position: "flow" };
    const slot = this.take(kept, "carrier", "FRAME", () => figma.createFrame());
    const carrierKept = await this.reset(slot);
    wrapper.appendChild(slot);
    slot.name = "carrier";
    mark(slot, "carrier", "role");
    for (const [frame, layout, parentAuto] of [[wrapper, model.layout, isAuto(parent)], [slot, flow, true]] as const) {
      frame.layoutMode = "HORIZONTAL";
      frame.itemSpacing = 0;
      frame.paddingLeft = frame.paddingRight = frame.paddingTop = frame.paddingBottom = 0;
      this.layout(frame, layout, parentAuto, at);
    }
    const carrier = await this.component({ [name]: { ...model, indicators: undefined, layout: flow } }, slot, carrierKept, path, key);
    for (const leftover of carrierKept.values()) leftover.remove();
    const placed: [FrameNode, Indicator][] = [];
    for (const [index, indicator] of (model.indicators as Indicator[]).entries()) {
      const node = await this.component(indicator.content, wrapper, kept, `${at}.indicators[${index}]`, `indicators[${index}]`);
      if (!node) continue;
      node.layoutPositioning = "ABSOLUTE";
      placed.push([node, indicator]);
    }
    for (const leftover of kept.values()) leftover.remove();
    if (carrier) this.placements.push(() => this.cutout(wrapper, carrier, placed, at));
    return wrapper;
  }

  /**
   * Центры индикаторов — в точках углов носителя, маска — прямоугольник с запасом минус формы индикаторов, расширенные
   * на `gap`. Маска режет слои над собой: индикаторы с вырезом лежат под ней и видны сквозь вырез, без выреза — над
   * носителем.
   */
  private cutout(wrapper: FrameNode, carrier: FrameNode, placed: [FrameNode, Indicator][], at: string) {
    const cuts: { x: number; y: number; width: number; height: number; radii: number[]; constraints: Constraints }[] = [];
    const below: FrameNode[] = [];
    for (const [node, { anchor, gap }] of placed) {
      const end = anchor.endsWith("End");
      const bottom = anchor.startsWith("bottom");
      const corner = `${bottom ? "bottom" : "top"}${end ? "Right" : "Left"}Radius` as const;
      // Середина дуги скругления под 45°; у прямого угла — вершина.
      const inset = Math.min(carrier[corner], carrier.width / 2, carrier.height / 2) * (1 - Math.SQRT1_2);
      node.x = (end ? carrier.width - inset : inset) - node.width / 2;
      node.y = (bottom ? carrier.height - inset : inset) - node.height / 2;
      node.constraints = { horizontal: end ? "MAX" : "MIN", vertical: bottom ? "MAX" : "MIN" };
      if (gap === undefined) {
        wrapper.appendChild(node);
        continue;
      }
      const g = this.tokens.number(SPACING, gap);
      const radii = [node.topLeftRadius, node.topRightRadius, node.bottomRightRadius, node.bottomLeftRadius].map((r) => r + g);
      cuts.push({ x: node.x - g, y: node.y - g, width: node.width + 2 * g, height: node.height + 2 * g, radii, constraints: node.constraints });
      wrapper.insertChild(below.length, node);
      below.push(node);
    }
    if (!cuts.length) return;
    // Запас маски — всё, что рисуют носитель и индикаторы, включая тень.
    const [ox, oy] = [wrapper.absoluteTransform[0][2], wrapper.absoluteTransform[1][2]];
    const boxes = [carrier, ...placed.map(([node]) => node)].map((n) => n.absoluteRenderBounds ?? n.absoluteBoundingBox).filter((b): b is Rect => b !== null);
    const left = Math.min(...boxes.map((b) => b.x)) - ox - 1;
    const top = Math.min(...boxes.map((b) => b.y)) - oy - 1;
    const right = Math.max(...boxes.map((b) => b.x + b.width)) - ox + 1;
    const bottom = Math.max(...boxes.map((b) => b.y + b.height)) - oy + 1;
    const shapes: RectangleNode[] = [];
    try {
      const shape = (x: number, y: number, width: number, height: number) => {
        const rect = figma.createRectangle();
        shapes.push(rect);
        rect.x = x;
        rect.y = y;
        rect.resize(Math.max(width, 0.01), Math.max(height, 0.01));
        rect.fills = [{ type: "SOLID", color: { r: 0, g: 0, b: 0 } }];
        return rect;
      };
      const base = shape(left, top, right - left, bottom - top);
      base.name = "area";
      base.constraints = { horizontal: "STRETCH", vertical: "STRETCH" };
      for (const cut of cuts) {
        const rect = shape(cut.x, cut.y, cut.width, cut.height);
        rect.name = "cut";
        [rect.topLeftRadius, rect.topRightRadius, rect.bottomRightRadius, rect.bottomLeftRadius] = cut.radii;
        // Вырез переезжает вместе с углом, как индикатор.
        rect.constraints = cut.constraints;
      }
      // Маска — фрейм с булевой операцией: у булевой операции нет констрейнтов, а фрейм растягивается вместе с
      // обёрткой, и вырезы внутри едут за своими углами.
      const mask = figma.createFrame();
      wrapper.insertChild(below.length, mask);
      mask.layoutPositioning = "ABSOLUTE";
      mask.name = "cutout";
      mask.fills = [];
      mask.clipsContent = false;
      mask.x = left;
      mask.y = top;
      mask.resize(right - left, bottom - top);
      for (const rect of shapes) {
        mask.appendChild(rect);
        rect.x -= left;
        rect.y -= top;
      }
      figma.subtract(shapes, mask).name = "shape";
      mask.constraints = { horizontal: "STRETCH", vertical: "STRETCH" };
      mask.isMask = true;
    } catch (e) {
      for (const rect of shapes) if (!rect.removed) rect.remove();
      wrapper.findChild((n) => n.name === "cutout")?.remove();
      for (const node of below) wrapper.appendChild(node);
      this.warn(at, `вырез не построен, индикаторы нарисованы поверх носителя: ${(e as Error).message}`);
    }
  }

  // Контейнер (раздел 10.3).

  private arrangement(frame: FrameNode, model: Base, at: string) {
    const [kind, value] = variant<Record<string, unknown>>(model.arrangement);
    if (kind === "overlay") return;
    frame.layoutMode = kind.startsWith("horizontal") ? "HORIZONTAL" : "VERTICAL";
    const wrap = kind.endsWith("Wrap");
    frame.layoutWrap = "NO_WRAP";
    if (wrap) {
      try {
        frame.layoutWrap = "WRAP";
      } catch (e) {
        this.warn(at, `перенос строк не поддерживается Figma, контейнер без переноса: ${(e as Error).message}`);
      }
    }
    const alignment = value.alignment as string;
    if (alignment === "baseline") {
      try {
        frame.counterAxisAlignItems = "BASELINE";
        if (frame.counterAxisAlignItems !== "BASELINE") throw new Error("значение не сохранилось");
      } catch (e) {
        frame.counterAxisAlignItems = "MIN";
        this.warn(at, `выравнивание по базовой линии заменено на MIN: ${(e as Error).message}`);
      }
    } else {
      frame.counterAxisAlignItems = ALIGN[alignment];
    }
    const [distribution, packed] = variant<{ gap: Token; position: string }>(value.distribution);
    if (distribution === "packed") {
      this.assign(frame, "itemSpacing", SPACING, packed.gap, at);
      frame.primaryAxisAlignItems = ALIGN[packed.position];
    } else {
      frame.itemSpacing = 0;
      frame.primaryAxisAlignItems = "SPACE_BETWEEN";
    }
    if (wrap && frame.layoutWrap === "WRAP") {
      const [lines, linePacked] = variant<{ gap: Token }>(value.lineDistribution);
      if (lines === "packed") {
        frame.counterAxisAlignContent = "AUTO";
        this.assign(frame, "counterAxisSpacing", SPACING, linePacked.gap, at);
      } else {
        frame.counterAxisAlignContent = "SPACE_BETWEEN";
      }
    }
  }

  private async children(frame: FrameNode, model: Base, kept: Kept, at: string) {
    const children = (model.children as Entry[] | undefined) ?? [];
    const drawn: [FrameNode, Base][] = [];
    for (const [index, child] of children.entries()) {
      const node = await this.component(child, frame, kept, `${at}.children[${index}]`, `children[${index}]`);
      if (node) drawn.push([node, Object.values(child)[0]]);
    }
    const [kind, value] = variant<{ horizontal: string; vertical: string }>(model.arrangement);
    if (kind === "overlay") this.layouts.push(() => this.overlayLayout(frame, model.layout, value, drawn));
  }

  /** Контейнер overlay: размеры по детям и координаты детей по выравниванию (раздел 10.3). */
  private overlayLayout(frame: FrameNode, layout: Layout, alignment: { horizontal: string; vertical: string }, drawn: [FrameNode, Base][]) {
    const pad = JSON.parse(ref(frame, "padding") || "{}") as Record<string, number>;
    const flow = drawn.filter(([, child]) => child.layout.position === "flow");
    const hug = (dimension: Dimension) => dimension.sizing === "hug";
    const span = (pick: (node: FrameNode) => number, dimension: (child: Base) => Dimension) =>
      Math.max(0.01, ...flow.filter(([, child]) => dimension(child).sizing !== "fill").map(([node]) => pick(node)));
    const width = hug(layout.width) ? span((n) => n.width, (c) => c.layout.width) + (pad.start ?? 0) + (pad.end ?? 0) : frame.width;
    const height = hug(layout.height) ? span((n) => n.height, (c) => c.layout.height) + (pad.top ?? 0) + (pad.bottom ?? 0) : frame.height;
    if (hug(layout.width) || hug(layout.height)) {
      frame.resize(width, height);
      // Изменение размера фиксирует оба измерения: растянутое возвращается к FILL.
      if (frame.parent && "layoutMode" in frame.parent && frame.parent.layoutMode !== "NONE" && layout.position === "flow") {
        if (layout.width.sizing === "fill") frame.layoutSizingHorizontal = "FILL";
        if (layout.height.sizing === "fill") frame.layoutSizingVertical = "FILL";
      }
    }
    for (const [node, child] of flow) {
      const innerWidth = width - (pad.start ?? 0) - (pad.end ?? 0);
      const innerHeight = height - (pad.top ?? 0) - (pad.bottom ?? 0);
      const fillWidth = child.layout.width.sizing === "fill";
      const fillHeight = child.layout.height.sizing === "fill";
      if (fillWidth || fillHeight) {
        node.resize(fillWidth ? Math.max(innerWidth, 0.01) : node.width, fillHeight ? Math.max(innerHeight, 0.01) : node.height);
        if (node.layoutMode !== "NONE") {
          if (child.layout.width.sizing === "hug") node.layoutSizingHorizontal = "HUG";
          if (child.layout.height.sizing === "hug") node.layoutSizingVertical = "HUG";
        }
      }
      const h = fillWidth ? "STRETCH" : ALIGN[alignment.horizontal];
      const v = fillHeight ? "STRETCH" : ALIGN[alignment.vertical];
      const place = (constraint: string, start: number, inner: number, length: number) =>
        constraint === "MAX" ? start + inner - length : constraint === "CENTER" ? start + (inner - length) / 2 : start;
      node.x = place(h, pad.start ?? 0, innerWidth, node.width);
      node.y = place(v, pad.top ?? 0, innerHeight, node.height);
      node.constraints = { horizontal: h, vertical: v };
    }
  }

  // Текст (разделы 4.1, правило 11, и 10.5).

  private async loadFont(font: FontName) {
    const key = `${font.family}/${font.style}`;
    if (this.fonts.has(key)) return;
    await figma.loadFontAsync(font);
    this.fonts.add(key);
  }

  /** Текстовый слой со стилем роли и цветом. Шрифт стиля должен быть загружен до изменения текста. */
  private async textNode(kept: Kept, slot: string, characters: string, role: string, color: string, at: string): Promise<TextNode> {
    const node = this.take(kept, slot, "TEXT", () => figma.createText());
    await this.loadFont(DEFAULT_FONT);
    // Прежний слой: шрифты всех фрагментов нужны до замены текста, обрезка и перенос сбрасываются.
    if (node.characters.length) for (const font of node.getRangeAllFontNames(0, node.characters.length)) await this.loadFont(font);
    node.textTruncation = "DISABLED";
    node.maxLines = null;
    node.maxWidth = null;
    node.textDecoration = "NONE";
    const style = this.tokens.textStyle(role);
    if (style) await this.loadFont(style.fontName);
    node.characters = characters;
    if (style) await node.setTextStyleIdAsync(style.id);
    else this.warn(at, `нет стиля typography/${role}`);
    node.fills = [this.paint(color, at)];
    return node;
  }

  /** Экземпляр символа с цветом вектора (раздел 4.1, правило 12); прежний экземпляр меняет символ на месте. */
  private async icon(kept: Kept, slot: string, name: string, color: string, at: string): Promise<InstanceNode | undefined> {
    const component = this.tokens.icon(name);
    if (!component) {
      kept.get(slot)?.remove();
      kept.delete(slot);
      this.warn(at, `нет символа ${prefix("Icon")}/${name}`);
      return undefined;
    }
    const instance = this.take(kept, slot, "INSTANCE", () => component.createInstance());
    if ((await instance.getMainComponentAsync())?.id !== component.id) instance.swapComponent(component);
    instance.resize(component.width, component.height);
    const vector = instance.findOne((n) => n.name === "vector");
    if (vector && "fills" in vector) vector.fills = [this.paint(color, at)];
    return instance;
  }

  private async text(frame: FrameNode, model: Base, kept: Kept, at: string) {
    const typography = model.typography as string;
    const color = (model.color as Fill).solid;
    const [kind, value] = variant<string | { spans: Span[] }>(model.content);
    const spans: Span[] = kind === "plain" ? [{ text: { value: value as string } }] : (value as { spans: Span[] }).spans;
    let leading: Span | undefined;
    let trailing: Span | undefined;
    const parts: { value: string; span?: Span }[] = [];
    for (const [index, span] of spans.entries()) {
      if ("icon" in span) {
        if (index === 0) leading = span;
        else if (index === spans.length - 1) trailing = span;
        else {
          parts.push({ value: " " });
          this.warn(at, `символ ${span.icon.name} в середине текста заменён пробелом (раздел 10.5)`);
        }
      } else {
        parts.push({ value: span.text.value, span });
      }
    }
    const node = await this.textNode(kept, "text", parts.map((p) => p.value).join(""), typography, color, at);
    this.attach(frame, node);
    node.name = "text";
    let start = 0;
    for (const part of parts) {
      const end = start + part.value.length;
      const span = part.span && "text" in part.span ? part.span.text : undefined;
      if (span && end > start) {
        if (span.typography) {
          const style = this.tokens.textStyle(span.typography);
          if (style) {
            await this.loadFont(style.fontName);
            await node.setRangeTextStyleIdAsync(start, end, style.id);
          } else this.warn(at, `нет стиля typography/${span.typography}`);
        }
        if (span.color) node.setRangeFills(start, end, [this.paint(span.color, at)]);
        if (span.decoration) node.setRangeTextDecoration(start, end, DECORATION[span.decoration]);
      }
      start = end;
    }
    if (model.decoration) node.textDecoration = DECORATION[model.decoration as string];
    node.textAlignHorizontal = TEXT_ALIGN[model.alignment as string];
    if (model.layout.width.sizing === "hug" && frame.minWidth === null && frame.maxWidth === null) {
      node.textAutoResize = "WIDTH_AND_HEIGHT";
      node.layoutSizingHorizontal = "HUG";
    } else {
      // У корня `hug` с `min` или `max` текст растягивается с переносом: корень обнимает текст в пределах min и max, как
      // на платформах. maxWidth самого текста в экземпляре библиотеки Figma переопределить не даёт (раздел 10.2, правило 6).
      node.textAutoResize = "HEIGHT";
      node.layoutSizingHorizontal = "FILL";
    }
    // Обрезка — после режима размера: смена textAutoResize сбрасывает её.
    if (model.maxLines !== undefined) {
      node.textTruncation = "ENDING";
      node.maxLines = model.maxLines as number;
      if (model.truncation !== undefined && model.truncation !== "end") {
        this.warn(at, `обрезка «${model.truncation}» показана многоточием в конце: Figma поддерживает только его`);
      }
    }
    // Символы по краям: строка из трёх слоёв, размер символа — кегль роли (раздел 10.5, правило 2).
    for (const [span, index] of [[leading, 0], [trailing, -1]] as const) {
      if (!span || !("icon" in span)) continue;
      const role = span.icon.typography ?? typography;
      const instance = await this.icon(kept, index === 0 ? "leadingIcon" : "trailingIcon", span.icon.name, span.icon.color ?? color, at);
      if (!instance) continue;
      this.attach(frame, instance, index === 0);
      const size = this.tokens.textStyle(role)?.fontSize ?? instance.height;
      instance.resize(size, size);
      const variable = this.tokens.variable(TYPOGRAPHY, `${role}/fontSize`);
      if (variable) {
        instance.setBoundVariable("width", variable);
        instance.setBoundVariable("height", variable);
      } else this.warn(at, `нет переменной typography/${role}/fontSize`);
    }
  }

  // Изображение (раздел 10.4).

  /** Источник изображения: символ или картинка по адресу. `fit` — вписывать символ в размер слоя (правило 2). */
  private async source(frame: FrameNode, source: unknown, layout: Layout, fit: boolean, kept: Kept, at: string) {
    const [kind, value] = variant<Resource | Remote>(source);
    if (kind === "resource") {
      await this.resource(frame, value as Resource, fit ? layout : undefined, kept, at);
      return;
    }
    const remote = value as Remote;
    if (this.library) {
      // В библиотеке вместо картинки по адресу — нейтральная заливка (раздел 4.1, правило 12).
      const layer = this.take(kept, "image", "RECTANGLE", () => figma.createRectangle());
      this.attach(frame, layer);
      layer.name = "image";
      if (layout.width.sizing !== "hug") layer.layoutSizingHorizontal = "FILL";
      if (layout.height.sizing !== "hug") layer.layoutSizingVertical = "FILL";
      layer.fills = [this.paint("surface2", at)];
      return;
    }
    let image: Image;
    let size: { width: number; height: number };
    try {
      image = await figma.createImageAsync(remote.origin.url);
      size = await image.getSizeAsync();
    } catch (e) {
      if ((frame as SceneNode).type === "INSTANCE") {
        // В экземпляре варианта remote нет слоя символа: остаётся заглушка компонента.
        kept.delete("image");
        this.warn(at, `картинка ${remote.origin.url} не загрузилась, показана заглушка: ${(e as Error).message}`);
        return;
      }
      const fallback = remote.error ?? remote.placeholder;
      this.warn(at, `картинка ${remote.origin.url} не загрузилась${fallback ? `, показан символ ${fallback.icon.name}` : ""}: ${(e as Error).message}`);
      if (fallback) await this.resource(frame, fallback, undefined, kept, at);
      return;
    }
    const layer = this.take(kept, "image", "RECTANGLE", () => figma.createRectangle());
    this.attach(frame, layer);
    layer.name = "image";
    layer.resize(size.width, size.height);
    // У оси `hug` с `min` или `max` слой растягивается: корень обнимает собственный размер картинки в пределах min и max,
    // и картинка вписывается в него по `scale`, как на платформах (правило 3).
    if (layout.width.sizing !== "hug" || frame.minWidth !== null || frame.maxWidth !== null) layer.layoutSizingHorizontal = "FILL";
    if (layout.height.sizing !== "hug" || frame.minHeight !== null || frame.maxHeight !== null) layer.layoutSizingVertical = "FILL";
    layer.fills = [{ type: "IMAGE", imageHash: image.hash, scaleMode: remote.scale === "fit" ? "FIT" : "FILL" }];
    if (remote.alignment !== "center") this.placements.push(() => this.alignImage(layer, image.hash, size, remote));
  }

  private async resource(frame: FrameNode, resource: Resource, fitTo: Layout | undefined, kept: Kept, at: string) {
    const instance = await this.icon(kept, "icon", resource.icon.name, resource.icon.color, at);
    if (!instance) return;
    this.attach(frame, instance);
    if (fitTo && (fitTo.width.sizing !== "hug" || fitTo.height.sizing !== "hug")) {
      this.placements.push(() => {
        const scales = [
          fitTo.width.sizing !== "hug" ? (frame.width - frame.paddingLeft - frame.paddingRight) / instance.width : Infinity,
          fitTo.height.sizing !== "hug" ? (frame.height - frame.paddingTop - frame.paddingBottom) / instance.height : Infinity,
        ];
        const scale = Math.max(Math.min(...scales), 0.01);
        instance.resize(instance.width * scale, instance.height * scale);
      });
    }
  }

  /** Смещение картинки по `alignment` через `imageTransform`: видимая доля картинки и её сдвиг. */
  private alignImage(layer: RectangleNode, hash: string, size: { width: number; height: number }, remote: Remote) {
    const fit = Math.min(layer.width / size.width, layer.height / size.height);
    const crop = Math.max(layer.width / size.width, layer.height / size.height);
    const scale = remote.scale === "fit" ? fit : crop;
    const fractionX = layer.width / (size.width * scale);
    const fractionY = layer.height / (size.height * scale);
    const [ax, ay] = IMAGE_ALIGN[remote.alignment];
    layer.fills = [{
      type: "IMAGE", imageHash: hash, scaleMode: "CROP",
      imageTransform: [[fractionX, 0, (1 - fractionX) * ax], [0, fractionY, (1 - fractionY) * ay]],
    }];
  }

  // Бейдж (раздел 4.1, правило 13).

  private async badge(frame: FrameNode, model: Base, kept: Kept, at: string) {
    if (model.content) {
      const [kind, value] = variant<{ value: string; typography: string; color: string }>(model.content);
      if (kind === "text") {
        const node = await this.textNode(kept, "text", value.value, value.typography, value.color, at);
        this.attach(frame, node);
        node.name = "text";
        node.textAutoResize = "WIDTH_AND_HEIGHT";
      } else {
        await this.source(frame, value, model.layout, false, kept, at);
      }
    }
    // Ширина не меньше высоты.
    if (model.layout.width.min !== undefined) return;
    const [kind, token] = variant<Token>(model.layout.height.sizing);
    if (kind === "fixed") this.assign(frame, "minWidth", SIZE, token, at);
    else this.placements.push(() => { frame.minWidth = frame.height; });
  }
}
