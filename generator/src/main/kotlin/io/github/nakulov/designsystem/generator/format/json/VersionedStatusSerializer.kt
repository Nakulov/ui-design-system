package io.github.nakulov.designsystem.generator.format.json

import io.github.nakulov.designsystem.generator.format.VersionedStatus
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.JsonTransformingSerializer
import kotlinx.serialization.serializer

/** Статус строкой или объектом `{ "status", "version" }`. */
object VersionedStatusSerializer : JsonTransformingSerializer<VersionedStatus>(serializer<VersionedStatus>()) {
    override fun transformDeserialize(element: JsonElement): JsonElement =
        if (element is JsonPrimitive) JsonObject(mapOf("status" to element)) else element
}
