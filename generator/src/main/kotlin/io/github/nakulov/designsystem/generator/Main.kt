package io.github.nakulov.designsystem.generator

import io.github.nakulov.designsystem.generator.resolve.ContractResolutionException
import io.github.nakulov.designsystem.generator.resolve.ContractResolver
import kotlin.io.path.Path
import kotlin.system.exitProcess

/** Точка входа: `<каталог contracts>`. Читает и разрешает все контракты; при ошибке — код выхода 1 и список ошибок. */
fun main(args: Array<String>) {
    val root = args.singleOrNull()
    if (root == null) {
        System.err.println(/* x = */ "Использование: generator <каталог contracts>")
        exitProcess(status = 2)
    }
    try {
        val model = ContractResolver.load(Path(root))
        println(message = "Контракты разрешены: ${model.contracts.size}")
    } catch (e: ContractResolutionException) {
        System.err.println(e.message)
        exitProcess(status = 1)
    }
}
