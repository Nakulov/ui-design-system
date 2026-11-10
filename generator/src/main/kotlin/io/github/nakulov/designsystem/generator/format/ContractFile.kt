package io.github.nakulov.designsystem.generator.format

import io.github.nakulov.designsystem.generator.format.json.ContractJson
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/**
 * Файл контракта как он записан: specifications/contract.schema.json, schema-format.md, разделы 3–8.
 *
 * Классы пишутся руками: переименование поля формата — ошибка разбора или компиляции, а не молча пропущенное поле.
 * Ссылки здесь не разрешены, это делает [io.github.nakulov.designsystem.generator.resolve.ContractResolver].
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
        fun parse(text: String): ContractFile {
            return ContractJson.decodeFromString<ContractFile>(text)
        }
    }
}
