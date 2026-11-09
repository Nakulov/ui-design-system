package io.github.nakulov.designsystem.generator.format

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.JsonTransformingSerializer

/**
 * Файл контракта как он записан: specifications/contract.schema.json, schema-format.md, разделы 3–8.
 *
 * Классы пишутся руками: переименование поля формата — ошибка разбора или компиляции, а не молча пропущенное поле.
 * Ссылки здесь не разрешены, это делает [io.github.nakulov.designsystem.generator.model.ContractResolver].
 */
@Serializable
data class ContractFile(
    @SerialName("name")
    val name: String,
    @SerialName("type")
    val type: ContractType,
    @SerialName("description")
    val description: String,
    @SerialName("properties")
    val properties: List<PropertyDef>? = null,
    @SerialName("values")
    val values: List<EnumValueDef>? = null,
    @SerialName("oneOf")
    val oneOf: List<OneOfItemDef>? = null,
    @SerialName("extends")
    val extends: String? = null,
    @SerialName("appearanceContext")
    val appearanceContext: List<String>? = null,
    @SerialName("sample")
    val sample: JsonElement? = null,
    @SerialName("releaseVersions")
    val releaseVersions: ReleaseVersions? = null,
) {
    companion object {
        /** Строгий разбор: неизвестное поле — ошибка (раздел 3, «Другие поля в контракте не используются»). */
        private val json = Json { ignoreUnknownKeys = false }

        fun parse(text: String): ContractFile = json.decodeFromString(serializer(), text)
    }
}

@Serializable
enum class ContractType {
    @SerialName("object")
    OBJECT,

    @SerialName("enum")
    ENUM,

    @SerialName("oneOf")
    ONE_OF,
}

/** Тип свойства или элемента: примитивный тип (раздел 4.1) или `array` (раздел 5.1). */
@Serializable
enum class FormatType {
    @SerialName("string")
    STRING,

    @SerialName("boolean")
    BOOLEAN,

    @SerialName("integer")
    INTEGER,

    @SerialName("float")
    FLOAT,

    @SerialName("color")
    COLOR,

    @SerialName("payload")
    PAYLOAD,

    @SerialName("array")
    ARRAY,
}

/** Свойство объекта (раздел 5). */
@Serializable
data class PropertyDef(
    @SerialName("name")
    val name: String,
    @SerialName("description")
    val description: String,
    @SerialName("required")
    val required: Boolean,
    @SerialName("type")
    val type: FormatType? = null,
    @SerialName("\$ref")
    val ref: String? = null,
    @SerialName("items")
    val items: ArrayItemsDef? = null,
    @SerialName("uniqueVariants")
    val uniqueVariants: Boolean? = null,
    @SerialName("default")
    val default: JsonElement? = null,
    @SerialName("defaultsTo")
    val defaultsTo: String? = null,
    @SerialName("sample")
    val sample: JsonElement? = null,
    @SerialName("releaseVersions")
    val releaseVersions: ReleaseVersions? = null,
)

/** Тип элементов массива (раздел 5.1). */
@Serializable
data class ArrayItemsDef(
    @SerialName("type")
    val type: FormatType? = null,
    @SerialName("\$ref")
    val ref: String? = null,
)

/** Значение перечисления (раздел 4.4). */
@Serializable
data class EnumValueDef(
    @SerialName("name")
    val name: String,
    @SerialName("description")
    val description: String,
    @SerialName("releaseVersions")
    val releaseVersions: ReleaseVersions? = null,
)

/** Элемент `oneOf` (раздел 7). */
@Serializable
data class OneOfItemDef(
    @SerialName("\$ref")
    val ref: String? = null,
    @SerialName("type")
    val type: FormatType? = null,
    @SerialName("inline")
    val inline: Boolean? = null,
    @SerialName("name")
    val name: String? = null,
    @SerialName("sample")
    val sample: JsonElement? = null,
    @SerialName("releaseVersions")
    val releaseVersions: ReleaseVersions? = null,
)

/** Поддержка на платформах (раздел 8). */
@Serializable
data class ReleaseVersions(
    @SerialName("android")
    @Serializable(VersionedStatusSerializer::class)
    val android: VersionedStatus? = null,
    @SerialName("ios")
    @Serializable(VersionedStatusSerializer::class)
    val ios: VersionedStatus? = null,
    @SerialName("figma")
    val figma: ReleaseStatus? = null,
)

@Serializable
enum class ReleaseStatus {
    @SerialName("released")
    RELEASED,

    @SerialName("notReleased")
    NOT_RELEASED,

    @SerialName("willNotBeReleased")
    WILL_NOT_BE_RELEASED,
}

/** Статус с версией реализации (раздел 8.3). Запись строкой — статус без версии. */
@Serializable
data class VersionedStatus(
    @SerialName("status")
    val status: ReleaseStatus,
    @SerialName("version")
    val version: String? = null,
)

/** Статус строкой или объектом `{ "status", "version" }`. */
object VersionedStatusSerializer : JsonTransformingSerializer<VersionedStatus>(VersionedStatus.serializer()) {
    override fun transformDeserialize(element: JsonElement): JsonElement =
        if (element is JsonPrimitive) JsonObject(mapOf("status" to element)) else element
}
