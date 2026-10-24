// Основной поток плагина: доступ к документу Figma.
import { type Bundle, readBundle } from "./bundle";
import { FoundationBuilder } from "./foundation";
import type { ToPlugin, ToUi } from "./messages";
import type { Report } from "./report";
import { SamplesBuilder } from "./samples";

/** Открытый набор. */
let bundle: Bundle | undefined;

figma.showUI(__html__, { width: 360, height: 560, themeColors: true });

function send(message: ToUi) {
  figma.ui.postMessage(message);
}

figma.ui.onmessage = async (message: ToPlugin) => {
  if (message.type === "load") load(message.text);
  else if (message.type === "buildFoundation") await run("Строится foundation…", "Foundation построен", "Foundation не построен", (b) => new FoundationBuilder(b).build());
  else if (message.type === "renderSamples") await run("Рисуются примеры…", "Примеры отрисованы", "Примеры не отрисованы", (b) => new SamplesBuilder(b).build());
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

function load(text: string) {
  let data: unknown;
  try {
    data = JSON.parse(text);
  } catch (e) {
    send({ type: "error", message: `Некорректный JSON: ${(e as Error).message}` });
    return;
  }
  const result = readBundle(data);
  if ("error" in result) {
    bundle = undefined;
    send({ type: "error", message: result.error });
    return;
  }
  bundle = result.bundle;
  send({ type: "loaded", summary: result.summary });
}
