package io.github.nakulov.designsystem.generator.model

import io.github.nakulov.kotestkit.case
import io.github.nakulov.kotestkit.shouldReturnTest
import io.github.nakulov.kotestkit.shouldThrowWithMessageTest
import io.github.nakulov.kotestkit.withGivens
import io.kotest.core.spec.style.BehaviorSpec
import kotlinx.serialization.json.JsonPrimitive
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.isRegularFile
import kotlin.io.path.name
import kotlin.io.path.nameWithoutExtension
import kotlin.io.path.relativeTo
import kotlin.io.path.walk
import kotlin.io.path.writeText

class ContractResolverSpec : BehaviorSpec({
    val root = Path(System.getProperty("contracts"))
    val model = ContractResolver.load(root)

    fun id(value: String) = ContractId(value)
    fun ref(value: String) = TypeRef.Contract(id(value))
    fun objectContract(value: String) = model[id(value)] as ObjectContract
    fun oneOfContract(value: String) = model[id(value)] as OneOfContract

    shouldReturnTest(contractFiles(root), options = options("все контракты репозитория загружаются и разрешаются")) {
        model.contracts.keys
    }

    withGivens(
        nameFn = { "Badge, свойство ${it.arg3}: объявлено в ${it.arg1}" },
        case(id("core/component/Component"), PropertyType.Single(TypeRef.Primitive(PrimitiveType.STRING)), "id"),
        case(id("core/component/Component"), PropertyType.Single(ref("core/layout/Layout")), "layout"),
        case(id("core/component/Component"), PropertyType.Single(ref("core/padding/Padding")), "padding"),
        case(id("core/component/Component"), PropertyType.Single(ref("core/appearance/Appearance")), "appearance"),
        // Имя без `/` разрешается в сущности, где свойство объявлено, а не в наследнике.
        case(id("core/component/Component"), PropertyType.Array(ref("core/component/ComponentIndicator"), false), "indicators"),
        case(id("core/component/Component"), PropertyType.Array(ref("core/event/Event"), true), "events"),
        case(id("components/badge/Badge"), PropertyType.Single(ref("components/badge/BadgeContent")), "content"),
    ) { declaredIn, type, name ->
        shouldReturnTest(declaredIn to type) {
            objectContract("components/badge/Badge").properties.single { it.name == name }.let { it.declaredIn to it.type }
        }
    }

    shouldReturnTest(
        listOf("id", "layout", "padding", "appearance", "indicators", "events", "content"),
        options = options("наследование: свойства Component идут перед собственными свойствами Badge"),
    ) {
        objectContract("components/badge/Badge").properties.map { it.name }
    }

    shouldReturnTest(
        id("core/component/Component") to listOf("plain"),
        options = options("наследование: базовая модель и контекст оформления Badge"),
    ) {
        objectContract("components/badge/Badge").let { it.base to it.appearanceContext }
    }

    shouldReturnTest(
        listOf(
            "zero" to VariantData.None,
            "s" to VariantData.None,
            "m" to VariantData.None,
            "l" to VariantData.None,
            "custom" to VariantData.Fields(id("foundation/borderWidth/BorderWidthCustom")),
        ),
        options = options("inline: значения BorderWidthScale — варианты BorderWidth"),
    ) {
        oneOfContract("foundation/borderWidth/BorderWidth").variants.map { it.name to it.data }
    }

    shouldReturnTest(
        listOf(
            "text" to VariantData.Fields(id("components/badge/BadgeText")),
            "image" to VariantData.Value(ref("components/image/ImageSource")),
        ),
        options = options("именованные варианты: объект — свойства, oneOf — одно значение"),
    ) {
        oneOfContract("components/badge/BadgeContent").variants.map { it.name to it.data }
    }

    shouldReturnTest(
        listOf("start", "center", "end"),
        options = options("перечисление: значения TextAlignment по порядку"),
    ) {
        (model[id("components/text/TextAlignment")] as EnumContract).values.map { it.name }
    }

    withGivens(
        nameFn = { "PressableStates, свойство ${it.arg2}: ${it.arg1}" },
        case(null, "default"),
        case(DefaultValue.Sibling("default"), "pressed"),
        case(DefaultValue.Sibling("default"), "disabled"),
    ) { expected, name ->
        shouldReturnTest(expected) {
            objectContract("components/pressable/PressableStates").properties.single { it.name == name }.default
        }
    }

    shouldReturnTest(
        DefaultValue.Value(JsonPrimitive("edge")),
        options = options("default: значение из контракта LayoutPinned"),
    ) {
        objectContract("core/layout/LayoutPinned").properties.single { it.default != null }.default
    }

    withGivens(
        nameFn = { it.arg1 },
        case(
            "Контракты не разрешены:\nlayer/entity/Entity: ссылка «Missing» не найдена (layer/entity/Missing)",
            "ссылка на несуществующий контракт",
            """{ "name": "Entity", "type": "oneOf", "description": "d", "oneOf": [ { "${'$'}ref": "Missing" } ] }""",
        ),
        case(
            "Контракты не разрешены:\nlayer/entity/Entity: inline ссылается на layer/entity/EntityObject, а не на enum",
            "inline на объект",
            """{ "name": "Entity", "type": "oneOf", "description": "d", "oneOf": [ { "${'$'}ref": "EntityObject", "inline": true } ] }""",
        ),
        case(
            "Контракты не разрешены:\nlayer/entity/Entity.b: defaultsTo «c» — не соседнее свойство",
            "defaultsTo на несуществующее свойство",
            """{ "name": "Entity", "type": "object", "description": "d", "properties": [
                 { "name": "b", "type": "string", "required": true, "defaultsTo": "c", "description": "d" } ] }""",
        ),
    ) { message, _, contract ->
        shouldThrowWithMessageTest<ContractResolutionException>(message) {
            ContractResolver.load(contracts("Entity" to contract, "EntityObject" to ENTITY_OBJECT))
        }
    }

    shouldThrowWithMessageTest<ContractResolutionException>(
        "Контракты не разрешены:\nlayer/entity/Entity: Encountered an unknown key 'title' at offset 57 at path: \$",
    ) {
        ContractResolver.load(
            contracts("Entity" to """{ "name": "Entity", "type": "enum", "description": "d", "title": "x", "values": [] }"""),
        )
    }
})

private const val ENTITY_OBJECT =
    """{ "name": "EntityObject", "type": "object", "description": "d", "properties": [
         { "name": "a", "type": "string", "required": true, "description": "d" } ] }"""

private fun options(name: String) = io.github.nakulov.kotestkit.TestOptions(name = name)

/** Идентификаторы по файлам: главные контракты и `definitions/` (schema-format.md, раздел 11.1). */
private fun contractFiles(root: Path): Set<ContractId> = root.walk()
    .filter { it.isRegularFile() && it.name.endsWith(".json") }
    .map { it.relativeTo(root) }
    .filter { path ->
        val entity = path.getName(1).name
        path.nameCount == 3 && path.nameWithoutExtension == entity.replaceFirstChar { it.uppercase() } ||
            path.nameCount == 4 && path.getName(2).name == "definitions"
    }
    .map { ContractId("${it.getName(0)}/${it.getName(1)}/${it.nameWithoutExtension}") }
    .toSet()

/** Каталог контрактов из одной сущности `layer/entity`: главный контракт и определения. */
private fun contracts(vararg files: Pair<String, String>): Path {
    val root = Files.createTempDirectory("contracts")
    val entity = root.resolve("layer/entity")
    val definitions = entity.resolve("definitions").createDirectories()
    for ((name, text) in files) {
        (if (name == "Entity") entity else definitions).resolve("$name.json").writeText(text)
    }
    return root
}
