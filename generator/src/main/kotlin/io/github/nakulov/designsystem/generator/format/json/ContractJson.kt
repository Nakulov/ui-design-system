package io.github.nakulov.designsystem.generator.format.json

import io.github.nakulov.designsystem.generator.format.VersionedStatus
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule

/**
 * Разбор контрактов. Строгий: неизвестное поле — ошибка (раздел 3, «Другие поля в контракте не используются»).
 * Нестандартные сериализаторы формата регистрируются здесь, а не аннотациями полей.
 */
val ContractJson: Json = Json {
    ignoreUnknownKeys = false
    serializersModule = SerializersModule {
        contextual(VersionedStatus::class, VersionedStatusSerializer)
    }
}
