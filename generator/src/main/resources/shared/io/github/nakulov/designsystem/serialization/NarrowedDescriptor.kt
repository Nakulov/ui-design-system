// Общий класс генератора (generator/src/main/resources/shared/). Не править в выводе.

package io.github.nakulov.designsystem.serialization

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SealedSerializationApi
import kotlinx.serialization.descriptors.SerialDescriptor

/** Описание сужённого варианта: запись `oneOf` [oneOf], где допустим только вариант [variant]. */
@OptIn(ExperimentalSerializationApi::class, SealedSerializationApi::class)
class NarrowedDescriptor(
    val oneOf: OneOfDescriptor,
    val variant: String,
) : SerialDescriptor by oneOf {
    override val serialName: String = "${oneOf.serialName}.$variant"
}
