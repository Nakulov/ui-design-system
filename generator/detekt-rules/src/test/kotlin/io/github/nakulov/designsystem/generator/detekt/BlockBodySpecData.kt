package io.github.nakulov.designsystem.generator.detekt

fun makeExpressionBodyFindings(): List<Reported> {
    return listOf(
            Reported(line = 1, column = 5, message = "У «name» тело-выражение: перепишите блоком с return."),
            Reported(line = 6, column = 18, message = "У «toString» тело-выражение: перепишите блоком с return."),
    )
}
