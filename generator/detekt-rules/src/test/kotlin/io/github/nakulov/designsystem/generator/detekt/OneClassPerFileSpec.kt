package io.github.nakulov.designsystem.generator.detekt

import dev.detekt.api.Config
import io.github.nakulov.kotestkit.case
import io.github.nakulov.kotestkit.shouldReturnTest
import io.github.nakulov.kotestkit.withGivens
import io.kotest.core.spec.style.BehaviorSpec

class OneClassPerFileSpec :
    BehaviorSpec({
        withGivens(
            nameFn = { sample -> "образец: ${sample.arg2}" },
            case(arg1 = makeTwoClassesFindings(), arg2 = "two-classes"),
            case(arg1 = emptyList(), arg2 = "nested"),
        ) { expected, sample ->
            shouldReturnTest(expected) { OneClassPerFile(Config.empty).lintSample(name = sample) }
        }
    })
