package io.github.nakulov.designsystem.generator.model

sealed interface VariantData {
    /**
     * Вариант из значения перечисления [contract], развёрнутого `inline` (раздел 7, правило 1): данных нет, запись —
     * строка с именем.
     */
    data class Unfolded(
        val contract: ContractId,
    ) : VariantData

    /** Данные — свойства объекта [contract] (раздел 7, правило 4). */
    data class Fields(
        val contract: ContractId,
    ) : VariantData

    /** Данные — одно значение типа [type] (раздел 7, правила 4, 5). */
    data class Value(
        val type: TypeRef,
    ) : VariantData
}
