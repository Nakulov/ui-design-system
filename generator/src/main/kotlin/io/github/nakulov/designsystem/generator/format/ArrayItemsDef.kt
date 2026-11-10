package io.github.nakulov.designsystem.generator.format

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Тип элементов массива (раздел 5.1). */
@Serializable
data class ArrayItemsDef(
    @SerialName("type")
    val type: FormatType? = null,
    @SerialName("\$ref")
    val ref: String? = null,
)
