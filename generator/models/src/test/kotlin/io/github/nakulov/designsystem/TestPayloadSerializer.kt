package io.github.nakulov.designsystem

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.jsonObject

/** Разбор [TestPayload]: объект JSON целиком; подключается разбором по умолчанию для `Payload` без дискриминатора. */
object TestPayloadSerializer : KSerializer<TestPayload> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor(serialName = "TestPayload")

    override fun deserialize(decoder: Decoder): TestPayload {
        return TestPayload((decoder as JsonDecoder).decodeJsonElement().jsonObject)
    }

    override fun serialize(encoder: Encoder, value: TestPayload) {
        (encoder as JsonEncoder).encodeJsonElement(value.data)
    }
}
