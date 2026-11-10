package io.github.nakulov.designsystem.generator.detekt

import dev.detekt.api.Config
import io.github.nakulov.kotestkit.case
import io.github.nakulov.kotestkit.shouldReturnTest
import io.github.nakulov.kotestkit.withGivens
import io.kotest.core.spec.style.BehaviorSpec

class BlockBodySpec :
    BehaviorSpec({
        withGivens(
            nameFn = { sample -> "образец: ${sample.arg2}" },
            case(arg1 = makeExpressionBodyFindings(), arg2 = "expression"),
            case(arg1 = emptyList(), arg2 = "block"),
        ) { expected, sample ->
            shouldReturnTest(expected) { BlockBody(Config.empty).lintSample(name = sample) }
        }
    })
