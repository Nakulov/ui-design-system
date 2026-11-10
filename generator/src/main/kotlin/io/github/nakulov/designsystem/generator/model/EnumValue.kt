package io.github.nakulov.designsystem.generator.model

import io.github.nakulov.designsystem.generator.format.ReleaseVersions

data class EnumValue(
    val name: String,
    val description: String,
    val releaseVersions: ReleaseVersions?,
)
