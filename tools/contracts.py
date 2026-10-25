"""Загрузка контрактов, разворачивание вариантов и проверка записи значений.

schema-format.md, разделы 4.5, 6, 7, 11.
"""

import json
import re
from dataclasses import dataclass
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
SCHEMAS = ROOT / "schemas"
THEMES = ROOT / "themes"
ELEMENTS = ROOT / "figma" / "documentation" / "elements"  # элементы оформления документации (figma-documentation.md, раздел 5)
PREVIEWS = ROOT / "figma" / "documentation" / "previews"  # превью величин foundation (figma-documentation.md, раздел 5)
COMPONENT = "schemas/core/component/Component"
APPEARANCE = "schemas/core/appearance/Appearance"
SKIPPED = {"build", "figma-plugin"}  # верхние каталоги без контрактов

PRIMITIVES = {
    "string": lambda v: isinstance(v, str),
    "boolean": lambda v: isinstance(v, bool),
    "integer": lambda v: isinstance(v, int) and not isinstance(v, bool),
    "float": lambda v: isinstance(v, (int, float)) and not isinstance(v, bool),
    "color": lambda v: isinstance(v, str) and re.fullmatch(r"#[0-9A-F]{6}([0-9A-F]{2})?", v) is not None,
    "payload": lambda v: v is not None,
}


def is_contract(path):
    """Контракт — <Имя>.json вне samples/; <Имя>.<вид>.json — не контракты (раздел 11.1, правила 5, 8)."""
    return len(path.suffixes) == 1 and path != ROOT / "spec-versions.json" and not is_sample(path)


def is_sample(path):
    return path.parent.name == "samples"


def is_element(path):
    """Шаблон-модель документации: элемент оформления или превью величин."""
    return path.parent in (ELEMENTS, PREVIEWS)


HOLE_KINDS = {"text", "spans", "component", "items"}


def fill(node, values):
    """Модель элемента с подставленными дырками `$имя`; дырка `items` в массиве разворачивается в его элементы."""
    if isinstance(node, str) and node.startswith("$") and node[1:] in values:
        return values[node[1:]]
    if isinstance(node, list):
        result = []
        for item in node:
            filled = fill(item, values)
            spread = isinstance(item, str) and item.startswith("$") and isinstance(filled, list)
            result.extend(filled) if spread else result.append(filled)
        return result
    if isinstance(node, dict):
        return {key: fill(item, values) for key, item in node.items()}
    return node


def is_skipped(path):
    """Не файлы репозитория контрактов: скрытые каталоги, производный build/ и плагин со своими JSON (npm, manifest)."""
    parts = path.relative_to(ROOT).parts
    return any(part.startswith(".") for part in parts) or parts[0] in SKIPPED


def load_registry():
    """Контракты репозитория без проверки структуры — для инструментов, работающих с проверенным репозиторием."""
    paths = [p for p in sorted(ROOT.rglob("*.json")) if is_contract(p) and not is_skipped(p) and not is_element(p)
             and not p.relative_to(ROOT).parts[0] == "specifications"]
    return Registry([Contract(p, json.loads(p.read_text())) for p in paths])


def ref_path(file, ref):
    """Путь контракта по ссылке без расширения, относительно файла со ссылкой (раздел 6)."""
    return (file.parent / f"{ref}.json").resolve()


def pascal(name):
    return name[:1].upper() + name[1:]


@dataclass(eq=False)
class Contract:
    path: Path
    data: dict

    @property
    def id(self):
        return self.path.relative_to(ROOT).with_suffix("").as_posix()

    @property
    def name(self):
        return self.data["name"]

    @property
    def type(self):
        return self.data["type"]

    @property
    def layer(self):
        parts = self.path.relative_to(ROOT).parts
        return parts[1] if parts[0] == "schemas" and len(parts) > 2 else None

    @property
    def kind(self):
        """public, helper или internal (раздел 11.1, правила 2, 3, 7)."""
        if not self.path.is_relative_to(SCHEMAS):
            return "internal"
        entity = self.path.parent
        if entity.parent.parent == SCHEMAS and self.path.stem == pascal(entity.name):
            return "public"
        return "helper"


@dataclass(eq=False)
class Variant:
    """Вариант после разворачивания (раздел 7). target — Contract или примитив; None у варианта без данных."""
    name: str
    element: dict
    target: object

    @property
    def has_data(self):
        return self.target is not None


class Registry:
    def __init__(self, contracts):
        self.by_path = {c.path: c for c in contracts}
        self.by_id = {c.id: c for c in contracts}

    def __iter__(self):
        return iter(self.by_path.values())

    def resolve(self, contract, ref):
        return self.by_path.get(ref_path(contract.path, ref)) if isinstance(ref, str) else None

    def properties(self, contract):
        """Свойства объекта; у компонента — сначала свойства Component (раздел 5.2)."""
        return [prop for _, prop in self.declared_properties(contract)]

    def declared_properties(self, contract):
        """Пары (контракт, где объявлено свойство; свойство): ссылки свойства разрешаются от его контракта."""
        base = self.by_id.get(COMPONENT) if "extends" in contract.data else None
        inherited = [(base, prop) for prop in base.data["properties"]] if base else []
        return [*inherited, *((contract, prop) for prop in contract.data.get("properties", []))]

    def variants(self, contract):
        if contract.type == "enum":
            return [Variant(v["name"], v, None) for v in contract.data["values"]]
        if contract.type != "oneOf":
            return []
        result = []
        for element in contract.data["oneOf"]:
            if "type" in element:
                result.append(Variant(element["name"], element, element["type"]))
                continue
            target = self.resolve(contract, element["$ref"])
            if target is None:
                continue
            if element.get("inline"):
                result += [Variant(v["name"], v, None) for v in target.data.get("values", [])]
            else:
                name = element.get("name") or target.name[:1].lower() + target.name[1:]
                result.append(Variant(name, element, target))
        return result

    def components(self):
        return {c.name: c for c in self if "extends" in c.data}

    def value_errors(self, value, kind, at):
        """Ошибки записи значения типа kind — примитива или контракта (раздел 4.5)."""
        if value is None:
            yield f"{at}: null не используется"
            return
        if isinstance(kind, str):
            if not PRIMITIVES[kind](value):
                yield f"{at}: {value!r} — не {kind}"
            return
        if kind.id == COMPONENT:
            components = self.components()
            if not (isinstance(value, dict) and len(value) == 1 and next(iter(value)) in components):
                yield f"{at}: ожидается объект с одним ключом — именем компонента"
                return
            name, data = next(iter(value.items()))
            yield from self.value_errors(data, components[name], f"{at}.{name}")
        elif kind.type == "object":
            if not isinstance(value, dict):
                yield f"{at}: ожидается объект {kind.name}"
                return
            properties = self.declared_properties(kind)
            for extra in value.keys() - {p["name"] for _, p in properties}:
                yield f"{at}: поле «{extra}» не определено в {kind.name}"
            for owner, prop in properties:
                if prop["name"] in value:
                    yield from self.property_value_errors(owner, prop, value[prop["name"]], f"{at}.{prop['name']}")
                elif prop["required"]:
                    yield f"{at}: нет обязательного поля «{prop['name']}»"
            appearance = value.get("appearance")
            context = kind.data.get("appearanceContext")
            if context and isinstance(appearance, dict) and len(appearance) == 1:
                variant = next(iter(appearance))
                if variant not in context:
                    yield f"{at}.appearance: «{variant}» вне appearanceContext {kind.name} ({', '.join(context)})"
        else:
            variants = {v.name: v for v in self.variants(kind)}
            if isinstance(value, str):
                if value not in variants or variants[value].has_data:
                    yield f"{at}: «{value}» — не вариант без данных {kind.name}"
            elif isinstance(value, dict) and len(value) == 1:
                name, data = next(iter(value.items()))
                if name not in variants or not variants[name].has_data:
                    yield f"{at}: «{name}» — не вариант с данными {kind.name}"
                else:
                    yield from self.value_errors(data, variants[name].target, f"{at}.{name}")
            else:
                yield f"{at}: ожидается имя варианта {kind.name} или объект с одним ключом"

    def property_value_errors(self, owner, prop, value, at):
        if prop.get("type") != "array":
            kind = self.resolve(owner, prop["$ref"]) if "$ref" in prop else prop["type"]
            if kind is not None:
                yield from self.value_errors(value, kind, at)
            return
        if not isinstance(value, list) or not value:
            yield f"{at}: ожидается непустой массив"
            return
        items = prop["items"]
        kind = self.resolve(owner, items["$ref"]) if "$ref" in items else items["type"]
        if kind is None:
            return
        for i, item in enumerate(value):
            yield from self.value_errors(item, kind, f"{at}[{i}]")
        if prop.get("uniqueVariants"):
            names = [next(iter(item)) if isinstance(item, dict) else item for item in value]
            for name in {n for n in names if names.count(n) > 1}:
                yield f"{at}: вариант «{name}» встречается больше одного раза"

    def objects(self, value, kind, at, inside_component=False):
        """Объекты значения вместе с контрактами для правил min и max: вложенные компоненты и значения, совпадающие
        с подсказкой sample типа или свойства, не обходятся (раздел 11.1, правила 2, 4)."""
        if isinstance(kind, str) or kind is None or value is None or value == kind.data.get("sample"):
            return
        if kind.id == COMPONENT:
            if inside_component or not (isinstance(value, dict) and len(value) == 1):
                return
            name, data = next(iter(value.items()))
            yield from self.objects(data, self.components().get(name), f"{at}.{name}", True)
        elif kind.type == "object":
            if not isinstance(value, dict):
                return
            yield kind, value, at
            for owner, prop in self.declared_properties(kind):
                if prop["name"] not in value or value[prop["name"]] == prop.get("sample"):
                    continue
                child, child_at = value[prop["name"]], f"{at}.{prop['name']}"
                ref = prop.get("$ref") or prop.get("items", {}).get("$ref")
                target = self.resolve(owner, ref)
                if prop.get("type") == "array" and isinstance(child, list):
                    for i, item in enumerate(child):
                        yield from self.objects(item, target, f"{child_at}[{i}]", inside_component)
                else:
                    yield from self.objects(child, target, child_at, inside_component)
        elif isinstance(value, dict) and len(value) == 1:
            name, data = next(iter(value.items()))
            variant = next((v for v in self.variants(kind) if v.name == name), None)
            if variant and variant.has_data:
                yield from self.objects(data, variant.target, f"{at}.{name}", inside_component)
