# Pressable

## Назначение

`Pressable` — нажимаемый элемент: обёртка над содержимым кнопки, кликабельной карточки или строки списка. Он хранит
по дереву содержимого на каждое состояние — обычное (`default`), нажатое (`pressed`) и недоступное (`disabled`) — и
рисует дерево активного состояния внутри своей рамки. Деревья могут различаться не только оформлением, но и
содержимым: например, подпись «Оплатить» и «Недостаточно средств».

`Pressable` наследует базовую модель `Component`: размеры, отступы и индикаторы общие для всех состояний,
`appearance` (контекст `control`) оформляет рамку по состоянию, `events` — действие нажимаемого элемента. Собственные
поля — доступность (`enabled`) и деревья состояний (`states`). Кнопка и другие типовые нажимаемые элементы — пресеты
над `Pressable`.

## Состав

| Файл                               | Роль                                                    |
|------------------------------------|---------------------------------------------------------|
| `Pressable.json`                   | Публичный контракт: доступность и деревья состояний.    |
| `definitions/PressableStates.json` | Деревья содержимого по состояниям.                      |
| `docs.json`                        | Манифест документации Figma: секции страницы и примеры. |
| `changelog.json`                   | Журнал изменений API.                                   |
| `samples/`                         | Образцы: `min.json`, `max.json` и сценарии.             |

## Варианты

```
Pressable : Component                  — контекст оформления control
  events?                              — действие: только при enabled
  appearance?: Appearance.Control      — оформление рамки по состоянию
  enabled?:    boolean                 — отсутствие = доступен
  states:      PressableStates<Component>

PressableStates<T : Component>
  default:   T
  pressed:   T = default               — defaultsTo default
  disabled:  T = default               — defaultsTo default
```

Ожидаемая модель на Kotlin:

```kotlin
data class Pressable(
    // поля Component
    val enabled: Boolean = true,
    val states: PressableStates<out Component>,
) : Component

data class PressableStates<T : Component>(
    val default: T,
    val pressed: T = default,
    val disabled: T = default,
)
```

На Swift `PressableStates<T>` объявляется без ограничения `T: Component`, поле — `PressableStates<any Component>`, в
`init` — `pressed ?? default`. Имя `default` в Swift пишется в обратных кавычках.

| Состояние  | Когда активно                      | Дерево                           |
|------------|------------------------------------|----------------------------------|
| `disabled` | `enabled = false`                  | `disabled`, если нет — `default` |
| `pressed`  | палец прижат, `Pressable` доступен | `pressed`, если нет — `default`  |
| `default`  | остальное время                    | `default`                        |

| Платформа | Реализация                                                                                                            |
|-----------|-----------------------------------------------------------------------------------------------------------------------|
| Compose   | `Modifier.clickable(enabled, interactionSource, indication = null)` на рамке; `pressed` — `collectIsPressedAsState()` |
| SwiftUI   | `Button` с `ButtonStyle`, дерево выбирает `configuration.isPressed`; выключенный — дерево без `Button`                |
| Figma     | фрейм, как `Container`: рамка с оформлением активного состояния и его дерево                                          |

1. Слои рамки снизу вверх: тень, фон, дерево активного состояния, наложение, обводка. Отклик на нажатие — наложение
   рамки в `pressed`, деревья в нём не участвуют.
2. Дерево меньше рамки стоит по центру по обеим осям. Другое положение задаёт само дерево: `fill` и выравнивание
   внутри него.
3. Корень каждого дерева оформлен только `plain`. Кликабельный элемент внутри дерева — вложенный `Pressable`.
4. Деревья — один и тот же компонент: `pressed` и `disabled` того же типа, что `default`.
5. Узлы с одинаковым `id` в деревьях одного `Pressable` — один элемент: повтор `id` между деревьями допустим. Узел
   без `id` сопоставляется по пути от корня дерева. Соответствие нужно Figma и анимации перехода между состояниями.
6. Срабатывает самый глубокий кликабельный элемент — вложенный `Pressable` или узел дерева со своими `events`;
   внешний `Pressable` при этом в `pressed` не переходит.
7. Выключенный `Pressable` нажатие не перехватывает: оно уходит к внешнему, а кликабельные узлы его дерева работают.
   SwiftUI: `.disabled(true)` не используется — он отключил бы и события дерева `disabled`.
8. Если деревья разного размера, рамка меняет размер при нажатии.

Сгенерированные образцы: [минимальная модель](samples/min.json) — только обязательные свойства,
[полная модель](samples/max.json) — все свойства.

Только обычное дерево: нажатие показывает наложение рамки.

<!-- sample: samples/defaultOnly.json -->
```json
{
  "Pressable": {
    "id": "details",
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
      "symmetric": {
        "horizontal": "l",
        "vertical": "s"
      }
    },
    "appearance": {
      "control": {
        "default": {
          "background": {
            "solid": "secondaryContainer"
          },
          "cornerRadius": "full"
        },
        "pressed": {
          "background": {
            "solid": "secondaryContainer"
          },
          "cornerRadius": "full",
          "overlay": {
            "solid": "overlayPressed"
          }
        },
        "disabled": {
          "background": {
            "solid": "surface2"
          },
          "cornerRadius": "full"
        }
      }
    },
    "events": [
      {
        "click": {
          "payload": {
            "action": "openDetails"
          }
        }
      }
    ],
    "states": {
      "default": {
        "Text": {
          "id": "label",
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
            "plain": "Подробнее"
          },
          "typography": "labelLarge",
          "color": {
            "solid": "onSecondaryContainer"
          },
          "alignment": "center"
        }
      }
    }
  }
}
```

Кнопка оплаты: в недоступном состоянии другая подпись, а нажатие на неё показывает подсказку.

<!-- sample: samples/insufficientFunds.json -->
```json
{
  "Pressable": {
    "id": "pay",
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
      "symmetric": {
        "horizontal": "l",
        "vertical": "s"
      }
    },
    "appearance": {
      "control": {
        "default": {
          "background": {
            "solid": "primary"
          },
          "cornerRadius": "full"
        },
        "pressed": {
          "background": {
            "solid": "primary"
          },
          "cornerRadius": "full",
          "overlay": {
            "solid": "overlayPressed"
          }
        },
        "disabled": {
          "background": {
            "solid": "surface2"
          },
          "cornerRadius": "full"
        }
      }
    },
    "events": [
      {
        "click": {
          "payload": {
            "action": "pay"
          }
        }
      }
    ],
    "enabled": false,
    "states": {
      "default": {
        "Text": {
          "id": "label",
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
            "plain": "Оплатить"
          },
          "typography": "labelLarge",
          "color": {
            "solid": "onPrimary"
          },
          "alignment": "center"
        }
      },
      "disabled": {
        "Text": {
          "id": "label",
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
          "events": [
            {
              "click": {
                "payload": {
                  "action": "showFundsHint"
                }
              }
            }
          ],
          "content": {
            "plain": "Недостаточно средств"
          },
          "typography": "labelLarge",
          "color": {
            "solid": "onSurfaceMuted"
          },
          "alignment": "center"
        }
      }
    }
  }
}
```

Другой цвет подписи при нажатии.

<!-- sample: samples/pressedLabel.json -->
```json
{
  "Pressable": {
    "id": "cancel",
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
      "symmetric": {
        "horizontal": "l",
        "vertical": "s"
      }
    },
    "appearance": {
      "control": {
        "default": {
          "cornerRadius": "full",
          "stroke": {
            "width": "s",
            "color": "outline"
          }
        },
        "pressed": {
          "background": {
            "solid": "primary"
          },
          "cornerRadius": "full",
          "stroke": {
            "width": "s",
            "color": "primary"
          }
        },
        "disabled": {
          "cornerRadius": "full",
          "stroke": {
            "width": "s",
            "color": "outline"
          }
        }
      }
    },
    "events": [
      {
        "click": {
          "payload": {
            "action": "cancel"
          }
        }
      }
    ],
    "states": {
      "default": {
        "Text": {
          "id": "label",
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
            "plain": "Отмена"
          },
          "typography": "labelLarge",
          "color": {
            "solid": "primary"
          },
          "alignment": "center"
        }
      },
      "pressed": {
        "Text": {
          "id": "label",
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
            "plain": "Отмена"
          },
          "typography": "labelLarge",
          "color": {
            "solid": "onPrimary"
          },
          "alignment": "center"
        }
      }
    }
  }
}
```

Нажимаемая карточка товара с вложенной недоступной кнопкой: нажатие на «Нет в наличии» открывает товар.

<!-- sample: samples/productCard.json -->
```json
{
  "Pressable": {
    "id": "product",
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
      "control": {
        "default": {
          "background": {
            "solid": "surface"
          },
          "cornerRadius": "l",
          "stroke": {
            "width": "s",
            "color": "outline"
          },
          "clip": true
        },
        "pressed": {
          "background": {
            "solid": "surface"
          },
          "cornerRadius": "l",
          "stroke": {
            "width": "s",
            "color": "outline"
          },
          "overlay": {
            "solid": "overlayPressed"
          },
          "clip": true
        },
        "disabled": {
          "background": {
            "solid": "surface"
          },
          "cornerRadius": "l",
          "stroke": {
            "width": "s",
            "color": "outline"
          },
          "clip": true
        }
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
    "states": {
      "default": {
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
                "id": "title",
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
                  "plain": "Кофемолка"
                },
                "typography": "titleMedium",
                "color": {
                  "solid": "onSurface"
                },
                "alignment": "start"
              }
            },
            {
              "Pressable": {
                "id": "addToCart",
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
                  "symmetric": {
                    "horizontal": "l",
                    "vertical": "s"
                  }
                },
                "appearance": {
                  "control": {
                    "default": {
                      "background": {
                        "solid": "primary"
                      },
                      "cornerRadius": "full"
                    },
                    "pressed": {
                      "background": {
                        "solid": "primary"
                      },
                      "cornerRadius": "full",
                      "overlay": {
                        "solid": "overlayPressed"
                      }
                    },
                    "disabled": {
                      "background": {
                        "solid": "surface2"
                      },
                      "cornerRadius": "full"
                    }
                  }
                },
                "events": [
                  {
                    "click": {
                      "payload": {
                        "action": "addToCart"
                      }
                    }
                  }
                ],
                "enabled": false,
                "states": {
                  "default": {
                    "Text": {
                      "id": "label",
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
                        "plain": "В корзину"
                      },
                      "typography": "labelLarge",
                      "color": {
                        "solid": "onPrimary"
                      },
                      "alignment": "center"
                    }
                  },
                  "disabled": {
                    "Text": {
                      "id": "label",
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
                        "plain": "Нет в наличии"
                      },
                      "typography": "labelLarge",
                      "color": {
                        "solid": "onSurfaceMuted"
                      },
                      "alignment": "center"
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
}
```

## Рекомендации по использованию

- Действие задавайте в `events` самого `Pressable`, а не у корня дерева `default`: оно срабатывает только при
  `enabled`. Дерево `default` подставляется в `pressed` и `disabled` целиком, вместе со своими событиями.
- Поведение в недоступном состоянии (например, подсказка, почему нельзя нажать) — `click` у корня или узла дерева
  `disabled`.
- Деревья `pressed` и `disabled` задавайте, только когда меняется содержимое: цвет подписи, текст, символ. Отклик на
  нажатие показывайте наложением `overlay` с ролью `overlayPressed` в `appearance.control.pressed`.
- Общим элементам деревьев давайте одинаковый `id`: так они остаются одним элементом при смене состояния.
- Не меняйте размер содержимого между состояниями без необходимости: рамка будет прыгать при нажатии.
- Загрузка — не состояние: на время загрузки приложение передаёт новую модель с деревом со спиннером.
- Не кладите `Pressable` корнем дерева другого `Pressable`: внутренний займёт всю площадь и перехватит каждое
  нажатие, внешний в `pressed` не перейдёт.

## Ограничения и открытые вопросы

- Выбор (`selected`: чипы, переключатели) — другой компонент по тому же принципу.
- Вибро-отклик на нажатие — отдельная задача.
- Пока у `Container`, `Text` и `Image` остаётся контекст `control`: второй путь к виду нажатия, его удаление —
  отдельная задача-миграция.
- Смешение типов деревьев компилятор ловит не всегда (Kotlin выводит `T = Component`), поэтому основная гарантия —
  валидатор деревьев: для JSON с сервера и в отладочной сборке для моделей из кода.
