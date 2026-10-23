# Image

## Назначение

`Image` показывает изображение: символ из словаря темы или картинку, загружаемую по адресу. Один компонент для обоих
случаев: вызывающему не нужно выбирать между иконкой и картинкой, компонент сам знает, показать ресурс сразу или
загрузить его.

Источник делится по происхождению:

- `resource` — ресурс темы или приложения, доступный без загрузки: показывается сразу, состояний загрузки нет;
- `remote` — картинка, которая загружается: пока загружается, показывается заглушка, при неудаче — ошибка.

Параметры, имеющие смысл только для одного вида источника, лежат внутри него: цвет — у символа, заглушка и вписывание —
у загружаемой картинки.

## Состав

| Файл                     | Роль                                                                       |
|--------------------------|----------------------------------------------------------------------------|
| `Image.json`             | Публичный контракт: источник изображения.                                  |
| `ImageSource.json`       | Источник: `resource` или `remote`.                                         |
| `ImageResource.json`     | Ресурс без загрузки: пока только `icon`; набор видов расширяется.          |
| `ImageResourceIcon.json` | Символ: имя из словаря `Icon` и цвет `ColorRole`.                          |
| `ImageRemote.json`       | Загружаемая картинка: адрес, вписывание, положение, заглушка, ошибка.      |
| `ImageOrigin.json`       | Откуда загружается: пока только `url`; набор способов расширяется.         |
| `ImageScale.json`        | Вписывание: `fit`, `crop`.                                                 |
| `ImageAlignment.json`    | Положение картинки: девять точек от `topStart` до `bottomEnd`.             |
| `samples/`               | Примеры моделей: `min.json`, `max.json`, `icon.json`, `productPhoto.json`. |

## Варианты

```
source: oneOf
  resource: oneOf
    icon: { name: Icon, color: ColorRole }
  remote: { origin: oneOf url(string), scale: fit | crop, alignment: ImageAlignment,
            placeholder?: ImageResource, error?: ImageResource }
```

| Поле                  | Compose                              | SwiftUI                                      | Figma                                         |
|-----------------------|--------------------------------------|----------------------------------------------|-----------------------------------------------|
| `resource.icon`       | `Icon(painterResource(…), tint = …)` | `Image(…).renderingMode(.template)`          | экземпляр символа `icon/<имя>`                |
| `resource.icon.color` | `tint`                               | `.foregroundStyle`                           | переменная цвета на заливке вектора           |
| `remote.origin.url`   | Coil `AsyncImage(model = url)`       | `AsyncImage(url:)`                           | заливка-картинка при отрисовке модели         |
| `remote.scale`        | `contentScale`: `Fit`, `Crop`        | `.aspectRatio(contentMode: .fit / .fill)`    | `scaleMode`: `FIT`, `FILL`                    |
| `remote.alignment`    | `alignment`: `Alignment.TopStart` …  | `.frame(alignment: .topLeading …)` + обрезка | `imageTransform`, только при отрисовке модели |
| `remote.placeholder`  | `placeholder`                        | фаза `.empty`                                | слой символа в библиотеке                     |
| `remote.error`        | `error`                              | фаза `.failure`                              | слой символа при ошибке загрузки              |

1. **Размер.** При `hug` компонент принимает собственный размер рисунка: у символа — `width` и `height` SVG. При
   `fixed` и `fill` символ вписывается в компонент за вычетом `padding` с сохранением пропорций и стоит по центру.
2. **Загружаемая картинка** вписывается по `scale` и `alignment`, размер компонента от неё не зависит. До загрузки
   собственного размера у картинки нет.
3. **Заглушка и ошибка** показываются в собственном размере по центру, без масштабирования: символ 24 на баннере не
   растягивается. Без `error` при ошибке показывается `placeholder`; без обоих компонент пуст, виден фон из
   `appearance`.
4. **Заглушка — ресурс**, а не любой источник: она не может загружаться сама, поэтому цепочек загрузок нет.
5. **Адрес** — абсолютный `https://`. Обе платформы по умолчанию блокируют `http` (App Transport Security на iOS,
   запрет cleartext на Android). Встроенные данные (`data:`) не поддерживаются: они раздувают модель.
6. Многоцветные ресурсы (иллюстрации) и другие способы получения картинки добавляются вариантами `ImageResource` и
   `ImageOrigin` без несовместимых изменений.

Ожидаемая модель на Kotlin:

```kotlin
data class Image(
    override val id: String? = null,
    override val layout: Layout,
    override val padding: Padding,
    override val appearance: Appearance,
    override val events: Set<Event>? = null,
    val source: ImageSource,
) : Component

sealed interface ImageSource {
    data class Resource(val value: ImageResource) : ImageSource
    data class Remote(
        val origin: ImageOrigin,
        val scale: ImageScale,
        val alignment: ImageAlignment,
        val placeholder: ImageResource? = null,
        val error: ImageResource? = null,
    ) : ImageSource
}

sealed interface ImageResource {
    // Вариант Icon затеняет тип foundation Icon внутри ImageResource, поэтому тип указан полным именем
    // (пакет задаёт генератор).
    data class Icon(val name: foundation.Icon, val color: ColorRole) : ImageResource
}

sealed interface ImageOrigin {
    data class Url(val value: String) : ImageOrigin
}
```

Сгенерированные примеры: [минимальная модель](samples/min.json) — только обязательные свойства,
[полная модель](samples/max.json) — все свойства.

Символ:

<!-- sample: samples/icon.json -->
```json
{
  "Image": {
    "layout": {
      "width": {
        "sizing": "hug"
      },
      "height": {
        "sizing": "hug"
      },
      "position": "flow"
    },
    "padding": {
      "all": "zero"
    },
    "appearance": {
      "plain": {}
    },
    "source": {
      "resource": {
        "icon": {
          "name": "close",
          "color": "onSurface"
        }
      }
    }
  }
}
```

Фото товара с заглушкой и ошибкой:

<!-- sample: samples/productPhoto.json -->
```json
{
  "Image": {
    "layout": {
      "width": {
        "sizing": "fill"
      },
      "height": {
        "sizing": {
          "fixed": "xl3"
        }
      },
      "position": "flow"
    },
    "padding": {
      "all": "zero"
    },
    "appearance": {
      "plain": {
        "background": {
          "solid": "surface2"
        },
        "cornerRadius": "m",
        "clip": true
      }
    },
    "source": {
      "remote": {
        "origin": {
          "url": "https://cdn.example.com/items/42.jpg"
        },
        "scale": "crop",
        "alignment": "center",
        "placeholder": {
          "icon": {
            "name": "image",
            "color": "onSurfaceMuted"
          }
        },
        "error": {
          "icon": {
            "name": "brokenImage",
            "color": "onSurfaceMuted"
          }
        }
      }
    }
  }
}
```

## Рекомендации по использованию

- Для символов используйте `resource.icon` и `layout` с `hug`: компонент примет размер рисунка. Если нужна область
  больше символа (например, для нажатия), увеличьте её `padding`, а не размер символа.
- Цвет символа берите из той же роли, что и у соседнего текста, чтобы символ и текст читались вместе.
- Для загружаемых картинок задавайте размер компонента явно: `fixed` или `fill`. При `hug` размер до загрузки
  неизвестен, и раскладка сдвинется после загрузки.
- Фото и обложки показывайте с `crop`, логотипы и схемы — с `fit`, чтобы ничего не обрезалось.
- Задавайте `placeholder` с фоном в `appearance` (например, `surface2`): пустое место до загрузки выглядит как ошибка
  вёрстки.
- Нажимаемое изображение — контекст `control` и событие `click`; отклик на нажатие задавайте наложением `overlay` в
  `pressed`: фон под картинкой не виден. Для кнопки-иконки с минимальной областью нажатия будет отдельный компонент.
- Картинку на фоне (баннер с текстом поверх) собирайте `Container` с раскладкой `overlay`: первым ребёнком `Image`
  с `fill` по обеим осям и `crop`, у контейнера — `cornerRadius` и `clip` (пример в README `Container`).

## Ограничения и открытые вопросы

1. Подпись для экранного диктора не задаётся: доступность решается для всех компонентов сразу.
2. Картинка на фоне формы — не вариант фона в `appearance`, а композиция `Container` с раскладкой `overlay`: адрес
   картинки — данные, а не оформление.
3. Индикаторы в углах с вырезом формы — отдельная задача.
4. Анимированная заглушка (мерцание) не поддерживается: заглушка — статичный ресурс.
5. Форматы загружаемых картинок определяют загрузчики платформ; рекомендуются растровые (JPEG, PNG, WebP): SVG по
   адресу на iOS без сторонних библиотек не отрисовывается.
6. Правило `https://` записано здесь, а не в контракте: ограничений значений строк в спецификации пока нет.
