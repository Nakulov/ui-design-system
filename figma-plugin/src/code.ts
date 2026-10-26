// Основной поток плагина: доступ к документу Figma.
import { type Bundle, type Origin, readBundle } from "./bundle";
import { ComponentsBuilder } from "./components";
import { DocumentationBuilder } from "./documentation";
import { FoundationBuilder } from "./foundation";
import type { ToPlugin, ToUi } from "./messages";
import type { Report } from "./report";
import { SamplesBuilder, SCREENS } from "./samples";

/** Открытый набор. */
let bundle: Bundle | undefined;

/** Ключ clientStorage с адресом набора, изменённым пользователем (bundle-format.md, раздел 6). */
const URL_KEY = "bundleUrl";

figma.showUI(__html__, { width: 360, height: 560, themeColors: true });
figma.clientStorage.getAsync(URL_KEY).then((url) => send({ type: "url", url: typeof url === "string" ? url : "" }));

function send(message: ToUi) {
  figma.ui.postMessage(message);
}

figma.ui.onmessage = async (message: ToPlugin) => {
  if (message.type === "load") load(message.text, message.origin);
  else if (message.type === "saveUrl") await (message.url ? figma.clientStorage.setAsync(URL_KEY, message.url) : figma.clientStorage.deleteAsync(URL_KEY));
  else if (message.type === "buildFoundation") await run("Строится foundation…", "Foundation построен", "Foundation не построен", (b) => new FoundationBuilder(b).build());
  else if (message.type === "buildComponents") await run("Строятся компоненты…", "Компоненты построены", "Компоненты не построены", (b) => new ComponentsBuilder(b).build());
  else if (message.type === "buildDocumentation") await run("Строится документация…", "Документация построена", "Документация не построена", (b) => new DocumentationBuilder(b).build());
  else if (message.type === "renderSamples") await run("Рисуются примеры…", "Примеры отрисованы", "Примеры не отрисованы", (b) => new SamplesBuilder(b).build());
  else if (message.type === "renderScreens") await run("Рисуются экраны…", "Экраны отрисованы", "Экраны не отрисованы", (b) => new SamplesBuilder(b, SCREENS).build());
};

async function run(busy: string, done: string, failed: string, build: (bundle: Bundle) => Promise<Report>) {
  if (!bundle) {
    send({ type: "error", message: "Сначала откройте набор данных" });
    return;
  }
  send({ type: "busy", message: busy });
  try {
    const report = await build(bundle);
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
