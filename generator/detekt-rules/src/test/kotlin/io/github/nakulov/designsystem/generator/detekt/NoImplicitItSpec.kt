package io.github.nakulov.designsystem.generator.detekt

import dev.detekt.api.Config
import io.github.nakulov.kotestkit.case
import io.github.nakulov.kotestkit.shouldReturnTest
import io.github.nakulov.kotestkit.withGivens
import io.kotest.core.spec.style.BehaviorSpec

class NoImplicitItSpec :
    BehaviorSpec({
        withGivens(
            nameFn = { sample -> "образец: ${sample.arg2}" },
            case(arg1 = makeImplicitItFindings(), arg2 = "implicit"),
            case(arg1 = makeNestedItFindings(), arg2 = "nested"),
            case(arg1 = emptyList(), arg2 = "named"),
        ) { expected, sample ->
            shouldReturnTest(expected) { NoImplicitIt(Config.empty).lintSample(name = sample) }
        }
    })
