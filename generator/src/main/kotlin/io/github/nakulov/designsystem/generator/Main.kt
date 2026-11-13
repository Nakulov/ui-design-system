package io.github.nakulov.designsystem.generator

import io.github.nakulov.designsystem.generator.output.kotlin.KotlinOutput
import io.github.nakulov.designsystem.generator.resolve.ContractResolutionException
import io.github.nakulov.designsystem.generator.resolve.ContractResolver
import kotlin.io.path.Path
import kotlin.io.path.createParentDirectories
import kotlin.io.path.writeText
import kotlin.system.exitProcess

/**
 * Точка входа: `<каталог contracts> [<каталог вывода>]`. Читает и разрешает все контракты; с каталогом вывода пишет в
 * него модели Kotlin. При ошибке — код выхода 1 и список ошибок.
 */
fun main(args: Array<String>) {
    if (args.size !in 1..2) {
        System.err.println(/* x = */ "Использование: generator <каталог contracts> [<каталог вывода>]")
        exitProcess(status = 2)
    }
    try {
        val model = ContractResolver.load(Path(args[0]))
        println(message = "Контракты разрешены: ${model.contracts.size}")
        val output = args.getOrNull(index = 1) ?: return
        val files = KotlinOutput.generate(model)
        for ((path, text) in files) {
            Path(output, path).createParentDirectories().writeText(text)
        }
        println(message = "Модели Kotlin: ${files.size} файлов в $output")
    } catch (e: ContractResolutionException) {
        System.err.println(e.message)
        exitProcess(status = 1)
    }
}
