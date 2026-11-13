package io.github.nakulov.designsystem.generator.output.kotlin

// Ожидаемый вывод — текст файлов целиком, строки без отступа; `$` в тексте файла — через ${'$'}.

/** Компонент: базовая модель, сужение appearance, обобщение по defaultsTo, варианты-объекты. */
fun makeComponentFiles(): Map<String, String> {
    return mapOf(
        "io/github/nakulov/designsystem/Payload.kt" to PAYLOAD_FILE,
        "io/github/nakulov/designsystem/Rgba.kt" to RGBA_FILE,
        "io/github/nakulov/designsystem/components/box/Box.kt" to COMPONENT_BOX_FILE,
        "io/github/nakulov/designsystem/components/button/Button.kt" to COMPONENT_BUTTON_FILE,
        "io/github/nakulov/designsystem/components/button/ButtonStates.kt" to COMPONENT_BUTTON_STATES_FILE,
        "io/github/nakulov/designsystem/core/appearance/Appearance.kt" to COMPONENT_APPEARANCE_FILE,
        "io/github/nakulov/designsystem/core/component/Component.kt" to COMPONENT_COMPONENT_FILE,
        "io/github/nakulov/designsystem/core/event/Event.kt" to COMPONENT_EVENT_FILE,
        "io/github/nakulov/designsystem/core/event/EventData.kt" to COMPONENT_EVENT_DATA_FILE,
        "io/github/nakulov/designsystem/core/pin/Pin.kt" to COMPONENT_PIN_FILE,
    )
}

/** Значения default всех видов и defaultsTo. */
fun makeDefaultsFiles(): Map<String, String> {
    return mapOf(
        "io/github/nakulov/designsystem/Payload.kt" to PAYLOAD_FILE,
        "io/github/nakulov/designsystem/Rgba.kt" to RGBA_FILE,
        "io/github/nakulov/designsystem/layer/entity/Entity.kt" to makeDefaultsEntityFile(),
        "io/github/nakulov/designsystem/layer/entity/EntityEmpty.kt" to DEFAULTS_ENTITY_EMPTY_FILE,
        "io/github/nakulov/designsystem/layer/entity/EntityKeyboard.kt" to DEFAULTS_ENTITY_KEYBOARD_FILE,
        "io/github/nakulov/designsystem/layer/entity/EntityMode.kt" to DEFAULTS_ENTITY_MODE_FILE,
        "io/github/nakulov/designsystem/layer/entity/EntitySize.kt" to DEFAULTS_ENTITY_SIZE_FILE,
    )
}

/** Конфликты имён: вариант, импорт, тип своего пакета. */
fun makeNamesFiles(): Map<String, String> {
    return mapOf(
        "io/github/nakulov/designsystem/Payload.kt" to PAYLOAD_FILE,
        "io/github/nakulov/designsystem/Rgba.kt" to RGBA_FILE,
        "io/github/nakulov/designsystem/layer/icon/Icon.kt" to NAMES_ICON_FILE,
        "io/github/nakulov/designsystem/layer/one/One.kt" to NAMES_ONE_FILE,
        "io/github/nakulov/designsystem/layer/one/OneVariant.kt" to NAMES_ONE_VARIANT_FILE,
        "io/github/nakulov/designsystem/layer/other/Icon.kt" to NAMES_OTHER_ICON_FILE,
        "io/github/nakulov/designsystem/layer/two/Icon.kt" to NAMES_TWO_ICON_FILE,
        "io/github/nakulov/designsystem/layer/two/Two.kt" to NAMES_TWO_FILE,
    )
}

/** Все примитивные типы. */
fun makePrimitivesFiles(): Map<String, String> {
    return mapOf(
        "io/github/nakulov/designsystem/Payload.kt" to PAYLOAD_FILE,
        "io/github/nakulov/designsystem/Rgba.kt" to RGBA_FILE,
        "io/github/nakulov/designsystem/layer/entity/Entity.kt" to PRIMITIVES_ENTITY_FILE,
    )
}

/** Контракты без свойств, значений и вариантов. */
fun makeEmptyFiles(): Map<String, String> {
    return mapOf(
        "io/github/nakulov/designsystem/Payload.kt" to PAYLOAD_FILE,
        "io/github/nakulov/designsystem/Rgba.kt" to RGBA_FILE,
        "io/github/nakulov/designsystem/layer/entity/Entity.kt" to EMPTY_ENTITY_FILE,
        "io/github/nakulov/designsystem/layer/entity/EntityEnum.kt" to EMPTY_ENTITY_ENUM_FILE,
        "io/github/nakulov/designsystem/layer/entity/EntityInline.kt" to EMPTY_ENTITY_INLINE_FILE,
        "io/github/nakulov/designsystem/layer/entity/EntityNone.kt" to EMPTY_ENTITY_NONE_FILE,
    )
}

/** io/github/nakulov/designsystem/components/box/Box.kt из каталога kotlinComponent. */
private const val COMPONENT_BOX_FILE = """// Сгенерировано генератором (generator/) из контракта components/box/Box. Не править.

package io.github.nakulov.designsystem.components.box

import io.github.nakulov.designsystem.core.appearance.Appearance
import io.github.nakulov.designsystem.core.component.Component
import io.github.nakulov.designsystem.core.event.Event

data class Box(
    override val id: String? = null,
    override val appearance: Appearance,
    override val events: Set<Event>? = null,
    val content: Component,
) : Component
"""

/** io/github/nakulov/designsystem/components/button/Button.kt из каталога kotlinComponent. */
private const val COMPONENT_BUTTON_FILE = """// Сгенерировано генератором (generator/) из контракта components/button/Button. Не править.

package io.github.nakulov.designsystem.components.button

import io.github.nakulov.designsystem.core.appearance.Appearance
import io.github.nakulov.designsystem.core.component.Component
import io.github.nakulov.designsystem.core.event.Event
import io.github.nakulov.designsystem.core.event.EventData

data class Button(
    override val id: String? = null,
    override val appearance: Appearance.Control,
    override val events: Set<Event>? = null,
    val label: String,
    val click: EventData? = null,
    val states: ButtonStates<out Component>,
    val children: List<Component>? = null,
) : Component
"""

/** io/github/nakulov/designsystem/components/button/ButtonStates.kt из каталога kotlinComponent. */
private const val COMPONENT_BUTTON_STATES_FILE = """// Сгенерировано генератором (generator/) из контракта components/button/ButtonStates. Не править.

package io.github.nakulov.designsystem.components.button

import io.github.nakulov.designsystem.core.component.Component

data class ButtonStates<T : Component>(
    val default: T,
    val pressed: T = default,
    val disabled: Component,
)
"""

/** io/github/nakulov/designsystem/core/appearance/Appearance.kt из каталога kotlinComponent. */
private const val COMPONENT_APPEARANCE_FILE = """// Сгенерировано генератором (generator/) из контракта core/appearance/Appearance. Не править.

package io.github.nakulov.designsystem.core.appearance

sealed interface Appearance {
    data class Plain(
        val clip: Boolean = false,
    ) : Appearance

    data class Control(
        val default: Appearance.Plain,
        val pressed: Appearance.Plain,
    ) : Appearance
}
"""

/** io/github/nakulov/designsystem/core/component/Component.kt из каталога kotlinComponent. */
private const val COMPONENT_COMPONENT_FILE = """// Сгенерировано генератором (generator/) из контракта core/component/Component. Не править.

package io.github.nakulov.designsystem.core.component

import io.github.nakulov.designsystem.core.appearance.Appearance
import io.github.nakulov.designsystem.core.event.Event

interface Component {
    val id: String?
    val appearance: Appearance
    val events: Set<Event>?
}
"""

/** io/github/nakulov/designsystem/core/event/Event.kt из каталога kotlinComponent. */
private const val COMPONENT_EVENT_FILE = """// Сгенерировано генератором (generator/) из контракта core/event/Event. Не править.

package io.github.nakulov.designsystem.core.event

import io.github.nakulov.designsystem.Payload

sealed interface Event {
    data class Click(
        val payload: Payload,
    ) : Event

    data class Focus(
        val payload: Payload,
    ) : Event
}
"""

/** io/github/nakulov/designsystem/core/event/EventData.kt из каталога kotlinComponent. */
private const val COMPONENT_EVENT_DATA_FILE = """// Сгенерировано генератором (generator/) из контракта core/event/EventData. Не править.

package io.github.nakulov.designsystem.core.event

import io.github.nakulov.designsystem.Payload

data class EventData(
    val payload: Payload,
)
"""

/** io/github/nakulov/designsystem/core/pin/Pin.kt из каталога kotlinComponent. */
private const val COMPONENT_PIN_FILE = """// Сгенерировано генератором (generator/) из контракта core/pin/Pin. Не править.

package io.github.nakulov.designsystem.core.pin

sealed interface Pin {
    data class Start(
        val offset: Int,
    ) : Pin

    data class End(
        val offset: Int,
    ) : Pin
}
"""

/** io/github/nakulov/designsystem/layer/entity/Entity.kt из каталога kotlinDefaults. */
private fun makeDefaultsEntityFile(): String {
    return """// Сгенерировано генератором (generator/) из контракта layer/entity/Entity. Не править.

package io.github.nakulov.designsystem.layer.entity

import io.github.nakulov.designsystem.Rgba

data class Entity(
    val text: String = "say \"hi\" \${'$'}x\\",
    val copy: String = text,
    val flag: Boolean = true,
    val count: Int = 2,
    val ratio: Float = 1.5f,
    val whole: Float = 0f,
    val tint: Rgba = Rgba(0xC9A227FFu),
    val shade: Rgba = Rgba(0x1E2B4414u),
    val mode: EntityMode = EntityMode.Auto,
    val size: EntitySize = EntitySize.Zero,
    val custom: EntitySize = EntitySize.Custom(value = 6),
    val fixed: EntitySize = EntitySize.Fixed(4),
    val keyboard: EntityKeyboard = EntityKeyboard(),
    val email: EntityKeyboard = EntityKeyboard(type = EntityMode.Email, `in` = true),
    val nothing: EntityEmpty = EntityEmpty,
)
"""
}

/** io/github/nakulov/designsystem/layer/entity/EntityEmpty.kt из каталога kotlinDefaults. */
private const val DEFAULTS_ENTITY_EMPTY_FILE = """// Сгенерировано генератором (generator/) из контракта layer/entity/EntityEmpty. Не править.

package io.github.nakulov.designsystem.layer.entity

data object EntityEmpty
"""

/** io/github/nakulov/designsystem/layer/entity/EntityKeyboard.kt из каталога kotlinDefaults. */
private const val DEFAULTS_ENTITY_KEYBOARD_FILE = """// Сгенерировано генератором (generator/) из контракта layer/entity/EntityKeyboard. Не править.

package io.github.nakulov.designsystem.layer.entity

data class EntityKeyboard(
    val type: EntityMode = EntityMode.Auto,
    val `in`: Boolean = false,
)
"""

/** io/github/nakulov/designsystem/layer/entity/EntityMode.kt из каталога kotlinDefaults. */
private const val DEFAULTS_ENTITY_MODE_FILE = """// Сгенерировано генератором (generator/) из контракта layer/entity/EntityMode. Не править.

package io.github.nakulov.designsystem.layer.entity

sealed interface EntityMode {
    data object Auto : EntityMode

    data object Email : EntityMode
}
"""

/** io/github/nakulov/designsystem/layer/entity/EntitySize.kt из каталога kotlinDefaults. */
private const val DEFAULTS_ENTITY_SIZE_FILE = """// Сгенерировано генератором (generator/) из контракта layer/entity/EntitySize. Не править.

package io.github.nakulov.designsystem.layer.entity

sealed interface EntitySize {
    data object Zero : EntitySize

    data object Xs2 : EntitySize

    data class Custom(
        val value: Int,
    ) : EntitySize

    data class Fixed(
        val value: Int,
    ) : EntitySize
}
"""

/** io/github/nakulov/designsystem/layer/icon/Icon.kt из каталога kotlinNames. */
private const val NAMES_ICON_FILE = """// Сгенерировано генератором (generator/) из контракта layer/icon/Icon. Не править.

package io.github.nakulov.designsystem.layer.icon

sealed interface Icon {
    data object Close : Icon
}
"""

/** io/github/nakulov/designsystem/layer/one/One.kt из каталога kotlinNames. */
private const val NAMES_ONE_FILE = """// Сгенерировано генератором (generator/) из контракта layer/one/One. Не править.

package io.github.nakulov.designsystem.layer.one

import io.github.nakulov.designsystem.layer.icon.Icon

data class One(
    val icon: Icon,
    val otherIcon: io.github.nakulov.designsystem.layer.other.Icon,
)
"""

/** io/github/nakulov/designsystem/layer/one/OneVariant.kt из каталога kotlinNames. */
private const val NAMES_ONE_VARIANT_FILE = """// Сгенерировано генератором (generator/) из контракта layer/one/OneVariant. Не править.

package io.github.nakulov.designsystem.layer.one

sealed interface OneVariant {
    data class Icon(
        val name: io.github.nakulov.designsystem.layer.icon.Icon,
    ) : OneVariant

    data class String(
        val value: kotlin.String,
    ) : OneVariant
}
"""

/** io/github/nakulov/designsystem/layer/other/Icon.kt из каталога kotlinNames. */
private const val NAMES_OTHER_ICON_FILE = """// Сгенерировано генератором (generator/) из контракта layer/other/Icon. Не править.

package io.github.nakulov.designsystem.layer.other

sealed interface Icon {
    data object Open : Icon
}
"""

/** io/github/nakulov/designsystem/layer/two/Icon.kt из каталога kotlinNames. */
private const val NAMES_TWO_ICON_FILE = """// Сгенерировано генератором (generator/) из контракта layer/two/Icon. Не править.

package io.github.nakulov.designsystem.layer.two

sealed interface Icon {
    data object Local : Icon
}
"""

/** io/github/nakulov/designsystem/layer/two/Two.kt из каталога kotlinNames. */
private const val NAMES_TWO_FILE = """// Сгенерировано генератором (generator/) из контракта layer/two/Two. Не править.

package io.github.nakulov.designsystem.layer.two

data class Two(
    val icon: io.github.nakulov.designsystem.layer.icon.Icon,
)
"""

/** io/github/nakulov/designsystem/layer/entity/Entity.kt из каталога primitives. */
private const val PRIMITIVES_ENTITY_FILE = """// Сгенерировано генератором (generator/) из контракта layer/entity/Entity. Не править.

package io.github.nakulov.designsystem.layer.entity

import io.github.nakulov.designsystem.Payload
import io.github.nakulov.designsystem.Rgba

data class Entity(
    val text: String,
    val flag: Boolean,
    val count: Int,
    val ratio: Float,
    val tint: Rgba,
    val data: Payload,
)
"""

/** io/github/nakulov/designsystem/layer/entity/Entity.kt из каталога empty. */
private const val EMPTY_ENTITY_FILE = """// Сгенерировано генератором (generator/) из контракта layer/entity/Entity. Не править.

package io.github.nakulov.designsystem.layer.entity

data object Entity
"""

/** io/github/nakulov/designsystem/layer/entity/EntityEnum.kt из каталога empty. */
private const val EMPTY_ENTITY_ENUM_FILE = """// Сгенерировано генератором (generator/) из контракта layer/entity/EntityEnum. Не править.

package io.github.nakulov.designsystem.layer.entity

sealed interface EntityEnum
"""

/** io/github/nakulov/designsystem/layer/entity/EntityInline.kt из каталога empty. */
private const val EMPTY_ENTITY_INLINE_FILE = """// Сгенерировано генератором (generator/) из контракта layer/entity/EntityInline. Не править.

package io.github.nakulov.designsystem.layer.entity

sealed interface EntityInline
"""

/** io/github/nakulov/designsystem/layer/entity/EntityNone.kt из каталога empty. */
private const val EMPTY_ENTITY_NONE_FILE = """// Сгенерировано генератором (generator/) из контракта layer/entity/EntityNone. Не править.

package io.github.nakulov.designsystem.layer.entity

sealed interface EntityNone
"""

/** Общий тип Rgba: одинаков в любом каталоге. */
private const val RGBA_FILE = """// Сгенерировано генератором (generator/). Не править.

package io.github.nakulov.designsystem

import kotlin.jvm.JvmInline

/** Цвет sRGB с прозрачностью в порядке `0xRRGGBBAA` (`color`, раздел 4.1). */
@JvmInline
value class Rgba(
    val value: UInt,
)
"""

/** Общий тип Payload: одинаков в любом каталоге. */
private const val PAYLOAD_FILE = """// Сгенерировано генератором (generator/). Не править.

package io.github.nakulov.designsystem

/** Данные приложения (`payload`, раздел 4.1): приложение реализует своими моделями и регистрирует для разбора. */
interface Payload
"""
