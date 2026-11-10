package io.github.nakulov.designsystem.generator.detekt

import dev.detekt.api.Config
import dev.detekt.api.Entity
import dev.detekt.api.Finding
import dev.detekt.api.Rule
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtNamedDeclaration
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtProperty

/** Тестовые данные `*SpecData.kt`: нет классов-спек, функции — `make…`, `val` — только `const val`. */
class SpecDataContent(
    config: Config,
) : Rule(
        config = config,
        description = "В *SpecData.kt нет классов-спек, функции — make…, val — только const val.",
    ) {
    override fun visitKtFile(file: KtFile) {
        super.visitKtFile(file)
        if (!file.name.endsWith(suffix = "SpecData.kt")) {
            return
        }
        for (declaration in file.declarations.filterIsInstance<KtNamedDeclaration>()) {
            val problem = when (declaration) {
                is KtClass -> declaration.specSuperType()?.let { spec -> "класс-спека ($spec) в файле данных" }
                is KtNamedFunction -> "функция без префикса make".takeUnless { declaration.isMake() }
                is KtProperty -> "val без const".takeUnless { declaration.hasModifier(KtTokens.CONST_KEYWORD) }
                else -> null
            }
            if (problem != null) {
                report(Finding(entity = Entity.atName(declaration), message = "«${declaration.name}»: $problem."))
            }
        }
    }

    private fun KtNamedFunction.isMake(): Boolean {
        return MAKE.matches(input = name.orEmpty())
    }

    private companion object {
        val MAKE = Regex(pattern = "make[A-Z].*")
    }
}
