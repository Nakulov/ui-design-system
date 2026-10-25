#!/usr/bin/env python3
"""Проверка контрактов, файлов значений и версий спецификаций (schema-format.md, раздел 12).

Запуск: python3 tools/validate.py [--base <git ref>]
С --base дополнительно проверяется, что изменённые спецификации повысили версию.
Выход 1, если найдены ошибки. Зависимости: pip install -r tools/requirements.txt.
"""

import argparse
import difflib
import json
import re
import subprocess
import sys
import xml.etree.ElementTree as ElementTree

try:
    from jsonschema import Draft202012Validator
    from jsonschema.exceptions import best_match
except ImportError:
    sys.exit("Нет пакета jsonschema: python3 -m pip install -r tools/requirements.txt")

from contracts import (APPEARANCE, COMPONENT, DEMOS, HOLE_KINDS, PREVIEWS, ROOT, SCHEMAS, THEMES, Contract, Registry, fill,
                       is_contract, is_element, is_sample, is_skipped, pascal, ref_path)
import idea
from samples import dumps, generate, render, required_samples

# Мета-схемы структуры файлов (schema-format.md, раздел 12).
CONTRACT_SCHEMA = Draft202012Validator(json.loads((ROOT / "specifications/contract.schema.json").read_text()))
VALUES_SCHEMA = Draft202012Validator(json.loads((ROOT / "specifications/values.schema.json").read_text()))

# Спецификации с версией в spec-versions.json (schema-format.md, раздел 11.3).
SPECS = {
    "schemaFormat": "specifications/schema-format.md",
    "figmaMapping": "specifications/figma-mapping.md",
    "documentationManifest": "specifications/documentation-manifest.md",
    "bundleFormat": "specifications/bundle-format.md",
}
VERSION_RE = re.compile(r"^\*\*Версия спецификации:\*\*\s*(\S+)", re.M)

# Символы: рисунки в теме проверяются как одноцветные SVG (foundation Icon, README).
ICON = "schemas/foundation/icon/Icon"
SVG_PAINTS = ("fill", "stroke", "stop-color", "color")
SVG_NO_PAINT = {"none", "currentcolor", "inherit", "transparent"}

# Типы, которые Figma не отображает (figma-mapping.md, раздел 4.1, правило 3).
FIGMA_UNREPRESENTABLE = {"schemas/core/event/Event", "schemas/core/event/EventData"}

# Области применения переменных Figma по типу переменной (figma-mapping.md, раздел 6.3).
FIGMA_SCOPES = {
    "COLOR": {"ALL_FILLS", "FRAME_FILL", "SHAPE_FILL", "TEXT_FILL", "STROKE_COLOR", "EFFECT_COLOR"},
    "FLOAT": {"CORNER_RADIUS", "WIDTH_HEIGHT", "GAP", "STROKE_FLOAT", "EFFECT_FLOAT", "OPACITY", "FONT_WEIGHT",
              "FONT_SIZE", "LINE_HEIGHT", "LETTER_SPACING", "PARAGRAPH_SPACING", "PARAGRAPH_INDENT"},
    "STRING": {"TEXT_CONTENT", "FONT_FAMILY", "FONT_STYLE"},
    "BOOLEAN": set(),
}
FIGMA_VARIABLE_TYPE = {"color": "COLOR", "integer": "FLOAT", "float": "FLOAT", "string": "STRING", "boolean": "BOOLEAN"}
FIGMA_FOUNDATION_FIELDS = {"$ref", "scopes", "publish"}
FIGMA_COMPONENT_FIELDS = {"$ref", "sample", "variants", "iconSample", "properties"}

PLATFORMS = ("android", "ios", "figma")
STATUS_RANK = {"released": 0, "notReleased": 1, "willNotBeReleased": 2}
README_SECTIONS = [
    "Назначение", "Состав", "Варианты", "Рекомендации по использованию", "Ограничения и открытые вопросы",
]
LOWER_CAMEL = re.compile(r"[a-z][A-Za-z0-9]*")
# Подмножество Markdown раздела «Рекомендации по использованию» (раздел 11.2): запрещённые конструкции.
USAGE_SECTION = "Рекомендации по использованию"
USAGE_FORBIDDEN = [
    (re.compile(r"^\s*\|"), "таблица"), (re.compile(r"^\s*(```|~~~)"), "блок кода"), (re.compile(r"^\s*#"), "подзаголовок"),
    (re.compile(r"^\s*>"), "цитата"), (re.compile(r"!\["), "картинка"), (re.compile(r"</?[A-Za-z!]"), "HTML"),
]
DIFF_LINES = 40  # строк диффа на файл в сообщении об устаревшем файле

errors = {}


def error(path, message):
    errors.setdefault(f"{path.relative_to(ROOT)}: {message}")


def json_files():
    for path in sorted(ROOT.rglob("*.json")):
        if not is_skipped(path):
            yield path


def check_structure(path, data, validator):
    """Структура по мета-схеме; True, если ошибок нет."""
    found = sorted(validator.iter_errors(data), key=lambda e: [str(part) for part in e.path])
    for e in found:
        e = best_match(e.context) if e.context else e
        location = "/".join(str(part) for part in e.absolute_path) or "корень"
        forbidden = e.validator_value.get("required") if e.validator == "not" else None
        error(path, f"{location}: " + (f"поле «{forbidden[0]}» здесь не допускается" if forbidden else e.message))
    return not found


def walk(node):
    if isinstance(node, dict):
        yield node
        for value in node.values():
            yield from walk(value)
    elif isinstance(node, list):
        for value in node:
            yield from walk(value)


def refs(path, data):
    """Пути всех ссылок файла: $ref, extends."""
    for node in walk(data):
        for key in ("$ref", "extends"):
            ref = node.get(key)
            if isinstance(ref, str) and not ref.startswith("/") and not ref.endswith(".json"):
                yield ref_path(path, ref)


def check_refs(path, data):
    for node in walk(data):
        for key in ("$ref", "extends"):
            ref = node.get(key)
            if isinstance(ref, str):
                if ref.startswith("/") or ref.endswith(".json"):
                    error(path, f"{key} «{ref}»: путь без ведущего / и без .json")
                elif not ref_path(path, ref).is_file():
                    error(path, f"{key} «{ref}»: файл не найден")


# Организация файлов (разделы 10, 11).

def check_files(registry, referenced):
    for contract in registry:
        if contract.name != contract.path.stem:
            error(contract.path, f"name «{contract.name}» не совпадает с именем файла")
        if contract.kind == "helper" and contract.path not in referenced:
            error(contract.path, "вспомогательное определение, на которое никто не ссылается")
    for layer in sorted(p for p in SCHEMAS.iterdir() if p.is_dir()):
        for entity in sorted(p for p in layer.iterdir() if p.is_dir()):
            if not LOWER_CAMEL.fullmatch(entity.name):
                error(entity, "имя каталога сущности не в lowerCamelCase")
            if not (entity / f"{pascal(entity.name)}.json").is_file():
                error(entity, f"нет публичного контракта {pascal(entity.name)}.json (раздел 11.1, правило 2)")
            readme = entity / "README.md"
            if not readme.is_file():
                error(entity, "нет README.md (раздел 11.2)")
                continue
            sections = re.findall(r"^## (.+?)\s*$", readme.read_text(), re.M)
            if sections != README_SECTIONS:
                error(readme, f"разделы {sections}, ожидаются {README_SECTIONS} (раздел 11.2)")
            check_usage(readme)
    for theme in sorted(p for p in THEMES.iterdir() if p.is_dir()):
        if not LOWER_CAMEL.fullmatch(theme.name):
            error(theme, "имя темы не в lowerCamelCase (раздел 9.4)")


# Примеры моделей (раздел 11.1, правило 8; раздел 11.2).

def check_usage(readme):
    """Раздел «Рекомендации по использованию» — подмножество Markdown для документации Figma (раздел 11.2)."""
    match = re.search(rf"^## {USAGE_SECTION}\s*$(.*?)(?=^## |\Z)", readme.read_text(), re.M | re.S)
    if match:
        check_subset(readme, match.group(1), readme.read_text()[:match.start(1)].count("\n") + 1, f"разделе «{USAGE_SECTION}»")


def check_overview():
    """Описание системы `docs/overview.md`: разделы `##` и подмножество Markdown (figma-documentation.md, раздел 3)."""
    path = ROOT / "docs" / "overview.md"
    if not path.is_file():
        error(ROOT / "docs", "нет overview.md: описание системы для документации Figma")
        return
    text = path.read_text()
    if not text.startswith("## "):
        error(path, "файл начинается с раздела «## …»")
    for match in re.finditer(r"^## [^\n]+\n(.*?)(?=^## |\Z)", text, re.M | re.S):
        check_subset(path, match.group(1), text[:match.start(1)].count("\n") + 1, "описании системы")


def check_subset(path, section, first, where):
    """Строки раздела — только абзацы, списки, код в строке, жирный и ссылки (раздел 11.2)."""
    for offset, line in enumerate(section.split("\n")):
        # Код в строке выводится как есть: разметка внутри него не считается.
        text = re.sub(r"`[^`]*`", "", line) if not re.match(r"^\s*(```|~~~)", line) else line
        for pattern, name in USAGE_FORBIDDEN:
            if pattern.search(text):
                error(path, f"строка {first + offset}: {name} в {where} — только абзацы, списки, код в строке, жирный "
                            f"и ссылки (раздел 11.2)")
                break


def mismatch(path, actual, expected, message):
    """Ошибка расхождения файла с ожидаемым текстом и дифф, ограниченный DIFF_LINES строками."""
    name = path.relative_to(ROOT)
    diff = list(difflib.unified_diff(actual.splitlines(), expected.splitlines(),
                                     f"{name} (в ветке)", f"{name} (ожидается)", lineterm=""))
    if len(diff) > DIFF_LINES:
        diff = diff[:DIFF_LINES] + [f"… ещё {len(diff) - DIFF_LINES} строк"]
    error(path, "\n".join([message, *diff]))


def check_samples(registry):
    for contract in registry:
        if contract.kind != "public":
            continue
        directory = contract.path.parent / "samples"
        samples = {p.stem: p for p in sorted(directory.glob("*.json"))} if directory.is_dir() else {}
        for name in required_samples(contract):
            path = directory / f"{name}.json"
            expected = dumps(generate(registry, contract, name))
            if not path.is_file():
                error(path, "нет примера: python3 tools/sync.py")
            elif path.read_text() != expected:
                mismatch(path, path.read_text(), expected, "пример устарел: python3 tools/sync.py")
        kind = registry.by_id.get(COMPONENT) if "extends" in contract.data else contract
        for name, path in samples.items():
            if not LOWER_CAMEL.fullmatch(name):
                error(path, "имя примера не в lowerCamelCase")
            try:
                value = json.loads(path.read_text())
            except json.JSONDecodeError as e:
                error(path, f"некорректный JSON: {e}")
                continue
            if kind is not contract and not (isinstance(value, dict) and list(value) == [contract.name]):
                error(path, f"пример компонента записывается объектом с ключом «{contract.name}»")
                continue
            if path.read_text() != dumps(value):
                mismatch(path, path.read_text(), dumps(value),
                         "формат примера не канонический: отступ 2, каждое поле на своей строке")
            for problem in registry.value_errors(value, kind, "модель"):
                error(path, problem)
            for owner, node, at in registry.objects(value, kind, "модель"):
                names = [p["name"] for p in registry.properties(owner)]
                if name == "min":
                    optional = [p["name"] for p in registry.properties(owner)
                                if not p["required"] and p["name"] in node]
                    if optional:
                        error(path, f"{at}: в min только обязательные свойства, лишние: {', '.join(optional)}")
                elif name == "max" and set(names) - set(node):
                    missing = [n for n in names if n not in node]
                    error(path, f"{at}: в max все свойства, нет: {', '.join(missing)}")
        readme = contract.path.parent / "README.md"
        if readme.is_file():
            for name in required_samples(contract):
                if f"](samples/{name}.json)" not in readme.read_text():
                    error(readme, f"нет ссылки на samples/{name}.json (раздел 11.2)")
            text, problems = render(readme)
            for problem in problems:
                error(readme, problem)
            if not problems and text != readme.read_text():
                mismatch(readme, readme.read_text(), text,
                         "блок примера не совпадает с файлом: python3 tools/sync.py")


def check_idea(registry):
    """Сопоставление схем в IDE перечисляет все контракты (раздел 12)."""
    expected = idea.json_schemas(registry)
    actual = idea.PATH.read_text() if idea.PATH.is_file() else ""
    if actual != expected:
        mismatch(idea.PATH, actual, expected, "сопоставление схем в IDE устарело: python3 tools/sync.py")


# Поддержка платформ (раздел 8).

def parse_status(value):
    if isinstance(value, str):
        return value, None
    version = value.get("version")
    return value["status"], tuple(int(part) for part in version.split(".")) if version else None


def inherit(parent, release_versions):
    """Действующие статусы вложенного уровня и нарушения ограничений (раздел 8.5)."""
    result, problems = dict(parent), []
    for platform, value in (release_versions or {}).items():
        status, version = parse_status(value)
        parent_status, parent_version = parent.get(platform, (None, None))
        if parent_status and STATUS_RANK[status] < STATUS_RANK[parent_status]:
            problems.append(f"{platform}: {status} доступнее родителя ({parent_status})")
        if version and parent_version and version < parent_version:
            problems.append(f"{platform}: версия {value['version']} меньше версии родителя")
        result[platform] = (status, version or parent_version)
    return result, problems


def check_release_versions(registry):
    for contract in registry:
        own = contract.data.get("releaseVersions")
        if contract.kind == "public" and set(own or {}) != set(PLATFORMS):
            error(contract.path, "публичный контракт указывает releaseVersions для android, ios и figma")
        elif contract.kind == "helper" and own:
            error(contract.path, "вспомогательное определение не указывает releaseVersions на уровне контракта")
        elif contract.kind == "internal" and any("releaseVersions" in node for node in walk(contract.data)):
            error(contract.path, "внутренний контракт не указывает releaseVersions (раздел 11.1, правило 7)")

    seen = set()

    def visit(contract, statuses, via):
        """Уровни контракта относительно родителя; по ссылкам — до публичного контракта (раздел 8.5)."""
        key = (contract.path, tuple(sorted(statuses.items())))
        if key in seen:
            return
        seen.add(key)

        def level(name, node):
            result, problems = inherit(statuses, node.get("releaseVersions"))
            for problem in problems:
                error(contract.path, f"«{name}»: {problem}" + (f", ссылка {via}" if via else ""))
            return result

        def follow(ref, result, name, inline=False):
            target = registry.resolve(contract, ref)
            if target and (inline or target.kind == "helper"):
                visit(target, result, f"{contract.name}.{name}")

        for prop in contract.data.get("properties", []):
            ref = prop.get("$ref") or prop.get("items", {}).get("$ref")
            follow(ref, level(prop["name"], prop), prop["name"])
        for value in contract.data.get("values", []):
            level(value["name"], value)
        for element in contract.data.get("oneOf", []):
            name = element.get("name") or element.get("$ref")
            follow(element.get("$ref"), level(name, element), name, element.get("inline", False))

    for contract in registry:
        if contract.kind == "public" and "releaseVersions" in contract.data:
            visit(contract, inherit({}, contract.data["releaseVersions"])[0], None)


def check_figma_unrepresentable(registry, contract):
    for prop in contract.data.get("properties", []):
        target = registry.resolve(contract, prop.get("$ref") or prop.get("items", {}).get("$ref"))
        if target and target.id in FIGMA_UNREPRESENTABLE:
            status = prop.get("releaseVersions", {}).get("figma")
            if status != "willNotBeReleased":
                error(contract.path,
                      f"свойство «{prop['name']}» не отображается в Figma: нужен figma: willNotBeReleased")


# Типы по ссылкам, варианты, компоненты (разделы 5, 7).

def check_sample_hints(registry, contract):
    """Подсказки sample — допустимые значения своего типа (раздел 11.1, правило 4)."""
    if "sample" in contract.data:
        for problem in registry.value_errors(contract.data["sample"], contract, "sample"):
            error(contract.path, problem)
    for prop in contract.data.get("properties", []):
        if "sample" in prop:
            for problem in registry.property_value_errors(contract, prop, prop["sample"], f"«{prop['name']}».sample"):
                error(contract.path, problem)
    for element in contract.data.get("oneOf", []):
        if "sample" in element:
            for problem in registry.value_errors(element["sample"], element["type"], f"«{element['name']}».sample"):
                error(contract.path, problem)


def duplicates(names):
    return sorted({name for name in names if names.count(name) > 1})


def check_types(registry, contract):
    for prop in contract.data.get("properties", []):
        if prop.get("uniqueVariants"):
            target = registry.resolve(contract, prop["items"].get("$ref"))
            if target is None or target.type != "oneOf":
                error(contract.path, f"«{prop['name']}»: uniqueVariants только у массива элементов oneOf (раздел 5.1)")
    for element in contract.data.get("oneOf", []):
        target = registry.resolve(contract, element.get("$ref"))
        if element.get("inline") and target and target.type != "enum":
            error(contract.path, f"«{element['$ref']}»: inline ссылается только на enum (раздел 7)")
    for name in duplicates([v.name for v in registry.variants(contract)]):
        error(contract.path, f"вариант «{name}» после разворачивания встречается больше одного раза")
    for name in duplicates([p["name"] for p in registry.properties(contract)]):
        error(contract.path, f"свойство «{name}» встречается больше одного раза (с учётом свойств Component)")


def check_component(registry, contract):
    is_component = contract.kind == "public" and contract.layer == "components"
    if is_component and "extends" not in contract.data:
        error(contract.path, "публичный контракт components/ указывает extends и appearanceContext (раздел 5.2)")
    if not is_component and "extends" in contract.data:
        error(contract.path, "extends допустим только у публичных контрактов components/ (раздел 5.2)")
    if "extends" not in contract.data:
        return
    if registry.resolve(contract, contract.data["extends"]) is not registry.by_id.get(COMPONENT):
        error(contract.path, "extends ссылается только на Component (раздел 5.2)")
    appearance = registry.by_id.get(APPEARANCE)
    allowed = {v.name for v in registry.variants(appearance)} if appearance else set()
    for name in contract.data["appearanceContext"]:
        if name not in allowed:
            error(contract.path, f"appearanceContext: «{name}» — не вариант Appearance")
    if "selectable" in contract.data["appearanceContext"]:
        selected = next((p for p in contract.data.get("properties", []) if p["name"] == "selected"), None)
        if not selected or selected.get("type") != "boolean" or not selected["required"]:
            error(contract.path, "при контексте selectable нужно обязательное свойство selected типа boolean")


# Файлы значений и темы (раздел 9).

def value_type(path, data, registry):
    """Тип величин файла: примитив или Contract; None, если ссылка не разрешилась."""
    kind = data["valueType"]
    return kind if isinstance(kind, str) else registry.by_path.get(ref_path(path, kind["$ref"]))


def values_file(theme_file, target):
    """Файл значений контракта target для алиаса из theme_file (раздел 9.3, правило 1; раздел 9.4, правило 3)."""
    if target.kind == "internal":
        return target.path.with_name(f"{target.name}.values.json")
    candidate = theme_file.parent / f"{target.name}.values.json"
    return candidate if candidate.is_file() else THEMES / "default" / candidate.name


def check_values(registry, values, path, data):
    target = registry.by_path.get(ref_path(path, data["$ref"]))
    if target is None:
        return
    if target.kind == "helper":
        error(path, f"$ref на вспомогательное определение {target.name}, а не на публичный или внутренний контракт")
    expected = (target.path.parent if target.kind == "internal" else path.parent) / f"{target.name}.values.json"
    if path != expected or (target.kind == "public" and path.parent.parent != THEMES):
        error(path, f"файл значений {target.name} располагается в каталоге темы"
                    f"{' рядом с контрактом' if target.kind == 'internal' else ''} (раздел 9.1)")

    kind = value_type(path, data, registry)
    if isinstance(kind, Contract) and kind.type != "object":
        error(path, f"valueType ссылается на {kind.name} типа {kind.type}, а не object")
        return

    keys = {v.name for v in registry.variants(target) if not v.has_data}
    for name in duplicates([mode["name"] for mode in data["modes"]]):
        error(path, f"режим «{name}» встречается больше одного раза")
    for mode in data["modes"]:
        present = set(mode["values"])
        if keys - present:
            error(path, f"режим «{mode['name']}»: нет величин вариантов {', '.join(sorted(keys - present))}")
        if present - keys:
            extra = ", ".join(sorted(present - keys))
            error(path, f"режим «{mode['name']}»: не варианты без данных {target.name}: {extra}")
        for key, value in mode["values"].items():
            at = f"режим «{mode['name']}»: «{key}»"
            if isinstance(value, dict) and "$ref" in value:
                check_alias(registry, values, path, mode["name"], value, at)
            elif kind is not None:
                for problem in registry.value_errors(value, kind, key):
                    error(path, f"режим «{mode['name']}»: {problem}")
                if isinstance(value, dict) and isinstance(value.get("source"), str):
                    check_resource(path, value["source"], target.id == ICON, at)


def check_resource(path, source, is_icon, at):
    """Файл ресурса темы существует (раздел 9.4, правило 6); рисунок символа — одноцветный SVG."""
    file = path.parent / source
    if not file.is_file():
        error(path, f"{at}: нет файла {source}")
    elif is_icon:
        for problem in svg_problems(file):
            error(path, f"{at}: {source}: {problem}")


def svg_problems(file):
    try:
        root = ElementTree.parse(file).getroot()
    except ElementTree.ParseError as e:
        return [f"некорректный SVG: {e}"]
    problems, colors = [], set()
    box = (root.get("viewBox") or "").replace(",", " ").split()
    if len(box) != 4 or box[2] != box[3]:
        problems.append(f"viewBox «{root.get('viewBox')}» не квадратный")
    for node in root.iter():
        tag = node.tag.rsplit("}", 1)[-1]
        if tag == "image":
            problems.append("растровая вставка <image>")
        if tag in ("linearGradient", "radialGradient", "pattern"):
            problems.append(f"заливка <{tag}>: рисунок символа одноцветный")
        style = dict(part.split(":", 1) for part in (node.get("style") or "").split(";") if ":" in part)
        style = {key.strip(): value.strip() for key, value in style.items()}
        for paint in SVG_PAINTS:
            value = (node.get(paint) or style.get(paint) or "").strip().lower()
            if value and value not in SVG_NO_PAINT:
                colors.add(value)
    if len(colors) > 1:
        problems.append(f"несколько цветов: {', '.join(sorted(colors))}")
    return list(dict.fromkeys(problems))


def check_alias(registry, values, path, mode_name, alias, at):
    """Алиас разрешается, без циклов, с тем же valueType (раздел 9.3)."""
    origin, kind, seen = path, value_type(path, values[path], registry), set()
    while True:
        target = registry.by_path.get(ref_path(path, alias["$ref"]))
        if target is None:
            return
        path = values_file(path, target)
        if path not in values:
            error(origin, f"{at}: нет файла значений {path.relative_to(ROOT)}")
            return
        data = values[path]
        mode = next((m for m in data["modes"] if m["name"] == mode_name), data["modes"][0])
        where = f"{path.relative_to(ROOT)}, режим «{mode['name']}»"
        if (path, mode["name"], alias["value"]) in seen:
            error(origin, f"{at}: циклическая ссылка алиасов через {where}")
            return
        seen.add((path, mode["name"], alias["value"]))
        if value_type(path, data, registry) != kind:
            error(origin, f"{at}: valueType цели алиаса ({where}) отличается")
            return
        if alias["value"] not in mode["values"]:
            error(origin, f"{at}: нет варианта «{alias['value']}» в {where}")
            return
        alias = mode["values"][alias["value"]]
        if not (isinstance(alias, dict) and "$ref" in alias):
            return


def check_themes(registry, values):
    default = THEMES / "default"
    for contract in registry:
        if contract.kind == "public" and contract.layer == "foundation":
            if default / f"{contract.name}.values.json" not in values:
                error(contract.path, "нет файла значений в теме default (раздел 9.4, правило 2)")
    for path, data in values.items():
        if path.parent.parent != THEMES or path.parent == default:
            continue
        base = values.get(default / path.name)
        names = {mode["name"] for mode in data["modes"]}
        missing = [mode["name"] for mode in base["modes"] if mode["name"] not in names] if base else []
        if missing:
            error(path, f"нет режимов темы default: {', '.join(missing)} (раздел 9.4, правило 5)")


def check_figma_foundation(registry, values, figma):
    """<Имя>.figma.json foundation-сущностей с примитивным valueType (figma-mapping.md, раздел 6.3)."""
    default = THEMES / "default"
    for contract in registry:
        if contract.kind != "public" or contract.layer != "foundation":
            continue
        data = values.get(default / f"{contract.name}.values.json")
        path = contract.path.with_name(f"{contract.name}.figma.json")
        primitive = data is not None and isinstance(data["valueType"], str)
        if not primitive:
            if path in figma:
                error(path, "файл нужен только сущности с примитивным valueType (figma-mapping.md, раздел 6.3)")
            continue
        if path not in figma:
            error(contract.path, f"нет {path.name}: scopes переменных (figma-mapping.md, раздел 6.3)")
            continue
        project = figma[path]
        if not isinstance(project, dict):
            error(path, "ожидается объект")
            continue
        for extra in sorted(project.keys() - FIGMA_FOUNDATION_FIELDS):
            error(path, f"поле «{extra}» не определено (figma-mapping.md, раздел 6.3)")
        if project.get("$ref") != contract.name:
            error(path, f"$ref «{project.get('$ref')}», ожидается «{contract.name}»")
        kind = FIGMA_VARIABLE_TYPE[data["valueType"]]
        scopes = project.get("scopes")
        if not (isinstance(scopes, list) and scopes and all(isinstance(scope, str) for scope in scopes)):
            error(path, "scopes — непустой список строк")
        else:
            for scope in duplicates(scopes):
                error(path, f"scope «{scope}» встречается больше одного раза")
            for scope in scopes:
                if scope not in FIGMA_SCOPES[kind]:
                    error(path, f"scope «{scope}» недопустим для переменных {kind}")
        if "publish" in project and not isinstance(project["publish"], bool):
            error(path, "publish — boolean")


def figma_mapping(registry, owner, prop):
    """Свойство Figma по умолчанию и допустимые переопределения (figma-mapping.md, раздел 4)."""
    kind = prop.get("type")
    if kind == "boolean":
        return "boolean", {"variant"}
    if kind == "string":
        return "text", set()
    if kind in ("integer", "float"):
        return None, {"variant", "override"}
    target = registry.resolve(owner, prop.get("$ref"))
    if target is None:
        return None, set()
    if "extends" in target.data:
        return "instanceSwap", set()
    if target.type == "enum":
        return "variant", {"mode", "override"}
    if target.type != "oneOf":
        return None, set()
    variants = registry.variants(target)
    if any(v.target == "string" for v in variants):
        return None, {"text"}
    if all(not v.has_data for v in variants):
        return "variant", {"mode", "override"}
    return None, {"override"}


def check_figma_components(registry, figma):
    """<Имя>.figma.json компонентов: образцы и переопределения свойств (figma-mapping.md, разделы 4, 6, 9)."""
    for contract in registry:
        if contract.kind != "public" or "extends" not in contract.data or contract.name == "Container":
            continue
        if contract.data.get("releaseVersions", {}).get("figma") == "willNotBeReleased":
            continue
        path = contract.path.with_name(f"{contract.name}.figma.json")
        project = figma.get(path)
        if not isinstance(project, dict):
            error(contract.path, f"нет {path.name}: образец Figma-компонента (figma-mapping.md, раздел 6)")
            continue
        for extra in sorted(project.keys() - FIGMA_COMPONENT_FIELDS):
            error(path, f"поле «{extra}» не определено (figma-mapping.md, раздел 6)")
        if project.get("$ref") != contract.name:
            error(path, f"$ref «{project.get('$ref')}», ожидается «{contract.name}»")
        if ("sample" in project) == ("variants" in project):
            error(path, "задаётся ровно одно из полей sample и variants (раздел 6)")
        if "iconSample" in project and contract.name != "Text":
            error(path, "iconSample — только у Text (раздел 10.5)")
        own = {prop["name"]: (owner, prop) for owner, prop in registry.declared_properties(contract)
               if owner is contract}

        def sample(name, at):
            """Модель образца или None; корень — этот компонент без индикаторов (раздел 6.2, правило 1)."""
            file = contract.path.parent / "samples" / f"{name}.json" if isinstance(name, str) else None
            if file is None or not file.is_file():
                error(path, f"{at}: нет примера samples/{name}.json")
                return None
            model = json.loads(file.read_text()).get(contract.name, {})
            if "indicators" in model:
                error(path, f"{at}: образец {name} с индикаторами: они в Figma-компоненты не входят (раздел 6.2)")
            return model

        for key in ("sample", "iconSample"):
            if key in project:
                sample(project[key], key)
        variants = project.get("variants", {})
        if not isinstance(variants, dict) or len(variants) > 1:
            error(path, "variants — объект с одним свойством (раздел 6.2, правило 3)")
            variants = {}
        for name, samples in variants.items():
            if name not in own:
                error(path, f"variants: «{name}» — не свойство {contract.name}")
                continue
            owner, prop = own[name]
            target = registry.resolve(owner, prop.get("$ref"))
            allowed = [v.name for v in registry.variants(target)] if target else []
            if not prop["required"]:
                allowed.append("none")
            if not isinstance(samples, dict) or not samples:
                error(path, f"variants.{name}: непустой объект «значение → образец»")
                continue
            for value, sample_name in samples.items():
                if value not in allowed:
                    error(path, f"variants.{name}: «{value}» — не значение свойства ({', '.join(allowed)})")
                    continue
                model = sample(sample_name, f"variants.{name}.{value}")
                if model is None:
                    continue
                actual = list(model[name])[0] if isinstance(model.get(name), dict) else model.get(name, "none")
                if actual != value:
                    error(path, f"variants.{name}.{value}: у образца {sample_name} значение «{actual}»")
        properties = project.get("properties", {})
        for name, override in properties.items():
            if name not in own:
                error(path, f"properties: «{name}» — не собственное свойство {contract.name}")
                continue
            if name in variants:
                error(path, f"properties: «{name}» уже задан в variants (раздел 6.1, правило 5)")
            _, allowed = figma_mapping(registry, *own[name])
            if not isinstance(override, dict) or override.get("as") not in allowed:
                error(path, f"properties.{name}: as «{override.get('as') if isinstance(override, dict) else override}» "
                            f"недопустимо, допустимо: {', '.join(sorted(allowed)) or 'нет'} (раздел 4)")
        for name, (owner, prop) in own.items():
            default, _ = figma_mapping(registry, owner, prop)
            hidden = prop.get("releaseVersions", {}).get("figma") == "willNotBeReleased"
            if default is None and not hidden and name not in properties and name not in variants:
                error(path, f"свойство «{name}» не отображается в Figma: нужно переопределение или "
                            f"figma: willNotBeReleased (раздел 4.1, правило 3)")


def check_elements(registry, elements):
    """Элементы оформления документации: шаблон-модель с дырками (figma-documentation.md, раздел 5)."""
    kind = registry.by_id.get(COMPONENT)
    for path, data in elements.items():
        if not LOWER_CAMEL.fullmatch(path.stem):
            error(path, "имя элемента не в lowerCamelCase")
        if not isinstance(data, dict) or set(data) != {"holes", "model"} or not isinstance(data["holes"], dict):
            error(path, "ожидается объект с полями holes и model (figma-documentation.md, раздел 5)")
            continue
        if path.parent.parent == DEMOS and data["holes"]:
            error(path, "у демонстрации нет дырок: holes пустой (figma-documentation.md, раздел 5)")
        if path.read_text() != dumps(data):
            mismatch(path, path.read_text(), dumps(data), "формат не канонический: отступ 2, каждое поле на своей строке")
        samples = {}
        for name, hole in data["holes"].items():
            if not isinstance(hole, dict) or hole.get("kind") not in HOLE_KINDS or "sample" not in hole:
                error(path, f"дырка «{name}»: kind из {', '.join(sorted(HOLE_KINDS))} и sample")
                continue
            expected = {"text": str, "spans": list, "component": dict, "items": list}[hole["kind"]]
            if not isinstance(hole["sample"], expected):
                error(path, f"дырка «{name}»: sample не подходит к kind {hole['kind']}")
                continue
            samples[name] = hole["sample"]
        used = set(re.findall(r'"\$([A-Za-z0-9]+)"', json.dumps(data["model"], ensure_ascii=False)))
        for name in sorted(used - data["holes"].keys()):
            error(path, f"дырка «${name}» не объявлена в holes")
        for name in sorted(data["holes"].keys() - used):
            error(path, f"дырка «{name}» не используется в model")
        if used - samples.keys():
            continue
        for problem in registry.value_errors(fill(data["model"], samples), kind, "модель"):
            error(path, f"с подставленными sample: {problem}")


def check_previews(registry, values, elements):
    """Превью величин: шаблон у каждой публичной foundation-сущности с файлом значений (figma-documentation.md, раздел 5)."""
    expected = {}
    for contract in registry:
        if contract.kind == "public" and contract.layer == "foundation" and THEMES / "default" / f"{contract.name}.values.json" in values:
            expected[PREVIEWS / f"{contract.path.parent.name}.json"] = contract
    for path, contract in sorted(expected.items()):
        if path not in elements:
            error(contract.path, f"нет превью величин {path.relative_to(ROOT)} (figma-documentation.md, раздел 5)")
    for path in sorted(p for p in elements if p.parent == PREVIEWS):
        if path not in expected:
            error(path, "превью без публичной foundation-сущности с файлом значений")
        elif set(elements[path].get("holes", {})) != {"value"}:
            error(path, "у превью одна дырка value — имя варианта")


# Манифесты документации (documentation-manifest.md).

MANIFEST_SECTIONS = {"overview", "properties", "variants", "states", "layoutBehavior", "usage"}
DEMO_SECTIONS = {"variants", "states", "layoutBehavior"}
COMPONENT_SECTIONS = {"overview", "properties", "variants", "usage"}
SAMPLE_SECTIONS = {"overview", "variants"}


def manifest_owners(registry):
    """Сущности, у которых обязателен манифест: публичные компоненты и core, видимые в Figma (раздел 2)."""
    return {contract.path.with_name(f"{contract.name}.docs.json"): contract for contract in registry
            if contract.kind == "public" and contract.layer in ("components", "core")
            and contract.data.get("releaseVersions", {}).get("figma") != "willNotBeReleased"}


def sample_file(registry, owner, name):
    """Файл примера по ссылке sample: `<имя>` своего компонента или `<Контракт>#<имя>` (раздел 5.1)."""
    if not isinstance(name, str):
        return None
    contract, _, name = name.rpartition("#")
    if contract:
        owner = next((c for c in registry if c.kind == "public" and c.layer == "components" and c.name == contract), None)
    if owner is None or name in required_samples(owner):
        return None
    file = owner.path.parent / "samples" / f"{name}.json"
    return file if file.is_file() else None


def check_manifests(registry, manifests, elements):
    """Манифесты: секции, примеры и наличие у каждой сущности, видимой в Figma (разделы 2, 4.2, 5)."""
    expected = manifest_owners(registry)
    for path, contract in sorted(expected.items()):
        if path not in manifests:
            error(contract.path, f"нет манифеста документации {path.name} (documentation-manifest.md, раздел 2)")
    used = set()
    for path, data in sorted(manifests.items()):
        if path not in expected:
            error(path, "манифест есть только у публичных компонентов и сущностей core, видимых в Figma (documentation-manifest.md, раздел 2)")
            continue
        if path.read_text() != dumps(data):
            mismatch(path, path.read_text(), dumps(data), "формат не канонический: отступ 2, каждое поле на своей строке")
        if not isinstance(data, dict) or set(data) != {"$ref", "sections"} or not isinstance(data["sections"], list):
            error(path, "ожидается объект с полями $ref и sections (documentation-manifest.md, раздел 3)")
            continue
        owner = expected[path]
        if data["$ref"] != owner.name:
            error(path, f"$ref «{data['$ref']}»: ожидается «{owner.name}» — контракт рядом с манифестом")
        component = owner.layer == "components"
        types = COMPONENT_SECTIONS if component else MANIFEST_SECTIONS
        with_examples = SAMPLE_SECTIONS if component else DEMO_SECTIONS
        source, other = ("sample", "demo") if component else ("demo", "sample")
        ids = []
        for i, section in enumerate(data["sections"]):
            at = f"sections[{i}]"
            if not isinstance(section, dict) or section.get("type") not in types:
                error(path, f"{at}: type из {', '.join(sorted(types))} (раздел 4.2)")
                continue
            kind = section["type"]
            allowed = {"id", "type"} | (set() if kind == "overview" else {"title"}) | ({"examples"} if kind in with_examples else set())
            for extra in sorted(section.keys() - allowed):
                error(path, f"{at}: поле «{extra}» не допускается в секции {kind} (раздел 4.2)")
            if not isinstance(section.get("id"), str) or not LOWER_CAMEL.fullmatch(section["id"]):
                error(path, f"{at}: id в lowerCamelCase (раздел 4.3)")
            else:
                ids.append(section["id"])
            if kind != "overview" and not isinstance(section.get("title"), str):
                error(path, f"{at}: нет title")
            if kind not in with_examples:
                continue
            examples = section.get("examples")
            if not isinstance(examples, list) or not examples:
                error(path, f"{at}: нет примеров examples")
                continue
            if kind == "overview" and len(examples) != 1:
                error(path, f"{at}: в Overview компонента ровно один основной пример (раздел 4.2, правило 6)")
            example_ids = []
            for j, example in enumerate(examples):
                where = f"{at}.examples[{j}]"
                if not isinstance(example, dict):
                    error(path, f"{where}: ожидается объект")
                    continue
                if other in example:
                    error(path, f"{where}: {other} не допускается у {'компонента' if component else 'core'}, пример — {source} (раздел 5)")
                for extra in sorted(example.keys() - {"id", source, other, "caption"}):
                    error(path, f"{where}: поле «{extra}» не допускается (раздел 5)")
                if not isinstance(example.get("id"), str) or not LOWER_CAMEL.fullmatch(example["id"]):
                    error(path, f"{where}: id в lowerCamelCase (раздел 4.3)")
                else:
                    example_ids.append(example["id"])
                if "caption" in example and not isinstance(example["caption"], str):
                    error(path, f"{where}: caption — строка")
                if component:
                    if sample_file(registry, owner, example.get("sample")) is None:
                        error(path, f"{where}: sample «{example.get('sample')}» — нет примера компонента; min и max не используются (раздел 5.1)")
                    continue
                demo = DEMOS / f"{example.get('demo')}.json"
                if not isinstance(example.get("demo"), str) or demo not in elements:
                    error(path, f"{where}: demo «{example.get('demo')}» — нет файла в {DEMOS.relative_to(ROOT)} (раздел 5.2)")
                else:
                    used.add(demo)
            for name in duplicates(example_ids):
                error(path, f"{at}: id примера «{name}» повторяется")
        for name in duplicates(ids):
            error(path, f"id секции «{name}» повторяется")
    for path in sorted(p for p in elements if p.parent.parent == DEMOS and p not in used):
        error(path, "демонстрация не используется ни одним манифестом")


# Версии спецификаций (раздел 11.3).

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

    contracts, values, figma, elements, manifests, referenced = [], {}, {}, {}, {}, set()
    for path in json_files():
        try:
            data = json.loads(path.read_text())
        except json.JSONDecodeError as e:
            error(path, f"некорректный JSON: {e}")
            continue
        if path.name.endswith(".schema.json") or is_sample(path):
            continue
        if is_element(path):
            elements[path] = data
            continue
        check_refs(path, data)
        referenced.update(refs(path, data))
        if path.name.endswith(".figma.json"):
            figma[path] = data
        elif path.name.endswith(".docs.json"):
            manifests[path] = data
        elif path.name.endswith(".values.json"):
            if check_structure(path, data, VALUES_SCHEMA):
                values[path] = data
        elif is_contract(path) and check_structure(path, data, CONTRACT_SCHEMA):
            contracts.append(Contract(path, data))

    # Смысловые проверки — только для файлов с корректной структурой.
    registry = Registry(contracts)
    check_files(registry, referenced)
    check_release_versions(registry)
    for contract in registry:
        check_figma_unrepresentable(registry, contract)
        check_types(registry, contract)
        check_sample_hints(registry, contract)
        check_component(registry, contract)
    for path, data in values.items():
        check_values(registry, values, path, data)
    check_themes(registry, values)
    check_figma_foundation(registry, values, figma)
    check_figma_components(registry, figma)
    check_elements(registry, elements)
    check_previews(registry, values, elements)
    check_manifests(registry, manifests, elements)
    check_overview()
    check_samples(registry)
    check_idea(registry)

    check_spec_versions()
    if args.base:
        check_spec_bumps(args.base)

    for line in errors:
        print(line)
    print(f"Ошибок: {len(errors)}" if errors else "Ошибок нет")
    return 1 if errors else 0


if __name__ == "__main__":
    sys.exit(main())
