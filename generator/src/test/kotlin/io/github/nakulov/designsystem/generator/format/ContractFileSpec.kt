package io.github.nakulov.designsystem.generator.format

import io.github.nakulov.designsystem.generator.format.json.ContractJson
import io.github.nakulov.kotestkit.serialization.withJson
import io.kotest.core.spec.style.BehaviorSpec

class ContractFileSpec :
    BehaviorSpec({
        withJson(ContractJson) {
            shouldDecodeTest(resource = "format/object.json", expected = makeObjectContractFile())
            shouldDecodeTest(resource = "format/enum.json", expected = makeEnumContractFile())
            shouldDecodeTest(resource = "format/oneOf.json", expected = makeOneOfContractFile())
        }
    })
