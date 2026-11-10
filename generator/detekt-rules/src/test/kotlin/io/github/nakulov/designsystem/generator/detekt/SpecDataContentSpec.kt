package io.github.nakulov.designsystem.generator.detekt

import dev.detekt.api.Config
import io.github.nakulov.kotestkit.case
import io.github.nakulov.kotestkit.shouldReturnTest
import io.github.nakulov.kotestkit.withGivens
import io.kotest.core.spec.style.BehaviorSpec

class SpecDataContentSpec :
    BehaviorSpec({
        withGivens(
            nameFn = { sample -> "образец: ${sample.arg2}" },
            case(arg1 = makeSpecDataFindings(), arg2 = "BrokenSpecData"),
            case(arg1 = emptyList(), arg2 = "ExampleSpecData"),
            case(arg1 = emptyList(), arg2 = "Example"),
        ) { expected, sample ->
            shouldReturnTest(expected) { SpecDataContent(Config.empty).lintSample(name = sample) }
        }
    })
