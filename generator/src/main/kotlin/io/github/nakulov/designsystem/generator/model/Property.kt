package io.github.nakulov.designsystem.generator.model

import io.github.nakulov.designsystem.generator.format.ReleaseVersions
import kotlinx.serialization.json.JsonElement

/**
 * Свойство объекта (раздел 5). [declaredIn] — контракт, где свойство объявлено: у унаследованных свойств компонента
 * это базовая модель.
 */
data class Property(
    val name: String,
    val description: String,
    val type: PropertyType,
    val required: Boolean,
    val default: DefaultValue?,
    val sample: JsonElement?,
    val releaseVersions: ReleaseVersions?,
    val declaredIn: ContractId,
)
