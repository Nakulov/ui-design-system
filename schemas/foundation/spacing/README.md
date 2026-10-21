# Spacing

## Назначение

`Spacing` задаёт отступы между компонентами и внутренние отступы (padding) компонентов. Используется единая шкала для
всех видов отступов.

Величины задаются в условных единицах: `dp` на Android, `pt` на iOS, `px` в Figma.

## Состав

| Файл                 | Роль                                                      |
|----------------------|-----------------------------------------------------------|
| `Spacing.json`       | Публичный контракт: значения шкалы и произвольный отступ. |
| `SpacingScale.json`  | Вспомогательное перечисление: именованные значения шкалы. |
| `SpacingCustom.json` | Вспомогательный объект: произвольный отступ вне шкалы.    |

Величины шкалы задаются в темах: по умолчанию — `themes/default/Spacing.values.json`.

## Варианты

Величины справочные: источником истины является `themes/default/Spacing.values.json`.

| Вариант  | Величина по умолчанию |
|----------|-----------------------|
| `zero`   | 0                     |
| `xs2`    | 2                     |
| `xs`     | 4                     |
| `s`      | 8                     |
| `m`      | 12                    |
| `l`      | 16                    |
| `xl`     | 24                    |
| `xl2`    | 32                    |
| `xl3`    | 48                    |
| `custom` | произвольное целое    |

Ожидаемая модель на Kotlin:

```kotlin
sealed interface Spacing {
    data object Zero : Spacing
    data object Xs2 : Spacing
    data object Xs : Spacing
    data object S : Spacing
    data object M : Spacing
    data object L : Spacing
    data object Xl : Spacing
    data object Xl2 : Spacing
    data object Xl3 : Spacing
    data class Custom(val value: Int) : Spacing
}
```

Величины значений шкалы не входят в тип: они генерируются из файла значений и берутся из темы, что позволяет иметь
разные величины для разных режимов. `Custom` несёт собственную величину:

```kotlin
val Spacing.dp: Dp
    @Composable get() = when (this) {
        is Spacing.Custom -> value.dp
        else -> LocalSpacingValues.current.of(this)
    }

Modifier.padding(Spacing.M.dp)
```

## Рекомендации по использованию

- Предпочитайте значения шкалы: они обеспечивают единый ритм интерфейса.
- `custom` используйте только в исключительных случаях, когда ни одно значение шкалы не подходит.
- В Figma `custom` задаётся числом без привязки к переменной; как значение свойства Figma-компонента он недоступен.

## Ограничения и открытые вопросы

- Неотрицательность `custom.value` пока не выражается форматом контракта.
