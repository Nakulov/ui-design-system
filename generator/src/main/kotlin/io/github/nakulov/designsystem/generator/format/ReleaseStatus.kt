package io.github.nakulov.designsystem.generator.format

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class ReleaseStatus {
    @SerialName("released")
    RELEASED,

    @SerialName("notReleased")
    NOT_RELEASED,

    @SerialName("willNotBeReleased")
    WILL_NOT_BE_RELEASED,
}
