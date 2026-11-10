package io.github.nakulov.designsystem.generator.detekt

import dev.detekt.api.Config
import io.github.nakulov.kotestkit.case
import io.github.nakulov.kotestkit.shouldReturnTest
import io.github.nakulov.kotestkit.withGivens
import io.kotest.core.spec.style.BehaviorSpec

class BehaviorSpecOnlySpec :
    BehaviorSpec({
        withGivens(
            nameFn = { sample -> "образец: ${sample.arg2}" },
            case(arg1 = makeFunSpecFindings(), arg2 = "fun-spec"),
            case(arg1 = emptyList(), arg2 = "behavior-spec"),
        ) { expected, sample ->
            shouldReturnTest(expected) { BehaviorSpecOnly(Config.empty).lintSample(name = sample) }
        }
    })
