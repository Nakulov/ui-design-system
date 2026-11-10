package io.github.nakulov.designsystem.generator.detekt

fun makeUnnamedLiteralFindings(): List<Reported> {
    return listOf(
            Reported(line = 5, column = 20, message = "Литерал \"x\" без имени: назовите параметр."),
            Reported(line = 12, column = 11, message = "Литерал \"Ann\" без имени: назовите параметр."),
            Reported(line = 12, column = 18, message = "Литерал 2 без имени: назовите параметр."),
            Reported(line = 12, column = 21, message = "Литерал true без имени: назовите параметр."),
            Reported(line = 12, column = 27, message = "Литерал '!' без имени: назовите параметр."),
            Reported(line = 12, column = 32, message = "Литерал null без имени: назовите параметр."),
            Reported(line = 13, column = 12, message = "Литерал -1 без имени: назовите параметр."),
    )
}

fun makeJavaLiteralFindings(): List<Reported> {
    return listOf(
            Reported(line = 2, column = 24, message = "Литерал \"x\" без имени: добавьте комментарий /* имя = */ (метод Java)."),
    )
}
