# Appearance

## Назначение

`Appearance` задаёт оформление корня компонента: фон, тень, скругление, обводку, наложение поверх содержимого и
обрезку содержимого. Вариант
оформления определяется контекстом компонента: он задаёт, какие состояния у компонента бывают. Поэтому у кнопки нет
состояния «выбран», а у текста — «нажат».

`Appearance` описывает только вид каждого состояния. Само состояние определяют поля компонента (например, доступность,
выбор) и взаимодействие пользователя (нажатие, фокус).

## Состав

| Файл                                    | Роль                                                    |
|-----------------------------------------|---------------------------------------------------------|
| `Appearance.json`                       | Публичный контракт: контексты оформления.               |
| `definitions/AppearancePlain.json`      | Оформление без состояний: фон, тень, скругление и т. д. |
| `definitions/AppearanceFill.json`       | Заливка: фон или наложение.                             |
| `definitions/AppearanceStroke.json`     | Внутренняя обводка.                                     |
| `definitions/AppearanceControl.json`    | Состояния нажимаемого компонента.                       |
| `definitions/AppearanceSelectable.json` | Положения выбора выбираемого компонента.                |
| `definitions/AppearanceFocusable.json`  | Состояния компонента, принимающего фокус ввода.         |
| `docs.json`                             | Манифест документации Figma: секции страницы и примеры. |
| `changelog.json`                        | Журнал изменений API.                                   |
| `demos/`                                | Демонстрации для страницы документации Figma.           |

## Варианты

| Вариант      | Состояния                                      | Примеры           |
|--------------|------------------------------------------------|-------------------|
| `plain`      | нет                                            | текст, карточка   |
| `control`    | `default`, `pressed`, `disabled`               | кнопка            |
| `selectable` | `unselected`, `selected`, в каждом — `control` | чип, чекбокс, таб |
| `focusable`  | `default`, `focused`, `disabled`               | поле ввода        |

Сочетания состояний выражаются вложением: нажатый выбранный чип оформлен `selected.pressed`, недоступный выбранный —
`selected.disabled`.

Поля `plain`:

| Поле           | Тип                                      | Figma         | Compose              | SwiftUI         |
|----------------|------------------------------------------|---------------|----------------------|-----------------|
| `background`   | `solid(ColorRole)`                       | Fill          | `background`         | `.background`   |
| `shadow`       | `Elevation`                              | Effect style  | `shadow`             | `.shadow`       |
| `cornerRadius` | `CornerRadius`                           | Corner radius | `RoundedCornerShape` | `.clipShape`    |
| `stroke`       | `width: BorderWidth`, `color: ColorRole` | Stroke inside | `border`             | `.strokeBorder` |
| `overlay`      | `solid(ColorRole)`                       | верхний слой  | `drawWithContent`    | `.overlay`      |
| `clip`         | `boolean`                                | Clip content  | `clip`               | `.clipped()`    |

Все поля `plain` необязательны: отсутствие означает прозрачный фон, отсутствие тени, обводки и наложения, прямые углы
и содержимое без обрезки. Обводка только внутренняя: она не меняет размер компонента.

Слои корня снизу вверх: тень, фон (`background`), содержимое, наложение (`overlay`), обводка (`stroke`).

1. **Наложение** — заливка поверх содержимого. Нужно, когда содержимое закрывает фон: отклик на нажатие баннера с
   картинкой задаётся `overlay` в `pressed`, а не фоном, который картинка не даёт увидеть.
2. Наложение повторяет форму корня (`cornerRadius`) и обрезается по ней всегда, независимо от `clip`.
3. Наложение только рисуется: нажатия проходят к содержимому.
4. Фон и наложение — один тип `AppearanceFill`: новые виды заливки (например, градиент) появятся в обоих.
5. Платформы: Compose — `Modifier.drawWithContent { drawContent(); drawOutline(shape.createOutline(…), color) }`
   перед `border`; SwiftUI — `.overlay(shape.fill(color))` перед `.strokeBorder`; Figma — верхний прямоугольник в
   форме корня с заливкой переменной `ColorRole`, видимый только при заданном наложении.

Ожидаемая модель на Kotlin:

```kotlin
sealed interface Appearance {
    data class Plain(
        val background: Fill? = null,
        val shadow: Elevation? = null,
        val cornerRadius: CornerRadius? = null,
        val stroke: Stroke? = null,
        val overlay: Fill? = null,
        val clip: Boolean? = null,
    ) : Appearance

    data class Control(val default: Plain, val pressed: Plain, val disabled: Plain) : Appearance
    data class Selectable(val unselected: Control, val selected: Control) : Appearance
    data class Focusable(val default: Plain, val focused: Plain, val disabled: Plain) : Appearance
}

sealed interface Fill {
    data class Solid(val value: ColorRole) : Fill
}

data class Stroke(val width: BorderWidth, val color: ColorRole)
```

## Рекомендации по использованию

- Контекст компонента объявляет его контракт полем `appearanceContext` (`schema-format.md`, раздел 5.2); в Figma он
  задаёт variant-свойства `state` и `selected` (`figma-mapping.md`, раздел 3.5).
- Выбирайте вариант по контексту компонента: `control` для нажимаемых, `selectable` для выбираемых, `focusable` для
  полей ввода, `plain` для остальных.
- Значения берите из ролей и шкал `foundation`: величины задаёт тема.
- Используйте `clip`, когда содержимое (например, изображение) не должно выходить за скругление.
- Если содержимое закрывает фон (картинка, баннер), показывайте нажатие наложением: `overlay` с ролью `overlayPressed`
  в `pressed`. Для приглушения недоступного состояния — наложение в `disabled`.
- Не затемняйте наложением текст ради читаемости: наложение лежит и поверх текста. Затемнение под текстом — отдельный
  слой между картинкой и текстом.

## Ограничения и открытые вопросы

- Каждое состояние задаёт оформление полностью, поэтому общие поля (скругление, обводка) повторяются в состояниях.
  Предполагается решить пресетами оформления.
- Градиент (`gradient`) добавляется вариантом `AppearanceFill` после проектирования его типа.
- Анимация перехода между состояниями (в том числе появления наложения) не задаётся.
- Значений по умолчанию в формате пока нет, поэтому в `Component` поле `appearance` записано обязательным.
