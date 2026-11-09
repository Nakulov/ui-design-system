package io.github.nakulov.designsystem.generator.model

import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.isRegularFile
import kotlin.io.path.name
import kotlin.io.path.nameWithoutExtension
import kotlin.io.path.relativeTo
import kotlin.io.path.toPath
import kotlin.io.path.walk

const val COMPONENT = "core/component/Component"
const val BADGE = "components/badge/Badge"
const val BADGE_CONTENT = "components/badge/BadgeContent"
const val BORDER_WIDTH = "foundation/borderWidth/BorderWidth"
const val TEXT_ALIGNMENT = "components/text/TextAlignment"
const val PRESSABLE_STATES = "components/pressable/PressableStates"
const val LAYOUT_PINNED = "core/layout/LayoutPinned"

/** Каталог `contracts/` репозитория: путь передаёт сборка в свойстве `contracts`. */
fun makeContractsRoot(): Path = Path(System.getProperty("contracts"))

/** Тип-ссылка на контракт по идентификатору. */
fun makeContractRef(id: String): TypeRef.Contract = TypeRef.Contract(ContractId(id))

/** Идентификаторы по файлам: главные контракты и `definitions/` (schema-format.md, раздел 11.1). */
fun makeContractIds(root: Path): Set<ContractId> = root.walk()
    .filter { it.isRegularFile() && it.name.endsWith(".json") }
    .map { it.relativeTo(root) }
    .filter { path ->
        val entity = path.getName(1).name
        path.nameCount == 3 && path.nameWithoutExtension == entity.replaceFirstChar { it.uppercase() } ||
            path.nameCount == 4 && path.getName(2).name == "definitions"
    }
    .map { ContractId("${it.getName(0)}/${it.getName(1)}/${it.nameWithoutExtension}") }
    .toSet()

/** Каталог контрактов с ошибкой из ресурсов `broken/<случай>/`. */
fun makeBrokenContractsRoot(case: String): Path =
    ContractResolverSpec::class.java.getResource("/broken/$case")!!.toURI().toPath()
