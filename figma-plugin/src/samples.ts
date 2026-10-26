// Страницы Samples и Screens: отрисовка примеров моделей компонентов для проверки ядра отрисовки (figma-mapping.md,
// раздел 10) и демо-экранов для проверки импорта (bundle-format.md, раздел 3.9). Ядро и обновление на месте общие.
import type { Bundle, Element, Id } from "./bundle";
import { COLOR, mark, ref } from "./ids";
import { Renderer, Tokens } from "./render";
import { Report } from "./report";

/** Страница построителя: роль страницы и корня, имя страницы, вид объектов в отчёте. */
export type Kind = { role: string; name: string; counted: string };

export const SAMPLES: Kind = { role: "samples", name: "Samples", counted: "примеры" };
export const SCREENS: Kind = { role: "screens", name: "Screens", counted: "экраны" };
/** Строка экранов: экраны лежат одной строкой, ключ карточки — `screens/<имя>`. */
const SCREENS_ROW = "screens";
const CARD_WIDTH = 360;
/** Элементы документации растягиваются по ширине страницы: карточки шире. */
const DOCUMENTATION_CARD_WIDTH = 600;
const CARD_PADDING = 16;
const LABEL_FONT: FontName = { family: "Inter", style: "Regular" };
/** Примеры, сгенерированные из контракта (schema-format.md, раздел 11.1): проверяют ветки ядра, а не вид. */
const GENERATED = ["min", "max"];
/** Строка элементов оформления, превью и демонстраций документации (figma-documentation.md, раздел 5). */
const DOCUMENTATION = "figma/documentation/elements";
/** Превью и демонстрации лежат в сущности и подписываются с ней: `Documentation#layout/widthSizing`,
 *  `Documentation#spacing/preview`; элементы оформления — по имени: `Documentation#heading`. */
const IN_ENTITY = /^[a-z][A-Za-z0-9]*\/([a-z][A-Za-z0-9]*)\/(?:demos\/)?([a-z][A-Za-z0-9]*)$/;

function label(key: string): string {
  const match = key.startsWith(DOCUMENTATION) ? null : IN_ENTITY.exec(key);
  return match ? `${match[1]}/${match[2]}` : key.split("/").pop()!;
}

/** Строка страницы: идентификатор, имя и карточки — ключ, подпись, модель. */
type Row = { id: Id; name: string; cards: [string, string, unknown][] };

/** Модель элемента с подставленными дырками; дырка-список внутри массива разворачивается. */
export function fill(node: unknown, values: Record<string, unknown>): unknown {
  const hole = (item: unknown) => typeof item === "string" && item.startsWith("$") && item.slice(1) in values;
  if (hole(node)) return values[(node as string).slice(1)];
  if (Array.isArray(node)) return node.flatMap((item) => (hole(item) && Array.isArray(fill(item, values)) ? fill(item, values) as unknown[] : [fill(item, values)]));
  if (typeof node === "object" && node !== null) return Object.fromEntries(Object.entries(node).map(([k, v]) => [k, fill(v, values)]));
  return node;
}

function autoLayout(frame: FrameNode, mode: "HORIZONTAL" | "VERTICAL", spacing: number) {
  frame.layoutMode = mode;
  frame.itemSpacing = spacing;
  frame.primaryAxisSizingMode = "AUTO";
  frame.counterAxisSizingMode = "AUTO";
  frame.fills = [];
  frame.clipsContent = false;
}

function child<T extends SceneNode>(parent: ChildrenMixin, test: (node: SceneNode) => node is T): T | undefined {
  return parent.children.find(test);
}

export class SamplesBuilder {
  private readonly report = new Report();

  constructor(private readonly bundle: Bundle, private readonly kind: Kind = SAMPLES) {}

  async build(): Promise<Report> {
    const tokens = await Tokens.load(this.bundle);
    await figma.loadFontAsync(LABEL_FONT);
    const page = await this.page();
    const root = this.root(page);
    const ordered = this.kind === SCREENS ? this.screens() : this.samples();
    const rows = new Map(root.children.filter((n): n is FrameNode => n.type === "FRAME").map((n) => [ref(n), n]));
    for (const group of ordered) {
      let row = rows.get(group.id);
      rows.delete(group.id);
      if (!row) {
        row = figma.createFrame();
        mark(row, group.id);
      }
      root.appendChild(row);
      row.name = group.name;
      autoLayout(row, "HORIZONTAL", 24);
      row.counterAxisAlignItems = "MIN";
      await this.cards(row, group.cards, tokens);
    }
    for (const row of rows.values()) {
      for (const card of row.children) if (ref(card)) this.report.deleted(this.kind.counted);
      row.remove();
    }
    await figma.setCurrentPageAsync(page);
    return this.report;
  }

  /** Экраны по имени файла — одной строкой (bundle-format.md, раздел 3.9). */
  private screens(): Row[] {
    const cards = Object.entries(this.bundle.screens).map(([name, model]): [string, string, unknown] => [`screens/${name}`, name, model]);
    return cards.length ? [{ id: SCREENS_ROW, name: "Screens", cards }] : [];
  }

  private samples(): Row[] {
    // Примеры публичных контрактов компонентов; примеры core — фрагменты модели и не рисуются.
    const groups = new Map<Id, Row>();
    for (const [key, model] of Object.entries(this.bundle.samples)) {
      const contract = key.split("#")[0];
      if (!contract.startsWith("components/")) continue;
      const name = this.bundle.contracts[contract].name;
      const row = groups.get(contract) ?? { id: contract, name, cards: [] };
      row.cards.push([key, `${name}#${key.split("#")[1]}`, model]);
      groups.set(contract, row);
    }
    const elements = Object.entries(this.bundle.documentation ?? {});
    const documentation: Row = {
      id: DOCUMENTATION, name: "Documentation",
      cards: elements.map(([key, element]: [string, Element]) => [key, `Documentation#${label(key)}`,
        fill(element.model, Object.fromEntries(Object.entries(element.holes).map(([name, hole]) => [name, hole.sample])))]),
    };
    const ordered = [...groups.values()].sort((a, b) => a.id.localeCompare(b.id));
    if (documentation.cards.length) ordered.push(documentation);
    return ordered;
  }

  private async page(): Promise<PageNode> {
    await figma.loadAllPagesAsync();
    let page = figma.root.children.find((p) => ref(p, "role") === this.kind.role);
    if (!page) {
      page = figma.createPage();
      page.name = this.kind.name;
      mark(page, this.kind.role, "role");
    }
    return page;
  }

  private root(page: PageNode): FrameNode {
    let root = child(page, (n): n is FrameNode => n.type === "FRAME" && ref(n, "role") === this.kind.role);
    if (!root) {
      root = figma.createFrame();
      page.appendChild(root);
      mark(root, this.kind.role, "role");
    }
    root.name = this.kind.name;
    autoLayout(root, "VERTICAL", 48);
    return root;
  }

  /** Карточки примеров и экранов: подпись (`<Компонент>#<пример>` или имя экрана) и отрисованная модель. */
  private async cards(row: FrameNode, samples: [string, string, unknown][], tokens: Tokens) {
    const cards = new Map(row.children.filter((n): n is FrameNode => n.type === "FRAME").map((n) => [ref(n), n]));
    for (const [key, label, model] of samples) {
      let card = cards.get(key);
      cards.delete(key);
      if (card) this.report.updated(this.kind.counted);
      else {
        card = figma.createFrame();
        mark(card, key);
        this.report.created(this.kind.counted);
      }
      row.appendChild(card);
      card.name = label;
      autoLayout(card, "VERTICAL", 12);
      card.primaryAxisSizingMode = "AUTO";
      card.counterAxisSizingMode = "FIXED";
      // Корень с фиксированной шириной больше карточки (например, страница документации) расширяет карточку.
      const width = (Object.values(model as Record<string, { layout?: { width?: { sizing?: { fixed?: { custom?: { value?: number } } } } } }>)[0]
        ?.layout?.width?.sizing as { fixed?: { custom?: { value?: number } } } | undefined)?.fixed?.custom?.value ?? 0;
      const base = row.name === "Documentation" ? DOCUMENTATION_CARD_WIDTH : CARD_WIDTH;
      card.resize(Math.max(base, width + 2 * CARD_PADDING), card.height);
      card.paddingLeft = card.paddingRight = card.paddingTop = card.paddingBottom = CARD_PADDING;
      card.cornerRadius = 8;
      card.fills = [this.paint(tokens, "background")];
      card.strokes = [this.paint(tokens, "outline")];
      this.label(card, GENERATED.includes(key.split("#")[1]) ? `${label} · сгенерирован` : label, tokens);
      // Слои обновляются на месте по месту в модели (раздел 10.1, правило 4).
      const reuse = new Map<string, SceneNode>();
      for (const node of [...card.children]) {
        if (ref(node, "role") === "label") continue;
        if (ref(node, "slot")) reuse.set(ref(node, "slot"), node);
        else node.remove();
      }
      await new Renderer(tokens, this.report, label).draw(model, card, reuse);
      for (const node of reuse.values()) node.remove();
    }
    for (const card of cards.values()) {
      if (ref(card)) this.report.deleted(this.kind.counted);
      card.remove();
    }
  }

  private label(card: FrameNode, text: string, tokens: Tokens) {
    let label = child(card, (n): n is TextNode => n.type === "TEXT" && ref(n, "role") === "label");
    if (!label) {
      label = figma.createText();
      mark(label, "label", "role");
    }
    card.insertChild(0, label);
    label.fontName = LABEL_FONT;
    label.fontSize = 12;
    label.characters = text;
    label.name = "label";
    label.fills = [this.paint(tokens, "onSurfaceMuted")];
  }

  private paint(tokens: Tokens, color: string): SolidPaint {
    const paint: SolidPaint = { type: "SOLID", color: { r: 0.5, g: 0.5, b: 0.5 } };
    const variable = tokens.variable(COLOR, color);
    return variable ? figma.variables.setBoundVariableForPaint(paint, "color", variable) : paint;
  }
}
