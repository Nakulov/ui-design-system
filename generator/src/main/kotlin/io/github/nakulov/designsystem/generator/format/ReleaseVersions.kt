// Поля VersionedStatus разбираются сериализатором из ContractJson, а не сгенерированным.
@file:UseContextualSerialization(VersionedStatus::class)

package io.github.nakulov.designsystem.generator.format

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseContextualSerialization

/** Поддержка на платформах (раздел 8). */
@Serializable
data class ReleaseVersions(
    @SerialName("android")
    val android: VersionedStatus? = null,
    @SerialName("ios")
    val ios: VersionedStatus? = null,
    @SerialName("figma")
    val figma: ReleaseStatus? = null,
)
