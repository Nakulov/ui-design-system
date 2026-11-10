package io.github.nakulov.designsystem.generator.format

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Статус с версией реализации (раздел 8.3). Запись строкой — статус без версии. */
@Serializable
data class VersionedStatus(
    @SerialName("status")
    val status: ReleaseStatus,
    @SerialName("version")
    val version: String? = null,
)
