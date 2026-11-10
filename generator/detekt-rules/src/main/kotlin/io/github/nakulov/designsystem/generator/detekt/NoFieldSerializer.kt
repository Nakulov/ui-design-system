package io.github.nakulov.designsystem.generator.detekt

import dev.detekt.api.Config
import dev.detekt.api.Entity
import dev.detekt.api.Finding
import dev.detekt.api.Rule
import org.jetbrains.kotlin.psi.KtAnnotated
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.psi.KtProperty

/** DTO: нет `@Serializable(with = …)` на поле: сериализатор регистрирует `Json` формата через `contextual`. */
class NoFieldSerializer(
    config: Config,
) : Rule(
        config = config,
        description = "DTO: сериализатор не указывается у поля, только contextual в Json формата.",
    ) {
    override fun visitParameter(parameter: KtParameter) {
        super.visitParameter(parameter)
        reportFieldSerializer(parameter)
    }

    override fun visitProperty(property: KtProperty) {
        super.visitProperty(property)
        reportFieldSerializer(property)
    }

    private fun reportFieldSerializer(field: KtAnnotated) {
        for (entry in field.annotations(name = "Serializable")) {
            report(
                Finding(
                    entity = Entity.from(entry),
                    message = "${entry.text} на поле: зарегистрируйте сериализатор через contextual.",
                ),
            )
        }
    }
}
