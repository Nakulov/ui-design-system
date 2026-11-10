package io.github.nakulov.designsystem.generator.detekt

import dev.detekt.api.Config
import dev.detekt.api.Entity
import dev.detekt.api.Finding
import dev.detekt.api.Rule
import org.jetbrains.kotlin.psi.KtClass

/** Тесты: спека — только `BehaviorSpec`. */
class BehaviorSpecOnly(
    config: Config,
) : Rule(
        config = config,
        description = "Спека Kotest — только BehaviorSpec.",
    ) {
    override fun visitClass(klass: KtClass) {
        super.visitClass(klass)
        val spec = klass.specSuperType() ?: return
        if (spec != "BehaviorSpec") {
            report(
                Finding(
                    entity = Entity.atName(klass),
                    message = "«${klass.name}» наследует $spec: спека — только BehaviorSpec.",
                ),
            )
        }
    }
}
