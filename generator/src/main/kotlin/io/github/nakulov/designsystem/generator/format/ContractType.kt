package io.github.nakulov.designsystem.generator.format

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class ContractType {
    @SerialName("object")
    OBJECT,

    @SerialName("base")
    BASE,

    @SerialName("enum")
    ENUM,

    @SerialName("oneOf")
    ONE_OF,
}
