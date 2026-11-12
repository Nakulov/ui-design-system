package io.github.nakulov.designsystem.generator.model

import io.github.nakulov.designsystem.generator.format.ReleaseVersions

/**
 * Контракт типа `object`. У реализации базовой модели [base] — контракт типа `base` по `extends`, его свойства идут в
 * [properties] первыми (раздел 5.2, правило 1), [appearanceContext] — имена вариантов `Appearance`.
 */
data class ObjectContract(
    override val id: ContractId,
    override val name: String,
    override val description: String,
    override val kind: ContractKind,
    override val releaseVersions: ReleaseVersions?,
    val base: ContractId?,
    val appearanceContext: List<String>,
    val properties: List<Property>,
) : ResolvedContract
