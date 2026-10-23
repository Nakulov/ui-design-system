#!/usr/bin/env python3
"""Проверка контрактов, файлов значений и версий спецификаций (schema-format.md, раздел 12).

Запуск: python3 tools/validate.py [--base <git ref>]
С --base дополнительно проверяется, что изменённые спецификации повысили версию.
Выход 1, если найдены ошибки. Зависимости: pip install -r tools/requirements.txt.
"""

import argparse
import json
import re
import subprocess
import sys
from pathlib import Path

try:
    from jsonschema import Draft202012Validator
    from jsonschema.exceptions import best_match
except ImportError:
    sys.exit("Нет пакета jsonschema: python3 -m pip install -r tools/requirements.txt")

ROOT = Path(__file__).resolve().parent.parent

# Мета-схемы структуры файлов (schema-format.md, раздел 12).
CONTRACT_SCHEMA = Draft202012Validator(json.loads((ROOT / "specifications/contract.schema.json").read_text()))
VALUES_SCHEMA = Draft202012Validator(json.loads((ROOT / "specifications/values.schema.json").read_text()))

# Спецификации с версией в spec-versions.json (schema-format.md, раздел 11.3).
SPECS = {
    "schemaFormat": "specifications/schema-format.md",
    "figmaMapping": "specifications/figma-mapping.md",
    "documentationManifest": "specifications/documentation-manifest.md",
}
VERSION_RE = re.compile(r"^\*\*Версия спецификации:\*\*\s*(\S+)", re.M)

# Типы, которые Figma не отображает (figma-mapping.md, раздел 4.1, правило 3).
FIGMA_UNREPRESENTABLE = {"schemas/core/event/Event", "schemas/core/event/EventData"}

STATUS_RANK = {"released": 0, "notReleased": 1, "willNotBeReleased": 2}

errors = []


def error(path, message):
    errors.append(f"{path.relative_to(ROOT)}: {message}")


def json_files():
    for path in sorted(ROOT.rglob("*.json")):
        if not any(part.startswith(".") for part in path.relative_to(ROOT).parts):
            yield path


def is_contract(path):
    """Контракт — <Имя>.json; файлы <Имя>.<вид>.json контрактами не являются (раздел 11.1, правило 5)."""
    return len(path.suffixes) == 1 and path != ROOT / "spec-versions.json"


def check_structure(path, data, validator):
    """Структура по мета-схеме; True, если ошибок нет."""
    found = sorted(validator.iter_errors(data), key=lambda e: [str(part) for part in e.path])
    for e in found:
        e = best_match(e.context) if e.context else e
        location = "/".join(str(part) for part in e.absolute_path) or "корень"
        forbidden = e.validator_value.get("required") if e.validator == "not" else None
        error(path, f"{location}: " + (f"поле «{forbidden[0]}» здесь не допускается" if forbidden else e.message))
    return not found


def resolve(path, ref):
    """Путь контракта по ссылке без расширения, относительно файла со ссылкой (раздел 6)."""
    return (path.parent / f"{ref}.json").resolve()


def contract_id(file):
    return file.relative_to(ROOT).with_suffix("").as_posix()


def walk(node):
    if isinstance(node, dict):
        yield node
        for value in node.values():
            yield from walk(value)
    elif isinstance(node, list):
        for value in node:
            yield from walk(value)


def check_refs(path, data):
    for node in walk(data):
        for key in ("$ref", "extends"):
            ref = node.get(key)
            if isinstance(ref, str):
                if ref.startswith("/") or ref.endswith(".json"):
                    error(path, f"{key} «{ref}»: путь без ведущего / и без .json")
                elif not resolve(path, ref).is_file():
                    error(path, f"{key} «{ref}»: файл не найден")


def status(release_versions, platform):
    value = (release_versions or {}).get(platform)
    return value.get("status") if isinstance(value, dict) else value


def check_release_inheritance(path, data):
    """Вложенный уровень не доступнее родителя (раздел 8.5, правило 2) — в пределах файла."""
    parent = data.get("releaseVersions")
    if not parent:
        return
    children = [*data.get("properties", []), *data.get("values", []), *data.get("oneOf", [])]
    for child in children:
        for platform in ("android", "ios", "figma"):
            p, c = status(parent, platform), status(child.get("releaseVersions"), platform)
            if p in STATUS_RANK and c in STATUS_RANK and STATUS_RANK[c] < STATUS_RANK[p]:
                error(path, f"«{child.get('name')}»: {platform} {c} доступнее родителя ({p})")


def check_figma_unrepresentable(path, data):
    for prop in data.get("properties", []):
        ref = prop.get("$ref") or (prop.get("items") or {}).get("$ref")
        if not isinstance(ref, str) or not resolve(path, ref).is_file():
            continue
        if contract_id(resolve(path, ref)) in FIGMA_UNREPRESENTABLE:
            if status(prop.get("releaseVersions"), "figma") != "willNotBeReleased":
                error(path, f"свойство «{prop.get('name')}» не отображается в Figma: нужен figma: willNotBeReleased")


def check_values(path, data):
    modes = data.get("modes", [])
    keys = [set(mode.get("values", {})) for mode in modes]
    for mode, mode_keys in zip(modes[1:], keys[1:]):
        if mode_keys != keys[0]:
            error(path, f"режим «{mode.get('name')}»: набор ключей отличается от первого режима (раздел 9.2, правило 5)")
    for mode in modes:
        for key, value in mode.get("values", {}).items():
            if isinstance(value, dict) and "$ref" in value and "value" in value:
                target = resolve(path, value["$ref"])
                target_values = target.with_name(f"{target.stem}.values.json")
                if not target_values.is_file():
                    error(path, f"«{key}»: у алиаса нет файла значений {target_values.name}")
                    continue
                target_modes = json.loads(target_values.read_text()).get("modes", [])
                if not any(value["value"] in m.get("values", {}) for m in target_modes):
                    error(path, f"«{key}»: варианта «{value['value']}» нет в {target_values.name}")


def check_spec_versions():
    path = ROOT / "spec-versions.json"
    versions = json.loads(path.read_text())
    for key, spec in SPECS.items():
        match = VERSION_RE.search((ROOT / spec).read_text())
        header = match.group(1) if match else None
        if versions.get(key) != header:
            error(path, f"{key} = {versions.get(key)}, а в заголовке {spec} — {header}")


def check_spec_bumps(base):
    def git(*args):
        return subprocess.run(["git", *args], cwd=ROOT, capture_output=True, text=True, check=True).stdout

    # Сравнение с точкой ответвления, включая незакоммиченные правки.
    fork = git("merge-base", base, "HEAD").strip()
    changed = git("diff", "--name-only", fork).split()
    for spec in SPECS.values():
        if spec not in changed:
            continue
        old = subprocess.run(["git", "show", f"{fork}:{spec}"], cwd=ROOT, capture_output=True, text=True)
        old_match = VERSION_RE.search(old.stdout)
        new_match = VERSION_RE.search((ROOT / spec).read_text())
        if old_match and new_match and old_match.group(1) == new_match.group(1):
            error(ROOT / spec, f"файл изменён, а версия осталась {new_match.group(1)} (раздел 11.3)")


def main():
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("--base", help="git ref, с которым сравниваются спецификации")
    args = parser.parse_args()

    for path in json_files():
        try:
            data = json.loads(path.read_text())
        except json.JSONDecodeError as e:
            error(path, f"некорректный JSON: {e}")
            continue
        if path.name.endswith(".schema.json"):
            continue
        check_refs(path, data)
        if path.name.endswith(".values.json"):
            if check_structure(path, data, VALUES_SCHEMA):
                check_values(path, data)
        elif is_contract(path) and check_structure(path, data, CONTRACT_SCHEMA):
            check_release_inheritance(path, data)
            check_figma_unrepresentable(path, data)

    check_spec_versions()
    if args.base:
        check_spec_bumps(args.base)

    for line in errors:
        print(line)
    print(f"Ошибок: {len(errors)}" if errors else "Ошибок нет")
    return 1 if errors else 0


if __name__ == "__main__":
    sys.exit(main())
