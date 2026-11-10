package io.github.nakulov.designsystem.generator.detekt

import dev.detekt.api.Config
import dev.detekt.api.Entity
import dev.detekt.api.Finding
import dev.detekt.api.Rule
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtNamedDeclaration

/** DTO: `@SerialName` у каждого поля `@Serializable`-класса и значения `@Serializable enum`. */
class SerialNameRequired(
    config: Config,
) : Rule(
        config = config,
        description = "DTO: у каждого поля и значения enum — @SerialName с именем в JSON.",
    ) {
    override fun visitClass(klass: KtClass) {
        super.visitClass(klass)
        if (!klass.isSerializable()) {
            return
        }
        val members: List<KtNamedDeclaration> =
            if (klass.isEnum()) klass.declarations.enumEntries() else klass.serialFields()
        for (member in members.filter { declaration -> declaration.annotations(name = "SerialName").isEmpty() }) {
            report(
                Finding(
                    entity = Entity.atName(member),
                    message = "«${member.name}» без @SerialName.",
                ),
            )
        }
    }
}
