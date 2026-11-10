package io.github.nakulov.designsystem.generator.detekt

import dev.detekt.api.Finding
import dev.detekt.api.RequiresAnalysisApi
import dev.detekt.api.Rule
import dev.detekt.test.lint
import dev.detekt.test.lintWithContext
import dev.detekt.test.utils.compileContentForTest
import dev.detekt.test.utils.createEnvironment

/** Образец `samples/<правило>/<name>.kt.txt` под правилом; файл образца называется `<name>.kt`. */
fun Rule.lintSample(name: String): List<Reported> {
    val file = compileContentForTest(content = readSample(rule = this, name = name), filename = "$name.kt")
    return lint(file).toReported()
}

/** То же для правила с анализом типов: образец компилируется с classpath тестов (JDK, stdlib). */
fun <T> T.lintSampleWithContext(name: String): List<Reported> where T : Rule, T : RequiresAnalysisApi {
    return lintWithContext(environment = createEnvironment(), content = readSample(rule = this, name = name))
        .toReported()
}

private fun readSample(rule: Rule, name: String): String {
    val path = "/samples/${rule.ruleName.value}/$name.kt.txt"
    val resource = requireNotNull(Reported::class.java.getResource(/* name = */ path)) { "Нет образца $path" }
    return resource.readText()
}

private fun List<Finding>.toReported(): List<Reported> {
    return map { finding ->
        val source = finding.entity.location.source
        Reported(line = source.line, column = source.column, message = finding.message)
    }
}
