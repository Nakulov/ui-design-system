// Основной поток плагина: доступ к документу Figma.
import { type Bundle, type Origin, readBundle } from "./bundle";
import { type Checks, view } from "./checklist";
import { ComponentsBuilder } from "./components";
import { DocumentationBuilder } from "./documentation";
import { FoundationBuilder } from "./foundation";
import { readImports, writeImport } from "./imports";
import type { Build, ToPlugin, ToUi } from "./messages";
import type { Report } from "./report";
import { SamplesBuilder, SCREENS } from "./samples";

/** Открытый набор. */
let bundle: Bundle | undefined;

/** Ключ clientStorage с адресом набора, изменённым пользователем (bundle-format.md, раздел 6). */
const URL_KEY = "bundleUrl";

figma.showUI(__html__, { width: 360, height: 560, themeColors: true });
figma.clientStorage.getAsync(URL_KEY).then((url) => send({ type: "url", url: typeof url === "string" ? url : "" }));
send({ type: "imports", imports: readImports() });
sendChecklist();
figma.on("currentpagechange", sendChecklist);

function send(message: ToUi) {
  figma.ui.postMessage(message);
}

/** Ручные пункты Release Checklist открытой страницы (figma-documentation.md, раздел 3.5). */
function sendChecklist() {
  send({ type: "checklist", checklist: view(bundle, figma.currentPage) });
}

figma.ui.onmessage = async (message: ToPlugin) => {
  if (message.type === "load") load(message.text, message.origin);
  else if (message.type === "saveUrl") await (message.url ? figma.clientStorage.setAsync(URL_KEY, message.url) : figma.clientStorage.deleteAsync(URL_KEY));
  else if (message.type === "buildFoundation") await run("foundation", "Строится foundation…", "Foundation построен", "Foundation не построен", (b) => new FoundationBuilder(b).build());
  else if (message.type === "buildComponents") await run("components", "Строятся компоненты…", "Компоненты построены", "Компоненты не построены", (b) => new ComponentsBuilder(b).build());
  else if (message.type === "buildDocumentation") await run("documentation", "Строится документация…", "Документация построена", "Документация не построена", (b) => new DocumentationBuilder(b).build());
  else if (message.type === "renderSamples") await run("samples", "Рисуются образцы…", "Образцы отрисованы", "Образцы не отрисованы", (b) => new SamplesBuilder(b).build());
  else if (message.type === "renderScreens") await run("screens", "Рисуются экраны…", "Экраны отрисованы", "Экраны не отрисованы", (b) => new SamplesBuilder(b, SCREENS).build());
  else if (message.type === "toggleCheck") {
    const key = String(message.number);
    await check((checks, current) => {
      const { [key]: old, ...rest } = checks;
      return old ? rest : { ...checks, [key]: current };
    });
  } else if (message.type === "resetChecks") await check(() => ({}));
};

/** Меняет ручные отметки чеклиста открытой страницы и перерисовывает фрейм. */
async function check(change: (checks: Checks, current: Checks[string]) => Checks) {
  if (!bundle) return;
  try {
    await new DocumentationBuilder(bundle).updateChecks(figma.currentPage, change);
  } catch (e) {
    send({ type: "error", message: `Отметки не изменены: ${(e as Error).message}` });
  }
  sendChecklist();
}

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
  sendChecklist();
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
    sendChecklist();
    return;
  }
  bundle = result.bundle;
  send({ type: "loaded", summary: result.summary });
  sendChecklist();
}
