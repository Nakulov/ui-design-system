package io.github.nakulov.designsystem.generator.output.kotlin

import io.github.nakulov.designsystem.generator.model.ContractModel
import io.github.nakulov.designsystem.generator.output.kotlin.declaration.KotlinDeclarations
import io.github.nakulov.designsystem.generator.output.kotlin.declaration.KotlinModuleRenderer
import io.github.nakulov.designsystem.generator.output.kotlin.type.KotlinTypes

/**
 * Модели Kotlin из разрешённой модели контрактов (#239): по правилам конструкций формата, а не по компонентам.
 *
 * Модуль `generator/models` собирает вывод в jar `io.github.nakulov:design-system-kotlin`.
 */
object KotlinOutput {
    /**
     * Общие классы, не зависящие от контрактов: `Rgba`, `Payload` и разбор (раздел 4.5). Исходники — ресурсы
     * `shared/<путь>` генератора; генератор кладёт их в вывод без изменений.
     */
    val sharedFiles: List<String> = listOf(
        "io/github/nakulov/designsystem/Payload.kt",
        "io/github/nakulov/designsystem/Rgba.kt",
        "io/github/nakulov/designsystem/serialization/BaseDescriptor.kt",
        "io/github/nakulov/designsystem/serialization/BaseSerializer.kt",
        "io/github/nakulov/designsystem/serialization/CheckedDecoding.kt",
        "io/github/nakulov/designsystem/serialization/ContractChecker.kt",
        "io/github/nakulov/designsystem/serialization/NarrowedDescriptor.kt",
        "io/github/nakulov/designsystem/serialization/NarrowedSerializer.kt",
        "io/github/nakulov/designsystem/serialization/OneOfDescriptor.kt",
        "io/github/nakulov/designsystem/serialization/OneOfSerializer.kt",
        "io/github/nakulov/designsystem/serialization/RgbaSerializer.kt",
        "io/github/nakulov/designsystem/serialization/ValueVariant.kt",
    )

    /** Файлы вывода из контрактов: путь от корня исходников → текст; по пути, по алфавиту. Без [sharedFiles]. */
    fun generate(model: ContractModel): Map<String, String> {
        val types = KotlinTypes(model)
        val declarations = KotlinDeclarations(types)
        val files = types.topLevel.map { contract -> declarations.file(contract) } + KotlinModuleRenderer(types).file()
        return files.sortedBy { file -> file.first }.toMap()
    }
}
