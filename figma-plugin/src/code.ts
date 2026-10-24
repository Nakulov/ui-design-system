// Основной поток плагина: доступ к документу Figma.
import { type Bundle, readBundle } from "./bundle";
import type { ToPlugin, ToUi } from "./messages";

/** Открытый набор; объекты Figma из него строят следующие задачи этапа 1. */
let bundle: Bundle | undefined;

figma.showUI(__html__, { width: 360, height: 420, themeColors: true });

function send(message: ToUi) {
  figma.ui.postMessage(message);
}

figma.ui.onmessage = (message: ToPlugin) => {
  if (message.type !== "load") return;
  let data: unknown;
  try {
    data = JSON.parse(message.text);
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
};
