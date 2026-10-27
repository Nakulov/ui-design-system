// Сообщения между окном плагина и основным потоком.
import type { Origin, Source, Summary } from "./bundle";
import type { ReportData } from "./report";

export type ToPlugin = { type: "load"; text: string; origin: Origin } | { type: "saveUrl"; url: string } | { type: "buildFoundation" } | { type: "renderSamples" } | { type: "renderScreens" } | { type: "buildComponents" } | { type: "buildDocumentation" } | { type: "toggleCheck"; number: number } | { type: "resetChecks" };

/** Сборки плагина; у каждой своя метка в файле (figma-mapping.md, раздел 7, правило 11). */
export type Build = "foundation" | "components" | "documentation" | "samples" | "screens";

/** Метка сборки: из какого набора и когда она сделана. */
export type Import = { source: Source; importedAt: string };

/** Метки сборок в файле; сборки без метки — «не строилось». */
export type Imports = Partial<Record<Build, Import>>;

/** Ручные пункты Release Checklist открытой страницы (figma-documentation.md, раздел 3.5). */
export type ChecklistView = { page: string; items: { number: number; text: string; checked: boolean }[] };

export type ToUi =
  | { type: "url"; url: string }
  | { type: "imports"; imports: Imports }
  | { type: "loaded"; summary: Summary }
  | { type: "error"; message: string }
  | { type: "busy"; message: string }
  | { type: "report"; title: string; report: ReportData }
  | { type: "checklist"; checklist: ChecklistView | null };
