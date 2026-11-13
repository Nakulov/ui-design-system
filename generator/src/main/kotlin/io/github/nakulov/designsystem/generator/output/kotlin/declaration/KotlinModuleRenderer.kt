package io.github.nakulov.designsystem.generator.output.kotlin.declaration

import io.github.nakulov.designsystem.generator.model.BaseContract
import io.github.nakulov.designsystem.generator.model.ObjectContract
import io.github.nakulov.designsystem.generator.output.kotlin.file.KotlinFileBuilder
import io.github.nakulov.designsystem.generator.output.kotlin.name.KotlinSerialNames
import io.github.nakulov.designsystem.generator.output.kotlin.name.ROOT_PACKAGE
import io.github.nakulov.designsystem.generator.output.kotlin.type.KotlinTypes

/** Имя модуля сериализации библиотеки. */
private const val MODULE = "DesignSystemModule"

/**
 * Файл `DesignSystemModule`: реализации базовых моделей библиотеки для разбора ссылок на них (раздел 5.2). Продукт
 * подключает его `include(DesignSystemModule)` и регистрирует свои реализации.
 */
class KotlinModuleRenderer(
    private val types: KotlinTypes,
) {
    /** Путь файла от корня исходников и его текст. */
    fun file(): Pair<String, String> {
        val file = KotlinFileBuilder(ROOT_PACKAGE, types.packageNames(ROOT_PACKAGE) + MODULE)
        val bases = types.topLevel.filterIsInstance<BaseContract>()
        val registrations = bases.flatMap { base ->
            val baseType = file.ref(types.typeName(base.id), emptySet())
            val serializer = file.ref(KotlinSerialNames.serializer(base.id), emptySet())
            val polymorphic = file.ref(KotlinSerialNames.polymorphic, emptySet())
            val implementations = types.topLevel.filterIsInstance<ObjectContract>()
                .filter { contract -> contract.base == base.id }
                .map { contract ->
                    val subclass = file.ref(KotlinSerialNames.subclass, emptySet())
                    "        $subclass(${file.ref(types.typeName(contract.id), emptySet())}::class)"
                }
            listOf("    contextual($baseType::class, $serializer)", "    $polymorphic($baseType::class) {") +
                implementations +
                "    }"
        }
        val module = file.ref(KotlinSerialNames.serializersModule, emptySet())
        val lines = listOf(
            "/** Реализации базовых моделей библиотеки: продукт подключает `include($MODULE)`. */",
            "val $MODULE: $module = $module {",
        ) + registrations + "}"
        val path = "${ROOT_PACKAGE.replace(oldChar = '.', newChar = '/')}/$MODULE.kt"
        return path to file.build(source = "контрактов типа base", body = lines.joinToString(separator = "\n") + "\n")
    }
}
