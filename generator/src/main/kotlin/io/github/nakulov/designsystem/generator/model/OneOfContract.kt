package io.github.nakulov.designsystem.generator.model

import io.github.nakulov.designsystem.generator.format.ReleaseVersions
import kotlinx.serialization.json.JsonElement

/** Контракт типа `oneOf`: варианты после разворачивания (раздел 7). */
data class OneOfContract(
    override val id: ContractId,
    override val name: String,
    override val description: String,
    override val kind: ContractKind,
    override val releaseVersions: ReleaseVersions?,
    val variants: List<Variant>,
    val sample: JsonElement?,
) : ResolvedContract
