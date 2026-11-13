// Общий класс генератора (generator/src/main/resources/shared/). Не править в выводе.

package io.github.nakulov.designsystem.serialization

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialInfo

/**
 * Вариант `oneOf`, данные которого — одно значение (раздел 7, правила 4, 5): `Fixed(val value: Size)` пишется
 * `{ "fixed": "m" }`, без поля `value`.
 */
@OptIn(ExperimentalSerializationApi::class)
@SerialInfo
@Target(AnnotationTarget.CLASS)
annotation class ValueVariant
