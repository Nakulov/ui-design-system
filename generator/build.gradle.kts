plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
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
