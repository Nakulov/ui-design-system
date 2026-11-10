package io.github.nakulov.designsystem.generator.resolve

import io.github.nakulov.designsystem.generator.format.ContractFile
import io.github.nakulov.designsystem.generator.format.ContractType
import io.github.nakulov.designsystem.generator.format.FormatType
import io.github.nakulov.designsystem.generator.format.OneOfItemDef
import io.github.nakulov.designsystem.generator.format.PropertyDef
import io.github.nakulov.designsystem.generator.model.ContractId
import io.github.nakulov.designsystem.generator.model.ContractKind
import io.github.nakulov.designsystem.generator.model.ContractModel
import io.github.nakulov.designsystem.generator.model.DefaultValue
import io.github.nakulov.designsystem.generator.model.EnumContract
import io.github.nakulov.designsystem.generator.model.EnumValue
import io.github.nakulov.designsystem.generator.model.ObjectContract
import io.github.nakulov.designsystem.generator.model.OneOfContract
import io.github.nakulov.designsystem.generator.model.PrimitiveType
import io.github.nakulov.designsystem.generator.model.Property
import io.github.nakulov.designsystem.generator.model.PropertyType
import io.github.nakulov.designsystem.generator.model.ResolvedContract
import io.github.nakulov.designsystem.generator.model.TypeRef
import io.github.nakulov.designsystem.generator.model.Variant
import io.github.nakulov.designsystem.generator.model.VariantData
import java.io.IOException
import java.nio.file.Path
import kotlin.io.path.isDirectory
import kotlin.io.path.isRegularFile
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.name
import kotlin.io.path.nameWithoutExtension
import kotlin.io.path.readText

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
        for (entity in root.dirs().flatMap { layer -> layer.dirs() }) {
            for ((path, kind) in entityFiles(entity)) {
                val id = ContractId("${entity.parent.name}/${entity.name}/${path.nameWithoutExtension}")
                val file = read(id, path, errors) ?: continue
                files[id] = Loaded(file, kind)
            }
        }
        val model = Resolution(files, errors).run()
        if (errors.isNotEmpty()) throw ContractResolutionException(errors)
        return model
    }

    /** Главный файл сущности (если есть) и её определения из `definitions/`. */
    private fun entityFiles(entity: Path): List<Pair<Path, ContractKind>> {
        val main = entity.resolve("${entity.name.replaceFirstChar { it.uppercase() }}.json")
        val definitions = entity.resolve(DEFINITIONS)
        val helpers = if (definitions.isDirectory()) {
            definitions.listDirectoryEntries(
                "*.json",
            ).sorted()
        } else {
            emptyList()
        }
        return listOfNotNull(main.takeIf { it.isRegularFile() }).map { it to ContractKind.PUBLIC } +
            helpers.map { it to ContractKind.HELPER }
    }

    /** Файл контракта; ошибка чтения или разбора — ошибка контракта, а не падение. */
    private fun read(id: ContractId, path: Path, errors: MutableList<String>): ContractFile? {
        val file = try {
            ContractFile.parse(path.readText())
        } catch (e: IllegalArgumentException) {
            errors += "$id: ${e.firstLine()}"
            null
        } catch (e: IOException) {
            errors += "$id: ${e.firstLine()}"
            null
        }
        if (file != null && file.name != path.nameWithoutExtension) {
            errors += "$id: имя «${file.name}» не совпадает с именем файла"
        }
        return file
    }

    private fun Exception.firstLine(): String? = message?.lineSequence()?.first()

    private fun Path.dirs(): List<Path> = listDirectoryEntries().filter { it.isDirectory() }.sorted()

    private class Loaded(
        val file: ContractFile,
        val kind: ContractKind,
    )

    private class Resolution(
        val files: Map<ContractId, Loaded>,
        val errors: MutableList<String>,
    ) {
        private val ownProperties = mutableMapOf<ContractId, List<Property>>()

        fun run() = ContractModel(files.mapValues { (id, loaded) -> resolve(id, loaded) })

        private fun resolve(id: ContractId, loaded: Loaded): ResolvedContract {
            val file = loaded.file
            return when (file.type) {
                ContractType.OBJECT -> {
                    val base = file.extends?.let { base(id, it) }
                    ObjectContract(
                        id = id,
                        name = file.name,
                        description = file.description,
                        kind = loaded.kind,
                        releaseVersions = file.releaseVersions,
                        base = base,
                        appearanceContext = file.appearanceContext.orEmpty(),
                        properties = base?.let { own(it, files.getValue(it).file) }.orEmpty() + own(id, file),
                    )
                }

                ContractType.ENUM -> EnumContract(
                    id = id,
                    name = file.name,
                    description = file.description,
                    kind = loaded.kind,
                    releaseVersions = file.releaseVersions,
                    values = file.values.orEmpty().map { EnumValue(it.name, it.description, it.releaseVersions) },
                    sample = file.sample,
                )

                ContractType.ONE_OF -> OneOfContract(
                    id = id,
                    name = file.name,
                    description = file.description,
                    kind = loaded.kind,
                    releaseVersions = file.releaseVersions,
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
            val items = def.items
            return when {
                def.type != FormatType.ARRAY -> PropertyType.Single(
                    typeRef(id = id, at = at, type = def.type, ref = def.ref),
                )

                items == null -> {
                    errors += "$at: у массива нет items"
                    PropertyType.Array(TypeRef.Primitive(PrimitiveType.PAYLOAD), false)
                }

                else -> PropertyType.Array(
                    typeRef(id = id, at = at, type = items.type, ref = items.ref),
                    def.uniqueVariants == true,
                )
            }
        }

        /** Значение по умолчанию (раздел 5, правила 1, 4). */
        private fun default(at: String, def: PropertyDef, siblings: List<PropertyDef>): DefaultValue? {
            if (def.default != null && def.defaultsTo != null) errors += "$at: default и defaultsTo вместе"
            if ((def.default != null || def.defaultsTo != null) && !def.required) {
                errors += "$at: значение по умолчанию у необязательного свойства"
            }
            val name = def.defaultsTo
            return when {
                def.default != null -> DefaultValue.Value(def.default)
                name != null -> sibling(at = at, def = def, name = name, siblings = siblings)
                else -> null
            }
        }

        /** `defaultsTo` — соседнее обязательное свойство того же типа без своего `defaultsTo`. */
        private fun sibling(at: String, def: PropertyDef, name: String, siblings: List<PropertyDef>): DefaultValue {
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
            val ref = item.ref
            return when {
                item.type != null -> valueVariant(id, item, item.type)

                ref == null -> {
                    errors += "$id: у элемента oneOf нет ни type, ни \$ref"
                    emptyList()
                }

                else -> refVariants(id, item, ref)
            }
        }

        /** Вариант со значением примитива: `type` и `name`. */
        private fun valueVariant(id: ContractId, item: OneOfItemDef, type: FormatType): List<Variant> {
            if (item.ref != null) errors += "$id: у элемента oneOf и type, и \$ref"
            val name = item.name
            if (name == null) {
                errors += "$id: у элемента oneOf с type нет name"
                return emptyList()
            }
            val data = VariantData.Value(typeRef(id = id, at = "$id.$name", type = type, ref = null))
            return listOf(
                Variant(
                    name = name,
                    description = null,
                    data = data,
                    sample = item.sample,
                    releaseVersions = item.releaseVersions,
                ),
            )
        }

        /** Вариант по ссылке; `inline` — значения `enum` вариантами без данных. */
        private fun refVariants(id: ContractId, item: OneOfItemDef, ref: String): List<Variant> {
            val target = reference(id, ref) ?: return emptyList()
            if (item.inline == true) return inlineVariants(id, item, target)
            val file = files.getValue(target).file
            val name = item.name ?: file.name.replaceFirstChar { it.lowercase() }
            val data = if (file.type == ContractType.OBJECT) {
                VariantData.Fields(target)
            } else {
                VariantData.Value(TypeRef.Contract(target))
            }
            return listOf(
                Variant(
                    name = name,
                    description = null,
                    data = data,
                    sample = item.sample,
                    releaseVersions = item.releaseVersions,
                ),
            )
        }

        private fun inlineVariants(id: ContractId, item: OneOfItemDef, target: ContractId): List<Variant> {
            val file = files.getValue(target).file
            if (item.name != null) errors += "$id: name вместе с inline"
            if (file.type != ContractType.ENUM) {
                errors += "$id: inline ссылается на $target, а не на enum"
                return emptyList()
            }
            return file.values.orEmpty().map {
                Variant(
                    name = it.name,
                    description = it.description,
                    data = VariantData.None,
                    sample = null,
                    releaseVersions = it.releaseVersions,
                )
            }
        }

        private fun typeRef(id: ContractId, at: String, type: FormatType?, ref: String?): TypeRef {
            if ((type == null) == (ref == null)) errors += "$at: нужен ровно один из type и \$ref"
            if (ref != null) {
                val target = reference(id, ref)
                return if (target != null) TypeRef.Contract(target) else TypeRef.Primitive(PrimitiveType.PAYLOAD)
            }
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
