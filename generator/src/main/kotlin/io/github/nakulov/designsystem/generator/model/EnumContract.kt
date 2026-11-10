package io.github.nakulov.designsystem.generator.model

import io.github.nakulov.designsystem.generator.format.ReleaseVersions
import kotlinx.serialization.json.JsonElement

/** Контракт типа `enum` (раздел 4.4). */
data class EnumContract(
    override val id: ContractId,
    override val name: String,
    override val description: String,
    override val kind: ContractKind,
    override val releaseVersions: ReleaseVersions?,
    val values: List<EnumValue>,
    val sample: JsonElement?,
) : ResolvedContract
