# Text

## Назначение

`Text` отображает текст: простой или составленный из фрагментов с разным стилем и ссылками. Стиль всего текста
задаётся ролями `foundation` — типографика `TypographyRole`, заливка `TextColor` из ролей `ColorRole` — и линией (`decoration`); фрагменты
переопределяют стиль для своей части текста. Величины берутся из темы.

Компонент наследует базовую модель `Component` с контекстами оформления `plain` и `control`: текст бывает статичным и
кликабельным (`events`). При `control` нажатие и недоступность меняют оформление корня (фон, обводку), но не цвет
текста. Фон, отступы и обводка задаются полями базовой модели (`appearance`, `padding`).

## Состав

| Файл                  | Роль                                                           |
|-----------------------|----------------------------------------------------------------|
| `Text.json`           | Публичный контракт: содержимое и стиль всего текста.           |
| `TextContent.json`    | Содержимое: `plain` (строка) или `rich` (фрагменты).           |
| `TextRich.json`       | Текст из фрагментов.                                           |
| `TextSpan.json`       | Фрагмент: пока только `text`; набор видов расширяется.         |
| `TextSpanText.json`   | Текстовый фрагмент: значение, стиль, нажатие.                  |
| `TextColor.json`      | Заливка текста: `solid`; набор видов расширяется.              |
| `TextDecoration.json` | Линия: `underline`, `strikethrough`.                           |
| `TextAlignment.json`  | Выравнивание строк: `start`, `center`, `end`.                  |
| `TextTruncation.json` | Место многоточия: `start`, `middle`, `end`.                    |
| `samples/`            | Примеры моделей: `min.json`, `max.json`, `priceWithLink.json`. |

## Варианты

```
content: oneOf
  plain: string
  rich:  { spans: [TextSpan] }
typography, color, decoration?, alignment, maxLines?, truncation?     — для всего текста
color: oneOf
  solid: ColorRole

TextSpan: oneOf
  text: { value, typography?, color?, decoration?, click? }   — незаданное берётся у Text; color — ColorRole
```

| Поле              | Тип                          | Figma (текстовый слой)                   | Compose                      | SwiftUI                                |
|-------------------|------------------------------|------------------------------------------|------------------------------|----------------------------------------|
| `content`         | `plain` или `rich`           | characters; фрагменты — стили диапазонов | `String` / `AnnotatedString` | `String` / `AttributedString`          |
| `typography`      | `TypographyRole`             | текстовый стиль `typography/…`           | `style`                      | шрифт и интервалы роли                 |
| `color`           | `solid(ColorRole)`           | fill с переменной цвета                  | `color`                      | `.foregroundStyle`                     |
| `decoration`      | `underline`, `strikethrough` | `textDecoration`                         | `textDecoration`             | `.underline()`, `.strikethrough()`     |
| `alignment`       | `start`, `center`, `end`     | `textAlignHorizontal`                    | `textAlign`                  | `.multilineTextAlignment`              |
| `maxLines`        | `integer`, необязательное    | `maxLines`, `textTruncation = ENDING`    | `maxLines`, `Ellipsis`       | `.lineLimit`, `.truncationMode(.tail)` |
| `truncation`      | `start`, `middle`, `end`     | только `ENDING`                          | `overflow`                   | `.truncationMode`                      |
| `click` фрагмента | данные события (`payload`)   | не отображается                          | `LinkAnnotation.Clickable`   | ссылка в `AttributedString`            |

1. Линия одна: в Figma подчёркивание и зачёркивание взаимоисключающие.
2. У фрагмента есть только нажатие: долгое нажатие на часть текста платформы не поддерживают.
3. Многоточие в начале и середине — только при `maxLines = 1`: на нескольких строках платформы его не поддерживают
   (Compose обрезает текст без многоточия). В Figma многоточие только в конце, поэтому макет показывает его в конце.
4. `alignment` выравнивает строки внутри компонента. Положение самого компонента в родителе задают `layout` и
   выравнивание контейнера. По вертикали текст прижат к верху компонента: в Figma `textAlignVertical = TOP`. Для
   другого положения задайте `Text` высоту `hug` и выровняйте его в `Container`.
5. Регистр (например, все заглавные) задаётся ролью типографики: в Figma он входит в текстовый стиль.

Сгенерированные примеры: [минимальная модель](samples/min.json) — только обязательные свойства,
[полная модель](samples/max.json) — все свойства.

Старая и новая цена со ссылкой:

<!-- sample: samples/priceWithLink.json -->
```json
{
  "Text": {
    "layout": {
      "width": {
        "sizing": "fill"
      },
      "height": {
        "sizing": "hug"
      },
      "position": "flow"
    },
    "padding": {
      "all": "zero"
    },
    "appearance": {
      "plain": {}
    },
    "content": {
      "rich": {
        "spans": [
          {
            "text": {
              "value": "1 990 ₽",
              "decoration": "strikethrough",
              "color": "onSurfaceMuted"
            }
          },
          {
            "text": {
              "value": " 1 490 ₽ "
            }
          },
          {
            "text": {
              "value": "Условия",
              "decoration": "underline",
              "color": "primary",
              "click": {
                "payload": {
                  "action": "openTerms"
                }
              }
            }
          }
        ]
      }
    },
    "typography": "bodyMedium",
    "color": {
      "solid": "onSurface"
    },
    "alignment": "start"
  }
}
```

Ожидаемая модель на Kotlin:

```kotlin
data class Text(
    override val id: String? = null,
    override val layout: Layout,
    override val padding: Padding,
    override val appearance: Appearance,
    override val events: Set<Event>? = null,
    val content: TextContent,
    val typography: TypographyRole,
    val color: TextColor,
    val decoration: TextDecoration? = null,
    val alignment: TextAlignment,
    val maxLines: Int? = null,
    val truncation: TextTruncation? = null,
) : Component

sealed interface TextContent {
    data class Plain(val value: String) : TextContent
    data class Rich(val spans: List<TextSpan>) : TextContent
}

sealed interface TextColor {
    data class Solid(val value: ColorRole) : TextColor
}

sealed interface TextSpan {
    data class Text(
        val value: String,
        val typography: TypographyRole? = null,
        val color: ColorRole? = null,
        val decoration: TextDecoration? = null,
        val click: EventData? = null,
    ) : TextSpan
}
```

## Рекомендации по использованию

- Используйте роли типографики по назначению: `label…` — для текста элементов управления, `body…` — для содержимого.
- Во фрагментах меняйте роль в пределах одного размера (например, `bodyMedium` и `bodyMediumEmphasized`). Роль
  другого размера применяйте осознанно: высота строки определяется наибольшим фрагментом.
- Для текста на цветном фоне берите парную роль цвета (`onPrimary` на `primary`).
- Ограничивайте `maxLines` в списках и карточках, чтобы длинный текст не ломал раскладку.
- Обрезайте текст через `maxLines` и высоту `hug`, а не фиксированной высотой: при фиксированной высоте платформы
  могут обрезать текст без многоточия.
- Многоточие в середине подходит для имён файлов и адресов, в начале — для путей, где важен конец.
- Ссылки во фрагментах выделяйте не только цветом, но и подчёркиванием.
- Markdown переводите во фрагменты на стороне сервера или приложения, а не передавайте строкой.

## Ограничения и открытые вопросы

- Иконки внутри текста: будут видом фрагмента `TextSpan` после проектирования иконок. Размер иконки выводится из
  типографики, чтобы она масштабировалась вместе с текстом (sp). В Figma — Auto Layout из иконки и текста.
- Фуригана (ruby): будет видом фрагмента `ruby { value, annotation, … }`, когда появится продукт с японским текстом.
  Не определены стиль подписи, её выравнивание над основой, `decoration` и `click` у фрагмента. В Figma текстовый
  слой фуригану не поддерживает, нужна другая структура слоёв (изменение `figma-mapping.md`). В Compose готовой
  поддержки нет.
- Язык фрагмента (`language`): нужен для выбора начертания иероглифов (японское или китайское) в тексте, язык которого
  отличается от языка приложения.
- Градиент текста (`gradient`) добавляется вариантом `TextColor` после проектирования его типа. Только для всего
  текста: у фрагмента цвет — `ColorRole`, потому что градиент на часть текста не поддерживает `AttributedString`.
- Цвет текста по состояниям (например, у ссылки при нажатии) `control` не выражает: для этого нужен отдельный
  компонент.
- Набор вариантов Figma-компонента и `Text.figma.json` готовятся отдельной задачей (`figma-mapping.md`, раздел 11).
  Предполагается: `content` — text-свойство, `typography` и `color` — переопределения слоя, `alignment` и
  `maxLines` — variant.
- Значений по умолчанию в формате пока нет, поэтому `typography`, `color` и `alignment` обязательны.
- Ограничения `maxLines` не меньше 1 и непустого `spans` в формате пока не выражаются.
