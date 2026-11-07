# ScrollList

## Назначение

`ScrollList` — прокручиваемый список: окно, в котором содержимое прокручивается вдоль одной оси. Длинный экран
настроек или формы, лента из данных с подгрузкой (заказы, уведомления, поиск), чат, карусель карточек.

Одна модель и для содержимого, заданного в макете, и для списка из данных: модель — дерево, данные в дерево
разворачивает приложение или сервер. Контракту и Figma всё равно, статический список или из данных; ленивую отрисовку
выбирает платформа.

Список наследует базовую модель `Component`: размеры и положение в родителе (`layout`), отступ окна (`padding`),
оформление (`appearance`, контекст `plain`) и события (`events`, в том числе `reachEnd`). Собственные поля — отступ у
содержимого (`contentPadding`) и содержимое (`content`): вариант задаёт раскладку и элементы. Сетки и пейджеры
добавятся вариантами `content` (#221, #222).

## Состав

| Файл                                    | Роль                                                                            |
|-----------------------------------------|---------------------------------------------------------------------------------|
| `ScrollList.json`                       | Публичный контракт: отступ у содержимого и содержимое.                          |
| `definitions/ScrollListContent.json`    | Содержимое: `vertical` или `horizontal`.                                        |
| `definitions/ScrollListVertical.json`   | Вертикальный список: выравнивание, промежуток, положение, элементы.             |
| `definitions/ScrollListHorizontal.json` | Горизонтальный список: выравнивание, промежуток, положение, элементы.           |
| `definitions/ScrollListElement.json`    | Элемент списка: `header` или `item`.                                            |
| `definitions/ScrollListHeader.json`     | Заголовок раздела: компонент, прилипающий к началу окна.                        |
| `definitions/ScrollListItem.json`       | Обычный элемент: компонент и доля окна `viewportFraction`.                      |
| `docs.json`                             | Манифест документации Figma: секции страницы и примеры.                         |
| `changelog.json`                        | Журнал изменений API.                                                           |
| `samples/`                              | Образцы: `min.json`, `max.json`, `chat.json`, `settings.json`, `carousel.json`. |

## Варианты

```
ScrollList : Component                         appearanceContext: plain
  contentPadding: Padding = { "all": "zero" }  отступ у содержимого
  content: ScrollListContent

ScrollListContent: oneOf
  vertical    { alignment: start | center | end = start,  gap = zero, position: top | center | bottom = top,
                reversed = false, snapToItem = false, children?: [ScrollListElement] }
  horizontal  { alignment: top | center | bottom = top,   gap = zero, position: start | center | end = start,
                reversed = false, snapToItem = false, children?: [ScrollListElement] }

ScrollListElement: oneOf
  header  { component: Component }
  item    { component: Component, viewportFraction?: float }
```

| Поле               | Смысл                                                           | Compose                                       | SwiftUI                                                |
|--------------------|-----------------------------------------------------------------|-----------------------------------------------|--------------------------------------------------------|
| `vertical`         | вертикальный список                                             | `LazyColumn`                                  | `ScrollView { LazyVStack }`                            |
| `horizontal`       | горизонтальный список                                           | `LazyRow`                                     | `ScrollView(.horizontal) { LazyHStack }`               |
| `contentPadding`   | отступ у содержимого, элементы доезжают до края окна            | `contentPadding = PaddingValues(…)`           | `.contentMargins(…, for: .scrollContent)`              |
| `alignment`        | выравнивание элементов поперёк оси                              | `horizontalAlignment` / `verticalAlignment`   | `LazyVStack(alignment:)` / `LazyHStack(alignment:)`    |
| `gap`              | промежуток между элементами                                     | `Arrangement.spacedBy(gap, …)`                | `spacing:`                                             |
| `position`         | положение элементов вдоль оси, пока содержимое короче окна      | `Arrangement.spacedBy(gap, Alignment.Bottom)` | высота содержимого не меньше окна и выравнивание       |
| `reversed`         | первый элемент — у конца оси, прокрутка начинается оттуда       | `reverseLayout = true`                        | не проверено (раздел «Ограничения и открытые вопросы») |
| `snapToItem`       | докатывание к ближайшему элементу после жеста                   | `rememberSnapFlingBehavior(state)`            | `.scrollTargetBehavior(.viewAligned)`                  |
| `header`           | заголовок, прилипающий к началу окна                            | `stickyHeader`                                | `LazyVStack(pinnedViews: .sectionHeaders)`, `Section`  |
| `viewportFraction` | размер ячейки вдоль оси — доля окна за вычетом `contentPadding` | `Modifier.fillParentMaxWidth(fraction)`       | `.containerRelativeFrame(.horizontal) { … }`           |
| `reachEnd`         | долистали до текущего конца содержимого                         | `LazyListState`, `snapshotFlow`               | `.onAppear` последнего элемента                        |

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
}

sealed interface ScrollListElement {
    data class Header(val component: Component) : ScrollListElement
    data class Item(val component: Component, val viewportFraction: Float? = null) : ScrollListElement
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
- Сетки (`verticalGrid`, `horizontalGrid`) и пейджеры (`verticalPager`, `horizontalPager`) — #221 и #222.
