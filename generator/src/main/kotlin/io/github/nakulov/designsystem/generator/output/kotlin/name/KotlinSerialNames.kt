package io.github.nakulov.designsystem.generator.output.kotlin.name

import io.github.nakulov.designsystem.generator.model.ContractId

/** Пакет kotlinx.serialization. */
private const val SERIALIZATION = "kotlinx.serialization"

/** Пакет общих классов разбора вывода (`generator/src/main/resources/shared/`). */
private const val SHARED_SERIALIZATION = "$ROOT_PACKAGE.serialization"

/** Ключ имени варианта в форме `sealed`-сериализатора плагина: `VARIANT_KEY` общих классов. */
const val VARIANT_KEY = "#variant"

/** Имена типов разбора, на которые ссылается вывод: kotlinx.serialization и общие классы. */
object KotlinSerialNames {
    /** `@Serializable`. */
    val serializable = KotlinTypeName(packageName = SERIALIZATION, names = listOf(element = "Serializable"))

    /** `@SerialName`. */
    val serialName = KotlinTypeName(packageName = SERIALIZATION, names = listOf(element = "SerialName"))

    /** `@file:UseSerializers`. */
    val useSerializers = KotlinTypeName(packageName = SERIALIZATION, names = listOf(element = "UseSerializers"))

    /** `ExperimentalSerializationApi` для `@file:OptIn`. */
    val experimental = KotlinTypeName(
        packageName = SERIALIZATION,
        names = listOf(element = "ExperimentalSerializationApi"),
    )

    /** `@JsonClassDiscriminator`. */
    val classDiscriminator = KotlinTypeName(
        packageName = "$SERIALIZATION.json",
        names = listOf(element = "JsonClassDiscriminator"),
    )

    /** `SerializersModule`. */
    val serializersModule = KotlinTypeName(
        packageName = "$SERIALIZATION.modules",
        names = listOf(element = "SerializersModule"),
    )

    /** Функция `polymorphic` модуля сериализации. */
    val polymorphic = KotlinTypeName(packageName = "$SERIALIZATION.modules", names = listOf(element = "polymorphic"))

    /** Функция `subclass` модуля сериализации. */
    val subclass = KotlinTypeName(packageName = "$SERIALIZATION.modules", names = listOf(element = "subclass"))

    /** `@ValueVariant`. */
    val valueVariant = KotlinTypeName(packageName = SHARED_SERIALIZATION, names = listOf(element = "ValueVariant"))

    /** `OneOfSerializer`. */
    val oneOfSerializer = KotlinTypeName(
        packageName = SHARED_SERIALIZATION,
        names = listOf(element = "OneOfSerializer"),
    )

    /** `BaseSerializer`. */
    val baseSerializer = KotlinTypeName(packageName = SHARED_SERIALIZATION, names = listOf(element = "BaseSerializer"))

    /** `NarrowedSerializer`. */
    val narrowedSerializer = KotlinTypeName(
        packageName = SHARED_SERIALIZATION,
        names = listOf(element = "NarrowedSerializer"),
    )

    /** Сериализатор записи контракта [id] (`oneOf`, `enum`, `base`): объект `<Имя>Serializer` в его файле. */
    fun serializer(id: ContractId): KotlinTypeName {
        val type = KotlinNames.contractType(id)
        return type.copy(names = listOf(element = "${type.topName}Serializer"))
    }
}
