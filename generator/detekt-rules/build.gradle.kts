plugins {
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.kotlinx.kover")
}

dependencies {
    compileOnly(libs.detekt.api)

    testImplementation(libs.detekt.api)
    testImplementation(libs.detekt.test)
    testImplementation(libs.kotestkit)
}

kotlin {
    jvmToolchain(21)
}

tasks.test {
    useJUnitPlatform()
}

// Отчёт и порог покрытия — как в модуле генератора: 95% по строкам, koverVerify входит в check.
tasks.withType<Test>().configureEach {
    finalizedBy("koverHtmlReport")
}

kover {
    reports {
        verify {
            rule { minBound(95) }
        }
    }
}
