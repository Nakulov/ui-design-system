package io.github.nakulov.designsystem.generator.detekt

import dev.detekt.api.Config
import dev.detekt.api.Entity
import dev.detekt.api.Finding
import dev.detekt.api.Rule
import org.jetbrains.kotlin.psi.KtAnnotated
import org.jetbrains.kotlin.psi.KtClass

/** DTO: каждая аннотация поля и значения `@Serializable enum` — на своей строке. */
class SerialAnnotationOnOwnLine(
    config: Config,
) : Rule(
        config = config,
        description = "DTO: аннотация и поле (значение enum) — на разных строках.",
    ) {
    override fun visitClass(klass: KtClass) {
        super.visitClass(klass)
        if (!klass.isSerializable()) {
            return
        }
        val members: List<KtAnnotated> = if (klass.isEnum()) klass.declarations.enumEntries() else klass.serialFields()
        for (member in members) {
            reportSameLine(member)
        }
    }

    private fun reportSameLine(member: KtAnnotated) {
        for (entry in member.annotationEntries.filterNot { annotation -> annotation.isFollowedByLineBreak() }) {
            report(
                Finding(
                    entity = Entity.from(entry),
                    message = "Аннотация ${entry.text} и следующее за ней — на одной строке: перенесите.",
                ),
            )
        }
    }
}
