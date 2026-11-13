// Общий класс генератора (generator/src/main/resources/shared/). Не править в выводе.

package io.github.nakulov.designsystem.serialization

import kotlin.reflect.KClass
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject

/**
 * Ссылка на базовую модель (раздел 5.2): объект с одним ключом — именем реализации, `{ "Text": { … } }`. Реализации
 * ищутся в `polymorphic([base])` модуля сериализации: библиотеки — `DesignSystemModule`, продукта — его `subclass`.
 */
abstract class BaseSerializer<T : Any>(
    private val base: KClass<T>,
) : KSerializer<T> {
    override val descriptor: SerialDescriptor = BaseDescriptor(base)

    override fun deserialize(decoder: Decoder): T {
        val input = decoder as JsonDecoder
        val element = input.decodeJsonElement()
        return input.decodeChecked(descriptor, element) { json ->
            val (name, data) = element.jsonObject.entries.single()
            val implementation = json.serializersModule.getPolymorphic(base, name)
                ?: throw SerializationException("Реализация «$name» не зарегистрирована")
            json.decodeFromJsonElement(implementation, data)
        }
    }

    override fun serialize(encoder: Encoder, value: T) {
        val output = encoder as JsonEncoder
        val implementation = output.serializersModule.getPolymorphic(base, value)
            ?: throw SerializationException("Реализация ${value::class} не зарегистрирована")
        @Suppress("UNCHECKED_CAST")
        val serializer = implementation as KSerializer<T>
        val data = output.json.encodeToJsonElement(serializer, value)
        output.encodeJsonElement(JsonObject(mapOf(serializer.descriptor.serialName to data)))
    }
}
