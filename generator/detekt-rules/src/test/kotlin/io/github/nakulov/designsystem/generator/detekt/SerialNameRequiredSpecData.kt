package io.github.nakulov.designsystem.generator.detekt

fun makeMissingSerialNameFindings(): List<Reported> {
    return listOf(
            Reported(line = 5, column = 9, message = "«b» без @SerialName."),
            Reported(line = 13, column = 5, message = "«TWO» без @SerialName."),
    )
}
