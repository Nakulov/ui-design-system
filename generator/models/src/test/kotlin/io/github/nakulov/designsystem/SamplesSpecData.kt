package io.github.nakulov.designsystem

import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass

/** `Json` разбора образцов: модуль библиотеки и тестовый `payload` (`TestPayload`). */
fun makeSamplesJson(): Json {
    return Json {
        serializersModule = SerializersModule {
            include(DesignSystemModule)
            polymorphic(Payload::class) {
                subclass(TestPayload::class, TestPayloadSerializer)
                defaultDeserializer { TestPayloadSerializer }
            }
        }
    }
}
