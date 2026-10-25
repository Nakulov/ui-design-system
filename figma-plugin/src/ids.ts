// Идентификаторы объектов Figma и общие преобразования (specifications/figma-mapping.md, разделы 3.1, 7.1).

export const NAMESPACE = "uiContracts";

export const COLOR = "foundation/colorRole/ColorRole";
export const SPACING = "foundation/spacing/Spacing";
export const SIZE = "foundation/size/Size";
export const CORNER_RADIUS = "foundation/cornerRadius/CornerRadius";
export const BORDER_WIDTH = "foundation/borderWidth/BorderWidth";
export const ELEVATION = "foundation/elevation/Elevation";
export const TYPOGRAPHY = "foundation/typographyRole/TypographyRole";
export const ICON = "foundation/icon/Icon";

type PluginData = { getSharedPluginData(namespace: string, key: string): string };
type WritablePluginData = { setSharedPluginData(namespace: string, key: string, value: string): void };

export function ref(node: PluginData, key = "ref"): string {
  return node.getSharedPluginData(NAMESPACE, key);
}

export function mark(node: WritablePluginData, value: string, key = "ref") {
  node.setSharedPluginData(NAMESPACE, key, value);
}

/** Главный контракт сущности: `<слой>/<сущность>/<Имя>`, имя — имя каталога сущности с заглавной (figma-mapping.md, раздел 7.1). */
export function isMain(id: string): boolean {
  const [, entity, name] = id.split("/");
  return entity !== undefined && name === entity[0].toUpperCase() + entity.slice(1);
}

/** Префикс имён в Figma: имя контракта в lowerCamelCase без суффикса Role (`spacing/xs2`, `color/primary`). */
export function prefix(contractName: string): string {
  const name = contractName.replace(/Role$/, "");
  return name[0].toLowerCase() + name.slice(1);
}

export function rgba(hex: string): RGBA {
  const channel = (i: number) => parseInt(hex.slice(i, i + 2), 16) / 255;
  return { r: channel(1), g: channel(3), b: channel(5), a: hex.length > 7 ? channel(7) : 1 };
}
