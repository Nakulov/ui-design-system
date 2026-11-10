package io.github.nakulov.designsystem.generator.detekt

fun makeOptionalFirstFindings(): List<Reported> {
    return listOf(
            Reported(line = 6, column = 9, message = "Обязательное поле «b» после необязательного: перенесите выше."),
    )
}
