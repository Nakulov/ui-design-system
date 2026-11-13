@file:UseSerializers(AppearanceSerializer::class, EventSerializer::class, PaddingSerializer::class)

package io.github.nakulov.designsystem

import io.github.nakulov.designsystem.core.appearance.Appearance
import io.github.nakulov.designsystem.core.appearance.AppearanceSerializer
import io.github.nakulov.designsystem.core.component.Component
import io.github.nakulov.designsystem.core.component.ComponentIndicator
import io.github.nakulov.designsystem.core.event.Event
import io.github.nakulov.designsystem.core.event.EventSerializer
import io.github.nakulov.designsystem.core.layout.Layout
import io.github.nakulov.designsystem.core.padding.Padding
import io.github.nakulov.designsystem.core.padding.PaddingSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers

/** Компонент продукта в тестах: своя реализация `Component`, регистрируется рядом с `DesignSystemModule`. */
@Serializable
@SerialName("OrderStatus")
data class OrderStatus(
    override val id: String? = null,
    override val layout: Layout,
    override val padding: Padding,
    override val appearance: Appearance,
    override val indicators: List<ComponentIndicator>? = null,
    override val events: Set<Event>? = null,
    val status: String,
) : Component
