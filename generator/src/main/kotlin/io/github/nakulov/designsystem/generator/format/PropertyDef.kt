package io.github.nakulov.designsystem.generator.format

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

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
