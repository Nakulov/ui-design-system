// Общий класс генератора (generator/src/main/resources/shared/). Не править в выводе.

package io.github.nakulov.designsystem.serialization

import io.github.nakulov.designsystem.Rgba
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

/** Имя описания `Rgba`: по нему проверка записи узнаёт цвет (правило 9). */
internal const val RGBA_SERIAL_NAME = "io.github.nakulov.designsystem.Rgba"

private const val OPAQUE = 0xFFu
private const val SHORT_LENGTH = 7

/** `#RRGGBB` → `0xRRGGBBFF`, `#RRGGBBAA` → `0xRRGGBBAA`; непрозрачный цвет пишется коротко (раздел 4.5, правило 6). */
object RgbaSerializer : KSerializer<Rgba> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor(RGBA_SERIAL_NAME, PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): Rgba {
        val hex = decoder.decodeString().removePrefix("#")
        val value = if (hex.length == SHORT_LENGTH - 1) "${hex}FF" else hex
        return Rgba(value.toUInt(radix = 16))
    }

    override fun serialize(encoder: Encoder, value: Rgba) {
        val hex = value.value.toString(radix = 16).uppercase().padStart(length = 8, padChar = '0')
        val text = if (value.value and OPAQUE == OPAQUE) hex.dropLast(n = 2) else hex
        encoder.encodeString("#$text")
    }
}
