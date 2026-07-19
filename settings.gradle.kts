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
)

// :sample-app is intentionally absent. It is a standalone Gradle build
// (sample-app/settings.gradle.kts) that consumes the published JitPack
// artifacts instead of project dependencies, so it verifies the real
// integration path. Build it with: cd sample-app && ../gradlew assembleDebug
