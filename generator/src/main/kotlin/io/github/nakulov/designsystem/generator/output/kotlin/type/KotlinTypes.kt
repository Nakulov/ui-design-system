package io.github.nakulov.designsystem.generator.output.kotlin.type

import io.github.nakulov.designsystem.generator.model.BaseContract
import io.github.nakulov.designsystem.generator.model.ContractId
import io.github.nakulov.designsystem.generator.model.ContractModel
import io.github.nakulov.designsystem.generator.model.DefaultValue
import io.github.nakulov.designsystem.generator.model.EnumContract
import io.github.nakulov.designsystem.generator.model.ObjectContract
import io.github.nakulov.designsystem.generator.model.OneOfContract
import io.github.nakulov.designsystem.generator.model.Property
import io.github.nakulov.designsystem.generator.model.PropertyType
import io.github.nakulov.designsystem.generator.model.ResolvedContract
import io.github.nakulov.designsystem.generator.model.TypeRef
import io.github.nakulov.designsystem.generator.model.VariantData
import io.github.nakulov.designsystem.generator.output.kotlin.name.KotlinNames
import io.github.nakulov.designsystem.generator.output.kotlin.name.KotlinTypeName

/**
 * Какие типы Kotlin получают контракты модели: правила «Определения `object`» и перечислений задачи #239.
 *
 * - `object`, на который ссылается один вариант `oneOf`, — класс этого варианта (`Appearance.Plain`).
 * - `object`, на который ссылаются несколько вариантов, — свой класс, только если на него ссылается и свойство.
 * - `enum`, развёрнутый `inline`, — свой тип, только если на него ссылается и свойство.
 * - Остальные контракты, в том числе без ссылок, — типы верхнего уровня.
 */
class KotlinTypes(
    val model: ContractModel,
) {
    private val variantOwners: Map<ContractId, List<KotlinTypeName>> = variantOwners()
    private val unfolded: Set<ContractId> = unfolded()
    private val typeRefs: Set<ContractId> = typeRefs()

    /** Контракты, которые получают свой файл. */
    val topLevel: List<ResolvedContract> = model.contracts.values.filter { contract -> isTopLevel(contract) }

    /** Контракт по идентификатору. */
    fun contract(id: ContractId): ResolvedContract {
        return model.contracts.getValue(id)
    }

    /** Тип Kotlin контракта [id]: свой тип или класс единственного варианта, который на него ссылается. */
    fun typeName(id: ContractId): KotlinTypeName {
        val owners = variantOwners[id].orEmpty()
        return if (owners.size == 1) owners.single() else KotlinNames.contractType(id)
    }

    /** Имена типов верхнего уровня пакета [packageName]. */
    fun packageNames(packageName: String): Set<String> {
        return topLevel.map { contract -> KotlinNames.contractType(contract.id) }
            .filter { name -> name.packageName == packageName }
            .map { name -> name.topName }
            .toSet()
    }

    /**
     * Базовая модель, по которой объект [contract] обобщён: свойства со ссылкой на неё связаны `defaultsTo`
     * (раздел 5.2, правило 7). `null` — объект не обобщённый.
     */
    fun genericBase(contract: ObjectContract): ContractId? {
        return genericProperties(contract).firstOrNull()?.let { property -> contractRef(property) }
    }

    /** Свойства объекта [contract] с типом параметра `T`: ссылка на базовую модель, связанная `defaultsTo`. */
    fun genericProperties(contract: ObjectContract): List<Property> {
        val linked = contract.properties.mapNotNull { property ->
            (property.default as? DefaultValue.Sibling)?.property
        }
        val sources = contract.properties.filter { property -> property.default is DefaultValue.Sibling }
            .map { property -> property.name }
        return contract.properties.filter { property ->
            (property.name in linked || property.name in sources) && isBaseRef(property)
        }
    }

    private fun isBaseRef(property: Property): Boolean {
        val id = contractRef(property) ?: return false
        return contract(id) is BaseContract
    }

    private fun contractRef(property: Property): ContractId? {
        val type = property.type as? PropertyType.Single
        return (type?.type as? TypeRef.Contract)?.id
    }

    private fun isTopLevel(contract: ResolvedContract): Boolean {
        return when (contract) {
            is ObjectContract -> isTopLevelObject(contract.id)
            is EnumContract -> contract.id in typeRefs || contract.id !in unfolded
            is OneOfContract, is BaseContract -> true
        }
    }

    private fun isTopLevelObject(id: ContractId): Boolean {
        val owners = variantOwners[id].orEmpty().size
        return owners == 0 || (owners > 1 && id in typeRefs)
    }

    /** Классы вариантов, данные которых — поля объекта: по всем `oneOf` всех контрактов. */
    private fun variantOwners(): Map<ContractId, List<KotlinTypeName>> {
        return model.contracts.values.filterIsInstance<OneOfContract>()
            .flatMap { oneOf ->
                oneOf.variants.mapNotNull { variant ->
                    val data = variant.data as? VariantData.Fields
                    data?.let { fields ->
                        fields.contract to KotlinNames.contractType(oneOf.id).nested(KotlinNames.typeName(variant.name))
                    }
                }
            }
            .groupBy(keySelector = { pair -> pair.first }, valueTransform = { pair -> pair.second })
    }

    private fun unfolded(): Set<ContractId> {
        return model.contracts.values.filterIsInstance<OneOfContract>()
            .flatMap { oneOf -> oneOf.variants }
            .mapNotNull { variant -> (variant.data as? VariantData.Unfolded)?.contract }
            .toSet()
    }

    /** Контракты, на которые ссылаются свойства, элементы массивов и варианты со значением. */
    private fun typeRefs(): Set<ContractId> {
        val properties = model.contracts.values.flatMap { contract -> properties(contract) }.map { property ->
            when (val type = property.type) {
                is PropertyType.Single -> type.type
                is PropertyType.Array -> type.items
            }
        }
        val values = model.contracts.values.filterIsInstance<OneOfContract>()
            .flatMap { oneOf -> oneOf.variants }
            .mapNotNull { variant -> (variant.data as? VariantData.Value)?.type }
        return (properties + values).filterIsInstance<TypeRef.Contract>().map { ref -> ref.id }.toSet()
    }

    private fun properties(contract: ResolvedContract): List<Property> {
        return when (contract) {
            is ObjectContract -> contract.properties
            is BaseContract -> contract.properties
            is EnumContract, is OneOfContract -> emptyList()
        }
    }
}
