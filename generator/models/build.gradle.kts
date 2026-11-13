// Модели Kotlin из контрактов (#239): код генерирует корневой проект в build/, в git его нет. Сборка — обычный jar:
// библиотека отрисовки на Kotlin Multiplatform собирается только под JVM-цели (Android и десктоп, #237), ей хватает jar.
plugins {
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.kotlin.plugin.serialization")
    `maven-publish`
}

group = "io.github.nakulov"
version = "0.1.0-SNAPSHOT"

val generator = configurations.create("generator") {
    isCanBeConsumed = false
}

dependencies {
    generator(project(":"))

    // Зависимость в pom не попадает: kotlinx-serialization-json библиотека отрисовки подключает сама.
    compileOnly(libs.kotlinx.serialization.json)

    testImplementation(libs.kotlinx.serialization.json)
    testImplementation(libs.kotestkit)
    testImplementation(libs.kotestkit.serialization)
}

val contracts = rootProject.layout.projectDirectory.dir("../contracts")
val generated = layout.buildDirectory.dir("generated/kotlin")

// Генератор на ../contracts → build/generated/kotlin; старый вывод удаляется, чтобы не остались файлы удалённых
// контрактов.
val generate = tasks.register<JavaExec>("generate") {
    description = "Генерирует модели Kotlin из контрактов"
    classpath = generator
    mainClass = "io.github.nakulov.designsystem.generator.MainKt"
    inputs.dir(contracts)
    outputs.dir(generated)
    argumentProviders += CommandLineArgumentProvider {
        listOf(contracts.asFile.absolutePath, generated.get().asFile.absolutePath)
    }
    doFirst { delete(generated) }
}

val repository = rootProject.layout.projectDirectory.dir("..")
val sampleTests = layout.buildDirectory.dir("generated/sampleTests")

// Образцы samples/, demos/ (поле model) и screens/ → тестовые ресурсы с тем же путём; спека — строка на образец:
// shouldRoundTripTest с корнем по главному контракту сущности. Новый образец попадает в тесты сам.
val generateSampleTests = tasks.register("generateSampleTests") {
    description = "Копирует образцы в тестовые ресурсы и пишет спеку «туда и обратно»"
    inputs.dir(contracts)
    inputs.dir(repository.dir("screens"))
    outputs.dir(sampleTests)
    doLast {
        val output = sampleTests.get().asFile
        delete(output)
        val resources = output.resolve("resources")
        val lines = mutableListOf<String>()
        val contractsDir = contracts.asFile
        contractsDir.walkTopDown().filter { file -> file.isFile && file.extension == "json" }.sorted().forEach { file ->
            val path = file.relativeTo(repository.asFile).invariantSeparatorsPath
            val kind = file.parentFile.name
            if (kind != "samples" && kind != "demos") return@forEach
            val target = resources.resolve(path)
            target.parentFile.mkdirs()
            if (kind == "demos") {
                val demo = groovy.json.JsonSlurper().parse(file) as Map<String, Any?>
                target.writeText(groovy.json.JsonOutput.toJson(demo["model"]))
                lines += "shouldRoundTripTest<$COMPONENT>(resource = \"$path\")"
            } else {
                file.copyTo(target)
                lines += sampleTest(path, file.parentFile.parentFile)
            }
        }
        repository.dir("screens").asFile.listFiles().orEmpty().filter { file -> file.extension == "json" }.sorted()
            .forEach { file ->
                val path = file.relativeTo(repository.asFile).invariantSeparatorsPath
                file.copyTo(resources.resolve(path))
                lines += "shouldRoundTripTest<$COMPONENT>(resource = \"$path\")"
            }
        val spec = output.resolve("kotlin/io/github/nakulov/designsystem/SamplesSpec.kt")
        spec.parentFile.mkdirs()
        spec.writeText(
            """
            |// Сгенерировано задачей generateSampleTests (generator/models). Не править.
            |
            |package io.github.nakulov.designsystem
            |
            |import io.github.nakulov.kotestkit.serialization.withJson
            |import io.kotest.core.spec.style.BehaviorSpec
            |
            |class SamplesSpec :
            |    BehaviorSpec({
            |        withJson(makeSamplesJson()) {
            |${lines.joinToString(separator = "\n") { line -> "            $line" }}
            |        }
            |    })
            |""".trimMargin(),
        )
    }
}

kotlin {
    jvmToolchain(21)
    sourceSets.main {
        kotlin.srcDir(generate)
    }
    sourceSets.test {
        kotlin.srcDir(generateSampleTests.map { sampleTests.get().dir("kotlin") })
        resources.srcDir(generateSampleTests.map { sampleTests.get().dir("resources") })
    }
}

tasks.test {
    useJUnitPlatform()
}

java {
    withSourcesJar()
}

// Артефакт io.github.nakulov:design-system-kotlin; не публикуется, локально — ./gradlew :models:publishToMavenLocal.
publishing {
    publications.create<MavenPublication>("kotlin") {
        artifactId = "design-system-kotlin"
        from(components["java"])
    }
}

/** Корень ссылок на компоненты в спеке образцов. */
val COMPONENT = "io.github.nakulov.designsystem.core.component.Component"

/** Тест образца сущности [entity]: корень — главный контракт (11.1); `oneOf` — через свой сериализатор. */
fun sampleTest(path: String, entity: File): String {
    val name = entity.name.replaceFirstChar { char -> char.uppercase() }
    val contract = groovy.json.JsonSlurper().parse(entity.resolve("$name.json")) as Map<*, *>
    val type = "io.github.nakulov.designsystem.${entity.parentFile.name}.${entity.name}.$name"
    return when {
        contract["extends"] != null -> "shouldRoundTripTest<$COMPONENT>(resource = \"$path\")"
        contract["type"] == "object" -> "shouldRoundTripTest<$type>(resource = \"$path\")"
        else -> "shouldRoundTripTest(resource = \"$path\", serializer = ${type}Serializer)"
    }
}
