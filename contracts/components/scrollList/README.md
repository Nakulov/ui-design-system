# ScrollList

## Назначение

`ScrollList` — прокручиваемый список: окно, в котором содержимое прокручивается вдоль одной оси. Длинный экран
настроек или формы, лента из данных с подгрузкой (заказы, уведомления, поиск), чат, карусель карточек.

Одна модель и для содержимого, заданного в макете, и для списка из данных: модель — дерево, данные в дерево
разворачивает приложение или сервер. Контракту и Figma всё равно, статический список или из данных; ленивую отрисовку
выбирает платформа.

Список наследует базовую модель `Component`: размеры и положение в родителе (`layout`), отступ окна (`padding`),
оформление (`appearance`, контекст `plain`) и события (`events`, в том числе `reachEnd`). Собственные поля — отступ у
содержимого (`contentPadding`) и содержимое (`content`): вариант задаёт раскладку и элементы — список или сетку.
Пейджеры добавятся вариантами `content` (#222).

## Состав

| Файл                                        | Роль                                                                           |
|---------------------------------------------|--------------------------------------------------------------------------------|
| `ScrollList.json`                           | Публичный контракт: отступ у содержимого и содержимое.                         |
| `definitions/ScrollListContent.json`        | Содержимое: `vertical`, `horizontal`, `verticalGrid` или `horizontalGrid`.     |
| `definitions/ScrollListVertical.json`       | Вертикальный список: выравнивание, промежуток, положение, элементы.            |
| `definitions/ScrollListHorizontal.json`     | Горизонтальный список: выравнивание, промежуток, положение, элементы.          |
| `definitions/ScrollListElement.json`        | Элемент списка: `header` или `item`.                                           |
| `definitions/ScrollListHeader.json`         | Заголовок раздела: компонент, прилипающий к началу окна.                       |
| `definitions/ScrollListItem.json`           | Обычный элемент: компонент и доля окна `viewportFraction`.                     |
| `definitions/ScrollListVerticalGrid.json`   | Вертикальная сетка: колонки, промежутки, элементы.                             |
| `definitions/ScrollListHorizontalGrid.json` | Горизонтальная сетка: строки, промежутки, докатывание, элементы.               |
| `definitions/ScrollListGridTracks.json`     | Колонки (строки) сетки: `fixed` — число, `adaptive` — минимальный размер.      |
| `definitions/ScrollListGridElement.json`    | Элемент сетки: `header` или `item`.                                            |
| `definitions/ScrollListGridItem.json`       | Обычный элемент сетки: компонент и ширина в колонках `span`.                   |
| `definitions/ScrollListGridSpan.json`       | Ширина в колонках (строках): `count` — число, `full` — на всю строку.          |
| `definitions/ScrollListGridSpanMode.json`   | Ширина без числа: `full`.                                                      |
| `docs.json`                                 | Манифест документации Figma: секции страницы и примеры.                        |
| `changelog.json`                            | Журнал изменений API.                                                          |
| `samples/`                                  | Образцы: `min.json`, `max.json` и по одному на сценарий (`search.json` и др.). |

## Варианты

```
ScrollList : Component                         appearanceContext: plain
  contentPadding: Padding = { "all": "zero" }  отступ у содержимого
  content: ScrollListContent

ScrollListContent: oneOf
  vertical        { alignment: start | center | end = start,  gap = zero, position: top | center | bottom = top,
                    reversed = false, snapToItem = false, children?: [ScrollListElement] }
  horizontal      { alignment: top | center | bottom = top,   gap = zero, position: start | center | end = start,
                    reversed = false, snapToItem = false, children?: [ScrollListElement] }
  verticalGrid    { columns: ScrollListGridTracks, rowGap = zero, columnGap = zero,
                    children?: [ScrollListGridElement] }
  horizontalGrid  { rows: ScrollListGridTracks, rowGap = zero, columnGap = zero, snapToItem = false,
                    children?: [ScrollListGridElement] }

ScrollListElement: oneOf
  header  { component: Component }
  item    { component: Component, viewportFraction?: float }

ScrollListGridTracks: oneOf
  fixed     integer   число колонок (строк)
  adaptive  Size      минимальная ширина колонки (высота строки)

ScrollListGridElement: oneOf
  header  { component: Component }                        на всю строку (весь столбец)
  item    { component: Component, span = { "count": 1 } }

ScrollListGridSpan: oneOf
  count  integer
  full
```

| Поле                  | Смысл                                                           | Compose                                          | SwiftUI                                                     |
|-----------------------|-----------------------------------------------------------------|--------------------------------------------------|-------------------------------------------------------------|
| `vertical`            | вертикальный список                                             | `LazyColumn`                                     | `ScrollView { LazyVStack }`                                 |
| `horizontal`          | горизонтальный список                                           | `LazyRow`                                        | `ScrollView(.horizontal) { LazyHStack }`                    |
| `contentPadding`      | отступ у содержимого, элементы доезжают до края окна            | `contentPadding = PaddingValues(…)`              | `.contentMargins(…, for: .scrollContent)`                   |
| `alignment`           | выравнивание элементов поперёк оси                              | `horizontalAlignment` / `verticalAlignment`      | `LazyVStack(alignment:)` / `LazyHStack(alignment:)`         |
| `gap`                 | промежуток между элементами                                     | `Arrangement.spacedBy(gap, …)`                   | `spacing:`                                                  |
| `position`            | положение элементов вдоль оси, пока содержимое короче окна      | `Arrangement.spacedBy(gap, Alignment.Bottom)`    | высота содержимого не меньше окна и выравнивание            |
| `reversed`            | первый элемент — у конца оси, прокрутка начинается оттуда       | `reverseLayout = true`                           | не проверено (раздел «Ограничения и открытые вопросы»)      |
| `snapToItem`          | докатывание к ближайшему элементу после жеста                   | `rememberSnapFlingBehavior(state)`               | `.scrollTargetBehavior(.viewAligned)`                       |
| `header`              | заголовок, прилипающий к началу окна                            | `stickyHeader`                                   | `LazyVStack(pinnedViews: .sectionHeaders)`, `Section`       |
| `viewportFraction`    | размер ячейки вдоль оси — доля окна за вычетом `contentPadding` | `Modifier.fillParentMaxWidth(fraction)`          | `.containerRelativeFrame(.horizontal) { … }`                |
| `reachEnd`            | долистали до текущего конца содержимого                         | `LazyListState`, `snapshotFlow`                  | `.onAppear` последнего элемента                             |
| `verticalGrid`        | вертикальная сетка                                              | `LazyVerticalGrid`                               | `ScrollView { LazyVStack }`, строки из ячеек — ниже         |
| `horizontalGrid`      | горизонтальная сетка                                            | `LazyHorizontalGrid`                             | `ScrollView(.horizontal) { LazyHStack }`, столбцы           |
| `columns`, `rows`     | `fixed` — число, `adaptive` — минимальный размер                | `GridCells.Fixed(n)` / `GridCells.Adaptive(min)` | число; у `adaptive` — по ширине окна (`onGeometryChange`)   |
| `rowGap`, `columnGap` | промежутки между строками и колонками                           | `verticalArrangement` / `horizontalArrangement`  | `spacing:` стека и строки                                   |
| `span`                | ширина элемента в колонках (у `horizontalGrid` — в строках)     | `GridItemSpan(minOf(n, maxLineSpan))`            | `.containerRelativeFrame(.horizontal, count:span:spacing:)` |
| `header` в сетке      | ячейка на всю строку (весь столбец)                             | `item(span = { GridItemSpan(maxLineSpan) })`     | строка из одной ячейки, `Section`                           |

**Окно и содержимое.** `padding` — отступ окна по общим правилам: элементы обрезаются у его края. `contentPadding` —
отступ у содержимого: он есть в начале и в конце, а при прокрутке элементы доезжают до края окна. Ячейка элемента
поперёк оси — на всю ширину (высоту) содержимого; вдоль оси — по компоненту или по `viewportFraction`.

**Ключ элемента** — `id` компонента (`component-model.md`, раздел 5); без `id` — позиция. Если в модели с сервера `id`
повторяется, повторяющийся элемент получает ключ по позиции, платформа не падает. Образцы с повтором `id` валидатор
по-прежнему отклоняет.

**`header`** — обычный компонент; прилипание к краю окна обеспечивает платформа. Размер компонента в ячейке — его
`layout`.

**`position` и `reversed`.** `position` визуальный, `reversed` его смысл не меняет. В перевёрнутом списке первый
ребёнок — у конца оси (внизу), прокрутка начинается оттуда; порядок в модели — от новых к старым. История подгружается
дописыванием в конец `children` и событием `reachEnd`: в перевёрнутом списке конец — верх.

**`reachEnd`** срабатывает относительно текущего конца содержимого: пришла порция — конец сдвинулся, старых меток нет.

**`viewportFraction`** — доля окна вдоль оси, окно — за вычетом `contentPadding` (как страница пейджера). Пример: окно
360, `contentPadding` по 16, `0.4` → (360 − 32) × 0.4 ≈ 131. Если у списка вдоль оси `hug`, окна нет —
`viewportFraction` игнорируется, элемент меряется своим `layout`. Размер ячейки задаёт список, компонент в ней
меряется своим `layout`. Без поля элемент меряется своим `layout`.

**`snapToItem`** — после жеста список докатывается к ближайшему элементу; сильный свайп пролистывает несколько.

**Сетка.** Ширина колонки зависит от ширины окна (за вычетом `contentPadding`) и числа колонок, а не от числа
элементов. Элементы заполняют строки с начала; элемент, которому не хватает места в строке, начинает новую. Неполная
строка — элементы с начала, колонки сохраняют ширину, пустые ячейки остаются пустыми: 2 плитки при 4 колонках занимают
половину строки. `span` больше числа колонок прижимается к нему — элемент на всю строку, платформа не падает (при
адаптивных колонках число зависит от ширины экрана). `header` в сетке — всегда на всю строку; размер компонента в
ячейке — его `layout`. Адаптивные колонки (`adaptive`): число колонок считает платформа — столько, сколько помещается
колонок не уже заданного размера, остаток делится между ними. `horizontalGrid` — то же поперёк: элементы заполняют
столбцы сверху вниз, `span` — в строках, `header` занимает весь столбец, число адаптивных строк — по высоте окна.

**Сетка в SwiftUI.** У `LazyVGrid` элементы одной ширины, `span` не выразить. Способ проверен компиляцией (`swiftc`):
платформа собирает элементы в строки по правилу выше, строка — `HStack(spacing: columnGap)` внутри
`LazyVStack(spacing: rowGap)`, ячейка — `.containerRelativeFrame(.horizontal, count: columns, span: span, spacing:
columnGap)` (iOS 17+); число адаптивных колонок — по ширине окна (`onGeometryChange`). Горизонтальная сетка — так же
столбцами в `LazyHStack`.

**Вложенная прокрутка** в одном направлении — ошибка проектирования: валидатор её не проверяет, платформа не падает —
внутренний список рисуется колонкой без своей прокрутки. Разные направления (карусель в ленте) — норма.

Экран настроек: вертикальный список с заголовками разделов.

<!-- sample: samples/settings.json -->
```json
{
  "ScrollList": {
    "id": "settings",
    "layout": {
      "width": {
        "sizing": "fill"
      },
      "height": {
        "sizing": {
          "fixed": {
            "custom": {
              "value": 320
            }
          }
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
          "solid": "surface"
        }
      }
    },
    "contentPadding": {
      "symmetric": {
        "horizontal": "zero",
        "vertical": "s"
      }
    },
    "content": {
      "vertical": {
        "children": [
          {
            "header": {
              "component": {
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
                    "sides": {
                      "start": "m",
                      "top": "l",
                      "end": "m",
                      "bottom": "xs"
                    }
                  },
                  "appearance": {
                    "plain": {
                      "background": {
                        "solid": "surface"
                      }
                    }
                  },
                  "content": {
                    "plain": "Аккаунт"
                  },
                  "typography": "titleSmall",
                  "color": {
                    "solid": "primary"
                  },
                  "alignment": "start"
                }
              }
            }
          },
          {
            "item": {
              "component": {
                "Container": {
                  "id": "profile",
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
                    "symmetric": {
                      "horizontal": "m",
                      "vertical": "m"
                    }
                  },
                  "appearance": {
                    "plain": {}
                  },
                  "events": [
                    {
                      "click": {
                        "payload": {
                          "action": "profile"
                        }
                      }
                    }
                  ],
                  "arrangement": {
                    "horizontal": {
                      "alignment": "center",
                      "distribution": "spaceBetween"
                    }
                  },
                  "children": [
                    {
                      "Text": {
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
                        "content": {
                          "plain": "Профиль"
                        },
                        "typography": "bodyLarge",
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
                        "content": {
                          "plain": "Анна"
                        },
                        "typography": "bodyMedium",
                        "color": {
                          "solid": "onSurfaceVariant"
                        },
                        "alignment": "start"
                      }
                    }
                  ]
                }
              }
            }
          },
          {
            "item": {
              "component": {
                "Container": {
                  "id": "phone",
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
                    "symmetric": {
                      "horizontal": "m",
                      "vertical": "m"
                    }
                  },
                  "appearance": {
                    "plain": {}
                  },
                  "events": [
                    {
                      "click": {
                        "payload": {
                          "action": "phone"
                        }
                      }
                    }
                  ],
                  "arrangement": {
                    "horizontal": {
                      "alignment": "center",
                      "distribution": "spaceBetween"
                    }
                  },
                  "children": [
                    {
                      "Text": {
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
                        "content": {
                          "plain": "Телефон"
                        },
                        "typography": "bodyLarge",
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
                        "content": {
                          "plain": "+7 900 000-00-00"
                        },
                        "typography": "bodyMedium",
                        "color": {
                          "solid": "onSurfaceVariant"
                        },
                        "alignment": "start"
                      }
                    }
                  ]
                }
              }
            }
          },
          {
            "header": {
              "component": {
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
                    "sides": {
                      "start": "m",
                      "top": "l",
                      "end": "m",
                      "bottom": "xs"
                    }
                  },
                  "appearance": {
                    "plain": {
                      "background": {
                        "solid": "surface"
                      }
                    }
                  },
                  "content": {
                    "plain": "Приложение"
                  },
                  "typography": "titleSmall",
                  "color": {
                    "solid": "primary"
                  },
                  "alignment": "start"
                }
              }
            }
          },
          {
            "item": {
              "component": {
                "Container": {
                  "id": "language",
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
                    "symmetric": {
                      "horizontal": "m",
                      "vertical": "m"
                    }
                  },
                  "appearance": {
                    "plain": {}
                  },
                  "events": [
                    {
                      "click": {
                        "payload": {
                          "action": "language"
                        }
                      }
                    }
                  ],
                  "arrangement": {
                    "horizontal": {
                      "alignment": "center",
                      "distribution": "spaceBetween"
                    }
                  },
                  "children": [
                    {
                      "Text": {
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
                        "content": {
                          "plain": "Язык"
                        },
                        "typography": "bodyLarge",
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
                        "content": {
                          "plain": "Русский"
                        },
                        "typography": "bodyMedium",
                        "color": {
                          "solid": "onSurfaceVariant"
                        },
                        "alignment": "start"
                      }
                    }
                  ]
                }
              }
            }
          },
          {
            "item": {
              "component": {
                "Container": {
                  "id": "theme",
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
                    "symmetric": {
                      "horizontal": "m",
                      "vertical": "m"
                    }
                  },
                  "appearance": {
                    "plain": {}
                  },
                  "events": [
                    {
                      "click": {
                        "payload": {
                          "action": "theme"
                        }
                      }
                    }
                  ],
                  "arrangement": {
                    "horizontal": {
                      "alignment": "center",
                      "distribution": "spaceBetween"
                    }
                  },
                  "children": [
                    {
                      "Text": {
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
                        "content": {
                          "plain": "Тема"
                        },
                        "typography": "bodyLarge",
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
                        "content": {
                          "plain": "Как в системе"
                        },
                        "typography": "bodyMedium",
                        "color": {
                          "solid": "onSurfaceVariant"
                        },
                        "alignment": "start"
                      }
                    }
                  ]
                }
              }
            }
          },
          {
            "item": {
              "component": {
                "Container": {
                  "id": "notifications",
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
                    "symmetric": {
                      "horizontal": "m",
                      "vertical": "m"
                    }
                  },
                  "appearance": {
                    "plain": {}
                  },
                  "events": [
                    {
                      "click": {
                        "payload": {
                          "action": "notifications"
                        }
                      }
                    }
                  ],
                  "arrangement": {
                    "horizontal": {
                      "alignment": "center",
                      "distribution": "spaceBetween"
                    }
                  },
                  "children": [
                    {
                      "Text": {
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
                        "content": {
                          "plain": "Уведомления"
                        },
                        "typography": "bodyLarge",
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
                        "content": {
                          "plain": "Включены"
                        },
                        "typography": "bodyMedium",
                        "color": {
                          "solid": "onSurfaceVariant"
                        },
                        "alignment": "start"
                      }
                    }
                  ]
                }
              }
            }
          }
        ]
      }
    }
  }
}
```

Чат: перевёрнутый список, сообщения от новых к старым, новые прижаты к низу (`position: bottom`), подгрузка истории
по `reachEnd`.

<!-- sample: samples/chat.json -->
```json
{
  "ScrollList": {
    "id": "chat",
    "layout": {
      "width": {
        "sizing": "fill"
      },
      "height": {
        "sizing": {
          "fixed": {
            "custom": {
              "value": 320
            }
          }
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
          "solid": "background"
        }
      }
    },
    "events": [
      {
        "reachEnd": {
          "payload": {
            "action": "loadOlderMessages"
          }
        }
      }
    ],
    "contentPadding": {
      "all": "m"
    },
    "content": {
      "vertical": {
        "gap": "xs",
        "position": "bottom",
        "reversed": true,
        "children": [
          {
            "item": {
              "component": {
                "Container": {
                  "id": "message-4",
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
                    "horizontal": {
                      "alignment": "top",
                      "distribution": {
                        "packed": {
                          "gap": "zero",
                          "position": "end"
                        }
                      }
                    }
                  },
                  "children": [
                    {
                      "Text": {
                        "layout": {
                          "width": {
                            "sizing": "hug",
                            "max": {
                              "custom": {
                                "value": 260
                              }
                            }
                          },
                          "height": {
                            "sizing": "hug"
                          },
                          "position": "flow"
                        },
                        "padding": {
                          "symmetric": {
                            "horizontal": "m",
                            "vertical": "s"
                          }
                        },
                        "appearance": {
                          "plain": {
                            "background": {
                              "solid": "primaryContainer"
                            },
                            "cornerRadius": "l"
                          }
                        },
                        "content": {
                          "plain": "Отлично, тогда до встречи в семь"
                        },
                        "typography": "bodyMedium",
                        "color": {
                          "solid": "onPrimaryContainer"
                        },
                        "alignment": "start"
                      }
                    }
                  ]
                }
              }
            }
          },
          {
            "item": {
              "component": {
                "Container": {
                  "id": "message-3",
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
                    "horizontal": {
                      "alignment": "top",
                      "distribution": {
                        "packed": {
                          "gap": "zero",
                          "position": "start"
                        }
                      }
                    }
                  },
                  "children": [
                    {
                      "Text": {
                        "layout": {
                          "width": {
                            "sizing": "hug",
                            "max": {
                              "custom": {
                                "value": 260
                              }
                            }
                          },
                          "height": {
                            "sizing": "hug"
                          },
                          "position": "flow"
                        },
                        "padding": {
                          "symmetric": {
                            "horizontal": "m",
                            "vertical": "s"
                          }
                        },
                        "appearance": {
                          "plain": {
                            "background": {
                              "solid": "surface2"
                            },
                            "cornerRadius": "l"
                          }
                        },
                        "content": {
                          "plain": "Да, в семь у входа в парк"
                        },
                        "typography": "bodyMedium",
                        "color": {
                          "solid": "onSurface"
                        },
                        "alignment": "start"
                      }
                    }
                  ]
                }
              }
            }
          },
          {
            "item": {
              "component": {
                "Container": {
                  "id": "message-2",
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
                    "horizontal": {
                      "alignment": "top",
                      "distribution": {
                        "packed": {
                          "gap": "zero",
                          "position": "end"
                        }
                      }
                    }
                  },
                  "children": [
                    {
                      "Text": {
                        "layout": {
                          "width": {
                            "sizing": "hug",
                            "max": {
                              "custom": {
                                "value": 260
                              }
                            }
                          },
                          "height": {
                            "sizing": "hug"
                          },
                          "position": "flow"
                        },
                        "padding": {
                          "symmetric": {
                            "horizontal": "m",
                            "vertical": "s"
                          }
                        },
                        "appearance": {
                          "plain": {
                            "background": {
                              "solid": "primaryContainer"
                            },
                            "cornerRadius": "l"
                          }
                        },
                        "content": {
                          "plain": "Встречаемся сегодня?"
                        },
                        "typography": "bodyMedium",
                        "color": {
                          "solid": "onPrimaryContainer"
                        },
                        "alignment": "start"
                      }
                    }
                  ]
                }
              }
            }
          },
          {
            "header": {
              "component": {
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
                    "symmetric": {
                      "horizontal": "zero",
                      "vertical": "xs"
                    }
                  },
                  "appearance": {
                    "plain": {}
                  },
                  "content": {
                    "plain": "Сегодня"
                  },
                  "typography": "labelSmall",
                  "color": {
                    "solid": "onSurfaceMuted"
                  },
                  "alignment": "center"
                }
              }
            }
          },
          {
            "item": {
              "component": {
                "Container": {
                  "id": "message-1",
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
                    "horizontal": {
                      "alignment": "top",
                      "distribution": {
                        "packed": {
                          "gap": "zero",
                          "position": "start"
                        }
                      }
                    }
                  },
                  "children": [
                    {
                      "Text": {
                        "layout": {
                          "width": {
                            "sizing": "hug",
                            "max": {
                              "custom": {
                                "value": 260
                              }
                            }
                          },
                          "height": {
                            "sizing": "hug"
                          },
                          "position": "flow"
                        },
                        "padding": {
                          "symmetric": {
                            "horizontal": "m",
                            "vertical": "s"
                          }
                        },
                        "appearance": {
                          "plain": {
                            "background": {
                              "solid": "surface2"
                            },
                            "cornerRadius": "l"
                          }
                        },
                        "content": {
                          "plain": "Привет! Как дела?"
                        },
                        "typography": "bodyMedium",
                        "color": {
                          "solid": "onSurface"
                        },
                        "alignment": "start"
                      }
                    }
                  ]
                }
              }
            }
          },
          {
            "header": {
              "component": {
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
                    "symmetric": {
                      "horizontal": "zero",
                      "vertical": "xs"
                    }
                  },
                  "appearance": {
                    "plain": {}
                  },
                  "content": {
                    "plain": "Вчера"
                  },
                  "typography": "labelSmall",
                  "color": {
                    "solid": "onSurfaceMuted"
                  },
                  "alignment": "center"
                }
              }
            }
          }
        ]
      }
    }
  }
}
```

Карусель: горизонтальный список, карточки по 0,4 окна, докатывание к карточке.

<!-- sample: samples/carousel.json -->
```json
{
  "ScrollList": {
    "id": "recommendations",
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
      "plain": {
        "background": {
          "solid": "surface"
        }
      }
    },
    "events": [
      {
        "reachEnd": {
          "payload": {
            "action": "loadMoreProducts"
          }
        }
      }
    ],
    "contentPadding": {
      "symmetric": {
        "horizontal": "m",
        "vertical": "zero"
      }
    },
    "content": {
      "horizontal": {
        "gap": "s",
        "snapToItem": true,
        "children": [
          {
            "item": {
              "viewportFraction": 0.4,
              "component": {
                "Container": {
                  "id": "product-1",
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
                  "events": [
                    {
                      "click": {
                        "payload": {
                          "action": "openProduct",
                          "productId": "product-1"
                        }
                      }
                    }
                  ],
                  "arrangement": {
                    "vertical": {
                      "alignment": "start",
                      "distribution": {
                        "packed": {
                          "gap": "xs",
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
                          "plain": "Кружка керамическая"
                        },
                        "typography": "bodyMedium",
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
                    }
                  ]
                }
              }
            }
          },
          {
            "item": {
              "viewportFraction": 0.4,
              "component": {
                "Container": {
                  "id": "product-2",
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
                  "events": [
                    {
                      "click": {
                        "payload": {
                          "action": "openProduct",
                          "productId": "product-2"
                        }
                      }
                    }
                  ],
                  "arrangement": {
                    "vertical": {
                      "alignment": "start",
                      "distribution": {
                        "packed": {
                          "gap": "xs",
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
                          "plain": "Плед шерстяной"
                        },
                        "typography": "bodyMedium",
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
                          "plain": "3 200 ₽"
                        },
                        "typography": "labelLarge",
                        "color": {
                          "solid": "primary"
                        },
                        "alignment": "start"
                      }
                    }
                  ]
                }
              }
            }
          },
          {
            "item": {
              "viewportFraction": 0.4,
              "component": {
                "Container": {
                  "id": "product-3",
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
                  "events": [
                    {
                      "click": {
                        "payload": {
                          "action": "openProduct",
                          "productId": "product-3"
                        }
                      }
                    }
                  ],
                  "arrangement": {
                    "vertical": {
                      "alignment": "start",
                      "distribution": {
                        "packed": {
                          "gap": "xs",
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
                          "plain": "Подушка декоративная"
                        },
                        "typography": "bodyMedium",
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
                          "plain": "990 ₽"
                        },
                        "typography": "labelLarge",
                        "color": {
                          "solid": "primary"
                        },
                        "alignment": "start"
                      }
                    }
                  ]
                }
              }
            }
          },
          {
            "item": {
              "viewportFraction": 0.4,
              "component": {
                "Container": {
                  "id": "product-4",
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
                  "events": [
                    {
                      "click": {
                        "payload": {
                          "action": "openProduct",
                          "productId": "product-4"
                        }
                      }
                    }
                  ],
                  "arrangement": {
                    "vertical": {
                      "alignment": "start",
                      "distribution": {
                        "packed": {
                          "gap": "xs",
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
                          "plain": "Лампа настольная"
                        },
                        "typography": "bodyMedium",
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
                          "plain": "2 750 ₽"
                        },
                        "typography": "labelLarge",
                        "color": {
                          "solid": "primary"
                        },
                        "alignment": "start"
                      }
                    }
                  ]
                }
              }
            }
          }
        ]
      }
    }
  }
}
```

Экран поиска: вертикальная сетка в 4 колонки — заголовок, плитки (последняя строка неполная), заголовок, строки на
всю ширину (`span: full`), подгрузка по `reachEnd`.

<!-- sample: samples/search.json -->
```json
{
  "ScrollList": {
    "id": "search",
    "layout": {
      "width": {
        "sizing": "fill"
      },
      "height": {
        "sizing": {
          "fixed": {
            "custom": {
              "value": 480
            }
          }
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
          "solid": "surface"
        }
      }
    },
    "events": [
      {
        "reachEnd": {
          "payload": {
            "action": "loadMoreResults"
          }
        }
      }
    ],
    "contentPadding": {
      "symmetric": {
        "horizontal": "m",
        "vertical": "s"
      }
    },
    "content": {
      "verticalGrid": {
        "columns": {
          "fixed": 4
        },
        "rowGap": "s",
        "columnGap": "s",
        "children": [
          {
            "header": {
              "component": {
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
                    "sides": {
                      "start": "zero",
                      "top": "s",
                      "end": "zero",
                      "bottom": "xs"
                    }
                  },
                  "appearance": {
                    "plain": {
                      "background": {
                        "solid": "surface"
                      }
                    }
                  },
                  "content": {
                    "plain": "Категории"
                  },
                  "typography": "titleSmall",
                  "color": {
                    "solid": "primary"
                  },
                  "alignment": "start"
                }
              }
            }
          },
          {
            "item": {
              "component": {
                "Container": {
                  "id": "category-1",
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
                    "symmetric": {
                      "horizontal": "xs",
                      "vertical": "m"
                    }
                  },
                  "appearance": {
                    "plain": {
                      "background": {
                        "solid": "surface2"
                      },
                      "cornerRadius": "m"
                    }
                  },
                  "events": [
                    {
                      "click": {
                        "payload": {
                          "action": "openCategory",
                          "categoryId": "category-1"
                        }
                      }
                    }
                  ],
                  "arrangement": {
                    "vertical": {
                      "alignment": "center",
                      "distribution": {
                        "packed": {
                          "gap": "zero",
                          "position": "top"
                        }
                      }
                    }
                  },
                  "children": [
                    {
                      "Text": {
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
                        "content": {
                          "plain": "А"
                        },
                        "typography": "titleMedium",
                        "color": {
                          "solid": "onSurface"
                        },
                        "alignment": "center"
                      }
                    }
                  ]
                }
              }
            }
          },
          {
            "item": {
              "component": {
                "Container": {
                  "id": "category-2",
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
                    "symmetric": {
                      "horizontal": "xs",
                      "vertical": "m"
                    }
                  },
                  "appearance": {
                    "plain": {
                      "background": {
                        "solid": "surface2"
                      },
                      "cornerRadius": "m"
                    }
                  },
                  "events": [
                    {
                      "click": {
                        "payload": {
                          "action": "openCategory",
                          "categoryId": "category-2"
                        }
                      }
                    }
                  ],
                  "arrangement": {
                    "vertical": {
                      "alignment": "center",
                      "distribution": {
                        "packed": {
                          "gap": "zero",
                          "position": "top"
                        }
                      }
                    }
                  },
                  "children": [
                    {
                      "Text": {
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
                        "content": {
                          "plain": "Б"
                        },
                        "typography": "titleMedium",
                        "color": {
                          "solid": "onSurface"
                        },
                        "alignment": "center"
                      }
                    }
                  ]
                }
              }
            }
          },
          {
            "item": {
              "component": {
                "Container": {
                  "id": "category-3",
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
                    "symmetric": {
                      "horizontal": "xs",
                      "vertical": "m"
                    }
                  },
                  "appearance": {
                    "plain": {
                      "background": {
                        "solid": "surface2"
                      },
                      "cornerRadius": "m"
                    }
                  },
                  "events": [
                    {
                      "click": {
                        "payload": {
                          "action": "openCategory",
                          "categoryId": "category-3"
                        }
                      }
                    }
                  ],
                  "arrangement": {
                    "vertical": {
                      "alignment": "center",
                      "distribution": {
                        "packed": {
                          "gap": "zero",
                          "position": "top"
                        }
                      }
                    }
                  },
                  "children": [
                    {
                      "Text": {
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
                        "content": {
                          "plain": "В"
                        },
                        "typography": "titleMedium",
                        "color": {
                          "solid": "onSurface"
                        },
                        "alignment": "center"
                      }
                    }
                  ]
                }
              }
            }
          },
          {
            "item": {
              "component": {
                "Container": {
                  "id": "category-4",
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
                    "symmetric": {
                      "horizontal": "xs",
                      "vertical": "m"
                    }
                  },
                  "appearance": {
                    "plain": {
                      "background": {
                        "solid": "surface2"
                      },
                      "cornerRadius": "m"
                    }
                  },
                  "events": [
                    {
                      "click": {
                        "payload": {
                          "action": "openCategory",
                          "categoryId": "category-4"
                        }
                      }
                    }
                  ],
                  "arrangement": {
                    "vertical": {
                      "alignment": "center",
                      "distribution": {
                        "packed": {
                          "gap": "zero",
                          "position": "top"
                        }
                      }
                    }
                  },
                  "children": [
                    {
                      "Text": {
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
                        "content": {
                          "plain": "Г"
                        },
                        "typography": "titleMedium",
                        "color": {
                          "solid": "onSurface"
                        },
                        "alignment": "center"
                      }
                    }
                  ]
                }
              }
            }
          },
          {
            "item": {
              "component": {
                "Container": {
                  "id": "category-5",
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
                    "symmetric": {
                      "horizontal": "xs",
                      "vertical": "m"
                    }
                  },
                  "appearance": {
                    "plain": {
                      "background": {
                        "solid": "surface2"
                      },
                      "cornerRadius": "m"
                    }
                  },
                  "events": [
                    {
                      "click": {
                        "payload": {
                          "action": "openCategory",
                          "categoryId": "category-5"
                        }
                      }
                    }
                  ],
                  "arrangement": {
                    "vertical": {
                      "alignment": "center",
                      "distribution": {
                        "packed": {
                          "gap": "zero",
                          "position": "top"
                        }
                      }
                    }
                  },
                  "children": [
                    {
                      "Text": {
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
                        "content": {
                          "plain": "Д"
                        },
                        "typography": "titleMedium",
                        "color": {
                          "solid": "onSurface"
                        },
                        "alignment": "center"
                      }
                    }
                  ]
                }
              }
            }
          },
          {
            "item": {
              "component": {
                "Container": {
                  "id": "category-6",
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
                    "symmetric": {
                      "horizontal": "xs",
                      "vertical": "m"
                    }
                  },
                  "appearance": {
                    "plain": {
                      "background": {
                        "solid": "surface2"
                      },
                      "cornerRadius": "m"
                    }
                  },
                  "events": [
                    {
                      "click": {
                        "payload": {
                          "action": "openCategory",
                          "categoryId": "category-6"
                        }
                      }
                    }
                  ],
                  "arrangement": {
                    "vertical": {
                      "alignment": "center",
                      "distribution": {
                        "packed": {
                          "gap": "zero",
                          "position": "top"
                        }
                      }
                    }
                  },
                  "children": [
                    {
                      "Text": {
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
                        "content": {
                          "plain": "Е"
                        },
                        "typography": "titleMedium",
                        "color": {
                          "solid": "onSurface"
                        },
                        "alignment": "center"
                      }
                    }
                  ]
                }
              }
            }
          },
          {
            "header": {
              "component": {
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
                    "sides": {
                      "start": "zero",
                      "top": "s",
                      "end": "zero",
                      "bottom": "xs"
                    }
                  },
                  "appearance": {
                    "plain": {
                      "background": {
                        "solid": "surface"
                      }
                    }
                  },
                  "content": {
                    "plain": "Результаты"
                  },
                  "typography": "titleSmall",
                  "color": {
                    "solid": "primary"
                  },
                  "alignment": "start"
                }
              }
            }
          },
          {
            "item": {
              "span": "full",
              "component": {
                "Container": {
                  "id": "result-1",
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
                    "symmetric": {
                      "horizontal": "zero",
                      "vertical": "s"
                    }
                  },
                  "appearance": {
                    "plain": {}
                  },
                  "events": [
                    {
                      "click": {
                        "payload": {
                          "action": "openResult",
                          "resultId": "result-1"
                        }
                      }
                    }
                  ],
                  "arrangement": {
                    "vertical": {
                      "alignment": "start",
                      "distribution": {
                        "packed": {
                          "gap": "zero",
                          "position": "top"
                        }
                      }
                    }
                  },
                  "children": [
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
                          "plain": "Результат 1"
                        },
                        "typography": "bodyLarge",
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
                          "plain": "Краткое описание результата"
                        },
                        "typography": "bodyMedium",
                        "color": {
                          "solid": "onSurfaceMuted"
                        },
                        "alignment": "start"
                      }
                    }
                  ]
                }
              }
            }
          },
          {
            "item": {
              "span": "full",
              "component": {
                "Container": {
                  "id": "result-2",
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
                    "symmetric": {
                      "horizontal": "zero",
                      "vertical": "s"
                    }
                  },
                  "appearance": {
                    "plain": {}
                  },
                  "events": [
                    {
                      "click": {
                        "payload": {
                          "action": "openResult",
                          "resultId": "result-2"
                        }
                      }
                    }
                  ],
                  "arrangement": {
                    "vertical": {
                      "alignment": "start",
                      "distribution": {
                        "packed": {
                          "gap": "zero",
                          "position": "top"
                        }
                      }
                    }
                  },
                  "children": [
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
                          "plain": "Результат 2"
                        },
                        "typography": "bodyLarge",
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
                          "plain": "Краткое описание результата"
                        },
                        "typography": "bodyMedium",
                        "color": {
                          "solid": "onSurfaceMuted"
                        },
                        "alignment": "start"
                      }
                    }
                  ]
                }
              }
            }
          },
          {
            "item": {
              "span": "full",
              "component": {
                "Container": {
                  "id": "result-3",
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
                    "symmetric": {
                      "horizontal": "zero",
                      "vertical": "s"
                    }
                  },
                  "appearance": {
                    "plain": {}
                  },
                  "events": [
                    {
                      "click": {
                        "payload": {
                          "action": "openResult",
                          "resultId": "result-3"
                        }
                      }
                    }
                  ],
                  "arrangement": {
                    "vertical": {
                      "alignment": "start",
                      "distribution": {
                        "packed": {
                          "gap": "zero",
                          "position": "top"
                        }
                      }
                    }
                  },
                  "children": [
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
                          "plain": "Результат 3"
                        },
                        "typography": "bodyLarge",
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
                          "plain": "Краткое описание результата"
                        },
                        "typography": "bodyMedium",
                        "color": {
                          "solid": "onSurfaceMuted"
                        },
                        "alignment": "start"
                      }
                    }
                  ]
                }
              }
            }
          }
        ]
      }
    }
  }
}
```

Галерея: адаптивные колонки не уже 96, первый элемент — в две колонки.

<!-- sample: samples/gallery.json -->
```json
{
  "ScrollList": {
    "id": "gallery",
    "layout": {
      "width": {
        "sizing": "fill"
      },
      "height": {
        "sizing": {
          "fixed": {
            "custom": {
              "value": 320
            }
          }
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
          "solid": "surface"
        }
      }
    },
    "contentPadding": {
      "all": "s"
    },
    "content": {
      "verticalGrid": {
        "columns": {
          "adaptive": {
            "custom": {
              "value": 96
            }
          }
        },
        "rowGap": "xs",
        "columnGap": "xs",
        "children": [
          {
            "item": {
              "span": {
                "count": 2
              },
              "component": {
                "Image": {
                  "id": "photo-1",
                  "layout": {
                    "width": {
                      "sizing": "fill"
                    },
                    "height": {
                      "sizing": {
                        "fixed": {
                          "custom": {
                            "value": 96
                          }
                        }
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
              }
            }
          },
          {
            "item": {
              "component": {
                "Image": {
                  "id": "photo-2",
                  "layout": {
                    "width": {
                      "sizing": "fill"
                    },
                    "height": {
                      "sizing": {
                        "fixed": {
                          "custom": {
                            "value": 96
                          }
                        }
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
              }
            }
          },
          {
            "item": {
              "component": {
                "Image": {
                  "id": "photo-3",
                  "layout": {
                    "width": {
                      "sizing": "fill"
                    },
                    "height": {
                      "sizing": {
                        "fixed": {
                          "custom": {
                            "value": 96
                          }
                        }
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
              }
            }
          },
          {
            "item": {
              "component": {
                "Image": {
                  "id": "photo-4",
                  "layout": {
                    "width": {
                      "sizing": "fill"
                    },
                    "height": {
                      "sizing": {
                        "fixed": {
                          "custom": {
                            "value": 96
                          }
                        }
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
              }
            }
          },
          {
            "item": {
              "component": {
                "Image": {
                  "id": "photo-5",
                  "layout": {
                    "width": {
                      "sizing": "fill"
                    },
                    "height": {
                      "sizing": {
                        "fixed": {
                          "custom": {
                            "value": 96
                          }
                        }
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
              }
            }
          },
          {
            "item": {
              "component": {
                "Image": {
                  "id": "photo-6",
                  "layout": {
                    "width": {
                      "sizing": "fill"
                    },
                    "height": {
                      "sizing": {
                        "fixed": {
                          "custom": {
                            "value": 96
                          }
                        }
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
              }
            }
          },
          {
            "item": {
              "component": {
                "Image": {
                  "id": "photo-7",
                  "layout": {
                    "width": {
                      "sizing": "fill"
                    },
                    "height": {
                      "sizing": {
                        "fixed": {
                          "custom": {
                            "value": 96
                          }
                        }
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
              }
            }
          },
          {
            "item": {
              "component": {
                "Image": {
                  "id": "photo-8",
                  "layout": {
                    "width": {
                      "sizing": "fill"
                    },
                    "height": {
                      "sizing": {
                        "fixed": {
                          "custom": {
                            "value": 96
                          }
                        }
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
              }
            }
          }
        ]
      }
    }
  }
}
```

Полка в ленте: горизонтальная сетка в 3 строки, столбцы заполняются сверху вниз, последний неполный.

<!-- sample: samples/shelf.json -->
```json
{
  "ScrollList": {
    "id": "shelf",
    "layout": {
      "width": {
        "sizing": "fill"
      },
      "height": {
        "sizing": {
          "fixed": {
            "custom": {
              "value": 192
            }
          }
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
          "solid": "surface"
        }
      }
    },
    "events": [
      {
        "reachEnd": {
          "payload": {
            "action": "loadMoreTracks"
          }
        }
      }
    ],
    "contentPadding": {
      "symmetric": {
        "horizontal": "m",
        "vertical": "zero"
      }
    },
    "content": {
      "horizontalGrid": {
        "rows": {
          "fixed": 3
        },
        "rowGap": "xs",
        "columnGap": "m",
        "snapToItem": true,
        "children": [
          {
            "item": {
              "component": {
                "Container": {
                  "id": "track-1",
                  "layout": {
                    "width": {
                      "sizing": {
                        "fixed": {
                          "custom": {
                            "value": 240
                          }
                        }
                      }
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
                  "events": [
                    {
                      "click": {
                        "payload": {
                          "action": "openTrack",
                          "trackId": "track-1"
                        }
                      }
                    }
                  ],
                  "arrangement": {
                    "horizontal": {
                      "alignment": "center",
                      "distribution": {
                        "packed": {
                          "gap": "s",
                          "position": "start"
                        }
                      }
                    }
                  },
                  "children": [
                    {
                      "Image": {
                        "layout": {
                          "width": {
                            "sizing": {
                              "fixed": {
                                "custom": {
                                  "value": 48
                                }
                              }
                            }
                          },
                          "height": {
                            "sizing": {
                              "fixed": {
                                "custom": {
                                  "value": 48
                                }
                              }
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
                          "vertical": {
                            "alignment": "start",
                            "distribution": {
                              "packed": {
                                "gap": "zero",
                                "position": "top"
                              }
                            }
                          }
                        },
                        "children": [
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
                                "plain": "Запись 1"
                              },
                              "typography": "bodyLarge",
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
                                "plain": "Подпись"
                              },
                              "typography": "bodyMedium",
                              "color": {
                                "solid": "onSurfaceMuted"
                              },
                              "alignment": "start"
                            }
                          }
                        ]
                      }
                    }
                  ]
                }
              }
            }
          },
          {
            "item": {
              "component": {
                "Container": {
                  "id": "track-2",
                  "layout": {
                    "width": {
                      "sizing": {
                        "fixed": {
                          "custom": {
                            "value": 240
                          }
                        }
                      }
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
                  "events": [
                    {
                      "click": {
                        "payload": {
                          "action": "openTrack",
                          "trackId": "track-2"
                        }
                      }
                    }
                  ],
                  "arrangement": {
                    "horizontal": {
                      "alignment": "center",
                      "distribution": {
                        "packed": {
                          "gap": "s",
                          "position": "start"
                        }
                      }
                    }
                  },
                  "children": [
                    {
                      "Image": {
                        "layout": {
                          "width": {
                            "sizing": {
                              "fixed": {
                                "custom": {
                                  "value": 48
                                }
                              }
                            }
                          },
                          "height": {
                            "sizing": {
                              "fixed": {
                                "custom": {
                                  "value": 48
                                }
                              }
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
                          "vertical": {
                            "alignment": "start",
                            "distribution": {
                              "packed": {
                                "gap": "zero",
                                "position": "top"
                              }
                            }
                          }
                        },
                        "children": [
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
                                "plain": "Запись 2"
                              },
                              "typography": "bodyLarge",
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
                                "plain": "Подпись"
                              },
                              "typography": "bodyMedium",
                              "color": {
                                "solid": "onSurfaceMuted"
                              },
                              "alignment": "start"
                            }
                          }
                        ]
                      }
                    }
                  ]
                }
              }
            }
          },
          {
            "item": {
              "component": {
                "Container": {
                  "id": "track-3",
                  "layout": {
                    "width": {
                      "sizing": {
                        "fixed": {
                          "custom": {
                            "value": 240
                          }
                        }
                      }
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
                  "events": [
                    {
                      "click": {
                        "payload": {
                          "action": "openTrack",
                          "trackId": "track-3"
                        }
                      }
                    }
                  ],
                  "arrangement": {
                    "horizontal": {
                      "alignment": "center",
                      "distribution": {
                        "packed": {
                          "gap": "s",
                          "position": "start"
                        }
                      }
                    }
                  },
                  "children": [
                    {
                      "Image": {
                        "layout": {
                          "width": {
                            "sizing": {
                              "fixed": {
                                "custom": {
                                  "value": 48
                                }
                              }
                            }
                          },
                          "height": {
                            "sizing": {
                              "fixed": {
                                "custom": {
                                  "value": 48
                                }
                              }
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
                          "vertical": {
                            "alignment": "start",
                            "distribution": {
                              "packed": {
                                "gap": "zero",
                                "position": "top"
                              }
                            }
                          }
                        },
                        "children": [
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
                                "plain": "Запись 3"
                              },
                              "typography": "bodyLarge",
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
                                "plain": "Подпись"
                              },
                              "typography": "bodyMedium",
                              "color": {
                                "solid": "onSurfaceMuted"
                              },
                              "alignment": "start"
                            }
                          }
                        ]
                      }
                    }
                  ]
                }
              }
            }
          },
          {
            "item": {
              "component": {
                "Container": {
                  "id": "track-4",
                  "layout": {
                    "width": {
                      "sizing": {
                        "fixed": {
                          "custom": {
                            "value": 240
                          }
                        }
                      }
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
                  "events": [
                    {
                      "click": {
                        "payload": {
                          "action": "openTrack",
                          "trackId": "track-4"
                        }
                      }
                    }
                  ],
                  "arrangement": {
                    "horizontal": {
                      "alignment": "center",
                      "distribution": {
                        "packed": {
                          "gap": "s",
                          "position": "start"
                        }
                      }
                    }
                  },
                  "children": [
                    {
                      "Image": {
                        "layout": {
                          "width": {
                            "sizing": {
                              "fixed": {
                                "custom": {
                                  "value": 48
                                }
                              }
                            }
                          },
                          "height": {
                            "sizing": {
                              "fixed": {
                                "custom": {
                                  "value": 48
                                }
                              }
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
                          "vertical": {
                            "alignment": "start",
                            "distribution": {
                              "packed": {
                                "gap": "zero",
                                "position": "top"
                              }
                            }
                          }
                        },
                        "children": [
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
                                "plain": "Запись 4"
                              },
                              "typography": "bodyLarge",
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
                                "plain": "Подпись"
                              },
                              "typography": "bodyMedium",
                              "color": {
                                "solid": "onSurfaceMuted"
                              },
                              "alignment": "start"
                            }
                          }
                        ]
                      }
                    }
                  ]
                }
              }
            }
          },
          {
            "item": {
              "component": {
                "Container": {
                  "id": "track-5",
                  "layout": {
                    "width": {
                      "sizing": {
                        "fixed": {
                          "custom": {
                            "value": 240
                          }
                        }
                      }
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
                  "events": [
                    {
                      "click": {
                        "payload": {
                          "action": "openTrack",
                          "trackId": "track-5"
                        }
                      }
                    }
                  ],
                  "arrangement": {
                    "horizontal": {
                      "alignment": "center",
                      "distribution": {
                        "packed": {
                          "gap": "s",
                          "position": "start"
                        }
                      }
                    }
                  },
                  "children": [
                    {
                      "Image": {
                        "layout": {
                          "width": {
                            "sizing": {
                              "fixed": {
                                "custom": {
                                  "value": 48
                                }
                              }
                            }
                          },
                          "height": {
                            "sizing": {
                              "fixed": {
                                "custom": {
                                  "value": 48
                                }
                              }
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
                          "vertical": {
                            "alignment": "start",
                            "distribution": {
                              "packed": {
                                "gap": "zero",
                                "position": "top"
                              }
                            }
                          }
                        },
                        "children": [
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
                                "plain": "Запись 5"
                              },
                              "typography": "bodyLarge",
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
                                "plain": "Подпись"
                              },
                              "typography": "bodyMedium",
                              "color": {
                                "solid": "onSurfaceMuted"
                              },
                              "alignment": "start"
                            }
                          }
                        ]
                      }
                    }
                  ]
                }
              }
            }
          },
          {
            "item": {
              "component": {
                "Container": {
                  "id": "track-6",
                  "layout": {
                    "width": {
                      "sizing": {
                        "fixed": {
                          "custom": {
                            "value": 240
                          }
                        }
                      }
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
                  "events": [
                    {
                      "click": {
                        "payload": {
                          "action": "openTrack",
                          "trackId": "track-6"
                        }
                      }
                    }
                  ],
                  "arrangement": {
                    "horizontal": {
                      "alignment": "center",
                      "distribution": {
                        "packed": {
                          "gap": "s",
                          "position": "start"
                        }
                      }
                    }
                  },
                  "children": [
                    {
                      "Image": {
                        "layout": {
                          "width": {
                            "sizing": {
                              "fixed": {
                                "custom": {
                                  "value": 48
                                }
                              }
                            }
                          },
                          "height": {
                            "sizing": {
                              "fixed": {
                                "custom": {
                                  "value": 48
                                }
                              }
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
                          "vertical": {
                            "alignment": "start",
                            "distribution": {
                              "packed": {
                                "gap": "zero",
                                "position": "top"
                              }
                            }
                          }
                        },
                        "children": [
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
                                "plain": "Запись 6"
                              },
                              "typography": "bodyLarge",
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
                                "plain": "Подпись"
                              },
                              "typography": "bodyMedium",
                              "color": {
                                "solid": "onSurfaceMuted"
                              },
                              "alignment": "start"
                            }
                          }
                        ]
                      }
                    }
                  ]
                }
              }
            }
          },
          {
            "item": {
              "component": {
                "Container": {
                  "id": "track-7",
                  "layout": {
                    "width": {
                      "sizing": {
                        "fixed": {
                          "custom": {
                            "value": 240
                          }
                        }
                      }
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
                  "events": [
                    {
                      "click": {
                        "payload": {
                          "action": "openTrack",
                          "trackId": "track-7"
                        }
                      }
                    }
                  ],
                  "arrangement": {
                    "horizontal": {
                      "alignment": "center",
                      "distribution": {
                        "packed": {
                          "gap": "s",
                          "position": "start"
                        }
                      }
                    }
                  },
                  "children": [
                    {
                      "Image": {
                        "layout": {
                          "width": {
                            "sizing": {
                              "fixed": {
                                "custom": {
                                  "value": 48
                                }
                              }
                            }
                          },
                          "height": {
                            "sizing": {
                              "fixed": {
                                "custom": {
                                  "value": 48
                                }
                              }
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
                          "vertical": {
                            "alignment": "start",
                            "distribution": {
                              "packed": {
                                "gap": "zero",
                                "position": "top"
                              }
                            }
                          }
                        },
                        "children": [
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
                                "plain": "Запись 7"
                              },
                              "typography": "bodyLarge",
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
                                "plain": "Подпись"
                              },
                              "typography": "bodyMedium",
                              "color": {
                                "solid": "onSurfaceMuted"
                              },
                              "alignment": "start"
                            }
                          }
                        ]
                      }
                    }
                  ]
                }
              }
            }
          }
        ]
      }
    }
  }
}
```

Сгенерированные образцы: [минимальная модель](samples/min.json), [полная модель](samples/max.json).

Ожидаемая модель на Kotlin:

```kotlin
data class ScrollList(
    // поля Component
    val contentPadding: Padding = Padding.All(Spacing.Zero),
    val content: ScrollListContent,
) : Component

sealed interface ScrollListContent {
    data class Vertical(
        val alignment: ContainerHorizontalAlignment = ContainerHorizontalAlignment.Start,
        val gap: Spacing = Spacing.Zero,
        val position: ContainerVerticalAlignment = ContainerVerticalAlignment.Top,
        val reversed: Boolean = false,
        val snapToItem: Boolean = false,
        val children: List<ScrollListElement>? = null,
    ) : ScrollListContent

    data class Horizontal(
        val alignment: ContainerVerticalAlignment = ContainerVerticalAlignment.Top,
        val gap: Spacing = Spacing.Zero,
        val position: ContainerHorizontalAlignment = ContainerHorizontalAlignment.Start,
        val reversed: Boolean = false,
        val snapToItem: Boolean = false,
        val children: List<ScrollListElement>? = null,
    ) : ScrollListContent

    data class VerticalGrid(
        val columns: ScrollListGridTracks,
        val rowGap: Spacing = Spacing.Zero,
        val columnGap: Spacing = Spacing.Zero,
        val children: List<ScrollListGridElement>? = null,
    ) : ScrollListContent

    data class HorizontalGrid(
        val rows: ScrollListGridTracks,
        val rowGap: Spacing = Spacing.Zero,
        val columnGap: Spacing = Spacing.Zero,
        val snapToItem: Boolean = false,
        val children: List<ScrollListGridElement>? = null,
    ) : ScrollListContent
}

sealed interface ScrollListElement {
    data class Header(val component: Component) : ScrollListElement
    data class Item(val component: Component, val viewportFraction: Float? = null) : ScrollListElement
}

sealed interface ScrollListGridTracks {
    data class Fixed(val value: Int) : ScrollListGridTracks
    data class Adaptive(val value: Size) : ScrollListGridTracks
}

sealed interface ScrollListGridElement {
    data class Header(val component: Component) : ScrollListGridElement
    data class Item(
        val component: Component,
        val span: ScrollListGridSpan = ScrollListGridSpan.Count(1),
    ) : ScrollListGridElement
}

sealed interface ScrollListGridSpan {
    data class Count(val value: Int) : ScrollListGridSpan
    data object Full : ScrollListGridSpan
}
```

## Рекомендации по использованию

- Один `ScrollList` подходит и для содержимого макета, и для списка из данных: приложение или сервер разворачивает
  данные в элементы `item`.
- Задавайте `id` компонентам элементов из данных: это ключ элемента, он сохраняет состояние и анимации при обновлении
  списка. Без `id` ключ — позиция.
- Отступ, до которого прокручиваются элементы, задавайте `contentPadding`, а не `padding`: `padding` обрезает
  элементы у своего края.
- Для чата используйте `reversed` и `position: bottom`: новые сообщения — в начале `children`, история дописывается
  в конец по `reachEnd`.
- Для подгрузки ленты задавайте `reachEnd` у списка: событие сработает снова, когда придёт новая порция и конец
  сдвинется.
- Заголовки разделов задавайте элементами `header`: они прилипают к началу окна.
- Для карусели «две с половиной карточки» задайте элементам `viewportFraction` и включите `snapToItem`.
- Для плиток в несколько колонок используйте `verticalGrid`; строки на всю ширину в той же ленте — элементы с
  `span: full`, заголовки разделов — `header`. Один список с подгрузкой лучше нескольких.
- Если число колонок должно зависеть от ширины экрана (телефон, планшет), задайте `columns: adaptive` с минимальной
  шириной колонки — обычно `custom`: шкала `Size` рассчитана на размеры элементов.
- Короткую горизонтальную сетку в 2–3 строки внутри вертикальной ленты задавайте `horizontalGrid` с фиксированной
  высотой списка.
- Не вкладывайте список в список с той же осью прокрутки: внутренний не прокручивается. Карусель в вертикальной ленте
  — норма.

## Ограничения и открытые вопросы

- `viewportFraction` — в пределах `0 < значение ≤ 1`; формат это ограничение не выражает, валидатор его не
  проверяет.
- Не проверено: поведение прилипающего `header` в перевёрнутом списке на Compose (`stickyHeader` и `reverseLayout`) и
  SwiftUI (`LazyVStack(pinnedViews:)`) — поведение во время работы, компиляцией не проверяется; проверяется на этапе
  5.
- Не проверено: способ `reversed` в SwiftUI — прямого аналога `reverseLayout` нет; проверяется на этапе 5.
- Не проверено: учитывают ли `Modifier.fillParentMaxWidth` в Compose и `.containerRelativeFrame` в SwiftUI отступ
  `contentPadding` при расчёте `viewportFraction`; проверяется на этапе 5.
- `fixed` у `columns` / `rows` и `count` у `span` — не меньше 1; формат это ограничение не выражает, валидатор его не
  проверяет.
- Не проверено: прилипающий `header` в сетке Compose (`stickyHeader` у `LazyGridScope`) и SwiftUI (`Section` в
  `LazyVStack(pinnedViews:)` со строками из ячеек); проверяется на этапе 5.
- Не проверено: учитывает ли `.containerRelativeFrame` в SwiftUI отступ `contentPadding` (`.contentMargins`) при
  расчёте ширины ячейки сетки; проверяется на этапе 5.
- Не проверено: вертикальная сетка внутри горизонтального списка — в Compose `LazyVerticalGrid` с неограниченной
  шириной, вероятно, падает; правило вложенной прокрутки (платформа не падает) распространяется и на этот случай,
  способ — на этапе 5.
- Пейджеры (`verticalPager`, `horizontalPager`) — #222.
