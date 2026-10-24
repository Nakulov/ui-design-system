// Основной поток плагина: доступ к документу Figma.
import { type Bundle, readBundle } from "./bundle";
import { FoundationBuilder } from "./foundation";
import type { ToPlugin, ToUi } from "./messages";

/** Открытый набор. */
let bundle: Bundle | undefined;

figma.showUI(__html__, { width: 360, height: 560, themeColors: true });

function send(message: ToUi) {
  figma.ui.postMessage(message);
}

figma.ui.onmessage = async (message: ToPlugin) => {
  if (message.type === "load") load(message.text);
  else if (message.type === "buildFoundation") await buildFoundation();
};

async function buildFoundation() {
  if (!bundle) {
    send({ type: "error", message: "Сначала откройте набор данных" });
    return;
  }
  send({ type: "busy", message: "Строится foundation…" });
  try {
    const report = await new FoundationBuilder(bundle).build();
    send({ type: "report", title: "Foundation построен", report: report.data() });
    figma.notify("Foundation построен");
  } catch (e) {
    send({ type: "error", message: `Foundation не построен: ${(e as Error).message}` });
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
