// sample-app is a standalone Gradle build, deliberately NOT part of the root
// build (it is absent from the root settings.gradle.kts `include(...)` list).
// It consumes DroidShield the way a real integrator does — published artifacts
// off JitPack, not `project(":droidshield-sdk")` — so this file doubles as an
// executable version of the setup in INTEGRATION.md. If JitPack publishing
// breaks, this build stops compiling, which is the point.
//
// Note there is no `resolutionStrategy` here: the Gradle plugin's ID matches
// the group JitPack publishes it under (DECISIONS.md D033), so adding the
// JitPack repository is the entire setup.

pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
        maven("https://jitpack.io")
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven("https://jitpack.io")
    }
}

rootProject.name = "droidshield-sample"
