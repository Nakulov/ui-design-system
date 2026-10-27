// Набор данных плагина: типы и проверка совместимости (specifications/bundle-format.md).

/** Версия bundleFormat, которую поддерживает плагин (раздел 4). */
export const SUPPORTED_FORMAT = "0.12";

/** Адрес набора на GitHub Pages по умолчанию (раздел 6). */
export const DEFAULT_URL = "https://nakulov.github.io/ui-design-system/bundle.json";

/** Откуда открыт набор: с GitHub или из файла (раздел 6). */
export type Origin = "github" | "file";

/** Идентификатор сущности: путь от корня репозитория без .json; вариант — после «#» (раздел 2). */
export type Id = string;

/** Контракт с идентификаторами вместо относительных $ref и extends (раздел 3.1). */
export type Contract = {
  name: string;
  type: string;
  description: string;
  extends?: Id;
  [field: string]: unknown;
};

/** Алиас на вариант другого контракта; режим цели — по schema-format.md, раздел 9.3, правило 2. */
export type Alias = { alias: Id };

export type ValueMode = { name: string; values: Record<string, unknown> };

export type Values = {
  valueType: string | { $ref: Id };
  /** Первый режим — режим по умолчанию. */
  modes: ValueMode[];
};

export type Icon = { svg: string; mirrored: boolean };

/** Содержимое figma.json без $ref (раздел 3.6); у foundation-сущности — figma-mapping.md, раздел 6.3. */
export type FigmaProject = { scopes?: string[]; publish?: boolean; [field: string]: unknown };

/** Состояние репозитория, из которого собран набор (раздел 3.11); вне Git — null. */
export type Source = { commit: string | null; committedAt: string | null; branch: string | null; dirty: boolean };

export type Bundle = {
  specVersions: Record<string, string>;
  source: Source;
  theme: string;
  contracts: Record<Id, Contract>;
  values: Record<Id, Values>;
  figma: Record<Id, FigmaProject>;
  samples: Record<Id, unknown>;
  /** Демо-экраны по имени файла (раздел 3.9). */
  screens: Record<string, unknown>;
  icons: Record<Id, Icon>;
  /** Разделы README: заголовок → исходный Markdown. */
  texts: Record<Id, Record<string, string>>;
  /** Элементы оформления документации, превью и демонстрации (раздел 3.7). */
  documentation: Record<Id, Element>;
  /** Манифесты документации по идентификатору контракта (раздел 3.8). */
  manifests: Record<Id, Manifest>;
  /** Журналы изменений по идентификатору публичного контракта (раздел 3.10). */
  changelogs: Record<Id, Changelog>;
  /** Пункты чеклиста готовности к релизу в Figma (раздел 3.12). */
  checklist: Checklist;
};

/** К чему применим пункт чеклиста (figma-mapping.md, раздел 8.5). */
export type ChecklistScope = "component" | "container" | "foundation" | "core";

export type ChecklistItem = { number: number; kind: "auto" | "manual"; text: string; appliesTo: ChecklistScope[] };

export type Checklist = { items: ChecklistItem[] };

/** Запись журнала изменений (schema-format.md, раздел 11.4); from — только у renamed. */
export type ChangelogEntry = {
  date: string;
  issue?: number;
  kind: "added" | "changed" | "removed" | "renamed";
  target?: string;
  from?: string;
  description: string;
};

export type Changelog = { entries: ChangelogEntry[] };

/** Пример секции: демонстрация core или пример компонента по идентификатору и подпись (documentation-manifest.md, раздел 5). */
export type ManifestExample = { id: string; demo?: Id; sample?: Id; caption?: string };

export type ManifestSection = { id: string; type: string; title?: string; examples?: ManifestExample[] };

export type Manifest = { sections: ManifestSection[] };

export type HoleKind = "text" | "spans" | "component" | "items";

/** Элемент оформления документации: шаблон-модель с дырками (figma-documentation.md, раздел 5). */
export type Element = { holes: Record<string, { kind: HoleKind; sample: unknown }>; model: unknown };

export type Summary = {
  specVersions: Record<string, string>;
  source: Source;
  theme: string;
  counts: Record<(typeof SECTIONS)[number], number>;
};

const SECTIONS = ["contracts", "values", "figma", "samples", "screens", "icons", "texts", "documentation", "manifests", "changelogs", "checklist"] as const;

/** Сравнение сборки в файле с открытым набором (figma-mapping.md, раздел 7, правило 11). */
export function compare(built: Source, bundle: Source): string {
  if (built.dirty || bundle.dirty || !built.commit || !bundle.commit) return "нельзя сравнить";
  if (built.commit === bundle.commit) return "совпадает";
  const difference = Date.parse(built.committedAt ?? "") - Date.parse(bundle.committedAt ?? "");
  if (!difference) return "нельзя сравнить";
  return difference < 0 ? "отстаёт" : "новее набора";
}

/** Совпадает основная версия, а до 1.0 — и дополнительная (раздел 4, правило 1). */
export function isSupported(version: string): boolean {
  const [major, minor] = version.split(".");
  const [supportedMajor, supportedMinor] = SUPPORTED_FORMAT.split(".");
  return major === supportedMajor && (major !== "0" || minor === supportedMinor);
}

/** Проверяет набор; возвращает набор со сводкой или текст ошибки. */
export function readBundle(data: unknown, origin: Origin): { bundle: Bundle; summary: Summary } | { error: string } {
  if (typeof data !== "object" || data === null) {
    return { error: "Файл не является набором данных: ожидается объект JSON" };
  }
  const candidate = data as Partial<Bundle>;
  const version = candidate.specVersions?.bundleFormat;
  if (typeof version !== "string") {
    return { error: "В наборе нет specVersions.bundleFormat: файл собран не tools/bundle.py" };
  }
  if (!isSupported(version)) {
    // Набор с GitHub всегда из актуального dev: отстаёт плагин (раздел 4, правило 2).
    const fix = origin === "github" ? "обновите плагин (git pull, npm run build)" : "пересоберите набор или обновите плагин";
    return { error: `Набор формата ${version}, плагин поддерживает ${SUPPORTED_FORMAT}: ${fix}` };
  }
  const missing: string[] = SECTIONS.filter((key) => typeof candidate[key] !== "object" || candidate[key] === null);
  if (typeof candidate.theme !== "string") missing.unshift("theme");
  if (typeof candidate.source !== "object" || candidate.source === null) missing.unshift("source");
  if (missing.length > 0) {
    return { error: `В наборе нет разделов: ${missing.join(", ")}` };
  }
  const bundle = candidate as Bundle;
  if (!Array.isArray(bundle.checklist.items)) return { error: "В наборе нет пунктов чеклиста: checklist.items" };
  // У чеклиста считаются пункты, у остальных разделов — ключи.
  const counts = Object.fromEntries(SECTIONS.map((key) => [key, key === "checklist" ? bundle.checklist.items.length : Object.keys(bundle[key]).length])) as Summary["counts"];
  return { bundle, summary: { specVersions: bundle.specVersions, source: bundle.source, theme: bundle.theme, counts } };
}
