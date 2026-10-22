# Модель компонентов

**Статус:** Черновик, в обсуждении  
**Версия спецификации:** 0.1

Документ фиксирует согласованные решения по универсальному контракту компонентов. Он будет дополняться по мере
обсуждения и затем войдёт в спецификацию формата (`schema-format.md`).

## 1. Назначение

Компонент — модель данных: экран описывается деревом моделей, а платформы (Android, iOS) и Figma отрисовывают модели.
Модели создаются как в коде приложений, так и на сервере (server-driven UI), поэтому они сериализуемы и не содержат
кода отрисовки.

## 2. Принципы

1. **Модель — только данные.** Отрисовка модели (Compose, SwiftUI) генерируется для платформы и в контракт не входит.
2. **В контракте только то, что выражается в Figma.** Раскладка следует модели Figma Auto Layout: внешних отступов
   (margin) нет, связей между соседними элементами нет.
3. **Структура повторяет слои Figma.** Вложенность моделей соответствует вложенности фреймов в Figma.

## 3. Базовая модель `Component`

Каждый компонент расширяет базовую модель `Component`. Её поля:

| Поле         | Статус                 | Контракт                             |
|--------------|------------------------|--------------------------------------|
| `id`         | Согласовано (раздел 5) | свойство `Component`                 |
| `layout`     | Согласовано (раздел 4) | `schemas/core/layout/Layout`         |
| `padding`    | Согласовано (раздел 6) | `schemas/core/padding/Padding`       |
| `appearance` | Согласовано (раздел 8) | `schemas/core/appearance/Appearance` |
| `events`     | Согласовано (раздел 7) | `schemas/core/events/Event`          |

Базовая модель: `schemas/core/component/Component`.

Остальные поля обсуждаются.

## 4. `layout`

`layout` описывает поведение компонента внутри родителя: размеры и положение. Поведение родителя (направление,
промежутки, внутренние отступы, выравнивание детей) задаётся собственными полями контейнера.

```
layout:
  width:   { sizing: hug | fill | fixed(Size), min?: Size, max?: Size }
  height:  { sizing: hug | fill | fixed(Size), min?: Size, max?: Size }
  position:
    flow                                   — по умолчанию
    pinned:                                — только в контейнере наложения
      horizontal: start(offset) | end(offset) | center(offset) | stretch(start, end)
      vertical:   top(offset) | bottom(offset) | center(offset) | stretch(top, bottom)
      relativeTo: edge (по умолчанию) | content
```

### 4.1. Размеры

| Вариант | Смысл                       | Figma          | Compose                                | SwiftUI                       |
|---------|-----------------------------|----------------|----------------------------------------|-------------------------------|
| `hug`   | по содержимому              | Hug contents   | `wrapContentWidth()`                   | `.fixedSize(...)`             |
| `fill`  | всё доступное пространство  | Fill container | `fillMaxWidth()` или `weight` в строке | `.frame(maxWidth: .infinity)` |
| `fixed` | фиксированный размер `Size` | Fixed          | `width(...)`                           | `.frame(width:)`              |

1. `min` и `max` задаются величинами `Size` и соответствуют min/max width/height в Figma, `widthIn` в Compose,
   `frame(minWidth:maxWidth:)` в SwiftUI.
2. Смысл `fill` уточняется родителем: в строке это доля свободного места (вес), в колонке и контейнере наложения —
   растяжение по оси.

### 4.2. Положение

1. `flow` — компонент располагается в потоке родителя (ребёнок Auto Layout, `Row`/`Column`, `HStack`/`VStack`).
2. `pinned` — компонент закреплён относительно родителя (абсолютная позиция с констрейнтами в Figma, `Box` с `align`
   в Compose, `ZStack` с `alignment` в SwiftUI). Допустим только у детей контейнера наложения.
3. Смещения задаются величинами `Spacing`. Смещение — позиция, а не отступ: размер и область нажатия компонента не
   меняются.
4. `relativeTo: edge` — смещение от края родителя без учёта его `padding` (как у абсолютных детей в Figma);
   `relativeTo: content` — от области содержимого внутри `padding`. В Figma для `content` плагин прибавляет отступ
   родителя к смещению.
5. Констрейнт Scale и координаты x/y не используются: на платформах им нет идиоматичного соответствия, а `pinned` со
   смещением покрывает те же случаи адаптивно.

### 4.3. Значения по умолчанию

Значения `layout` по умолчанию задаёт каждый компонент (например, текст — `fill` × `hug`, кнопка — `hug` × `hug`).

## 5. `id`

Необязательная строка без ограничений формата.

| Назначение              | Compose                   | SwiftUI                        |
|-------------------------|---------------------------|--------------------------------|
| UI-тесты                | `Modifier.testTag(id)`    | `.accessibilityIdentifier(id)` |
| Источник события        | передаётся с событием     | передаётся с событием          |
| Ключ элемента в списках | `key = id` в `LazyColumn` | `ForEach(..., id: \.id)`       |
| Аналитика               | по `id`                   | по `id`                        |

1. Если `id` задан, он уникален в пределах дерева модели. Уникальность относится к дереву, а не к схеме: её проверяет
   валидатор деревьев и генератор в отладочной сборке.
2. В Figma `id` хранится в `sharedPluginData` instance (пространство имён `uiContracts`, как в `figma-mapping.md`,
   раздел 7.1). Дизайнер его не видит; плагин использует его, чтобы находить слой при повторной синхронизации и
   сопоставлять слои с моделью. Правило войдёт в `figma-mapping.md` при переносе модели компонентов в спецификацию.

## 6. `padding`

Внутренние отступы корня компонента; внешних отступов в модели нет. Тип `Padding` — `oneOf`:

| Вариант     | Данные                          | Compose                                  | SwiftUI                                    |
|-------------|---------------------------------|------------------------------------------|--------------------------------------------|
| `all`       | одно значение `Spacing`         | `PaddingValues(all)`                     | `.padding(x)`                              |
| `symmetric` | `horizontal`, `vertical`        | `PaddingValues(horizontal, vertical)`    | `.padding(.horizontal, x)` и т. д.         |
| `sides`     | `start`, `top`, `end`, `bottom` | `PaddingValues(start, top, end, bottom)` | `EdgeInsets(top:leading:bottom:trailing:)` |

1. Стороны задаются как `start` и `end` для поддержки письма справа налево; в Figma плагин подставляет left и right.
2. В Figma поле соответствует отступам Auto Layout корня instance, которые можно переопределять.
3. Значения по умолчанию задаёт каждый компонент. Переопределять отступы рекомендуется в основном у контейнеров.

## 7. `events`

Необязательный набор событий, которые компонент передаёт приложению при взаимодействии пользователя.

```
events: Set<Event>                 — каждый вариант не более одного раза
Event: oneOf
  click(payload)
  longPress(payload)
payload: Payload                   — тип задаёт приложение
```

| Вариант     | Смысл          | Compose                                   | SwiftUI                    |
|-------------|----------------|-------------------------------------------|----------------------------|
| `click`     | нажатие        | `Modifier.clickable` / `onClick`          | `.onTapGesture` / `Button` |
| `longPress` | долгое нажатие | `Modifier.combinedClickable(onLongClick)` | `.onLongPressGesture`      |

1. `payload` описывает, что нужно сделать. Его тип — `Payload` (`schema-format.md`, раздел 4.1): на Android
   `interface Payload`, на iOS `protocol Payload`; приложение реализует его своими моделями. Дизайн-система не знает
   действий приложения и передаёт `payload` обработчику без изменений. Модели с сервера приложение получает
   полиморфной десериализацией своих реализаций `Payload`.
2. Поле — массив с уникальными вариантами (`schema-format.md`, раздел 5.1), поэтому одно событие нельзя задать дважды.
   Новые триггеры добавляются вариантами `Event` по мере необходимости.
3. Компонент обрабатывает события, которые поддерживает. Неподдерживаемое событие проверяет валидатор деревьев, как
   уникальность `id`; в формате набор поддерживаемых событий пока не выражается.
4. В Figma события не реализуются (`willNotBeReleased` у свойства `events` и контракта `Event`): `payload` — тип
   приложения, дизайнер его не задаёт, а на отрисовку события не влияют.

## 8. `appearance`

Оформление корня компонента. Тип `Appearance` — `oneOf` по контексту компонента: контекст задаёт набор состояний, поэтому
у кнопки нет состояния «выбран», а у текста — «нажат».

```
appearance:
  plain:      { background?, shadow?, cornerRadius?, stroke?, clip? }
  control:    { default: plain, pressed: plain, disabled: plain }          — кнопка
  selectable: { unselected: control, selected: control }                   — чип, чекбокс, таб
  focusable:  { default: plain, focused: plain, disabled: plain }          — поле ввода
background: solid(ColorRole)
stroke:     { width: BorderWidth, color: ColorRole }                       — только внутренняя
```

| Поле `plain`   | Тип                         | Figma         | Compose              | SwiftUI         |
|----------------|-----------------------------|---------------|----------------------|-----------------|
| `background`   | `solid(ColorRole)`          | Fill          | `background`         | `.background`   |
| `shadow`       | `Elevation`                 | Effect style  | `shadow`             | `.shadow`       |
| `cornerRadius` | `CornerRadius`              | Corner radius | `RoundedCornerShape` | `.clipShape`    |
| `stroke`       | `BorderWidth` + `ColorRole` | Stroke inside | `border`             | `.strokeBorder` |
| `clip`         | `boolean`                   | Clip content  | `clip`               | `.clipped()`    |

1. Поля `plain` необязательны: отсутствие означает прозрачный фон, отсутствие тени и обводки, прямые углы и содержимое
   без обрезки.
2. Сочетания состояний выражаются вложением контекстов: нажатый выбранный чип оформлен `selected.pressed`.
3. `Appearance` описывает только вид состояний. Состояние определяют поля компонента (доступность, выбор) и
   взаимодействие пользователя (нажатие, фокус).
4. Фон — `oneOf`, чтобы градиент (`gradient`) добавился вариантом без несовместимого изменения.
5. Обводка только внутренняя: не меняет размер компонента; в Compose `border` рисует внутри, в Figma — Stroke inside.
6. В Figma `plain` соответствует оформлению корневого фрейма instance (fill, effect style, corner radius, stroke, clip
   content), состояния — variant-свойству состояния в Component Set. Правила войдут в `figma-mapping.md` при переносе
   модели компонентов в спецификацию.
7. Каждое состояние задаёт оформление полностью; повтор общих полей предполагается решить пресетами оформления.

## 9. Контейнер

`Container` (`schemas/components/container/Container`) — первый компонент: располагает детей в строку, в колонку (в том
числе с переносом) или наложением. Наследует `Component` (`extends`, `schema-format.md`, раздел 5.2).

```
arrangement: oneOf
  horizontal { alignment: top | center | bottom,    distribution }
  vertical   { alignment: start | center | end,     distribution }
  horizontalWrap { alignment: top | center | bottom, distribution, lineDistribution }
  verticalWrap   { alignment: start | center | end,  distribution, lineDistribution }
  overlay    { horizontal: start | center | end,    vertical: top | center | bottom }
distribution: oneOf
  packed { gap: Spacing, position }               — position по оси контейнера
  spaceBetween
lineDistribution: oneOf                           — строки (колонки) при переносе
  packed { gap: Spacing }                         — положение группы задаёт alignment
  spaceBetween
children?: [Component]                            — любой компонент, порядок значим
```

| Вариант          | Figma                                       | Compose                               | SwiftUI                      |
|------------------|---------------------------------------------|---------------------------------------|------------------------------|
| `horizontal`     | Auto Layout, horizontal                     | `Row`                                 | `HStack`                     |
| `vertical`       | Auto Layout, vertical                       | `Column`                              | `VStack`                     |
| `horizontalWrap` | Auto Layout, horizontal, Wrap               | `FlowRow`                             | свой `FlowLayout` (`Layout`) |
| `verticalWrap`   | Auto Layout, vertical, Wrap                 | `FlowColumn`                          | свой `FlowLayout` (`Layout`) |
| `overlay`        | фрейм без Auto Layout, дети с констрейнтами | `Box(contentAlignment)`               | `ZStack(alignment:)`         |
| `packed`         | Spacing between items = `gap`               | `Arrangement.spacedBy(gap, position)` | `spacing:`, `Spacer`         |
| `spaceBetween`   | Spacing between items = Auto                | `Arrangement.SpaceBetween`            | `Spacer` между детьми        |

1. Один контейнер с вариантами вместо отдельных `Row`, `Column`, `Overlay`: в Figma направление — свойство фрейма, и
   сервер меняет направление, не меняя тип компонента. `oneOf` исключает бессмысленные сочетания (`gap` у наложения).
2. `gap` есть только у `packed`: при `spaceBetween` промежуток вычисляется.
3. `alignment` выравнивает детей поперёк оси. Растяжение задаёт `layout` ребёнка (`fill`), поэтому `stretch` в
   выравнивании нет.
4. `spaceAround` и `spaceEvenly` не используются: в Figma их нет.
5. В наложении `overlay` задаёт положение детей с `position = flow`; дети с `position = pinned` располагаются по
   своему закреплению. Последний ребёнок рисуется сверху. В Figma плагин переводит положение в констрейнты.
6. `children` необязательно: контейнер без детей — оформленный блок (плашка, разделитель).
7. В Figma контейнер — обычный фрейм, а не Figma-компонент.
8. Перенос — отдельные варианты `horizontalWrap` и `verticalWrap`, а не флаг: у переноса есть промежуток между
   строками, а оси задают типы выравнивания. Один `alignment` выравнивает и детей в строке, и группу строк: в Figma
   оба задаются `counterAxisAlignItems`. `maxLines` и `maxItemsInEachRow` не используются: в Figma их нет.

## 10. Открытые вопросы

1. Остальные поля `Component`.
2. Контейнер: выравнивание по базовой линии текста, правила Figma для контейнера в `figma-mapping.md`.
3. Изменения `schema-format.md`: набор поддерживаемых событий и контекстов `appearance` у компонента, значения по
   умолчанию, состояния взаимодействия для Figma.
4. Закреплённые дети в потоковых контейнерах (вне контейнера наложения).
5. Оформление: пресеты, градиент и изображение фона, набор контекстов `appearance`, допустимых для компонента.
6. Контекст `expandable` (`collapsed`, `expanded`, в каждом — `control`) — вместе с первым аккордеоном, если его корень
   выглядит по-разному в этих положениях.
7. Состояния загрузки контента (изображение, файл): загрузка, ошибка, готово. Относятся к содержимому компонента, а не к
   `appearance`.
