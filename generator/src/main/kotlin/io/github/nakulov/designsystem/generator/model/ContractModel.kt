package io.github.nakulov.designsystem.generator.model

/**
 * Разрешённая модель контрактов: ссылки заменены идентификаторами, наследование и `inline` развёрнуты.
 *
 * Модель не знает о языке вывода: в ней имена и типы формата, а не Kotlin или Swift. Имена классов, регистр
 * вариантов, коллекции и обобщения выбирает вывод языка.
 */
data class ContractModel(val contracts: Map<ContractId, ResolvedContract>)
