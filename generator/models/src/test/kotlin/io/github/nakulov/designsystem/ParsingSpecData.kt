package io.github.nakulov.designsystem

import io.github.nakulov.designsystem.components.container.Container
import io.github.nakulov.designsystem.components.container.ContainerArrangement
import io.github.nakulov.designsystem.components.container.ContainerHorizontalDistribution
import io.github.nakulov.designsystem.components.container.ContainerRowAlignment
import io.github.nakulov.designsystem.core.appearance.Appearance
import io.github.nakulov.designsystem.core.component.Component
import io.github.nakulov.designsystem.core.layout.Layout
import io.github.nakulov.designsystem.core.layout.LayoutDimension
import io.github.nakulov.designsystem.core.layout.LayoutPosition
import io.github.nakulov.designsystem.core.layout.LayoutSizing
import io.github.nakulov.designsystem.core.padding.Padding
import io.github.nakulov.designsystem.foundation.spacing.Spacing
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass

/** Текст ресурса `parsing/<case>.json`: экран `Container` с ошибкой в третьем ребёнке. */
fun makeParsingResource(case: String): String {
    return ParsingSpec::class.java.getResource(/* name = */ "/parsing/$case.json")!!.readText()
}

/** `Json` продукта: модуль библиотеки и свой компонент [OrderStatus]. */
fun makeProductJson(): Json {
    return Json {
        serializersModule = SerializersModule {
            include(makeSamplesJson().serializersModule)
            polymorphic(Component::class) {
                subclass(OrderStatus::class)
            }
        }
    }
}

/** `Container` из `parsing/productComponent.json` с компонентом продукта внутри. */
fun makeProductScreen(): Component {
    return Container(
        layout = makeHugLayout(),
        padding = Padding.All(Spacing.M),
        appearance = Appearance.Plain(),
        arrangement = ContainerArrangement.Horizontal(
            alignment = ContainerRowAlignment.Center,
            distribution = ContainerHorizontalDistribution.SpaceBetween,
        ),
        children = listOf(
            OrderStatus(
                layout = makeHugLayout(),
                padding = Padding.All(Spacing.M),
                appearance = Appearance.Plain(),
                status = "Доставлен",
            ),
        ),
    )
}

private fun makeHugLayout(): Layout {
    return Layout(
        width = LayoutDimension(sizing = LayoutSizing.Hug),
        height = LayoutDimension(sizing = LayoutSizing.Hug),
        position = LayoutPosition.Flow,
    )
}
