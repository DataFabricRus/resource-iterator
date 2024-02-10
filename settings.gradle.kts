rootProject.name = "resource-iterator"

pluginManagement {
    plugins {
        plugins {
            val kotlinVersion: String by settings
            kotlin("multiplatform") version kotlinVersion
        }
    }
}
