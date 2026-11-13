package io.github.nakulov.designsystem.generator.resolve

import io.github.nakulov.designsystem.generator.format.ReleaseStatus
import io.github.nakulov.designsystem.generator.format.ReleaseVersions
import io.github.nakulov.designsystem.generator.format.VersionedStatus
import io.github.nakulov.designsystem.generator.model.BaseContract
import io.github.nakulov.designsystem.generator.model.ContractId
import io.github.nakulov.designsystem.generator.model.ContractKind
import io.github.nakulov.designsystem.generator.model.ContractModel
import io.github.nakulov.designsystem.generator.model.DefaultValue
import io.github.nakulov.designsystem.generator.model.EnumContract
import io.github.nakulov.designsystem.generator.model.EnumValue
import io.github.nakulov.designsystem.generator.model.ObjectContract
import io.github.nakulov.designsystem.generator.model.OneOfContract
import io.github.nakulov.designsystem.generator.model.PrimitiveType
import io.github.nakulov.designsystem.generator.model.Property
import io.github.nakulov.designsystem.generator.model.PropertyType
import io.github.nakulov.designsystem.generator.model.TypeRef
import io.github.nakulov.designsystem.generator.model.Variant
import io.github.nakulov.designsystem.generator.model.VariantData
import kotlinx.serialization.json.JsonPrimitive
import java.nio.file.Path
import kotlin.io.path.toPath

const val COMPONENT_DESCRIPTION = "Базовая модель компонента. Каждый компонент содержит поля базовой модели"
const val COMPONENT_ID_DESCRIPTION =
    "Идентификатор компонента: UI-тесты, источник событий, ключ элемента в списках, аналитика. " +
        "Если задан, уникален в пределах дерева модели"
const val BADGE_TEXT_DESCRIPTION = "Текст бейджа в одну строку, без переноса и многоточия: бейдж растёт по тексту"
const val BADGE_TEXT_VALUE_DESCRIPTION =
    "Текст: например, «9» или «99+». Сокращение и форматирование чисел выполняет сервер или приложение"

/** Каталог контрактов случая из ресурсов `contracts/<случай>/`. */
fun makeContractsRoot(case: String): Path {
    return ContractResolverSpec::class.java.getResource(/* name = */ "/contracts/$case")!!.toURI().toPath()
}

/** Статусы главных контрактов: в Figma выпущен, на платформах нет. */
fun makeFigmaOnlyReleaseVersions(): ReleaseVersions {
    return ReleaseVersions(
        android = VersionedStatus(ReleaseStatus.NOT_RELEASED),
        ios = VersionedStatus(ReleaseStatus.NOT_RELEASED),
        figma = ReleaseStatus.RELEASED,
    )
}

/** `BadgeText` с одним свойством `value`. */
fun makeBadgeText(): ObjectContract {
    return ObjectContract(
        id = ContractId(value = "components/badge/BadgeText"),
        name = "BadgeText",
        description = BADGE_TEXT_DESCRIPTION,
        kind = ContractKind.HELPER,
        releaseVersions = null,
        base = null,
        appearanceContext = emptyList(),
        properties = listOf(
            Property(
                name = "value",
                description = BADGE_TEXT_VALUE_DESCRIPTION,
                type = PropertyType.Single(TypeRef.Primitive(PrimitiveType.STRING)),
                required = true,
                default = null,
                sample = JsonPrimitive(value = "99+"),
                releaseVersions = null,
                declaredIn = ContractId(value = "components/badge/BadgeText"),
            ),
        ),
    )
}

/** Свойство `id` базовой модели. */
fun makeComponentIdProperty(): Property {
    return Property(
        name = "id",
        description = COMPONENT_ID_DESCRIPTION,
        type = PropertyType.Single(TypeRef.Primitive(PrimitiveType.STRING)),
        required = false,
        default = null,
        sample = null,
        releaseVersions = null,
        declaredIn = ContractId(value = "core/component/Component"),
    )
}

/** Свойство `indicators` базовой модели: элементы — `ComponentIndicator` из сущности `Component`. */
fun makeComponentIndicatorsProperty(): Property {
    return Property(
        name = "indicators",
        description = "Индикаторы у углов формы компонента: точка, символ, счётчик. Компонент вырезает их из своей " +
            "формы с зазором; размер компонента в раскладке не меняется",
        type = PropertyType.Array(
            TypeRef.Contract(ContractId(value = "core/component/ComponentIndicator")),
            uniqueVariants = false,
        ),
        required = false,
        default = null,
        sample = null,
        releaseVersions = null,
        declaredIn = ContractId(value = "core/component/Component"),
    )
}

/** Наследование: свойства `Component` перед собственными свойствами `Badge`. */
fun makeInheritanceModel(): ContractModel {
    return ContractModel(
        mapOf(
            ContractId(value = "core/component/Component") to makeComponent(
                releaseVersions = makeFigmaOnlyReleaseVersions(),
                properties = listOf(makeComponentIdProperty(), makeComponentIndicatorsProperty()),
            ),
            ContractId(value = "core/component/ComponentIndicator") to EnumContract(
                id = ContractId(value = "core/component/ComponentIndicator"),
                name = "ComponentIndicator",
                description = "Индикатор у угла носителя: рисуется поверх, может выступать за границы и не меняет " +
                    "размер носителя в раскладке",
                kind = ContractKind.HELPER,
                releaseVersions = null,
                values = listOf(EnumValue(name = "dot", description = "Точка", releaseVersions = null)),
                sample = null,
            ),
            ContractId(value = "components/badge/Badge") to makeBadge(),
            ContractId(value = "components/badge/BadgeText") to makeBadgeText(),
        ),
    )
}

/** `Component` с заданными статусами и свойствами. */
private fun makeComponent(releaseVersions: ReleaseVersions?, properties: List<Property>): BaseContract {
    return BaseContract(
        id = ContractId(value = "core/component/Component"),
        name = "Component",
        description = COMPONENT_DESCRIPTION,
        kind = ContractKind.PUBLIC,
        releaseVersions = releaseVersions,
        properties = properties,
    )
}

/** `Badge`: базовые свойства `Component`, затем собственное `content`. */
private fun makeBadge(): ObjectContract {
    return ObjectContract(
        id = ContractId(value = "components/badge/Badge"),
        name = "Badge",
        description = "Метка внимания: форма с текстом, символом или картинкой либо без содержимого. " +
            "Ширина не меньше высоты: один знак — круг, длинный текст — таблетка",
        kind = ContractKind.PUBLIC,
        releaseVersions = makeFigmaOnlyReleaseVersions(),
        base = ContractId(value = "core/component/Component"),
        appearanceContext = listOf(element = "plain"),
        properties = listOf(
            makeComponentIdProperty(),
            makeComponentIndicatorsProperty(),
            Property(
                name = "content",
                description = "Содержимое по центру. Если не задано, бейдж — только фон (точка)",
                type = PropertyType.Single(TypeRef.Contract(ContractId(value = "components/badge/BadgeText"))),
                required = false,
                default = null,
                sample = null,
                releaseVersions = null,
                declaredIn = ContractId(value = "components/badge/Badge"),
            ),
        ),
    )
}

/** `inline`: значения `BorderWidthScale` — варианты `BorderWidth` без данных, `custom` — свойства объекта. */
fun makeInlineModel(): ContractModel {
    return ContractModel(
        mapOf(
            ContractId(value = "foundation/borderWidth/BorderWidth") to makeBorderWidth(),
            ContractId(value = "foundation/borderWidth/BorderWidthScale") to EnumContract(
                id = ContractId(value = "foundation/borderWidth/BorderWidthScale"),
                name = "BorderWidthScale",
                description = "Именованные значения шкалы толщины обводки",
                kind = ContractKind.HELPER,
                releaseVersions = null,
                values = listOf(
                    EnumValue(name = "zero", description = "Без обводки, 0 условных единиц", releaseVersions = null),
                    EnumValue(
                        name = "s",
                        description = "Обычная обводка и разделители, по умолчанию 1 условная единица",
                        releaseVersions = null,
                    ),
                    EnumValue(
                        name = "m",
                        description = "Выделение: фокус, выбранный элемент, по умолчанию 2 условные единицы",
                        releaseVersions = null,
                    ),
                    EnumValue(
                        name = "l",
                        description = "Акцентная обводка и индикаторы, по умолчанию 4 условные единицы",
                        releaseVersions = null,
                    ),
                ),
                sample = null,
            ),
            ContractId(value = "foundation/borderWidth/BorderWidthCustom") to makeBorderWidthCustom(),
        ),
    )
}

/** `BorderWidth`: значения шкалы вариантами без данных и `custom` со свойствами объекта. */
private fun makeBorderWidth(): OneOfContract {
    return OneOfContract(
        id = ContractId(value = "foundation/borderWidth/BorderWidth"),
        name = "BorderWidth",
        description = "Толщина обводки или разделителя. Значения задаются в условных единицах: dp на Android, " +
            "pt на iOS, px в Figma",
        kind = ContractKind.PUBLIC,
        releaseVersions = makeFigmaOnlyReleaseVersions(),
        variants = listOf(
            makeScaleVariant(name = "zero", description = "Без обводки, 0 условных единиц"),
            makeScaleVariant(
                name = "s",
                description = "Обычная обводка и разделители, по умолчанию 1 условная единица",
            ),
            makeScaleVariant(
                name = "m",
                description = "Выделение: фокус, выбранный элемент, по умолчанию 2 условные единицы",
            ),
            makeScaleVariant(
                name = "l",
                description = "Акцентная обводка и индикаторы, по умолчанию 4 условные единицы",
            ),
            Variant(
                name = "custom",
                description = null,
                data = VariantData.Fields(ContractId(value = "foundation/borderWidth/BorderWidthCustom")),
                sample = null,
                releaseVersions = null,
            ),
        ),
        sample = JsonPrimitive(value = "s"),
    )
}

/** Вариант из значения шкалы, развёрнутого `inline`: без данных. */
private fun makeScaleVariant(name: String, description: String): Variant {
    return Variant(
        name = name,
        description = description,
        data = VariantData.Unfolded(ContractId(value = "foundation/borderWidth/BorderWidthScale")),
        sample = null,
        releaseVersions = null,
    )
}

/** `BorderWidthCustom` с одним свойством `value`. */
private fun makeBorderWidthCustom(): ObjectContract {
    return ObjectContract(
        id = ContractId(value = "foundation/borderWidth/BorderWidthCustom"),
        name = "BorderWidthCustom",
        description = "Произвольная толщина обводки вне шкалы",
        kind = ContractKind.HELPER,
        releaseVersions = null,
        base = null,
        appearanceContext = emptyList(),
        properties = listOf(
            Property(
                name = "value",
                description = "Толщина обводки в условных единицах",
                type = PropertyType.Single(TypeRef.Primitive(PrimitiveType.INTEGER)),
                required = true,
                default = null,
                sample = null,
                releaseVersions = null,
                declaredIn = ContractId(value = "foundation/borderWidth/BorderWidthCustom"),
            ),
        ),
    )
}

/** Именованные варианты: ссылка на объект — свойства, на `oneOf` — одно значение, `type` — одно значение. */
fun makeNamedVariantModel(): ContractModel {
    return ContractModel(
        mapOf(
            ContractId(value = "components/badge/BadgeContent") to OneOfContract(
                id = ContractId(value = "components/badge/BadgeContent"),
                name = "BadgeContent",
                description = "Содержимое бейджа",
                kind = ContractKind.HELPER,
                releaseVersions = null,
                variants = listOf(
                    Variant(
                        name = "text",
                        description = null,
                        data = VariantData.Fields(ContractId(value = "components/badge/BadgeText")),
                        sample = null,
                        releaseVersions = null,
                    ),
                    Variant(
                        name = "image",
                        description = null,
                        data = VariantData.Value(TypeRef.Contract(ContractId(value = "components/image/ImageSource"))),
                        sample = null,
                        releaseVersions = null,
                    ),
                ),
                sample = null,
            ),
            ContractId(value = "components/badge/BadgeText") to makeBadgeText(),
            ContractId(value = "components/image/ImageSource") to makeImageSource(),
        ),
    )
}

/** `ImageSource` с одним вариантом `remote`: строка. */
private fun makeImageSource(): OneOfContract {
    return OneOfContract(
        id = ContractId(value = "components/image/ImageSource"),
        name = "ImageSource",
        description = "Источник изображения: ресурс показывается сразу, удалённая картинка загружается",
        kind = ContractKind.HELPER,
        releaseVersions = null,
        variants = listOf(
            Variant(
                name = "remote",
                description = null,
                data = VariantData.Value(TypeRef.Primitive(PrimitiveType.STRING)),
                sample = null,
                releaseVersions = null,
            ),
        ),
        sample = null,
    )
}

/** Перечисление: значения по порядку. */
fun makeEnumModel(): ContractModel {
    return ContractModel(
        mapOf(
            ContractId(value = "components/text/TextAlignment") to EnumContract(
                id = ContractId(value = "components/text/TextAlignment"),
                name = "TextAlignment",
                description = "Выравнивание строк текста по горизонтали",
                kind = ContractKind.HELPER,
                releaseVersions = null,
                values = listOf(
                    EnumValue(
                        name = "start",
                        description = "По начальному краю: слева при письме слева направо",
                        releaseVersions = null,
                    ),
                    EnumValue(name = "center", description = "По центру", releaseVersions = null),
                    EnumValue(
                        name = "end",
                        description = "По конечному краю: справа при письме слева направо",
                        releaseVersions = null,
                    ),
                ),
                sample = null,
            ),
        ),
    )
}

/** `defaultsTo`: `pressed` по умолчанию — значение `default`. */
fun makeDefaultsToModel(): ContractModel {
    return ContractModel(
        mapOf(
            ContractId(value = "core/component/Component") to makeComponent(
                releaseVersions = null,
                properties = listOf(makeComponentIdProperty()),
            ),
            ContractId(value = "components/pressable/PressableStates") to ObjectContract(
                id = ContractId(value = "components/pressable/PressableStates"),
                name = "PressableStates",
                description =
                "Деревья содержимого Pressable по состояниям. Деревья — один и тот же компонент, корень " +
                    "оформлен plain; узлы с одинаковым id в разных деревьях — один элемент",
                kind = ContractKind.HELPER,
                releaseVersions = null,
                base = null,
                appearanceContext = emptyList(),
                properties = listOf(
                    makePressableStatesProperty(name = "default", description = "Обычное состояние", default = null),
                    makePressableStatesProperty(
                        name = "pressed",
                        description = "Состояние во время нажатия. По умолчанию — дерево default целиком, " +
                            "с его событиями",
                        default = DefaultValue.Sibling(property = "default"),
                    ),
                ),
            ),
        ),
    )
}

/** Обязательное свойство-дерево `PressableStates`. */
private fun makePressableStatesProperty(name: String, description: String, default: DefaultValue?): Property {
    return Property(
        name = name,
        description = description,
        type = PropertyType.Single(TypeRef.Contract(ContractId(value = "core/component/Component"))),
        required = true,
        default = default,
        sample = null,
        releaseVersions = null,
        declaredIn = ContractId(value = "components/pressable/PressableStates"),
    )
}

/** `default`: значение из контракта записано как есть. */
fun makeDefaultModel(): ContractModel {
    return ContractModel(
        mapOf(
            ContractId(value = "core/layout/LayoutPinned") to ObjectContract(
                id = ContractId(value = "core/layout/LayoutPinned"),
                name = "LayoutPinned",
                description = "Закрепление компонента относительно родителя. Допустимо только у детей контейнера " +
                    "наложения. Смещения — позиция, а не отступ: размер и область нажатия компонента не меняются",
                kind = ContractKind.HELPER,
                releaseVersions = null,
                base = null,
                appearanceContext = emptyList(),
                properties = listOf(
                    Property(
                        name = "relativeTo",
                        description = "От чего отсчитываются смещения",
                        type = PropertyType.Single(
                            TypeRef.Contract(ContractId(value = "core/layout/LayoutPinReference")),
                        ),
                        required = true,
                        default = DefaultValue.Value(JsonPrimitive(value = "edge")),
                        sample = null,
                        releaseVersions = null,
                        declaredIn = ContractId(value = "core/layout/LayoutPinned"),
                    ),
                ),
            ),
            ContractId(value = "core/layout/LayoutPinReference") to EnumContract(
                id = ContractId(value = "core/layout/LayoutPinReference"),
                name = "LayoutPinReference",
                description = "От чего отсчитываются смещения закрепления",
                kind = ContractKind.HELPER,
                releaseVersions = null,
                values = listOf(EnumValue(name = "edge", description = "От края родителя", releaseVersions = null)),
                sample = null,
            ),
        ),
    )
}

/** Примитивные типы свойств (раздел 4.1). */
fun makePrimitivesModel(): ContractModel {
    return ContractModel(
        mapOf(
            ContractId(value = "layer/entity/Entity") to makeEntity(
                properties = listOf(
                    makeEntityProperty(name = "text", description = "Свойство text", type = PrimitiveType.STRING),
                    makeEntityProperty(name = "flag", description = "Свойство flag", type = PrimitiveType.BOOLEAN),
                    makeEntityProperty(name = "count", description = "Свойство count", type = PrimitiveType.INTEGER),
                    makeEntityProperty(name = "ratio", description = "Свойство ratio", type = PrimitiveType.FLOAT),
                    makeEntityProperty(name = "tint", description = "Свойство tint", type = PrimitiveType.COLOR),
                    makeEntityProperty(name = "data", description = "Свойство data", type = PrimitiveType.PAYLOAD),
                ),
            ),
        ),
    )
}

/** Пустые списки: объект без свойств, перечисление без значений, `oneOf` без элементов и с развёрнутым пустым enum. */
fun makeEmptyModel(): ContractModel {
    return ContractModel(
        mapOf(
            ContractId(value = "layer/entity/Entity") to makeEntity(properties = emptyList()),
            ContractId(value = "layer/entity/EntityEnum") to EnumContract(
                id = ContractId(value = "layer/entity/EntityEnum"),
                name = "EntityEnum",
                description = "Перечисление",
                kind = ContractKind.HELPER,
                releaseVersions = null,
                values = emptyList(),
                sample = null,
            ),
            ContractId(value = "layer/entity/EntityInline") to makeEmptyOneOf(
                id = ContractId(value = "layer/entity/EntityInline"),
                name = "EntityInline",
            ),
            ContractId(value = "layer/entity/EntityNone") to makeEmptyOneOf(
                id = ContractId(value = "layer/entity/EntityNone"),
                name = "EntityNone",
            ),
        ),
    )
}

/** Главный объект `Entity` случая с заданными свойствами. */
private fun makeEntity(properties: List<Property>): ObjectContract {
    return ObjectContract(
        id = ContractId(value = "layer/entity/Entity"),
        name = "Entity",
        description = "Объект",
        kind = ContractKind.PUBLIC,
        releaseVersions = null,
        base = null,
        appearanceContext = emptyList(),
        properties = properties,
    )
}

/** Обязательное свойство `Entity` примитивного типа. */
private fun makeEntityProperty(name: String, description: String, type: PrimitiveType): Property {
    return Property(
        name = name,
        description = description,
        type = PropertyType.Single(TypeRef.Primitive(type)),
        required = true,
        default = null,
        sample = null,
        releaseVersions = null,
        declaredIn = ContractId(value = "layer/entity/Entity"),
    )
}

/** Вспомогательный `oneOf` без вариантов. */
private fun makeEmptyOneOf(id: ContractId, name: String): OneOfContract {
    return OneOfContract(
        id = id,
        name = name,
        description = "Варианты",
        kind = ContractKind.HELPER,
        releaseVersions = null,
        variants = emptyList(),
        sample = null,
    )
}
