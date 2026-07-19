package dev.droidshield.gradleplugin

import com.android.build.api.variant.AndroidComponentsExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.dsl.KotlinAndroidProjectExtension
import kotlin.random.Random

/**
 * ARCHITECTURE.md §3 — build-time half of the polymorphic-injection
 * mechanism. Generates one Kotlin source file per build exposing a random
 * (or pinned, via `-PdroidshieldSeed=<n>`) seed, wired into the host app's
 * (or library's) Kotlin compilation via the Kotlin Gradle Plugin's own
 * source-set API. See DECISIONS.md D026 for why this is source codegen
 * rather than ASM bytecode instrumentation of host-app classes, and D028
 * for why the wiring uses KGP's `sourceSets` rather than AGP's
 * `variant.sources.kotlin` (the latter doesn't actually reach
 * `compileDebugKotlin` for a plain Android+Kotlin module — verified
 * directly while building this plugin).
 *
 * Applying this plugin with no Android plugin present is a no-op (with a
 * warning) rather than a failure — it can be applied speculatively without
 * breaking a non-Android module.
 */
class DroidShieldPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        val seed = resolveSeed(target)

        val generateSeedTask = target.tasks.register(
            "generateDroidShieldSeed",
            GenerateDroidShieldSeedTask::class.java,
        ) { task ->
            task.seed.set(seed)
            task.outputDir.set(target.layout.buildDirectory.dir("generated/droidshield/kotlin"))
        }

        val androidComponents = target.extensions.findByType(AndroidComponentsExtension::class.java)
        if (androidComponents == null) {
            target.logger.warn(
                "DroidShield Gradle plugin applied to '${target.path}', but no Android Gradle Plugin " +
                    "(application or library) was found — generateDroidShieldSeed will run but its output " +
                    "won't be wired into any compilation. Apply com.android.application or " +
                    "com.android.library before dev.droidshield.",
            )
            return
        }

        // withId defers until the Kotlin Android plugin is applied, whichever
        // order the consumer's plugins {} block lists them in — unlike a
        // one-shot hasPlugin() check, this doesn't depend on dev.droidshield
        // being applied after org.jetbrains.kotlin.android.
        target.plugins.withId("org.jetbrains.kotlin.android") {
            val kotlinExtension = target.extensions.getByType(KotlinAndroidProjectExtension::class.java)
            kotlinExtension.sourceSets.getByName("main").kotlin.srcDir(generateSeedTask.map { it.outputDir })
        }
    }

    private fun resolveSeed(target: Project): Long =
        (target.findProperty("droidshieldSeed") as? String)?.toLongOrNull()
            ?: Random(System.nanoTime()).nextLong()
}
