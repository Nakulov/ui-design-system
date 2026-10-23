# Container

## Назначение

`Container` располагает дочерние компоненты в строку, в колонку (в том числе с переносом) или наложением. Экран собирается из вложенных
контейнеров, как макет в Figma — из вложенных фреймов с Auto Layout.

Контейнер наследует базовую модель `Component`: размеры и положение в родителе (`layout`), внутренние отступы
(`padding`), оформление (`appearance`, контексты `plain` и `control`) и события (`events`). Собственные поля
контейнера — способ расположения детей (`arrangement`) и сами дети (`children`).

## Состав

| Файл                                   | Роль                                                                       |
|----------------------------------------|----------------------------------------------------------------------------|
| `Container.json`                       | Публичный контракт: способ расположения и дети.                            |
| `ContainerArrangement.json`            | Способ расположения: строка, колонка, перенос, наложение.                  |
| `ContainerHorizontal.json`             | Строка: выравнивание по вертикали, распределение.                          |
| `ContainerVertical.json`               | Колонка: выравнивание по горизонтали, распределение.                       |
| `ContainerHorizontalWrap.json`         | Строка с переносом: выравнивание, распределение в строке и строк.          |
| `ContainerVerticalWrap.json`           | Колонка с переносом: выравнивание, распределение в колонке и колонок.      |
| `ContainerOverlay.json`                | Наложение: положение детей по обеим осям.                                  |
| `ContainerHorizontalDistribution.json` | Распределение в строке: `packed` или `spaceBetween`.                       |
| `ContainerVerticalDistribution.json`   | Распределение в колонке: `packed` или `spaceBetween`.                      |
| `ContainerHorizontalPacked.json`       | Дети строки вместе: промежуток и положение группы.                         |
| `ContainerVerticalPacked.json`         | Дети колонки вместе: промежуток и положение группы.                        |
| `ContainerLineDistribution.json`       | Распределение строк или колонок при переносе: `packed` или `spaceBetween`. |
| `ContainerLinePacked.json`             | Строки или колонки вместе: промежуток.                                     |
| `ContainerDistributionMode.json`       | Перечисление распределений без параметров: `spaceBetween`.                 |
| `ContainerHorizontalAlignment.json`    | Положение по горизонтали: `start`, `center`, `end`.                        |
| `ContainerVerticalAlignment.json`      | Положение по вертикали: `top`, `center`, `bottom`.                         |
| `ContainerRowAlignment.json`           | Выравнивание в строке: `top`, `center`, `bottom`, `baseline`.              |

## Варианты

```
arrangement: oneOf
  horizontal     { alignment: top | center | bottom | baseline, distribution }
  vertical       { alignment: start | center | end,             distribution }
  horizontalWrap { alignment: top | center | bottom | baseline, distribution, lineDistribution }
  verticalWrap   { alignment: start | center | end,             distribution, lineDistribution }
  overlay        { horizontal: start | center | end,            vertical: top | center | bottom }

distribution: oneOf
  packed { gap: Spacing, position }    — position по оси контейнера
  spaceBetween

lineDistribution: oneOf               — строки (колонки) при переносе
  packed { gap: Spacing }             — положение группы задаёт alignment
  spaceBetween
```

| Вариант            | Figma                                                                 | Compose                                                                    | SwiftUI                                         |
|--------------------|-----------------------------------------------------------------------|----------------------------------------------------------------------------|-------------------------------------------------|
| `horizontal`       | Auto Layout, horizontal                                               | `Row`                                                                      | `HStack`                                        |
| `vertical`         | Auto Layout, vertical                                                 | `Column`                                                                   | `VStack`                                        |
| `horizontalWrap`   | Auto Layout, horizontal, Wrap                                         | `FlowRow`                                                                  | `FlowLayout` дизайн-системы (`Layout`, iOS 16+) |
| `verticalWrap`     | Auto Layout, vertical, Wrap                                           | `FlowColumn`                                                               | `FlowLayout` дизайн-системы (`Layout`, iOS 16+) |
| `overlay`          | фрейм без Auto Layout, дети с констрейнтами                           | `Box(contentAlignment)`                                                    | `ZStack(alignment:)`                            |
| `packed`           | Spacing between items = `gap`, выравнивание                           | `Arrangement.spacedBy(gap, position)`                                      | `spacing:` и `Spacer` у краёв                   |
| `spaceBetween`     | Spacing between items = Auto                                          | `Arrangement.SpaceBetween`                                                 | `Spacer` между детьми                           |
| `lineDistribution` | `counterAxisSpacing`, `counterAxisAlignContent` (Auto, Space between) | `verticalArrangement` / `horizontalArrangement` у `FlowRow` / `FlowColumn` | параметр `FlowLayout`                           |

1. `alignment` выравнивает детей поперёк оси контейнера. Растяжение поперёк оси задаёт не контейнер, а `layout`
   ребёнка (`fill`).
2. `gap` есть только у `packed`: при `spaceBetween` промежуток вычисляется.
3. В наложении последний ребёнок рисуется сверху. `overlay` задаёт положение детей с `position = flow`; дети с
   `position = pinned` располагаются по своему закреплению. В Figma плагин переводит положение в констрейнты.
4. `spaceAround` и `spaceEvenly` не поддерживаются: в Figma их нет.
5. При переносе один `alignment` выравнивает и детей внутри строки, и группу строк в контейнере: в Figma оба
   задаются `counterAxisAlignItems`. Поэтому у `lineDistribution.packed` нет своего положения. Исключение —
   `baseline`: дети в строке выравниваются по базовой линии, а строки прижаты к верху.
6. Ограничения числа строк и детей в строке (`maxLines`, `maxItemsInEachRow` в Compose) не поддерживаются: в Figma
   их нет.
7. В Figma контейнер — фрейм, а не Figma-компонент: он создаётся только при отрисовке модели (`figma-mapping.md`,
   разделы 3.4 и 10.3).
8. `baseline` есть только у строки (`horizontal`, `horizontalWrap`): в Figma `BASELINE` только у горизонтального
   Auto Layout, у `Column` в Compose его нет. Используется первая базовая линия: другой в Figma нет, многострочный
   текст выравнивается по первой строке. Compose — `Modifier.alignByBaseline()`, SwiftUI — `.firstTextBaseline`, в
   `FlowLayout` — по `ViewDimensions[.firstTextBaseline]`.
9. При `baseline` ребёнок без текста (иконка, плашка) выравнивается нижним краем, как в SwiftUI и Figma; в Compose —
   `Modifier.alignBy { it.measuredHeight }`. Вложенный контейнер выравнивается по базовой линии своего первого текста.

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
      { "Text": { "...": "..." } },
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
        val alignment: ContainerRowAlignment,
        val distribution: ContainerHorizontalDistribution,
    ) : ContainerArrangement

    data class Vertical(
        val alignment: ContainerHorizontalAlignment,
        val distribution: ContainerVerticalDistribution,
    ) : ContainerArrangement

    data class HorizontalWrap(
        val alignment: ContainerRowAlignment,
        val distribution: ContainerHorizontalDistribution,
        val lineDistribution: ContainerLineDistribution,
    ) : ContainerArrangement

    data class VerticalWrap(
        val alignment: ContainerHorizontalAlignment,
        val distribution: ContainerVerticalDistribution,
        val lineDistribution: ContainerLineDistribution,
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
- Для строки из текстов разного размера используйте `baseline`, а не `center`: подписи встанут на одну линию.

## Ограничения и открытые вопросы

- `baseline` с переносом (`layoutWrap = WRAP`) в Figma требует проверки; если не поддерживается, при отрисовке
  используется `top`.
- В SwiftUI готового переноса нет: генератор поставляет собственный `FlowLayout` на протоколе `Layout`.
- Значений по умолчанию в формате пока нет, поэтому поля выравнивания и распределения обязательны.
