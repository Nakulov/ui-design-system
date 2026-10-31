# Container

## Назначение

`Container` располагает дочерние компоненты в строку, в колонку (в том числе с переносом) или наложением. Экран собирается из вложенных
контейнеров, как макет в Figma — из вложенных фреймов с Auto Layout.

Контейнер наследует базовую модель `Component`: размеры и положение в родителе (`layout`), внутренние отступы
(`padding`), оформление (`appearance`, контекст `plain`) и события (`events`). Собственные поля
контейнера — способ расположения детей (`arrangement`) и сами дети (`children`).

## Состав

| Файл                                               | Роль                                                                       |
|----------------------------------------------------|----------------------------------------------------------------------------|
| `Container.json`                                   | Публичный контракт: способ расположения и дети.                            |
| `definitions/ContainerArrangement.json`            | Способ расположения: строка, колонка, перенос, наложение.                  |
| `definitions/ContainerHorizontal.json`             | Строка: выравнивание по вертикали, распределение.                          |
| `definitions/ContainerVertical.json`               | Колонка: выравнивание по горизонтали, распределение.                       |
| `definitions/ContainerHorizontalWrap.json`         | Строка с переносом: выравнивание, распределение в строке и строк.          |
| `definitions/ContainerVerticalWrap.json`           | Колонка с переносом: выравнивание, распределение в колонке и колонок.      |
| `definitions/ContainerOverlay.json`                | Наложение: положение детей по обеим осям.                                  |
| `definitions/ContainerHorizontalDistribution.json` | Распределение в строке: `packed` или `spaceBetween`.                       |
| `definitions/ContainerVerticalDistribution.json`   | Распределение в колонке: `packed` или `spaceBetween`.                      |
| `definitions/ContainerHorizontalPacked.json`       | Дети строки вместе: промежуток и положение группы.                         |
| `definitions/ContainerVerticalPacked.json`         | Дети колонки вместе: промежуток и положение группы.                        |
| `definitions/ContainerLineDistribution.json`       | Распределение строк или колонок при переносе: `packed` или `spaceBetween`. |
| `definitions/ContainerLinePacked.json`             | Строки или колонки вместе: промежуток.                                     |
| `definitions/ContainerDistributionMode.json`       | Перечисление распределений без параметров: `spaceBetween`.                 |
| `definitions/ContainerHorizontalAlignment.json`    | Положение по горизонтали: `start`, `center`, `end`.                        |
| `definitions/ContainerVerticalAlignment.json`      | Положение по вертикали: `top`, `center`, `bottom`.                         |
| `definitions/ContainerRowAlignment.json`           | Выравнивание в строке: `top`, `center`, `bottom`, `baseline`.              |
| `docs.json`                                        | Манифест документации Figma: секции страницы и примеры.                    |
| `changelog.json`                                   | Журнал изменений API.                                                      |
| `samples/`                                         | Образцы: `min.json`, `max.json`.                                           |

## Варианты

```
arrangement: oneOf
  horizontal     { alignment: top | center | bottom | baseline, distribution }
  vertical       { alignment: start | center | end,             distribution }
  horizontalWrap { alignment: top | center | bottom | baseline, distribution, lineDistribution }
  verticalWrap   { alignment: start | center | end,             distribution, lineDistribution }
  overlay        { horizontal: start | center | end,            vertical: top | center | bottom }

distribution: oneOf
  packed { gap: Spacing, position }    — position по оси контейнера
  spaceBetween

lineDistribution: oneOf               — строки (колонки) при переносе
  packed { gap: Spacing }             — положение группы задаёт alignment
  spaceBetween
```

| Вариант            | Figma                                                                 | Compose                                                                    | SwiftUI                                         |
|--------------------|-----------------------------------------------------------------------|----------------------------------------------------------------------------|-------------------------------------------------|
| `horizontal`       | Auto Layout, horizontal                                               | `Row`                                                                      | `HStack`                                        |
| `vertical`         | Auto Layout, vertical                                                 | `Column`                                                                   | `VStack`                                        |
| `horizontalWrap`   | Auto Layout, horizontal, Wrap                                         | `FlowRow`                                                                  | `FlowLayout` дизайн-системы (`Layout`, iOS 16+) |
| `verticalWrap`     | Auto Layout, vertical, Wrap                                           | `FlowColumn`                                                               | `FlowLayout` дизайн-системы (`Layout`, iOS 16+) |
| `overlay`          | фрейм без Auto Layout, дети с констрейнтами                           | `Box(contentAlignment)`                                                    | `ZStack(alignment:)`                            |
| `packed`           | Spacing between items = `gap`, выравнивание                           | `Arrangement.spacedBy(gap, position)`                                      | `spacing:` и `Spacer` у краёв                   |
| `spaceBetween`     | Spacing between items = Auto                                          | `Arrangement.SpaceBetween`                                                 | `Spacer` между детьми                           |
| `lineDistribution` | `counterAxisSpacing`, `counterAxisAlignContent` (Auto, Space between) | `verticalArrangement` / `horizontalArrangement` у `FlowRow` / `FlowColumn` | параметр `FlowLayout`                           |

1. `alignment` выравнивает детей поперёк оси контейнера. Растяжение поперёк оси задаёт не контейнер, а `layout`
   ребёнка (`fill`).
2. `gap` есть только у `packed`: при `spaceBetween` промежуток вычисляется.
3. В наложении последний ребёнок рисуется сверху. `overlay` задаёт положение детей с `position = flow`; дети с
   `position = pinned` располагаются по своему закреплению. В Figma плагин переводит положение в констрейнты.
4. `spaceAround` и `spaceEvenly` не поддерживаются: в Figma их нет.
5. При переносе один `alignment` выравнивает и детей внутри строки, и группу строк в контейнере: в Figma оба
   задаются `counterAxisAlignItems`. Поэтому у `lineDistribution.packed` нет своего положения. Исключение —
   `baseline`: дети в строке выравниваются по базовой линии, а строки прижаты к верху.
6. Ограничения числа строк и детей в строке (`maxLines`, `maxItemsInEachRow` в Compose) не поддерживаются: в Figma
   их нет.
7. В Figma контейнер — фрейм, а не Figma-компонент: он создаётся только при отрисовке модели (`figma-mapping.md`,
   разделы 3.4 и 10.3).
8. `baseline` есть только у строки (`horizontal`, `horizontalWrap`): в Figma `BASELINE` только у горизонтального
   Auto Layout, у `Column` в Compose его нет. Используется первая базовая линия: другой в Figma нет, многострочный
   текст выравнивается по первой строке. Compose — `Modifier.alignByBaseline()`, SwiftUI — `.firstTextBaseline`, в
   `FlowLayout` — по `ViewDimensions[.firstTextBaseline]`.
9. При `baseline` ребёнок без текста (иконка, плашка) выравнивается нижним краем, как в SwiftUI и Figma; в Compose —
   `Modifier.alignBy { it.measuredHeight }`. Вложенный контейнер выравнивается по базовой линии своего первого текста.

**Картинка на фоне.** Баннер или карточка с обложкой — контейнер с раскладкой `overlay`: первый ребёнок — `Image` с
`fill` по обеим осям и `crop`, поверх — содержимое. Скругление и `clip` задаёт контейнер. Картинка закрывает фон
контейнера. Кликабельный баннер — `Pressable` над контейнером: нажатие показывает наложение `overlayPressed` в
`appearance.control.pressed` рамки (README [`Pressable`](../pressable/README.md)).

Сгенерированные образцы: [минимальная модель](samples/min.json) — только обязательные свойства,
[полная модель](samples/max.json) — все свойства.

Промо-баннер с картинкой на фоне:

<!-- sample: samples/promoBanner.json -->
```json
{
  "Container": {
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
        "cornerRadius": "l",
        "clip": true
      }
    },
    "arrangement": {
      "overlay": {
        "horizontal": "start",
        "vertical": "bottom"
      }
    },
    "children": [
      {
        "Image": {
          "layout": {
            "width": {
              "sizing": "fill"
            },
            "height": {
              "sizing": "fill"
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
            "remote": {
              "origin": {
                "url": "https://raw.githubusercontent.com/Nakulov/ui-design-system/dev/assets/images/banner.jpg"
              },
              "scale": "crop",
              "alignment": "center",
              "placeholder": {
                "icon": {
                  "name": "image",
                  "color": "onSurfaceMuted"
                }
              }
            }
          }
        }
      },
      {
        "Text": {
          "layout": {
            "width": {
              "sizing": "fill"
            },
            "height": {
              "sizing": "hug"
            },
            "position": "flow"
          },
          "padding": {
            "all": "l"
          },
          "appearance": {
            "plain": {}
          },
          "content": {
            "plain": "Осенняя распродажа"
          },
          "typography": "headlineSmall",
          "color": {
            "solid": "inverseOnSurface"
          },
          "alignment": "start"
        }
      }
    ]
  }
}
```

Карточка товара: фото, название и цена в столбец, кнопка закрытия закреплена в правом верхнем углу внутри отступов
(`pinned`, `relativeTo: content`):

<!-- sample: samples/productCard.json -->
```json
{
  "Container": {
    "layout": {
      "width": {
        "sizing": "fill"
      },
      "height": {
        "sizing": "hug"
      },
      "position": "flow"
    },
    "padding": {
      "all": "m"
    },
    "appearance": {
      "plain": {
        "background": {
          "solid": "surface"
        },
        "shadow": "m",
        "cornerRadius": "l",
        "stroke": {
          "width": "s",
          "color": "outline"
        },
        "clip": true
      }
    },
    "events": [
      {
        "click": {
          "payload": {
            "action": "openProduct"
          }
        }
      }
    ],
    "arrangement": {
      "vertical": {
        "alignment": "start",
        "distribution": {
          "packed": {
            "gap": "s",
            "position": "top"
          }
        }
      }
    },
    "children": [
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
                "url": "https://raw.githubusercontent.com/Nakulov/ui-design-system/dev/assets/images/product.jpg"
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
      },
      {
        "Text": {
          "layout": {
            "width": {
              "sizing": "fill"
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
          "content": {
            "plain": "Кружка керамическая, 350 мл"
          },
          "typography": "titleSmall",
          "color": {
            "solid": "onSurface"
          },
          "alignment": "start"
        }
      },
      {
        "Text": {
          "layout": {
            "width": {
              "sizing": "fill"
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
          "content": {
            "plain": "1 490 ₽"
          },
          "typography": "labelLarge",
          "color": {
            "solid": "primary"
          },
          "alignment": "start"
        }
      },
      {
        "Image": {
          "id": "close",
          "layout": {
            "width": {
              "sizing": "hug"
            },
            "height": {
              "sizing": "hug"
            },
            "position": {
              "pinned": {
                "horizontal": {
                  "end": {
                    "offset": "s"
                  }
                },
                "vertical": {
                  "top": {
                    "offset": "s"
                  }
                },
                "relativeTo": "content"
              }
            }
          },
          "padding": {
            "all": "xs2"
          },
          "appearance": {
            "plain": {
              "background": {
                "solid": "surface"
              },
              "cornerRadius": "full"
            }
          },
          "events": [
            {
              "click": {
                "payload": {
                  "action": "dismiss"
                }
              }
            }
          ],
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
    ]
  }
}
```

Облако меток с переносом строк: расстояние между метками вне шкалы (`custom`), между строками — `s`:

<!-- sample: samples/tagCloud.json -->
```json
{
  "Container": {
    "layout": {
      "width": {
        "sizing": "fill"
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
    "arrangement": {
      "horizontalWrap": {
        "alignment": "center",
        "distribution": {
          "packed": {
            "gap": {
              "custom": {
                "value": 6
              }
            },
            "position": "start"
          }
        },
        "lineDistribution": {
          "packed": {
            "gap": "s"
          }
        }
      }
    },
    "children": [
      {
        "Badge": {
          "layout": {
            "width": {
              "sizing": "hug"
            },
            "height": {
              "sizing": {
                "fixed": "s"
              }
            },
            "position": "flow"
          },
          "padding": {
            "symmetric": {
              "horizontal": "s",
              "vertical": "zero"
            }
          },
          "appearance": {
            "plain": {
              "background": {
                "solid": "secondaryContainer"
              },
              "cornerRadius": "full"
            }
          },
          "content": {
            "text": {
              "value": "Новинка",
              "typography": "labelMedium",
              "color": "onSecondaryContainer"
            }
          }
        }
      },
      {
        "Badge": {
          "layout": {
            "width": {
              "sizing": "hug"
            },
            "height": {
              "sizing": {
                "fixed": "s"
              }
            },
            "position": "flow"
          },
          "padding": {
            "symmetric": {
              "horizontal": "s",
              "vertical": "zero"
            }
          },
          "appearance": {
            "plain": {
              "background": {
                "solid": "secondaryContainer"
              },
              "cornerRadius": "full"
            }
          },
          "content": {
            "text": {
              "value": "Хит продаж",
              "typography": "labelMedium",
              "color": "onSecondaryContainer"
            }
          }
        }
      },
      {
        "Badge": {
          "layout": {
            "width": {
              "sizing": "hug"
            },
            "height": {
              "sizing": {
                "fixed": "s"
              }
            },
            "position": "flow"
          },
          "padding": {
            "symmetric": {
              "horizontal": "s",
              "vertical": "zero"
            }
          },
          "appearance": {
            "plain": {
              "background": {
                "solid": "secondaryContainer"
              },
              "cornerRadius": "full"
            }
          },
          "content": {
            "text": {
              "value": "Бесплатная доставка",
              "typography": "labelMedium",
              "color": "onSecondaryContainer"
            }
          }
        }
      },
      {
        "Badge": {
          "layout": {
            "width": {
              "sizing": "hug"
            },
            "height": {
              "sizing": {
                "fixed": "s"
              }
            },
            "position": "flow"
          },
          "padding": {
            "symmetric": {
              "horizontal": "s",
              "vertical": "zero"
            }
          },
          "appearance": {
            "plain": {
              "background": {
                "solid": "secondaryContainer"
              },
              "cornerRadius": "full"
            }
          },
          "content": {
            "text": {
              "value": "Скидка 20%",
              "typography": "labelMedium",
              "color": "onSecondaryContainer"
            }
          }
        }
      },
      {
        "Badge": {
          "layout": {
            "width": {
              "sizing": "hug"
            },
            "height": {
              "sizing": {
                "fixed": "s"
              }
            },
            "position": "flow"
          },
          "padding": {
            "symmetric": {
              "horizontal": "s",
              "vertical": "zero"
            }
          },
          "appearance": {
            "plain": {
              "background": {
                "solid": "secondaryContainer"
              },
              "cornerRadius": "full"
            }
          },
          "content": {
            "text": {
              "value": "Керамика",
              "typography": "labelMedium",
              "color": "onSecondaryContainer"
            }
          }
        }
      },
      {
        "Badge": {
          "layout": {
            "width": {
              "sizing": "hug"
            },
            "height": {
              "sizing": {
                "fixed": "s"
              }
            },
            "position": "flow"
          },
          "padding": {
            "symmetric": {
              "horizontal": "s",
              "vertical": "zero"
            }
          },
          "appearance": {
            "plain": {
              "background": {
                "solid": "secondaryContainer"
              },
              "cornerRadius": "full"
            }
          },
          "content": {
            "text": {
              "value": "Ручная работа",
              "typography": "labelMedium",
              "color": "onSecondaryContainer"
            }
          }
        }
      }
    ]
  }
}
```

Ожидаемая модель на Kotlin:

```kotlin
data class Container(
    override val id: String? = null,
    override val layout: Layout,
    override val padding: Padding,
    override val appearance: Appearance,
    override val indicators: List<ComponentIndicator>? = null,
    override val events: Set<Event>? = null,
    val arrangement: ContainerArrangement,
    val children: List<Component>? = null,
) : Component

sealed interface ContainerArrangement {
    data class Horizontal(
        val alignment: ContainerRowAlignment,
        val distribution: ContainerHorizontalDistribution,
    ) : ContainerArrangement

    data class Vertical(
        val alignment: ContainerHorizontalAlignment,
        val distribution: ContainerVerticalDistribution,
    ) : ContainerArrangement

    data class HorizontalWrap(
        val alignment: ContainerRowAlignment,
        val distribution: ContainerHorizontalDistribution,
        val lineDistribution: ContainerLineDistribution,
    ) : ContainerArrangement

    data class VerticalWrap(
        val alignment: ContainerHorizontalAlignment,
        val distribution: ContainerVerticalDistribution,
        val lineDistribution: ContainerLineDistribution,
    ) : ContainerArrangement

    data class Overlay(
        val horizontal: ContainerHorizontalAlignment,
        val vertical: ContainerVerticalAlignment,
    ) : ContainerArrangement
}

sealed interface ContainerHorizontalDistribution {
    data class Packed(val gap: Spacing, val position: ContainerHorizontalAlignment) : ContainerHorizontalDistribution
    data object SpaceBetween : ContainerHorizontalDistribution
}
```

## Рекомендации по использованию

- Сложные раскладки собирайте вложенными контейнерами: связей между соседними детьми в модели нет.
- Для отступа между детьми используйте `gap`, а не `padding` детей.
- Для растяжения ребёнка поперёк оси задавайте ему `fill` в `layout`.
- Для строки из текстов разного размера используйте `baseline`, а не `center`: подписи встанут на одну линию.

**Сборка макетов в Figma**

- В макете раскладку собирайте фреймами с Auto Layout — строка, колонка, перенос; наложение — фрейм без Auto Layout.
  Отдельного Figma-компонента у контейнера нет: такой фрейм и есть контейнер.
- Внутри — только экземпляры компонентов библиотеки и такие же фреймы. Прямоугольники, группы и тексты без компонента
  `Text` на платформах не отрисовываются.
- Промежутки и отступы — переменными `Spacing`, заливка и обводка — `ColorRole`, скругление — `CornerRadius`, тень —
  стилем `Elevation`. Числа без переменных в модель не переносятся.
- Настройки фрейма, которых нет в свойствах контейнера, на платформах не воспроизводятся.
- Слой называйте по компоненту: `Container`, `Text`, `Badge`. Смысловое имя — через ` · `: `Container · header`. Оно
  становится `id` модели (UI-тесты, события, аналитика), поэтому пишется camelCase латиницей и не повторяется в макете.
  Элементам без `id` (например, повторяющимся в списке) имя не нужно.

## Ограничения и открытые вопросы

- В SwiftUI готового переноса нет: генератор поставляет собственный `FlowLayout` на протоколе `Layout`.
- Значений по умолчанию в формате пока нет, поэтому поля выравнивания и распределения обязательны.
