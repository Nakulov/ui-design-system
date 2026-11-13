package io.github.nakulov.designsystem.generator.output.kotlin.declaration

import io.github.nakulov.designsystem.generator.model.BaseContract
import io.github.nakulov.designsystem.generator.model.EnumContract
import io.github.nakulov.designsystem.generator.model.ObjectContract
import io.github.nakulov.designsystem.generator.model.OneOfContract
import io.github.nakulov.designsystem.generator.model.PrimitiveType
import io.github.nakulov.designsystem.generator.model.PropertyType
import io.github.nakulov.designsystem.generator.model.TypeRef
import io.github.nakulov.designsystem.generator.model.VariantData
import io.github.nakulov.designsystem.generator.output.kotlin.name.KotlinNames
import io.github.nakulov.designsystem.generator.output.kotlin.name.KotlinTypeName
import io.github.nakulov.designsystem.generator.output.kotlin.type.KotlinTypeRenderer
import io.github.nakulov.designsystem.generator.output.kotlin.type.KotlinTypes
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/** Цифр в записи `#RRGGBBAA`. */
private const val RGBA_DIGITS = 8

/**
 * Значение `default` (запись по разделу 4.5) выражением Kotlin: `Padding.All(Spacing.Zero)`, `Keyboard()`.
 *
 * Запись проверяет tools/validate.py; значение не по контракту — ошибка генерации.
 */
class KotlinDefaults(
    private val types: KotlinTypes,
    private val renderer: KotlinTypeRenderer,
) {
    /** Выражение значения [json] свойства типа [type] в месте, где видны вложенные типы [shadowed]. */
    fun property(json: JsonElement, type: PropertyType, shadowed: Set<String>): String {
        val single = type as? PropertyType.Single ?: error(message = "default у массива: $json")
        return value(json, single.type, shadowed)
    }

    private fun value(json: JsonElement, ref: TypeRef, shadowed: Set<String>): String {
        return when (ref) {
            is TypeRef.Primitive -> primitive(json.jsonPrimitive, ref.type, shadowed)
            is TypeRef.Contract -> contract(json, ref, shadowed)
        }
    }

    private fun contract(json: JsonElement, ref: TypeRef.Contract, shadowed: Set<String>): String {
        val name = types.typeName(ref.id)
        return when (val contract = types.contract(ref.id)) {
            is EnumContract -> enumValue(name, json.jsonPrimitive, shadowed)

            is OneOfContract -> oneOf(json, contract, shadowed)

            is ObjectContract -> construct(
                name = name,
                contract = contract,
                json = json.jsonObject,
                shadowed = shadowed,
            )

            is BaseContract -> error(message = "${ref.id}: default у ссылки на базовую модель")
        }
    }

    private fun enumValue(name: KotlinTypeName, json: JsonPrimitive, shadowed: Set<String>): String {
        return renderer.ref(name.nested(KotlinNames.typeName(json.content)), shadowed)
    }

    /** Строка — вариант из развёрнутого перечисления, объект с одним ключом — вариант с данными. */
    private fun oneOf(json: JsonElement, contract: OneOfContract, shadowed: Set<String>): String {
        val owner = types.typeName(contract.id)
        if (json is JsonPrimitive) return enumValue(owner, json, shadowed)
        val (key, content) = json.jsonObject.entries.single()
        val variant = contract.variants.single { candidate -> candidate.name == key }
        val name = owner.nested(KotlinNames.typeName(key))
        return when (val data = variant.data) {
            is VariantData.Value -> "${renderer.ref(name, shadowed)}(${value(content, data.type, shadowed)})"

            is VariantData.Fields -> {
                val fields = types.contract(data.contract) as ObjectContract
                construct(name = name, contract = fields, json = content.jsonObject, shadowed = shadowed)
            }

            is VariantData.Unfolded -> error(message = "${contract.id}: вариант «$key» без данных записан объектом")
        }
    }

    /** Объект: указанные поля — именованными аргументами в порядке контракта, остальные берут свои умолчания. */
    private fun construct(
        name: KotlinTypeName,
        contract: ObjectContract,
        json: JsonObject,
        shadowed: Set<String>,
    ): String {
        val type = renderer.ref(name, shadowed)
        if (contract.properties.isEmpty()) return type
        val arguments = contract.properties.filter { property -> property.name in json }.map { property ->
            val expression = property(json.getValue(property.name), property.type, shadowed)
            "${KotlinNames.propertyName(property.name)} = $expression"
        }
        return "$type(${arguments.joinToString(separator = ", ")})"
    }

    private fun primitive(json: JsonPrimitive, type: PrimitiveType, shadowed: Set<String>): String {
        return when (type) {
            PrimitiveType.STRING -> quote(json.content)
            PrimitiveType.BOOLEAN, PrimitiveType.INTEGER -> json.content
            PrimitiveType.FLOAT -> "${json.content}f"
            PrimitiveType.COLOR -> "${renderer.ref(KotlinNames.rgba, shadowed)}(${rgba(json.content)})"
            PrimitiveType.PAYLOAD -> error(message = "default у payload: ${json.content}")
        }
    }

    /** `#RRGGBB` → `0xRRGGBBFFu`, `#RRGGBBAA` → `0xRRGGBBAAu` (раздел 4.5, правило 6). */
    private fun rgba(color: String): String {
        val hex = color.removePrefix(prefix = "#")
        return "0x${hex.padEnd(length = RGBA_DIGITS, padChar = 'F')}u"
    }

    private fun quote(text: String): String {
        val escaped = text.replace(oldValue = "\\", newValue = "\\\\")
            .replace(oldValue = "\"", newValue = "\\\"")
            .replace(oldValue = "$", newValue = "\\$")
            .replace(oldValue = "\n", newValue = "\\n")
        return "\"$escaped\""
    }
}
