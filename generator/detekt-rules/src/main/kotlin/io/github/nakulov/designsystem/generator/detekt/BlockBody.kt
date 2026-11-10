package io.github.nakulov.designsystem.generator.detekt

import dev.detekt.api.Config
import dev.detekt.api.Entity
import dev.detekt.api.Finding
import dev.detekt.api.Rule
import org.jetbrains.kotlin.psi.KtNamedFunction

/** Тело функции — блок с `return`, у всех функций; тело-выражение запрещено. */
class BlockBody(
    config: Config,
) : Rule(
        config = config,
        description = "Тело функции — блок с return, не выражение.",
    ) {
    override fun visitNamedFunction(function: KtNamedFunction) {
        super.visitNamedFunction(function)
        if (function.hasBody() && !function.hasBlockBody()) {
            report(
                Finding(
                    entity = Entity.atName(function),
                    message = "У «${function.name}» тело-выражение: перепишите блоком с return.",
                ),
            )
        }
    }
}
