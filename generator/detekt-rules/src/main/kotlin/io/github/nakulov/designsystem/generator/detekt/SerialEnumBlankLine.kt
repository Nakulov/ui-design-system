package io.github.nakulov.designsystem.generator.detekt

import dev.detekt.api.Config
import dev.detekt.api.Entity
import dev.detekt.api.Finding
import dev.detekt.api.Rule
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtEnumEntry

/** DTO: значения `@Serializable enum` разделены пустой строкой; обычные `enum` не затрагиваются. */
class SerialEnumBlankLine(
    config: Config,
) : Rule(
        config = config,
        description = "DTO: значения @Serializable enum разделены пустой строкой.",
    ) {
    override fun visitClass(klass: KtClass) {
        super.visitClass(klass)
        if (!klass.isSerializable() || !klass.isEnum()) {
            return
        }
        val entries = klass.declarations.enumEntries()
        for ((previous, entry) in entries.zipWithNext()) {
            reportWithoutBlankLine(previous = previous, entry = entry)
        }
    }

    private fun reportWithoutBlankLine(previous: KtEnumEntry, entry: KtEnumEntry) {
        val between = entry.containingFile.text.substring(previous.textRange.endOffset, entry.textRange.startOffset)
        if (between.count { char -> char == '\n' } < 2) {
            report(
                Finding(
                    entity = Entity.atName(entry),
                    message = "Перед значением «${entry.name}» нет пустой строки.",
                ),
            )
        }
    }
}
