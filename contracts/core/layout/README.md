# Layout

## Назначение

`Layout` описывает поведение компонента внутри родителя: размеры по осям и положение. Поведение родителя
(направление, промежутки, внутренние отступы, выравнивание детей) задаётся полями контейнера.

Модель следует Figma Auto Layout: внешних отступов нет, связей между соседними элементами нет. Сложные раскладки
собираются вложенными контейнерами.

## Состав

| Файл                                          | Роль                                                             |
|-----------------------------------------------|------------------------------------------------------------------|
| `Layout.json`                                 | Публичный контракт: ширина, высота, положение.                   |
| `definitions/LayoutDimension.json`            | Размер по оси: способ задания, минимум, максимум.                |
| `definitions/LayoutSizing.json`               | Способ задания размера: `hug`, `fill` или `fixed`.               |
| `definitions/LayoutSizingMode.json`           | Перечисление способов без величины: `hug`, `fill`.               |
| `definitions/LayoutPosition.json`             | Положение: `flow` или `pinned`.                                  |
| `definitions/LayoutPositionMode.json`         | Перечисление положений без параметров: `flow`.                   |
| `definitions/LayoutPinned.json`               | Закрепление относительно родителя.                               |
| `definitions/LayoutPinHorizontal.json`        | Закрепление по горизонтали: `start`, `end`, `center`, `stretch`. |
| `definitions/LayoutPinVertical.json`          | Закрепление по вертикали: `top`, `bottom`, `center`, `stretch`.  |
| `definitions/LayoutPinOffset.json`            | Закрепление к стороне или центру со смещением.                   |
| `definitions/LayoutPinStretchHorizontal.json` | Растяжение между левой и правой сторонами.                       |
| `definitions/LayoutPinStretchVertical.json`   | Растяжение между верхней и нижней сторонами.                     |
| `definitions/LayoutPinReference.json`         | Точка отсчёта смещений: `edge`, `content`.                       |
| `docs.json`                                   | Манифест документации Figma: секции страницы и примеры.          |
| `changelog.json`                              | Журнал изменений API.                                            |
| `demos/`                                      | Демонстрации для страницы документации Figma.                    |
| `samples/`                                    | Образцы: `min.json`, `max.json`.                                 |

## Варианты

**Размеры** (`width`, `height`):

| Вариант        | Смысл                        | Figma          | Compose                                | SwiftUI                       |
|----------------|------------------------------|----------------|----------------------------------------|-------------------------------|
| `hug`          | по содержимому               | Hug contents   | `wrapContentWidth()`                   | `.fixedSize(...)`             |
| `fill`         | всё доступное пространство   | Fill container | `fillMaxWidth()` или `weight` в строке | `.frame(maxWidth: .infinity)` |
| `fixed` (Size) | фиксированный размер         | Fixed          | `width(...)`                           | `.frame(width:)`              |

`min` и `max` задаются величинами `Size`.

**Положение** (`position`):

- `flow` — в потоке родителя: ребёнок Auto Layout, `Row`/`Column`, `HStack`/`VStack`.
- `pinned` — закреплён относительно родителя: абсолютная позиция с констрейнтами в Figma, `Box` с `align` в Compose,
  `ZStack` с `alignment` в SwiftUI. Смещения задаются величинами `Spacing`; `relativeTo` определяет, отсчитываются
  ли они от края родителя (`edge`) или от области содержимого внутри его отступов (`content`).

Сгенерированные образцы: [минимальная модель](samples/min.json) — только обязательные свойства,
[полная модель](samples/max.json) — все свойства.

Ожидаемая модель на Kotlin:

```kotlin
data class Layout(
    val width: LayoutDimension,
    val height: LayoutDimension,
    val position: LayoutPosition,
)

data class LayoutDimension(
    val sizing: LayoutSizing,
    val min: Size? = null,
    val max: Size? = null,
)

sealed interface LayoutSizing {
    data object Hug : LayoutSizing
    data object Fill : LayoutSizing
    data class Fixed(val value: Size) : LayoutSizing
}

sealed interface LayoutPosition {
    data object Flow : LayoutPosition
    data class Pinned(
        val horizontal: LayoutPinHorizontal,
        val vertical: LayoutPinVertical,
        val relativeTo: LayoutPinReference? = null,
    ) : LayoutPosition
}

sealed interface LayoutPinHorizontal {
    data class Start(val offset: Spacing) : LayoutPinHorizontal
    data class End(val offset: Spacing) : LayoutPinHorizontal
    data class Center(val offset: Spacing) : LayoutPinHorizontal
    data class Stretch(val start: Spacing, val end: Spacing) : LayoutPinHorizontal
}
```

`LayoutPinVertical` устроен так же с вариантами `Top`, `Bottom`, `Center`, `Stretch`.

## Рекомендации по использованию

- Значения `layout` по умолчанию задаёт каждый компонент (например, текст — `fill` × `hug`, кнопка — `hug` × `hug`).
- `pinned` используйте только у детей контейнера наложения.
- Связи между соседними элементами выражайте вложенными контейнерами, как в Figma Auto Layout.

## Ограничения и открытые вопросы

- Значений по умолчанию в формате пока нет, поэтому `width`, `height` и `position` записаны обязательными.
- Констрейнт Scale и координаты x/y не поддерживаются.
- Закреплённые дети в потоковых контейнерах (вне контейнера наложения) не поддерживаются.
