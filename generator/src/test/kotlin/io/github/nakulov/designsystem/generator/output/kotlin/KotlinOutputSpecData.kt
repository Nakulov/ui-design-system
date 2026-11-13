package io.github.nakulov.designsystem.generator.output.kotlin

// Ожидаемый вывод — текст файлов целиком, строки без отступа; `$` в тексте файла — через ${'$'}.

/** Компонент: базовая модель, сужение appearance, обобщение по defaultsTo, варианты-объекты. */
fun makeComponentFiles(): Map<String, String> {
    return mapOf(
        "io/github/nakulov/designsystem/DesignSystemModule.kt" to COMPONENT_DESIGN_SYSTEM_MODULE_FILE,
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
        "io/github/nakulov/designsystem/DesignSystemModule.kt" to DEFAULTS_DESIGN_SYSTEM_MODULE_FILE,
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
        "io/github/nakulov/designsystem/DesignSystemModule.kt" to NAMES_DESIGN_SYSTEM_MODULE_FILE,
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
        "io/github/nakulov/designsystem/DesignSystemModule.kt" to PRIMITIVES_DESIGN_SYSTEM_MODULE_FILE,
        "io/github/nakulov/designsystem/layer/entity/Entity.kt" to PRIMITIVES_ENTITY_FILE,
    )
}

/** Контракты без свойств, значений и вариантов. */
fun makeEmptyFiles(): Map<String, String> {
    return mapOf(
        "io/github/nakulov/designsystem/DesignSystemModule.kt" to EMPTY_DESIGN_SYSTEM_MODULE_FILE,
        "io/github/nakulov/designsystem/layer/entity/Entity.kt" to EMPTY_ENTITY_FILE,
        "io/github/nakulov/designsystem/layer/entity/EntityEnum.kt" to EMPTY_ENTITY_ENUM_FILE,
        "io/github/nakulov/designsystem/layer/entity/EntityInline.kt" to EMPTY_ENTITY_INLINE_FILE,
        "io/github/nakulov/designsystem/layer/entity/EntityNone.kt" to EMPTY_ENTITY_NONE_FILE,
    )
}

/** Базовая модель без свойств и без реализаций. */
fun makeEmptyBaseFiles(): Map<String, String> {
    return mapOf(
        "io/github/nakulov/designsystem/DesignSystemModule.kt" to EMPTY_BASE_MODULE_FILE,
        "io/github/nakulov/designsystem/layer/entity/Entity.kt" to EMPTY_BASE_ENTITY_FILE,
    )
}

/** io/github/nakulov/designsystem/DesignSystemModule.kt из каталога kotlinComponent. */
private const val COMPONENT_DESIGN_SYSTEM_MODULE_FILE = """// Сгенерировано генератором (generator/) из контрактов типа base. Не править.

package io.github.nakulov.designsystem

import io.github.nakulov.designsystem.components.box.Box
import io.github.nakulov.designsystem.components.button.Button
import io.github.nakulov.designsystem.core.component.Component
import io.github.nakulov.designsystem.core.component.ComponentSerializer
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass

/** Реализации базовых моделей библиотеки: продукт подключает `include(DesignSystemModule)`. */
val DesignSystemModule: SerializersModule = SerializersModule {
    contextual(Component::class, ComponentSerializer)
    polymorphic(Component::class) {
        subclass(Box::class)
        subclass(Button::class)
    }
}
"""

/** io/github/nakulov/designsystem/components/box/Box.kt из каталога kotlinComponent. */
private const val COMPONENT_BOX_FILE = """// Сгенерировано генератором (generator/) из контракта components/box/Box. Не править.

@file:UseSerializers(AppearanceSerializer::class, ComponentSerializer::class, EventSerializer::class)

package io.github.nakulov.designsystem.components.box

import io.github.nakulov.designsystem.core.appearance.Appearance
import io.github.nakulov.designsystem.core.appearance.AppearanceSerializer
import io.github.nakulov.designsystem.core.component.Component
import io.github.nakulov.designsystem.core.component.ComponentSerializer
import io.github.nakulov.designsystem.core.event.Event
import io.github.nakulov.designsystem.core.event.EventSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers

@Serializable
@SerialName("Box")
data class Box(
    override val id: String? = null,
    override val appearance: Appearance,
    override val events: Set<Event>? = null,
    val content: Component,
) : Component
"""

/** io/github/nakulov/designsystem/components/button/Button.kt из каталога kotlinComponent. */
private const val COMPONENT_BUTTON_FILE = """// Сгенерировано генератором (generator/) из контракта components/button/Button. Не править.

@file:UseSerializers(ComponentSerializer::class, EventSerializer::class)

package io.github.nakulov.designsystem.components.button

import io.github.nakulov.designsystem.core.appearance.Appearance
import io.github.nakulov.designsystem.core.appearance.AppearanceSerializer
import io.github.nakulov.designsystem.core.component.Component
import io.github.nakulov.designsystem.core.component.ComponentSerializer
import io.github.nakulov.designsystem.core.event.Event
import io.github.nakulov.designsystem.core.event.EventData
import io.github.nakulov.designsystem.core.event.EventSerializer
import io.github.nakulov.designsystem.serialization.NarrowedSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers

@Serializable
@SerialName("Button")
data class Button(
    override val id: String? = null,
    @Serializable(with = ButtonAppearanceSerializer::class)
    override val appearance: Appearance.Control,
    override val events: Set<Event>? = null,
    val label: String,
    val click: EventData? = null,
    val states: ButtonStates<out Component>,
    val children: List<Component>? = null,
) : Component

object ButtonAppearanceSerializer : NarrowedSerializer<Appearance, Appearance.Control>(AppearanceSerializer, variant = "control")
"""

/** io/github/nakulov/designsystem/components/button/ButtonStates.kt из каталога kotlinComponent. */
private const val COMPONENT_BUTTON_STATES_FILE = """// Сгенерировано генератором (generator/) из контракта components/button/ButtonStates. Не править.

@file:UseSerializers(ComponentSerializer::class)

package io.github.nakulov.designsystem.components.button

import io.github.nakulov.designsystem.core.component.Component
import io.github.nakulov.designsystem.core.component.ComponentSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers

@Serializable
data class ButtonStates<T : Component>(
    val default: T,
    val pressed: T = default,
    val disabled: Component,
)
"""

/** io/github/nakulov/designsystem/core/appearance/Appearance.kt из каталога kotlinComponent. */
private const val COMPONENT_APPEARANCE_FILE = """// Сгенерировано генератором (generator/) из контракта core/appearance/Appearance. Не править.

@file:OptIn(ExperimentalSerializationApi::class)

package io.github.nakulov.designsystem.core.appearance

import io.github.nakulov.designsystem.serialization.OneOfSerializer
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonClassDiscriminator

@Serializable
@JsonClassDiscriminator("#variant")
sealed interface Appearance {
    @Serializable
    @SerialName("plain")
    data class Plain(
        val clip: Boolean = false,
    ) : Appearance

    @Serializable
    @SerialName("control")
    data class Control(
        val default: Appearance.Plain,
        val pressed: Appearance.Plain,
    ) : Appearance
}

object AppearanceSerializer : OneOfSerializer<Appearance>(Appearance.serializer())
"""

/** io/github/nakulov/designsystem/core/component/Component.kt из каталога kotlinComponent. */
private const val COMPONENT_COMPONENT_FILE = """// Сгенерировано генератором (generator/) из контракта core/component/Component. Не править.

@file:UseSerializers(AppearanceSerializer::class, EventSerializer::class)

package io.github.nakulov.designsystem.core.component

import io.github.nakulov.designsystem.core.appearance.Appearance
import io.github.nakulov.designsystem.core.appearance.AppearanceSerializer
import io.github.nakulov.designsystem.core.event.Event
import io.github.nakulov.designsystem.core.event.EventSerializer
import io.github.nakulov.designsystem.serialization.BaseSerializer
import kotlinx.serialization.UseSerializers

interface Component {
    val id: String?
    val appearance: Appearance
    val events: Set<Event>?
}

object ComponentSerializer : BaseSerializer<Component>(Component::class)
"""

/** io/github/nakulov/designsystem/core/event/Event.kt из каталога kotlinComponent. */
private const val COMPONENT_EVENT_FILE = """// Сгенерировано генератором (generator/) из контракта core/event/Event. Не править.

@file:OptIn(ExperimentalSerializationApi::class)

package io.github.nakulov.designsystem.core.event

import io.github.nakulov.designsystem.Payload
import io.github.nakulov.designsystem.serialization.OneOfSerializer
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonClassDiscriminator

@Serializable
@JsonClassDiscriminator("#variant")
sealed interface Event {
    @Serializable
    @SerialName("click")
    data class Click(
        val payload: Payload,
    ) : Event

    @Serializable
    @SerialName("focus")
    data class Focus(
        val payload: Payload,
    ) : Event
}

object EventSerializer : OneOfSerializer<Event>(Event.serializer())
"""

/** io/github/nakulov/designsystem/core/event/EventData.kt из каталога kotlinComponent. */
private const val COMPONENT_EVENT_DATA_FILE = """// Сгенерировано генератором (generator/) из контракта core/event/EventData. Не править.

package io.github.nakulov.designsystem.core.event

import io.github.nakulov.designsystem.Payload
import kotlinx.serialization.Serializable

@Serializable
data class EventData(
    val payload: Payload,
)
"""

/** io/github/nakulov/designsystem/core/pin/Pin.kt из каталога kotlinComponent. */
private const val COMPONENT_PIN_FILE = """// Сгенерировано генератором (generator/) из контракта core/pin/Pin. Не править.

@file:OptIn(ExperimentalSerializationApi::class)

package io.github.nakulov.designsystem.core.pin

import io.github.nakulov.designsystem.serialization.OneOfSerializer
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonClassDiscriminator

@Serializable
@JsonClassDiscriminator("#variant")
sealed interface Pin {
    @Serializable
    @SerialName("start")
    data class Start(
        val offset: Int,
    ) : Pin

    @Serializable
    @SerialName("end")
    data class End(
        val offset: Int,
    ) : Pin
}

object PinSerializer : OneOfSerializer<Pin>(Pin.serializer())
"""

/** io/github/nakulov/designsystem/DesignSystemModule.kt из каталога kotlinDefaults. */
private const val DEFAULTS_DESIGN_SYSTEM_MODULE_FILE = """// Сгенерировано генератором (generator/) из контрактов типа base. Не править.

package io.github.nakulov.designsystem

import kotlinx.serialization.modules.SerializersModule

/** Реализации базовых моделей библиотеки: продукт подключает `include(DesignSystemModule)`. */
val DesignSystemModule: SerializersModule = SerializersModule {
}
"""

/** io/github/nakulov/designsystem/layer/entity/Entity.kt из каталога kotlinDefaults. */
private fun makeDefaultsEntityFile(): String {
    return """// Сгенерировано генератором (generator/) из контракта layer/entity/Entity. Не править.

@file:UseSerializers(EntityModeSerializer::class, EntitySizeSerializer::class)

package io.github.nakulov.designsystem.layer.entity

import io.github.nakulov.designsystem.Rgba
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers

@Serializable
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

import kotlinx.serialization.Serializable

@Serializable
data object EntityEmpty
"""

/** io/github/nakulov/designsystem/layer/entity/EntityKeyboard.kt из каталога kotlinDefaults. */
private const val DEFAULTS_ENTITY_KEYBOARD_FILE = """// Сгенерировано генератором (generator/) из контракта layer/entity/EntityKeyboard. Не править.

@file:UseSerializers(EntityModeSerializer::class)

package io.github.nakulov.designsystem.layer.entity

import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers

@Serializable
data class EntityKeyboard(
    val type: EntityMode = EntityMode.Auto,
    val `in`: Boolean = false,
)
"""

/** io/github/nakulov/designsystem/layer/entity/EntityMode.kt из каталога kotlinDefaults. */
private const val DEFAULTS_ENTITY_MODE_FILE = """// Сгенерировано генератором (generator/) из контракта layer/entity/EntityMode. Не править.

@file:OptIn(ExperimentalSerializationApi::class)

package io.github.nakulov.designsystem.layer.entity

import io.github.nakulov.designsystem.serialization.OneOfSerializer
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonClassDiscriminator

@Serializable
@JsonClassDiscriminator("#variant")
sealed interface EntityMode {
    @Serializable
    @SerialName("auto")
    data object Auto : EntityMode

    @Serializable
    @SerialName("email")
    data object Email : EntityMode
}

object EntityModeSerializer : OneOfSerializer<EntityMode>(EntityMode.serializer())
"""

/** io/github/nakulov/designsystem/layer/entity/EntitySize.kt из каталога kotlinDefaults. */
private const val DEFAULTS_ENTITY_SIZE_FILE = """// Сгенерировано генератором (generator/) из контракта layer/entity/EntitySize. Не править.

@file:OptIn(ExperimentalSerializationApi::class)

package io.github.nakulov.designsystem.layer.entity

import io.github.nakulov.designsystem.serialization.OneOfSerializer
import io.github.nakulov.designsystem.serialization.ValueVariant
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonClassDiscriminator

@Serializable
@JsonClassDiscriminator("#variant")
sealed interface EntitySize {
    @Serializable
    @SerialName("zero")
    data object Zero : EntitySize

    @Serializable
    @SerialName("xs2")
    data object Xs2 : EntitySize

    @Serializable
    @SerialName("custom")
    data class Custom(
        val value: Int,
    ) : EntitySize

    @Serializable
    @SerialName("fixed")
    @ValueVariant
    data class Fixed(
        val value: Int,
    ) : EntitySize
}

object EntitySizeSerializer : OneOfSerializer<EntitySize>(EntitySize.serializer())
"""

/** io/github/nakulov/designsystem/DesignSystemModule.kt из каталога kotlinNames. */
private const val NAMES_DESIGN_SYSTEM_MODULE_FILE = """// Сгенерировано генератором (generator/) из контрактов типа base. Не править.

package io.github.nakulov.designsystem

import kotlinx.serialization.modules.SerializersModule

/** Реализации базовых моделей библиотеки: продукт подключает `include(DesignSystemModule)`. */
val DesignSystemModule: SerializersModule = SerializersModule {
}
"""

/** io/github/nakulov/designsystem/layer/icon/Icon.kt из каталога kotlinNames. */
private const val NAMES_ICON_FILE = """// Сгенерировано генератором (generator/) из контракта layer/icon/Icon. Не править.

@file:OptIn(ExperimentalSerializationApi::class)

package io.github.nakulov.designsystem.layer.icon

import io.github.nakulov.designsystem.serialization.OneOfSerializer
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonClassDiscriminator

@Serializable
@JsonClassDiscriminator("#variant")
sealed interface Icon {
    @Serializable
    @SerialName("close")
    data object Close : Icon
}

object IconSerializer : OneOfSerializer<Icon>(Icon.serializer())
"""

/** io/github/nakulov/designsystem/layer/one/One.kt из каталога kotlinNames. */
private const val NAMES_ONE_FILE = """// Сгенерировано генератором (generator/) из контракта layer/one/One. Не править.

@file:UseSerializers(IconSerializer::class, io.github.nakulov.designsystem.layer.other.IconSerializer::class)

package io.github.nakulov.designsystem.layer.one

import io.github.nakulov.designsystem.layer.icon.Icon
import io.github.nakulov.designsystem.layer.icon.IconSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers

@Serializable
data class One(
    val icon: Icon,
    val otherIcon: io.github.nakulov.designsystem.layer.other.Icon,
)
"""

/** io/github/nakulov/designsystem/layer/one/OneVariant.kt из каталога kotlinNames. */
private const val NAMES_ONE_VARIANT_FILE = """// Сгенерировано генератором (generator/) из контракта layer/one/OneVariant. Не править.

@file:OptIn(ExperimentalSerializationApi::class)
@file:UseSerializers(IconSerializer::class)

package io.github.nakulov.designsystem.layer.one

import io.github.nakulov.designsystem.layer.icon.IconSerializer
import io.github.nakulov.designsystem.serialization.OneOfSerializer
import io.github.nakulov.designsystem.serialization.ValueVariant
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import kotlinx.serialization.json.JsonClassDiscriminator

@Serializable
@JsonClassDiscriminator("#variant")
sealed interface OneVariant {
    @Serializable
    @SerialName("icon")
    data class Icon(
        val name: io.github.nakulov.designsystem.layer.icon.Icon,
    ) : OneVariant

    @Serializable
    @SerialName("string")
    @ValueVariant
    data class String(
        val value: kotlin.String,
    ) : OneVariant
}

object OneVariantSerializer : OneOfSerializer<OneVariant>(OneVariant.serializer())
"""

/** io/github/nakulov/designsystem/layer/other/Icon.kt из каталога kotlinNames. */
private const val NAMES_OTHER_ICON_FILE = """// Сгенерировано генератором (generator/) из контракта layer/other/Icon. Не править.

@file:OptIn(ExperimentalSerializationApi::class)

package io.github.nakulov.designsystem.layer.other

import io.github.nakulov.designsystem.serialization.OneOfSerializer
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonClassDiscriminator

@Serializable
@JsonClassDiscriminator("#variant")
sealed interface Icon {
    @Serializable
    @SerialName("open")
    data object Open : Icon
}

object IconSerializer : OneOfSerializer<Icon>(Icon.serializer())
"""

/** io/github/nakulov/designsystem/layer/two/Icon.kt из каталога kotlinNames. */
private const val NAMES_TWO_ICON_FILE = """// Сгенерировано генератором (generator/) из контракта layer/two/Icon. Не править.

@file:OptIn(ExperimentalSerializationApi::class)

package io.github.nakulov.designsystem.layer.two

import io.github.nakulov.designsystem.serialization.OneOfSerializer
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonClassDiscriminator

@Serializable
@JsonClassDiscriminator("#variant")
sealed interface Icon {
    @Serializable
    @SerialName("local")
    data object Local : Icon
}

object IconSerializer : OneOfSerializer<Icon>(Icon.serializer())
"""

/** io/github/nakulov/designsystem/layer/two/Two.kt из каталога kotlinNames. */
private const val NAMES_TWO_FILE = """// Сгенерировано генератором (generator/) из контракта layer/two/Two. Не править.

@file:UseSerializers(IconSerializer::class)

package io.github.nakulov.designsystem.layer.two

import io.github.nakulov.designsystem.layer.icon.IconSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers

@Serializable
data class Two(
    val icon: io.github.nakulov.designsystem.layer.icon.Icon,
)
"""

/** io/github/nakulov/designsystem/DesignSystemModule.kt из каталога primitives. */
private const val PRIMITIVES_DESIGN_SYSTEM_MODULE_FILE = """// Сгенерировано генератором (generator/) из контрактов типа base. Не править.

package io.github.nakulov.designsystem

import kotlinx.serialization.modules.SerializersModule

/** Реализации базовых моделей библиотеки: продукт подключает `include(DesignSystemModule)`. */
val DesignSystemModule: SerializersModule = SerializersModule {
}
"""

/** io/github/nakulov/designsystem/layer/entity/Entity.kt из каталога primitives. */
private const val PRIMITIVES_ENTITY_FILE = """// Сгенерировано генератором (generator/) из контракта layer/entity/Entity. Не править.

package io.github.nakulov.designsystem.layer.entity

import io.github.nakulov.designsystem.Payload
import io.github.nakulov.designsystem.Rgba
import kotlinx.serialization.Serializable

@Serializable
data class Entity(
    val text: String,
    val flag: Boolean,
    val count: Int,
    val ratio: Float,
    val tint: Rgba,
    val data: Payload,
)
"""

/** io/github/nakulov/designsystem/DesignSystemModule.kt из каталога empty. */
private const val EMPTY_DESIGN_SYSTEM_MODULE_FILE = """// Сгенерировано генератором (generator/) из контрактов типа base. Не править.

package io.github.nakulov.designsystem

import kotlinx.serialization.modules.SerializersModule

/** Реализации базовых моделей библиотеки: продукт подключает `include(DesignSystemModule)`. */
val DesignSystemModule: SerializersModule = SerializersModule {
}
"""

/** io/github/nakulov/designsystem/layer/entity/Entity.kt из каталога empty. */
private const val EMPTY_ENTITY_FILE = """// Сгенерировано генератором (generator/) из контракта layer/entity/Entity. Не править.

package io.github.nakulov.designsystem.layer.entity

import kotlinx.serialization.Serializable

@Serializable
data object Entity
"""

/** io/github/nakulov/designsystem/layer/entity/EntityEnum.kt из каталога empty. */
private const val EMPTY_ENTITY_ENUM_FILE = """// Сгенерировано генератором (generator/) из контракта layer/entity/EntityEnum. Не править.

@file:OptIn(ExperimentalSerializationApi::class)

package io.github.nakulov.designsystem.layer.entity

import io.github.nakulov.designsystem.serialization.OneOfSerializer
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonClassDiscriminator

@Serializable
@JsonClassDiscriminator("#variant")
sealed interface EntityEnum

object EntityEnumSerializer : OneOfSerializer<EntityEnum>(EntityEnum.serializer())
"""

/** io/github/nakulov/designsystem/layer/entity/EntityInline.kt из каталога empty. */
private const val EMPTY_ENTITY_INLINE_FILE = """// Сгенерировано генератором (generator/) из контракта layer/entity/EntityInline. Не править.

@file:OptIn(ExperimentalSerializationApi::class)

package io.github.nakulov.designsystem.layer.entity

import io.github.nakulov.designsystem.serialization.OneOfSerializer
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonClassDiscriminator

@Serializable
@JsonClassDiscriminator("#variant")
sealed interface EntityInline

object EntityInlineSerializer : OneOfSerializer<EntityInline>(EntityInline.serializer())
"""

/** io/github/nakulov/designsystem/layer/entity/EntityNone.kt из каталога empty. */
private const val EMPTY_ENTITY_NONE_FILE = """// Сгенерировано генератором (generator/) из контракта layer/entity/EntityNone. Не править.

@file:OptIn(ExperimentalSerializationApi::class)

package io.github.nakulov.designsystem.layer.entity

import io.github.nakulov.designsystem.serialization.OneOfSerializer
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonClassDiscriminator

@Serializable
@JsonClassDiscriminator("#variant")
sealed interface EntityNone

object EntityNoneSerializer : OneOfSerializer<EntityNone>(EntityNone.serializer())
"""

/** io/github/nakulov/designsystem/DesignSystemModule.kt из каталога kotlinEmptyBase. */
private const val EMPTY_BASE_MODULE_FILE = """// Сгенерировано генератором (generator/) из контрактов типа base. Не править.

package io.github.nakulov.designsystem

import io.github.nakulov.designsystem.layer.entity.Entity
import io.github.nakulov.designsystem.layer.entity.EntitySerializer
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic

/** Реализации базовых моделей библиотеки: продукт подключает `include(DesignSystemModule)`. */
val DesignSystemModule: SerializersModule = SerializersModule {
    contextual(Entity::class, EntitySerializer)
    polymorphic(Entity::class) {
    }
}
"""

/** io/github/nakulov/designsystem/layer/entity/Entity.kt из каталога kotlinEmptyBase. */
private const val EMPTY_BASE_ENTITY_FILE = """// Сгенерировано генератором (generator/) из контракта layer/entity/Entity. Не править.

package io.github.nakulov.designsystem.layer.entity

import io.github.nakulov.designsystem.serialization.BaseSerializer

interface Entity

object EntitySerializer : BaseSerializer<Entity>(Entity::class)
"""
