#!/usr/bin/env python3
"""Сборка набора данных плагина Figma (bundle-format.md).

Запуск: python3 tools/bundle.py [--out <файл>]
Сначала выполняются проверки tools/validate.py; при ошибках набор не собирается.
По умолчанию набор записывается в build/bundle.json. Выход 1 при ошибках.
"""

import argparse
import json
import re
import subprocess
import sys
from pathlib import Path

from contracts import DEMOS, ELEMENTS, PREVIEWS, ROOT, SCHEMAS, THEMES, is_sample, is_skipped, load_registry, pascal, ref_path

THEME = "default"
ICON = "schemas/foundation/icon/Icon"
SECTION_RE = re.compile(r"^## (.+)$", re.M)


def ident(path):
    """Идентификатор сущности: путь от корня репозитория без .json (раздел 2)."""
    return path.relative_to(ROOT).with_suffix("").as_posix()


def rewrite_refs(node, file):
    """Копия узла контракта: $ref и extends — идентификаторы целевых контрактов (раздел 3.1)."""
    if isinstance(node, list):
        return [rewrite_refs(item, file) for item in node]
    if not isinstance(node, dict):
        return node
    result = {}
    for key, value in node.items():
        if key in ("$ref", "extends") and isinstance(value, str):
            result[key] = ident(ref_path(file, value))
        else:
            result[key] = rewrite_refs(value, file)
    return result


def value(node, file, theme_dir):
    """Величина файла значений: алиас — ссылкой, пути ресурсов — от корня репозитория (раздел 3.2)."""
    if isinstance(node, dict) and "$ref" in node and "value" in node:
        return {"alias": f"{ident(ref_path(file, node['$ref']))}#{node['value']}"}
    if isinstance(node, dict):
        return {key: ((theme_dir / item).relative_to(ROOT).as_posix() if key == "source" else item)
                for key, item in node.items()}
    return node


def collect_values(theme_dir):
    result = {}
    for file in sorted(theme_dir.glob("*.values.json")):
        data = json.loads(file.read_text())
        kind = data["valueType"]
        result[ident(ref_path(file, data["$ref"]))] = {
            "valueType": kind if isinstance(kind, str) else {"$ref": ident(ref_path(file, kind["$ref"]))},
            "modes": [{"name": mode["name"], "values": {k: value(v, file, theme_dir) for k, v in mode["values"].items()}}
                      for mode in data["modes"]],
        }
    return result


def collect_figma():
    """Файлы <Имя>.figma.json по идентификатору контракта, без $ref (раздел 3.6)."""
    result = {}
    for file in sorted(SCHEMAS.rglob("*.figma.json")):
        data = json.loads(file.read_text())
        result[ident(ref_path(file, data["$ref"]))] = rewrite_refs({k: v for k, v in data.items() if k != "$ref"}, file)
    return result


def collect_samples(registry):
    result = {}
    for file in sorted(SCHEMAS.rglob("samples/*.json")):
        if is_skipped(file) or not is_sample(file):
            continue
        entity = file.parent.parent
        owner = registry.by_path.get(entity / f"{pascal(entity.name)}.json")
        if owner is not None:
            result[f"{owner.id}#{file.stem}"] = json.loads(file.read_text())
    return result


def collect_icons(values, theme_dir):
    result = {}
    for name, item in values[ICON]["modes"][0]["values"].items():
        result[f"{ICON}#{name}"] = {"svg": (ROOT / item["source"]).read_text(), "mirrored": item["mirrored"]}
    return result


def sections(readme):
    """Разделы второго уровня: заголовок → текст в исходном Markdown без заголовка (раздел 3.5)."""
    text = readme.read_text()
    found = list(SECTION_RE.finditer(text))
    return {m.group(1).strip(): text[m.end():found[i + 1].start() if i + 1 < len(found) else len(text)].strip()
            for i, m in enumerate(found)}


def collect_texts(registry):
    result = {}
    for readme in sorted(SCHEMAS.glob("*/*/README.md")):
        owner = registry.by_path.get(readme.parent / f"{pascal(readme.parent.name)}.json")
        if owner is not None:
            result[owner.id] = sections(readme)
    result["docs/overview"] = sections(ROOT / "docs" / "overview.md")
    return result


def collect_documentation():
    """Элементы оформления документации, превью величин и демонстрации по пути без .json (раздел 3.7)."""
    files = sorted([*ELEMENTS.glob("*.json"), *PREVIEWS.glob("*.json"), *DEMOS.glob("*/*.json")])
    return {ident(file): json.loads(file.read_text()) for file in files}


def collect_manifests(registry):
    """Манифесты документации по идентификатору контракта; demo — идентификатор демонстрации (раздел 3.8)."""
    result = {}
    for file in sorted(SCHEMAS.glob("*/*/*.docs.json")):
        data = json.loads(file.read_text())
        owner = registry.by_path[ref_path(file, data["$ref"])]
        result[owner.id] = {"sections": [
            {**section, "examples": [{**example, "demo": ident(DEMOS / example["demo"])} for example in section["examples"]]}
            if "examples" in section else section
            for section in data["sections"]
        ]}
    return result


def dangling(bundle):
    """Ссылки набора на отсутствующие сущности (раздел 5, правило 3)."""
    contracts, values = bundle["contracts"], bundle["values"]

    def walk(node, at):
        if isinstance(node, dict):
            for key, item in node.items():
                if key in ("$ref", "extends") and isinstance(item, str) and item not in contracts:
                    yield f"{at}: {key} «{item}» не найден"
                else:
                    yield from walk(item, at)
        elif isinstance(node, list):
            for item in node:
                yield from walk(item, at)

    for key, contract in contracts.items():
        yield from walk(contract, key)
    for key in bundle["figma"]:
        if key not in contracts:
            yield f"figma: {key} — нет контракта"
    for key, manifest in bundle["manifests"].items():
        for section in manifest["sections"]:
            for example in section.get("examples", []):
                if example["demo"] not in bundle["documentation"]:
                    yield f"manifests: {key} — демонстрация «{example['demo']}» не найдена"
    for key, data in values.items():
        if key not in contracts:
            yield f"values: {key} — нет контракта"
        yield from walk(data["valueType"], key)
        for mode in data["modes"]:
            for name, item in mode["values"].items():
                if isinstance(item, dict) and "alias" in item:
                    target, variant = item["alias"].split("#", 1)
                    names = {n for m in values.get(target, {}).get("modes", []) for n in m["values"]}
                    if variant not in names:
                        yield f"{key}, режим «{mode['name']}», «{name}»: алиас «{item['alias']}» не найден"


def main():
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("--out", default=str(ROOT / "build" / "bundle.json"), help="файл набора")
    args = parser.parse_args()

    check = subprocess.run([sys.executable, str(ROOT / "tools" / "validate.py")], capture_output=True, text=True)
    if check.returncode != 0:
        print(check.stdout, end="")
        print("Набор не собран: репозиторий не проходит проверку")
        return 1

    registry = load_registry()
    theme_dir = THEMES / THEME
    values = collect_values(theme_dir)
    bundle = {
        "specVersions": json.loads((ROOT / "spec-versions.json").read_text()),
        "theme": THEME,
        "contracts": {c.id: rewrite_refs(c.data, c.path) for c in sorted(registry, key=lambda c: c.id)},
        "values": values,
        "figma": collect_figma(),
        "samples": collect_samples(registry),
        "icons": collect_icons(values, theme_dir),
        "texts": collect_texts(registry),
        "documentation": collect_documentation(),
        "manifests": collect_manifests(registry),
    }
    problems = list(dangling(bundle))
    for problem in problems:
        print(problem)
    if problems:
        print("Набор не собран: ссылки не разрешились")
        return 1

    out = Path(args.out)
    out.parent.mkdir(parents=True, exist_ok=True)
    out.write_text(json.dumps(bundle, ensure_ascii=False, indent=2) + "\n")
    counts = ", ".join(f"{key}: {len(bundle[key])}" for key in ("contracts", "values", "figma", "samples", "icons", "texts", "documentation", "manifests"))
    print(f"Набор собран: {out.relative_to(ROOT) if out.is_relative_to(ROOT) else out} ({counts})")
    return 0


if __name__ == "__main__":
    sys.exit(main())
