// Сборка плагина: основной поток в dist/code.js, окно — dist/ui.html со встроенным скриптом.
import * as esbuild from "esbuild";
import { mkdir, readFile, writeFile } from "node:fs/promises";

const watch = process.argv.includes("--watch");
const common = { bundle: true, target: "es2020", logLevel: "info" };

async function ui() {
  const result = await esbuild.build({ ...common, entryPoints: ["src/ui.ts"], write: false, format: "iife" });
  const html = await readFile("src/ui.html", "utf8");
  // Окно плагина загружается строкой: внешние файлы недоступны, скрипт встраивается.
  const script = result.outputFiles[0].text.replace(/<\/script/g, "<\\/script");
  await writeFile("dist/ui.html", html.replace("<!-- script -->", `<script>${script}</script>`));
}

await mkdir("dist", { recursive: true });
const code = { ...common, entryPoints: ["src/code.ts"], outfile: "dist/code.js" };
if (watch) {
  const context = await esbuild.context({
    ...common, entryPoints: ["src/ui.ts", "src/ui.html"], outdir: "dist/.watch", loader: { ".html": "copy" },
    plugins: [{ name: "ui", setup: (build) => build.onEnd(ui) }],
  });
  await (await esbuild.context(code)).watch();
  await context.watch();
} else {
  await esbuild.build(code);
  await ui();
}
