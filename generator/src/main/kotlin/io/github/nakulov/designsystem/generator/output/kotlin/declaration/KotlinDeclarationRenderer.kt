package io.github.nakulov.designsystem.generator.output.kotlin.declaration

import io.github.nakulov.designsystem.generator.model.BaseContract
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
import io.github.nakulov.designsystem.generator.output.kotlin.type.KotlinTypeRenderer
import io.github.nakulov.designsystem.generator.output.kotlin.type.KotlinTypes

/** Свойство базовой модели, которое сужается по `appearanceContext` (раздел 5.2, правило 5). */
private const val APPEARANCE = "appearance"

/**
 * Строки объявления одного типа верхнего уровня; ссылки на типы пишутся через [file].
 *
 * Любой класс выводится одним правилом [classLines]: класс варианта, объекта и варианта со значением отличаются
 * только списком параметров.
 */
class KotlinDeclarationRenderer(
    private val types: KotlinTypes,
    file: KotlinFileBuilder,
) {
    private val renderer = KotlinTypeRenderer(types, file)
    private val defaults = KotlinDefaults(types, renderer)

    /** Объявление контракта [contract] по его конструкции формата. */
    fun declaration(contract: ResolvedContract): List<String> {
        val name = KotlinNames.contractType(contract.id).topName
        return when (contract) {
            is BaseContract -> base(contract)

            is ObjectContract -> objectClass(name = name, contract = contract, shadowed = emptySet(), supertype = null)

            is EnumContract -> sealed(
                name = name,
                nested = contract.values.map { value ->
                    classLines(KotlinNames.typeName(value.name), parameters = emptyList(), supertypes = listOf(name))
                },
            )

            is OneOfContract -> oneOf(name, contract)
        }
    }

    /** Базовая модель — открытый `interface` (раздел 5.2). */
    private fun base(contract: BaseContract): List<String> {
        if (contract.properties.isEmpty()) return listOf(element = "interface ${contract.name}")
        val members = contract.properties.map { property ->
            val type = renderer.propertyType(property.type, emptySet())
            "    val ${KotlinNames.propertyName(property.name)}: $type${if (property.required) "" else "?"}"
        }
        return listOf(element = "interface ${contract.name} {") + members + "}"
    }

    /** `sealed interface` с вложенными классами [nested]; классы разделены пустой строкой. */
    private fun sealed(name: String, nested: List<List<String>>): List<String> {
        if (nested.isEmpty()) return listOf(element = "sealed interface $name")
        val indented = nested.map { lines -> lines.map { line -> "    $line" } }
        return listOf(element = "sealed interface $name {") +
            indented.reduce { joined, next -> joined + "" + next } +
            "}"
    }

    private fun oneOf(name: String, contract: OneOfContract): List<String> {
        val shadowed = contract.variants.map { variant -> KotlinNames.typeName(variant.name) }.toSet()
        return sealed(name, contract.variants.map { variant -> variant(name, variant, shadowed) })
    }

    /** Класс варианта: без данных, с полями объекта или с одним значением `value` (раздел 7, правила 4, 5). */
    private fun variant(owner: String, variant: Variant, shadowed: Set<String>): List<String> {
        val name = KotlinNames.typeName(variant.name)
        return when (val data = variant.data) {
            is VariantData.Unfolded -> classLines(name, parameters = emptyList(), supertypes = listOf(owner))

            is VariantData.Value -> classLines(
                name = name,
                parameters = listOf(KotlinParameter(name = "value", type = renderer.type(data.type, shadowed))),
                supertypes = listOf(owner),
            )

            is VariantData.Fields -> objectClass(
                name = name,
                contract = types.contract(data.contract) as ObjectContract,
                shadowed = shadowed,
                supertype = owner,
            )
        }
    }

    /** Класс объекта; реализация базовой модели — с `override` (5.2.1), обобщённый объект — с `T` (5.2.7). */
    private fun objectClass(
        name: String,
        contract: ObjectContract,
        shadowed: Set<String>,
        supertype: String?,
    ): List<String> {
        val base = contract.base?.let { id -> renderer.ref(types.typeName(id), shadowed) }
        val genericBase = types.genericBase(contract)?.let { id -> renderer.ref(types.typeName(id), shadowed) }
        val generics = types.genericProperties(contract)
        val parameters = contract.properties.map { property ->
            parameter(contract = contract, property = property, generic = property in generics, shadowed = shadowed)
        }
        return classLines(
            name = name,
            parameters = parameters,
            supertypes = listOfNotNull(supertype, base),
            typeParameters = genericBase?.let { bound -> "<T : $bound>" }.orEmpty(),
        )
    }

    /** Класс: без параметров — `data object`, иначе `data class` с параметром на строке. */
    private fun classLines(
        name: String,
        parameters: List<KotlinParameter>,
        supertypes: List<String>,
        typeParameters: String = "",
    ): List<String> {
        val suffix = if (supertypes.isEmpty()) "" else " : ${supertypes.joinToString(separator = ", ")}"
        if (parameters.isEmpty()) return listOf(element = "data object $name$suffix")
        val lines = parameters.map { parameter ->
            val modifier = if (parameter.override) "override val" else "val"
            val default = parameter.default?.let { value -> " = $value" }.orEmpty()
            "    $modifier ${parameter.name}: ${parameter.type}$default,"
        }
        return listOf(element = "data class $name$typeParameters(") + lines + ")$suffix"
    }

    private fun parameter(
        contract: ObjectContract,
        property: Property,
        generic: Boolean,
        shadowed: Set<String>,
    ): KotlinParameter {
        val type = when {
            generic -> "T"
            isNarrowed(contract, property) -> narrowed(contract, property, shadowed)
            else -> renderer.propertyType(property.type, shadowed)
        }
        val default = when (val value = property.default) {
            null -> null
            is DefaultValue.Sibling -> KotlinNames.propertyName(value.property)
            is DefaultValue.Value -> defaults.property(value.value, property.type, shadowed)
        }
        return KotlinParameter(
            name = KotlinNames.propertyName(property.name),
            type = if (property.required) type else "$type?",
            default = if (property.required) default else "null",
            override = property.declaredIn == contract.base,
        )
    }

    /** `appearance` компонента с одним контекстом — тип варианта (раздел 5.2, правило 5). */
    private fun isNarrowed(contract: ObjectContract, property: Property): Boolean {
        return property.name == APPEARANCE && property.declaredIn == contract.base &&
            contract.appearanceContext.size == 1
    }

    private fun narrowed(contract: ObjectContract, property: Property, shadowed: Set<String>): String {
        val ref = (property.type as PropertyType.Single).type as TypeRef.Contract
        val owner = types.typeName(ref.id)
        return renderer.ref(owner.nested(KotlinNames.typeName(contract.appearanceContext.single())), shadowed)
    }
}
