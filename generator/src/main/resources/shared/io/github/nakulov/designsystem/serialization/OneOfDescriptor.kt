// Общий класс генератора (generator/src/main/resources/shared/). Не править в выводе.

package io.github.nakulov.designsystem.serialization

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SealedSerializationApi
import kotlinx.serialization.descriptors.SerialDescriptor

/** Описание записи `oneOf`: варианты [variants] — имя → описание класса варианта из `sealed`-описания [sealed]. */
@OptIn(ExperimentalSerializationApi::class, SealedSerializationApi::class)
class OneOfDescriptor(
    val sealed: SerialDescriptor,
) : SerialDescriptor by sealed {
    override val serialName: String = "oneOf:${sealed.serialName}"

    /** Варианты в порядке `sealed`-описания. */
    val variants: Map<String, SerialDescriptor> = sealed.getElementDescriptor(index = 1).let { subclasses ->
        (0 until subclasses.elementsCount).associate { index ->
            subclasses.getElementName(index) to subclasses.getElementDescriptor(index)
        }
    }
}

/** Вариант с одним значением: помечен [ValueVariant]. */
fun SerialDescriptor.isValueVariant(): Boolean {
    return annotations.any { annotation -> annotation is ValueVariant }
}
