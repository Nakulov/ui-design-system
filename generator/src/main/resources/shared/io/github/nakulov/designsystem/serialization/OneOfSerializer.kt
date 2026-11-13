// Общий класс генератора (generator/src/main/resources/shared/). Не править в выводе.

package io.github.nakulov.designsystem.serialization

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.StructureKind
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/** Ключ, под которым `sealed`-сериализатор плагина пишет имя варианта (`@JsonClassDiscriminator` в выводе). */
const val VARIANT_KEY = "#variant"

private const val VALUE = "value"

/**
 * Запись `oneOf` (раздел 4.5) — одно правило для всех типов: строка — вариант без данных (`"hug"`), объект с одним
 * ключом — вариант с данными (`{ "fixed": "m" }`, `{ "custom": { "value": 6 } }`).
 *
 * Варианты и их данные берутся из `sealed`-сериализатора плагина [sealed]: запись переводится в его форму с ключом
 * [VARIANT_KEY] и обратно. Перед разбором запись проверяется целиком ([decodeChecked]).
 */
abstract class OneOfSerializer<T : Any>(
    private val sealed: KSerializer<T>,
) : KSerializer<T> {
    override val descriptor: SerialDescriptor = OneOfDescriptor(sealed.descriptor)

    private val variants: Map<String, SerialDescriptor> = (descriptor as OneOfDescriptor).variants

    override fun deserialize(decoder: Decoder): T {
        val input = decoder as JsonDecoder
        val element = input.decodeJsonElement()
        return input.decodeChecked(descriptor, element) { json -> json.decodeFromJsonElement(sealed, tagged(element)) }
    }

    override fun serialize(encoder: Encoder, value: T) {
        val output = encoder as JsonEncoder
        val tagged = output.json.encodeToJsonElement(sealed, value).jsonObject
        val name = tagged.getValue(VARIANT_KEY).jsonPrimitive.content
        val data = JsonObject(tagged - VARIANT_KEY)
        val variant = variants.getValue(name)
        val element = when {
            variant.kind == StructureKind.OBJECT -> JsonPrimitive(name)
            variant.isValueVariant() -> JsonObject(mapOf(name to data.getValue(VALUE)))
            else -> JsonObject(mapOf(name to data))
        }
        output.encodeJsonElement(element)
    }

    /** Запись в форме плагина: `{ "#variant": "fixed", "value": "m" }`. Запись уже проверена. */
    private fun tagged(element: JsonElement): JsonObject {
        if (element is JsonPrimitive) return JsonObject(mapOf(VARIANT_KEY to element))
        val (name, data) = element.jsonObject.entries.single()
        val fields = if (variants.getValue(name).isValueVariant()) mapOf(VALUE to data) else data.jsonObject
        return JsonObject(mapOf(VARIANT_KEY to JsonPrimitive(name)) + fields)
    }
}
