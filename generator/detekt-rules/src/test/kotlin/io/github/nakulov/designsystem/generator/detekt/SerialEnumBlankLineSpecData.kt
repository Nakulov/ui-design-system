package io.github.nakulov.designsystem.generator.detekt

fun makeAdjacentFindings(): List<Reported> {
    return listOf(
            Reported(line = 6, column = 5, message = "Перед значением «TWO» нет пустой строки."),
    )
}
