package io.github.nakulov.designsystem.generator.detekt

fun makeTwoClassesFindings(): List<Reported> {
    return listOf(
            Reported(line = 5, column = 7, message = "«Second» — второй класс верхнего уровня в файле: вынесите в свой файл."),
            Reported(line = 7, column = 11, message = "«Third» — второй класс верхнего уровня в файле: вынесите в свой файл."),
    )
}
