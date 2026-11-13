package io.github.nakulov.designsystem.generator.output.kotlin.name

import io.github.nakulov.designsystem.generator.model.ContractId

/** Корневой пакет моделей. */
const val ROOT_PACKAGE = "io.github.nakulov.designsystem"

private val hardKeywords = setOf(
    "as", "break", "class", "continue", "do", "else", "false", "for", "fun", "if", "in", "interface", "is", "null",
    "object", "package", "return", "super", "this", "throw", "true", "try", "typealias", "typeof", "val", "var",
    "when", "while",
)

/** Имена Kotlin по разделу 10 и пакеты по идентификатору контракта. */
object KotlinNames {
    /** Цвет `0xRRGGBBAA` (`color`, раздел 4.1): общий тип вывода. */
    val rgba = KotlinTypeName(packageName = ROOT_PACKAGE, names = listOf(element = "Rgba"))

    /** Данные приложения (`payload`, раздел 4.1): общий тип вывода. */
    val payload = KotlinTypeName(packageName = ROOT_PACKAGE, names = listOf(element = "Payload"))

    /** Имя типа из имени значения или варианта: `xs2` → `Xs2`, `onPrimary` → `OnPrimary`. */
    fun typeName(name: String): String {
        return name.replaceFirstChar { char -> char.uppercaseChar() }
    }

    /** Имя свойства; жёсткое ключевое слово Kotlin — в обратных кавычках. */
    fun propertyName(name: String): String {
        return if (name in hardKeywords) "`$name`" else name
    }

    /** Пакет сущности: `<корень>.<слой>.<сущность>`; `definitions/` в пакет не входит. */
    fun packageOf(id: ContractId): String {
        return "$ROOT_PACKAGE.${id.value.substringBeforeLast(delimiter = '/').replace(oldChar = '/', newChar = '.')}"
    }

    /** Тип верхнего уровня, названный по контракту. */
    fun contractType(id: ContractId): KotlinTypeName {
        return KotlinTypeName(packageOf(id), listOf(id.value.substringAfterLast(delimiter = '/')))
    }
}
