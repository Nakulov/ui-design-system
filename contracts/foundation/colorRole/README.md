# ColorRole

## Назначение

`ColorRole` задаёт семантические роли цвета: назначение цвета в интерфейсе (основной акцент, фон, текст, обводка,
ошибка), а не конкретный цвет. Компоненты используют только роли; конкретные цвета задаются темой.

Тема содержит режимы `light` и `dark`: одна и та же роль имеет разные цвета в светлом и тёмном режимах.

Роли организованы группами:

- **Акцентные:** `primary`, `secondary`, `tertiary` с парами `on…`, `…Container`, `on…Container`.
- **Статусы:** `error`, `success`, `warning`, `info` с теми же парами.
- **Поверхности:** `background`, `surface`, `surface1`–`surface3`, `inverseSurface`, `inverseOnSurface`.
- **Содержимое (текст и иконки):** `onSurface`, `onSurfaceVariant`, `onSurfaceMuted`.
- **Обводки:** `outline`, `outlineStrong`.
- **Наложения:** `scrim`, `overlayPressed`, `overlayNeutral`, `overlayNeutralPressed`.

## Состав

| Файл                              | Роль                                                |
|-----------------------------------|-----------------------------------------------------|
| `ColorRole.json`                  | Публичный контракт: семантические роли цвета.       |
| `definitions/ColorRoleScale.json` | Вспомогательное перечисление: именованные роли.     |
| `figma.json`                      | Отображение в Figma: области применения переменных. |
| `changelog.json`                  | Журнал изменений API.                               |
| `preview.json`                    | Превью величины на странице документации Figma.     |

Цвета ролей задаются в темах: по умолчанию — `themes/default/ColorRole.values.json`. Большинство цветов — алиасы на
внутреннюю палитру темы `themes/default/ColorPalette.json`; полупрозрачные наложения заданы непосредственно.

## Варианты

Цвета справочные: источником истины является `themes/default/ColorRole.values.json`.

| Роль                    | light       | dark        |
|-------------------------|-------------|-------------|
| `primary`               | `#C9A227`   | `#E5B74F`   |
| `onPrimary`             | `#16140F`   | `#16140F`   |
| `primaryContainer`      | `#FCECC8`   | `#4E3C0A`   |
| `onPrimaryContainer`    | `#4E3C0A`   | `#FCECC8`   |
| `secondary`             | `#7A3E57`   | `#C97FA3`   |
| `onSecondary`           | `#FFFFFF`   | `#2C121E`   |
| `secondaryContainer`    | `#EFCFDB`   | `#471F32`   |
| `onSecondaryContainer`  | `#2C121E`   | `#F7E9EF`   |
| `tertiary`              | `#9A6B1B`   | `#FFD28A`   |
| `onTertiary`            | `#FFFFFF`   | `#2B1B05`   |
| `tertiaryContainer`     | `#FFE7C2`   | `#3A2510`   |
| `onTertiaryContainer`   | `#2B1B05`   | `#FFE7C2`   |
| `error`                 | `#B3261E`   | `#F2B8B5`   |
| `onError`               | `#FFFFFF`   | `#601410`   |
| `errorContainer`        | `#F9DEDC`   | `#8C1D18`   |
| `onErrorContainer`      | `#410E0B`   | `#F9DEDC`   |
| `success`               | `#3F6F3A`   | `#A8D99A`   |
| `onSuccess`             | `#FFFFFF`   | `#10210D`   |
| `successContainer`      | `#DCEFD4`   | `#243D1F`   |
| `onSuccessContainer`    | `#183013`   | `#DCEFD4`   |
| `warning`               | `#9A6A00`   | `#FFD36A`   |
| `onWarning`             | `#FFFFFF`   | `#3F2A00`   |
| `warningContainer`      | `#FFE6AD`   | `#3F2A00`   |
| `onWarningContainer`    | `#3F2A00`   | `#FFE6AD`   |
| `info`                  | `#3F5F8F`   | `#AFC8FF`   |
| `onInfo`                | `#FFFFFF`   | `#1D2D4D`   |
| `infoContainer`         | `#DCE7FF`   | `#1D2D4D`   |
| `onInfoContainer`       | `#1D2D4D`   | `#DCE7FF`   |
| `background`            | `#FBF9F4`   | `#11110F`   |
| `surface`               | `#FFFFFF`   | `#181714`   |
| `surface1`              | `#F3EFE6`   | `#22201C`   |
| `surface2`              | `#E7E1D4`   | `#2C2A25`   |
| `surface3`              | `#D6CDBA`   | `#3A3731`   |
| `inverseSurface`        | `#181714`   | `#181714`   |
| `inverseOnSurface`      | `#F3EFE6`   | `#F3EFE6`   |
| `onSurface`             | `#16140F`   | `#F3EFE6`   |
| `onSurfaceVariant`      | `#6F6657`   | `#D6CDBA`   |
| `onSurfaceMuted`        | `#9C907A`   | `#A29A8E`   |
| `outline`               | `#D6CDBA`   | `#3A3731`   |
| `outlineStrong`         | `#BFB49D`   | `#555149`   |
| `scrim`                 | `#000000`   | `#000000`   |
| `overlayPressed`        | `#1E2B4414` | `#E1E1F817` |
| `overlayNeutral`        | `#090C2547` | `#E4E4FB47` |
| `overlayNeutralPressed` | `#04041578` | `#EEEEFE75` |

Ожидаемая модель на Kotlin:

```kotlin
sealed interface ColorRole {
    data object Primary : ColorRole
    data object OnPrimary : ColorRole
    data object PrimaryContainer : ColorRole
    data object OnPrimaryContainer : ColorRole
    // … остальные роли по тому же образцу
}
```

Цвета не входят в тип: они генерируются из темы и берутся из неё, например
`LocalColorRoleValues.current.of(ColorRole.Primary)`. Алиасы на палитру генератор заменяет итоговыми цветами.

## Рекомендации по использованию

- Используйте роли, а не цвета палитры: это обеспечивает поддержку тёмного режима и других тем.
- Сочетайте роль с её парой `on…`: текст поверх `primary` — `onPrimary`, поверх `surface` — `onSurface`.
- `…Container` используйте для приглушённых фонов (бейджи, плашки, выделенные карточки), основную роль — для
  акцентных элементов (кнопки, активные индикаторы).
- Уровни `surface1`–`surface3` используйте для вложенных поверхностей по возрастанию контраста.

## Ограничения и открытые вопросы

- Произвольный цвет вне ролей (`custom`) пока не предусмотрен; добавляется новым элементом `oneOf`.
- `inverseSurface` и `inverseOnSurface` в теме `default` одинаковы в режимах `light` и `dark`; требуется проверить, что
  это намеренно.
