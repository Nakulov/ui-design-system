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
  ) {}

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
    return new Tokens(
      bundle, variables,
      byRef(await figma.getLocalTextStylesAsync()),
      byRef(await figma.getLocalEffectStylesAsync()),
      byRef(iconPage?.findAllWithCriteria({ types: ["COMPONENT"] }) ?? []),
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

  /** Число величины: вариант шкалы — из первого режима темы, `custom` — его значение. */
  number(contract: Id, token: Token): number {
    if (typeof token !== "string") return token.custom.value;
    return this.bundle.values[contract].modes[0].values[token] as number;
  }

  /** Идентификатор публичного контракта компонента по имени. */
  component(name: string): Id | undefined {
    return Object.keys(this.bundle.contracts).find((id) => id.startsWith("schemas/components/") && this.bundle.contracts[id].name === name);
  }
}

/**
 * Отрисовка одной модели. Слои строятся сразу; раскладка `overlay`, координаты `pinned`, слой наложения и вписывание
 * картинок требуют готовых размеров и выполняются в `finish`.
 */
export class Renderer {
  private readonly layouts: (() => void)[] = [];
  private readonly placements: (() => void)[] = [];
  private readonly fonts = new Set<string>();

  constructor(private readonly tokens: Tokens, private readonly report: Report, private readonly label: string) {}

  /** Рисует модель последним ребёнком `parent`; слои из `reuse` с тем же `id` обновляются на месте (§10.1, правило 4). */
  async draw(entry: unknown, parent: FrameNode, reuse: Map<string, SceneNode>): Promise<SceneNode | undefined> {
    const node = await this.component(entry as Entry, parent, reuse, "");
    // Размеры вложенных контейнеров overlay нужны родителям, а размеры родителей — растянутым детям.
    for (const layout of [...this.layouts].reverse()) layout();
    for (const layout of this.layouts) layout();
    for (const place of this.placements) place();
    return node;
  }

  private warn(at: string, message: string) {
    this.report.warn(`${this.label} › ${at}: ${message}`);
  }

  private async component(entry: Entry, parent: FrameNode, reuse: Map<string, SceneNode>, path: string): Promise<FrameNode | undefined> {
    const [name, model] = Object.entries(entry)[0];
    const at = path ? `${path}/${name}` : name;
    const contract = this.tokens.component(name);
    if (!contract || !["Container", "Text", "Image", "Badge"].includes(name)) {
      this.warn(at, "компонент не поддерживается и не отрисован");
      return undefined;
    }
    const old = model.id ? reuse.get(model.id) : undefined;
    if (model.id) reuse.delete(model.id);
    let frame: FrameNode;
    let kept = new Map<string, SceneNode>();
    if (old?.type === "FRAME" && ref(old) === contract) {
      frame = old;
      kept = await this.reset(frame);
    } else {
      old?.remove();
      frame = figma.createFrame();
      await this.reset(frame);
    }
    parent.appendChild(frame);
    frame.name = name;
    mark(frame, contract);
    mark(frame, model.id ?? "", "id");

    if (name === "Container") this.arrangement(frame, model, at);
    else {
      frame.layoutMode = "HORIZONTAL";
      frame.primaryAxisAlignItems = name === "Text" ? "MIN" : "CENTER";
      frame.counterAxisAlignItems = name === "Text" ? "MIN" : "CENTER";
    }
    this.layout(frame, model.layout, parent.layoutMode !== "NONE", at);
    this.padding(frame, model.padding, at);
    const plain = this.plain(model);
    await this.appearance(frame, plain, at);

    if (name === "Container") await this.children(frame, model, kept, at);
    else if (name === "Text") await this.text(frame, model, at);
    else if (name === "Image") await this.source(frame, model.source, model.layout, true, at);
    else if (name === "Badge") await this.badge(frame, model, at);
    for (const leftover of kept.values()) leftover.remove();
    this.collapseEmpty(frame, model.layout);

    if (plain.overlay) this.overlay(frame, plain, at);
    if (model.indicators) this.warn(at, "индикаторы пока не поддерживаются и не отрисованы (#54)");
    return frame;
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

  /** Сбрасывает свойства слоя; возвращает дочерние слои моделей с `id`, остальные удаляет. */
  private async reset(frame: FrameNode): Promise<Map<string, SceneNode>> {
    const kept = new Map<string, SceneNode>();
    for (const child of [...frame.children]) {
      const id = ref(child, "id");
      if (id && !ref(child, "role")) kept.set(id, child);
      else child.remove();
    }
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
    frame.layoutMode = "NONE";
    return kept;
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
    const pad = content
      ? { start: parent.paddingLeft, end: parent.paddingRight, top: parent.paddingTop, bottom: parent.paddingBottom }
      : { start: 0, end: 0, top: 0, bottom: 0 };
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
  private overlay(frame: FrameNode, plain: Plain, at: string) {
    const layer = figma.createRectangle();
    frame.appendChild(layer);
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

  private async children(frame: FrameNode, model: Base, kept: Map<string, SceneNode>, at: string) {
    const children = (model.children as Entry[] | undefined) ?? [];
    const drawn: [FrameNode, Base][] = [];
    for (const [index, child] of children.entries()) {
      const node = await this.component(child, frame, kept, `${at}.children[${index}]`);
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
  private async textNode(characters: string, role: string, color: string, at: string): Promise<TextNode> {
    const node = figma.createText();
    await this.loadFont(DEFAULT_FONT);
    const style = this.tokens.textStyle(role);
    if (style) await this.loadFont(style.fontName);
    node.characters = characters;
    if (style) await node.setTextStyleIdAsync(style.id);
    else this.warn(at, `нет стиля typography/${role}`);
    node.fills = [this.paint(color, at)];
    return node;
  }

  /** Экземпляр символа с цветом вектора (раздел 4.1, правило 12). */
  private icon(name: string, color: string, at: string): InstanceNode | undefined {
    const component = this.tokens.icon(name);
    if (!component) {
      this.warn(at, `нет символа ${prefix("Icon")}/${name}`);
      return undefined;
    }
    const instance = component.createInstance();
    const vector = instance.findOne((n) => n.name === "vector");
    if (vector && "fills" in vector) vector.fills = [this.paint(color, at)];
    return instance;
  }

  private async text(frame: FrameNode, model: Base, at: string) {
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
    const node = await this.textNode(parts.map((p) => p.value).join(""), typography, color, at);
    frame.appendChild(node);
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
    if (model.maxLines !== undefined) {
      node.textTruncation = "ENDING";
      node.maxLines = model.maxLines as number;
      if (model.truncation !== undefined && model.truncation !== "end") {
        this.warn(at, `обрезка «${model.truncation}» показана многоточием в конце: Figma поддерживает только его`);
      }
    }
    if (model.layout.width.sizing === "hug") node.textAutoResize = "WIDTH_AND_HEIGHT";
    else {
      node.textAutoResize = "HEIGHT";
      node.layoutSizingHorizontal = "FILL";
    }
    // Символы по краям: строка из трёх слоёв, размер символа — кегль роли (раздел 10.5, правило 2).
    for (const [span, index] of [[leading, 0], [trailing, -1]] as const) {
      if (!span || !("icon" in span)) continue;
      const role = span.icon.typography ?? typography;
      const instance = this.icon(span.icon.name, span.icon.color ?? color, at);
      if (!instance) continue;
      if (index === 0) frame.insertChild(0, instance);
      else frame.appendChild(instance);
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
  private async source(frame: FrameNode, source: unknown, layout: Layout, fit: boolean, at: string) {
    const [kind, value] = variant<Resource | Remote>(source);
    if (kind === "resource") {
      this.resource(frame, value as Resource, fit ? layout : undefined, at);
      return;
    }
    const remote = value as Remote;
    let image: Image;
    let size: { width: number; height: number };
    try {
      image = await figma.createImageAsync(remote.origin.url);
      size = await image.getSizeAsync();
    } catch (e) {
      const fallback = remote.error ?? remote.placeholder;
      this.warn(at, `картинка ${remote.origin.url} не загрузилась${fallback ? `, показан символ ${fallback.icon.name}` : ""}: ${(e as Error).message}`);
      if (fallback) this.resource(frame, fallback, undefined, at);
      return;
    }
    const layer = figma.createRectangle();
    frame.appendChild(layer);
    layer.name = "image";
    layer.resize(size.width, size.height);
    if (layout.width.sizing !== "hug") layer.layoutSizingHorizontal = "FILL";
    if (layout.height.sizing !== "hug") layer.layoutSizingVertical = "FILL";
    layer.fills = [{ type: "IMAGE", imageHash: image.hash, scaleMode: remote.scale === "fit" ? "FIT" : "FILL" }];
    if (remote.alignment !== "center") this.placements.push(() => this.alignImage(layer, image.hash, size, remote));
  }

  private resource(frame: FrameNode, resource: Resource, fitTo: Layout | undefined, at: string) {
    const instance = this.icon(resource.icon.name, resource.icon.color, at);
    if (!instance) return;
    frame.appendChild(instance);
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

  private async badge(frame: FrameNode, model: Base, at: string) {
    if (model.content) {
      const [kind, value] = variant<{ value: string; typography: string; color: string }>(model.content);
      if (kind === "text") {
        const node = await this.textNode(value.value, value.typography, value.color, at);
        frame.appendChild(node);
        node.name = "text";
        node.textAutoResize = "WIDTH_AND_HEIGHT";
      } else {
        await this.source(frame, value, model.layout, false, at);
      }
    }
    // Ширина не меньше высоты.
    if (model.layout.width.min !== undefined) return;
    const [kind, token] = variant<Token>(model.layout.height.sizing);
    if (kind === "fixed") this.assign(frame, "minWidth", SIZE, token, at);
    else this.placements.push(() => { frame.minWidth = frame.height; });
  }
}
