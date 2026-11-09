package io.github.nakulov.designsystem.generator.model

import io.github.nakulov.kotestkit.case
import io.github.nakulov.kotestkit.shouldReturnTest
import io.github.nakulov.kotestkit.shouldThrowWithMessageTest
import io.github.nakulov.kotestkit.withGivens
import io.kotest.core.spec.style.BehaviorSpec

class ContractResolverSpec : BehaviorSpec({
    withGivens(
        nameFn = { "контракты: ${it.arg2}" },
        case(makeInheritanceModel(), "inheritance"),
        case(makeInlineModel(), "inline"),
        case(makeNamedVariantModel(), "namedVariant"),
        case(makeEnumModel(), "enum"),
        case(makeDefaultsToModel(), "defaultsTo"),
        case(makeDefaultModel(), "default"),
    ) { expected, case ->
        shouldReturnTest(expected) { ContractResolver.load(makeContractsRoot(case)) }
    }

    withGivens(
        nameFn = { "контракты с ошибкой: ${it.arg2}" },
        case("layer/entity/Entity: ссылка «Missing» не найдена (layer/entity/Missing)", "broken/missingRef"),
        case("layer/entity/Entity: inline ссылается на layer/entity/EntityObject, а не на enum", "broken/inlineObject"),
        case("layer/entity/Entity.b: defaultsTo «c» — не соседнее свойство", "broken/defaultsToMissing"),
        case("layer/entity/Entity: Encountered an unknown key 'title' at offset 80 at path: \$", "broken/unknownKey"),
    ) { error, case ->
        shouldThrowWithMessageTest<ContractResolutionException>("Контракты не разрешены:\n$error") {
            ContractResolver.load(makeContractsRoot(case))
        }
    }
})
