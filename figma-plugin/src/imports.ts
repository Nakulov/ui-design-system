// Метки сборок на корне документа (figma-mapping.md, раздел 7, правило 11).
import { mark, ref } from "./ids";
import type { Build, Import, Imports } from "./messages";

const IMPORTS_KEY = "imports";
const BUILDS: Build[] = ["foundation", "components", "documentation", "samples", "screens"];

function isImport(value: unknown): value is Import {
  if (typeof value !== "object" || value === null) return false;
  const { source, importedAt } = value as Record<string, unknown>;
  if (typeof source !== "object" || source === null || typeof importedAt !== "string") return false;
  const { commit, committedAt, branch, dirty } = source as Record<string, unknown>;
  const text = (field: unknown) => field === null || typeof field === "string";
  return text(commit) && text(committedAt) && text(branch) && typeof dirty === "boolean";
}

/** Повреждённое значение или метка не по формату — «не строилось». */
export function readImports(): Imports {
  let data: unknown;
  try {
    data = JSON.parse(ref(figma.root, IMPORTS_KEY) || "{}");
  } catch {
    return {};
  }
  const imports: Imports = {};
  if (typeof data !== "object" || data === null) return imports;
  for (const key of BUILDS) {
    const value = (data as Record<string, unknown>)[key];
    if (isImport(value)) imports[key] = value;
  }
  return imports;
}

/** Меняет только метку своей сборки. */
export function writeImport(key: Build, value: Import): Imports {
  const imports = { ...readImports(), [key]: value };
  mark(figma.root, JSON.stringify(imports), IMPORTS_KEY);
  return imports;
}
