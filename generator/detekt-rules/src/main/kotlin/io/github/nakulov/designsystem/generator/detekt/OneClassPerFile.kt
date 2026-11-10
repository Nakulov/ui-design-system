package io.github.nakulov.designsystem.generator.detekt

import dev.detekt.api.Config
import dev.detekt.api.Entity
import dev.detekt.api.Finding
import dev.detekt.api.Rule
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtFile

/** Один класс, объект или интерфейс верхнего уровня на файл; вложенные не считаются. */
class OneClassPerFile(
    config: Config,
) : Rule(
        config = config,
        description = "Один класс на файл: второй класс верхнего уровня — в свой файл или внутрь первого.",
    ) {
    override fun visitKtFile(file: KtFile) {
        super.visitKtFile(file)
        val declarations = file.declarations.filterIsInstance<KtClassOrObject>()
        for (declaration in declarations.drop(n = 1)) {
            report(
                Finding(
                    entity = Entity.atName(declaration),
                    message = "«${declaration.name}» — второй класс верхнего уровня в файле: вынесите в свой файл.",
                ),
            )
        }
    }
}
