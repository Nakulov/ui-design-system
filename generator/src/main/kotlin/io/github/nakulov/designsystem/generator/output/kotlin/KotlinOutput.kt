package io.github.nakulov.designsystem.generator.output.kotlin

import io.github.nakulov.designsystem.generator.model.ContractModel
import io.github.nakulov.designsystem.generator.output.kotlin.declaration.KotlinDeclarations
import io.github.nakulov.designsystem.generator.output.kotlin.name.ROOT_PACKAGE
import io.github.nakulov.designsystem.generator.output.kotlin.type.KotlinTypes

/**
 * Модели Kotlin из разрешённой модели контрактов (#239): по правилам конструкций формата, а не по компонентам.
 *
 * Модуль `generator/models` собирает вывод в jar `io.github.nakulov:design-system-kotlin`.
 */
object KotlinOutput {
    /** Файлы вывода: путь от корня исходников → текст; по пути, по алфавиту. */
    fun generate(model: ContractModel): Map<String, String> {
        val types = KotlinTypes(model)
        val declarations = KotlinDeclarations(types)
        val files = types.topLevel.map { contract -> declarations.file(contract) } + sharedFiles()
        return files.sortedBy { file -> file.first }.toMap()
    }

    /** Общие типы, не зависящие от контрактов: `Rgba` и `Payload` (раздел 4.1). */
    private fun sharedFiles(): List<Pair<String, String>> {
        val directory = ROOT_PACKAGE.replace(oldChar = '.', newChar = '/')
        val header = "// Сгенерировано генератором (generator/). Не править.\n\npackage ${ROOT_PACKAGE}\n\n"
        val rgba = header + """
            |import kotlin.jvm.JvmInline
            |
            |/** Цвет sRGB с прозрачностью в порядке `0xRRGGBBAA` (`color`, раздел 4.1). */
            |@JvmInline
            |value class Rgba(
            |    val value: UInt,
            |)
            |
        """.trimMargin()
        val payload = header + """
            |/** Данные приложения (`payload`, раздел 4.1): приложение реализует своими моделями и регистрирует для разбора. */
            |interface Payload
            |
        """.trimMargin()
        return listOf("$directory/Rgba.kt" to rgba, "$directory/Payload.kt" to payload)
    }
}
