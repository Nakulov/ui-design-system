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
/** Вариант Figma-компонента: значения variant-свойств, образец и его модель. */
export type Variant = { values: [string, string][]; sample: string; model: Model };
/** Переименование значения variant-свойства из журнала изменений: свойство, прежнее и новое значение. */
type Rename = [string, string, string];
/** Figma-компонент: идентификатор (раздел 7.1), имя, контракт, варианты и переименования от старых к новым. */
export type Target = { id: Id; name: string; contract: string; variants: Variant[]; renames: Rename[] };

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
  targets(): Target[] {
    const result: Target[] = [];
    for (const [id, data] of Object.entries(this.bundle.figma)) {
      const contract = this.bundle.contracts[id];
      if (!contract?.extends) continue;
      const project = data as Project;
      const samples: [[string, string][], string][] = project.sample !== undefined
        ? [[[], project.sample]]
        : Object.entries(project.variants ?? {}).flatMap(([property, values]) =>
          Object.entries(values).map(([value, sample]) => [[[property, value]], sample] as [[string, string][], string]));
      result.push({ id, name: contract.name, contract: contract.name, variants: this.variants(id, contract.name, samples), renames: this.renames(id, project) });
      if (project.iconSample !== undefined) {
        result.push({ id: `${id}#icon`, name: "text/icon", contract: contract.name, variants: this.variants(id, "text/icon", [[[], project.iconSample]]), renames: [] });
      }
    }
    return result;
  }

  /** Записи `renamed` журнала о значениях variant-свойств: `target` и `from` с общим свойством (раздел 7, правило 3). */
  private renames(id: Id, project: Project): Rename[] {
    const result: Rename[] = [];
    for (const entry of [...(this.bundle.changelogs[id]?.entries ?? [])].reverse()) {
      if (entry.kind !== "renamed" || !entry.target || !entry.from) continue;
      const [property, to, ...rest] = entry.target.split(".");
      const [fromProperty, from, ...fromRest] = entry.from.split(".");
      if (rest.length || fromRest.length || to === undefined || property !== fromProperty || !project.variants?.[property]) continue;
      result.push([property, from, to]);
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
        result.push({ values, sample, model });
        continue;
      }
      for (const state of states) {
        result.push({ values: [...values, ["state", state]], sample, model: { [component]: { ...body, appearance: { plain: appearance[state] } } } });
      }
    }
    return result;
  }

  private async component(page: PageNode, target: Target, tokens: Tokens): Promise<ComponentNode | ComponentSetNode | undefined> {
    if (!target.variants.length) return undefined;
    const isSet = target.variants[0].values.length > 0;
    const existing = page.children.find((n) => ref(n) === target.id);
    if (existing) this.report.updated("компоненты");
    else this.report.created("компоненты");
    if (!isSet) {
      let node = existing?.type === "COMPONENT" ? existing : undefined;
      if (existing?.type === "COMPONENT_SET") {
        // Component Set → Component: вариант с тем же образцом выносится из набора и сохраняет ключ (раздел 7, правило 10).
        const variants = existing.children as ComponentNode[];
        node = variants.find((n) => ref(n, "sample") === target.variants[0].sample) ?? variants[0];
        page.appendChild(node);
        for (const other of [...existing.children] as ComponentNode[]) await this.removeVariant(other, target);
        if (!existing.removed) existing.remove();
      }
      node ??= figma.createComponent();
      page.appendChild(node);
      await this.draw(node, target.variants[0], target, tokens);
      node.name = target.name;
      mark(node, target.id);
      mark(node, target.variants[0].sample, "sample");
      await this.properties(node, [node], target);
      return node;
    }
    const set = existing?.type === "COMPONENT_SET" ? existing : undefined;
    const name = (values: [string, string][]) => values.map(([property, value]) => `${property}=${value}`).join(", ");
    const old = new Map((set?.children ?? []).map((n) => [n.name, n as ComponentNode]));
    if (existing?.type === "COMPONENT") {
      // Component → Component Set: компонент становится вариантом с тем же образцом и сохраняет ключ (раздел 7, правило 10).
      const variant = target.variants.find((v) => v.sample === ref(existing, "sample")) ?? target.variants[0];
      old.set(name(variant.values), existing);
    }
    this.rename(old, target);
    const nodes: ComponentNode[] = [];
    for (const variant of target.variants) {
      const node = old.get(name(variant.values)) ?? figma.createComponent();
      old.delete(name(variant.values));
      // Подмена символа в слое, привязанном к свойству, меняет значение свойства во всём наборе: привязки снимаются
      // до отрисовки и ставятся заново в `properties`.
      for (const layer of node.children) if (Object.keys(layer.componentPropertyReferences ?? {}).length) layer.componentPropertyReferences = {};
      (set ?? page).appendChild(node);
      await this.draw(node, variant, target, tokens);
      node.name = name(variant.values);
      mark(node, variant.sample, "sample");
      nodes.push(node);
    }
    for (const node of old.values()) await this.removeVariant(node, target);
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

  /** Переименовывает варианты по записям `renamed` журнала, если варианта с новым именем ещё нет (раздел 7, правило 3). */
  private rename(old: Map<string, ComponentNode>, target: Target) {
    for (const [property, from, to] of target.renames) {
      for (const [name, node] of [...old]) {
        const renamed = name.split(", ").map((pair) => (pair === `${property}=${from}` ? `${property}=${to}` : pair)).join(", ");
        if (renamed === name || old.has(renamed)) continue;
        old.delete(name);
        old.set(renamed, node);
        this.report.warn(`${target.name}: вариант ${name} переименован в ${renamed}`);
      }
    }
  }

  /** Удаляет вариант и перечисляет его экземпляры в текущем файле (раздел 7, правило 6). */
  private async removeVariant(node: ComponentNode, target: Target) {
    // Экземпляры по страницам: имя экземпляра — имя компонента, а страницы документации названы по сущностям.
    const pages = new Map<string, number>();
    for (const instance of await node.getInstancesAsync()) {
      let page: BaseNode | null = instance;
      while (page && page.type !== "PAGE") page = page.parent;
      pages.set(page?.name ?? "?", (pages.get(page?.name ?? "?") ?? 0) + 1);
    }
    const where = [...pages].sort((a, b) => b[1] - a[1]).map(([page, count]) => `${page} ×${count}`);
    const used = where.length ? `; экземпляры в этом файле: ${where.join(", ")}` : "";
    this.report.warn(`${target.name}: вариант ${node.name} удалён${used}. В других файлах экземпляры останутся на удалённом компоненте без обновлений`);
    node.remove();
    this.report.deleted("варианты");
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
    // Значение свойства одно на весь Component Set: его задаёт первый вариант со слоем, вариант с другим значением
    // получает своё свойство `<имя> (<значения варианта>)` (раздел 6.2, правило 4).
    const first = new Map<string, string | boolean>();
    for (const [index, node] of nodes.entries()) {
      const define = (name: string, type: "TEXT" | "BOOLEAN" | "INSTANCE_SWAP", value: string | boolean) => {
        if (!first.has(name)) first.set(name, value);
        if (first.get(name) === value) return this.define(owner, name, type, value);
        return this.define(owner, `${name} (${target.variants[index].values.map(([, v]) => v).join(", ")})`, type, value);
      };
      const layer = (slot: string) => node.children.find((n) => ref(n, "slot") === slot);
      const text = layer("text");
      if (text?.type === "TEXT" && TEXT_PROPERTY[target.contract]) {
        text.componentPropertyReferences = { characters: define(TEXT_PROPERTY[target.contract], "TEXT", text.characters) };
      }
      const icons: [string, string | undefined][] = isIcon ? [["leadingIcon", "leading"], ["trailingIcon", "trailing"]] : [["icon", undefined]];
      for (const [slot, visibility] of icons) {
        const icon = layer(slot);
        if (icon?.type !== "INSTANCE") continue;
        const main = await icon.getMainComponentAsync();
        if (!main) continue;
        const references: { [key: string]: string } = { mainComponent: define(slot, "INSTANCE_SWAP", main.id) };
        if (visibility) references.visible = define(visibility, "BOOLEAN", true);
        icon.componentPropertyReferences = references;
      }
    }
    // Свойство без слоя Figma не публикует (Unused properties): такое остаётся после выноса варианта из набора или
    // удаления варианта, который один его использовал.
    const used = new Set(nodes.flatMap((node) => node.findAll((n) => !!n.componentPropertyReferences).flatMap((n) => Object.values(n.componentPropertyReferences ?? {}))));
    for (const [key, definition] of Object.entries(owner.componentPropertyDefinitions)) {
      if (definition.type !== "VARIANT" && !used.has(key)) owner.deleteComponentProperty(key);
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
