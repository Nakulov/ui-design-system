package io.github.nakulov.designsystem.generator.detekt

import dev.detekt.api.Config
import dev.detekt.api.Entity
import dev.detekt.api.Finding
import dev.detekt.api.Rule
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.psi.KtLambdaExpression
import org.jetbrains.kotlin.psi.KtNameReferenceExpression

/** Параметр лямбды именуется: неявный `it` запрещён везде. */
class NoImplicitIt(
    config: Config,
) : Rule(
        config = config,
        description = "Параметр лямбды именуется явно, без it.",
    ) {
    override fun visitLambdaExpression(lambdaExpression: KtLambdaExpression) {
        super.visitLambdaExpression(lambdaExpression)
        if (lambdaExpression.functionLiteral.hasParameterSpecification()) {
            return
        }
        val references = PsiTreeUtil.collectElementsOfType(lambdaExpression, KtNameReferenceExpression::class.java)
        val usesIt = references.any { reference -> reference.isImplicitItOf(lambdaExpression) }
        if (usesIt) {
            report(
                Finding(
                    entity = Entity.from(lambdaExpression),
                    message = "Лямбда с неявным it: назовите параметр (`{ item -> … }`).",
                ),
            )
        }
    }

    private fun KtNameReferenceExpression.isImplicitItOf(lambda: KtLambdaExpression): Boolean {
        val owner = PsiTreeUtil.getParentOfType(this, KtLambdaExpression::class.java)
        return getReferencedName() == "it" && owner == lambda
    }
}
