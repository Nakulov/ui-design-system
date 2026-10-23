"""Примеры моделей: генерация min и max, формат файлов, блоки в README (schema-format.md, разделы 11.1, 11.2).

Пример вставляется в README блоком ```json сразу после пометки <!-- sample: samples/<файл>.json -->.
"""

import json
import re

from contracts import COMPONENT

# Величины-заглушки примитивов; name — имя ближайшего свойства или варианта (раздел 11.1, правило 8).
PLACEHOLDERS = {
    "string": lambda name: name,
    "boolean": lambda name: True,
    "integer": lambda name: 1,
    "float": lambda name: 1,
    "color": lambda name: "#000000",
    "payload": lambda name: {"action": name},
}


def dumps(value):
    """Канонический формат файла примера."""
    return json.dumps(value, indent=2, ensure_ascii=False) + "\n"


def required_samples(contract):
    """Примеры, которые генерируются для публичного контракта: min и max у object, кроме Component."""
    if contract.kind == "public" and contract.type == "object" and contract.id != COMPONENT:
        return ("min", "max")
    return ()


def generate(registry, contract, mode):
    """Пример min или max публичного контракта; компонент — объектом с ключом-именем."""
    def value(kind, name):
        if isinstance(kind, str):
            return PLACEHOLDERS[kind](name)
        if kind.id == COMPONENT:
            return {root.name: generate(registry, root, "min")[root.name]} if "extends" in root.data else None
        if kind.type == "object":
            result = {}
            for owner, prop in registry.declared_properties(kind):
                if mode == "min" and not prop["required"]:
                    continue
                if prop.get("type") == "array":
                    items = prop["items"]
                    item = registry.resolve(owner, items["$ref"]) if "$ref" in items else items["type"]
                    result[prop["name"]] = [value(item, prop["name"])]
                else:
                    target = registry.resolve(owner, prop["$ref"]) if "$ref" in prop else prop["type"]
                    result[prop["name"]] = value(target, prop["name"])
            return result
        variants = registry.variants(kind)
        plain = [v for v in variants if not v.has_data]
        objects = [v for v in variants if v.has_data and not isinstance(v.target, str) and v.target.type == "object"]
        if mode == "min":
            variant = (plain or variants)[0]
        else:
            variant = (objects or [v for v in variants if v.has_data] or variants)[0]
        return variant.name if not variant.has_data else {variant.name: value(variant.target, variant.name)}

    root = contract
    data = value(contract, contract.name)
    return {contract.name: data} if "extends" in contract.data else data

MARKER = re.compile(r"<!-- sample: (samples/[A-Za-z0-9]+\.json) -->")


def render(readme):
    """Текст README с блоками, заполненными из файлов примеров, и список ошибок разметки."""
    lines = readme.read_text().split("\n")
    result, problems, i = [], [], 0
    while i < len(lines):
        line = lines[i]
        result.append(line)
        marker = MARKER.fullmatch(line.strip())
        if line.strip() == "```json":
            problems.append(f"строка {i + 1}: блок ```json без пометки <!-- sample: … --> (раздел 11.2)")
        elif marker:
            sample = readme.parent / marker.group(1)
            if i + 1 >= len(lines) or lines[i + 1].strip() != "```json":
                problems.append(f"строка {i + 1}: после пометки нет блока ```json")
            elif not sample.is_file():
                problems.append(f"строка {i + 1}: нет файла {marker.group(1)}")
                result.append(lines[i + 1])
                i += 1
            else:
                end = next((j for j in range(i + 2, len(lines)) if lines[j].strip() == "```"), None)
                if end is None:
                    problems.append(f"строка {i + 2}: блок ```json не закрыт")
                else:
                    result += ["```json", *sample.read_text().rstrip("\n").split("\n"), "```"]
                    i = end
        i += 1
    return "\n".join(result), problems
