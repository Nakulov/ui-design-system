plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.kover)
    alias(libs.plugins.detekt)
    application
}

application {
    mainClass = "io.github.nakulov.designsystem.generator.MainKt"
}

dependencies {
    implementation(libs.kotlinx.serialization.json)

    testImplementation(libs.kotestkit)
    testImplementation(libs.kotestkit.serialization)

    detektPlugins(libs.detekt.rules.ktlint.wrapper)
    detektPlugins(project(":detekt-rules"))
}

kotlin {
    jvmToolchain(21)
}

tasks.test {
    useJUnitPlatform()
}

// HTML-отчёт Kover собирается после каждого прогона тестов: build/reports/kover/html.
tasks.withType<Test>().configureEach {
    finalizedBy("koverHtmlReport")
}

// Порог 95% по строкам, поднимаем постепенно: koverVerify входит в check, падение порога роняет сборку и CI.
kover {
    reports {
        filters {
            excludes {
                // Точка входа: exitProcess завершает JVM, в тестах её не вызвать; проверяется запуском в CI.
                classes("io.github.nakulov.designsystem.generator.MainKt")
            }
        }
        verify {
            rule { minBound(95) }
        }
    }
}

// Код-стайл: готовые правила detekt поверх конфига по умолчанию, оформление — правила ktlint через обёртку detekt.
detekt {
    buildUponDefaultConfig = true
    config.setFrom(file("detekt.yml"))
}

// NamedArguments работает только с анализом типов: check запускает detektMain и detektTest вместо простой detekt.
// Автоисправление оформления: ./gradlew detektMain detektTest --auto-correct.
tasks.named("check") {
    setDependsOn(dependsOn.filterNot { it is TaskProvider<*> && it.name == "detekt" })
    dependsOn("detektMain", "detektTest")
}
