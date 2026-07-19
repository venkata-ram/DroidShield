package dev.droidshield.gradleplugin

import com.android.build.api.variant.AndroidComponentsExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.dsl.KotlinAndroidProjectExtension
import kotlin.random.Random

/**
 * Generates one Kotlin source file exposing a version-derived
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
        val generateSeedTask = target.tasks.register(
            "generateDroidShieldSeed",
            GenerateDroidShieldSeedTask::class.java,
        ) { task ->
            task.outputDir.set(target.layout.buildDirectory.dir("generated/droidshield/kotlin"))
        }

        // withId defers until the Kotlin Android plugin is applied, whichever
        // order the consumer's plugins {} block lists them in — unlike a
        // one-shot hasPlugin() check, this doesn't depend on dev.droidshield
        // being applied after org.jetbrains.kotlin.android.
        target.plugins.withId("org.jetbrains.kotlin.android") {
            val kotlinExtension = target.extensions.getByType(KotlinAndroidProjectExtension::class.java)
            kotlinExtension.sourceSets.getByName("main").kotlin.srcDir(generateSeedTask.map { it.outputDir })
        }

        // The no-Android-plugin warning has to be deferred too. Reading the
        // extension eagerly here made the check order-dependent in exactly
        // the way the comment above says it must not be: applying
        // dev.droidshield before com.android.application warned and
        // early-returned on a perfectly valid build. afterEvaluate runs once
        // the consumer's whole plugins {} block has been processed.
        target.afterEvaluate {
            // Build scripts normally assign `version` after their plugins {}
            // block. Resolving the seed in apply() therefore observed
            // Project.DEFAULT_VERSION ("unspecified") instead of the release
            // version. Resolve it only after the complete script has run.
            generateSeedTask.configure { task ->
                task.seed.set(resolveSeed(it))
            }

            if (it.extensions.findByType(AndroidComponentsExtension::class.java) == null) {
                it.logger.warn(
                    "DroidShield Gradle plugin applied to '${it.path}', but no Android Gradle Plugin " +
                        "(application or library) was found — generateDroidShieldSeed will run but its output " +
                        "won't be wired into any compilation. Apply com.android.application or " +
                        "com.android.library alongside dev.droidshield.",
                )
            }
        }
    }

    /**
     * `Random(System.nanoTime())` produced a different seed on every
     * *configuration*, not every release. Because [GenerateDroidShieldSeedTask]
     * declares the seed as an `@Input`, the task was never up to date, so
     * it rewrote its source file and forced a full Kotlin recompile on
     * every single build — including no-op ones — and made the build
     * unreproducible and configuration-cache-hostile.
     *
     * Polymorphism only needs the seed to differ between *shipped builds*,
     * not between compiles of the same source. Deriving it from the
     * project's identity and version gives a value that is stable within a
     * version (so incremental builds cache correctly) and changes when the
     * version is bumped for a release. `-PdroidshieldSeed=<n>` still pins
     * it explicitly, which is what CI should use when it needs a specific
     * build's ordering reproduced.
     */
    private fun resolveSeed(target: Project): Long =
        (target.findProperty("droidshieldSeed") as? String)?.toLongOrNull()
            ?: Random("${target.path}:${target.version}".hashCode()).nextLong()
}
