package io.github.nakulov.designsystem.generator.model

import io.github.nakulov.designsystem.generator.format.ReleaseVersions
import kotlinx.serialization.json.JsonElement

/**
 * Вариант `oneOf` после разворачивания (раздел 7). Имя — имя варианта в записи значения (раздел 4.5).
 * [description] есть у варианта из развёрнутого перечисления — описание значения.
 */
data class Variant(
    val name: String,
    val description: String?,
    val data: VariantData,
    val sample: JsonElement?,
    val releaseVersions: ReleaseVersions?,
)
