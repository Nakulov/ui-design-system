// Сообщения между окном плагина и основным потоком.
import type { Summary } from "./bundle";

export type ToPlugin = { type: "load"; text: string };

export type ToUi = { type: "loaded"; summary: Summary } | { type: "error"; message: string };
