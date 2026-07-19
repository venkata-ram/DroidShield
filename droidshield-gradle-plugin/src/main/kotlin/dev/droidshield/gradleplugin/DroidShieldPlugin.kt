package dev.droidshield.gradleplugin

import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.instrumentation.FramesComputationMode
import com.android.build.api.instrumentation.InstrumentationScope
import com.android.build.api.variant.AndroidComponentsExtension
import com.android.build.api.variant.ApplicationAndroidComponentsExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.dsl.KotlinAndroidProjectExtension
import java.security.MessageDigest

/**
 * Generates one Kotlin source file exposing a version-derived
 * (or pinned, via `-PdroidshieldSeed=<n>`) seed, wired into the host app's
 * (or library's) Kotlin compilation via the Kotlin Gradle Plugin's own
 * source-set API. See DECISIONS.md D026 for why the seed remains source
 * codegen rather than being embedded through ASM, and D028
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
        val extension = target.extensions.create("droidShield", DroidShieldExtension::class.java)

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

        target.plugins.withId("com.android.application") {
            configureGuardedMethodInstrumentation(target, extension)
            configureReleaseHardening(target, extension)
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
                        "com.android.library alongside com.github.venkata-ram.DroidShield.",
                )
            }
        }
    }

    private fun configureGuardedMethodInstrumentation(
        target: Project,
        extension: DroidShieldExtension,
    ) {
        val androidComponents = target.extensions.getByType(ApplicationAndroidComponentsExtension::class.java)
        androidComponents.onVariants(androidComponents.selector().all()) { variant ->
            variant.instrumentation.transformClassesWith(
                DroidShieldGuardedMethodClassVisitorFactory::class.java,
                InstrumentationScope.PROJECT,
            ) { parameters ->
                parameters.enabled.set(extension.instrumentGuardedMethods)
            }
            variant.instrumentation.setAsmFramesComputationMode(
                FramesComputationMode.COMPUTE_FRAMES_FOR_INSTRUMENTED_METHODS,
            )
        }
    }

    private fun configureReleaseHardening(
        target: Project,
        extension: DroidShieldExtension,
    ) {
        val verifyTask = target.tasks.register(
            "verifyDroidShieldReleaseHardening",
            VerifyDroidShieldReleaseHardeningTask::class.java,
        ) {
            it.group = "verification"
            it.description = "Verifies that the Android release build enables DroidShield's minimum hardening."
        }

        target.tasks.matching { it.name == "preReleaseBuild" }.configureEach {
            it.dependsOn(verifyTask)
        }

        target.afterEvaluate {
            val android = it.extensions.getByType(ApplicationExtension::class.java)
            val release = android.buildTypes.findByName("release")
                ?: error("DroidShield requires an Android 'release' build type for release hardening verification.")
            verifyTask.configure { task ->
                task.minifyEnabled.set(release.isMinifyEnabled)
                task.shrinkResources.set(release.isShrinkResources)
                task.debuggable.set(release.isDebuggable)
                task.enforcementEnabled.set(extension.enforceReleaseHardening)
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
     * not between compiles of the same source, so the seed must stay stable
     * within a version (incremental builds cache correctly) while being
     * unpredictable to an attacker who inspects a shipped build. The three
     * sources, in precedence order (see DECISIONS.md D038):
     *
     *  1. `-PdroidshieldSeed=<n>` — an explicit pin. CI uses this when it
     *     needs a specific build's ordering reproduced exactly.
     *  2. A build-seed *secret* (`-PdroidshieldSeedSecret=<s>` or the
     *     `DROIDSHIELD_SEED_SECRET` env var) folded together with the build
     *     identity through SHA-256. Stable per version, but not derivable
     *     without the secret — this is what makes the ordering a real moat
     *     rather than a value anyone can recompute from the public APK.
     *  3. A fallback derived from the public build identity alone, emitted
     *     with a warning, because an attacker who reverse-engineers one
     *     build can recompute the ordering of every version from it.
     *
     * Even the fallback goes through SHA-256 rather than `String.hashCode()`,
     * whose 32-bit output is trivially invertible; the warning, not the
     * hash, is what tells the integrator the ordering is still predictable.
     */
    private fun resolveSeed(target: Project): Long {
        (target.findProperty("droidshieldSeed") as? String)?.toLongOrNull()?.let { return it }

        val secret = (target.findProperty("droidshieldSeedSecret") as? String)?.takeIf { it.isNotBlank() }
            ?: System.getenv("DROIDSHIELD_SEED_SECRET")?.takeIf { it.isNotBlank() }

        val identity = "${target.path}:${target.version}"
        if (secret == null) {
            target.logger.warn(
                "DroidShield: no build-seed secret configured, so the polymorphic check-ordering " +
                    "seed is derived from public build identity ('$identity') and can be recomputed by " +
                    "anyone who inspects a shipped build. Set -PdroidshieldSeedSecret=<secret> or the " +
                    "DROIDSHIELD_SEED_SECRET environment variable in CI so the ordering can't be " +
                    "precomputed. See DECISIONS.md D038.",
            )
            return seedFrom(identity)
        }
        return seedFrom("$secret $identity")
    }

    /**
     * Folds [material] to a `Long` via SHA-256. Deterministic for a given
     * input (so builds of the same source cache), but — unlike
     * `String.hashCode()` — not reversible, so a secret-derived seed can't
     * be worked backwards from the emitted value.
     */
    internal fun seedFrom(material: String): Long {
        val digest = MessageDigest.getInstance("SHA-256").digest(material.toByteArray(Charsets.UTF_8))
        var seed = 0L
        for (i in 0 until Long.SIZE_BYTES) {
            seed = (seed shl 8) or (digest[i].toLong() and 0xff)
        }
        return seed
    }
}
