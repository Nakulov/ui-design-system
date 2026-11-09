package io.github.nakulov.designsystem.generator.model

import io.github.nakulov.designsystem.generator.format.ReleaseVersions
import kotlinx.serialization.json.JsonElement

/**
 * Разрешённая модель контрактов: ссылки заменены идентификаторами, наследование и `inline` развёрнуты.
 *
 * Модель не знает о языке вывода: в ней имена и типы формата, а не Kotlin или Swift. Имена классов, регистр
 * вариантов, коллекции и обобщения выбирает вывод языка.
 */
data class ContractModel(val contracts: Map<ContractId, ResolvedContract>) {
    operator fun get(id: ContractId): ResolvedContract =
        contracts[id] ?: throw NoSuchElementException("Контракт $id не найден")
}

/** Идентификатор `<слой>/<сущность>/<Имя>` (schema-format.md, раздел 6). */
@JvmInline
value class ContractId(val value: String) {
    override fun toString(): String = value
}

/** Главный контракт сущности или вспомогательное определение из `definitions/` (раздел 11.1, правила 2, 3). */
enum class ContractKind { PUBLIC, HELPER }

sealed interface ResolvedContract {
    val id: ContractId
    val name: String
    val description: String
    val kind: ContractKind
    val releaseVersions: ReleaseVersions?
}

/**
 * Контракт типа `object`. У компонента [base] — базовая модель, её свойства идут в [properties] первыми
 * (раздел 5.2, правило 1), [appearanceContext] — имена вариантов `Appearance`.
 */
data class ObjectContract(
    override val id: ContractId,
    override val name: String,
    override val description: String,
    override val kind: ContractKind,
    override val releaseVersions: ReleaseVersions?,
    val base: ContractId?,
    val appearanceContext: List<String>,
    val properties: List<Property>,
) : ResolvedContract

/** Контракт типа `enum` (раздел 4.4). */
data class EnumContract(
    override val id: ContractId,
    override val name: String,
    override val description: String,
    override val kind: ContractKind,
    override val releaseVersions: ReleaseVersions?,
    val values: List<EnumValue>,
    val sample: JsonElement?,
) : ResolvedContract

/** Контракт типа `oneOf`: варианты после разворачивания (раздел 7). */
data class OneOfContract(
    override val id: ContractId,
    override val name: String,
    override val description: String,
    override val kind: ContractKind,
    override val releaseVersions: ReleaseVersions?,
    val variants: List<Variant>,
    val sample: JsonElement?,
) : ResolvedContract

data class EnumValue(
    val name: String,
    val description: String,
    val releaseVersions: ReleaseVersions?,
)

/**
 * Свойство объекта (раздел 5). [declaredIn] — контракт, где свойство объявлено: у унаследованных свойств компонента
 * это базовая модель.
 */
data class Property(
    val name: String,
    val description: String,
    val type: PropertyType,
    val required: Boolean,
    val default: DefaultValue?,
    val sample: JsonElement?,
    val releaseVersions: ReleaseVersions?,
    val declaredIn: ContractId,
)

/** Примитивный тип (раздел 4.1). */
enum class PrimitiveType { STRING, BOOLEAN, INTEGER, FLOAT, COLOR, PAYLOAD }

/** Тип значения: примитивный тип или контракт. */
sealed interface TypeRef {
    data class Primitive(val type: PrimitiveType) : TypeRef
    data class Contract(val id: ContractId) : TypeRef
}

sealed interface PropertyType {
    /** Одно значение. */
    data class Single(val type: TypeRef) : PropertyType

    /** Массив (раздел 5.1); [uniqueVariants] — каждый вариант элементов не более одного раза. */
    data class Array(val items: TypeRef, val uniqueVariants: Boolean) : PropertyType
}

/** Значение по умолчанию обязательного свойства (раздел 5). */
sealed interface DefaultValue {
    /** `default`: значение, записанное по разделу 4.5. */
    data class Value(val value: JsonElement) : DefaultValue

    /** `defaultsTo`: значение соседнего свойства того же объекта. */
    data class Sibling(val property: String) : DefaultValue
}

/**
 * Вариант `oneOf` после разворачивания (раздел 7). Имя — имя варианта в записи значения (раздел 4.5).
 * [description] есть у варианта из развёрнутого перечисления — описание значения.
 */
data class Variant(
    val name: String,
    val description: String?,
    val data: VariantData,
    val sample: JsonElement?,
    val releaseVersions: ReleaseVersions?,
)

sealed interface VariantData {
    /** Вариант из развёрнутого перечисления: данных нет, запись — строка с именем. */
    data object None : VariantData

    /** Данные — свойства объекта [contract] (раздел 7, правило 4). */
    data class Fields(val contract: ContractId) : VariantData

    /** Данные — одно значение типа [type] (раздел 7, правила 4, 5). */
    data class Value(val type: TypeRef) : VariantData
}
