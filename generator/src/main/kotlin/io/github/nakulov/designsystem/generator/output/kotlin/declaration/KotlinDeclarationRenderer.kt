package io.github.nakulov.designsystem.generator.output.kotlin.declaration

import io.github.nakulov.designsystem.generator.model.BaseContract
import io.github.nakulov.designsystem.generator.model.ContractId
import io.github.nakulov.designsystem.generator.model.DefaultValue
import io.github.nakulov.designsystem.generator.model.EnumContract
import io.github.nakulov.designsystem.generator.model.ObjectContract
import io.github.nakulov.designsystem.generator.model.OneOfContract
import io.github.nakulov.designsystem.generator.model.Property
import io.github.nakulov.designsystem.generator.model.PropertyType
import io.github.nakulov.designsystem.generator.model.ResolvedContract
import io.github.nakulov.designsystem.generator.model.TypeRef
import io.github.nakulov.designsystem.generator.model.Variant
import io.github.nakulov.designsystem.generator.model.VariantData
import io.github.nakulov.designsystem.generator.output.kotlin.file.KotlinFileBuilder
import io.github.nakulov.designsystem.generator.output.kotlin.name.KotlinNames
import io.github.nakulov.designsystem.generator.output.kotlin.name.KotlinSerialNames
import io.github.nakulov.designsystem.generator.output.kotlin.name.KotlinTypeName
import io.github.nakulov.designsystem.generator.output.kotlin.name.VARIANT_KEY
import io.github.nakulov.designsystem.generator.output.kotlin.type.KotlinTypeRenderer
import io.github.nakulov.designsystem.generator.output.kotlin.type.KotlinTypes

/** Свойство базовой модели, которое сужается по `appearanceContext` (раздел 5.2, правило 5). */
private const val APPEARANCE = "appearance"

/**
 * Строки объявления одного типа верхнего уровня; ссылки на типы пишутся через [file].
 *
 * Любой класс выводится одним правилом [classLines]: класс варианта, объекта и варианта со значением отличаются
 * только аннотациями и списком параметров. Запись — по разделу 4.5: `oneOf` и перечисления — своим сериализатором
 * `<Имя>Serializer`, базовая модель — `<Имя>Serializer` с ключом-именем реализации.
 */
class KotlinDeclarationRenderer(
    private val types: KotlinTypes,
    private val file: KotlinFileBuilder,
) {
    private val renderer = KotlinTypeRenderer(types, file)
    private val defaults = KotlinDefaults(types, renderer)

    /** Объявление контракта [contract] по его конструкции формата. */
    fun declaration(contract: ResolvedContract): List<String> {
        val name = KotlinNames.contractType(contract.id).topName
        return when (contract) {
            is BaseContract -> base(contract)

            is ObjectContract -> objectClass(
                name = name,
                contract = contract,
                shadowed = emptySet(),
                variant = null,
            ) + narrowedSerializer(contract)

            is EnumContract -> sealed(
                name = name,
                contract = contract,
                nested = contract.values.map { value ->
                    classLines(
                        name = KotlinNames.typeName(value.name),
                        annotations = variantAnnotations(value.name, valueVariant = false),
                        parameters = emptyList(),
                        supertypes = listOf(name),
                    )
                },
            )

            is OneOfContract -> oneOf(name, contract)
        }
    }

    /** Базовая модель — открытый `interface` (раздел 5.2) и сериализатор ссылки на неё. */
    private fun base(contract: BaseContract): List<String> {
        val members = contract.properties.map { property ->
            val type = renderer.propertyType(property.type, emptySet())
            "    val ${KotlinNames.propertyName(property.name)}: $type${if (property.required) "" else "?"}"
        }
        val declaration = if (members.isEmpty()) {
            listOf(element = "interface ${contract.name}")
        } else {
            listOf(element = "interface ${contract.name} {") + members + "}"
        }
        val serializer = "${contract.name}Serializer"
        val parent = renderer.ref(KotlinSerialNames.baseSerializer, emptySet())
        return declaration + "" + "object $serializer : $parent<${contract.name}>(${contract.name}::class)"
    }

    /** `sealed interface` с вложенными классами [nested] и сериализатором записи `oneOf` (раздел 4.5). */
    private fun sealed(name: String, contract: ResolvedContract, nested: List<List<String>>): List<String> {
        file.useExperimental()
        val annotations = listOf(
            "@${renderer.ref(KotlinSerialNames.serializable, emptySet())}",
            "@${renderer.ref(KotlinSerialNames.classDiscriminator, emptySet())}(\"$VARIANT_KEY\")",
        )
        val body = if (nested.isEmpty()) {
            listOf(element = "sealed interface $name")
        } else {
            val indented = nested.map { lines -> lines.map { line -> "    $line" } }
            listOf(element = "sealed interface $name {") + indented.reduce { joined, next -> joined + "" + next } + "}"
        }
        val serializer = KotlinSerialNames.serializer(contract.id).topName
        val parent = renderer.ref(KotlinSerialNames.oneOfSerializer, emptySet())
        return annotations + body + "" + "object $serializer : $parent<$name>($name.serializer())"
    }

    private fun oneOf(name: String, contract: OneOfContract): List<String> {
        val shadowed = contract.variants.map { variant -> KotlinNames.typeName(variant.name) }.toSet()
        return sealed(name, contract, contract.variants.map { variant -> variant(name, variant, shadowed) })
    }

    /** Класс варианта: без данных, с полями объекта или с одним значением `value` (раздел 7, правила 4, 5). */
    private fun variant(owner: String, variant: Variant, shadowed: Set<String>): List<String> {
        val name = KotlinNames.typeName(variant.name)
        return when (val data = variant.data) {
            is VariantData.Unfolded -> classLines(
                name = name,
                annotations = variantAnnotations(variant.name, valueVariant = false),
                parameters = emptyList(),
                supertypes = listOf(owner),
            )

            is VariantData.Value -> classLines(
                name = name,
                annotations = variantAnnotations(variant.name, valueVariant = true),
                parameters = listOf(KotlinParameter(name = "value", type = renderer.type(data.type, shadowed))),
                supertypes = listOf(owner),
            )

            is VariantData.Fields -> objectClass(
                name = name,
                contract = types.contract(data.contract) as ObjectContract,
                shadowed = shadowed,
                variant = VariantOf(owner = owner, name = variant.name),
            )
        }
    }

    /** `@Serializable`, `@SerialName` — имя варианта в записи; вариант с одним значением — `@ValueVariant`. */
    private fun variantAnnotations(name: String, valueVariant: Boolean): List<String> {
        val annotations = listOf(
            "@${renderer.ref(KotlinSerialNames.serializable, emptySet())}",
            "@${renderer.ref(KotlinSerialNames.serialName, emptySet())}(\"$name\")",
        )
        if (!valueVariant) return annotations
        return annotations + "@${renderer.ref(KotlinSerialNames.valueVariant, emptySet())}"
    }

    /**
     * Класс объекта; реализация базовой модели — с `override` (5.2.1) и `@SerialName` — именем контракта, обобщённый
     * объект — с `T` (5.2.7). [variant] — вариант `oneOf`, классом которого стал объект.
     */
    private fun objectClass(
        name: String,
        contract: ObjectContract,
        shadowed: Set<String>,
        variant: VariantOf?,
    ): List<String> {
        val base = contract.base?.let { id -> renderer.ref(types.typeName(id), shadowed) }
        val genericBase = types.genericBase(contract)?.let { id -> renderer.ref(types.typeName(id), shadowed) }
        val generics = types.genericProperties(contract)
        val parameters = contract.properties.map { property ->
            parameter(contract = contract, property = property, generic = property in generics, shadowed = shadowed)
        }
        val annotations = when {
            variant != null -> variantAnnotations(variant.name, valueVariant = false)
            base != null -> variantAnnotations(contract.name, valueVariant = false)
            else -> listOf(element = "@${renderer.ref(KotlinSerialNames.serializable, shadowed)}")
        }
        return classLines(
            name = name,
            annotations = annotations,
            parameters = parameters,
            supertypes = listOfNotNull(variant?.owner, base),
            typeParameters = genericBase?.let { bound -> "<T : $bound>" }.orEmpty(),
        )
    }

    /** Класс: без параметров — `data object`, иначе `data class` с параметром на строке. */
    private fun classLines(
        name: String,
        annotations: List<String>,
        parameters: List<KotlinParameter>,
        supertypes: List<String>,
        typeParameters: String = "",
    ): List<String> {
        val suffix = if (supertypes.isEmpty()) "" else " : ${supertypes.joinToString(separator = ", ")}"
        if (parameters.isEmpty()) return annotations + "data object $name$suffix"
        val lines = parameters.flatMap { parameter ->
            val modifier = if (parameter.override) "override val" else "val"
            val default = parameter.default?.let { value -> " = $value" }.orEmpty()
            val annotation = parameter.annotation?.let { value -> listOf(element = "    $value") }.orEmpty()
            annotation + "    $modifier ${parameter.name}: ${parameter.type}$default,"
        }
        return annotations + "data class $name$typeParameters(" + lines + ")$suffix"
    }

    private fun parameter(
        contract: ObjectContract,
        property: Property,
        generic: Boolean,
        shadowed: Set<String>,
    ): KotlinParameter {
        val narrowed = isNarrowed(contract, property)
        val type = when {
            generic -> "T"
            narrowed -> narrowed(contract, property, shadowed)
            else -> renderer.propertyType(property.type, shadowed)
        }
        val default = when (val value = property.default) {
            null -> null
            is DefaultValue.Sibling -> KotlinNames.propertyName(value.property)
            is DefaultValue.Value -> defaults.property(value.value, property.type, shadowed)
        }
        val serializable = renderer.ref(KotlinSerialNames.serializable, shadowed)
        return KotlinParameter(
            name = KotlinNames.propertyName(property.name),
            type = if (property.required) type else "$type?",
            default = if (property.required) default else "null",
            override = property.declaredIn == contract.base,
            annotation = if (narrowed) "@$serializable(with = ${narrowedName(contract)}::class)" else null,
        )
    }

    /** `appearance` компонента с одним контекстом — тип варианта (раздел 5.2, правило 5). */
    private fun isNarrowed(contract: ObjectContract, property: Property): Boolean {
        return property.name == APPEARANCE && property.declaredIn == contract.base &&
            contract.appearanceContext.size == 1
    }

    private fun narrowed(contract: ObjectContract, property: Property, shadowed: Set<String>): String {
        return renderer.ref(narrowedOwner(property).nested(narrowedVariant(contract)), shadowed)
    }

    /** Сериализатор сужённого `appearance`: тип — класс варианта, запись — с ключом варианта. */
    private fun narrowedSerializer(contract: ObjectContract): List<String> {
        val property = contract.properties.firstOrNull { candidate -> isNarrowed(contract, candidate) }
            ?: return emptyList()
        val owner = narrowedOwner(property)
        val oneOf = renderer.ref(owner, emptySet())
        val variant = renderer.ref(owner.nested(narrowedVariant(contract)), emptySet())
        val serializer = renderer.ref(KotlinSerialNames.serializer(contractOf(property)), emptySet())
        val parent = renderer.ref(KotlinSerialNames.narrowedSerializer, emptySet())
        val variantName = contract.appearanceContext.single()
        return listOf(
            "",
            "object ${narrowedName(contract)} : $parent<$oneOf, $variant>($serializer, variant = \"$variantName\")",
        )
    }

    private fun narrowedName(contract: ObjectContract): String {
        return "${contract.name}AppearanceSerializer"
    }

    private fun narrowedOwner(property: Property): KotlinTypeName {
        return types.typeName(contractOf(property))
    }

    private fun narrowedVariant(contract: ObjectContract): String {
        return KotlinNames.typeName(contract.appearanceContext.single())
    }

    private fun contractOf(property: Property): ContractId {
        return ((property.type as PropertyType.Single).type as TypeRef.Contract).id
    }
}
