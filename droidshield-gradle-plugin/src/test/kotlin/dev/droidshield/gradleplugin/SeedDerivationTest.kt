package dev.droidshield.gradleplugin

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Covers the seed-derivation contract that makes release-seeded ordering a
 * moat (DECISIONS.md D038): the same input must reproduce (so builds cache
 * and can be reproduced with an explicit pin), different inputs must
 * diverge (so a secret actually changes the ordering), and the derivation
 * must not be `String.hashCode()`.
 */
class SeedDerivationTest {

    private val plugin = DroidShieldPlugin()

    @Test
    fun `same material reproduces the same seed`() {
        val material = "secret :app:1.2.3"
        assertThat(plugin.seedFrom(material)).isEqualTo(plugin.seedFrom(material))
    }

    @Test
    fun `a different secret produces a different seed for the same build identity`() {
        val withSecretA = plugin.seedFrom("secretA :app:1.2.3")
        val withSecretB = plugin.seedFrom("secretB :app:1.2.3")
        assertThat(withSecretA).isNotEqualTo(withSecretB)
    }

    @Test
    fun `a version bump changes the seed`() {
        val v1 = plugin.seedFrom("secret :app:1.2.3")
        val v2 = plugin.seedFrom("secret :app:1.2.4")
        assertThat(v1).isNotEqualTo(v2)
    }

    @Test
    fun `seed is not the JVM String hashCode`() {
        val material = ":app:1.2.3"
        assertThat(plugin.seedFrom(material)).isNotEqualTo(material.hashCode().toLong())
    }
}
