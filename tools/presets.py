#!/usr/bin/env python3
"""Генератор описаний пресетов figma/presets/<Имя>.json (figma-mapping.md, раздел 11.2, правило 6).

Запуск: python3 tools/presets.py
Генератор держит оси, таблицы и исключения, а в файл пишет явный список вариантов (раздел 11.1, правило 1).
tools/validate.py сравнивает файлы с выводом generate(): ручная правка описания не сотрётся молча.
"""

import sys

from contracts import PRESETS
from samples import dumps

AVATAR = "https://raw.githubusercontent.com/Nakulov/ui-design-system/dev/assets/images/avatar.jpg"
PRODUCT = "https://raw.githubusercontent.com/Nakulov/ui-design-system/dev/assets/images/product.jpg"

# ImageIndicator: носитель по шкале Size → бейдж и отступ подложки символа (шаги шкал, без величин).
SIZES = ["m", "l", "xl", "xl2", "xl3"]
HALF = {"m": 16, "l": 20, "xl": 24, "xl2": 28, "xl3": 32}  # половина стороны носителя
RADII = {"zero": 0, "s": 8, "m": 12, "l": 16, "full": None}  # CornerRadius; full строится всегда
ANCHORS = ["topStart", "topEnd", "bottomStart", "bottomEnd"]
INDICATORS = ["dot", "9", "99", "99+", "icon", "image"]
DOT = {"m": "xs2", "l": "xs2", "xl": "xs2", "xl2": "xs", "xl3": "xs"}
GAP = {"m": "xs2", "l": "xs2", "xl": "xs2", "xl2": "xs", "xl3": "xs"}
# Счётчик и картинка-индикатор: высота (Size), текст, боковой отступ (Spacing), отступ подложки символа (Spacing).
BADGE = {
    "l": ("xs", "labelSmall", "xs", "xs2"),
    "xl": ("xs", "labelSmall", "xs", "xs2"),
    "xl2": ("s", "labelMedium", "s", "xs"),
    "xl3": ("s", "labelMedium", "s", "xs"),
}
# Примеры для фрейма Presets документации: carrier, size, cornerRadius, anchor, indicator.
EXAMPLES = [
    ("photo", "xl", "full", "bottomEnd", "dot"),
    ("photo", "xl", "full", "topEnd", "9"),
    ("photo", "xl", "full", "topEnd", "99+"),
    ("photo", "xl", "full", "bottomEnd", "icon"),
    ("photo", "xl", "full", "bottomEnd", "image"),
    ("icon", "xl", "m", "topEnd", "99"),
    ("icon", "l", "s", "topEnd", "dot"),
    ("photo", "m", "full", "bottomEnd", "dot"),
    ("photo", "xl3", "l", "topEnd", "99+"),
]
CARRIER_PADDING = {"m": "xs", "l": "s", "xl": "m", "xl2": "l", "xl3": "l"}  # символ 24, у xl3 — 32


def fixed(width, height=None):
    return {
        "width": {"sizing": {"fixed": width} if width != "hug" else "hug"},
        "height": {"sizing": {"fixed": height or width}},
        "position": "flow",
    }


def indicator(size, kind):
    if kind == "dot":
        return {"Badge": {
            "id": "indicator",
            "layout": fixed(DOT[size]),
            "padding": {"all": "zero"},
            "appearance": {"plain": {"background": {"solid": "success"}, "cornerRadius": "full"}},
        }}
    height, typography, side, inset = BADGE[size]
    if kind == "icon":
        return {"Image": {
            "id": "indicator",
            "layout": fixed(height),
            "padding": {"all": inset},
            "appearance": {"plain": {"background": {"solid": "primary"}, "cornerRadius": "full", "clip": True}},
            "source": {"resource": {"icon": {"name": "check", "color": "onPrimary"}}},
        }}
    if kind == "image":
        return {"Image": {
            "id": "indicator",
            "layout": fixed(height),
            "padding": {"all": "zero"},
            "appearance": {"plain": {"background": {"solid": "surface2"}, "cornerRadius": "full", "clip": True}},
            "source": remote(PRODUCT),
        }}
    return {"Badge": {
        "id": "indicator",
        "layout": fixed("hug", height),
        "padding": {"symmetric": {"horizontal": side, "vertical": "zero"}},
        "appearance": {"plain": {"background": {"solid": "error"}, "cornerRadius": "full"}},
        "content": {"text": {"value": kind, "typography": typography, "color": "onError"}},
    }}


def remote(url):
    return {"remote": {
        "origin": {"url": url},
        "scale": "crop",
        "alignment": "center",
        "placeholder": {"icon": {"name": "image", "color": "onSurfaceMuted"}},
    }}


def image_indicator():
    variants = []
    for carrier in ["photo", "icon"]:
        for size in SIZES:
            for radius, step in RADII.items():
                if step is not None and step >= HALF[size]:
                    continue  # дублирует full
                for anchor in ANCHORS:
                    for kind in INDICATORS if size != "m" else ["dot"]:
                        if carrier == "photo":
                            padding, background, source = "zero", "surface2", remote(AVATAR)
                        else:
                            padding, background = CARRIER_PADDING[size], "primaryContainer"
                            source = {"resource": {"icon": {"name": "image", "color": "onPrimaryContainer"}}}
                        variants.append({
                            "axes": {"carrier": carrier, "size": size, "cornerRadius": radius, "anchor": anchor, "indicator": kind},
                            "model": {"Image": {
                                "id": "image",
                                "layout": fixed(size),
                                "padding": {"all": padding},
                                "appearance": {"plain": {"background": {"solid": background}, "cornerRadius": radius, "clip": True}},
                                "indicators": [{"anchor": anchor, "content": indicator(size, kind), "gap": GAP[size]}],
                                "source": source,
                            }},
                        })
    return {
        "name": "ImageIndicator",
        "root": "components/image/Image",
        "description": "Картинка с индикатором у угла: фото или символ на подложке, точка, счётчик, символ или фото",
        "axes": {
            "carrier": ["photo", "icon"],
            "size": SIZES,
            "cornerRadius": list(RADII),
            "anchor": ANCHORS,
            "indicator": INDICATORS,
        },
        "layout": {"rows": ["cornerRadius", "anchor"], "columns": "indicator", "blocks": ["carrier", "size"]},
        "examples": [dict(zip(["carrier", "size", "cornerRadius", "anchor", "indicator"], e)) for e in EXAMPLES],
        "variants": variants,
    }


# Button: уровень → оформление рамки default, наложение pressed, оформление disabled, цвет подписи default.
STROKE = {"width": "s", "color": "outline"}
LEVELS = {
    "primary": ({"background": {"solid": "primary"}}, "overlayPressed", {"background": {"solid": "surface2"}}, "onPrimary"),
    "secondary": ({"background": {"solid": "secondaryContainer"}}, "overlayPressed", {"background": {"solid": "surface2"}},
                  "onSecondaryContainer"),
    "outline": ({"stroke": STROKE}, "overlayPressed", {"stroke": STROKE}, "primary"),
    "text": ({}, "overlayPressed", {}, "primary"),
}
# Размер → высота рамки (Size), боковой отступ (Spacing), роль подписи.
BUTTON_SIZES = {"s": ("m", "m", "labelMedium"), "m": ("l", "l", "labelLarge"), "l": ("xl", "xl", "labelLarge")}
ICONS = {"none": [], "leading": [{"icon": {"name": "check"}}], "trailing": [{"icon": {"name": "chevronDown"}}]}
BUTTON_STATES = ["default", "pressed", "disabled"]
# Примеры для фрейма Presets документации: level, size, icon; все в состоянии default.
BUTTON_EXAMPLES = [
    ("primary", "m", "none"),
    ("secondary", "m", "none"),
    ("outline", "m", "none"),
    ("text", "m", "none"),
    ("primary", "m", "leading"),
    ("primary", "m", "trailing"),
    ("primary", "s", "none"),
    ("primary", "l", "none"),
]


def label(icon, typography, color):
    """Подпись кнопки: ширина hug — в одну строку, кнопка растёт по ширине; maxLines не нужен (число в пресете запрещено)."""
    # Отступ между символом и текстом — пробел в тексте, как у символа в строке на платформах.
    if icon == "trailing":
        spans = [{"text": {"value": "Кнопка "}}] + ICONS[icon]
    else:
        spans = ICONS[icon] + [{"text": {"value": " Кнопка"}}]
    return {"Text": {
        "id": "label",
        "layout": {"width": {"sizing": "hug"}, "height": {"sizing": "hug"}, "position": "flow"},
        "padding": {"all": "zero"},
        "appearance": {"plain": {}},
        "content": {"rich": {"spans": spans}} if icon != "none" else {"plain": "Кнопка"},
        "typography": typography,
        "color": {"solid": color},
        "alignment": "center",
    }}


def button():
    variants = []
    for level, (default, overlay, disabled, color) in LEVELS.items():
        for size, (height, side, typography) in BUTTON_SIZES.items():
            for icon in ICONS:
                model = {"Pressable": {
                    "id": "button",
                    "layout": {"width": {"sizing": "hug"}, "height": {"sizing": {"fixed": height}}, "position": "flow"},
                    "padding": {"symmetric": {"horizontal": side, "vertical": "zero"}},
                    "appearance": {"control": {
                        "default": {**default, "cornerRadius": "full"},
                        "pressed": {**default, "cornerRadius": "full", "overlay": {"solid": overlay}},
                        "disabled": {**disabled, "cornerRadius": "full"},
                    }},
                    "states": {
                        "default": label(icon, typography, color),
                        "disabled": label(icon, typography, "onSurfaceMuted"),
                    },
                }}
                for state in BUTTON_STATES:
                    variants.append({"axes": {"level": level, "size": size, "icon": icon, "state": state}, "model": model})
    return {
        "name": "Button",
        "root": "components/pressable/Pressable",
        "description": "Кнопка: подпись, при необходимости символ до или после неё; четыре уровня, три размера",
        "axes": {"level": list(LEVELS), "size": list(BUTTON_SIZES), "icon": list(ICONS), "state": BUTTON_STATES},
        "layout": {"rows": ["level", "icon"], "columns": "state", "blocks": "size"},
        "examples": [dict(zip(["level", "size", "icon", "state"], e + ("default",))) for e in BUTTON_EXAMPLES],
        "variants": variants,
    }


GENERATORS = {"Button": button, "ImageIndicator": image_indicator}


def generate():
    """Описания пресетов с генератором: путь файла → текст в каноническом формате."""
    return {PRESETS / f"{name}.json": dumps(build()) for name, build in GENERATORS.items()}


def main():
    PRESETS.mkdir(parents=True, exist_ok=True)
    for path, text in generate().items():
        if not path.exists() or path.read_text() != text:
            path.write_text(text)
            print(f"Записано: {path.relative_to(PRESETS.parent.parent)}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
