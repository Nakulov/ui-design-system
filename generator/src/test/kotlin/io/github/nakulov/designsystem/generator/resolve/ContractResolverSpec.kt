package io.github.nakulov.designsystem.generator.resolve

import io.github.nakulov.kotestkit.case
import io.github.nakulov.kotestkit.shouldReturnTest
import io.github.nakulov.kotestkit.shouldThrowWithMessageTest
import io.github.nakulov.kotestkit.withGivens
import io.kotest.core.spec.style.BehaviorSpec

class ContractResolverSpec :
    BehaviorSpec({
        withGivens(
            nameFn = { sample -> "контракты: ${sample.arg2}" },
            case(arg1 = makeInheritanceModel(), arg2 = "inheritance"),
            case(arg1 = makeInlineModel(), arg2 = "inline"),
            case(arg1 = makeNamedVariantModel(), arg2 = "namedVariant"),
            case(arg1 = makeEnumModel(), arg2 = "enum"),
            case(arg1 = makeDefaultsToModel(), arg2 = "defaultsTo"),
            case(arg1 = makeDefaultModel(), arg2 = "default"),
            case(arg1 = makePrimitivesModel(), arg2 = "primitives"),
            case(arg1 = makeEmptyModel(), arg2 = "empty"),
        ) { expected, case ->
            shouldReturnTest(expected) { ContractResolver.load(makeContractsRoot(case)) }
        }

        withGivens(
            nameFn = { sample -> "контракты с ошибкой: ${sample.arg2}" },
            case(
                arg1 = "layer/entity/Entity: ссылка «Missing» не найдена (layer/entity/Missing)",
                arg2 = "broken/missingRef",
            ),
            case(
                arg1 = "layer/entity/Entity: inline ссылается на layer/entity/EntityObject, а не на enum",
                arg2 = "broken/inlineObject",
            ),
            case(
                arg1 = "layer/entity/Entity.b: defaultsTo «c» — не соседнее свойство",
                arg2 = "broken/defaultsToMissing",
            ),
            case(arg1 = "layer/entity/Entity.a: у массива нет items", arg2 = "broken/arrayWithoutItems"),
            case(arg1 = "layer/entity/Entity.a: вложенный массив", arg2 = "broken/nestedArray"),
            case(
                arg1 = "layer/entity/Entity.a: значение по умолчанию у необязательного свойства",
                arg2 = "broken/defaultOnOptional",
            ),
            case(
                arg1 = "layer/entity/Entity.b: defaultsTo «a» — свойство необязательное или само с defaultsTo",
                arg2 = "broken/defaultsToOptional",
            ),
            case(
                arg1 = "layer/entity/Entity.c: defaultsTo «b» — свойство необязательное или само с defaultsTo",
                arg2 = "broken/defaultsToChain",
            ),
            case(arg1 = "layer/entity/Entity.b: тип отличается от типа «a»", arg2 = "broken/defaultsToOtherType"),
            case(arg1 = "layer/entity/Entity: у элемента oneOf с type нет name", arg2 = "broken/typeWithoutName"),
            case(arg1 = "layer/entity/Entity: имя «Other» не совпадает с именем файла", arg2 = "broken/nameMismatch"),
            case(
                arg1 = "layer/entity/Entity: ссылка «Missing» не найдена (layer/entity/Missing)",
                arg2 = "broken/extendsMissing",
            ),
            case(
                arg1 = "layer/entity/Entity: extends ссылается на layer/entity/EntityBase, " +
                    "а не на core/component/Component",
                arg2 = "broken/extendsNotComponent",
            ),
            case(arg1 = "layer/entity/Entity.b: default и defaultsTo вместе", arg2 = "broken/defaultAndDefaultsTo"),
            case(arg1 = "layer/entity/Entity.a: defaultsTo «a» — не соседнее свойство", arg2 = "broken/defaultsToSelf"),
            case(arg1 = "layer/entity/Entity.b: тип отличается от типа «a»", arg2 = "broken/defaultsToOtherRef"),
            case(arg1 = "layer/entity/Entity.b: тип отличается от типа «a»", arg2 = "broken/defaultsToOtherItems"),
            case(arg1 = "layer/entity/Entity: вариант «a» повторяется", arg2 = "broken/duplicateVariant"),
            case(arg1 = "layer/entity/Entity: у элемента oneOf и type, и \$ref", arg2 = "broken/variantTypeAndRef"),
            case(arg1 = "layer/entity/Entity: у элемента oneOf нет ни type, ни \$ref", arg2 = "broken/variantEmpty"),
            case(arg1 = "layer/entity/Entity: name вместе с inline", arg2 = "broken/inlineWithName"),
            case(arg1 = "layer/entity/Entity.a: нужен ровно один из type и \$ref", arg2 = "broken/propertyWithoutType"),
            case(arg1 = "layer/entity/Entity.a: нужен ровно один из type и \$ref", arg2 = "broken/propertyTypeAndRef"),
            case(
                arg1 = "layer/entity/Entity: ссылка «Missing» не найдена (layer/entity/Missing)",
                arg2 = "broken/propertyMissingRef",
            ),
            case(
                arg1 = "layer/entity/Entity: Encountered an unknown key 'title' at offset 80 at path: \$",
                arg2 = "broken/unknownKey",
            ),
            case(arg1 = "layer/entity/EntityDir: Is a directory", arg2 = "broken/unreadable"),
        ) { error, case ->
            shouldThrowWithMessageTest<ContractResolutionException>(message = "Контракты не разрешены:\n$error") {
                ContractResolver.load(makeContractsRoot(case))
            }
        }
    })
