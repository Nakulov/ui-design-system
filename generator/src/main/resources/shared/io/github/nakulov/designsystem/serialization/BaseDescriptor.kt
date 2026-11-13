// Общий класс генератора (generator/src/main/resources/shared/). Не править в выводе.

package io.github.nakulov.designsystem.serialization

import kotlin.reflect.KClass
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SealedSerializationApi
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.json.JsonObject

/** Описание ссылки на базовую модель [base]: по нему проверка записи ищет реализацию в модуле сериализации. */
@OptIn(ExperimentalSerializationApi::class, SealedSerializationApi::class)
class BaseDescriptor(
    val base: KClass<*>,
) : SerialDescriptor by JsonObject.serializer().descriptor {
    override val serialName: String = "base:${base.simpleName}"
}
