package io.github.nakulov.designsystem.generator.detekt

fun makeFieldSerializerFindings(): List<Reported> {
    return listOf(
            Reported(line = 4, column = 5, message = "@Serializable(with = StatusSerializer::class) на поле: зарегистрируйте сериализатор через contextual."),
        Reported(
            line = 7,
            column = 5,
            message = "@Serializable(with = StatusSerializer::class) на поле: зарегистрируйте сериализатор через contextual.",
        ),
    )
}
