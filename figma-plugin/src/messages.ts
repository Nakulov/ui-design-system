// Сообщения между окном плагина и основным потоком.
import type { Summary } from "./bundle";
import type { ReportData } from "./report";

export type ToPlugin = { type: "load"; text: string } | { type: "buildFoundation" } | { type: "renderSamples" };

export type ToUi =
  | { type: "loaded"; summary: Summary }
  | { type: "error"; message: string }
  | { type: "busy"; message: string }
  | { type: "report"; title: string; report: ReportData };
