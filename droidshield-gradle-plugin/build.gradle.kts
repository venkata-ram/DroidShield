// `java` alone would resolve to the JavaPluginExtension this script applies,
// not the JDK package, so Properties has to be imported explicitly.
import java.util.Properties

// Build-time only: AGP Variant API + source codegen (see DECISIONS.md
// D026). No dependency on droidshield-engine, droidshield-data-android, or
// droidshield-native — it never needs to know about specific checks, and
// (as of D027) it's now a standalone included build, so a project
// dependency back into the main build isn't even resolvable.
plugins {
    alias(libs.plugins.kotlin.jvm)
    `java-gradle-plugin`
    `maven-publish`
}

// Deliberately NOT "dev.droidshield" like the library modules, and not left to
// JitPack's group rewriting either (DECISIONS.md D033).
//
// Gradle resolves `plugins { id("X") }` by looking for a marker artifact at
// groupId == X. JitPack rewrites every published group to
// com.github.<user>.<repo>, so a plugin ID of "dev.droidshield" produces a
// marker Gradle can never find, and consumers need a resolutionStrategy block
// to bridge it. Naming the group *and* the plugin ID after the coordinate
// JitPack actually serves makes the marker land where Gradle looks, so
// consuming this plugin is plain `plugins { id(...) }` with no workaround.
//
// Setting it explicitly rather than relying on the rewrite means local and
// JitPack publications produce identical coordinates, so what is verified
// against mavenLocal is what integrators get.
group = "com.github.venkata-ram.DroidShield"

// This is a standalone included build (D027), so it does not inherit the root
// project's gradle.properties. Reading the parent file directly keeps the
// published version in exactly one place rather than drifting between two.
version = Properties().run {
    rootDir.resolve("../gradle.properties").inputStream().use { load(it) }
    getProperty("droidshield.version")
        ?: error("droidshield.version missing from the root gradle.properties")
}

kotlin {
    jvmToolchain(17)
}

java {
    withSourcesJar()
}

dependencies {
    compileOnly("com.android.tools.build:gradle-api:8.7.2")
    // See DECISIONS.md D028: AGP's variant.sources.kotlin API doesn't
    // actually get consumed by compileDebugKotlin for a plain (non-KMP)
    // Android+Kotlin module in this AGP/Kotlin combo — verified directly.
    // The Kotlin Gradle Plugin's own source-set API is what's needed.
    compileOnly("org.jetbrains.kotlin:kotlin-gradle-plugin:2.0.21")

    testImplementation(libs.junit)
    testImplementation(libs.truth)
    testImplementation("org.ow2.asm:asm:9.7.1")
}

gradlePlugin {
    plugins {
        create("droidshield") {
            // Must equal `group` above — see the comment there. The generated
            // Kotlin still lands in the dev.droidshield package; only the
            // Maven/plugin coordinate changed.
            id = "com.github.venkata-ram.DroidShield"
            implementationClass = "dev.droidshield.gradleplugin.DroidShieldPlugin"
        }
    }
}
