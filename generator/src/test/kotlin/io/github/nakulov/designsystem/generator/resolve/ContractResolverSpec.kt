package io.github.nakulov.designsystem.generator.resolve

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
        case(makePrimitivesModel(), "primitives"),
        case(makeEmptyModel(), "empty"),
    ) { expected, case ->
        shouldReturnTest(expected) { ContractResolver.load(makeContractsRoot(case)) }
    }

    withGivens(
        nameFn = { "контракты с ошибкой: ${it.arg2}" },
        case("layer/entity/Entity: ссылка «Missing» не найдена (layer/entity/Missing)", "broken/missingRef"),
        case("layer/entity/Entity: inline ссылается на layer/entity/EntityObject, а не на enum", "broken/inlineObject"),
        case("layer/entity/Entity.b: defaultsTo «c» — не соседнее свойство", "broken/defaultsToMissing"),
        case("layer/entity/Entity.a: у массива нет items", "broken/arrayWithoutItems"),
        case("layer/entity/Entity.a: вложенный массив", "broken/nestedArray"),
        case("layer/entity/Entity.a: значение по умолчанию у необязательного свойства", "broken/defaultOnOptional"),
        case(
            "layer/entity/Entity.b: defaultsTo «a» — свойство необязательное или само с defaultsTo",
            "broken/defaultsToOptional",
        ),
        case(
            "layer/entity/Entity.c: defaultsTo «b» — свойство необязательное или само с defaultsTo",
            "broken/defaultsToChain",
        ),
        case("layer/entity/Entity.b: тип отличается от типа «a»", "broken/defaultsToOtherType"),
        case("layer/entity/Entity: у элемента oneOf с type нет name", "broken/typeWithoutName"),
        case("layer/entity/Entity: имя «Other» не совпадает с именем файла", "broken/nameMismatch"),
        case("layer/entity/Entity: ссылка «Missing» не найдена (layer/entity/Missing)", "broken/extendsMissing"),
        case(
            "layer/entity/Entity: extends ссылается на layer/entity/EntityBase, а не на core/component/Component",
            "broken/extendsNotComponent",
        ),
        case("layer/entity/Entity.b: default и defaultsTo вместе", "broken/defaultAndDefaultsTo"),
        case("layer/entity/Entity.a: defaultsTo «a» — не соседнее свойство", "broken/defaultsToSelf"),
        case("layer/entity/Entity.b: тип отличается от типа «a»", "broken/defaultsToOtherRef"),
        case("layer/entity/Entity.b: тип отличается от типа «a»", "broken/defaultsToOtherItems"),
        case("layer/entity/Entity: вариант «a» повторяется", "broken/duplicateVariant"),
        case("layer/entity/Entity: у элемента oneOf и type, и \$ref", "broken/variantTypeAndRef"),
        case("layer/entity/Entity: у элемента oneOf нет ни type, ни \$ref", "broken/variantEmpty"),
        case("layer/entity/Entity: name вместе с inline", "broken/inlineWithName"),
        case("layer/entity/Entity.a: нужен ровно один из type и \$ref", "broken/propertyWithoutType"),
        case("layer/entity/Entity.a: нужен ровно один из type и \$ref", "broken/propertyTypeAndRef"),
        case("layer/entity/Entity: ссылка «Missing» не найдена (layer/entity/Missing)", "broken/propertyMissingRef"),
        case("layer/entity/Entity: Encountered an unknown key 'title' at offset 80 at path: \$", "broken/unknownKey"),
    ) { error, case ->
        shouldThrowWithMessageTest<ContractResolutionException>("Контракты не разрешены:\n$error") {
            ContractResolver.load(makeContractsRoot(case))
        }
    }
})
