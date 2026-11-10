package io.github.nakulov.designsystem.generator.format

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/** Ожидаемый разбор `format/object.json`: все поля свойств, статус платформы строкой и объектом. */
fun makeObjectContractFile(): ContractFile = ContractFile(
    name = "Example",
    type = ContractType.OBJECT,
    description = "Объект со всеми полями свойств",
    extends = "core/component/Component",
    appearanceContext = listOf("plain", "control"),
    // Статус строкой и объектом с версией — один класс VersionedStatus.
    releaseVersions = ReleaseVersions(
        android = VersionedStatus(status = ReleaseStatus.NOT_RELEASED),
        ios = VersionedStatus(status = ReleaseStatus.RELEASED, version = "1.2"),
        figma = ReleaseStatus.RELEASED,
    ),
    properties = makeExamplePropertyDefs(),
)

/** Свойства `Example`: все поля `PropertyDef`. */
private fun makeExamplePropertyDefs(): List<PropertyDef> = listOf(
    PropertyDef(
        name = "title",
        description = "Необязательная строка",
        required = false,
        type = FormatType.STRING,
        sample = JsonPrimitive("Заголовок"),
        releaseVersions = ReleaseVersions(figma = ReleaseStatus.WILL_NOT_BE_RELEASED),
    ),
    PropertyDef(
        name = "spacing",
        description = "Ссылка со значением по умолчанию",
        required = true,
        ref = "foundation/spacing/Spacing",
        default = JsonObject(mapOf("custom" to JsonObject(mapOf("value" to JsonPrimitive(value = 6))))),
    ),
    PropertyDef(
        name = "pressed",
        description = "Значение по умолчанию — соседнее свойство",
        required = true,
        ref = "foundation/spacing/Spacing",
        defaultsTo = "spacing",
    ),
    PropertyDef(
        name = "events",
        description = "Массив без повторов вариантов",
        required = false,
        type = FormatType.ARRAY,
        items = ArrayItemsDef(ref = "core/event/Event"),
        uniqueVariants = true,
    ),
)

/** Ожидаемый разбор `format/enum.json`. */
fun makeEnumContractFile(): ContractFile = ContractFile(
    name = "ExampleScale",
    type = ContractType.ENUM,
    description = "Перечисление",
    sample = JsonPrimitive("m"),
    values = listOf(
        EnumValueDef(name = "s", description = "Малый"),
        EnumValueDef(
            name = "m",
            description = "Средний",
            releaseVersions = ReleaseVersions(android = VersionedStatus(ReleaseStatus.NOT_RELEASED)),
        ),
    ),
)

/** Ожидаемый разбор `format/oneOf.json`: варианты всех видов. */
fun makeOneOfContractFile(): ContractFile = ContractFile(
    name = "ExampleSize",
    type = ContractType.ONE_OF,
    description = "Варианты всех видов",
    oneOf = listOf(
        OneOfItemDef(ref = "ExampleScale", inline = true),
        OneOfItemDef(ref = "ExampleCustom", name = "custom"),
        OneOfItemDef(type = FormatType.STRING, name = "plain", sample = JsonPrimitive("текст")),
    ),
)
