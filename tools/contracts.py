"""Загрузка контрактов, разворачивание вариантов и проверка записи значений.

schema-format.md, разделы 4.5, 6, 7, 11.
"""

import re
from dataclasses import dataclass
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
SCHEMAS = ROOT / "schemas"
THEMES = ROOT / "themes"
COMPONENT = "schemas/core/component/Component"
APPEARANCE = "schemas/core/appearance/Appearance"

PRIMITIVES = {
    "string": lambda v: isinstance(v, str),
    "boolean": lambda v: isinstance(v, bool),
    "integer": lambda v: isinstance(v, int) and not isinstance(v, bool),
    "float": lambda v: isinstance(v, (int, float)) and not isinstance(v, bool),
    "color": lambda v: isinstance(v, str) and re.fullmatch(r"#[0-9A-F]{6}([0-9A-F]{2})?", v) is not None,
    "payload": lambda v: v is not None,
}


def is_contract(path):
    """Контракт — <Имя>.json; файлы <Имя>.<вид>.json контрактами не являются (раздел 11.1, правило 5)."""
    return len(path.suffixes) == 1 and path != ROOT / "spec-versions.json"


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
        base = self.by_id.get(COMPONENT) if "extends" in contract.data else None
        return [*(base.data["properties"] if base else []), *contract.data.get("properties", [])]

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
            properties = self.properties(kind)
            for extra in value.keys() - {p["name"] for p in properties}:
                yield f"{at}: поле «{extra}» не определено в {kind.name}"
            for prop in properties:
                if prop["name"] in value:
                    yield from self.property_value_errors(kind, prop, value[prop["name"]], f"{at}.{prop['name']}")
                elif prop["required"]:
                    yield f"{at}: нет обязательного поля «{prop['name']}»"
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
