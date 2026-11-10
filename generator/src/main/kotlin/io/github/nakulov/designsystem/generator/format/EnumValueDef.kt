package io.github.nakulov.designsystem.generator.format

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

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
