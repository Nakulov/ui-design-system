// Общий класс генератора (generator/src/main/resources/shared/). Не править в выводе.

package io.github.nakulov.designsystem.serialization

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.PolymorphicKind
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.StructureKind
import kotlinx.serialization.descriptors.nonNullOriginal
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.floatOrNull
import kotlinx.serialization.modules.SerializersModule

private val colorPattern = Regex(pattern = "#[0-9A-F]{6}([0-9A-F]{2})?")
private val integerPattern = Regex(pattern = "-?[0-9]+")

/**
 * Строгая проверка записи по разделу 4.5 до разбора, по описаниям сериализаторов. Ошибка — [SerializationException]
 * с путём в JSON: `$.Container.children[2].Text.typography: неизвестный вариант «huge»`.
 *
 * Проверяет: неизвестный ключ, нет обязательного поля, неизвестный вариант или реализация, обёртку не из одного
 * ключа, `null`, пустой массив, повтор варианта во множестве, `integer` не 32-битное целое, `color` не
 * `#RRGGBB[AA]`, неверный тип значения. Данные `payload` не проверяет: их формат задаёт приложение.
 */
@OptIn(ExperimentalSerializationApi::class)
class ContractChecker(
    private val module: SerializersModule,
) {
    /** Проверка записи [element] корня с описанием [descriptor]. */
    fun check(element: JsonElement, descriptor: SerialDescriptor) {
        value(element, descriptor, path = "$")
    }

    private fun value(element: JsonElement, descriptor: SerialDescriptor, path: String) {
        if (element is JsonNull) fail(path, reason = "null не используется")
        when (val type = descriptor.nonNullOriginal) {
            is OneOfDescriptor -> oneOf(element, type, path, only = null)
            is NarrowedDescriptor -> oneOf(element, type.oneOf, path, only = type.variant)
            is BaseDescriptor -> base(element, type, path)
            else -> plain(element, type, path)
        }
    }

    private fun plain(element: JsonElement, descriptor: SerialDescriptor, path: String) {
        when (val kind = descriptor.kind) {
            StructureKind.CLASS, StructureKind.OBJECT -> fields(element, descriptor, path)
            StructureKind.LIST -> list(element, descriptor, path)
            is PrimitiveKind -> primitive(element, descriptor, kind, path)
            is PolymorphicKind -> Unit
            else -> Unit
        }
    }

    /** Строка — вариант без данных; объект с одним ключом — вариант с данными (раздел 4.5). */
    private fun oneOf(element: JsonElement, descriptor: OneOfDescriptor, path: String, only: String?) {
        val name = when {
            element is JsonPrimitive && element.isString -> element.content
            element is JsonObject && element.size == 1 -> element.keys.single()
            element is JsonObject -> fail(path, reason = "ожидается объект с одним ключом — именем варианта")
            else -> fail(path, reason = "ожидается имя варианта или объект с одним ключом")
        }
        val variant = descriptor.variants[name] ?: fail(path, reason = "неизвестный вариант «$name»")
        if (only != null && name != only) fail(path, reason = "допустим только вариант «$only», а не «$name»")
        val withoutData = variant.kind == StructureKind.OBJECT
        if (element is JsonPrimitive) {
            if (!withoutData) fail(path, reason = "вариант «$name» с данными записан строкой")
            return
        }
        val data = (element as JsonObject).getValue(name)
        val at = "$path.$name"
        when {
            withoutData -> fail(path, reason = "вариант «$name» без данных записывается строкой")
            variant.isValueVariant() -> value(data, variant.getElementDescriptor(index = 0), at)
            else -> fields(data, variant, at)
        }
    }

    /** Ссылка на базовую модель: объект с одним ключом — именем зарегистрированной реализации (раздел 5.2). */
    private fun base(element: JsonElement, descriptor: BaseDescriptor, path: String) {
        if (element !is JsonObject || element.size != 1) {
            fail(path, reason = "ожидается объект с одним ключом — именем реализации")
        }
        val (name, data) = element.entries.single()
        val implementation = module.getPolymorphic(descriptor.base, name)
            ?: fail(path, reason = "неизвестная реализация «$name»")
        fields(data, implementation.descriptor, "$path.$name")
    }

    /** Объект: только ключи контракта, все обязательные поля без значения по умолчанию (раздел 4.5, правило 2). */
    private fun fields(element: JsonElement, descriptor: SerialDescriptor, path: String) {
        if (element !is JsonObject) fail(path, reason = "ожидается объект")
        val names = (0 until descriptor.elementsCount).map { index -> descriptor.getElementName(index) }
        element.keys.firstOrNull { key -> key !in names }?.let { key -> fail(path, reason = "неизвестный ключ «$key»") }
        for ((index, name) in names.withIndex()) {
            val field = element[name]
            if (field == null && !descriptor.isElementOptional(index)) fail(path, reason = "нет обязательного поля «$name»")
            if (field != null) value(field, descriptor.getElementDescriptor(index), "$path.$name")
        }
    }

    /** Массив: не пустой (раздел 4.5, правило 5); множество — без повтора вариантов (раздел 5.1, правило 2). */
    private fun list(element: JsonElement, descriptor: SerialDescriptor, path: String) {
        if (element !is JsonArray) fail(path, reason = "ожидается массив")
        if (element.isEmpty()) fail(path, reason = "пустой массив не используется")
        val items = descriptor.getElementDescriptor(index = 0)
        element.forEachIndexed { index, item -> value(item, items, "$path[$index]") }
        if (descriptor.serialName.contains(other = "Set")) unique(element, path)
    }

    private fun unique(element: JsonArray, path: String) {
        val names = element.map { item -> if (item is JsonObject) item.keys.single() else (item as JsonPrimitive).content }
        names.groupBy { name -> name }.entries.firstOrNull { entry -> entry.value.size > 1 }?.let { entry ->
            fail(path, reason = "вариант «${entry.key}» повторяется")
        }
    }

    private fun primitive(element: JsonElement, descriptor: SerialDescriptor, kind: PrimitiveKind, path: String) {
        val primitive = element as? JsonPrimitive ?: fail(path, reason = "ожидается значение, а не ${kindOf(element)}")
        val valid = when (kind) {
            PrimitiveKind.STRING -> primitive.isString && isColor(descriptor, primitive)
            PrimitiveKind.BOOLEAN -> !primitive.isString && primitive.booleanOrNull != null
            PrimitiveKind.INT -> !primitive.isString && isInteger(primitive.content)
            PrimitiveKind.FLOAT -> !primitive.isString && primitive.floatOrNull != null
            else -> true
        }
        if (!valid) fail(path, reason = "${primitive} — не ${typeName(descriptor, kind)}")
    }

    private fun isColor(descriptor: SerialDescriptor, primitive: JsonPrimitive): Boolean {
        return descriptor.serialName != RGBA_SERIAL_NAME || colorPattern.matches(primitive.content)
    }

    private fun isInteger(content: String): Boolean {
        return integerPattern.matches(content) && content.toIntOrNull() != null
    }

    private fun typeName(descriptor: SerialDescriptor, kind: PrimitiveKind): String {
        return when {
            descriptor.serialName == RGBA_SERIAL_NAME -> "color (#RRGGBB или #RRGGBBAA, верхний регистр)"
            kind == PrimitiveKind.STRING -> "string"
            kind == PrimitiveKind.BOOLEAN -> "boolean"
            kind == PrimitiveKind.INT -> "integer (целое, 32 бита)"
            else -> "float"
        }
    }

    private fun kindOf(element: JsonElement): String {
        return if (element is JsonArray) "массив" else "объект"
    }

    private fun fail(path: String, reason: String): Nothing {
        throw SerializationException("$path: $reason")
    }
}
