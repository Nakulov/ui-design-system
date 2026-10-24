// Набор данных плагина: типы и проверка совместимости (specifications/bundle-format.md).

/** Версия bundleFormat, которую поддерживает плагин (раздел 4). */
export const SUPPORTED_FORMAT = "0.1";

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

export type Bundle = {
  specVersions: Record<string, string>;
  theme: string;
  contracts: Record<Id, Contract>;
  values: Record<Id, Values>;
  samples: Record<Id, unknown>;
  icons: Record<Id, Icon>;
  /** Разделы README: заголовок → исходный Markdown. */
  texts: Record<Id, Record<string, string>>;
};

export type Summary = {
  specVersions: Record<string, string>;
  theme: string;
  counts: Record<"contracts" | "values" | "samples" | "icons" | "texts", number>;
};

const SECTIONS = ["contracts", "values", "samples", "icons", "texts"] as const;

/** Совпадает основная версия, а до 1.0 — и дополнительная (раздел 4, правило 1). */
export function isSupported(version: string): boolean {
  const [major, minor] = version.split(".");
  const [supportedMajor, supportedMinor] = SUPPORTED_FORMAT.split(".");
  return major === supportedMajor && (major !== "0" || minor === supportedMinor);
}

/** Проверяет набор; возвращает набор со сводкой или текст ошибки. */
export function readBundle(data: unknown): { bundle: Bundle; summary: Summary } | { error: string } {
  if (typeof data !== "object" || data === null) {
    return { error: "Файл не является набором данных: ожидается объект JSON" };
  }
  const candidate = data as Partial<Bundle>;
  const version = candidate.specVersions?.bundleFormat;
  if (typeof version !== "string") {
    return { error: "В наборе нет specVersions.bundleFormat: файл собран не tools/bundle.py" };
  }
  if (!isSupported(version)) {
    return { error: `Набор формата ${version}, плагин поддерживает ${SUPPORTED_FORMAT}: пересоберите набор или обновите плагин` };
  }
  const missing: string[] = SECTIONS.filter((key) => typeof candidate[key] !== "object" || candidate[key] === null);
  if (typeof candidate.theme !== "string") missing.unshift("theme");
  if (missing.length > 0) {
    return { error: `В наборе нет разделов: ${missing.join(", ")}` };
  }
  const bundle = candidate as Bundle;
  const counts = Object.fromEntries(SECTIONS.map((key) => [key, Object.keys(bundle[key]).length])) as Summary["counts"];
  return { bundle, summary: { specVersions: bundle.specVersions, theme: bundle.theme, counts } };
}
