package io.github.nakulov.designsystem.generator.output.kotlin.file

import io.github.nakulov.designsystem.generator.output.kotlin.name.KotlinSerialNames
import io.github.nakulov.designsystem.generator.output.kotlin.name.KotlinTypeName

/** Пакеты, типы которых видны без `import`. */
private val implicitPackages = setOf("kotlin", "kotlin.collections")

/**
 * Файл Kotlin одного типа верхнего уровня: собирает `import` по ссылкам на типы и аннотации файла.
 *
 * Простое имя пишется, если оно однозначно в месте ссылки; иначе — полное имя: имя совпадает с вложенным типом,
 * в котором стоит ссылка ([ref], `shadowed`), с типом своего пакета или с уже импортированным типом другого пакета.
 */
class KotlinFileBuilder(
    private val packageName: String,
    private val packageNames: Set<String>,
) {
    private val imports = mutableMapOf<String, String>()
    private val serializers = sortedSetOf<String>()
    private var experimental = false

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

    /** Сериализатор [name] для свойств этого типа во всём файле: `@file:UseSerializers`. */
    fun useSerializer(name: KotlinTypeName) {
        serializers += "${ref(name, emptySet())}::class"
    }

    /** Файлу нужен `@file:OptIn(ExperimentalSerializationApi::class)`: `@JsonClassDiscriminator`. */
    fun useExperimental() {
        experimental = true
    }

    /** Текст файла: заголовок с источником [source], аннотации файла, пакет, `import` по алфавиту и [body]. */
    fun build(source: String, body: String): String {
        val annotations = listOfNotNull(optInAnnotation(), useSerializersAnnotation())
        val annotationBlock = if (annotations.isEmpty()) "" else annotations.joinToString(separator = "\n") + "\n\n"
        val header = "// Сгенерировано генератором (generator/) из $source. Не править.\n\n"
        val importLines = imports.map { (top, pkg) -> "import $pkg.$top\n" }.sorted().joinToString(separator = "")
        val importBlock = if (importLines.isEmpty()) "" else "$importLines\n"
        return header + annotationBlock + "package $packageName\n\n" + importBlock + body
    }

    private fun optInAnnotation(): String? {
        if (!experimental) return null
        return "@file:OptIn(${ref(KotlinSerialNames.experimental, emptySet())}::class)"
    }

    private fun useSerializersAnnotation(): String? {
        if (serializers.isEmpty()) return null
        val list = serializers.joinToString(separator = ", ")
        return "@file:${ref(KotlinSerialNames.useSerializers, emptySet())}($list)"
    }
}
