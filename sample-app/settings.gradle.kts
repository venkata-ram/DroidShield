// sample-app is a standalone Gradle build, deliberately NOT part of the root
// build (it is absent from the root settings.gradle.kts `include(...)` list).
// It consumes DroidShield the way a real integrator does — published artifacts
// off JitPack, not `project(":droidshield-sdk")` — so this file doubles as an
// executable version of the setup in INTEGRATION.md. If JitPack publishing
// breaks, this build stops compiling, which is the point.

pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
        maven("https://jitpack.io")
    }
    resolutionStrategy {
        eachPlugin {
            // A plugin marker must live at a group ID equal to the plugin ID,
            // but JitPack rewrites every group to com.github.<user>.<repo>, so
            // the marker is never published where Gradle looks for it. This
            // maps the plugin ID onto the coordinate JitPack actually serves.
            if (requested.id.id == "dev.droidshield") {
                useModule("com.github.venkata-ram.DroidShield:droidshield-gradle-plugin:${requested.version}")
            }
        }
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
