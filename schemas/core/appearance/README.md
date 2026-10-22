# Appearance

## Назначение

`Appearance` задаёт оформление корня компонента: фон, тень, скругление, обводку и обрезку содержимого. Вариант
оформления определяется контекстом компонента: он задаёт, какие состояния у компонента бывают. Поэтому у кнопки нет
состояния «выбран», а у текста — «нажат».

`Appearance` описывает только вид каждого состояния. Само состояние определяют поля компонента (например, доступность,
выбор) и взаимодействие пользователя (нажатие, фокус).

## Состав

| Файл                        | Роль                                                    |
|-----------------------------|---------------------------------------------------------|
| `Appearance.json`           | Публичный контракт: контексты оформления.               |
| `AppearancePlain.json`      | Оформление без состояний: фон, тень, скругление и т. д. |
| `AppearanceBackground.json` | Заливка фона.                                           |
| `AppearanceStroke.json`     | Внутренняя обводка.                                     |
| `AppearanceControl.json`    | Состояния нажимаемого компонента.                       |
| `AppearanceSelectable.json` | Положения выбора выбираемого компонента.                |
| `AppearanceFocusable.json`  | Состояния компонента, принимающего фокус ввода.         |

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
| `clip`         | `boolean`                                | Clip content  | `clip`               | `.clipped()`    |

Все поля `plain` необязательны: отсутствие означает прозрачный фон, отсутствие тени и обводки, прямые углы и
содержимое без обрезки. Обводка только внутренняя: она не меняет размер компонента.

Ожидаемая модель на Kotlin:

```kotlin
sealed interface Appearance {
    data class Plain(
        val background: Background? = null,
        val shadow: Elevation? = null,
        val cornerRadius: CornerRadius? = null,
        val stroke: Stroke? = null,
        val clip: Boolean? = null,
    ) : Appearance

    data class Control(val default: Plain, val pressed: Plain, val disabled: Plain) : Appearance
    data class Selectable(val unselected: Control, val selected: Control) : Appearance
    data class Focusable(val default: Plain, val focused: Plain, val disabled: Plain) : Appearance
}

sealed interface Background {
    data class Solid(val value: ColorRole) : Background
}

data class Stroke(val width: BorderWidth, val color: ColorRole)
```

## Рекомендации по использованию

- Выбирайте вариант по контексту компонента: `control` для нажимаемых, `selectable` для выбираемых, `focusable` для
  полей ввода, `plain` для остальных.
- Значения берите из ролей и шкал `foundation`: величины задаёт тема.
- Используйте `clip`, когда содержимое (например, изображение) не должно выходить за скругление.

## Ограничения и открытые вопросы

- Каждое состояние задаёт оформление полностью, поэтому общие поля (скругление, обводка) повторяются в состояниях.
  Предполагается решить пресетами оформления.
- Какие варианты принимает конкретный компонент, в формате пока не выражается; несоответствие ловит валидатор деревьев.
- Градиент фона (`gradient`) добавляется вариантом `AppearanceBackground` после проектирования его типа.
- Значений по умолчанию в формате пока нет, поэтому в `Component` поле `appearance` записано обязательным.
