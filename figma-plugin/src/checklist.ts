// Release Checklist: автоматические пункты и ручные отметки (figma-documentation.md, раздел 3.5; figma-mapping.md, раздел 8).
import { type Bundle, type ChecklistScope, compare, type Id, type Source } from "./bundle";
import { ComponentsBuilder } from "./components";
import { FoundationBuilder } from "./foundation";
import { ICON, ref } from "./ids";
import type { Build, ChecklistView, Imports } from "./messages";

/** `Container` всегда рисуется телом: у него сокращённый чеклист (figma-mapping.md, раздел 8.3). */
const CONTAINER = "components/container/Container";

/** Обязательные секции манифеста (documentation-manifest.md, раздел 4.2). */
const REQUIRED: Record<Exclude<ChecklistScope, "foundation">, string[]> = {
  component: ["overview", "properties", "variants", "usage", "platformSupport", "history"],
  container: ["overview", "properties", "variants", "usage", "platformSupport", "history"],
  core: ["overview", "properties", "usage", "platformSupport", "history"],
};

/** Поля, которые в Figma-компоненте должны быть привязаны к переменным, если величина не нулевая. */
const NUMBER_FIELDS = ["paddingLeft", "paddingRight", "paddingTop", "paddingBottom", "itemSpacing", "counterAxisSpacing",
  "topLeftRadius", "topRightRadius", "bottomLeftRadius", "bottomRightRadius", "strokeWeight"] as const;

/** Ключи `sharedPluginData` фрейма: результаты автоматических пунктов и ручные отметки. */
export const AUTO_KEY = "auto";
export const CHECKS_KEY = "checks";

/** Ручная отметка хранит сборку документации, на которой её поставили. */
export type Check = { commit: string | null; committedAt: string | null };
export type Checks = Record<string, Check>;
/** Результат автоматического пункта: пройден или нет. */
export type Auto = Record<string, boolean>;

/** Вид чеклиста сущности; без чеклиста — undefined. */
export function scope(id: Id): ChecklistScope | undefined {
  if (id.startsWith("components/")) return id === CONTAINER ? "container" : "component";
  if (id.startsWith("foundation/")) return "foundation";
  if (id.startsWith("core/")) return "core";
  return undefined;
}

export function readJson<T>(node: BaseNode, key: string): T {
  try {
    const data = JSON.parse(ref(node, key) || "{}");
    return typeof data === "object" && data !== null ? data as T : {} as T;
  } catch {
    return {} as T;
  }
}

/** Проверки пунктов 1–5 для сущности: номер → причина ✗ или null (раздел 3.5, правило 4). */
export class AutoChecks {
  private readonly targets;
  private readonly specs;

  constructor(private readonly bundle: Bundle, private readonly imports: Imports) {
    this.targets = new ComponentsBuilder(bundle).targets();
    this.specs = new FoundationBuilder(bundle).collectionSpecs();
  }

  async run(id: Id, kind: ChecklistScope, warnings: number): Promise<Record<number, string | null>> {
    const builds: Build[] = kind === "foundation" ? ["foundation"] : ["foundation", "components"];
    const result: Record<number, string | null> = {
      1: this.clean(builds),
      2: this.current(builds),
      5: this.documentation(id, kind, warnings),
    };
    if (kind === "component") {
      result[3] = this.component(id);
      result[4] = await this.bound(id);
    }
    if (kind === "foundation") result[3] = await this.foundation(id);
    return result;
  }

  private clean(builds: Build[]): string | null {
    const missing = builds.filter((b) => !this.imports[b]);
    if (missing.length) return `не строилось: ${missing.join(", ")}`;
    const dirty = builds.filter((b) => this.imports[b]!.source.dirty);
    return dirty.length ? `сборка с незакоммиченными изменениями: ${dirty.join(", ")}` : null;
  }

  private current(builds: Build[]): string | null {
    const differ = builds.filter((b) => !this.imports[b] || compare(this.imports[b]!.source, this.bundle.source) !== "совпадает");
    return differ.length ? `сборка не совпадает с открытым набором: ${differ.join(", ")}` : null;
  }

  private documentation(id: Id, kind: ChecklistScope, warnings: number): string | null {
    if (warnings) return `предупреждений при отрисовке документации: ${warnings}`;
    if (kind === "foundation") return null;
    const types = new Set((this.bundle.manifests[id]?.sections ?? []).map((s) => s.type));
    const missing = REQUIRED[kind].filter((type) => !types.has(type));
    return missing.length ? `в манифесте нет секций: ${missing.join(", ")}` : null;
  }

  /** Figma-компоненты сущности на странице Components: вид и variant-свойства (figma-mapping.md, раздел 9, пункт 4). */
  private component(id: Id): string | null {
    const page = figma.root.children.find((p) => ref(p, "role") === "components");
    const problems: string[] = [];
    for (const target of this.targets.filter((t) => t.id === id || t.id.startsWith(`${id}#`))) {
      const node = page?.children.find((n) => ref(n) === target.id);
      const isSet = target.variants[0]?.values.length > 0;
      if (!node) problems.push(`нет Figma-компонента ${target.name}`);
      else if (node.type !== (isSet ? "COMPONENT_SET" : "COMPONENT")) problems.push(`${target.name}: ${node.type} вместо ${isSet ? "COMPONENT_SET" : "COMPONENT"}`);
      else if (node.type === "COMPONENT_SET") {
        const expected = new Set(target.variants.map((v) => v.values.map(([p, value]) => `${p}=${value}`).join(", ")));
        const actual = new Set(node.children.map((n) => n.name));
        const missing = [...expected].filter((name) => !actual.has(name));
        const extra = [...actual].filter((name) => !expected.has(name));
        if (missing.length) problems.push(`${target.name}: нет variant ${missing.join("; ")}`);
        if (extra.length) problems.push(`${target.name}: лишние variant ${extra.join("; ")}`);
      }
    }
    if (!problems.length && !this.targets.some((t) => t.id === id)) problems.push("нет figma.json или образцов");
    return problems.length ? problems.join("; ") : null;
  }

  /** Коллекция foundation-сущности: режимы и переменные вариантов; у Icon — компоненты символов. */
  private async foundation(id: Id): Promise<string | null> {
    const problems: string[] = [];
    const spec = this.specs.find((s) => s.ref === id);
    if (spec) {
      const collection = (await figma.variables.getLocalVariableCollectionsAsync()).find((c) => ref(c) === id);
      if (!collection) return `нет коллекции ${spec.name}`;
      const modes = collection.modes.map((m) => m.name);
      if (modes.join(",") !== spec.modes.join(",")) problems.push(`режимы ${modes.join(", ")} вместо ${spec.modes.join(", ")}`);
      const variables = new Set((await figma.variables.getLocalVariablesAsync())
        .filter((v) => v.variableCollectionId === collection.id).map((v) => ref(v)));
      const missing = spec.variables.filter((v) => !variables.has(v.ref)).map((v) => v.name);
      if (missing.length) problems.push(`нет переменных ${missing.slice(0, 5).join(", ")}${missing.length > 5 ? ` и ещё ${missing.length - 5}` : ""}`);
    }
    if (id === ICON) {
      const page = figma.root.children.find((p) => ref(p) === ICON);
      const components = new Set(page?.findAll((n) => n.type === "COMPONENT").map((n) => ref(n)) ?? []);
      const missing = Object.keys(this.bundle.icons).filter((icon) => !components.has(icon)).map((icon) => icon.split("#")[1]);
      if (missing.length) problems.push(`нет символов ${missing.join(", ")}`);
    }
    if (!spec && id !== ICON) problems.push("сущность не отображается в Figma");
    return problems.length ? problems.join("; ") : null;
  }

  /** Привязки величин в Figma-компонентах сущности; вложенные экземпляры не проверяются. */
  private async bound(id: Id): Promise<string | null> {
    const page = figma.root.children.find((p) => ref(p, "role") === "components");
    const roots = page?.children.filter((n) => ref(n) === id || ref(n).startsWith(`${id}#`)) ?? [];
    if (!roots.length) return "нет Figma-компонента";
    const problems = new Set<string>();
    const visit = (node: SceneNode) => {
      if (node.type === "INSTANCE") return;
      const bound = (node as { boundVariables?: Record<string, unknown> }).boundVariables ?? {};
      for (const key of ["fills", "strokes"] as const) {
        const paints = (node as Partial<GeometryMixin>)[key];
        if (Array.isArray(paints) && paints.some((p: Paint) => p.type === "SOLID" && p.visible !== false && !p.boundVariables?.color)) {
          problems.add(`${node.name}: ${key === "fills" ? "заливка" : "обводка"}`);
        }
      }
      for (const field of NUMBER_FIELDS) {
        const value = (node as unknown as Record<string, unknown>)[field];
        if (field === "strokeWeight" && !(node as Partial<GeometryMixin>).strokes?.length) continue;
        if ((field === "itemSpacing" || field === "counterAxisSpacing") && (node as Partial<AutoLayoutMixin>).layoutMode === "NONE") continue;
        if (field === "itemSpacing" && (node as Partial<AutoLayoutMixin>).primaryAxisAlignItems === "SPACE_BETWEEN") continue;
        if (field === "counterAxisSpacing" && (node as Partial<AutoLayoutMixin>).layoutWrap !== "WRAP") continue;
        if (typeof value === "number" && value > 0 && !bound[field]) problems.add(`${node.name}: ${field}`);
      }
      if (node.type === "TEXT" && node.characters.length) {
        const styled = node.getStyledTextSegments(["textStyleId"]).every((s) => s.textStyleId);
        if (!styled) problems.add(`${node.name}: стиль текста`);
      }
      if ("children" in node) for (const child of node.children) visit(child);
    };
    for (const root of roots) {
      if (root.type === "COMPONENT_SET") for (const variant of root.children) visit(variant);
      else visit(root);
    }
    const list = [...problems];
    return list.length ? `не привязано: ${list.slice(0, 5).join(", ")}${list.length > 5 ? ` и ещё ${list.length - 5}` : ""}` : null;
  }
}

/** Отметки, поставленные не на текущей сборке документации. */
export function stale(checks: Checks, current: Source): boolean {
  return Object.values(checks).some((c) => c.commit !== current.commit);
}

/** Самая старая сборка отметок. */
export function oldest(checks: Checks): Check | undefined {
  return Object.values(checks).sort((a, b) => (a.committedAt ?? "").localeCompare(b.committedAt ?? ""))[0];
}

/** Ручные пункты Release Checklist страницы для окна плагина; без набора или фрейма — null (раздел 3.5, правило 5). */
export function view(bundle: Bundle | undefined, page: PageNode): ChecklistView | null {
  const frame = page.children.find((n) => ref(n, "role") === "checklist");
  const kind = frame && scope(ref(frame));
  if (!bundle || !frame || !kind) return null;
  const checks = readJson<Checks>(frame, CHECKS_KEY);
  return {
    page: page.name,
    items: bundle.checklist.items.filter((item) => item.kind === "manual" && item.appliesTo.includes(kind))
      .map((item) => ({ number: item.number, text: item.text, checked: !!checks[item.number] })),
  };
}
