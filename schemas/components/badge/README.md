# Badge

## Назначение

`Badge` — метка внимания: форма с текстом, символом или картинкой либо без содержимого. Счётчик «9» и «99+», точка
«есть новое», галочка или флаг на аватаре. Используется как содержимое индикатора (`indicators` базовой модели) и в
строке рядом с текстом.

Смысл содержимого определяет сервер или приложение: бейдж показывает любую строку, а сокращение («99+») и
форматирование чисел зависят от продукта. Собственное поведение бейджа — форма: ширина не меньше высоты, поэтому один
знак даёт круг, а длинный текст — таблетку той же высоты.

Вид (фон, скругление, размер, отступы) задают поля базовой модели. Типовая геометрия описана в рекомендациях; вид из
темы появится вместе с пресетами типовых компонентов.

Статусные плашки и теги («Оплачен», «Новинка» в списке) — не бейджи, а отдельные компоненты.

## Состав

| Файл                | Роль                                                     |
|---------------------|----------------------------------------------------------|
| `Badge.json`        | Публичный контракт: необязательное содержимое.           |
| `BadgeContent.json` | Содержимое: `text` или `image`.                          |
| `BadgeText.json`    | Текст в одну строку: значение, роль типографики, цвет.   |
| `samples/`          | Примеры моделей: `min.json`, `max.json`, `count`, `dot`. |

Картинка — `ImageSource` компонента `Image` (`schemas/components/image`).

## Варианты

```
content?: oneOf                            — нет: только фон (точка)
  text:  { value: string, typography: TypographyRole, color: ColorRole }
  image: ImageSource                       — символ или картинка по адресу, как у Image
```

| Поле            | Figma                                | Compose                                    | SwiftUI                            |
|-----------------|--------------------------------------|--------------------------------------------|------------------------------------|
| корень          | Auto Layout, `minWidth` равен высоте | `Box` с минимальной шириной, равной высоте | `Layout` с `minWidth = height`     |
| `content` нет   | variant `content = none`             | пустой `Box`                               | пустая форма                       |
| `content.text`  | variant `text`, text-свойство        | `Text(maxLines = 1, softWrap = false)`     | `Text(…).lineLimit(1).fixedSize()` |
| `content.image` | variant `image`, слои как у `Image`  | как `Image`                                | как `Image`                        |

1. **Ширина не меньше высоты.** При ширине `hug` бейдж с одним знаком или символом — круг (при `cornerRadius = full`),
   с длинным текстом — таблетка той же высоты. Высота задаётся `layout` или содержимым с `padding`.
2. **Текст** — в одну строку, без переноса и многоточия: бейдж растёт по тексту. Длину ограничивает сервер.
3. **Содержимое по центру.** Символ — в собственном размере. Картинка по адресу заполняет область внутри `padding` по
   своему `scale`; размер бейджа с такой картинкой задавайте `fixed`: до загрузки собственного размера нет.
4. **Без содержимого** бейдж — фон. При `hug` его размер задаёт только `padding`, поэтому точку задавайте `fixed`.
5. **Контекст только `plain`:** бейдж — метка, а не кнопка. Нажимается носитель.

Ожидаемая модель на Kotlin:

```kotlin
data class Badge(
    override val id: String? = null,
    override val layout: Layout,
    override val padding: Padding,
    override val appearance: Appearance,
    override val indicators: List<ComponentIndicator>? = null,
    override val events: Set<Event>? = null,
    val content: BadgeContent? = null,
) : Component

sealed interface BadgeContent {
    data class Text(
        val value: String,
        val typography: TypographyRole,
        val color: ColorRole,
    ) : BadgeContent

    data class Image(val value: ImageSource) : BadgeContent
}
```

Сгенерированные примеры: [минимальная модель](samples/min.json) — только обязательные свойства,
[полная модель](samples/max.json) — все свойства.

Счётчик:

<!-- sample: samples/count.json -->
```json
{
  "Badge": {
    "layout": {
      "width": {
        "sizing": "hug"
      },
      "height": {
        "sizing": {
          "fixed": "xs"
        }
      },
      "position": "flow"
    },
    "padding": {
      "symmetric": {
        "horizontal": "xs",
        "vertical": "zero"
      }
    },
    "appearance": {
      "plain": {
        "background": {
          "solid": "error"
        },
        "cornerRadius": "full"
      }
    },
    "content": {
      "text": {
        "value": "99+",
        "typography": "labelSmall",
        "color": "onError"
      }
    }
  }
}
```

Точка:

<!-- sample: samples/dot.json -->
```json
{
  "Badge": {
    "layout": {
      "width": {
        "sizing": {
          "fixed": "xs2"
        }
      },
      "height": {
        "sizing": {
          "fixed": "xs2"
        }
      },
      "position": "flow"
    },
    "padding": {
      "all": "zero"
    },
    "appearance": {
      "plain": {
        "background": {
          "solid": "error"
        },
        "cornerRadius": "full"
      }
    }
  }
}
```

Бейдж как индикатор на аватаре — пример `avatarOnline` в README `Image`.

## Рекомендации по использованию

Типовая геометрия до появления пресетов:

| Вид     | Размер                            | Отступы             | Оформление                                        |
|---------|-----------------------------------|---------------------|---------------------------------------------------|
| точка   | `fixed` `xs2` × `xs2`             | `zero`              | фон `error`, `cornerRadius = full`                |
| счётчик | ширина `hug`, высота `fixed` `xs` | по горизонтали `xs` | фон `error`, текст `labelSmall` `onError`, `full` |
| символ  | `hug` или `fixed` `xs`            | `xs2` или `zero`    | фон роли, символ — парная роль `on…`              |

- Цвет текста и символа берите парным к фону: `onError` на `error`, `onPrimary` на `primary`.
- Сокращайте большие числа на стороне сервера или приложения («99+»): бейдж показывает строку как есть.
- Держите текст коротким: бейдж не переносит и не обрезает текст.
- Бейдж на носителе задавайте индикатором (`indicators`) с зазором `gap`, а не дочерним элементом: так он выступает за
  носитель и не меняет раскладку.

## Ограничения и открытые вопросы

- Вид из темы (смысловое поле вроде `tone` с парами ролей и фиксированная геометрия) — вместе с пресетами типовых
  компонентов: нужен механизм значений по умолчанию.
- Правило «ширина не меньше высоты» не выражается полями `layout`: это поведение компонента.
