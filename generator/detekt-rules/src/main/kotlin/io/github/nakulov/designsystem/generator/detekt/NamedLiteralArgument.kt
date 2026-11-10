package io.github.nakulov.designsystem.generator.detekt

import com.intellij.psi.PsiComment
import com.intellij.psi.util.PsiTreeUtil
import dev.detekt.api.Config
import dev.detekt.api.Entity
import dev.detekt.api.Finding
import dev.detekt.api.RequiresAnalysisApi
import dev.detekt.api.Rule
import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.analysis.api.resolution.singleFunctionCallOrNull
import org.jetbrains.kotlin.analysis.api.resolution.symbol
import org.jetbrains.kotlin.psi.KtCallElement
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtConstantExpression
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtLambdaArgument
import org.jetbrains.kotlin.psi.KtPrefixExpression
import org.jetbrains.kotlin.psi.KtStringTemplateExpression
import org.jetbrains.kotlin.psi.KtSuperTypeCallEntry
import org.jetbrains.kotlin.psi.KtValueArgument

/**
 * Литерал в аргументе вызова (число, `Boolean`, `Char`, строка, `null`) — именованный. У вызова Java-метода имена
 * параметров не стабильны: перед литералом комментарий `/* имя = */`. Не затрагиваются аргументы аннотаций,
 * `vararg` и инфиксные вызовы.
 */
class NamedLiteralArgument(
    config: Config,
) : Rule(
        config = config,
        description = "Литерал в аргументе вызова — с именем параметра.",
    ),
    RequiresAnalysisApi {
    override fun visitCallExpression(expression: KtCallExpression) {
        super.visitCallExpression(expression)
        reportUnnamedLiterals(expression)
    }

    override fun visitSuperTypeCallEntry(call: KtSuperTypeCallEntry) {
        super.visitSuperTypeCallEntry(call)
        reportUnnamedLiterals(call)
    }

    private fun reportUnnamedLiterals(call: KtCallElement) {
        val literals = call.valueArguments.filterIsInstance<KtValueArgument>().filter { argument ->
            !argument.isNamed() && argument !is KtLambdaArgument && argument.getArgumentExpression().isLiteral()
        }
        if (literals.isEmpty()) {
            return
        }
        analyze(call) {
            val functionCall = call.resolveToCall()?.singleFunctionCallOrNull() ?: return@analyze
            val stableNames = functionCall.symbol.hasStableParameterNames
            for (argument in literals) {
                val parameter = functionCall.valueArgumentMapping[argument.getArgumentExpression()]
                if (parameter != null && !parameter.symbol.isVararg) {
                    reportArgument(argument = argument, stableNames = stableNames)
                }
            }
        }
    }

    private fun reportArgument(argument: KtValueArgument, stableNames: Boolean) {
        if (!stableNames && argument.hasNameComment()) {
            return
        }
        val hint = if (stableNames) "назовите параметр" else "добавьте комментарий /* имя = */ (метод Java)"
        report(
            Finding(
                entity = Entity.from(argument),
                message = "Литерал ${argument.text} без имени: $hint.",
            ),
        )
    }

    private fun KtExpression?.isLiteral(): Boolean {
        return when (this) {
            is KtConstantExpression, is KtStringTemplateExpression -> true
            is KtPrefixExpression -> baseExpression is KtConstantExpression
            else -> false
        }
    }

    private fun KtValueArgument.hasNameComment(): Boolean {
        val comment = firstChild as? PsiComment ?: PsiTreeUtil.skipWhitespacesBackward(this) as? PsiComment
        return comment != null && NAME_COMMENT.matches(input = comment.text)
    }

    private companion object {
        val NAME_COMMENT = Regex(pattern = """/\*\s*\w+\s*=\s*\*/""")
    }
}
