// Сообщения между окном плагина и основным потоком.
import type { Origin, Summary } from "./bundle";
import type { ReportData } from "./report";

export type ToPlugin = { type: "load"; text: string; origin: Origin } | { type: "saveUrl"; url: string } | { type: "buildFoundation" } | { type: "renderSamples" } | { type: "renderScreens" } | { type: "buildComponents" } | { type: "buildDocumentation" };

export type ToUi =
  | { type: "url"; url: string }
  | { type: "loaded"; summary: Summary }
  | { type: "error"; message: string }
  | { type: "busy"; message: string }
  | { type: "report"; title: string; report: ReportData };
