rootProject.name = "resource-iterator"

pluginManagement {
    plugins {
        plugins {
            val kotlinVersion: String by settings
            val dokkaVersion: String by settings

            kotlin("multiplatform") version kotlinVersion
            id("org.jetbrains.dokka") version dokkaVersion
        }
    }
}
