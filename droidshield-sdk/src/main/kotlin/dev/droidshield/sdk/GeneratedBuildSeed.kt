package dev.droidshield.sdk

/**
 * Reads the seed the Gradle plugin generates into the *host app's*
 * compilation (`dev.droidshield.generated.DroidShieldBuildSeed.SEED`).
 *
 * The SDK modules cannot import that class directly — it does not exist
 * until the consumer applies `com.github.venkata-ram.DroidShield`, and it
 * is compiled into the host app, not into this library — so the lookup is
 * done reflectively. This is what lets an integrator get release-seeded
 * ordering just by applying the plugin, without hand-wiring
 * `DroidShieldConfig.polymorphicSeed`. See DECISIONS.md D038.
 *
 * When the plugin is not applied the class is absent and [value] is null,
 * which the engine treats as unseeded/deterministic ordering. The result
 * is resolved once and cached, so a missing class costs one failed lookup
 * rather than one per engine construction.
 */
internal object GeneratedBuildSeed {

    private const val GENERATED_CLASS = "dev.droidshield.generated.DroidShieldBuildSeed"
    private const val SEED_FIELD = "SEED"

    val value: Long? by lazy { readGeneratedSeed() }

    private fun readGeneratedSeed(): Long? = runCatching {
        val field = Class.forName(GENERATED_CLASS).getDeclaredField(SEED_FIELD)
        field.isAccessible = true
        field.getLong(null)
    }.getOrNull()
}
