package io.github.nakulov.designsystem.generator.resolve

import io.github.nakulov.designsystem.generator.format.ReleaseStatus
import io.github.nakulov.designsystem.generator.format.ReleaseVersions
import io.github.nakulov.designsystem.generator.format.VersionedStatus
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
import java.nio.file.Path
import kotlin.io.path.toPath
import kotlinx.serialization.json.JsonPrimitive

const val COMPONENT_DESCRIPTION = "Базовая модель компонента. Каждый компонент содержит поля базовой модели"
const val COMPONENT_ID_DESCRIPTION =
    "Идентификатор компонента: UI-тесты, источник событий, ключ элемента в списках, аналитика. " +
        "Если задан, уникален в пределах дерева модели"
const val BADGE_TEXT_DESCRIPTION = "Текст бейджа в одну строку, без переноса и многоточия: бейдж растёт по тексту"
const val BADGE_TEXT_VALUE_DESCRIPTION =
    "Текст: например, «9» или «99+». Сокращение и форматирование чисел выполняет сервер или приложение"

/** Каталог контрактов случая из ресурсов `contracts/<случай>/`. */
fun makeContractsRoot(case: String): Path =
    ContractResolverSpec::class.java.getResource("/contracts/$case")!!.toURI().toPath()

/** Статусы главных контрактов: в Figma выпущен, на платформах нет. */
fun makeFigmaOnlyReleaseVersions(): ReleaseVersions = ReleaseVersions(
    android = VersionedStatus(ReleaseStatus.NOT_RELEASED),
    ios = VersionedStatus(ReleaseStatus.NOT_RELEASED),
    figma = ReleaseStatus.RELEASED,
)

/** `BadgeText` с одним свойством `value`. */
fun makeBadgeText(): ObjectContract = ObjectContract(
    id = ContractId("components/badge/BadgeText"),
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
            sample = JsonPrimitive("99+"),
            releaseVersions = null,
            declaredIn = ContractId("components/badge/BadgeText"),
        ),
    ),
)

/** Наследование: свойства `Component` перед собственными свойствами `Badge`, `ComponentIndicator` — из сущности `Component`. */
/** Свойство `id` базовой модели. */
fun makeComponentIdProperty(): Property = Property(
    name = "id",
    description = COMPONENT_ID_DESCRIPTION,
    type = PropertyType.Single(TypeRef.Primitive(PrimitiveType.STRING)),
    required = false,
    default = null,
    sample = null,
    releaseVersions = null,
    declaredIn = ContractId("core/component/Component"),
)

/** Свойство `indicators` базовой модели: элементы — `ComponentIndicator` из сущности `Component`. */
fun makeComponentIndicatorsProperty(): Property = Property(
    name = "indicators",
    description = "Индикаторы у углов формы компонента: точка, символ, счётчик. Компонент вырезает их из своей " +
        "формы с зазором; размер компонента в раскладке не меняется",
    type = PropertyType.Array(TypeRef.Contract(ContractId("core/component/ComponentIndicator")), false),
    required = false,
    default = null,
    sample = null,
    releaseVersions = null,
    declaredIn = ContractId("core/component/Component"),
)

/** Наследование: свойства `Component` перед собственными свойствами `Badge`. */
fun makeInheritanceModel(): ContractModel = ContractModel(
    mapOf(
        ContractId("core/component/Component") to ObjectContract(
            id = ContractId("core/component/Component"),
            name = "Component",
            description = COMPONENT_DESCRIPTION,
            kind = ContractKind.PUBLIC,
            releaseVersions = makeFigmaOnlyReleaseVersions(),
            base = null,
            appearanceContext = emptyList(),
            properties = listOf(makeComponentIdProperty(), makeComponentIndicatorsProperty()),
        ),
        ContractId("core/component/ComponentIndicator") to EnumContract(
            id = ContractId("core/component/ComponentIndicator"),
            name = "ComponentIndicator",
            description = "Индикатор у угла носителя: рисуется поверх, может выступать за границы и не меняет " +
                "размер носителя в раскладке",
            kind = ContractKind.HELPER,
            releaseVersions = null,
            values = listOf(EnumValue("dot", "Точка", null)),
            sample = null,
        ),
        ContractId("components/badge/Badge") to ObjectContract(
            id = ContractId("components/badge/Badge"),
            name = "Badge",
            description = "Метка внимания: форма с текстом, символом или картинкой либо без содержимого. " +
                "Ширина не меньше высоты: один знак — круг, длинный текст — таблетка",
            kind = ContractKind.PUBLIC,
            releaseVersions = makeFigmaOnlyReleaseVersions(),
            base = ContractId("core/component/Component"),
            appearanceContext = listOf("plain"),
            properties = listOf(
                makeComponentIdProperty(),
                makeComponentIndicatorsProperty(),
                Property(
                    name = "content",
                    description = "Содержимое по центру. Если не задано, бейдж — только фон (точка)",
                    type = PropertyType.Single(TypeRef.Contract(ContractId("components/badge/BadgeText"))),
                    required = false,
                    default = null,
                    sample = null,
                    releaseVersions = null,
                    declaredIn = ContractId("components/badge/Badge"),
                ),
            ),
        ),
        ContractId("components/badge/BadgeText") to makeBadgeText(),
    ),
)

/** `inline`: значения `BorderWidthScale` — варианты `BorderWidth` без данных, `custom` — свойства объекта. */
fun makeInlineModel(): ContractModel = ContractModel(
    mapOf(
        ContractId("foundation/borderWidth/BorderWidth") to OneOfContract(
            id = ContractId("foundation/borderWidth/BorderWidth"),
            name = "BorderWidth",
            description = "Толщина обводки или разделителя. Значения задаются в условных единицах: dp на Android, " +
                "pt на iOS, px в Figma",
            kind = ContractKind.PUBLIC,
            releaseVersions = makeFigmaOnlyReleaseVersions(),
            variants = listOf(
                Variant("zero", "Без обводки, 0 условных единиц", VariantData.None, null, null),
                Variant(
                    "s",
                    "Обычная обводка и разделители, по умолчанию 1 условная единица",
                    VariantData.None,
                    null,
                    null,
                ),
                Variant(
                    "m",
                    "Выделение: фокус, выбранный элемент, по умолчанию 2 условные единицы",
                    VariantData.None,
                    null,
                    null,
                ),
                Variant(
                    "l",
                    "Акцентная обводка и индикаторы, по умолчанию 4 условные единицы",
                    VariantData.None,
                    null,
                    null,
                ),
                Variant(
                    "custom",
                    null,
                    VariantData.Fields(ContractId("foundation/borderWidth/BorderWidthCustom")),
                    null,
                    null,
                ),
            ),
            sample = JsonPrimitive("s"),
        ),
        ContractId("foundation/borderWidth/BorderWidthScale") to EnumContract(
            id = ContractId("foundation/borderWidth/BorderWidthScale"),
            name = "BorderWidthScale",
            description = "Именованные значения шкалы толщины обводки",
            kind = ContractKind.HELPER,
            releaseVersions = null,
            values = listOf(
                EnumValue("zero", "Без обводки, 0 условных единиц", null),
                EnumValue("s", "Обычная обводка и разделители, по умолчанию 1 условная единица", null),
                EnumValue("m", "Выделение: фокус, выбранный элемент, по умолчанию 2 условные единицы", null),
                EnumValue("l", "Акцентная обводка и индикаторы, по умолчанию 4 условные единицы", null),
            ),
            sample = null,
        ),
        ContractId("foundation/borderWidth/BorderWidthCustom") to ObjectContract(
            id = ContractId("foundation/borderWidth/BorderWidthCustom"),
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
                    declaredIn = ContractId("foundation/borderWidth/BorderWidthCustom"),
                ),
            ),
        ),
    ),
)

/** Именованные варианты: ссылка на объект — свойства, на `oneOf` — одно значение, `type` — одно значение. */
fun makeNamedVariantModel(): ContractModel = ContractModel(
    mapOf(
        ContractId("components/badge/BadgeContent") to OneOfContract(
            id = ContractId("components/badge/BadgeContent"),
            name = "BadgeContent",
            description = "Содержимое бейджа",
            kind = ContractKind.HELPER,
            releaseVersions = null,
            variants = listOf(
                Variant("text", null, VariantData.Fields(ContractId("components/badge/BadgeText")), null, null),
                Variant(
                    "image",
                    null,
                    VariantData.Value(TypeRef.Contract(ContractId("components/image/ImageSource"))),
                    null,
                    null,
                ),
            ),
            sample = null,
        ),
        ContractId("components/badge/BadgeText") to makeBadgeText(),
        ContractId("components/image/ImageSource") to OneOfContract(
            id = ContractId("components/image/ImageSource"),
            name = "ImageSource",
            description = "Источник изображения: ресурс показывается сразу, удалённая картинка загружается",
            kind = ContractKind.HELPER,
            releaseVersions = null,
            variants = listOf(
                Variant("remote", null, VariantData.Value(TypeRef.Primitive(PrimitiveType.STRING)), null, null),
            ),
            sample = null,
        ),
    ),
)

/** Перечисление: значения по порядку. */
fun makeEnumModel(): ContractModel = ContractModel(
    mapOf(
        ContractId("components/text/TextAlignment") to EnumContract(
            id = ContractId("components/text/TextAlignment"),
            name = "TextAlignment",
            description = "Выравнивание строк текста по горизонтали",
            kind = ContractKind.HELPER,
            releaseVersions = null,
            values = listOf(
                EnumValue("start", "По начальному краю: слева при письме слева направо", null),
                EnumValue("center", "По центру", null),
                EnumValue("end", "По конечному краю: справа при письме слева направо", null),
            ),
            sample = null,
        ),
    ),
)

/** `defaultsTo`: `pressed` по умолчанию — значение `default`. */
fun makeDefaultsToModel(): ContractModel = ContractModel(
    mapOf(
        ContractId("core/component/Component") to ObjectContract(
            id = ContractId("core/component/Component"),
            name = "Component",
            description = COMPONENT_DESCRIPTION,
            kind = ContractKind.PUBLIC,
            releaseVersions = null,
            base = null,
            appearanceContext = emptyList(),
            properties = listOf(
                Property(
                    name = "id",
                    description = COMPONENT_ID_DESCRIPTION,
                    type = PropertyType.Single(TypeRef.Primitive(PrimitiveType.STRING)),
                    required = false,
                    default = null,
                    sample = null,
                    releaseVersions = null,
                    declaredIn = ContractId("core/component/Component"),
                ),
            ),
        ),
        ContractId("components/pressable/PressableStates") to ObjectContract(
            id = ContractId("components/pressable/PressableStates"),
            name = "PressableStates",
            description = "Деревья содержимого Pressable по состояниям. Деревья — один и тот же компонент, корень " +
                "оформлен plain; узлы с одинаковым id в разных деревьях — один элемент",
            kind = ContractKind.HELPER,
            releaseVersions = null,
            base = null,
            appearanceContext = emptyList(),
            properties = listOf(
                Property(
                    name = "default",
                    description = "Обычное состояние",
                    type = PropertyType.Single(TypeRef.Contract(ContractId("core/component/Component"))),
                    required = true,
                    default = null,
                    sample = null,
                    releaseVersions = null,
                    declaredIn = ContractId("components/pressable/PressableStates"),
                ),
                Property(
                    name = "pressed",
                    description = "Состояние во время нажатия. По умолчанию — дерево default целиком, с его событиями",
                    type = PropertyType.Single(TypeRef.Contract(ContractId("core/component/Component"))),
                    required = true,
                    default = DefaultValue.Sibling("default"),
                    sample = null,
                    releaseVersions = null,
                    declaredIn = ContractId("components/pressable/PressableStates"),
                ),
            ),
        ),
    ),
)

/** `default`: значение из контракта записано как есть. */
fun makeDefaultModel(): ContractModel = ContractModel(
    mapOf(
        ContractId("core/layout/LayoutPinned") to ObjectContract(
            id = ContractId("core/layout/LayoutPinned"),
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
                    type = PropertyType.Single(TypeRef.Contract(ContractId("core/layout/LayoutPinReference"))),
                    required = true,
                    default = DefaultValue.Value(JsonPrimitive("edge")),
                    sample = null,
                    releaseVersions = null,
                    declaredIn = ContractId("core/layout/LayoutPinned"),
                ),
            ),
        ),
        ContractId("core/layout/LayoutPinReference") to EnumContract(
            id = ContractId("core/layout/LayoutPinReference"),
            name = "LayoutPinReference",
            description = "От чего отсчитываются смещения закрепления",
            kind = ContractKind.HELPER,
            releaseVersions = null,
            values = listOf(EnumValue("edge", "От края родителя", null)),
            sample = null,
        ),
    ),
)

/** Примитивные типы свойств (раздел 4.1). */
fun makePrimitivesModel(): ContractModel = ContractModel(
    mapOf(
        ContractId("layer/entity/Entity") to ObjectContract(
            id = ContractId("layer/entity/Entity"),
            name = "Entity",
            description = "Объект",
            kind = ContractKind.PUBLIC,
            releaseVersions = null,
            base = null,
            appearanceContext = emptyList(),
            properties = listOf(
                Property(
                    name = "text",
                    description = "Свойство text",
                    type = PropertyType.Single(TypeRef.Primitive(PrimitiveType.STRING)),
                    required = true,
                    default = null,
                    sample = null,
                    releaseVersions = null,
                    declaredIn = ContractId("layer/entity/Entity"),
                ),
                Property(
                    name = "flag",
                    description = "Свойство flag",
                    type = PropertyType.Single(TypeRef.Primitive(PrimitiveType.BOOLEAN)),
                    required = true,
                    default = null,
                    sample = null,
                    releaseVersions = null,
                    declaredIn = ContractId("layer/entity/Entity"),
                ),
                Property(
                    name = "count",
                    description = "Свойство count",
                    type = PropertyType.Single(TypeRef.Primitive(PrimitiveType.INTEGER)),
                    required = true,
                    default = null,
                    sample = null,
                    releaseVersions = null,
                    declaredIn = ContractId("layer/entity/Entity"),
                ),
                Property(
                    name = "ratio",
                    description = "Свойство ratio",
                    type = PropertyType.Single(TypeRef.Primitive(PrimitiveType.FLOAT)),
                    required = true,
                    default = null,
                    sample = null,
                    releaseVersions = null,
                    declaredIn = ContractId("layer/entity/Entity"),
                ),
                Property(
                    name = "tint",
                    description = "Свойство tint",
                    type = PropertyType.Single(TypeRef.Primitive(PrimitiveType.COLOR)),
                    required = true,
                    default = null,
                    sample = null,
                    releaseVersions = null,
                    declaredIn = ContractId("layer/entity/Entity"),
                ),
                Property(
                    name = "data",
                    description = "Свойство data",
                    type = PropertyType.Single(TypeRef.Primitive(PrimitiveType.PAYLOAD)),
                    required = true,
                    default = null,
                    sample = null,
                    releaseVersions = null,
                    declaredIn = ContractId("layer/entity/Entity"),
                ),
            ),
        ),
    ),
)

/** Пустые списки: объект без свойств, перечисление без значений, `oneOf` без элементов и с развёрнутым пустым enum. */
fun makeEmptyModel(): ContractModel = ContractModel(
    mapOf(
        ContractId("layer/entity/Entity") to ObjectContract(
            id = ContractId("layer/entity/Entity"),
            name = "Entity",
            description = "Объект",
            kind = ContractKind.PUBLIC,
            releaseVersions = null,
            base = null,
            appearanceContext = emptyList(),
            properties = emptyList(),
        ),
        ContractId("layer/entity/EntityEnum") to EnumContract(
            id = ContractId("layer/entity/EntityEnum"),
            name = "EntityEnum",
            description = "Перечисление",
            kind = ContractKind.HELPER,
            releaseVersions = null,
            values = emptyList(),
            sample = null,
        ),
        ContractId("layer/entity/EntityInline") to OneOfContract(
            id = ContractId("layer/entity/EntityInline"),
            name = "EntityInline",
            description = "Варианты",
            kind = ContractKind.HELPER,
            releaseVersions = null,
            variants = emptyList(),
            sample = null,
        ),
        ContractId("layer/entity/EntityNone") to OneOfContract(
            id = ContractId("layer/entity/EntityNone"),
            name = "EntityNone",
            description = "Варианты",
            kind = ContractKind.HELPER,
            releaseVersions = null,
            variants = emptyList(),
            sample = null,
        ),
    ),
)
