// Основной поток плагина: доступ к документу Figma.
import { type Bundle, type Origin, readBundle } from "./bundle";
import { ComponentsBuilder } from "./components";
import { DocumentationBuilder } from "./documentation";
import { FoundationBuilder } from "./foundation";
import { mark, ref } from "./ids";
import type { Build, Import, Imports, ToPlugin, ToUi } from "./messages";
import type { Report } from "./report";
import { SamplesBuilder, SCREENS } from "./samples";

/** Открытый набор. */
let bundle: Bundle | undefined;

/** Ключ clientStorage с адресом набора, изменённым пользователем (bundle-format.md, раздел 6). */
const URL_KEY = "bundleUrl";

/** Метки сборок на корне документа (figma-mapping.md, раздел 7, правило 11). */
const IMPORTS_KEY = "imports";
const BUILDS: Build[] = ["foundation", "components", "documentation", "samples", "screens"];

figma.showUI(__html__, { width: 360, height: 560, themeColors: true });
figma.clientStorage.getAsync(URL_KEY).then((url) => send({ type: "url", url: typeof url === "string" ? url : "" }));
send({ type: "imports", imports: readImports() });

function send(message: ToUi) {
  figma.ui.postMessage(message);
}

figma.ui.onmessage = async (message: ToPlugin) => {
  if (message.type === "load") load(message.text, message.origin);
  else if (message.type === "saveUrl") await (message.url ? figma.clientStorage.setAsync(URL_KEY, message.url) : figma.clientStorage.deleteAsync(URL_KEY));
  else if (message.type === "buildFoundation") await run("foundation", "Строится foundation…", "Foundation построен", "Foundation не построен", (b) => new FoundationBuilder(b).build());
  else if (message.type === "buildComponents") await run("components", "Строятся компоненты…", "Компоненты построены", "Компоненты не построены", (b) => new ComponentsBuilder(b).build());
  else if (message.type === "buildDocumentation") await run("documentation", "Строится документация…", "Документация построена", "Документация не построена", (b) => new DocumentationBuilder(b).build());
  else if (message.type === "renderSamples") await run("samples", "Рисуются примеры…", "Примеры отрисованы", "Примеры не отрисованы", (b) => new SamplesBuilder(b).build());
  else if (message.type === "renderScreens") await run("screens", "Рисуются экраны…", "Экраны отрисованы", "Экраны не отрисованы", (b) => new SamplesBuilder(b, SCREENS).build());
};

async function run(key: Build, busy: string, done: string, failed: string, build: (bundle: Bundle) => Promise<Report>) {
  if (!bundle) {
    send({ type: "error", message: "Сначала откройте набор данных" });
    return;
  }
  send({ type: "busy", message: busy });
  try {
    const report = await build(bundle);
    send({ type: "imports", imports: writeImport(key, { source: bundle.source, importedAt: new Date().toISOString() }) });
    send({ type: "report", title: done, report: report.data() });
    figma.notify(done);
  } catch (e) {
    send({ type: "error", message: `${failed}: ${(e as Error).message}` });
  }
}

function load(text: string, origin: Origin) {
  let data: unknown;
  try {
    data = JSON.parse(text);
  } catch (e) {
    send({ type: "error", message: `Некорректный JSON: ${(e as Error).message}` });
    return;
  }
  const result = readBundle(data, origin);
  if ("error" in result) {
    bundle = undefined;
    send({ type: "error", message: result.error });
    return;
  }
  bundle = result.bundle;
  send({ type: "loaded", summary: result.summary });
}


function isImport(value: unknown): value is Import {
  if (typeof value !== "object" || value === null) return false;
  const { source, importedAt } = value as Record<string, unknown>;
  if (typeof source !== "object" || source === null || typeof importedAt !== "string") return false;
  const { commit, committedAt, branch, dirty } = source as Record<string, unknown>;
  const text = (field: unknown) => field === null || typeof field === "string";
  return text(commit) && text(committedAt) && text(branch) && typeof dirty === "boolean";
}

/** Повреждённое значение или метка не по формату — «не строилось». */
function readImports(): Imports {
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
function writeImport(key: Build, value: Import): Imports {
  const imports = { ...readImports(), [key]: value };
  mark(figma.root, JSON.stringify(imports), IMPORTS_KEY);
  return imports;
}
