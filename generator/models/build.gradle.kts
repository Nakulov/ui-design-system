// Модели Kotlin из контрактов (#239): код генерирует корневой проект в build/, в git его нет. Сборка — обычный jar:
// библиотека отрисовки на Kotlin Multiplatform собирается только под JVM-цели (Android и десктоп, #237), ей хватает jar.
plugins {
    id("org.jetbrains.kotlin.jvm")
    `maven-publish`
}

group = "io.github.nakulov"
version = "0.1.0-SNAPSHOT"

val generator = configurations.create("generator") {
    isCanBeConsumed = false
}

dependencies {
    generator(project(":"))
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

kotlin {
    jvmToolchain(21)
    sourceSets.main {
        kotlin.srcDir(generate)
    }
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

// Тестов моделей пока нет (тесты образцов — PR B): check компилирует модели сам.
tasks.named("check") {
    dependsOn("compileKotlin")
}
