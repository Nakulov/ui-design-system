plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
}

dependencies {
    implementation(libs.kotlinx.serialization.json)

    testImplementation(libs.kotestkit)
}

kotlin {
    jvmToolchain(21)
}

tasks.test {
    useJUnitPlatform()
    // Тесты читают настоящие контракты репозитория: правка контракта перезапускает тесты.
    val contracts = layout.projectDirectory.dir("../contracts")
    inputs.dir(contracts)
    systemProperty("contracts", contracts.asFile.absolutePath)
}
