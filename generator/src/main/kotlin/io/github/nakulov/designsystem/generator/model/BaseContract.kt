package io.github.nakulov.designsystem.generator.model

import io.github.nakulov.designsystem.generator.format.ReleaseVersions

/**
 * Контракт типа `base` — базовая модель (раздел 4.2): свойства, общие для реализаций, контрактов с `extends`
 * (раздел 5.2). Набор реализаций открыт; ссылка на базовую модель принимает любую реализацию (раздел 4.5).
 */
data class BaseContract(
    override val id: ContractId,
    override val name: String,
    override val description: String,
    override val kind: ContractKind,
    override val releaseVersions: ReleaseVersions?,
    val properties: List<Property>,
) : ResolvedContract
