package io.github.nakulov.designsystem.generator.model

import io.github.nakulov.kotestkit.TestOptions
import io.github.nakulov.kotestkit.case
import io.github.nakulov.kotestkit.shouldReturnTest
import io.github.nakulov.kotestkit.shouldThrowWithMessageTest
import io.github.nakulov.kotestkit.withGivens
import io.kotest.core.spec.style.BehaviorSpec
import kotlinx.serialization.json.JsonPrimitive

class ContractResolverSpec : BehaviorSpec({
    val root = makeContractsRoot()
    val model = ContractResolver.load(root)

    shouldReturnTest(
        makeContractIds(root),
        options = TestOptions(name = "все контракты репозитория загружаются и разрешаются"),
    ) {
        model.contracts.keys
    }

    withGivens(
        nameFn = { "Badge, свойство ${it.arg3}: объявлено в ${it.arg1}" },
        case(ContractId(COMPONENT), PropertyType.Single(TypeRef.Primitive(PrimitiveType.STRING)), "id"),
        case(ContractId(COMPONENT), PropertyType.Single(makeContractRef("core/layout/Layout")), "layout"),
        case(ContractId(COMPONENT), PropertyType.Single(makeContractRef("core/padding/Padding")), "padding"),
        case(ContractId(COMPONENT), PropertyType.Single(makeContractRef("core/appearance/Appearance")), "appearance"),
        // Имя без `/` разрешается в сущности, где свойство объявлено, а не в наследнике.
        case(ContractId(COMPONENT), PropertyType.Array(makeContractRef("core/component/ComponentIndicator"), false), "indicators"),
        case(ContractId(COMPONENT), PropertyType.Array(makeContractRef("core/event/Event"), true), "events"),
        case(ContractId(BADGE), PropertyType.Single(makeContractRef(BADGE_CONTENT)), "content"),
    ) { declaredIn, type, name ->
        shouldReturnTest(declaredIn to type) {
            (model[ContractId(BADGE)] as ObjectContract).properties.single { it.name == name }
                .let { it.declaredIn to it.type }
        }
    }

    shouldReturnTest(
        listOf("id", "layout", "padding", "appearance", "indicators", "events", "content"),
        options = TestOptions(name = "наследование: свойства Component идут перед собственными свойствами Badge"),
    ) {
        (model[ContractId(BADGE)] as ObjectContract).properties.map { it.name }
    }

    shouldReturnTest(
        ContractId(COMPONENT) to listOf("plain"),
        options = TestOptions(name = "наследование: базовая модель и контекст оформления Badge"),
    ) {
        (model[ContractId(BADGE)] as ObjectContract).let { it.base to it.appearanceContext }
    }

    shouldReturnTest(
        listOf(
            "zero" to VariantData.None,
            "s" to VariantData.None,
            "m" to VariantData.None,
            "l" to VariantData.None,
            "custom" to VariantData.Fields(ContractId("foundation/borderWidth/BorderWidthCustom")),
        ),
        options = TestOptions(name = "inline: значения BorderWidthScale — варианты BorderWidth"),
    ) {
        (model[ContractId(BORDER_WIDTH)] as OneOfContract).variants.map { it.name to it.data }
    }

    shouldReturnTest(
        listOf(
            "text" to VariantData.Fields(ContractId("components/badge/BadgeText")),
            "image" to VariantData.Value(makeContractRef("components/image/ImageSource")),
        ),
        options = TestOptions(name = "именованные варианты: объект — свойства, oneOf — одно значение"),
    ) {
        (model[ContractId(BADGE_CONTENT)] as OneOfContract).variants.map { it.name to it.data }
    }

    shouldReturnTest(
        listOf("start", "center", "end"),
        options = TestOptions(name = "перечисление: значения TextAlignment по порядку"),
    ) {
        (model[ContractId(TEXT_ALIGNMENT)] as EnumContract).values.map { it.name }
    }

    withGivens(
        nameFn = { "PressableStates, свойство ${it.arg2}: ${it.arg1}" },
        case(null, "default"),
        case(DefaultValue.Sibling("default"), "pressed"),
        case(DefaultValue.Sibling("default"), "disabled"),
    ) { expected, name ->
        shouldReturnTest(expected) {
            (model[ContractId(PRESSABLE_STATES)] as ObjectContract).properties.single { it.name == name }.default
        }
    }

    shouldReturnTest(
        DefaultValue.Value(JsonPrimitive("edge")),
        options = TestOptions(name = "default: значение из контракта LayoutPinned"),
    ) {
        (model[ContractId(LAYOUT_PINNED)] as ObjectContract).properties.single { it.default != null }.default
    }

    withGivens(
        nameFn = { it.arg2 },
        case("layer/entity/Entity: ссылка «Missing» не найдена (layer/entity/Missing)", "missingRef"),
        case("layer/entity/Entity: inline ссылается на layer/entity/EntityObject, а не на enum", "inlineObject"),
        case("layer/entity/Entity.b: defaultsTo «c» — не соседнее свойство", "defaultsToMissing"),
        case("layer/entity/Entity: Encountered an unknown key 'title' at offset 80 at path: \$", "unknownKey"),
    ) { error, case ->
        shouldThrowWithMessageTest<ContractResolutionException>("Контракты не разрешены:\n$error") {
            ContractResolver.load(makeBrokenContractsRoot(case))
        }
    }
})
