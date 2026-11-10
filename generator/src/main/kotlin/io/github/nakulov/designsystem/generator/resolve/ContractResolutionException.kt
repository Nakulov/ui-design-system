package io.github.nakulov.designsystem.generator.resolve

/** Ошибки чтения и разрешения контрактов: все найденные сразу, по строке на ошибку. */
class ContractResolutionException(
    val errors: List<String>,
) : IllegalStateException(errors.joinToString("\n", prefix = "Контракты не разрешены:\n"))
