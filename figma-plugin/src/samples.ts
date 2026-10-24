// Страница Samples: отрисовка примеров моделей компонентов для проверки ядра отрисовки (figma-mapping.md, раздел 10).
import type { Bundle, Id } from "./bundle";
import { COLOR, mark, ref } from "./ids";
import { Renderer, Tokens } from "./render";
import { Report } from "./report";

const ROLE = "samples";
const CARD_WIDTH = 360;
const LABEL_FONT: FontName = { family: "Inter", style: "Regular" };
/** Примеры, сгенерированные из контракта (schema-format.md, раздел 11.1): проверяют ветки ядра, а не вид. */
const GENERATED = ["min", "max"];

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

  constructor(private readonly bundle: Bundle) {}

  async build(): Promise<Report> {
    const tokens = await Tokens.load(this.bundle);
    await figma.loadFontAsync(LABEL_FONT);
    const page = await this.page();
    const root = this.root(page);
    // Примеры публичных контрактов компонентов; примеры core — фрагменты модели и не рисуются.
    const groups = new Map<Id, [string, unknown][]>();
    for (const [key, model] of Object.entries(this.bundle.samples)) {
      const contract = key.split("#")[0];
      if (!contract.startsWith("schemas/components/")) continue;
      groups.set(contract, [...(groups.get(contract) ?? []), [key, model]]);
    }
    const rows = new Map(root.children.filter((n): n is FrameNode => n.type === "FRAME").map((n) => [ref(n), n]));
    for (const [contract, samples] of [...groups].sort(([a], [b]) => a.localeCompare(b))) {
      let row = rows.get(contract);
      rows.delete(contract);
      if (!row) {
        row = figma.createFrame();
        mark(row, contract);
      }
      root.appendChild(row);
      row.name = this.bundle.contracts[contract].name;
      autoLayout(row, "HORIZONTAL", 24);
      row.counterAxisAlignItems = "MIN";
      await this.cards(row, samples, tokens);
    }
    for (const row of rows.values()) {
      for (const card of row.children) if (ref(card)) this.report.deleted("примеры");
      row.remove();
    }
    await figma.setCurrentPageAsync(page);
    return this.report;
  }

  private async page(): Promise<PageNode> {
    await figma.loadAllPagesAsync();
    let page = figma.root.children.find((p) => ref(p, "role") === ROLE);
    if (!page) {
      page = figma.createPage();
      page.name = "Samples";
      mark(page, ROLE, "role");
    }
    return page;
  }

  private root(page: PageNode): FrameNode {
    let root = child(page, (n): n is FrameNode => n.type === "FRAME" && ref(n, "role") === ROLE);
    if (!root) {
      root = figma.createFrame();
      page.appendChild(root);
      mark(root, ROLE, "role");
    }
    root.name = "Samples";
    autoLayout(root, "VERTICAL", 48);
    return root;
  }

  /** Карточки примеров: подпись `<Компонент>#<пример>` и отрисованная модель. */
  private async cards(row: FrameNode, samples: [string, unknown][], tokens: Tokens) {
    const cards = new Map(row.children.filter((n): n is FrameNode => n.type === "FRAME").map((n) => [ref(n), n]));
    for (const [key, model] of samples) {
      let card = cards.get(key);
      cards.delete(key);
      const label = `${this.bundle.contracts[key.split("#")[0]].name}#${key.split("#")[1]}`;
      if (card) this.report.updated("примеры");
      else {
        card = figma.createFrame();
        mark(card, key);
        this.report.created("примеры");
      }
      row.appendChild(card);
      card.name = label;
      autoLayout(card, "VERTICAL", 12);
      card.primaryAxisSizingMode = "AUTO";
      card.counterAxisSizingMode = "FIXED";
      card.resize(CARD_WIDTH, card.height);
      card.paddingLeft = card.paddingRight = card.paddingTop = card.paddingBottom = 16;
      card.cornerRadius = 8;
      card.fills = [this.paint(tokens, "background")];
      card.strokes = [this.paint(tokens, "outline")];
      this.label(card, GENERATED.includes(key.split("#")[1]) ? `${label} · сгенерирован` : label, tokens);
      // Модель с тем же id обновляется на месте, остальные слои пересоздаются (раздел 10.1, правило 4).
      const reuse = new Map<string, SceneNode>();
      for (const node of [...card.children]) {
        if (ref(node, "role") === "label") continue;
        if (ref(node, "id")) reuse.set(ref(node, "id"), node);
        else node.remove();
      }
      await new Renderer(tokens, this.report, label).draw(model, card, reuse);
      for (const node of reuse.values()) node.remove();
    }
    for (const card of cards.values()) {
      if (ref(card)) this.report.deleted("примеры");
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
