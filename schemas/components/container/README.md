# Container

## Назначение

`Container` располагает дочерние компоненты в строку, в колонку или наложением. Экран собирается из вложенных
контейнеров, как макет в Figma — из вложенных фреймов с Auto Layout.

Контейнер наследует базовую модель `Component`: размеры и положение в родителе (`layout`), внутренние отступы
(`padding`), оформление (`appearance`) и события (`events`). Собственные поля контейнера — способ расположения детей
(`arrangement`) и сами дети (`children`).

## Состав

| Файл                                   | Роль                                                       |
|----------------------------------------|------------------------------------------------------------|
| `Container.json`                       | Публичный контракт: способ расположения и дети.            |
| `ContainerArrangement.json`            | Способ расположения: `horizontal`, `vertical`, `overlay`.  |
| `ContainerHorizontal.json`             | Строка: выравнивание по вертикали, распределение.          |
| `ContainerVertical.json`               | Колонка: выравнивание по горизонтали, распределение.       |
| `ContainerOverlay.json`                | Наложение: положение детей по обеим осям.                  |
| `ContainerHorizontalDistribution.json` | Распределение в строке: `packed` или `spaceBetween`.       |
| `ContainerVerticalDistribution.json`   | Распределение в колонке: `packed` или `spaceBetween`.      |
| `ContainerHorizontalPacked.json`       | Дети строки вместе: промежуток и положение группы.         |
| `ContainerVerticalPacked.json`         | Дети колонки вместе: промежуток и положение группы.        |
| `ContainerDistributionMode.json`       | Перечисление распределений без параметров: `spaceBetween`. |
| `ContainerHorizontalAlignment.json`    | Положение по горизонтали: `start`, `center`, `end`.        |
| `ContainerVerticalAlignment.json`      | Положение по вертикали: `top`, `center`, `bottom`.         |

## Варианты

```
arrangement: oneOf
  horizontal { alignment: top | center | bottom,    distribution }
  vertical   { alignment: start | center | end,     distribution }
  overlay    { horizontal: start | center | end,    vertical: top | center | bottom }

distribution: oneOf
  packed { gap: Spacing, position }    — position по оси контейнера
  spaceBetween
```

| Вариант        | Figma                                       | Compose                               | SwiftUI                       |
|----------------|---------------------------------------------|---------------------------------------|-------------------------------|
| `horizontal`   | Auto Layout, horizontal                     | `Row`                                 | `HStack`                      |
| `vertical`     | Auto Layout, vertical                       | `Column`                              | `VStack`                      |
| `overlay`      | фрейм без Auto Layout, дети с констрейнтами | `Box(contentAlignment)`               | `ZStack(alignment:)`          |
| `packed`       | Spacing between items = `gap`, выравнивание | `Arrangement.spacedBy(gap, position)` | `spacing:` и `Spacer` у краёв |
| `spaceBetween` | Spacing between items = Auto                | `Arrangement.SpaceBetween`            | `Spacer` между детьми         |

1. `alignment` выравнивает детей поперёк оси контейнера. Растяжение поперёк оси задаёт не контейнер, а `layout`
   ребёнка (`fill`).
2. `gap` есть только у `packed`: при `spaceBetween` промежуток вычисляется.
3. В наложении последний ребёнок рисуется сверху. `overlay` задаёт положение детей с `position = flow`; дети с
   `position = pinned` располагаются по своему закреплению. В Figma плагин переводит положение в констрейнты.
4. `spaceAround` и `spaceEvenly` не поддерживаются: в Figma их нет.

Пример модели:

```json
{
  "Container": {
    "layout": { "width": { "sizing": "fill" }, "height": { "sizing": "hug" }, "position": "flow" },
    "padding": { "all": "m" },
    "appearance": { "plain": { "background": { "solid": "surface" } } },
    "arrangement": {
      "vertical": { "alignment": "start", "distribution": { "packed": { "gap": "s", "position": "top" } } }
    },
    "children": [
      { "TextLabel": { "...": "..." } },
      { "Button": { "...": "..." } }
    ]
  }
}
```

Ожидаемая модель на Kotlin:

```kotlin
data class Container(
    override val id: String? = null,
    override val layout: Layout,
    override val padding: Padding,
    override val appearance: Appearance,
    override val events: Set<Event>? = null,
    val arrangement: ContainerArrangement,
    val children: List<Component>? = null,
) : Component

sealed interface ContainerArrangement {
    data class Horizontal(
        val alignment: ContainerVerticalAlignment,
        val distribution: ContainerHorizontalDistribution,
    ) : ContainerArrangement

    data class Vertical(
        val alignment: ContainerHorizontalAlignment,
        val distribution: ContainerVerticalDistribution,
    ) : ContainerArrangement

    data class Overlay(
        val horizontal: ContainerHorizontalAlignment,
        val vertical: ContainerVerticalAlignment,
    ) : ContainerArrangement
}

sealed interface ContainerHorizontalDistribution {
    data class Packed(val gap: Spacing, val position: ContainerHorizontalAlignment) : ContainerHorizontalDistribution
    data object SpaceBetween : ContainerHorizontalDistribution
}
```

## Рекомендации по использованию

- Сложные раскладки собирайте вложенными контейнерами: связей между соседними детьми в модели нет.
- Для отступа между детьми используйте `gap`, а не `padding` детей.
- Для растяжения ребёнка поперёк оси задавайте ему `fill` в `layout`.

## Ограничения и открытые вопросы

- Представление в Figma: контейнер — обычный фрейм, а не Figma-компонент. Правило войдёт в `figma-mapping.md` при
  переносе модели компонентов в спецификацию.
- Перенос детей на новую строку (wrap) и выравнивание по базовой линии текста не поддерживаются.
- Значений по умолчанию в формате пока нет, поэтому поля выравнивания и распределения обязательны.
