// Окно плагина: загрузка набора с GitHub или из файла, метки сборок в файле, сводка, построение foundation и компонентов, отрисовка примеров и экранов, ручные отметки Release Checklist, отчёт.
import { compare, DEFAULT_URL, type Source, type Summary } from "./bundle";
import type { Build, ChecklistView, Imports, ToPlugin, ToUi } from "./messages";
import type { ReportData } from "./report";

const COUNTS: Record<keyof Summary["counts"], string> = {
  contracts: "Контракты",
  values: "Файлы значений",
  figma: "Отображение в Figma",
  samples: "Примеры",
  screens: "Экраны",
  icons: "Символы",
  texts: "README",
  documentation: "Элементы документации",
  manifests: "Манифесты документации",
  changelogs: "Журналы изменений",
  checklist: "Пункты чеклиста",
};

const BUILDS: Record<Build, string> = {
  foundation: "Foundation",
  components: "Компоненты",
  documentation: "Документация",
  samples: "Примеры",
  screens: "Экраны",
};

const element = <T extends HTMLElement>(id: string) => document.getElementById(id) as T;
const file = element<HTMLInputElement>("file");
const url = element<HTMLInputElement>("url");
const error = element<HTMLDivElement>("error");
const status = element<HTMLDivElement>("status");
const summary = element<HTMLTableElement>("summary");
const importsTable = element<HTMLTableElement>("imports");
const report = element<HTMLDivElement>("report");
const foundation = element<HTMLButtonElement>("foundation");
const components = element<HTMLButtonElement>("components");
const samples = element<HTMLButtonElement>("samples");
const screens = element<HTMLButtonElement>("screens");
const documentation = element<HTMLButtonElement>("documentation");
const checklist = element<HTMLDivElement>("checklist");
const checklistHint = element<HTMLParagraphElement>("checklistHint");
const checklistItems = element<HTMLDivElement>("checklistItems");

/** Метки сборок в файле и источник открытого набора, с которым они сравниваются. */
let imports: Imports = {};
let opened: Source | undefined;

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

/** «dev · 48bcb86 · 26.10.2026 11:40»: время коммита как записано, без перевода в пояс пользователя (bundle-format.md, раздел 3.11). */
function sourceText(source: Source): string {
  const time = source.committedAt?.match(/^(\d{4})-(\d{2})-(\d{2})T(\d{2}:\d{2})/);
  const parts = [
    source.branch,
    source.commit?.slice(0, 7) ?? "вне Git",
    time && `${time[3]}.${time[2]}.${time[1]} ${time[4]}`,
    source.dirty && "есть незакоммиченные изменения",
  ];
  return parts.filter(Boolean).join(" · ");
}

function showImports() {
  importsTable.replaceChildren();
  for (const [key, label] of Object.entries(BUILDS)) {
    const value = imports[key as Build];
    const text = value ? sourceText(value.source) : "не строилось";
    row(importsTable, label, value && opened ? `${text} · ${compare(value.source, opened)}` : text);
    if (value) importsTable.rows[importsTable.rows.length - 1].title = `Собрано ${new Date(value.importedAt).toLocaleString("ru-RU")}`;
  }
}

function showSummary(data: Summary) {
  summary.replaceChildren();
  row(summary, "Источник", sourceText(data.source));
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

/** Ручные пункты Release Checklist открытой страницы с переключателями (figma-documentation.md, раздел 3.5). */
function showChecklist(view: ChecklistView | null) {
  checklist.hidden = !view;
  checklistHint.hidden = !!view;
  checklistItems.replaceChildren(...(view?.items ?? []).map((item) => {
    const label = document.createElement("label");
    label.className = "check";
    const box = document.createElement("input");
    box.type = "checkbox";
    box.checked = item.checked;
    box.onchange = () => send({ type: "toggleCheck", number: item.number });
    // Разметка Markdown в окне не нужна: код в строке показывается без обратных кавычек.
    label.append(box, `${item.number}. ${item.text.replace(/`/g, "")}`);
    return label;
  }));
}

url.placeholder = DEFAULT_URL;
url.onchange = () => send({ type: "saveUrl", url: url.value.trim() });

element<HTMLButtonElement>("github").onclick = async () => {
  const address = url.value.trim() || DEFAULT_URL;
  show({ type: "busy", message: "Загружается набор…" });
  let response: Response;
  try {
    response = await fetch(address, { cache: "no-store" });
  } catch {
    show({ type: "error", message: `Не удалось загрузить набор: нет сети или сервер не разрешает загрузку (CORS) — ${address}` });
    return;
  }
  if (!response.ok) {
    show({ type: "error", message: `Не удалось загрузить набор: HTTP ${response.status} — ${address}` });
    return;
  }
  send({ type: "load", text: await response.text(), origin: "github" });
};
element<HTMLButtonElement>("open").onclick = () => file.click();
foundation.onclick = () => send({ type: "buildFoundation" });
components.onclick = () => send({ type: "buildComponents" });
samples.onclick = () => send({ type: "renderSamples" });
screens.onclick = () => send({ type: "renderScreens" });
documentation.onclick = () => send({ type: "buildDocumentation" });
element<HTMLButtonElement>("resetChecks").onclick = () => send({ type: "resetChecks" });

file.onchange = async () => {
  const chosen = file.files?.[0];
  file.value = "";
  if (chosen) send({ type: "load", text: await chosen.text(), origin: "file" });
};

function show(message: ToUi) {
  if (message.type === "url") {
    url.value = message.url;
    return;
  }
  if (message.type === "checklist") {
    showChecklist(message.checklist);
    return;
  }
  if (message.type === "imports") {
    imports = message.imports;
    showImports();
    return;
  }
  error.hidden = message.type !== "error";
  status.hidden = message.type !== "busy";
  foundation.disabled = components.disabled = samples.disabled = screens.disabled = documentation.disabled = message.type === "busy" || (message.type === "error" && summary.hidden === true);
  if (message.type === "error") {
    error.textContent = message.message;
  } else if (message.type === "busy") {
    status.textContent = message.message;
  } else if (message.type === "loaded") {
    report.hidden = true;
    opened = message.summary.source;
    showImports();
    showSummary(message.summary);
    foundation.disabled = components.disabled = samples.disabled = screens.disabled = documentation.disabled = false;
  } else {
    showReport(message.title, message.report);
  }
}

onmessage = (event: MessageEvent<{ pluginMessage: ToUi }>) => show(event.data.pluginMessage);
