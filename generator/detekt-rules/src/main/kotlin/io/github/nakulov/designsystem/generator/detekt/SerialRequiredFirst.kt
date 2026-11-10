package io.github.nakulov.designsystem.generator.detekt

import dev.detekt.api.Config
import dev.detekt.api.Entity
import dev.detekt.api.Finding
import dev.detekt.api.Rule
import org.jetbrains.kotlin.psi.KtClass

/** DTO: сначала обязательные поля, затем необязательные (со значением по умолчанию). */
class SerialRequiredFirst(
    config: Config,
) : Rule(
        config = config,
        description = "DTO: обязательные поля перед необязательными `? = null`.",
    ) {
    override fun visitClass(klass: KtClass) {
        super.visitClass(klass)
        val fields = klass.serialFields()
        val firstOptional = fields.indexOfFirst { field -> field.hasDefaultValue() }
        if (firstOptional < 0) {
            return
        }
        for (field in fields.drop(n = firstOptional).filterNot { field -> field.hasDefaultValue() }) {
            report(
                Finding(
                    entity = Entity.atName(field),
                    message = "Обязательное поле «${field.name}» после необязательного: перенесите выше.",
                ),
            )
        }
    }
}
