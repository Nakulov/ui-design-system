package io.github.nakulov.designsystem.generator.detekt

fun makeImplicitItFindings(): List<Reported> {
    return listOf(
            Reported(line = 2, column = 22, message = "Лямбда с неявным it: назовите параметр (`{ item -> … }`)."),
    )
}

fun makeNestedItFindings(): List<Reported> {
    return listOf(
            Reported(line = 2, column = 44, message = "Лямбда с неявным it: назовите параметр (`{ item -> … }`)."),
    )
}
