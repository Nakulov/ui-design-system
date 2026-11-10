package io.github.nakulov.designsystem.generator.detekt

import dev.detekt.api.Config
import io.github.nakulov.kotestkit.case
import io.github.nakulov.kotestkit.shouldReturnTest
import io.github.nakulov.kotestkit.withGivens
import io.kotest.core.spec.style.BehaviorSpec

class NamedLiteralArgumentSpec :
    BehaviorSpec({
        withGivens(
            nameFn = { sample -> "образец: ${sample.arg2}" },
            case(arg1 = makeUnnamedLiteralFindings(), arg2 = "unnamed"),
            case(arg1 = makeJavaLiteralFindings(), arg2 = "java"),
            case(arg1 = emptyList(), arg2 = "named"),
            case(arg1 = emptyList(), arg2 = "exceptions"),
        ) { expected, sample ->
            shouldReturnTest(expected) { NamedLiteralArgument(Config.empty).lintSampleWithContext(name = sample) }
        }
    })
