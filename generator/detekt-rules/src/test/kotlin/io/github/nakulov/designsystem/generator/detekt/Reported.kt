package io.github.nakulov.designsystem.generator.detekt

/** Найденное правилом в сравнимом виде: `Finding` без `equals` (внутри элемент PSI). */
data class Reported(
    val line: Int,
    val column: Int,
    val message: String,
)
