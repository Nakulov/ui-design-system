package io.github.nakulov.designsystem.generator.output.kotlin.declaration

import io.github.nakulov.designsystem.generator.model.ResolvedContract
import io.github.nakulov.designsystem.generator.output.kotlin.file.KotlinFileBuilder
import io.github.nakulov.designsystem.generator.output.kotlin.name.KotlinNames
import io.github.nakulov.designsystem.generator.output.kotlin.type.KotlinTypes

/** Файлы типов верхнего уровня по правилам генерации #239: один контракт — один файл. */
class KotlinDeclarations(
    private val types: KotlinTypes,
) {
    /** Путь файла от корня исходников и его текст. */
    fun file(contract: ResolvedContract): Pair<String, String> {
        val name = KotlinNames.contractType(contract.id)
        val file = KotlinFileBuilder(name.packageName, types.packageNames(name.packageName))
        val lines = KotlinDeclarationRenderer(types, file).declaration(contract)
        val path = "${name.packageName.replace(oldChar = '.', newChar = '/')}/${name.topName}.kt"
        return path to file.build(
            source = "контракта ${contract.id}",
            body = lines.joinToString(separator = "\n") + "\n",
        )
    }
}
