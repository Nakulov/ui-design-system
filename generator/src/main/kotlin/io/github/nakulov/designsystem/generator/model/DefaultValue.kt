package io.github.nakulov.designsystem.generator.model

import kotlinx.serialization.json.JsonElement

/** Значение по умолчанию обязательного свойства (раздел 5). */
sealed interface DefaultValue {
    /** `default`: значение, записанное по разделу 4.5. */
    data class Value(val value: JsonElement) : DefaultValue

    /** `defaultsTo`: значение соседнего свойства того же объекта. */
    data class Sibling(val property: String) : DefaultValue
}
