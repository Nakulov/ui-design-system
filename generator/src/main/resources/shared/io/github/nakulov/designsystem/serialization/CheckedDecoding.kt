// Общий класс генератора (generator/src/main/resources/shared/). Не править в выводе.

package io.github.nakulov.designsystem.serialization

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.plus

/** Метка в модуле сериализации: запись уже проверена внешним разбором, вложенные разборы её не проверяют. */
object CheckedMark

/** Сериализатор метки [CheckedMark]: нужен только для регистрации `contextual`, данных не пишет. */
object CheckedMarkSerializer : KSerializer<CheckedMark> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("CheckedMark", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): CheckedMark {
        return CheckedMark
    }

    override fun serialize(encoder: Encoder, value: CheckedMark) {
        encoder.encodeString("")
    }
}

/**
 * Разбор [element] по описанию [descriptor]: самый внешний разбор проверяет запись целиком ([ContractChecker]) и
 * передаёт [decode] `Json` с меткой [CheckedMark]; вложенные разборы получают его и не проверяют повторно.
 */
fun <T> JsonDecoder.decodeChecked(descriptor: SerialDescriptor, element: JsonElement, decode: (Json) -> T): T {
    val module = json.serializersModule
    if (module.getContextual(CheckedMark::class) != null) return decode(json)
    ContractChecker(module).check(element, descriptor)
    val checked = Json(json) {
        serializersModule = module + SerializersModule { contextual(CheckedMark::class, CheckedMarkSerializer) }
    }
    return decode(checked)
}
