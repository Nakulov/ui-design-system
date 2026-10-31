"""Загрузка контрактов, разворачивание вариантов и проверка записи значений.

schema-format.md, разделы 4.5, 6, 7, 11.
"""

import json
import re
from collections import Counter
from dataclasses import dataclass
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
CONTRACTS = ROOT / "contracts"
THEMES = ROOT / "themes"
ELEMENTS = ROOT / "figma" / "documentation" / "elements"  # элементы оформления документации (figma-documentation.md, раздел 5)
CHECKLIST = ROOT / "figma" / "releaseChecklist.json"  # пункты чеклиста готовности к релизу (figma-mapping.md, раздел 8.5)
PRESETS = ROOT / "figma" / "presets"  # описания пресетов: figma/presets/<Имя>.json (figma-mapping.md, раздел 11.2)
SCREENS = ROOT / "screens"  # демо-экраны: screens/<имя>.json в формате образцов (bundle-format.md, раздел 3.9)
DEFINITIONS = "definitions"  # вспомогательные определения сущности (раздел 11.1)
PREVIEW = "preview.json"  # превью величин foundation (figma-documentation.md, раздел 5)
DEMOS = "demos"  # демонстрации core: demos/<имя>.json (figma-documentation.md, раздел 5)
SERVICE_FILES = {"README.md", "figma.json", "docs.json", "changelog.json", PREVIEW}  # раздел 11.1, правило 5
SERVICE_DIRS = {DEFINITIONS, "samples", DEMOS}
COMPONENT = "core/component/Component"
APPEARANCE = "core/appearance/Appearance"
PRESSABLE = "components/pressable/Pressable"
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
    """Контракт — <Имя>.json вне samples/, demos/, screens/ и figma/presets/, не служебный файл сущности
    (раздел 11.1, правила 5, 8, 9)."""
    return (len(path.suffixes) == 1 and path not in (ROOT / "spec-versions.json", CHECKLIST) and not is_sample(path)
            and not is_screen(path) and not is_preset(path) and not is_element(path)
            and not (path.is_relative_to(CONTRACTS) and path.name in SERVICE_FILES))


def is_sample(path):
    return path.parent.name == "samples"


def is_screen(path):
    return path.parent == SCREENS


def is_preset(path):
    return path.is_relative_to(PRESETS)


def is_element(path):
    """Шаблон-модель документации: элемент оформления, превью величин или демонстрация."""
    return path.parent == ELEMENTS or path.is_relative_to(CONTRACTS) and (path.name == PREVIEW or path.parent.name == DEMOS)


def element_id(path):
    """Ключ шаблона-модели в наборе: путь без .json от contracts/ или от корня репозитория (bundle-format.md, раздел 3.7)."""
    base = CONTRACTS if path.is_relative_to(CONTRACTS) else ROOT
    return path.relative_to(base).with_suffix("").as_posix()


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
    paths = [p for p in sorted(ROOT.rglob("*.json")) if is_contract(p) and not is_skipped(p)
             and not p.relative_to(ROOT).parts[0] == "specifications"]
    return Registry([Contract(p, json.loads(p.read_text())) for p in paths])


def scope(path):
    """Область имён файла: `<слой>/<сущность>` в contracts/, иначе каталог от корня репозитория (раздел 6)."""
    if path.is_relative_to(CONTRACTS):
        return "/".join(path.relative_to(CONTRACTS).parts[:2])
    return path.parent.relative_to(ROOT).as_posix()


def ref_id(file, ref):
    """Идентификатор контракта по ссылке: имя без `/` — в области имён файла, с `/` — идентификатор (раздел 6)."""
    return ref if "/" in ref else f"{scope(file)}/{ref}"


def id_path(ident):
    """Файл контракта по идентификатору или None: в корне сущности, в её definitions/ или вне contracts/."""
    *where, name = ident.split("/")
    candidates = [CONTRACTS.joinpath(*where, f"{name}.json"), CONTRACTS.joinpath(*where, DEFINITIONS, f"{name}.json"),
                  ROOT.joinpath(*where, f"{name}.json")] if len(where) == 2 else []
    return next((p for p in candidates if p.is_file() and is_contract(p)), None)


def pascal(name):
    return name[:1].upper() + name[1:]


@dataclass(eq=False)
class Contract:
    path: Path
    data: dict

    @property
    def id(self):
        """`<слой>/<сущность>/<Имя>` без корня и definitions/; вне contracts/ — путь от корня (figma-mapping.md, раздел 7.1)."""
        return f"{scope(self.path)}/{self.path.stem}"

    @property
    def name(self):
        return self.data["name"]

    @property
    def type(self):
        return self.data["type"]

    @property
    def entity(self):
        """Каталог сущности или None у внутреннего контракта."""
        return CONTRACTS.joinpath(*scope(self.path).split("/")) if self.path.is_relative_to(CONTRACTS) else None

    @property
    def layer(self):
        return scope(self.path).split("/")[0] if self.entity else None

    @property
    def kind(self):
        """public, helper или internal (раздел 11.1, правила 2, 3, 7)."""
        if self.entity is None:
            return "internal"
        return "public" if self.path.parent == self.entity and self.path.stem == pascal(self.entity.name) else "helper"


@dataclass(eq=False)
class Variant:
    """Вариант после разворачивания (раздел 7). target — Contract или примитивный тип; None у варианта без данных."""
    name: str
    element: dict
    target: object

    @property
    def has_data(self):
        return self.target is not None


class Registry:
    def __init__(self, contracts):
        self.by_path = {c.path: c for c in contracts}
        self.by_id = {c.id: c for c in sorted(contracts, key=lambda c: c.kind == "public")}  # при совпадении — главный
        self.by_entity = {c.entity: c for c in contracts if c.kind == "public"}

    def __iter__(self):
        return iter(self.by_path.values())

    def resolve(self, contract, ref):
        return self.by_id.get(ref_id(contract.path, ref)) if isinstance(ref, str) else None

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
        """Ошибки записи значения типа kind — примитивного типа или контракта (раздел 4.5)."""
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
            for owner, prop in properties:
                other = prop.get("defaultsTo")
                if prop["name"] in value and other in value and self.resolve(owner, prop.get("$ref")) is self.by_id.get(COMPONENT):
                    mine, theirs = (next(iter(v)) if isinstance(v, dict) and len(v) == 1 else None
                                    for v in (value[prop["name"]], value[other]))
                    if mine and theirs and mine != theirs:
                        yield f"{at}.{prop['name']}: {mine}, а у «{other}» — {theirs}: компонент тот же (defaultsTo)"
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

    def model_errors(self, value, kind, at):
        """Ошибки модели: запись значений (раздел 4.5) и правила дерева (component-model.md, разделы 5, 14)."""
        yield from self.value_errors(value, kind, at)
        yield from self.tree_errors(value, kind, at)

    def tree_errors(self, value, kind, at):
        """Правила дерева модели, которых нет в контрактах: `id` уникален в дереве, где каждый `Pressable` учитывается
        одним состоянием; корень дерева состояния `Pressable` — только `plain` (component-model.md, разделы 5, 14)."""
        components, problems = self.components(), []

        def ids(name, data, at):
            """Число повторов каждого id в поддереве компонента: деревья состояний Pressable — по максимуму."""
            contract = components[name]
            counts = Counter([data["id"]] if isinstance(data.get("id"), str) else [])
            rest = data
            if contract.id == PRESSABLE and isinstance(data.get("states"), dict):
                rest = {key: item for key, item in data.items() if key != "states"}
                states = Counter()
                for key, tree in data["states"].items():
                    for child, child_data, child_at in self.nested(tree, self.by_id[COMPONENT], f"{at}.states.{key}"):
                        appearance = child_data.get("appearance")
                        if isinstance(appearance, dict) and list(appearance) != ["plain"]:
                            problems.append(f"{child_at}.appearance: корень дерева состояния — только plain")
                        states |= ids(child, child_data, child_at)
                counts += states
            for child, child_data, child_at in self.nested(rest, contract, at):
                counts += ids(child, child_data, child_at)
            return counts

        total = Counter()
        for name, data, node_at in self.nested(value, kind, at):
            total += ids(name, data, node_at)
        yield from problems
        for duplicate in sorted(n for n, count in total.items() if count > 1):
            yield f"{at}: id «{duplicate}» встречается больше одного раза (component-model.md, раздел 5)"

    def nested(self, value, kind, at):
        """Компоненты, вложенные в значение типа kind, без захода внутрь них: (имя, данные, путь)."""
        if isinstance(kind, str) or kind is None or value is None:
            return
        if kind.id == COMPONENT:
            if isinstance(value, dict) and len(value) == 1 and next(iter(value)) in self.components():
                name, data = next(iter(value.items()))
                if isinstance(data, dict):
                    yield name, data, f"{at}.{name}"
        elif kind.type == "object":
            if not isinstance(value, dict):
                return
            for owner, prop in self.declared_properties(kind):
                if prop["name"] not in value:
                    continue
                child, child_at = value[prop["name"]], f"{at}.{prop['name']}"
                target = self.resolve(owner, prop.get("$ref") or prop.get("items", {}).get("$ref"))
                if prop.get("type") == "array" and isinstance(child, list):
                    for i, item in enumerate(child):
                        yield from self.nested(item, target, f"{child_at}[{i}]")
                else:
                    yield from self.nested(child, target, child_at)
        elif isinstance(value, dict) and len(value) == 1:
            name, data = next(iter(value.items()))
            variant = next((v for v in self.variants(kind) if v.name == name), None)
            if variant and variant.has_data:
                yield from self.nested(data, variant.target, f"{at}.{name}")

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
