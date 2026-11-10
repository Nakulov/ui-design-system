package io.github.nakulov.designsystem.generator.detekt

fun makeFunSpecFindings(): List<Reported> {
    return listOf(
            Reported(line = 1, column = 7, message = "«ExampleSpec» наследует FunSpec: спека — только BehaviorSpec."),
    )
}
