package io.github.nakulov.designsystem.generator.model

import io.github.nakulov.designsystem.generator.format.ContractFile
import io.github.nakulov.designsystem.generator.format.ContractType
import io.github.nakulov.designsystem.generator.format.FormatType
import io.github.nakulov.designsystem.generator.format.OneOfItemDef
import io.github.nakulov.designsystem.generator.format.PropertyDef
import java.nio.file.Path
import kotlin.io.path.isDirectory
import kotlin.io.path.isRegularFile
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.name
import kotlin.io.path.nameWithoutExtension
import kotlin.io.path.readText

/** Ошибки чтения и разрешения контрактов: все найденные сразу, по строке на ошибку. */
class ContractResolutionException(val errors: List<String>) :
    IllegalStateException(errors.joinToString("\n", prefix = "Контракты не разрешены:\n"))

/**
 * Читает контракты каталога `contracts/` и разрешает связи между ними (schema-format.md, разделы 5–7, 11.1).
 *
 * Структуру файлов и записи значений проверяет tools/validate.py; здесь — только то, без чего модель не построить.
 */
object ContractResolver {
    /** Базовая модель компонента (раздел 5.2). */
    val COMPONENT = ContractId("core/component/Component")

    private const val DEFINITIONS = "definitions"

    /** Все контракты `<root>/<слой>/<сущность>/`: главный и вспомогательные определения. */
    fun load(root: Path): ContractModel {
        val errors = mutableListOf<String>()
        val files = mutableMapOf<ContractId, Loaded>()
        for (layer in root.dirs()) for (entity in layer.dirs()) {
            val main = entity.resolve("${entity.name.replaceFirstChar { it.uppercase() }}.json")
            val helpers = entity.resolve(DEFINITIONS).takeIf { it.isDirectory() }
                ?.listDirectoryEntries("*.json")?.sorted().orEmpty()
            val paths = listOfNotNull(main.takeIf { it.isRegularFile() }).map { it to ContractKind.PUBLIC } +
                helpers.map { it to ContractKind.HELPER }
            for ((path, kind) in paths) {
                val id = ContractId("${layer.name}/${entity.name}/${path.nameWithoutExtension}")
                val file = try {
                    ContractFile.parse(path.readText())
                } catch (e: Exception) {
                    errors += "$id: ${e.message?.lineSequence()?.first()}"
                    continue
                }
                if (file.name != path.nameWithoutExtension) errors += "$id: имя «${file.name}» не совпадает с именем файла"
                files[id] = Loaded(file, kind)
            }
        }
        val model = Resolution(files, errors).run()
        if (errors.isNotEmpty()) throw ContractResolutionException(errors)
        return model
    }

    private fun Path.dirs(): List<Path> = listDirectoryEntries().filter { it.isDirectory() }.sorted()

    private class Loaded(val file: ContractFile, val kind: ContractKind)

    private class Resolution(val files: Map<ContractId, Loaded>, val errors: MutableList<String>) {
        private val ownProperties = mutableMapOf<ContractId, List<Property>>()

        fun run() = ContractModel(files.mapValues { (id, loaded) -> resolve(id, loaded) })

        private fun resolve(id: ContractId, loaded: Loaded): ResolvedContract {
            val file = loaded.file
            return when (file.type) {
                ContractType.OBJECT -> {
                    val base = file.extends?.let { base(id, it) }
                    ObjectContract(
                        id, file.name, file.description, loaded.kind, file.releaseVersions,
                        base = base,
                        appearanceContext = file.appearanceContext.orEmpty(),
                        properties = base?.let { own(it, files.getValue(it).file) }.orEmpty() + own(id, file),
                    )
                }
                ContractType.ENUM -> EnumContract(
                    id, file.name, file.description, loaded.kind, file.releaseVersions,
                    values = file.values.orEmpty().map { EnumValue(it.name, it.description, it.releaseVersions) },
                    sample = file.sample,
                )
                ContractType.ONE_OF -> OneOfContract(
                    id, file.name, file.description, loaded.kind, file.releaseVersions,
                    variants = variants(id, file),
                    sample = file.sample,
                )
            }
        }

        /** `extends` — только ссылка на `Component` (раздел 5.2, правило 2). */
        private fun base(id: ContractId, ref: String): ContractId? {
            val base = reference(id, ref) ?: return null
            if (base != COMPONENT) errors += "$id: extends ссылается на $base, а не на $COMPONENT"
            return base.takeIf { it == COMPONENT }
        }

        /** Собственные свойства; у компонента базовые идут перед ними (раздел 5.2, правило 1). */
        private fun own(id: ContractId, file: ContractFile): List<Property> = ownProperties.getOrPut(id) {
            val defs = file.properties.orEmpty()
            defs.map { property(id, it, defs) }
        }

        private fun property(id: ContractId, def: PropertyDef, siblings: List<PropertyDef>): Property {
            val at = "$id.${def.name}"
            return Property(
                name = def.name,
                description = def.description,
                type = propertyType(id, at, def),
                required = def.required,
                default = default(at, def, siblings),
                sample = def.sample,
                releaseVersions = def.releaseVersions,
                declaredIn = id,
            )
        }

        private fun propertyType(id: ContractId, at: String, def: PropertyDef): PropertyType {
            if (def.type == FormatType.ARRAY) {
                val items = def.items
                if (items == null) {
                    errors += "$at: у массива нет items"
                    return PropertyType.Array(TypeRef.Primitive(PrimitiveType.PAYLOAD), false)
                }
                return PropertyType.Array(typeRef(id, at, items.type, items.ref), def.uniqueVariants == true)
            }
            return PropertyType.Single(typeRef(id, at, def.type, def.ref))
        }

        /** Значение по умолчанию (раздел 5, правила 1, 4). */
        private fun default(at: String, def: PropertyDef, siblings: List<PropertyDef>): DefaultValue? {
            if (def.default != null && def.defaultsTo != null) errors += "$at: default и defaultsTo вместе"
            if ((def.default != null || def.defaultsTo != null) && !def.required) {
                errors += "$at: значение по умолчанию у необязательного свойства"
            }
            def.default?.let { return DefaultValue.Value(it) }
            val name = def.defaultsTo ?: return null
            val target = siblings.find { it.name == name }
            when {
                target == null || target === def -> errors += "$at: defaultsTo «$name» — не соседнее свойство"
                !target.required || target.defaultsTo != null ->
                    errors += "$at: defaultsTo «$name» — свойство необязательное или само с defaultsTo"
                target.type != def.type || target.ref != def.ref || target.items != def.items ->
                    errors += "$at: тип отличается от типа «$name»"
            }
            return DefaultValue.Sibling(name)
        }

        /** Варианты `oneOf` (раздел 7). */
        private fun variants(id: ContractId, file: ContractFile): List<Variant> {
            val variants = file.oneOf.orEmpty().flatMap { variant(id, it) }
            variants.groupBy { it.name }.filterValues { it.size > 1 }.keys
                .forEach { errors += "$id: вариант «$it» повторяется" }
            return variants
        }

        private fun variant(id: ContractId, item: OneOfItemDef): List<Variant> {
            if (item.type != null) {
                if (item.ref != null) errors += "$id: у элемента oneOf и type, и \$ref"
                val name = item.name ?: run {
                    errors += "$id: у элемента oneOf с type нет name"
                    return emptyList()
                }
                val type = typeRef(id, "$id.$name", item.type, null)
                return listOf(Variant(name, null, VariantData.Value(type), item.sample, item.releaseVersions))
            }
            val target = item.ref?.let { reference(id, it) } ?: run {
                if (item.ref == null) errors += "$id: у элемента oneOf нет ни type, ни \$ref"
                return emptyList()
            }
            val file = files.getValue(target).file
            if (item.inline == true) {
                if (item.name != null) errors += "$id: name вместе с inline"
                if (file.type != ContractType.ENUM) {
                    errors += "$id: inline ссылается на $target, а не на enum"
                    return emptyList()
                }
                return file.values.orEmpty()
                    .map { Variant(it.name, it.description, VariantData.None, null, it.releaseVersions) }
            }
            val name = item.name ?: file.name.replaceFirstChar { it.lowercase() }
            val data = if (file.type == ContractType.OBJECT) {
                VariantData.Fields(target)
            } else {
                VariantData.Value(TypeRef.Contract(target))
            }
            return listOf(Variant(name, null, data, item.sample, item.releaseVersions))
        }

        private fun typeRef(id: ContractId, at: String, type: FormatType?, ref: String?): TypeRef {
            if ((type == null) == (ref == null)) errors += "$at: нужен ровно один из type и \$ref"
            if (ref != null) return reference(id, ref)?.let { TypeRef.Contract(it) } ?: TypeRef.Primitive(PrimitiveType.PAYLOAD)
            return when (type) {
                FormatType.STRING -> TypeRef.Primitive(PrimitiveType.STRING)
                FormatType.BOOLEAN -> TypeRef.Primitive(PrimitiveType.BOOLEAN)
                FormatType.INTEGER -> TypeRef.Primitive(PrimitiveType.INTEGER)
                FormatType.FLOAT -> TypeRef.Primitive(PrimitiveType.FLOAT)
                FormatType.COLOR -> TypeRef.Primitive(PrimitiveType.COLOR)
                FormatType.PAYLOAD, null -> TypeRef.Primitive(PrimitiveType.PAYLOAD)
                FormatType.ARRAY -> {
                    errors += "$at: вложенный массив"
                    TypeRef.Primitive(PrimitiveType.PAYLOAD)
                }
            }
        }

        /** Ссылка по разделу 6: имя без `/` — контракт своей сущности, с `/` — идентификатор. */
        private fun reference(from: ContractId, ref: String): ContractId? {
            val id = if ('/' in ref) ContractId(ref) else ContractId("${from.value.substringBeforeLast('/')}/$ref")
            if (id !in files) {
                errors += "$from: ссылка «$ref» не найдена ($id)"
                return null
            }
            return id
        }
    }
}
