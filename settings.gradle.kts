pluginManagement {
    includeBuild("droidshield-gradle-plugin")
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "droidshield"

include(
    ":droidshield-domain",
    ":droidshield-data-android",
    ":droidshield-native",
    ":droidshield-engine",
    ":droidshield-sdk",
    ":sample-app",
)
