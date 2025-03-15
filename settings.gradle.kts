pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.PREFER_SETTINGS) // Esto ahora está bien
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "Minechap"
include(":app")
