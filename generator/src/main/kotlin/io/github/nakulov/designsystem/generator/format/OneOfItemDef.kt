package io.github.nakulov.designsystem.generator.format

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

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
