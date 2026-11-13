package io.github.nakulov.designsystem.generator.output.kotlin.name

/**
 * Имя типа Kotlin: пакет и цепочка имён от типа верхнего уровня к вложенному (`Appearance`, `Plain`).
 */
data class KotlinTypeName(
    val packageName: String,
    val names: List<String>,
) {
    /** Имя типа верхнего уровня: по нему пишется `import`. */
    val topName: String
        get() {
            return names.first()
        }

    /** Имя в пакете: `Appearance.Plain`. */
    val localName: String
        get() {
            return names.joinToString(separator = ".")
        }

    /** Полное имя: `io.github.nakulov.designsystem.core.appearance.Appearance.Plain`. */
    val qualifiedName: String
        get() {
            return "$packageName.$localName"
        }

    /** Вложенный тип с именем [name]. */
    fun nested(name: String): KotlinTypeName {
        return copy(names = names + name)
    }
}
