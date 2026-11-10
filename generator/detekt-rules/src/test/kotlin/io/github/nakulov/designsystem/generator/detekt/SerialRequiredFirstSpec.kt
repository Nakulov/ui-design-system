package io.github.nakulov.designsystem.generator.detekt

import dev.detekt.api.Config
import io.github.nakulov.kotestkit.case
import io.github.nakulov.kotestkit.shouldReturnTest
import io.github.nakulov.kotestkit.withGivens
import io.kotest.core.spec.style.BehaviorSpec

class SerialRequiredFirstSpec :
    BehaviorSpec({
        withGivens(
            nameFn = { sample -> "образец: ${sample.arg2}" },
            case(arg1 = makeOptionalFirstFindings(), arg2 = "optional-first"),
            case(arg1 = emptyList(), arg2 = "required-first"),
            case(arg1 = emptyList(), arg2 = "plain"),
        ) { expected, sample ->
            shouldReturnTest(expected) { SerialRequiredFirst(Config.empty).lintSample(name = sample) }
        }
    })
