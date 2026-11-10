package io.github.nakulov.designsystem.generator.model

/** Тип значения: примитивный тип или контракт. */
sealed interface TypeRef {
    data class Primitive(val type: PrimitiveType) : TypeRef
    data class Contract(val id: ContractId) : TypeRef
}
