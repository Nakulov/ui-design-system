# Модель компонентов

**Статус:** Черновик, в обсуждении  
**Версия спецификации:** 0.12

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

| Поле         | Статус                  | Контракт                            |
|--------------|-------------------------|-------------------------------------|
| `id`         | Согласовано (раздел 5)  | свойство `Component`                |
| `layout`     | Согласовано (раздел 4)  | `core/layout/Layout`                |
| `padding`    | Согласовано (раздел 6)  | `core/padding/Padding`              |
| `appearance` | Согласовано (раздел 8)  | `core/appearance/Appearance`        |
| `indicators` | Согласовано (раздел 12) | `core/component/ComponentIndicator` |
| `events`     | Согласовано (раздел 7)  | `core/event/Event`                  |

Базовая модель: `core/component/Component`.

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
      relativeTo: edge | content = edge
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
   валидатор деревьев и генератор в отладочной сборке. Каждый `Pressable` учитывается в дереве одним состоянием:
   повтор `id` в разных деревьях состояний одного `Pressable` допустим и означает один элемент (раздел 14).
2. В Figma `id` хранится в `sharedPluginData` слоя (пространство имён `uiContracts`, ключ `id`; `figma-mapping.md`,
   раздел 10.2). Дизайнер видит его в имени слоя `<Компонент> · <id>`; плагин находит слой по метке, а не по имени,
   при повторной отрисовке модели.
3. В списке (`ScrollList`, раздел 16) `id` компонента элемента — ключ элемента; без `id` ключ — позиция. Если в модели
   с сервера `id` повторяется, повторяющийся элемент получает ключ по позиции, платформа не падает. Образцы с повтором
   `id` валидатор по-прежнему отклоняет (правило 1).

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
  change(payload)                  — TextField (раздел 15)
  submit(payload)
  focus(payload)
  unfocus(payload)
  reachEnd(payload)                — ScrollList (раздел 16)
payload: Payload                   — тип задаёт приложение
```

| Вариант     | Смысл                            | Compose                                                         | SwiftUI                         |
|-------------|----------------------------------|-----------------------------------------------------------------|---------------------------------|
| `click`     | нажатие                          | `Modifier.clickable` / `onClick`                                | `.onTapGesture` / `Button`      |
| `longPress` | долгое нажатие                   | `Modifier.combinedClickable(onLongClick)`                       | `.onLongPressGesture`           |
| `change`    | изменение текста поля ввода      | `TextFieldState`, `snapshotFlow { text }`                       | `.onChange(of: text)`           |
| `submit`    | Return в однострочном поле ввода | `KeyboardActions` / `onKeyboardAction`                          | `.onSubmit`                     |
| `focus`     | поле ввода получило фокус        | `Modifier.onFocusChanged`                                       | `@FocusState`, `.onChange`      |
| `unfocus`   | поле ввода потеряло фокус        | `Modifier.onFocusChanged`                                       | `@FocusState`, `.onChange`      |
| `reachEnd`  | долистали до конца списка        | `LazyListState`, `snapshotFlow` по последнему видимому элементу | `.onAppear` последнего элемента |

1. `payload` описывает, что нужно сделать. Его тип — `Payload` (`schema-format.md`, раздел 4.1): на Android
   `interface Payload`, на iOS `protocol Payload`; приложение реализует его своими моделями. Дизайн-система не знает
   действий приложения и передаёт `payload` обработчику без изменений. Модели с сервера приложение получает
   полиморфной десериализацией своих реализаций `Payload`.
2. Поле — массив с уникальными вариантами (`schema-format.md`, раздел 5.1), поэтому одно событие нельзя задать дважды.
   Новые триггеры добавляются вариантами `Event` по мере необходимости.
3. Компонент обрабатывает события, которые поддерживает. В формате набор поддерживаемых событий пока не выражается
   (раздел 17, пункт 2), и валидатор деревьев неподдерживаемые события пока не проверяет: `change`, `submit`, `focus`
   и `unfocus` поддерживает только `TextField` (раздел 15), `reachEnd` — только `ScrollList` (раздел 16); у других
   компонентов они не отклоняются.
4. Обработчик на платформе получает `payload` без изменений и, у событий поля ввода, текущее значение поля
   (`TextFieldValue`). Значения в контракте события нет: его нет в модели поля, и `core` не ссылается на
   `components`.
5. В Figma события не реализуются (`willNotBeReleased` у свойства `events` и контракта `Event`): `payload` — тип
   приложения, дизайнер его не задаёт, а на отрисовку события не влияют.

## 8. `appearance`

Оформление корня компонента. Тип `Appearance` — `oneOf` по контексту компонента: контекст задаёт набор состояний, поэтому
у кнопки нет состояния «выбран», а у текста — «нажат».

```
appearance:
  plain:      { background?, shadow?, cornerRadius?, stroke?, overlay?, clip = false }
  control:    { default: plain, pressed: plain, disabled: plain }          — Pressable (раздел 14)
  selectable: { unselected: control, selected: control }                   — чип, чекбокс, таб
  focusable:  { default: plain, focused: plain, disabled: plain }          — TextField (раздел 15)
background, overlay: AppearanceFill = solid(ColorRole)
stroke:     { width: BorderWidth, color: ColorRole }                       — только внутренняя
```

| Поле `plain`   | Тип                         | Figma         | Compose              | SwiftUI         |
|----------------|-----------------------------|---------------|----------------------|-----------------|
| `background`   | `solid(ColorRole)`          | Fill          | `background`         | `.background`   |
| `shadow`       | `Elevation`                 | Effect style  | `shadow`             | `.shadow`       |
| `cornerRadius` | `CornerRadius`              | Corner radius | `RoundedCornerShape` | `.clipShape`    |
| `stroke`       | `BorderWidth` + `ColorRole` | Stroke inside | `border`             | `.strokeBorder` |
| `overlay`      | `solid(ColorRole)`          | верхний слой  | `drawWithContent`    | `.overlay`      |
| `clip`         | `boolean`                   | Clip content  | `clip`               | `.clipped()`    |

1. Поля `plain`, кроме `clip`, необязательны: отсутствие означает прозрачный фон, отсутствие тени, обводки и наложения,
   прямые углы. `clip` по умолчанию `false` — содержимое не обрезается.
2. Сочетания состояний выражаются вложением контекстов: нажатый выбранный чип оформлен `selected.pressed`.
3. `Appearance` описывает только вид состояний. Состояние определяют поля компонента (доступность, выбор) и
   взаимодействие пользователя (нажатие, фокус).
4. Фон и наложение — один тип `AppearanceFill` (`oneOf`), чтобы градиент (`gradient`) добавился вариантом без
   несовместимого изменения сразу в обе заливки.
5. Обводка только внутренняя: не меняет размер компонента; в Compose `border` рисует внутри, в Figma — Stroke inside.
6. Контракт компонента объявляет допустимые контексты полем `appearanceContext` (`schema-format.md`, раздел 5.2):
   свойство `appearance` компонента принимает только эти варианты. Кликабельность от контекста не зависит: её задаёт
   `events`; контекст определяет, меняется ли вид корня по состояниям. У компонента с `selectable` есть обязательное
   свойство `selected: boolean`.
   Контекст `control` принадлежит `Pressable`: состояние `disabled` выбирает его поле `enabled` (раздел 14).
   Контекст `focusable` — у `TextField` (раздел 15), так же с полем `enabled`.
   Остальные компоненты с действием, но без вида состояния — с `plain` и `events`; вид нажатия задаёт `Pressable`
   над ними.
7. В Figma `plain` соответствует оформлению корня instance (fill, effect style, corner radius, stroke, clip content),
   контекст — variant-свойствам `state` и `selected` в Component Set (`figma-mapping.md`, разделы 3.5 и 10.2).
8. Каждое состояние задаёт оформление полностью; повтор общих полей предполагается решить стилями оформления.
9. Наложение (`overlay`) — заливка поверх содержимого: слои корня снизу вверх — тень, фон, содержимое, наложение,
   обводка. Повторяет форму корня и обрезается по ней независимо от `clip`, нажатия не перехватывает. Нужно, когда
   содержимое закрывает фон: отклик на нажатие картинки или баннера (`overlayPressed` в `pressed`).
10. Картинки в `appearance` нет: адрес — данные, а не оформление, в состояниях он повторялся бы, а смена при нажатии
    перезагружала бы картинку. Картинка на фоне — `Container` с раскладкой `overlay` и `Image` первым ребёнком.

## 9. Контейнер

`Container` (`components/container/Container`) — первый компонент: располагает детей в строку, в колонку (в том
числе с переносом) или наложением. Наследует `Component` (`extends`, `schema-format.md`, раздел 5.2).

```
arrangement: oneOf
  horizontal     { alignment: top | center | bottom | baseline, distribution }
  vertical       { alignment: start | center | end,             distribution }
  horizontalWrap { alignment: top | center | bottom | baseline, distribution, lineDistribution }
  verticalWrap   { alignment: start | center | end,             distribution, lineDistribution }
  overlay        { horizontal: start | center | end,            vertical: top | center | bottom }
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
7. В Figma контейнер — фрейм, а не Figma-компонент, и создаётся только при отрисовке модели (`figma-mapping.md`,
   разделы 3.4 и 10.3). Контекст оформления — `plain`; кликабельная карточка — `Pressable` над контейнером.
8. Перенос — отдельные варианты `horizontalWrap` и `verticalWrap`, а не флаг: у переноса есть промежуток между
   строками, а оси задают типы выравнивания. Один `alignment` выравнивает и детей в строке, и группу строк: в Figma
   оба задаются `counterAxisAlignItems`. `maxLines` и `maxItemsInEachRow` не используются: в Figma их нет.
9. `baseline` — выравнивание детей строки по первой базовой линии текста (`ContainerRowAlignment`), только у
   `horizontal` и `horizontalWrap`: в Figma `BASELINE` только у горизонтального Auto Layout, у `Column` в Compose его
   нет. Последней базовой линии нет: в Figma её нет. Ребёнок без текста выравнивается нижним краем, вложенный
   контейнер — по базовой линии своего первого текста. При переносе строки прижаты к верху.

## 10. `Text`

`Text` (`components/text/Text`) — простой текст или текст из фрагментов. Контекст оформления — `plain`; вид нажатия
кликабельного текста задаёт `Pressable` над ним.

```
content: oneOf
  plain: string
  rich:  { spans: [TextSpan] }
typography: TypographyRole                 — для всего текста
color:      oneOf solid(ColorRole)
decoration?: underline | strikethrough
alignment:  start | center | end
maxLines:   count(integer) | unlimited = unlimited
truncation: start | middle | end = end      — место многоточия
TextSpan: oneOf
  text: { value, typography?, color?, decoration?, click? }   — незаданное берётся у Text; color — ColorRole
  icon: { name: Icon, typography?, color? }                   — символ; незаданное берётся у Text
```

1. Стиль текста — собственные поля компонента, без группы: `appearance` оформляет корень, а не текст.
2. Содержимое — `oneOf`, чтобы форматированный текст и будущие виды содержимого добавлялись совместимо. Простой текст
   записывается `{ "plain": "…" }` (вариант с примитивным типом, `schema-format.md`, раздел 7).
3. Фрагменты — `oneOf`: `text` и `icon`. Символ — часть текста: стоит между словами, переносится вместе с ними и
   масштабируется со шрифтом (sp, Dynamic Type); размер равен кеглю роли типографики, выравнивание — по центру
   строчных букв, параметров размера и выравнивания нет. При обрезке символ — неделимый знак. Нажатия у символа нет:
   SwiftUI не вешает ссылку на картинку внутри `Text`. Это исключение из принципа 2: в Figma текст с символами по
   краям — компонент `text/icon` из символов и текста (`figma-mapping.md`, раздел 10.5).
4. Линия одна (`underline` или `strikethrough`): в Figma они взаимоисключающие. Регистр задаётся ролью типографики.
5. У фрагмента только `click` (ссылка): долгое нажатие на часть текста платформы не поддерживают.
6. Markdown — формат записи, а не модель: сервер или приложение переводят inline-markdown во фрагменты. Блочный
   markdown — отдельный компонент из контейнеров и текстов.
7. Многоточие в начале и середине — только при одной строке (`maxLines.count = 1`), при большем значении оно в конце.
   В Figma только многоточие в конце: `start` и `middle` — `figma: willNotBeReleased`.
8. В Figma `Text` — Figma-компонент. `content` — text-свойство, фрагменты при отрисовке модели — стили диапазонов;
   `typography`, `color`, `decoration`, `alignment` и `maxLines` — переопределения слоя (`figma.json`), а не
   variant-свойства.
9. Заливка текста — `oneOf`, как фон: градиент (`gradient`) добавится вариантом без несовместимого изменения. Только
   для всего текста: у фрагмента цвет — `ColorRole`, градиент на часть текста не поддерживает `AttributedString`.
10. Цвет текста по состояниям задаёт `Pressable`: в его дереве состояния у `Text` другой цвет (раздел 14).
11. Выравнивания по ширине и по вертикали нет: `justify` не поддерживает SwiftUI, по вертикали текст прижат к верху,
    другое положение задаёт `Container`.
12. Фуригана (`ruby`) и язык фрагмента (`language`) — будущие расширения `TextSpan` (README `Text`).

## 11. `Image`

`Image` (`components/image/Image`) — символ из словаря темы или картинка, загружаемая по адресу. Контекст
оформления — `plain`; нажимаемое изображение — `Pressable` над ним.

```
source: oneOf
  resource: oneOf                          — без загрузки, показывается сразу
    icon: { name: Icon, color: ColorRole }
  remote: { origin: oneOf url(string),     — загружается
            scale: fit | crop, alignment: topStart … bottomEnd,
            placeholder?: ImageResource, error?: ImageResource }
```

1. Один компонент для символов и картинок: вызывающий не выбирает между иконкой и изображением. Символ — значение
   foundation `Icon`, на него ссылаются и другие компоненты; `Image` — компонент, который его показывает.
2. Источник делится по происхождению: `resource` не бывает в состоянии загрузки, `remote` бывает всегда. Имена — по
   происхождению, а не по реализации (`sync`/`async`).
3. Параметры одного вида источника лежат внутри него: цвет — у символа, вписывание и заглушки — у загружаемой
   картинки. Бессмысленных сочетаний (окрашенная фотография, заглушка у символа) нет.
4. Заглушка и ошибка — `ImageResource`: заглушка не может загружаться сама.
5. Размера у символа нет: собственный размер вектора при `hug`, вписывание в `fixed` и `fill`. Заглушка и ошибка — в
   собственном размере по центру.
6. Нет полей `tint`, `alpha`, индикаторов и картинки-фона: цвет — внутри символа, прозрачности нет и в базовой модели,
   индикаторы с вырезом — отдельная задача, картинка на фоне — композиция `Container` (раздел 8, правило 10). Отклик
   на нажатие поверх картинки — наложение `overlay`. Подпись для диктора — с доступностью всех компонентов.
7. В Figma `Image` — Figma-компонент со слоем символа и слоем картинки (`figma-mapping.md`, раздел 4.1, правило 12).

## 12. `indicators`

Индикаторы — маленькие компоненты у углов формы носителя (точка «в сети», счётчик, галочка), отделённые от него
вырезом. Необязательное поле базовой модели: индикатор может быть у любого компонента.

```
indicators?: [ComponentIndicator]
ComponentIndicator: { anchor: topStart | topEnd | bottomStart | bottomEnd,
                      content: Component,
                      gap?: Spacing }                — зазор выреза; без него выреза нет
```

1. Индикатор принадлежит носителю: носитель вырезает форму индикатора, расширенную на `gap`, из всей своей отрисовки —
   фона, содержимого, наложения и обводки — независимо от `clip`. Сквозь зазор видно то, что под носителем; обводка
   цветом фона так не умеет.
2. Центр индикатора — в точке угла формы носителя: у прямого угла — вершина, у скруглённого — середина дуги. Смещения
   и якорей на серединах сторон нет; якоря добавляются значениями перечисления совместимо.
3. Раскладка индикатор не учитывает: размер носителя прежний, индикатор рисуется поверх и может выступать за его
   границы. Поэтому индикатор — не ребёнок `Container(overlay)`: контейнер равен носителю, смещения `pinned`
   неотрицательны, а группа выреза обрезала бы выступ.
4. Содержимое — любой компонент с размером `hug` или `fixed`; `fill` не имеет смысла (проверка — #37). Один индикатор
   на угол. Нажатие — собственные `events` содержимого.
5. Поле в базовой модели, а не в каждом носителе: иначе поля повторялись бы в `Image`, кнопке, вкладке. Компонент
   `Badge` — один из возможных индикаторов (раздел 13).
6. В Figma индикаторы строятся только при отрисовке модели: фрейм размером с носитель без обрезки, в нём носитель с
   маской «форма минус вырезы» и индикаторы над ним (`figma-mapping.md`, раздел 10.2, правило 4).

## 13. `Badge`

`Badge` (`components/badge/Badge`) — метка внимания: форма с текстом, символом или картинкой либо без
содержимого. Контекст оформления — только `plain`.

```
content?: oneOf                            — нет: только фон (точка)
  text:  { value: string, typography: TypographyRole, color: ColorRole }
  image: ImageSource                       — как у Image
```

1. Текст — любая строка: сокращение («99+») и форматирование чисел зависят от продукта и выполняются сервером или
   приложением. Числового значения с правилом сокращения в контракте нет.
2. Картинка — `ImageSource` компонента `Image`: символ, загрузка, заглушка и ошибка не дублируются.
3. Ширина не меньше высоты: один знак — круг, длинный текст — таблетка той же высоты. Текст в одну строку, без переноса
   и многоточия. Содержимое по центру.
4. Вид задают поля базовой модели; типовая геометрия — рекомендации README, в Figma её закрепляют пресеты
   (`figma-mapping.md`, раздел 11). Вид из темы (смысловое поле с парами ролей) отложен до стилей оформления.
5. Цвет текста — `ColorRole`, как у фрагмента текста: градиент в бейдже не нужен.
6. В Figma `Badge` — Figma-компонент с variant `content` (`figma-mapping.md`, раздел 4.1, правило 13).

## 14. `Pressable`

`Pressable` (`components/pressable/Pressable`) — первый компонент с действием: обёртка над любым нажимаемым элементом
(кнопка, кликабельная карточка, строка), а не кнопка. Кнопка — пресет над ним. Контекст оформления — только
`control`.

```
Pressable : Component
  id?, layout?, padding?, indicators?          — из Component, общие для всех состояний
  events?                                      — из Component; действие Pressable, только при enabled
  appearance?: Appearance.Control              — оформление рамки по состоянию
  enabled:     boolean = true                  — доступность
  states:      PressableStates<Component>      — содержимое по состояниям

PressableStates<T : Component>                 — components/pressable/definitions/PressableStates
  default:   T
  pressed:   T = default                       — defaultsTo default
  disabled:  T = default                       — defaultsTo default
```

Ниже «дерево состояния» — значение поля `states.default`, `states.pressed` или `states.disabled`.

**Состояния и вид**

1. Активное состояние: `enabled = false` → `disabled`; иначе `pressed`, пока палец прижат, и `default`. `enabled`
   по умолчанию `true` — «доступен» (поле свойства `default`, `schema-format.md`, раздел 5).
2. `pressed` и `disabled` не nullable: по умолчанию — дерево `default` целиком, с его событиями
   (`val pressed: T = default` в Kotlin, `pressed ?? default` в `init` Swift; в JSON поле можно не передавать).
3. Деревья — один и тот же компонент, значение по умолчанию — `default`: поле свойства `defaultsTo`
   (`schema-format.md`, раздел 5). Генераторы выпускают `PressableStates` обобщённым по `T`, поле `states` — с
   `Component` (`schema-format.md`, раздел 5.2, правило 7). Обобщение ловит смешение типов, только когда тип известен
   компилятору: Kotlin выводит `T = Component` и компилирует `PressableStates(default = Container(), disabled = Text())`.
   Основная гарантия — валидатор деревьев: для JSON с сервера и в отладочной сборке для моделей из кода.
4. Деревья могут различаться содержимым, а не только оформлением: например, подпись «Оплатить» в `default` и
   «Недостаточно средств» в `disabled`.

**Оформление**

5. `Pressable.appearance` — контекст `control`: оформление рамки по состоянию. Слои снизу вверх (раздел 8,
   правило 9): тень, фон, дерево активного состояния, наложение, обводка. Отклик на нажатие — наложение рамки в
   `pressed`, без участия деревьев. Дерево меньше рамки (`hug`) стоит по центру по обеим осям; другое положение
   задаёт само дерево: растяжение `fill` и выравнивание внутри него (например, `Container` с `alignment = start`).
6. Корни деревьев — только `plain` (проверяет валидатор деревьев). Кликабельный элемент внутри дерева — вложенный
   `Pressable`. `Pressable` в корне дерева допустим: он занимает всю площадь и по правилу 11 перехватывает каждое
   нажатие, внешний в `pressed` не переходит. Это ошибка проектирования макета, а не формата; валидатор её не
   запрещает.

**События**

7. У `Pressable` свои `events` (из `Component`) — действие нажимаемого элемента. `Pressable` обрабатывает их по всей
   своей площади, включая свой `padding`, и показывает `pressed` только при `enabled`. При `enabled = false`
   `Pressable` нажатие не обрабатывает.
8. Деревья состояний — обычные компоненты: их события — собственные, состояние `Pressable` на них не влияет. Своё
   поведение в выключенном состоянии (подсказка) — `click` корня или узла дерева `disabled`.
9. Дерево `default` подставляется в `pressed` и `disabled` целиком, с событиями (правило 2). Поэтому действие, которое
   должно отключаться, задаётся на `Pressable`, а не на корне `default`; это рекомендация README, а не проверка
   валидатора.
10. Запретов валидатора на события нет: ни у `Pressable`, ни в деревьях.
11. Срабатывает самый глубокий кликабельный элемент — вложенный `Pressable` или узел дерева со своими `events` (так
    поглощают нажатие Compose и SwiftUI); `Pressable` в `pressed` не переходит. Выключенный `Pressable` нажатие не
    перехватывает: оно уходит к внешнему, а кликабельные узлы его дерева работают. Пример: в нажимаемой карточке
    товара выключенная кнопка «В корзину» без своих событий в дереве — нажатие на неё открывает товар; с деревом
    `disabled` и `click` подсказки — срабатывает подсказка.

**Реализация на платформах**

| Платформа | Реализация                                                                                                            |
|-----------|-----------------------------------------------------------------------------------------------------------------------|
| Compose   | `Modifier.clickable(enabled, interactionSource, indication = null)` на рамке; `pressed` — `collectIsPressedAsState()` |
| SwiftUI   | `Button` с `ButtonStyle`, `configuration.isPressed` выбирает дерево; выключенный `Pressable` — дерево без `Button`    |

- Compose: `clickable(enabled = false)` не отключает кликабельных детей и не забирает нажатие у внешнего (правило 11).
- SwiftUI: выключенный `Pressable` рисует дерево без обёртки `Button`, чтобы не перехватывать нажатие (правило 11).
  `.disabled(true)` не подходит: он отключает всё поддерево через окружение, и подсказка в дереве `disabled` перестанет
  работать; недоступность — проверкой `enabled` в действии и признаком доступности. Внутри `List` вложенным кнопкам
  нужен `.buttonStyle(.borderless)` или `.plain`, иначе нажатие получает вся строка.
- Если деревья состояний разного размера, рамка меняет размер при нажатии; это решение макета.

**Соответствие узлов по `id`**

12. Узлы с одинаковым `id` в деревьях одного `Pressable` — один элемент (раздел 5, правило 1).
13. Узел с `id` сопоставляется с узлом того же `id`; узел без `id` — по пути от корня дерева (номера детей), как при
    повторной отрисовке (`figma-mapping.md`, раздел 10.1, правило 4). Соответствие нужно Figma (правило 14) и
    платформам — для анимации перехода между состояниями.

**Figma**

14. Модель с `Pressable` плагин рисует фреймом, как `Container` (`figma-mapping.md`, разделы 3.4, 10.6): рамка с
    оформлением `control` для активного состояния и дерево этого состояния. Статичная модель рисует `disabled` (дерево
    или подставленный `default`) при `enabled = false`, иначе `default`; `pressed` в модели не рисуется. Своего
    Figma-компонента и `figma.json` нет. Узлы с одинаковым `id` получают одинаковое имя слоя `<Компонент> · <id>`,
    поэтому в Component Set пресета override общей подписи переносится между variants `state`, а узлы состояния со
    своим `id` сохраняют своё содержимое.

## 15. `TextField`

`TextField` (`components/textField/TextField`) — первый компонент, данные которого меняет пользователь: примитив,
только область ввода и редактирования текста. Подпись над полем, вспомогательный текст и текст ошибки под ним —
композиция вокруг; поле формы, поиск и пароль — пресеты. Контекст оформления — только `focusable`.

```
TextField : Component
  id?, layout?, padding?, indicators?      — из Component
  appearance?:  Appearance.Focusable       — только рамка
  events?:      Set<Event>                 — + change, submit, focus, unfocus

  value:        TextFieldValue = plain("")
  typography:   TypographyRole
  color:        TextFieldColor
  alignment:    TextAlignment               — start | center | end, как у Text
  cursorColor:  ColorRole
  placeholder?: Placeholder
  enabled:      boolean = true
  secure:       boolean = false
  autofocus:    boolean = false
  lines:        TextFieldLines = single
  keyboard:     Keyboard = Keyboard()
  clearButton?: ClearButton
  leading?:     TextFieldEdge
  trailing?:    TextFieldEdge

TextFieldValue:   oneOf plain: string
TextFieldColor:   { default, focused = default, highlighted = default, disabled }   — TextColor
PlaceholderColor: { default, focused = default, disabled }                          — TextColor
Placeholder:      { content: TextContent, typography: TypographyRole, color: PlaceholderColor }
TextFieldLines:   oneOf single | multi { min: integer, max: MaxLines = unlimited }
ClearButton:      { visibility: ClearButtonVisibility, gap: Spacing, button: Pressable }
ClearButtonVisibility: always | whileEditing | whileNotEmpty
TextFieldEdge:    { content: TextFieldStates, gap: Spacing, verticalAlignment: EdgeAlignment }
EdgeAlignment:    top | center | bottom
TextFieldStates<T : Component>: { default: T, focused: T = default, disabled: Component }
Keyboard:         { type: KeyboardType = text, returnKey: ReturnKey = default, contentType: ContentType = none,
                    capitalization: Capitalization = auto, autocorrect: Autocorrect = auto }
```

Ниже «подсказка поля» — `placeholder`, «край поля» — `leading` или `trailing`, «дерево края» — значение поля
`content.default`, `content.focused` или `content.disabled` края.

**Состояния и вид**

1. Состояние компонента — то, что меняется от действий пользователя или выбирается полем поведения компонента
   (`enabled`); то, что меняет только вид (ошибка, успех), — новая модель от приложения.
2. Активное состояние: `enabled = false` → `disabled`; иначе фокус → `focused`; иначе `default`. Одно состояние
   выбирает рамку (`appearance.focusable`), цвета текста и подсказки поля, деревья краёв.
3. `highlighted` — цвет выделенной части введённого текста (выделение бывает только в фокусе); по умолчанию —
   `default`, а не `focused`: цепочки `defaultsTo` нет (`schema-format.md`, раздел 5, правило 4), а `focused` обычно
   равен `default`. Подсветка выделения и ручки — `cursorColor` (подсветка с прозрачностью; iOS `tintColor`, Compose
   `TextSelectionColors`). Курсор виден только в фокусе, состояний у `cursorColor` нет.
4. Типографика одна на поле; у подсказки поля своя (подсказка — текст интерфейса, ввод — любая письменность). Разная
   высота строки — скачок высоты с первым символом, решение макета.
5. `disabled` у цветов и в `TextFieldStates` обязателен: выключенное поле с ярким текстом или нажимаемым глазком
   выглядит и ведёт себя как активное. В `disabled` цвета фрагментов подсказки поля не действуют.
6. `TextFieldStates.disabled` — любой `Component` (глазок в `disabled` — `Image`), в отличие от `PressableStates`, где
   все деревья одного `T`: у `Pressable` `enabled = false` сам отключает его действие, у краёв поля — нет. Узлы с
   одинаковым `id` в деревьях края — один элемент, как у `Pressable` (раздел 14, правила 12–13); узлы разных типов —
   замена.

**Текст**

7. Текстом владеет поле (Compose `TextFieldState`, SwiftUI `@State` в обёртке). Поле показывает `value` и сбрасывает
   ввод пользователя, когда `value` меняется (`remember(value)`). В `value` передаются данные, а не введённый текст.
8. Приложение узнаёт текст событиями: `change` (каждое изменение), `submit` (Return в однострочном поле, в том числе
   `next`), `focus`, `unfocus` (раздел 7). Обработчик получает `payload` и текущее значение поля (раздел 7,
   правило 4). Команд из кода приложения (`valueOf`, `setValue`, `focus(id)`) у поля нет.
9. Подсказка поля видна, пока весь текущий текст пуст: составляемый текст клавиатуры (IME), пробелы и точки скрытого
   ввода — текст. Видна и в фокусе. Выравнивание — как у поля.
10. `value` — только `plain`; форматированный ввод — вариант `rich` позже; форматирование ввода (маски) — отдельно.

**Строки, края, кнопка очистки**

11. `single` — одна строка, длинный текст прокручивается вбок, Return — действие. `multi` — перенос, рост от `min` до
    `max` строк, дальше вертикальная прокрутка; текст прижат к верху.
12. Порядок слева направо: `leading` → текст → кнопка очистки → `trailing`. Текст не заезжает под края и кнопку
    (граница — с отступом `gap`); края не прокручиваются с текстом; край получает свой размер, текст — оставшуюся
    ширину. `verticalAlignment` — положение края в выросшем поле.
13. Кнопка очистки — часть поля: видимость по `visibility` (`whileEditing` — в фокусе); `enabled` кнопки выставляет
    поле (`disabled`, если поле выключено или пусто); нажатие стирает текст, оставляет фокус, даёт `change`. Скрытая
    кнопка не занимает места. `button` — `Pressable`, записанный без обёртки (`schema-format.md`, раздел 4.5).

**Клавиатура и фокус**

14. `autofocus` — фокус при первом появлении поля; новая модель фокус не трогает.
15. `returnKey = next` переводит фокус к следующему доступному `TextField` в порядке дерева, иначе скрывает
    клавиатуру.
16. Значения клавиатуры — пересечение платформ; платформа переводит в своё. Android: `Password` и `NumberPassword` —
    по `contentType` `password` и `newPassword` (с `type` `text` и `number`), а не по `secure` (после глазка
    клавиатура не должна запоминать пароль). iOS: у `numberPad`, `decimalPad`, `phonePad` нет Return. Значение,
    недоступное на версии iOS (карта, дата рождения — iOS 17+), не передаётся; минимальная версия — выбор платформы.
17. `auto` у `capitalization` и `autocorrect` выводит платформа (вычисляемых значений по умолчанию нет,
    `schema-format.md`, раздел 5, правило 6):

    - `type` `number`, `decimal`, `phone`, `email`, `url` — `none`, исправление выключено;
    - `contentType` `username`, `password`, `newPassword`, `oneTimeCode`, `email`, `phone`, `postalCode`, данные карты
      (`card…`) и даты рождения (`birth…`) — `none`, выключено;
    - `contentType` имени и адреса (`fullName`, `givenName`, `middleName`, `familyName`, `namePrefix`, `nameSuffix`,
      `street`, `streetDetails`, `city`, `region`, `country`) — `words`, выключено;
    - остальное — `sentences`, включено.

    Условия проверяются по порядку; явное значение `capitalization` или `autocorrect` действует вместо `auto`.
18. `secure` — системная маска, символ платформенный; кнопка «показать пароль» — `Pressable` в `trailing`, приложение
    присылает модель с `secure = false`.

**Валидатор деревьев**

19. События `change`, `submit`, `focus`, `unfocus` у других компонентов не проверяются (раздел 7, правило 3).
20. Проверяются: одно `autofocus = true` в дереве; `min ≥ 1`, `count ≥ 1`, `min ≤ count`; при `multi` —
    `secure = false` и `returnKey = default`; нет `click` во фрагментах подсказки поля; у `clearButton.button` нет
    `enabled` (его выставляет поле) и `events`. Повтор `id` в деревьях одного края допустим (правило 6).

**Реализация на платформах**

| Платформа | Реализация                                                                                                                                      |
|-----------|-------------------------------------------------------------------------------------------------------------------------------------------------|
| Compose   | `BasicTextField(TextFieldState)`, `KeyboardOptions`, `ContentType` в семантике, `Modifier.onFocusChanged`, `ImeAction.Next` → `moveFocus`       |
| SwiftUI   | обычное поле — обёртка над `UITextView` (цвет выделенного текста — TextKit 2 `setRenderingAttributes`, рост и прокрутка строк); скрытое — `UITextField` или `SecureField` |

- SwiftUI `TextField` доступа к цвету выделенного текста не даёт, `UITextView` не умеет скрывать ввод — отсюда два
  представления. Многострочное SwiftUI-поле — `TextField(axis: .vertical)`, `.lineLimit(min...max)`, фокус —
  `@FocusState`.
- Открытые вопросы реализации: окраска выделенного текста в Compose (`SpanStyle` по `selection`) и учёт шрифта и
  цветов `prompt` в SwiftUI — README `TextField`.

**Figma**

21. `TextField` — Figma-компонент (Component Set) `state` × `filled` (`figma-mapping.md`, разделы 3.5, 6.1): `filled`
    — пустое или непустое `value`; края — только при отрисовке модели, фреймом (`figma-mapping.md`, раздел 10.7).
    Однострочное поле — текст по содержимому без многоточия, курсор сразу после текста; длинный текст обрезается
    краем текстовой области, как прокрутка на платформах.

## 16. `ScrollList`

`ScrollList` (`components/scrollList/ScrollList`) — прокручиваемый список: окно, в котором содержимое прокручивается
вдоль одной оси. Одна модель и для прокрутки содержимого, заданного в макете, и для списка из данных: модель — дерево,
данные в дерево разворачивает приложение или сервер (привязка к данным и загрузка данных — не в модели, `ROADMAP.md`).
Ленивую отрисовку выбирает платформа. Вариант `content` целиком описывает список — раскладку и детей; бессмысленные
сочетания исключает `oneOf`. Контекст оформления — `plain`.

```
ScrollList : Component
  id?, layout?, padding?, appearance?, indicators?   — из Component
  events?:        Set<Event>                         — + reachEnd

  contentPadding: Padding = all(zero)                — отступ у содержимого
  content:        ScrollListContent

ScrollListContent: oneOf
  vertical        ScrollListVertical
  horizontal      ScrollListHorizontal
  verticalGrid    ScrollListVerticalGrid
  horizontalGrid  ScrollListHorizontalGrid
  verticalPager   ScrollListPager
  horizontalPager ScrollListPager

ScrollListVertical
  alignment:  ContainerHorizontalAlignment = start   — поперёк оси
  gap:        Spacing = zero
  position:   ContainerVerticalAlignment = top       — вдоль оси, пока содержимое короче окна
  reversed:   boolean = false
  snapToItem: boolean = false
  children?:  [ScrollListElement]

ScrollListHorizontal
  alignment:  ContainerVerticalAlignment = top
  gap:        Spacing = zero
  position:   ContainerHorizontalAlignment = start
  reversed:   boolean = false
  snapToItem: boolean = false
  children?:  [ScrollListElement]

ScrollListElement: oneOf
  header  ScrollListHeader { component: Component }
  item    ScrollListItem   { component: Component, viewportFraction?: float }

ScrollListVerticalGrid
  columns:   ScrollListGridTracks                    — обязательно
  rowGap:    Spacing = zero
  columnGap: Spacing = zero
  children?: [ScrollListGridElement]

ScrollListHorizontalGrid
  rows:       ScrollListGridTracks                   — обязательно
  rowGap:     Spacing = zero
  columnGap:  Spacing = zero
  snapToItem: boolean = false
  children?:  [ScrollListGridElement]

ScrollListGridTracks: oneOf
  fixed     integer                                  — число колонок (строк), ≥ 1
  adaptive  Size                                     — минимальная ширина колонки (высота строки)

ScrollListGridElement: oneOf
  header  ScrollListHeader                           — ячейка на всю строку (весь столбец)
  item    ScrollListGridItem { component: Component, span: ScrollListGridSpan = count(1) }

ScrollListGridSpan: oneOf
  count  integer                                     — ≥ 1
  full                                               — inline-перечисление ScrollListGridSpanMode

ScrollListPager
  gap:         Spacing = zero
  initialPage: integer = 0                           — ≥ 0
  children?:   [Component]                           — страница = окно минус contentPadding
```

| Вариант           | Figma                                   | Compose              | SwiftUI                                               |
|-------------------|-----------------------------------------|----------------------|-------------------------------------------------------|
| `vertical`        | содержимое — вертикальный Auto Layout   | `LazyColumn`         | `ScrollView { LazyVStack }`                           |
| `horizontal`      | содержимое — горизонтальный Auto Layout | `LazyRow`            | `ScrollView(.horizontal) { LazyHStack }`              |
| `verticalGrid`    | содержимое — Auto Layout `GRID`         | `LazyVerticalGrid`   | `ScrollView { LazyVStack }` со строками из ячеек      |
| `horizontalGrid`  | содержимое — Auto Layout `GRID`         | `LazyHorizontalGrid` | `ScrollView(.horizontal) { LazyHStack }` со столбцами |
| `verticalPager`   | окно показывает страницу `initialPage`  | `VerticalPager`      | `ScrollView { LazyVStack }`, правило 9                |
| `horizontalPager` | окно показывает страницу `initialPage`  | `HorizontalPager`    | `ScrollView(.horizontal) { LazyHStack }`, правило 9   |

Имя — `ScrollList`, а не `List`: `List` совпадает с `kotlin.collections.List` (генераторы пишут `List<Component>`,
`schema-format.md`, раздел 5.2) и со SwiftUI `List`.

1. `padding` — по общим правилам (раздел 6): отступ окна, элементы обрезаются у его края. `contentPadding` — отступ у
   содержимого: в начале и в конце; при прокрутке элементы доезжают до края окна (Compose `contentPadding`, SwiftUI
   `.contentMargins`). Исключение — пейджер (правило 9). Ячейка элемента поперёк оси — на всю ширину (высоту)
   содержимого; вдоль оси — по компоненту или по `viewportFraction`.
2. Ключ элемента — `id` компонента (раздел 5, правило 3); без `id` — позиция; повтор `id` в модели с сервера —
   повторяющийся элемент получает ключ по позиции, платформа не падает. Задавать `id` элементам из данных — забота
   приложения.
3. `header` — обычный компонент; прилипание к краю окна обеспечивает платформа (Compose `stickyHeader`, SwiftUI
   `LazyVStack(pinnedViews: .sectionHeaders)`). В сетке ячейка `header` — всегда на всю строку (весь столбец). Размер
   компонента в ячейке — его `layout`.
4. `position` визуальный, `reversed` его смысл не меняет. `reversed`: первый ребёнок — у конца оси (внизу), прокрутка
   начинается оттуда; порядок в модели — от новых к старым, подгрузка истории — дописывание в конец `children` и
   `reachEnd` (в перевёрнутом списке конец — верх).
5. `reachEnd` (раздел 7) срабатывает относительно текущего конца содержимого: пришла порция — конец сдвинулся, старых
   меток нет.
6. `viewportFraction` — доля окна вдоль оси, окно — за вычетом `contentPadding` (пример: окно 360, `contentPadding` по
   16, `0.4` → (360 − 32) × 0.4 ≈ 131); `0 < viewportFraction ≤ 1`. Если у списка вдоль оси `hug`, окна нет —
   `viewportFraction` игнорируется, элемент меряется своим `layout`. Размер ячейки задаёт список, компонент в ней
   меряется своим `layout`; без поля элемент меряется своим `layout`.
7. `snapToItem` — после жеста список докатывается к ближайшему элементу; сильный свайп пролистывает несколько (Compose
   `rememberSnapFlingBehavior`, SwiftUI `.scrollTargetBehavior(.viewAligned)`). У `verticalGrid` поля нет — случаев
   нет.
8. Сетка: ширина колонки зависит от ширины окна (за вычетом `contentPadding`) и числа колонок, а не от числа
   элементов. Элементы заполняют строки с начала; элемент, которому не хватает места в строке, начинает новую.
   Неполная строка — элементы с начала, колонки сохраняют ширину, пустые ячейки остаются пустыми (2 плитки при 4
   колонках занимают половину строки). `span` — ширина элемента в колонках; больше числа колонок — прижимается к нему,
   элемент на всю строку, платформа не падает (Compose `GridItemSpan(minOf(n, maxLineSpan))`; при адаптивных колонках
   число зависит от ширины экрана). Адаптивные колонки (`adaptive`): число колонок считает платформа по ширине окна —
   столько, сколько помещается колонок не уже заданного размера (Compose `GridCells.Adaptive`). `horizontalGrid` — то
   же поперёк: элементы заполняют столбцы сверху вниз, `span` — в строках, `header` — весь столбец, число адаптивных
   строк — по высоте окна. SwiftUI: у `LazyVGrid` элементы одной ширины, поэтому платформа собирает элементы в строки
   по этому правилу — `HStack` в `LazyVStack`, ячейка `.containerRelativeFrame(.horizontal, count:span:spacing:)`
   (iOS 17+), число адаптивных колонок — по ширине окна; способ проверен компиляцией (`swiftc`).
9. Пейджер: страница — окно минус `contentPadding` вдоль оси и поперёк (Compose `PageSize.Fill`), компонент в ней
   меряется своим `layout`. При нулевом `contentPadding` страница — ровно окно (онбординг, лента видео); при
   ненулевом текущая страница стоит с отступом `contentPadding` от краёв окна, а не только в начале и в конце
   содержимого, — по бокам видны края соседних страниц (баннеры, галерея). Один жест — не больше одной страницы.
   `initialPage` (≥ 0) больше числа страниц прижимается к последней странице, платформа не падает. `hug` вдоль оси —
   окна нет, страница меряется своим `layout` (как `viewportFraction`, правило 6); `hug` поперёк оси — окно обнимает
   самую большую страницу. SwiftUI: `ScrollView`
   со стеком, страница — `.containerRelativeFrame([.horizontal, .vertical])`, не больше страницы за жест —
   `.scrollTargetBehavior(.viewAligned(limitBehavior: .alwaysByOne))` (iOS 18+; на iOS 17 — `.paging`), начальная
   страница — `.scrollPosition(id:)`; способ проверен компиляцией (`swiftc`). Событие смены страницы и индикатор
   страниц — #219.
10. Вложенная прокрутка в одном направлении — ошибка проектирования, валидатор её не проверяет; платформа не падает:
    внутренний список рисуется колонкой без своей прокрутки. Разные направления (карусель в ленте) — норма.

**Figma**

11. `ScrollList`, как `Container`, — фрейм без Figma-компонента и `figma.json`; создаётся только при отрисовке модели
    (`figma-mapping.md`, раздел 3.4): внешний фрейм — окно (`padding`, оформление, clip, высота — `layout.height`),
    внутренний — содержимое (`contentPadding`, `gap`, выравнивание или Auto Layout `GRID` у сеток), каждый элемент —
    фрейм ячейки (в сетке — с `gridColumnSpan` / `gridRowSpan`). Прокрутку в прототипе плагин не ставит. Отрисовка и
    метки — `figma-mapping.md`, раздел 10.8.

**Не проверено** (поведение во время работы, компиляцией не проверяется; проверяется на этапе 5):

- прилипающий `header` в перевёрнутом списке: Compose `stickyHeader` и `reverseLayout`, SwiftUI
  `LazyVStack(pinnedViews:)`;
- `reversed` в SwiftUI: прямого аналога `reverseLayout` нет;
- учёт `contentPadding` в доле окна: Compose `Modifier.fillParentMaxWidth(fraction)`, SwiftUI
  `.containerRelativeFrame`; так же — ширина ячейки сетки в SwiftUI;
- прилипающий `header` в сетке: Compose `stickyHeader` у `LazyGridScope`, SwiftUI `Section` в
  `LazyVStack(pinnedViews:)` со строками из ячеек;
- вертикальная сетка внутри горизонтального списка: Compose `LazyVerticalGrid` с неограниченной шириной, вероятно,
  падает; правило 10 (платформа не падает) распространяется и на этот случай, способ — на этапе 5;
- страница пейджера при ненулевом `contentPadding` в SwiftUI на iOS 17: учитывает ли `.paging` отступ
  `.contentMargins`;
- пейджер с `hug` поперёк оси: Compose `HorizontalPager` с `wrapContentHeight`, SwiftUI `.containerRelativeFrame` в
  стеке без высоты окна.

## 17. Открытые вопросы

1. Остальные поля `Component`.
2. Изменения `schema-format.md`: набор поддерживаемых событий у компонента, значения по умолчанию.
3. Закреплённые дети в потоковых контейнерах (вне контейнера наложения).
4. Оформление: стили оформления и градиент.
5. Контекст `expandable` (`collapsed`, `expanded`, в каждом — `control`) — вместе с первым аккордеоном, если его корень
   выглядит по-разному в этих положениях.
6. Состояния загрузки контента, кроме изображения (у `Image` — `placeholder` и `error`): файл и другие. Относятся к
   содержимому компонента, а не к `appearance`.
7. Неизвестные клиенту варианты `oneOf` в модели с сервера (например, новый вид фрагмента `TextSpan` у старого
   клиента): правило отображения вместо ошибки.
