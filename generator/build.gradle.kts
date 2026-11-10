plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.kover)
    application
}

application {
    mainClass = "io.github.nakulov.designsystem.generator.MainKt"
}

dependencies {
    implementation(libs.kotlinx.serialization.json)

    testImplementation(libs.kotestkit)
    testImplementation(libs.kotestkit.serialization)
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
