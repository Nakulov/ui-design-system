package io.github.nakulov.designsystem.generator.output.kotlin

import io.github.nakulov.designsystem.generator.resolve.ContractResolver
import io.github.nakulov.designsystem.generator.resolve.makeContractsRoot
import io.github.nakulov.kotestkit.case
import io.github.nakulov.kotestkit.shouldReturnTest
import io.github.nakulov.kotestkit.shouldThrowWithMessageTest
import io.github.nakulov.kotestkit.withGivens
import io.kotest.core.spec.style.BehaviorSpec

class KotlinOutputSpec :
    BehaviorSpec({
        withGivens(
            nameFn = { sample -> "модели Kotlin: ${sample.arg2}" },
            case(arg1 = makeComponentFiles(), arg2 = "kotlinComponent"),
            case(arg1 = makeDefaultsFiles(), arg2 = "kotlinDefaults"),
            case(arg1 = makeNamesFiles(), arg2 = "kotlinNames"),
            case(arg1 = makePrimitivesFiles(), arg2 = "primitives"),
            case(arg1 = makeEmptyFiles(), arg2 = "empty"),
            case(arg1 = makeEmptyBaseFiles(), arg2 = "kotlinEmptyBase"),
        ) { expected, case ->
            shouldReturnTest(expected) { KotlinOutput.generate(ContractResolver.load(makeContractsRoot(case))) }
        }

        withGivens(
            nameFn = { sample -> "default не по контракту: ${sample.arg2}" },
            case(
                arg1 = "layer/entity/EntityBase: default у ссылки на базовую модель",
                arg2 = "kotlinBroken/defaultBase",
            ),
            case(
                arg1 = "layer/entity/EntitySize: вариант «zero» без данных записан объектом",
                arg2 = "kotlinBroken/defaultUnfolded",
            ),
            case(arg1 = "default у массива: [\"a\"]", arg2 = "kotlinBroken/defaultArray"),
            case(arg1 = "default у payload: x", arg2 = "kotlinBroken/defaultPayload"),
        ) { message, case ->
            shouldThrowWithMessageTest<IllegalStateException>(message) {
                KotlinOutput.generate(ContractResolver.load(makeContractsRoot(case)))
            }
        }
    })
