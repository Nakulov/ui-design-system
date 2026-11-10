package io.github.nakulov.designsystem.generator.format

import io.github.nakulov.designsystem.generator.format.json.ContractJson
import io.github.nakulov.kotestkit.serialization.withJson
import io.kotest.core.spec.style.BehaviorSpec

class ContractFileSpec :
    BehaviorSpec({
        withJson(ContractJson) {
            shouldDecodeTest("format/object.json", makeObjectContractFile())
            shouldDecodeTest("format/enum.json", makeEnumContractFile())
            shouldDecodeTest("format/oneOf.json", makeOneOfContractFile())
        }
    })
