rootProject.name = "resource-iterator"

pluginManagement {
    plugins {
        plugins {
            val kotlinVersion: String by settings
            kotlin("jvm") version kotlinVersion
        }
    }
}
