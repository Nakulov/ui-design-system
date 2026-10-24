// Окно плагина: выбор набора и сводка.
import type { Summary } from "./bundle";
import type { ToPlugin, ToUi } from "./messages";

const COUNTS: Record<keyof Summary["counts"], string> = {
  contracts: "Контракты",
  values: "Файлы значений",
  samples: "Примеры",
  icons: "Символы",
  texts: "README",
};

const element = <T extends HTMLElement>(id: string) => document.getElementById(id) as T;
const file = element<HTMLInputElement>("file");
const error = element<HTMLDivElement>("error");
const summary = element<HTMLTableElement>("summary");

function send(message: ToPlugin) {
  parent.postMessage({ pluginMessage: message }, "*");
}

function row(label: string, value: string | number) {
  const tr = summary.insertRow();
  const th = document.createElement("th");
  th.textContent = label;
  tr.append(th);
  tr.insertCell().textContent = String(value);
}

function show(data: Summary) {
  summary.replaceChildren();
  row("Тема", data.theme);
  for (const [key, label] of Object.entries(COUNTS)) row(label, data.counts[key as keyof Summary["counts"]]);
  for (const [spec, version] of Object.entries(data.specVersions)) row(spec, version);
  summary.hidden = false;
}

element<HTMLButtonElement>("open").onclick = () => file.click();

file.onchange = async () => {
  const chosen = file.files?.[0];
  file.value = "";
  if (chosen) send({ type: "load", text: await chosen.text() });
};

onmessage = (event: MessageEvent<{ pluginMessage: ToUi }>) => {
  const message = event.data.pluginMessage;
  error.hidden = message.type !== "error";
  if (message.type === "error") {
    error.textContent = message.message;
    summary.hidden = true;
  } else {
    show(message.summary);
  }
};
