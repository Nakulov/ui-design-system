# Event

## Назначение

`Event` — событие, которое компонент передаёт приложению при взаимодействии пользователя. Вариант события задаёт
триггер (нажатие, долгое нажатие, изменение текста), а `payload` — что нужно сделать. Тип `payload` задаёт приложение: дизайн-система его
не интерпретирует и передаёт обработчику без изменений.

Компонент получает набор событий в поле `events` базовой модели `Component` и обрабатывает те, которые поддерживает.
Каждое событие задаётся в наборе не более одного раза. Новые триггеры добавляются вариантами `Event` по мере
необходимости.

## Состав

| Файл                         | Роль                                              |
|------------------------------|---------------------------------------------------|
| `Event.json`                 | Публичный контракт: варианты событий.             |
| `definitions/EventData.json` | Данные события: `payload`.                        |
| `changelog.json`             | Журнал изменений API.                             |
| `samples/`                   | Образцы: по одному на вариант.                    |

## Варианты

| Вариант     | Смысл                            | Compose                                                         | SwiftUI                         |
|-------------|----------------------------------|-----------------------------------------------------------------|---------------------------------|
| `click`     | нажатие                          | `Modifier.clickable` / `onClick`                                | `.onTapGesture` / `Button`      |
| `longPress` | долгое нажатие                   | `Modifier.combinedClickable(onLongClick)`                       | `.onLongPressGesture`           |
| `change`    | изменение текста поля ввода      | `TextFieldState`, `snapshotFlow { text }`                       | `.onChange(of: text)`           |
| `submit`    | Return в однострочном поле ввода | `KeyboardActions` / `onKeyboardAction`                          | `.onSubmit`                     |
| `focus`     | поле ввода получило фокус        | `Modifier.onFocusChanged`                                       | `@FocusState`, `.onChange`      |
| `unfocus`   | поле ввода потеряло фокус        | `Modifier.onFocusChanged`                                       | `@FocusState`, `.onChange`      |
| `reachEnd`  | долистали до конца списка        | `LazyListState`, `snapshotFlow` по последнему видимому элементу | `.onAppear` последнего элемента |

`change`, `submit`, `focus` и `unfocus` поддерживает только `TextField`. Обработчик на платформе получает `payload` без
изменений и текущее значение поля (`TextFieldValue`): в контракте события значения нет — его нет в модели поля, а
`core` не ссылается на `components` (`component-model.md`, раздел 7).

`reachEnd` поддерживает только `ScrollList`: событие срабатывает, когда список долистали до текущего конца содержимого.
Пришла новая порция — конец сдвинулся, и событие сработает снова у нового конца; у перевёрнутого списка (`reversed`)
конец — верх.

Значения `Event` — элементы массива `events` компонента:

<!-- sample: samples/click.json -->
```json
{
  "click": {
    "payload": {
      "action": "openProfile",
      "userId": "42"
    }
  }
}
```

<!-- sample: samples/longPress.json -->
```json
{
  "longPress": {
    "payload": {
      "action": "copyLink"
    }
  }
}
```

<!-- sample: samples/change.json -->
```json
{
  "change": {
    "payload": {
      "action": "searchQueryChanged"
    }
  }
}
```

<!-- sample: samples/submit.json -->
```json
{
  "submit": {
    "payload": {
      "action": "login"
    }
  }
}
```

<!-- sample: samples/focus.json -->
```json
{
  "focus": {
    "payload": {
      "action": "trackFieldFocus",
      "field": "email"
    }
  }
}
```

<!-- sample: samples/unfocus.json -->
```json
{
  "unfocus": {
    "payload": {
      "action": "validateEmail"
    }
  }
}
```

<!-- sample: samples/reachEnd.json -->
```json
{
  "reachEnd": {
    "payload": {
      "action": "loadMoreOrders"
    }
  }
}
```

Ожидаемая модель на Kotlin:

```kotlin
sealed interface Event {
    data class Click(val payload: Payload) : Event
    data class LongPress(val payload: Payload) : Event
    data class Change(val payload: Payload) : Event
    data class Submit(val payload: Payload) : Event
    data class Focus(val payload: Payload) : Event
    data class Unfocus(val payload: Payload) : Event
    data class ReachEnd(val payload: Payload) : Event
}

// Тип дизайн-системы; приложение реализует его своими моделями
interface Payload
```

Использование в коде приложения:

```kotlin
data class OpenProfile(val userId: String) : Payload
data object CopyLink : Payload

Text(
    events = setOf(
        Event.Click(payload = OpenProfile(userId)),
        Event.LongPress(payload = CopyLink),
    ),
)
```

## Рекомендации по использованию

- Задавайте `id` компонента вместе с событиями, чтобы приложение различало источники событий в аналитике и тестах.
- Описывайте в `payload` действие приложения, а не детали интерфейса: что сделать, а не как это выглядит.
- Реализуйте `Payload` через `data class` и `data object` (в Swift — `Hashable`), чтобы модели сравнивались по
  значению.
- Модели с сервера приложение разбирает полиморфной десериализацией своих реализаций `Payload` (например,
  `polymorphic(Payload::class)` в kotlinx.serialization).
- `longPress` не используйте как единственный способ выполнить действие: его трудно обнаружить, и он хуже доступен.

## Ограничения и открытые вопросы

- Набор поддерживаемых событий у каждого компонента в формате пока не выражается. Валидатор деревьев неподдерживаемые
  события пока не проверяет: `change`, `submit`, `focus`, `unfocus` у компонентов, кроме `TextField`, и `reachEnd` у
  компонентов, кроме `ScrollList`, не отклоняются.
- В Figma события не реализуются (`willNotBeReleased`): `payload` — тип приложения, дизайнер его не задаёт, а на
  отрисовку события не влияют.
