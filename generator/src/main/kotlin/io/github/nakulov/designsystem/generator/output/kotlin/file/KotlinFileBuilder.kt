package io.github.nakulov.designsystem.generator.output.kotlin.file

import io.github.nakulov.designsystem.generator.output.kotlin.name.KotlinTypeName

/** Пакеты, типы которых видны без `import`. */
private val implicitPackages = setOf("kotlin", "kotlin.collections")

/**
 * Файл Kotlin одного типа верхнего уровня: собирает `import` по ссылкам на типы.
 *
 * Простое имя пишется, если оно однозначно в месте ссылки; иначе — полное имя: имя совпадает с вложенным типом,
 * в котором стоит ссылка ([ref], `shadowed`), с типом своего пакета или с уже импортированным типом другого пакета.
 */
class KotlinFileBuilder(
    private val packageName: String,
    private val packageNames: Set<String>,
) {
    private val imports = mutableMapOf<String, String>()

    /** Ссылка на тип [name] в месте, где видны вложенные типы [shadowed]. */
    fun ref(name: KotlinTypeName, shadowed: Set<String>): String {
        val top = name.topName
        val samePackage = name.packageName == packageName
        val implicit = samePackage || name.packageName in implicitPackages
        val imported = imports[top]
        val clash = (top in shadowed) ||
            (!samePackage && top in packageNames) ||
            (imported != null && imported != name.packageName)
        if (clash) return name.qualifiedName
        if (!implicit) imports[top] = name.packageName
        return name.localName
    }

    /** Текст файла: заголовок с источником [source], пакет, `import` по алфавиту и объявление [body]. */
    fun build(source: String, body: String): String {
        val header = "// Сгенерировано генератором (generator/) из $source. Не править.\n\npackage $packageName\n\n"
        val importLines = imports.map { (top, pkg) -> "import $pkg.$top\n" }.sorted().joinToString(separator = "")
        val importBlock = if (importLines.isEmpty()) "" else "$importLines\n"
        return header + importBlock + body
    }
}
