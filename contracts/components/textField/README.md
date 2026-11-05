# TextField

## Назначение

`TextField` — поле ввода: только область, в которой пользователь вводит и редактирует текст. Подпись над полем,
вспомогательный текст и текст ошибки под ним — композиция вокруг поля; поле формы, поиск и пароль — пресеты. Подписи,
как у Material 3 `TextField`, у компонента нет: имя — как у SwiftUI `TextField` и Compose `BasicTextField`.

Это первый компонент, данные которого меняет пользователь, а не модель. Текстом владеет поле: приложение передаёт в
`value` данные, а введённый текст узнаёт событиями `change`, `submit`, `focus` и `unfocus`.

`TextField` наследует базовую модель `Component`: размеры, отступы и индикаторы, `appearance` (контекст `focusable`)
оформляет рамку по состоянию. Собственные поля — текст и его вид, подсказка поля, поведение и клавиатура, кнопка
очистки и края поля — содержимое внутри рамки слева и справа от текста.

## Состав

| Файл                                     | Роль                                                        |
|------------------------------------------|-------------------------------------------------------------|
| `TextField.json`                         | Публичный контракт: поле ввода.                             |
| `definitions/TextFieldValue.json`        | Текст из данных приложения: `plain`.                        |
| `definitions/TextFieldColor.json`        | Цвет введённого текста по состояниям.                       |
| `definitions/Placeholder.json`           | Подсказка поля: текст, типографика, цвет.                   |
| `definitions/PlaceholderColor.json`      | Цвет подсказки поля по состояниям.                          |
| `definitions/TextFieldLines.json`        | Строки: `single` или `multi`.                               |
| `definitions/TextFieldLinesMode.json`    | Вариант без параметров: `single`.                           |
| `definitions/TextFieldMultiLines.json`   | Многострочное поле: `min` и `max` строк.                    |
| `definitions/ClearButton.json`           | Кнопка очистки: видимость, отступ, кнопка.                  |
| `definitions/ClearButtonVisibility.json` | Когда видна кнопка очистки.                                 |
| `definitions/TextFieldEdge.json`         | Край поля: содержимое, отступ, положение по вертикали.      |
| `definitions/EdgeAlignment.json`         | Положение края по вертикали.                                |
| `definitions/TextFieldStates.json`       | Деревья содержимого края по состояниям поля.                |
| `definitions/Keyboard.json`              | Клавиатура: раскладка, Return, автозаполнение, исправление. |
| `definitions/KeyboardType.json`          | Раскладка клавиатуры.                                       |
| `definitions/ReturnKey.json`             | Подпись и действие клавиши Return.                          |
| `definitions/ContentType.json`           | Смысл данных для автозаполнения.                            |
| `definitions/Capitalization.json`        | Заглавные буквы.                                            |
| `definitions/Autocorrect.json`           | Автоисправление.                                            |
| `figma.json`                             | Отображение в Figma: variant `filled` и переопределения.    |
| `docs.json`                              | Манифест документации Figma: секции страницы и примеры.     |
| `changelog.json`                         | Журнал изменений API.                                       |
| `samples/`                               | Образцы: `min.json`, `max.json` и сценарии.                 |

## Варианты

```
TextField : Component                      — контекст оформления focusable
  appearance?:  Appearance.Focusable       — только рамка
  events?:      Set<Event>                 — change, submit, focus, unfocus

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
Keyboard:         { type = text, returnKey = default, contentType = none, capitalization = auto, autocorrect = auto }
KeyboardType:     text | number | decimal | phone | email | url
ReturnKey:        default | done | go | next | search | send
Capitalization:   auto | none | sentences | words | characters
Autocorrect:      auto | enabled | disabled
```

Ожидаемая модель на Kotlin:

```kotlin
data class TextField(
    // поля Component
    val value: TextFieldValue = TextFieldValue.Plain(""),
    val typography: TypographyRole,
    val color: TextFieldColor,
    val alignment: TextAlignment,
    val cursorColor: ColorRole,
    val placeholder: Placeholder? = null,
    val enabled: Boolean = true,
    val secure: Boolean = false,
    val autofocus: Boolean = false,
    val lines: TextFieldLines = TextFieldLines.Single,
    val keyboard: Keyboard = Keyboard(),
    val clearButton: ClearButton? = null,
    val leading: TextFieldEdge? = null,
    val trailing: TextFieldEdge? = null,
) : Component

data class TextFieldColor(
    val default: TextColor,
    val focused: TextColor = default,
    val highlighted: TextColor = default,
    val disabled: TextColor,
)

data class ClearButton(val visibility: ClearButtonVisibility, val gap: Spacing, val button: Pressable)

data class TextFieldEdge(val content: TextFieldStates<out Component>, val gap: Spacing, val verticalAlignment: EdgeAlignment)

data class TextFieldStates<T : Component>(
    val default: T,
    val focused: T = default,
    val disabled: Component,
)
```

`ClearButton.button` в JSON записывается полями `Pressable`, без обёртки `{ "Pressable": … }`: тип известен из
контракта. На Swift `TextFieldStates<T>` объявляется без ограничения `T: Component`, как `PressableStates`.

**Состояния.** Одно активное состояние выбирает рамку (`appearance.focusable`), цвета текста и подсказки поля и
содержимое краёв.

| Состояние  | Когда активно        | Цвет текста                     | Дерево края                     |
|------------|----------------------|---------------------------------|---------------------------------|
| `disabled` | `enabled = false`    | `disabled`                      | `disabled`                      |
| `focused`  | фокус, поле доступно | `focused`, если нет — `default` | `focused`, если нет — `default` |
| `default`  | остальное время      | `default`                       | `default`                       |

`highlighted` — цвет выделенной части текста (выделение бывает только в фокусе), по умолчанию — `default`. Подсветка
выделения, ручки и курсор — `cursorColor`; курсор виден только в фокусе.

**Строки, края и кнопка очистки.**

| `lines`              | Поведение                                                                           |
|----------------------|-------------------------------------------------------------------------------------|
| `single`             | одна строка, длинный текст прокручивается вбок, Return — действие (`submit`)        |
| `multi { min, max }` | перенос, рост от `min` до `max` строк, дальше вертикальная прокрутка; текст у верха |

Порядок слева направо: `leading` → текст → кнопка очистки → `trailing`. Текст не заезжает под края и кнопку, граница —
с отступом `gap`; края не прокручиваются с текстом; край получает свой размер, текст — оставшуюся ширину.
`verticalAlignment` — положение края в выросшем поле.

| `ClearButtonVisibility` | Кнопка видна           |
|-------------------------|------------------------|
| `always`                | всегда                 |
| `whileEditing`          | пока поле в фокусе     |
| `whileNotEmpty`         | пока в поле есть текст |

Кнопка очистки — часть поля: `enabled` кнопки выставляет поле (`disabled`, если поле выключено или пусто), нажатие
стирает текст, оставляет фокус и даёт `change`. Скрытая кнопка не занимает места.

**Клавиатура.** Значения — пересечение платформ; платформа переводит в своё.

| `capitalization` и `autocorrect` = `auto`                                                                       | Заглавные   | Исправление |
|-----------------------------------------------------------------------------------------------------------------|-------------|-------------|
| `type` `number`, `decimal`, `phone`, `email`, `url`                                                             | `none`      | выключено   |
| `contentType` `username`, `password`, `newPassword`, `oneTimeCode`, `email`, `phone`, `postalCode`, карты, даты | `none`      | выключено   |
| `contentType` имени: `fullName`, `givenName`, `middleName`, `familyName`, `namePrefix`, `nameSuffix`            | `words`     | выключено   |
| `contentType` адреса: `street`, `streetDetails`, `city`, `region`, `country`                                    | `words`     | выключено   |
| остальное                                                                                                       | `sentences` | включено    |

1. Android: раскладка `Password` и `NumberPassword` — по `contentType` `password` и `newPassword` (с `type` `text` и
   `number`), а не по `secure`: после показа пароля глазком клавиатура не должна запоминать ввод.
2. iOS: у `numberPad`, `decimalPad` и `phonePad` нет клавиши Return. Значение `contentType`, недоступное на версии
   iOS (карта, дата рождения — iOS 17+), не передаётся; минимальная версия — выбор платформы.
3. `returnKey = next` переводит фокус к следующему доступному `TextField` в порядке дерева, иначе скрывает
   клавиатуру. `autofocus` — фокус при первом появлении поля; новая модель фокус не трогает.

| Платформа | Реализация                                                                                                                                       |
|-----------|--------------------------------------------------------------------------------------------------------------------------------------------------|
| Compose   | `BasicTextField(TextFieldState)`, `KeyboardOptions`, `ContentType` в семантике, `Modifier.onFocusChanged`, `ImeAction.Next` → `moveFocus`        |
| SwiftUI   | обычное поле — обёртка над `UITextView` (цвет выделенного текста — TextKit 2, рост и прокрутка строк); скрытое — `UITextField` или `SecureField` |
| Figma     | Component Set: `state` × `filled`; края — только при отрисовке модели, фреймом                                                                   |

Сгенерированные образцы: [минимальная модель](samples/min.json) — только обязательные свойства,
[полная модель](samples/max.json) — все свойства.

Пустое поле e-mail с подсказкой и кнопкой очистки: кнопка видна, когда в поле появится текст.

<!-- sample: samples/emailEmpty.json -->
```json
{
  "TextField": {
    "id": "email",
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
        "vertical": "s"
      }
    },
    "appearance": {
      "focusable": {
        "default": {
          "cornerRadius": "m",
          "stroke": {
            "width": "s",
            "color": "outline"
          }
        },
        "focused": {
          "cornerRadius": "m",
          "stroke": {
            "width": "m",
            "color": "primary"
          }
        },
        "disabled": {
          "background": {
            "solid": "surface2"
          },
          "cornerRadius": "m",
          "stroke": {
            "width": "s",
            "color": "outline"
          }
        }
      }
    },
    "events": [
      {
        "change": {
          "payload": {
            "action": "emailChanged"
          }
        }
      }
    ],
    "typography": "bodyLarge",
    "color": {
      "default": {
        "solid": "onSurface"
      },
      "disabled": {
        "solid": "onSurfaceMuted"
      }
    },
    "alignment": "start",
    "cursorColor": "primary",
    "placeholder": {
      "content": {
        "plain": "E-mail"
      },
      "typography": "bodyLarge",
      "color": {
        "default": {
          "solid": "onSurfaceVariant"
        },
        "disabled": {
          "solid": "onSurfaceMuted"
        }
      }
    },
    "keyboard": {
      "type": "email",
      "returnKey": "next",
      "contentType": "email"
    },
    "clearButton": {
      "visibility": "whileNotEmpty",
      "gap": "s",
      "button": {
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
          "control": {
            "default": {
              "cornerRadius": "full"
            },
            "pressed": {
              "cornerRadius": "full",
              "overlay": {
                "solid": "overlayPressed"
              }
            },
            "disabled": {
              "cornerRadius": "full"
            }
          }
        },
        "states": {
          "default": {
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
                    "color": "onSurfaceVariant"
                  }
                }
              }
            }
          }
        }
      }
    }
  }
}
```

То же поле с текстом.

<!-- sample: samples/email.json -->
```json
{
  "TextField": {
    "id": "email",
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
        "vertical": "s"
      }
    },
    "appearance": {
      "focusable": {
        "default": {
          "cornerRadius": "m",
          "stroke": {
            "width": "s",
            "color": "outline"
          }
        },
        "focused": {
          "cornerRadius": "m",
          "stroke": {
            "width": "m",
            "color": "primary"
          }
        },
        "disabled": {
          "background": {
            "solid": "surface2"
          },
          "cornerRadius": "m",
          "stroke": {
            "width": "s",
            "color": "outline"
          }
        }
      }
    },
    "events": [
      {
        "change": {
          "payload": {
            "action": "emailChanged"
          }
        }
      }
    ],
    "value": {
      "plain": "anna@example.com"
    },
    "typography": "bodyLarge",
    "color": {
      "default": {
        "solid": "onSurface"
      },
      "disabled": {
        "solid": "onSurfaceMuted"
      }
    },
    "alignment": "start",
    "cursorColor": "primary",
    "placeholder": {
      "content": {
        "plain": "E-mail"
      },
      "typography": "bodyLarge",
      "color": {
        "default": {
          "solid": "onSurfaceVariant"
        },
        "disabled": {
          "solid": "onSurfaceMuted"
        }
      }
    },
    "keyboard": {
      "type": "email",
      "returnKey": "next",
      "contentType": "email"
    },
    "clearButton": {
      "visibility": "whileNotEmpty",
      "gap": "s",
      "button": {
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
          "control": {
            "default": {
              "cornerRadius": "full"
            },
            "pressed": {
              "cornerRadius": "full",
              "overlay": {
                "solid": "overlayPressed"
              }
            },
            "disabled": {
              "cornerRadius": "full"
            }
          }
        },
        "states": {
          "default": {
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
                    "color": "onSurfaceVariant"
                  }
                }
              }
            }
          }
        }
      }
    }
  }
}
```

Пароль: скрытый ввод, глазок справа показывает пароль — приложение присылает модель с `secure = false`.

<!-- sample: samples/password.json -->
```json
{
  "TextField": {
    "id": "password",
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
        "vertical": "s"
      }
    },
    "appearance": {
      "focusable": {
        "default": {
          "cornerRadius": "m",
          "stroke": {
            "width": "s",
            "color": "outline"
          }
        },
        "focused": {
          "cornerRadius": "m",
          "stroke": {
            "width": "m",
            "color": "primary"
          }
        },
        "disabled": {
          "background": {
            "solid": "surface2"
          },
          "cornerRadius": "m",
          "stroke": {
            "width": "s",
            "color": "outline"
          }
        }
      }
    },
    "events": [
      {
        "change": {
          "payload": {
            "action": "passwordChanged"
          }
        }
      },
      {
        "unfocus": {
          "payload": {
            "action": "validatePassword"
          }
        }
      },
      {
        "submit": {
          "payload": {
            "action": "login"
          }
        }
      }
    ],
    "typography": "bodyLarge",
    "color": {
      "default": {
        "solid": "onSurface"
      },
      "disabled": {
        "solid": "onSurfaceMuted"
      }
    },
    "alignment": "start",
    "cursorColor": "primary",
    "placeholder": {
      "content": {
        "plain": "Пароль"
      },
      "typography": "bodyLarge",
      "color": {
        "default": {
          "solid": "onSurfaceVariant"
        },
        "disabled": {
          "solid": "onSurfaceMuted"
        }
      }
    },
    "secure": true,
    "keyboard": {
      "returnKey": "done",
      "contentType": "password"
    },
    "trailing": {
      "content": {
        "default": {
          "Pressable": {
            "id": "togglePassword",
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
              "control": {
                "default": {
                  "cornerRadius": "full"
                },
                "pressed": {
                  "cornerRadius": "full",
                  "overlay": {
                    "solid": "overlayPressed"
                  }
                },
                "disabled": {
                  "cornerRadius": "full"
                }
              }
            },
            "events": [
              {
                "click": {
                  "payload": {
                    "action": "togglePassword"
                  }
                }
              }
            ],
            "states": {
              "default": {
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
                        "name": "eye",
                        "color": "onSurfaceVariant"
                      }
                    }
                  }
                }
              }
            }
          }
        },
        "disabled": {
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
                  "name": "eye",
                  "color": "onSurfaceMuted"
                }
              }
            }
          }
        }
      },
      "gap": "s",
      "verticalAlignment": "center"
    }
  }
}
```

Поиск: лупа слева меняет цвет по состоянию, кнопка очистки, клавиша «Найти».

<!-- sample: samples/search.json -->
```json
{
  "TextField": {
    "id": "search",
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
        "vertical": "s"
      }
    },
    "appearance": {
      "focusable": {
        "default": {
          "cornerRadius": "full",
          "stroke": {
            "width": "s",
            "color": "outline"
          }
        },
        "focused": {
          "cornerRadius": "full",
          "stroke": {
            "width": "m",
            "color": "primary"
          }
        },
        "disabled": {
          "background": {
            "solid": "surface2"
          },
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
        "change": {
          "payload": {
            "action": "searchQueryChanged"
          }
        }
      },
      {
        "submit": {
          "payload": {
            "action": "search"
          }
        }
      }
    ],
    "typography": "bodyLarge",
    "color": {
      "default": {
        "solid": "onSurface"
      },
      "disabled": {
        "solid": "onSurfaceMuted"
      }
    },
    "alignment": "start",
    "cursorColor": "primary",
    "placeholder": {
      "content": {
        "plain": "Поиск"
      },
      "typography": "bodyLarge",
      "color": {
        "default": {
          "solid": "onSurfaceVariant"
        },
        "disabled": {
          "solid": "onSurfaceMuted"
        }
      }
    },
    "keyboard": {
      "returnKey": "search"
    },
    "clearButton": {
      "visibility": "whileNotEmpty",
      "gap": "s",
      "button": {
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
          "control": {
            "default": {
              "cornerRadius": "full"
            },
            "pressed": {
              "cornerRadius": "full",
              "overlay": {
                "solid": "overlayPressed"
              }
            },
            "disabled": {
              "cornerRadius": "full"
            }
          }
        },
        "states": {
          "default": {
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
                    "color": "onSurfaceVariant"
                  }
                }
              }
            }
          }
        }
      }
    },
    "leading": {
      "content": {
        "default": {
          "Image": {
            "id": "searchIcon",
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
                  "name": "search",
                  "color": "onSurfaceVariant"
                }
              }
            }
          }
        },
        "focused": {
          "Image": {
            "id": "searchIcon",
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
                  "name": "search",
                  "color": "onSurface"
                }
              }
            }
          }
        },
        "disabled": {
          "Image": {
            "id": "searchIcon",
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
                  "name": "search",
                  "color": "onSurfaceMuted"
                }
              }
            }
          }
        }
      },
      "gap": "s",
      "verticalAlignment": "center"
    }
  }
}
```

Сообщение: от одной до пяти строк, кнопка отправки прижата к низу.

<!-- sample: samples/message.json -->
```json
{
  "TextField": {
    "id": "message",
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
        "vertical": "s"
      }
    },
    "appearance": {
      "focusable": {
        "default": {
          "cornerRadius": "m",
          "stroke": {
            "width": "s",
            "color": "outline"
          }
        },
        "focused": {
          "cornerRadius": "m",
          "stroke": {
            "width": "m",
            "color": "primary"
          }
        },
        "disabled": {
          "background": {
            "solid": "surface2"
          },
          "cornerRadius": "m",
          "stroke": {
            "width": "s",
            "color": "outline"
          }
        }
      }
    },
    "events": [
      {
        "change": {
          "payload": {
            "action": "draftChanged"
          }
        }
      }
    ],
    "typography": "bodyLarge",
    "color": {
      "default": {
        "solid": "onSurface"
      },
      "disabled": {
        "solid": "onSurfaceMuted"
      }
    },
    "alignment": "start",
    "cursorColor": "primary",
    "placeholder": {
      "content": {
        "plain": "Сообщение"
      },
      "typography": "bodyLarge",
      "color": {
        "default": {
          "solid": "onSurfaceVariant"
        },
        "disabled": {
          "solid": "onSurfaceMuted"
        }
      }
    },
    "lines": {
      "multi": {
        "min": 1,
        "max": {
          "count": 5
        }
      }
    },
    "trailing": {
      "content": {
        "default": {
          "Pressable": {
            "id": "send",
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
              "all": "xs"
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
                    "action": "sendMessage"
                  }
                }
              }
            ],
            "states": {
              "default": {
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
                        "name": "send",
                        "color": "onPrimary"
                      }
                    }
                  }
                }
              }
            }
          }
        },
        "disabled": {
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
                  "name": "send",
                  "color": "onSurfaceMuted"
                }
              }
            }
          }
        }
      },
      "gap": "s",
      "verticalAlignment": "bottom"
    }
  }
}
```

Сумма: текст по правому краю, знак валюты справа, цифровая клавиатура с разделителем.

<!-- sample: samples/amount.json -->
```json
{
  "TextField": {
    "id": "amount",
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
        "vertical": "s"
      }
    },
    "appearance": {
      "focusable": {
        "default": {
          "cornerRadius": "m",
          "stroke": {
            "width": "s",
            "color": "outline"
          }
        },
        "focused": {
          "cornerRadius": "m",
          "stroke": {
            "width": "m",
            "color": "primary"
          }
        },
        "disabled": {
          "background": {
            "solid": "surface2"
          },
          "cornerRadius": "m",
          "stroke": {
            "width": "s",
            "color": "outline"
          }
        }
      }
    },
    "events": [
      {
        "change": {
          "payload": {
            "action": "amountChanged"
          }
        }
      }
    ],
    "value": {
      "plain": "1 500"
    },
    "typography": "headlineSmall",
    "color": {
      "default": {
        "solid": "onSurface"
      },
      "disabled": {
        "solid": "onSurfaceMuted"
      }
    },
    "alignment": "end",
    "cursorColor": "primary",
    "placeholder": {
      "content": {
        "plain": "0"
      },
      "typography": "headlineSmall",
      "color": {
        "default": {
          "solid": "onSurfaceVariant"
        },
        "disabled": {
          "solid": "onSurfaceMuted"
        }
      }
    },
    "keyboard": {
      "type": "decimal",
      "returnKey": "done"
    },
    "trailing": {
      "content": {
        "default": {
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
              "plain": "₽"
            },
            "typography": "headlineSmall",
            "color": {
              "solid": "onSurfaceVariant"
            },
            "alignment": "start"
          }
        },
        "disabled": {
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
              "plain": "₽"
            },
            "typography": "headlineSmall",
            "color": {
              "solid": "onSurfaceMuted"
            },
            "alignment": "start"
          }
        }
      },
      "gap": "xs",
      "verticalAlignment": "center"
    }
  }
}
```

Код из SMS: по центру, фокус при появлении, подсказка автозаполнения кода.

<!-- sample: samples/smsCode.json -->
```json
{
  "TextField": {
    "id": "smsCode",
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
        "vertical": "s"
      }
    },
    "appearance": {
      "focusable": {
        "default": {
          "cornerRadius": "m",
          "stroke": {
            "width": "s",
            "color": "outline"
          }
        },
        "focused": {
          "cornerRadius": "m",
          "stroke": {
            "width": "m",
            "color": "primary"
          }
        },
        "disabled": {
          "background": {
            "solid": "surface2"
          },
          "cornerRadius": "m",
          "stroke": {
            "width": "s",
            "color": "outline"
          }
        }
      }
    },
    "events": [
      {
        "change": {
          "payload": {
            "action": "codeChanged"
          }
        }
      }
    ],
    "typography": "headlineMedium",
    "color": {
      "default": {
        "solid": "onSurface"
      },
      "disabled": {
        "solid": "onSurfaceMuted"
      }
    },
    "alignment": "center",
    "cursorColor": "primary",
    "placeholder": {
      "content": {
        "plain": "Код из SMS"
      },
      "typography": "bodyLarge",
      "color": {
        "default": {
          "solid": "onSurfaceVariant"
        },
        "disabled": {
          "solid": "onSurfaceMuted"
        }
      }
    },
    "autofocus": true,
    "keyboard": {
      "type": "number",
      "contentType": "oneTimeCode"
    }
  }
}
```

Недоступное заполненное поле.

<!-- sample: samples/disabledFilled.json -->
```json
{
  "TextField": {
    "id": "email",
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
        "vertical": "s"
      }
    },
    "appearance": {
      "focusable": {
        "default": {
          "cornerRadius": "m",
          "stroke": {
            "width": "s",
            "color": "outline"
          }
        },
        "focused": {
          "cornerRadius": "m",
          "stroke": {
            "width": "m",
            "color": "primary"
          }
        },
        "disabled": {
          "background": {
            "solid": "surface2"
          },
          "cornerRadius": "m",
          "stroke": {
            "width": "s",
            "color": "outline"
          }
        }
      }
    },
    "value": {
      "plain": "anna@example.com"
    },
    "enabled": false,
    "typography": "bodyLarge",
    "color": {
      "default": {
        "solid": "onSurface"
      },
      "disabled": {
        "solid": "onSurfaceMuted"
      }
    },
    "alignment": "start",
    "cursorColor": "primary",
    "placeholder": {
      "content": {
        "plain": "E-mail"
      },
      "typography": "bodyLarge",
      "color": {
        "default": {
          "solid": "onSurfaceVariant"
        },
        "disabled": {
          "solid": "onSurfaceMuted"
        }
      }
    },
    "keyboard": {
      "type": "email",
      "contentType": "email"
    }
  }
}
```

## Рекомендации по использованию

- Передавайте в `value` данные приложения (сохранённый адрес, черновик), а не введённый текст: поле само хранит ввод
  и сбрасывает его, когда `value` в модели меняется. Введённый текст приложение узнаёт событием `change`.
- Ошибку и успех показывайте новой моделью: другая рамка в `appearance` и текст ошибки под полем. Состояний ошибки у
  поля нет.
- Подпись над полем и вспомогательный текст под ним собирайте `Container` вокруг поля.
- Задавайте `disabled` у цветов и деревьев краёв приглушённым: выключенное поле с ярким текстом или нажимаемым глазком
  выглядит и ведёт себя как активное. В `disabled` цвета фрагментов подсказки поля не действуют.
- Дерево `disabled` края может быть другим компонентом: например, глазок в `default` — `Pressable`, а в `disabled` —
  `Image` без нажатия. В отличие от `Pressable`, у края нет своего `enabled`, который отключил бы действие.
- Общим узлам деревьев края давайте одинаковый `id`: так они остаются одним элементом при смене состояния.
- Кнопку «показать пароль» кладите в `trailing` как `Pressable`; по нажатию приложение присылает модель с
  `secure = false`.
- Указывайте `contentType`: он включает автозаполнение и менеджер паролей и выбирает заглавные буквы и исправление.
- Типографика одна на поле. У подсказки поля своя: подсказка — текст интерфейса, ввод — любая письменность. Разная
  высота строки даёт скачок высоты поля с первым символом.

## Ограничения и открытые вопросы

- Форматированный ввод (`rich`), маски ввода и выделяемый текст без редактирования — отдельные задачи.
- Compose: цвет выделенного текста — окраска диапазона по `selection`; не проверено, решает реализация.
- SwiftUI: учитывает ли `TextField` шрифт и цвета подсказки (`prompt`); запасной путь — свой `Text` поверх пустого
  поля.
- В формате пока не выражаются и проверяются валидатором деревьев: одно `autofocus = true` в дереве; `min ≥ 1`,
  `count ≥ 1`, `min ≤ count`; при `multi` — `secure = false` и `returnKey = default`; нет `click` во фрагментах
  подсказки поля; у `clearButton.button` нет `enabled` и `events`. События `change`, `submit`, `focus`, `unfocus` у
  других компонентов валидатор не отклоняет.
- В библиотеке Figma `TextField` — Component Set `state` × `filled` (образцы `emailEmpty` и `email`). `value` и
  текст подсказки — text-свойства; типографика, цвета, выравнивание, строки, `secure` и отступ кнопки очистки —
  переопределения слоёв. `color.highlighted`, `clearButton.visibility`, `autofocus`, `keyboard` и `events` в Figma не
  отображаются; края — только при отрисовке модели. Пока плагин Component Set не строит: компонент
  пропускается с предупреждением «компонент не поддерживается».
