// Окно плагина: выбор набора, сводка, построение foundation и компонентов, отрисовка примеров и отчёт.
import type { Summary } from "./bundle";
import type { ToPlugin, ToUi } from "./messages";
import type { ReportData } from "./report";

const COUNTS: Record<keyof Summary["counts"], string> = {
  contracts: "Контракты",
  values: "Файлы значений",
  figma: "Отображение в Figma",
  samples: "Примеры",
  icons: "Символы",
  texts: "README",
  documentation: "Элементы документации",
  manifests: "Манифесты документации",
};

const element = <T extends HTMLElement>(id: string) => document.getElementById(id) as T;
const file = element<HTMLInputElement>("file");
const error = element<HTMLDivElement>("error");
const status = element<HTMLDivElement>("status");
const summary = element<HTMLTableElement>("summary");
const report = element<HTMLDivElement>("report");
const foundation = element<HTMLButtonElement>("foundation");
const components = element<HTMLButtonElement>("components");
const samples = element<HTMLButtonElement>("samples");
const documentation = element<HTMLButtonElement>("documentation");

function send(message: ToPlugin) {
  parent.postMessage({ pluginMessage: message }, "*");
}

function row(table: HTMLTableElement, label: string, value: string | number) {
  const tr = table.insertRow();
  const th = document.createElement("th");
  th.textContent = label;
  tr.append(th);
  tr.insertCell().textContent = String(value);
}

function showSummary(data: Summary) {
  summary.replaceChildren();
  row(summary, "Тема", data.theme);
  for (const [key, label] of Object.entries(COUNTS)) row(summary, label, data.counts[key as keyof Summary["counts"]]);
  for (const [spec, version] of Object.entries(data.specVersions)) row(summary, spec, version);
  summary.hidden = false;
}

function showReport(title: string, data: ReportData) {
  const heading = document.createElement("h2");
  heading.textContent = title;
  const table = document.createElement("table");
  for (const [kind, c] of Object.entries(data.counts)) {
    const parts = [
      c.created && `создано ${c.created}`, c.updated && `обновлено ${c.updated}`,
      c.deleted && `удалено ${c.deleted}`, c.unchanged && `без изменений ${c.unchanged}`,
    ].filter(Boolean);
    row(table, kind, parts.join(", "));
  }
  const list = document.createElement("ul");
  for (const warning of data.warnings) {
    const item = document.createElement("li");
    item.textContent = warning;
    list.append(item);
  }
  report.replaceChildren(heading, table, ...(data.warnings.length ? [list] : []));
  report.hidden = false;
}

element<HTMLButtonElement>("open").onclick = () => file.click();
foundation.onclick = () => send({ type: "buildFoundation" });
components.onclick = () => send({ type: "buildComponents" });
samples.onclick = () => send({ type: "renderSamples" });
documentation.onclick = () => send({ type: "buildDocumentation" });

file.onchange = async () => {
  const chosen = file.files?.[0];
  file.value = "";
  if (chosen) send({ type: "load", text: await chosen.text() });
};

onmessage = (event: MessageEvent<{ pluginMessage: ToUi }>) => {
  const message = event.data.pluginMessage;
  error.hidden = message.type !== "error";
  status.hidden = message.type !== "busy";
  foundation.disabled = components.disabled = samples.disabled = documentation.disabled = message.type === "busy" || (message.type === "error" && summary.hidden === true);
  if (message.type === "error") {
    error.textContent = message.message;
  } else if (message.type === "busy") {
    status.textContent = message.message;
  } else if (message.type === "loaded") {
    report.hidden = true;
    showSummary(message.summary);
    foundation.disabled = components.disabled = samples.disabled = documentation.disabled = false;
  } else {
    showReport(message.title, message.report);
  }
};
