package io.github.nakulov.designsystem.generator.model

sealed interface PropertyType {
    /** Одно значение. */
    data class Single(
        val type: TypeRef,
    ) : PropertyType

    /** Массив (раздел 5.1); [uniqueVariants] — каждый вариант элементов не более одного раза. */
    data class Array(
        val items: TypeRef,
        val uniqueVariants: Boolean,
    ) : PropertyType
}
