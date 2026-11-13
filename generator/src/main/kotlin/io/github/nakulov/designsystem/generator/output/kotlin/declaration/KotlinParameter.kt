package io.github.nakulov.designsystem.generator.output.kotlin.declaration

/**
 * Параметр конструктора класса: `override val appearance: Appearance.Plain`, `val gap: Spacing = Spacing.Zero`.
 * [type] и [default] — готовые выражения Kotlin; [default] `null` — без значения по умолчанию; [annotation] — аннотация
 * параметра, например сериализатор сужённого варианта.
 */
data class KotlinParameter(
    val name: String,
    val type: String,
    val default: String? = null,
    val override: Boolean = false,
    val annotation: String? = null,
)
