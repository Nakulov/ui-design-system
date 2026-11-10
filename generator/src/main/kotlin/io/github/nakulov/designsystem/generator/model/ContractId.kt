package io.github.nakulov.designsystem.generator.model

/** Идентификатор `<слой>/<сущность>/<Имя>` (schema-format.md, раздел 6). */
@JvmInline
value class ContractId(val value: String) {
    override fun toString(): String = value
}
