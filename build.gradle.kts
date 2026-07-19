import com.android.build.api.dsl.LibraryExtension

plugins {
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.jvm) apply false
}

// ---------------------------------------------------------------------------
// Publishing. See DECISIONS.md D031.
//
// All five library modules are published, not just droidshield-sdk. The SDK
// declares project(":droidshield-domain") et al as dependencies, and those
// become real POM coordinates once resolved from a repository — publishing the
// .aar alone would hand consumers an artifact that can't resolve. sample-app is
// deliberately excluded: it's a consumer, not a deliverable.
//
// droidshield-gradle-plugin is NOT in this list. It's a standalone included
// build (D027) with its own settings.gradle.kts, so it configures publishing
// itself and is driven by the aggregate task at the bottom of this file.
// ---------------------------------------------------------------------------
val publishedModules = setOf(
    "droidshield-domain",
    "droidshield-data-android",
    "droidshield-native",
    "droidshield-engine",
    "droidshield-sdk",
)

subprojects {
    if (name !in publishedModules) return@subprojects

    group = "dev.droidshield"
    version = property("droidshield.version") as String

    apply(plugin = "maven-publish")

    // Registers the publication once the named SoftwareComponent exists.
    //
    // The afterEvaluate has to be registered from inside a plugins.withId
    // callback rather than directly in this subprojects block. This block runs
    // while the *root* project is evaluating, i.e. before any subproject build
    // script has applied AGP — an afterEvaluate registered here would be queued
    // ahead of AGP's own, and components["release"] (which AGP creates in its
    // afterEvaluate) would not exist yet. Registering from the withId callback
    // queues it behind AGP's instead.
    fun Project.registerPublication(componentName: String) = afterEvaluate {
        extensions.configure<PublishingExtension>("publishing") {
            publications {
                create<MavenPublication>("maven") {
                    from(components[componentName])
                    artifactId = this@registerPublication.name

                    pom {
                        name.set(this@registerPublication.name)
                        description.set("DroidShield — Android runtime threat detection SDK.")
                        url.set("https://github.com/venkata-ram/DroidShield")
                        licenses {
                            license {
                                name.set("The Apache License, Version 2.0")
                                url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
                            }
                        }
                        developers {
                            developer {
                                id.set("venkata-ram")
                                name.set("venkataram k")
                            }
                        }
                        scm {
                            url.set("https://github.com/venkata-ram/DroidShield")
                        }
                    }
                }
            }
        }
    }

    // Android and JVM modules expose their publishable output through
    // different components, so each needs its own opt-in. Both carry a sources
    // jar — consumers otherwise land on decompiled bytecode in the IDE, which
    // makes an obfuscation-adjacent SDK unnecessarily painful to integrate
    // against.
    plugins.withId("com.android.library") {
        extensions.configure<LibraryExtension>("android") {
            publishing {
                // Only the release variant is published. Publishing debug too
                // would need multipleVariants() plus variant-aware consumer
                // config, and there's no reason to ship a debuggable build of
                // a security SDK.
                singleVariant("release") {
                    withSourcesJar()
                }
            }
        }
        registerPublication("release")
    }

    plugins.withId("org.jetbrains.kotlin.jvm") {
        extensions.configure<JavaPluginExtension>("java") {
            withSourcesJar()
        }
        registerPublication("java")
    }
}

// JitPack builds by running an install command and then collecting whatever
// landed in the local Maven repository. The included build's tasks are not
// aggregated into the root build automatically, so without this task the
// Gradle plugin would silently never be published.
tasks.register("publishAllToMavenLocal") {
    group = "publishing"
    description = "Publishes every DroidShield library and the Gradle plugin to ~/.m2."

    dependsOn(
        publishedModules.map { ":$it:publishToMavenLocal" }
    )
    dependsOn(
        gradle.includedBuild("droidshield-gradle-plugin").task(":publishToMavenLocal")
    )
}
