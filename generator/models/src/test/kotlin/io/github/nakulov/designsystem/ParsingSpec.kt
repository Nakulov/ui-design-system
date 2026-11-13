package io.github.nakulov.designsystem

import io.github.nakulov.designsystem.core.component.Component
import io.github.nakulov.kotestkit.case
import io.github.nakulov.kotestkit.shouldReturnTest
import io.github.nakulov.kotestkit.shouldThrowWithMessageTest
import io.github.nakulov.kotestkit.withGivens
import io.kotest.core.spec.style.BehaviorSpec
import kotlinx.serialization.SerializationException

class ParsingSpec :
    BehaviorSpec({
        withGivens(
            nameFn = { sample -> "ответ не по контракту: ${sample.arg2}" },
            case(arg1 = "$.Container.children[2].Text: неизвестный ключ «title»", arg2 = "unknownKey"),
            case(arg1 = "$.Container.children[2].Text: нет обязательного поля «typography»", arg2 = "missingField"),
            case(arg1 = "$.Container.children[2].Text.typography: неизвестный вариант «huge»", arg2 = "unknownVariant"),
            case(arg1 = "$.Container.children[2]: неизвестная реализация «Chip»", arg2 = "unknownComponent"),
            case(
                arg1 = "$.Container.children[2].Text.color: ожидается объект с одним ключом — именем варианта",
                arg2 = "wrapperKeys",
            ),
            case(arg1 = "$.Container.children[2].Text.decoration: null не используется", arg2 = "null"),
            case(arg1 = "$.Container.children[2].Text.events: пустой массив не используется", arg2 = "emptyArray"),
            case(arg1 = "$.Container.children[2].Text.events: вариант «click» повторяется", arg2 = "duplicateVariant"),
            case(
                arg1 = "$.Container.children[2].Text.maxLines.count: 1.5 — не integer (целое, 32 бита)",
                arg2 = "fractionalInteger",
            ),
            case(
                arg1 = "$.Container.children[2].Text.maxLines.count: 3000000000 — не integer (целое, 32 бита)",
                arg2 = "largeInteger",
            ),
            case(
                arg1 = "$.Container.children[2].Text.appearance.plain.shadow.custom.color: \"#c9a227\" — " +
                    "не color (#RRGGBB или #RRGGBBAA, верхний регистр)",
                arg2 = "lowercaseColor",
            ),
            case(arg1 = "$.Container.children[2].Text.content.plain: true — не string", arg2 = "wrongType"),
            case(
                arg1 = "$.Container.children[2].Text.appearance.plain.clip: \"true\" — не boolean",
                arg2 = "quotedBoolean",
            ),
        ) { message, case ->
            shouldThrowWithMessageTest<SerializationException>(message) {
                makeSamplesJson().decodeFromString<Component>(makeParsingResource(case))
            }
        }

        shouldReturnTest(makeProductScreen()) {
            makeProductJson().decodeFromString<Component>(makeParsingResource(case = "productComponent"))
        }
    })
