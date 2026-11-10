pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenCentral()

        // KotestKit: GitHub Packages отдаёт пакет только с токеном (read:packages), даже публичный.
        // Локально — gpr.user и gpr.key в ~/.gradle/gradle.properties, в CI — переменные окружения.
        maven {
            url = uri("https://maven.pkg.github.com/Nakulov/KotestKit")
            credentials {
                username = providers.gradleProperty("gpr.user").orElse(providers.environmentVariable("GPR_USER")).get()
                password = providers.gradleProperty("gpr.key").orElse(providers.environmentVariable("GPR_KEY")).get()
            }
        }
    }
}

rootProject.name = "generator"
