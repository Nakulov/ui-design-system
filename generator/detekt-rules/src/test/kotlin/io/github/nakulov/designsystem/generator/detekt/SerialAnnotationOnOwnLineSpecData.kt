package io.github.nakulov.designsystem.generator.detekt

fun makeSameLineFindings(): List<Reported> {
    return listOf(
            Reported(line = 3, column = 5, message = "Аннотация @SerialName(\"a\") и следующее за ней — на одной строке: перенесите."),
            Reported(line = 10, column = 5, message = "Аннотация @SerialName(\"one\") и следующее за ней — на одной строке: перенесите."),
    )
}
