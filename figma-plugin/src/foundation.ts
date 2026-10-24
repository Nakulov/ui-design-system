// Построение foundation в Figma: коллекции переменных, текстовые стили, стили эффектов, компоненты символов
// (specifications/figma-mapping.md, разделы 3.1, 6.3, 7).
import type { Bundle, Id, Values } from "./bundle";
import { Report } from "./report";

const NAMESPACE = "uiContracts";
const ICON = "schemas/foundation/icon/Icon";

type Kind = "COLOR" | "FLOAT" | "STRING" | "BOOLEAN";
type Raw = number | string | boolean | { alias: Id };

/** Описание переменной: величины по именам режимов коллекции. */
type VariableSpec = {
  ref: Id;
  name: string;
  kind: Kind;
  values: Record<string, Raw>;
  scopes: VariableScope[];
  hidden: boolean;
};

type CollectionSpec = { ref: Id; name: string; modes: string[]; variables: VariableSpec[] };

const PRIMITIVE_KIND: Record<string, Kind> = { color: "COLOR", integer: "FLOAT", float: "FLOAT", string: "STRING", boolean: "BOOLEAN" };

/** Начертание Figma по весу шрифта (раздел 3.1, правило 9). */
const FONT_STYLE: Record<number, string> = { 400: "Regular", 500: "Medium", 700: "Bold" };

const TYPOGRAPHY: { param: string; field: VariableBindableTextField; kind: Kind; scope: VariableScope; publish: boolean }[] = [
  { param: "fontSize", field: "fontSize", kind: "FLOAT", scope: "FONT_SIZE", publish: true },
  { param: "lineHeight", field: "lineHeight", kind: "FLOAT", scope: "LINE_HEIGHT", publish: false },
  { param: "letterSpacing", field: "letterSpacing", kind: "FLOAT", scope: "LETTER_SPACING", publish: false },
  { param: "fontFamily", field: "fontFamily", kind: "STRING", scope: "FONT_FAMILY", publish: false },
  { param: "fontWeight", field: "fontWeight", kind: "FLOAT", scope: "FONT_WEIGHT", publish: false },
];

const ELEVATION: { param: string; field: VariableBindableEffectField; kind: Kind; scope: VariableScope }[] = [
  { param: "offsetX", field: "offsetX", kind: "FLOAT", scope: "EFFECT_FLOAT" },
  { param: "offsetY", field: "offsetY", kind: "FLOAT", scope: "EFFECT_FLOAT" },
  { param: "blur", field: "radius", kind: "FLOAT", scope: "EFFECT_FLOAT" },
  { param: "spread", field: "spread", kind: "FLOAT", scope: "EFFECT_FLOAT" },
  { param: "color", field: "color", kind: "COLOR", scope: "EFFECT_COLOR" },
];

type Typography = { fontFamily: string; fontWeight: number; fontSize: number; lineHeight: number; letterSpacing?: number };
type Elevation = { offsetX: number; offsetY: number; blur: number; spread?: number; color: string };

function ref(node: { getSharedPluginData(namespace: string, key: string): string }, key = "ref"): string {
  return node.getSharedPluginData(NAMESPACE, key);
}

function mark(node: { setSharedPluginData(namespace: string, key: string, value: string): void }, value: string, key = "ref") {
  node.setSharedPluginData(NAMESPACE, key, value);
}

/** Префикс имён в Figma: имя контракта в lowerCamelCase без суффикса Role (`spacing/xs2`, `color/primary`). */
function prefix(contractName: string): string {
  const name = contractName.replace(/Role$/, "");
  return name[0].toLowerCase() + name.slice(1);
}

function rgba(hex: string): RGBA {
  const channel = (i: number) => parseInt(hex.slice(i, i + 2), 16) / 255;
  return { r: channel(1), g: channel(3), b: channel(5), a: hex.length > 7 ? channel(7) : 1 };
}

/** Значение переменной в Figma; алиас — ссылкой на переменную по идентификатору. */
function figmaValue(raw: Raw, kind: Kind, variables: Map<Id, Variable>): VariableValue {
  if (typeof raw === "object") {
    const target = variables.get(raw.alias);
    if (!target) throw new Error(`нет переменной цели алиаса ${raw.alias}`);
    return figma.variables.createVariableAlias(target);
  }
  return kind === "COLOR" ? rgba(raw as string) : raw;
}

/** Величина варианта в режиме коллекции: режим с тем же именем, иначе первый (schema-format.md, раздел 9.3). */
function modeValues(values: Values, variant: string, modes: string[]): Record<string, unknown> {
  return Object.fromEntries(modes.map((mode) => {
    const source = values.modes.find((m) => m.name === mode) ?? values.modes[0];
    return [mode, source.values[variant]];
  }));
}

export class FoundationBuilder {
  private variables = new Map<Id, Variable>();
  private collections = new Map<Id, VariableCollection>();
  private readonly report = new Report();

  constructor(private readonly bundle: Bundle) {}

  async build(): Promise<Report> {
    await this.index();
    const specs = this.collectionSpecs();
    // Сначала коллекции без алиасов — цели алиасов (палитра), затем остальные.
    const ordered = [...specs].sort((a, b) => Number(this.hasAliases(a)) - Number(this.hasAliases(b)));
    for (const spec of ordered) this.syncCollection(spec);
    await this.syncTextStyles();
    await this.syncEffectStyles();
    await this.syncIcons();
    await this.deleteStale(specs);
    return this.report;
  }

  private async index() {
    for (const collection of await figma.variables.getLocalVariableCollectionsAsync()) {
      const id = ref(collection);
      if (id) this.collections.set(id, collection);
    }
    for (const variable of await figma.variables.getLocalVariablesAsync()) {
      const id = ref(variable);
      if (id) this.variables.set(id, variable);
    }
  }

  private hasAliases(spec: CollectionSpec): boolean {
    return spec.variables.some((v) => Object.values(v.values).some((raw) => typeof raw === "object"));
  }

  private entities(): [Id, Values][] {
    return Object.entries(this.bundle.values);
  }

  private valueTypeName(values: Values): string | undefined {
    return typeof values.valueType === "string" ? undefined : this.bundle.contracts[values.valueType.$ref]?.name;
  }

  /** Коллекции: простые величины, параметры типографики и Elevation (раздел 3.1, правила 1–10). */
  private collectionSpecs(): CollectionSpec[] {
    const specs: CollectionSpec[] = [];
    for (const [id, values] of this.entities()) {
      const contract = this.bundle.contracts[id];
      const modes = values.modes.map((m) => m.name);
      const variants = Object.keys(values.modes[0].values);
      const base = prefix(contract.name);
      if (typeof values.valueType === "string") {
        const kind = PRIMITIVE_KIND[values.valueType];
        const project = this.bundle.figma[id];
        const internal = id.startsWith("themes/");
        specs.push({
          ref: id, name: contract.name, modes,
          variables: variants.map((variant) => ({
            ref: `${id}#${variant}`, name: `${base}/${variant}`, kind,
            values: modeValues(values, variant, modes) as Record<string, Raw>,
            scopes: internal ? [] : ((project?.scopes ?? []) as VariableScope[]),
            hidden: internal || project?.publish === false,
          })),
        });
      } else if (this.valueTypeName(values) === "TypographyStyle") {
        specs.push({
          ref: id, name: contract.name, modes,
          variables: variants.flatMap((variant) => TYPOGRAPHY.map((p) => ({
            ref: `${id}#${variant}/${p.param}`, name: `${base}/${variant}/${p.param}`, kind: p.kind,
            values: Object.fromEntries(Object.entries(modeValues(values, variant, modes))
              .map(([mode, style]) => [mode, (style as Typography)[p.param as keyof Typography] ?? 0])),
            scopes: [p.scope], hidden: !p.publish,
          }))),
        });
      } else if (this.valueTypeName(values) === "ElevationStyle") {
        specs.push({
          ref: id, name: contract.name, modes,
          variables: variants.flatMap((variant) => ELEVATION.map((p) => ({
            ref: `${id}#${variant}/${p.param}`, name: `${base}/${variant}/${p.param}`, kind: p.kind,
            values: Object.fromEntries(Object.entries(modeValues(values, variant, modes))
              .map(([mode, style]) => [mode, (style as Elevation)[p.param as keyof Elevation] ?? 0])),
            scopes: [p.scope], hidden: true,
          }))),
        });
      }
    }
    return specs;
  }

  private syncCollection(spec: CollectionSpec) {
    let collection = this.collections.get(spec.ref);
    if (!collection) {
      collection = figma.variables.createVariableCollection(spec.name);
      mark(collection, spec.ref);
      this.collections.set(spec.ref, collection);
      this.report.created("коллекции");
    } else if (collection.name !== spec.name) {
      collection.name = spec.name;
      this.report.updated("коллекции");
    }
    const modeIds = this.syncModes(collection, spec.modes);
    for (const variableSpec of spec.variables) this.syncVariable(collection, modeIds, variableSpec);
  }

  /** Режимы по именам; первый режим — режим по умолчанию (раздел 3.1, правило 3). */
  private syncModes(collection: VariableCollection, modes: string[]): Map<string, string> {
    const ids = new Map<string, string>();
    const existing = new Map(collection.modes.map((m) => [m.name, m.modeId]));
    const first = collection.modes[0];
    if (!existing.has(modes[0]) && first && !modes.includes(first.name)) {
      collection.renameMode(first.modeId, modes[0]);
      existing.delete(first.name);
      existing.set(modes[0], first.modeId);
    }
    for (const mode of modes) {
      const id = existing.get(mode);
      if (id) ids.set(mode, id);
      else {
        try {
          ids.set(mode, collection.addMode(mode));
        } catch (e) {
          throw new Error(`Коллекция ${collection.name}: не удалось добавить режим «${mode}». Число режимов ограничено тарифом Figma (${(e as Error).message})`);
        }
      }
    }
    for (const [name, id] of existing) if (!modes.includes(name)) collection.removeMode(id);
    return ids;
  }

  private syncVariable(collection: VariableCollection, modeIds: Map<string, string>, spec: VariableSpec) {
    let variable = this.variables.get(spec.ref);
    if (variable && (variable.resolvedType !== spec.kind || variable.variableCollectionId !== collection.id)) {
      this.report.warn(`Переменная ${variable.name} пересоздана: изменился тип или коллекция`);
      variable.remove();
      variable = undefined;
    }
    const isNew = !variable;
    if (!variable) {
      variable = figma.variables.createVariable(spec.name, collection, spec.kind);
      mark(variable, spec.ref);
      this.variables.set(spec.ref, variable);
    }
    const before = isNew ? null : JSON.stringify([variable.name, variable.scopes, variable.hiddenFromPublishing, variable.valuesByMode]);
    variable.name = spec.name;
    variable.scopes = spec.scopes;
    variable.hiddenFromPublishing = spec.hidden;
    for (const [mode, raw] of Object.entries(spec.values)) {
      variable.setValueForMode(modeIds.get(mode)!, figmaValue(raw, spec.kind, this.variables));
    }
    if (isNew) this.report.created("переменные");
    else if (before !== JSON.stringify([variable.name, variable.scopes, variable.hiddenFromPublishing, variable.valuesByMode])) this.report.updated("переменные");
    else this.report.unchanged("переменные");
  }

  private entity(typeName: string): [Id, Values] | undefined {
    return this.entities().find(([, values]) => this.valueTypeName(values) === typeName);
  }

  /** Текстовые стили, привязанные к переменным параметров (раздел 3.1, правила 9, 14). */
  private async syncTextStyles() {
    const entity = this.entity("TypographyStyle");
    if (!entity) return;
    const [id, values] = entity;
    const base = prefix(this.bundle.contracts[id].name);
    const existing = new Map((await figma.getLocalTextStylesAsync()).map((s) => [ref(s), s]));
    for (const [variant, raw] of Object.entries(values.modes[0].values)) {
      const value = raw as Typography;
      const font = { family: value.fontFamily, style: FONT_STYLE[value.fontWeight] ?? "Regular" };
      try {
        await figma.loadFontAsync(font);
      } catch {
        this.report.warn(`Стиль ${base}/${variant} пропущен: нет шрифта ${font.family} ${font.style}`);
        continue;
      }
      const styleRef = `${id}#${variant}`;
      let style = existing.get(styleRef);
      const isNew = !style;
      if (!style) {
        style = figma.createTextStyle();
        mark(style, styleRef);
      }
      const before = isNew ? null : JSON.stringify([style.name, style.fontName, style.fontSize, style.lineHeight, style.letterSpacing, style.boundVariables]);
      style.name = `${base}/${variant}`;
      style.fontName = font;
      style.fontSize = value.fontSize;
      style.lineHeight = { unit: "PIXELS", value: value.lineHeight };
      style.letterSpacing = { unit: "PIXELS", value: value.letterSpacing ?? 0 };
      for (const p of TYPOGRAPHY) {
        const variable = this.variables.get(`${id}#${variant}/${p.param}`);
        if (variable) style.setBoundVariable(p.field, variable);
      }
      if (isNew) this.report.created("текстовые стили");
      else if (before !== JSON.stringify([style.name, style.fontName, style.fontSize, style.lineHeight, style.letterSpacing, style.boundVariables])) this.report.updated("текстовые стили");
      else this.report.unchanged("текстовые стили");
    }
  }

  /** Стили эффектов Drop shadow, привязанные к переменным параметров (раздел 3.1, правило 10). */
  private async syncEffectStyles() {
    const entity = this.entity("ElevationStyle");
    if (!entity) return;
    const [id, values] = entity;
    const base = prefix(this.bundle.contracts[id].name);
    const existing = new Map((await figma.getLocalEffectStylesAsync()).map((s) => [ref(s), s]));
    for (const [variant, raw] of Object.entries(values.modes[0].values)) {
      const value = raw as Elevation;
      let effect: Effect = {
        type: "DROP_SHADOW", visible: true, blendMode: "NORMAL", showShadowBehindNode: false,
        color: rgba(value.color), offset: { x: value.offsetX, y: value.offsetY }, radius: value.blur, spread: value.spread ?? 0,
      };
      for (const p of ELEVATION) {
        const variable = this.variables.get(`${id}#${variant}/${p.param}`);
        if (variable) effect = figma.variables.setBoundVariableForEffect(effect, p.field, variable);
      }
      const styleRef = `${id}#${variant}`;
      let style = existing.get(styleRef);
      const isNew = !style;
      if (!style) {
        style = figma.createEffectStyle();
        mark(style, styleRef);
      }
      const before = isNew ? null : JSON.stringify([style.name, style.effects]);
      style.name = `${base}/${variant}`;
      style.effects = [effect];
      if (isNew) this.report.created("стили эффектов");
      else if (before !== JSON.stringify([style.name, style.effects])) this.report.updated("стили эффектов");
      else this.report.unchanged("стили эффектов");
    }
  }

  /** Страница сущности и её область Component (figma-documentation.md, раздел 3). */
  private async componentArea(id: Id, name: string): Promise<FrameNode> {
    await figma.loadAllPagesAsync();
    let page = figma.root.children.find((p) => ref(p) === id);
    if (!page) {
      page = figma.createPage();
      page.name = name;
      mark(page, id);
    }
    let area = page.children.find((n): n is FrameNode => n.type === "FRAME" && ref(n) === id && ref(n, "role") === "component");
    if (!area) {
      area = figma.createFrame();
      area.name = "Component";
      mark(area, id);
      mark(area, "component", "role");
      area.layoutMode = "HORIZONTAL";
      area.layoutWrap = "WRAP";
      area.itemSpacing = 24;
      area.counterAxisSpacing = 24;
      area.paddingLeft = area.paddingRight = area.paddingTop = area.paddingBottom = 24;
      area.primaryAxisSizingMode = "FIXED";
      area.counterAxisSizingMode = "AUTO";
      area.resize(480, area.height);
      area.fills = [];
      page.appendChild(area);
    }
    return area;
  }

  /** Компоненты символов icon/<имя> (раздел 3.1, правило 12). */
  private async syncIcons() {
    const icons = Object.entries(this.bundle.icons);
    if (icons.length === 0) return;
    const area = await this.componentArea(ICON, this.bundle.contracts[ICON].name);
    const existing = new Map(area.children.filter((n): n is ComponentNode => n.type === "COMPONENT").map((c) => [ref(c), c]));
    for (const [iconRef, icon] of icons) {
      const name = `${prefix("Icon")}/${iconRef.split("#")[1]}`;
      let component = existing.get(iconRef);
      const isNew = !component;
      if (component && ref(component, "svg") === icon.svg && component.name === name) {
        this.report.unchanged("символы");
        continue;
      }
      if (!component) {
        component = figma.createComponent();
        mark(component, iconRef);
        area.appendChild(component);
      }
      for (const child of [...component.children]) child.remove();
      // Рисунок в собственном размере SVG; вектор сохраняет положение внутри рамки рисунка.
      const drawing = figma.createNodeFromSvg(icon.svg);
      component.name = name;
      component.resizeWithoutConstraints(drawing.width, drawing.height);
      component.fills = [];
      component.appendChild(drawing);
      drawing.x = 0;
      drawing.y = 0;
      const vector = figma.flatten([...drawing.children], component);
      drawing.remove();
      vector.name = "vector";
      vector.constraints = { horizontal: "SCALE", vertical: "SCALE" };
      // Заливка — чёрная заглушка: цвет задаёт место использования (раздел 3.1, правило 12).
      vector.fills = [{ type: "SOLID", color: { r: 0, g: 0, b: 0 } }];
      vector.strokes = [];
      mark(component, icon.svg, "svg");
      if (isNew) this.report.created("символы");
      else this.report.updated("символы");
    }
  }

  /** Удаление объектов, исчезнувших из контрактов, с отчётом об использованиях (раздел 7, правило 6). */
  private async deleteStale(specs: CollectionSpec[]) {
    const wanted = new Set<Id>([
      ...specs.map((s) => s.ref),
      ...specs.flatMap((s) => s.variables.map((v) => v.ref)),
    ]);
    const stale: { id: string; name: string; kind: string; remove: () => void }[] = [];
    for (const [id, variable] of this.variables) {
      if (!wanted.has(id)) stale.push({ id: variable.id, name: variable.name, kind: "переменные", remove: () => variable.remove() });
    }
    const typography = this.entity("TypographyStyle")?.[0];
    const elevation = this.entity("ElevationStyle")?.[0];
    const styleRefs = (entity: Id | undefined) => new Set(entity ? Object.keys(this.bundle.values[entity].modes[0].values).map((v) => `${entity}#${v}`) : []);
    const textRefs = styleRefs(typography);
    const effectRefs = styleRefs(elevation);
    for (const style of await figma.getLocalTextStylesAsync()) {
      if (ref(style) && !textRefs.has(ref(style))) stale.push({ id: style.id, name: style.name, kind: "текстовые стили", remove: () => style.remove() });
    }
    for (const style of await figma.getLocalEffectStylesAsync()) {
      if (ref(style) && !effectRefs.has(ref(style))) stale.push({ id: style.id, name: style.name, kind: "стили эффектов", remove: () => style.remove() });
    }
    const page = figma.root.children.find((p) => ref(p) === ICON);
    for (const component of page?.findAllWithCriteria({ types: ["COMPONENT"] }) ?? []) {
      if (ref(component).startsWith(`${ICON}#`) && !(ref(component) in this.bundle.icons)) {
        stale.push({ id: component.id, name: component.name, kind: "символы", remove: () => component.remove() });
      }
    }
    if (stale.length > 0) {
      const usages = await findUsages(new Set(stale.map((s) => s.id)));
      for (const item of stale) {
        const used = usages.get(item.id) ?? [];
        if (used.length > 0) this.report.warn(`${item.name} удалён, но использовался: ${used.slice(0, 5).join(", ")}${used.length > 5 ? ` и ещё ${used.length - 5}` : ""}`);
        item.remove();
        this.report.deleted(item.kind);
      }
    }
    for (const [id, collection] of this.collections) {
      if (!wanted.has(id)) {
        collection.remove();
        this.report.deleted("коллекции");
      }
    }
  }
}

/** Использования переменных, стилей и компонентов в текущем файле: id объекта → имена слоёв. */
async function findUsages(ids: Set<string>): Promise<Map<string, string[]>> {
  await figma.loadAllPagesAsync();
  const result = new Map<string, string[]>();
  const add = (id: string, where: string) => {
    if (ids.has(id)) result.set(id, [...(result.get(id) ?? []), where]);
  };
  const aliases = (value: unknown, where: string) => {
    JSON.stringify(value, (key, item) => {
      if (key === "id" && typeof item === "string") add(item, where);
      return item;
    });
  };
  for (const variable of await figma.variables.getLocalVariablesAsync()) aliases(variable.valuesByMode, `переменная ${variable.name}`);
  for (const page of figma.root.children) {
    for (const node of page.findAll()) {
      const where = `${page.name} / ${node.name}`;
      if ("boundVariables" in node) aliases(node.boundVariables, where);
      if ("fills" in node && Array.isArray(node.fills)) aliases(node.fills, where);
      if ("strokes" in node) aliases(node.strokes, where);
      if ("effects" in node) aliases(node.effects, where);
      for (const key of ["fillStyleId", "strokeStyleId", "effectStyleId", "textStyleId"] as const) {
        const value = (node as unknown as Record<string, unknown>)[key];
        if (typeof value === "string") add(value, where);
      }
      if (node.type === "INSTANCE") {
        const main = await node.getMainComponentAsync();
        if (main) add(main.id, where);
      }
    }
  }
  return result;
}
