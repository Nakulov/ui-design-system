// Страница Components: Figma-компоненты библиотеки из тел образцов (figma-mapping.md, разделы 3.2, 3.5, 5, 6, 7).
import type { Bundle, Id } from "./bundle";
import { mark, ref } from "./ids";
import { Renderer, Tokens } from "./render";
import { Report } from "./report";

const ROLE = "components";
const GAP = 48;
const VARIANT_GAP = 24;
/** Значения variant `state` по контексту оформления образца (раздел 3.5). */
const STATES: Record<string, string[]> = {
  control: ["default", "pressed", "disabled"],
  focusable: ["default", "focused", "disabled"],
};
/** Свойства Figma на слоях тела по месту слоя (разделы 4.1, правила 11–13, и 10.5). */
const TEXT_PROPERTY: Record<string, string> = { Text: "content", Badge: "text" };

type Project = { sample?: string; variants?: Record<string, Record<string, string>>; iconSample?: string };
type Model = Record<string, { id?: string; appearance: Record<string, unknown>; [field: string]: unknown }>;
/** Вариант Figma-компонента: значения variant-свойств и модель образца. */
type Variant = { values: [string, string][]; model: Model };
/** Figma-компонент: идентификатор (раздел 7.1), имя, контракт и варианты. */
type Target = { id: Id; name: string; contract: string; variants: Variant[] };

export class ComponentsBuilder {
  private readonly report = new Report();

  constructor(private readonly bundle: Bundle) {}

  async build(): Promise<Report> {
    const tokens = await Tokens.load(this.bundle);
    const page = await this.page();
    const targets = this.targets().sort((a, b) => a.name.localeCompare(b.name));
    let y = 0;
    for (const target of targets) {
      const node = await this.component(page, target, tokens);
      if (!node) continue;
      node.x = 0;
      node.y = y;
      y += node.height + GAP;
    }
    const ids = new Set(targets.map((t) => t.id));
    for (const node of [...page.children]) {
      if (ref(node) && !ids.has(ref(node))) {
        node.remove();
        this.report.deleted("компоненты");
      }
    }
    await figma.setCurrentPageAsync(page);
    return this.report;
  }

  private async page(): Promise<PageNode> {
    await figma.loadAllPagesAsync();
    let page = figma.root.children.find((p) => ref(p, "role") === ROLE);
    if (!page) {
      page = figma.createPage();
      page.name = "Components";
      mark(page, ROLE, "role");
    }
    return page;
  }

  /** Figma-компоненты по `figma.json` компонентов; `iconSample` даёт `text/icon` (раздел 10.5). */
  private targets(): Target[] {
    const result: Target[] = [];
    for (const [id, data] of Object.entries(this.bundle.figma)) {
      const contract = this.bundle.contracts[id];
      if (!contract?.extends) continue;
      const project = data as Project;
      const samples: [[string, string][], string][] = project.sample !== undefined
        ? [[[], project.sample]]
        : Object.entries(project.variants ?? {}).flatMap(([property, values]) =>
          Object.entries(values).map(([value, sample]) => [[[property, value]], sample] as [[string, string][], string]));
      result.push({ id, name: contract.name, contract: contract.name, variants: this.variants(id, contract.name, samples) });
      if (project.iconSample !== undefined) {
        result.push({ id: `${id}#icon`, name: "text/icon", contract: contract.name, variants: this.variants(id, "text/icon", [[[], project.iconSample]]) });
      }
    }
    return result;
  }

  /** Варианты: образцы значений, умноженные на состояния оформления образца (раздел 3.5). */
  private variants(id: Id, name: string, samples: [[string, string][], string][]): Variant[] {
    const result: Variant[] = [];
    for (const [values, sample] of samples) {
      const model = this.bundle.samples[`${id}#${sample}`] as Model | undefined;
      if (!model) {
        this.report.warn(`${name}: нет образца ${sample}`);
        continue;
      }
      const [component, body] = Object.entries(model)[0];
      const [context, appearance] = Object.entries(body.appearance)[0] as [string, Record<string, unknown>];
      const states = STATES[context];
      if (!states) {
        if (context !== "plain") this.report.warn(`${name}: состояния оформления ${context} пока не строятся, взят образец как есть`);
        result.push({ values, model });
        continue;
      }
      for (const state of states) {
        result.push({ values: [...values, ["state", state]], model: { [component]: { ...body, appearance: { plain: appearance[state] } } } });
      }
    }
    return result;
  }

  private async component(page: PageNode, target: Target, tokens: Tokens): Promise<ComponentNode | ComponentSetNode | undefined> {
    if (!target.variants.length) return undefined;
    const isSet = target.variants[0].values.length > 0;
    let existing = page.children.find((n) => ref(n) === target.id);
    if (existing && (existing.type === "COMPONENT_SET") !== isSet) {
      // Переход Component ↔ Component Set разрывает связь с экземплярами (раздел 11).
      this.report.warn(`${target.name}: вид Figma-компонента сменился, компонент пересоздан; связь с экземплярами потеряна`);
      existing.remove();
      existing = undefined;
    }
    if (existing) this.report.updated("компоненты");
    else this.report.created("компоненты");
    if (!isSet) {
      const node = existing?.type === "COMPONENT" ? existing : figma.createComponent();
      page.appendChild(node);
      await this.draw(node, target.variants[0], target, tokens);
      node.name = target.name;
      mark(node, target.id);
      await this.properties(node, [node], target);
      return node;
    }
    const set = existing?.type === "COMPONENT_SET" ? existing : undefined;
    const old = new Map((set?.children ?? []).map((n) => [n.name, n as ComponentNode]));
    const nodes: ComponentNode[] = [];
    for (const variant of target.variants) {
      const name = variant.values.map(([property, value]) => `${property}=${value}`).join(", ");
      const node = old.get(name) ?? figma.createComponent();
      old.delete(name);
      (set ?? page).appendChild(node);
      await this.draw(node, variant, target, tokens);
      node.name = name;
      nodes.push(node);
    }
    for (const node of old.values()) node.remove();
    const result = set ?? figma.combineAsVariants(nodes, page);
    result.name = target.name;
    mark(result, target.id);
    // Варианты в строку: у Component Set без Auto Layout растянутые корни образцов сохраняют размер.
    let x = VARIANT_GAP;
    for (const node of nodes) {
      result.appendChild(node);
      node.x = x;
      node.y = VARIANT_GAP;
      x += node.width + VARIANT_GAP;
    }
    result.resize(x, Math.max(...nodes.map((n) => n.height)) + 2 * VARIANT_GAP);
    await this.properties(result, nodes, target);
    return result;
  }

  /** Тело образца в Figma-компонент; слои обновляются на месте (раздел 7, правило 8). */
  private async draw(node: ComponentNode, variant: Variant, target: Target, tokens: Tokens) {
    const label = `${target.name}${variant.values.length ? ` (${variant.values.map(([, v]) => v).join(", ")})` : ""}`;
    const body = Object.values(variant.model)[0];
    const slot = body.id ?? "root";
    const drawn = await new Renderer(tokens, this.report, label, true).draw(variant.model, node.parent as BaseNode & ChildrenMixin, new Map([[slot, node]]), slot);
    if ((drawn as SceneNode | undefined) !== node) this.report.warn(`${label}: образец не отрисован в Figma-компонент`);
  }

  /** Свойства Figma на слоях с местами `text`, `icon`, `leadingIcon`, `trailingIcon` (раздел 6.2, правило 4). */
  private async properties(owner: ComponentNode | ComponentSetNode, nodes: ComponentNode[], target: Target) {
    const isIcon = target.name === "text/icon";
    for (const node of nodes) {
      const layer = (slot: string) => node.children.find((n) => ref(n, "slot") === slot);
      const text = layer("text");
      if (text?.type === "TEXT" && TEXT_PROPERTY[target.contract]) {
        text.componentPropertyReferences = { characters: this.define(owner, TEXT_PROPERTY[target.contract], "TEXT", text.characters) };
      }
      const icons: [string, string | undefined][] = isIcon ? [["leadingIcon", "leading"], ["trailingIcon", "trailing"]] : [["icon", undefined]];
      for (const [slot, visibility] of icons) {
        const icon = layer(slot);
        if (icon?.type !== "INSTANCE") continue;
        const main = await icon.getMainComponentAsync();
        if (!main) continue;
        const references: { [key: string]: string } = { mainComponent: this.define(owner, slot, "INSTANCE_SWAP", main.id) };
        if (visibility) references.visible = this.define(owner, visibility, "BOOLEAN", true);
        icon.componentPropertyReferences = references;
      }
    }
  }

  /** Ключ свойства Figma по имени: существующее обновляет значение по умолчанию, иначе создаётся. */
  private define(owner: ComponentNode | ComponentSetNode, name: string, type: "TEXT" | "BOOLEAN" | "INSTANCE_SWAP", value: string | boolean): string {
    const key = Object.entries(owner.componentPropertyDefinitions).find(([k, d]) => k.split("#")[0] === name && d.type === type)?.[0];
    if (!key) return owner.addComponentProperty(name, type, value);
    if (owner.componentPropertyDefinitions[key].defaultValue !== value) owner.editComponentProperty(key, { defaultValue: value });
    return key;
  }
}
