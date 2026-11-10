package io.github.nakulov.designsystem.generator.detekt

import dev.detekt.api.Config
import io.github.nakulov.kotestkit.case
import io.github.nakulov.kotestkit.shouldReturnTest
import io.github.nakulov.kotestkit.withGivens
import io.kotest.core.spec.style.BehaviorSpec

class SerialEnumBlankLineSpec :
    BehaviorSpec({
        withGivens(
            nameFn = { sample -> "образец: ${sample.arg2}" },
            case(arg1 = makeAdjacentFindings(), arg2 = "adjacent"),
            case(arg1 = emptyList(), arg2 = "blank"),
            case(arg1 = emptyList(), arg2 = "plain-enum"),
        ) { expected, sample ->
            shouldReturnTest(expected) { SerialEnumBlankLine(Config.empty).lintSample(name = sample) }
        }
    })
