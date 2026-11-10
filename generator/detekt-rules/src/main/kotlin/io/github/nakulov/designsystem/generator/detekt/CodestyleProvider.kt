package io.github.nakulov.designsystem.generator.detekt

import dev.detekt.api.RuleSet
import dev.detekt.api.RuleSetId
import dev.detekt.api.RuleSetProvider

/** Правила кода генератора из `generator/CODESTYLE.md`, которых нет среди готовых правил detekt. */
class CodestyleProvider : RuleSetProvider {
    override val ruleSetId: RuleSetId = RuleSetId(value = "codestyle")

    override fun instance(): RuleSet {
        return RuleSet(
            ruleSetId,
            listOf(
                ::OneClassPerFile,
                ::SerialAnnotationOnOwnLine,
                ::SerialNameRequired,
                ::SerialEnumBlankLine,
                ::SerialRequiredFirst,
                ::NoFieldSerializer,
                ::BlockBody,
                ::NamedLiteralArgument,
                ::NoImplicitIt,
                ::BehaviorSpecOnly,
                ::SpecDataContent,
            ),
        )
    }
}
