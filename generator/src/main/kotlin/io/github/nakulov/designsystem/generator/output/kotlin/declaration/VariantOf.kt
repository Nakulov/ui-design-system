package io.github.nakulov.designsystem.generator.output.kotlin.declaration

/** Вариант `oneOf`, классом которого стал объект: тип-владелец [owner] и имя варианта в записи [name]. */
data class VariantOf(
    val owner: String,
    val name: String,
)
