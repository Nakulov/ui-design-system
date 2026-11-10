package io.github.nakulov.designsystem.generator.detekt

import dev.detekt.api.RuleName
import dev.detekt.api.RuleSetId

/** Набор правил в конфиге detekt: `codestyle`. */
fun makeCodestyleRuleSetId(): RuleSetId {
    return RuleSetId(value = "codestyle")
}

/** Все правила набора — по имени класса, как они включаются в `detekt.yml`. */
fun makeCodestyleRuleNames(): Set<RuleName> {
    return setOf(
        RuleName(value = "OneClassPerFile"),
        RuleName(value = "SerialAnnotationOnOwnLine"),
        RuleName(value = "SerialNameRequired"),
        RuleName(value = "SerialEnumBlankLine"),
        RuleName(value = "SerialRequiredFirst"),
        RuleName(value = "NoFieldSerializer"),
        RuleName(value = "BlockBody"),
        RuleName(value = "NamedLiteralArgument"),
        RuleName(value = "NoImplicitIt"),
        RuleName(value = "BehaviorSpecOnly"),
        RuleName(value = "SpecDataContent"),
    )
}
