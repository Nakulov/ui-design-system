"""Сопоставление JSON-схем в Android Studio и других IDE JetBrains (.idea/jsonSchemas.xml).

Маски IntelliJ не умеют исключать файлы, поэтому контракты перечисляются поимённо, а файлы значений и журналы
изменений — масками.
"""

from xml.sax.saxutils import quoteattr

from contracts import ROOT

PATH = ROOT / ".idea" / "jsonSchemas.xml"


def item(path, kind):
    pattern = "                    <option name=\"pattern\" value=\"true\" />\n" if kind == "Pattern" else ""
    return (f"                  <Item>\n"
            f"{pattern}"
            f"                    <option name=\"path\" value={quoteattr(path)} />\n"
            f"                    <option name=\"mappingKind\" value=\"{kind}\" />\n"
            f"                  </Item>\n")


def entry(name, schema, items):
    return (f"        <entry key=\"{name}\">\n"
            f"          <value>\n"
            f"            <SchemaInfo>\n"
            f"              <option name=\"name\" value=\"{name}\" />\n"
            f"              <option name=\"relativePathToSchema\" value=\"{schema}\" />\n"
            f"              <option name=\"patterns\">\n"
            f"                <list>\n"
            f"{''.join(items)}"
            f"                </list>\n"
            f"              </option>\n"
            f"            </SchemaInfo>\n"
            f"          </value>\n"
            f"        </entry>\n")


def json_schemas(registry):
    """Текст .idea/jsonSchemas.xml: каждый контракт — по файлу, файлы значений и журналы — по маске."""
    contracts = sorted(c.path.relative_to(ROOT).as_posix() for c in registry)
    return ("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
            "<project version=\"4\">\n"
            "  <component name=\"JsonSchemaMappingsProjectConfiguration\">\n"
            "    <state>\n"
            "      <map>\n"
            + entry("Контракт", "specifications/contract.schema.json", [item(p, "File") for p in contracts])
            + entry("Файл значений", "specifications/values.schema.json", [item("*.values.json", "Pattern")])
            + entry("Журнал изменений", "specifications/changelog.schema.json", [item("*.changelog.json", "Pattern")])
            + "      </map>\n"
            "    </state>\n"
            "  </component>\n"
            "</project>\n")
