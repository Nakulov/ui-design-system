# Text

## Назначение

`Text` отображает текст: простой или составленный из фрагментов с разным стилем, ссылками и символами. Стиль всего текста
задаётся ролями `foundation` — типографика `TypographyRole`, заливка `TextColor` из ролей `ColorRole` — и линией (`decoration`); фрагменты
переопределяют стиль для своей части текста. Величины берутся из темы.

Компонент наследует базовую модель `Component` с контекстом оформления `plain`: текст бывает статичным и
кликабельным (`events`). Вид нажатия и недоступности задаёт `Pressable` над текстом. Фон, отступы и обводка задаются
полями базовой модели (`appearance`, `padding`).

## Состав

| Файл                              | Роль                                                                     |
|-----------------------------------|--------------------------------------------------------------------------|
| `Text.json`                       | Публичный контракт: содержимое и стиль всего текста.                     |
| `definitions/TextContent.json`    | Содержимое: `plain` (строка) или `rich` (фрагменты).                     |
| `definitions/TextRich.json`       | Текст из фрагментов.                                                     |
| `definitions/TextSpan.json`       | Фрагмент: `text` или `icon`; набор видов расширяется.                    |
| `definitions/TextSpanText.json`   | Текстовый фрагмент: значение, стиль, нажатие.                            |
| `definitions/TextSpanIcon.json`   | Символ внутри текста: имя из словаря `Icon`, роль, цвет.                 |
| `definitions/TextColor.json`      | Заливка текста: `solid`; набор видов расширяется.                        |
| `definitions/TextDecoration.json` | Линия: `underline`, `strikethrough`.                                     |
| `definitions/TextAlignment.json`  | Выравнивание строк: `start`, `center`, `end`.                            |
| `definitions/TextTruncation.json` | Место многоточия: `start`, `middle`, `end`.                              |
| `figma.json`                      | Figma-компоненты `Text` и `text/icon`: образцы, свойства.                |
| `docs.json`                       | Манифест документации Figma: секции страницы и примеры.                  |
| `changelog.json`                  | Журнал изменений API.                                                    |
| `samples/`                        | Образцы: `min`, `max`, `priceWithLink`, `inStock`, `label`, `iconLabel`. |

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
  icon: { name: Icon, typography?, color? }                   — незаданное берётся у Text; color — ColorRole
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
| `icon` фрагмента  | символ `Icon`                | компонент `text/icon` (правило 10)       | `InlineTextContent`          | `Text("\(Image(…))")`                  |

1. Линия одна: в Figma подчёркивание и зачёркивание взаимоисключающие.
2. У фрагмента есть только нажатие: долгое нажатие на часть текста платформы не поддерживают.
3. Многоточие в начале и середине — только при `maxLines = 1`: на нескольких строках платформы его не поддерживают
   (Compose обрезает текст без многоточия). В Figma многоточие только в конце, поэтому макет показывает его в конце.
4. `alignment` выравнивает строки внутри компонента. Положение самого компонента в родителе задают `layout` и
   выравнивание контейнера. По вертикали текст прижат к верху компонента: в Figma `textAlignVertical = TOP`. Для
   другого положения задайте `Text` высоту `hug` и выровняйте его в `Container`.
5. Регистр (например, все заглавные) задаётся ролью типографики: в Figma он входит в текстовый стиль.
6. **Символ при обрезке** — один неделимый знак: виден целиком или скрыт многоточием вместе с соседним текстом.
   `Доставка завтра ⓘ` при нехватке места становится `Доставка зав…`; при `start` и `middle` так же.
7. **Символ — знак строки:** стоит между словами и переносится вместе с ними. Размер символа равен кеглю роли
   типографики фрагмента (или `Text`), поэтому он масштабируется вместе с текстом (sp, Dynamic Type); рисунок
   квадратный. Compose — `appendInlineContent` и `Placeholder` размером `1.em`; SwiftUI — символ темы, собранный как
   SF Symbol, интерполируется в `Text`.
8. **Выравнивание символа** по вертикали — по центру строчных букв: Compose `PlaceholderVerticalAlign.TextCenter`,
   SwiftUI — смещение базовой линии. Параметров размера и выравнивания у символа нет.
9. **Нажатия у символа нет:** SwiftUI не вешает ссылку на картинку внутри `Text`. Шеврон у ссылки — оформление,
   нажимается текстовый фрагмент.
10. **В Figma** текстовый слой не содержит векторов, поэтому текст с символами по краям — компонент библиотеки
    `text/icon`: строка `[символ]? [текст] [символ]?` (`figma-mapping.md`, раздел 10.5). Ограничения макета: символ в
    середине фразы заменяется пробелом; при переносе символы прижаты к первой строке, а символ после текста стоит
    справа от всего блока; символы по краям не обрезаются — многоточие ставится только в тексте
    (`Доставка зав… ⓘ`).

Сгенерированные образцы: [минимальная модель](samples/min.json) — только обязательные свойства,
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

Наличие товара с символом:

<!-- sample: samples/inStock.json -->
```json
{
  "Text": {
    "layout": {
      "width": {
        "sizing": "hug"
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
            "icon": {
              "name": "check",
              "color": "success"
            }
          },
          {
            "text": {
              "value": " В наличии, доставка завтра"
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
    override val indicators: List<ComponentIndicator>? = null,
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

    // Вариант Icon затеняет тип foundation Icon внутри TextSpan, поэтому тип указан полным именем
    // (пакет задаёт генератор).
    data class Icon(
        val name: foundation.Icon,
        val typography: TypographyRole? = null,
        val color: ColorRole? = null,
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
- Символ, который должен переноситься вместе со словом или стоять в середине фразы, задавайте фрагментом `icon`. Строка
  из `Image` и `Text` подходит, когда символ стоит сбоку от всего блока текста.
- Не делайте символ единственным носителем смысла ссылки: нажимается текст, а не символ.
- Markdown переводите во фрагменты на стороне сервера или приложения, а не передавайте строкой.

## Ограничения и открытые вопросы

- Размер символа в компоненте Figma `text/icon` привязан к переменной кегля роли образца
  (`typography/<роль>/fontSize`): меняется вместе с темой, но не при замене текстового стиля в экземпляре — тогда
  размер символа задаётся вручную.
- Фуригана (ruby): будет видом фрагмента `ruby { value, annotation, … }`, когда появится продукт с японским текстом.
  Не определены стиль подписи, её выравнивание над основой, `decoration` и `click` у фрагмента. В Figma текстовый
  слой фуригану не поддерживает, нужна другая структура слоёв (изменение `figma-mapping.md`). В Compose готовой
  поддержки нет.
- Язык фрагмента (`language`): нужен для выбора начертания иероглифов (японское или китайское) в тексте, язык которого
  отличается от языка приложения.
- Градиент текста (`gradient`) добавляется вариантом `TextColor` после проектирования его типа. Только для всего
  текста: у фрагмента цвет — `ColorRole`, потому что градиент на часть текста не поддерживает `AttributedString`.
- Цвет текста по состояниям (например, у ссылки при нажатии) выражается `Pressable`: в его дереве `pressed` у
  `Text` другой цвет (README [`Pressable`](../pressable/README.md)).
- В библиотеке Figma `Text` — образец `label`, `text/icon` — образец `iconLabel` (`figma.json`). `content` —
  text-свойство; `typography`, `color`, `decoration`, `alignment` и `maxLines` — переопределения текстового слоя, а не
  variant: дизайнер меняет их в панели текста. `truncation` в Figma не отображается: многоточие только в конце.
- Значений по умолчанию в формате пока нет, поэтому `typography`, `color` и `alignment` обязательны.
- Ограничения `maxLines` не меньше 1 и непустого `spans` в формате пока не выражаются.
