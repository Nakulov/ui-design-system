package io.github.nakulov.designsystem.generator.format

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

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
