// Общий класс генератора (generator/src/main/resources/shared/). Не править в выводе.

package io.github.nakulov.designsystem

import io.github.nakulov.designsystem.serialization.RgbaSerializer
import kotlin.jvm.JvmInline
import kotlinx.serialization.Serializable

/** Цвет sRGB с прозрачностью в порядке `0xRRGGBBAA` (`color`, раздел 4.1); запись — `#RRGGBB` или `#RRGGBBAA`. */
@Serializable(with = RgbaSerializer::class)
@JvmInline
value class Rgba(
    val value: UInt,
)
