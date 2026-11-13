// Общий класс генератора (generator/src/main/resources/shared/). Не править в выводе.

package io.github.nakulov.designsystem.serialization

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

/**
 * Сужённый вариант (раздел 5.2, правило 5): тип — класс варианта [variant] (`Appearance.Plain`), запись — как у
 * `oneOf` [oneOf], с ключом варианта: `{ "plain": { … } }`. Другой вариант — ошибка проверки записи.
 */
abstract class NarrowedSerializer<T : Any, V : T>(
    private val oneOf: OneOfSerializer<T>,
    variant: String,
) : KSerializer<V> {
    override val descriptor: SerialDescriptor = NarrowedDescriptor(oneOf.descriptor as OneOfDescriptor, variant)

    override fun deserialize(decoder: Decoder): V {
        @Suppress("UNCHECKED_CAST")
        return oneOf.deserialize(decoder) as V
    }

    override fun serialize(encoder: Encoder, value: V) {
        oneOf.serialize(encoder, value)
    }
}
