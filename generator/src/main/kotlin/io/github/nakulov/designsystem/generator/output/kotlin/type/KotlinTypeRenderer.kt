package io.github.nakulov.designsystem.generator.output.kotlin.type

import io.github.nakulov.designsystem.generator.model.ContractId
import io.github.nakulov.designsystem.generator.model.ObjectContract
import io.github.nakulov.designsystem.generator.model.PrimitiveType
import io.github.nakulov.designsystem.generator.model.PropertyType
import io.github.nakulov.designsystem.generator.model.TypeRef
import io.github.nakulov.designsystem.generator.output.kotlin.file.KotlinFileBuilder
import io.github.nakulov.designsystem.generator.output.kotlin.name.KotlinNames
import io.github.nakulov.designsystem.generator.output.kotlin.name.KotlinSerialNames
import io.github.nakulov.designsystem.generator.output.kotlin.name.KotlinTypeName

private val list = KotlinTypeName(packageName = "kotlin.collections", names = listOf(element = "List"))
private val set = KotlinTypeName(packageName = "kotlin.collections", names = listOf(element = "Set"))

/** Типы Kotlin в месте использования: примитивы, контракты, массивы (правила генерации #239). */
class KotlinTypeRenderer(
    private val types: KotlinTypes,
    private val file: KotlinFileBuilder,
) {
    /** Тип свойства: `List<T>`, `Set<T>` у `uniqueVariants` (раздел 5.1) или одно значение. */
    fun propertyType(type: PropertyType, shadowed: Set<String>): String {
        return when (type) {
            is PropertyType.Single -> type(type.type, shadowed)

            is PropertyType.Array -> {
                val collection = if (type.uniqueVariants) set else list
                "${file.ref(collection, shadowed)}<${type(type.items, shadowed)}>"
            }
        }
    }

    /** Тип значения; ссылка на обобщённый объект — с базовой моделью: `PressableStates<out Component>`. */
    fun type(ref: TypeRef, shadowed: Set<String>): String {
        return when (ref) {
            is TypeRef.Primitive -> file.ref(primitive(ref.type), shadowed)
            is TypeRef.Contract -> contractType(ref.id, shadowed)
        }
    }

    /** Ссылка на тип [name] — с учётом вложенных типов [shadowed] в месте ссылки. */
    fun ref(name: KotlinTypeName, shadowed: Set<String>): String {
        return file.ref(name, shadowed)
    }

    private fun contractType(id: ContractId, shadowed: Set<String>): String {
        useSerializer(id)
        val name = file.ref(types.typeName(id), shadowed)
        val contract = types.contract(id) as? ObjectContract
        val base = contract?.let { objectContract -> types.genericBase(objectContract) } ?: return name
        useSerializer(base)
        return "$name<out ${file.ref(types.typeName(base), shadowed)}>"
    }

    /** Запись `oneOf`, перечисления и базовой модели — своим сериализатором во всём файле (раздел 4.5). */
    private fun useSerializer(id: ContractId) {
        val contract = types.contract(id)
        if (contract !is ObjectContract) file.useSerializer(KotlinSerialNames.serializer(id))
    }

    private fun primitive(type: PrimitiveType): KotlinTypeName {
        return when (type) {
            PrimitiveType.STRING -> KotlinTypeName(packageName = "kotlin", names = listOf(element = "String"))
            PrimitiveType.BOOLEAN -> KotlinTypeName(packageName = "kotlin", names = listOf(element = "Boolean"))
            PrimitiveType.INTEGER -> KotlinTypeName(packageName = "kotlin", names = listOf(element = "Int"))
            PrimitiveType.FLOAT -> KotlinTypeName(packageName = "kotlin", names = listOf(element = "Float"))
            PrimitiveType.COLOR -> KotlinNames.rgba
            PrimitiveType.PAYLOAD -> KotlinNames.payload
        }
    }
}
