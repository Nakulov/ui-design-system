package io.github.nakulov.designsystem.generator.format.json

import io.github.nakulov.designsystem.generator.format.VersionedStatus
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.JsonTransformingSerializer

/** Статус строкой или объектом `{ "status", "version" }`. */
object VersionedStatusSerializer : JsonTransformingSerializer<VersionedStatus>(VersionedStatus.serializer()) {
    override fun transformDeserialize(element: JsonElement): JsonElement =
        if (element is JsonPrimitive) JsonObject(mapOf("status" to element)) else element
}
