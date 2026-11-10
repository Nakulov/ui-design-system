package io.github.nakulov.designsystem.generator.detekt

import io.github.nakulov.kotestkit.shouldReturnTest
import io.kotest.core.spec.style.BehaviorSpec

class CodestyleProviderSpec :
    BehaviorSpec({
        shouldReturnTest(makeCodestyleRuleSetId()) { CodestyleProvider().ruleSetId }

        shouldReturnTest(makeCodestyleRuleNames()) { CodestyleProvider().instance().rules.keys }
    })
