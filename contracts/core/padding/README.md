# Padding

## Назначение

`Padding` задаёт внутренние отступы корня компонента: расстояние от краёв компонента до его содержимого. Внешних
отступов в модели нет: расстояние между компонентами задаёт контейнер (промежутки и собственные отступы), как в Figma
Auto Layout.

Стороны задаются как `start` и `end`, чтобы отступы отражались для языков с письмом справа налево. Значения берутся из
шкалы `Spacing`.

## Состав

| Файл                    | Роль                                                      |
|-------------------------|-----------------------------------------------------------|
| `Padding.json`          | Публичный контракт: формы задания отступов.               |
| `PaddingSymmetric.json` | Отступы по горизонтали и по вертикали.                    |
| `PaddingSides.json`     | Отступы для каждой стороны.                               |

## Варианты

| Вариант     | Данные                          | Compose                                  | SwiftUI                                          | Figma                       |
|-------------|---------------------------------|------------------------------------------|--------------------------------------------------|-----------------------------|
| `all`       | одно значение `Spacing`         | `PaddingValues(all)`                     | `.padding(x)`                                    | одинаковые отступы          |
| `symmetric` | `horizontal`, `vertical`        | `PaddingValues(horizontal, vertical)`    | `.padding(.horizontal, x).padding(.vertical, y)` | горизонтальные/вертикальные |
| `sides`     | `start`, `top`, `end`, `bottom` | `PaddingValues(start, top, end, bottom)` | `EdgeInsets(top:leading:bottom:trailing:)`       | раздельные по сторонам      |

В Figma есть только стороны left и right; плагин подставляет `start` как left и `end` как right.

Ожидаемая модель на Kotlin:

```kotlin
sealed interface Padding {
    data class All(val value: Spacing) : Padding
    data class Symmetric(val horizontal: Spacing, val vertical: Spacing) : Padding
    data class Sides(val start: Spacing, val top: Spacing, val end: Spacing, val bottom: Spacing) : Padding
}
```

## Рекомендации по использованию

- Значения по умолчанию задаёт каждый компонент по дизайну.
- Переопределяйте отступы в основном у контейнеров; ручная подстройка отступов у остальных компонентов нарушает
  единообразие интерфейса.
- Используйте самую короткую подходящую форму: `all`, затем `symmetric`, затем `sides`.

## Ограничения и открытые вопросы

- Значений по умолчанию в формате пока нет, поэтому в `Component` поле `padding` записано обязательным.
