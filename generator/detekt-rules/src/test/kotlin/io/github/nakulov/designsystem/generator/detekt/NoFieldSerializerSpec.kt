package io.github.nakulov.designsystem.generator.detekt

import dev.detekt.api.Config
import io.github.nakulov.kotestkit.case
import io.github.nakulov.kotestkit.shouldReturnTest
import io.github.nakulov.kotestkit.withGivens
import io.kotest.core.spec.style.BehaviorSpec

class NoFieldSerializerSpec :
    BehaviorSpec({
        withGivens(
            nameFn = { sample -> "образец: ${sample.arg2}" },
            case(arg1 = makeFieldSerializerFindings(), arg2 = "field"),
            case(arg1 = emptyList(), arg2 = "contextual"),
        ) { expected, sample ->
            shouldReturnTest(expected) { NoFieldSerializer(Config.empty).lintSample(name = sample) }
        }
    })
