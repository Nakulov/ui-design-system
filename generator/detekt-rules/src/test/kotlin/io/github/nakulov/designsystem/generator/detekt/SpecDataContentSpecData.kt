package io.github.nakulov.designsystem.generator.detekt

fun makeSpecDataFindings(): List<Reported> {
    return listOf(
            Reported(line = 5, column = 5, message = "«shared»: val без const."),
            Reported(line = 7, column = 5, message = "«id»: функция без префикса make."),
            Reported(line = 11, column = 5, message = "«makeup»: функция без префикса make."),
            Reported(line = 15, column = 7, message = "«ExampleSpec»: класс-спека (BehaviorSpec) в файле данных."),
    )
}
