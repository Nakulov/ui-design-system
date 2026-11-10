package io.github.nakulov.designsystem.generator.model

import io.github.nakulov.designsystem.generator.format.ReleaseVersions

sealed interface ResolvedContract {
    val id: ContractId
    val name: String
    val description: String
    val kind: ContractKind
    val releaseVersions: ReleaseVersions?
}
