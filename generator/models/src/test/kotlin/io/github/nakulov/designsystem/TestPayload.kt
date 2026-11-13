package io.github.nakulov.designsystem

import kotlinx.serialization.json.JsonObject

/** `payload` образцов в тестах: объект JSON как есть (формат данных задаёт приложение, раздел 4.1). */
data class TestPayload(
    val data: JsonObject,
) : Payload
