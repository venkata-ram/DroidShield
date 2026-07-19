package dev.droidshield.sdk

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class GeneratedBuildSeedTest {

    /**
     * The generated `dev.droidshield.generated.DroidShieldBuildSeed` class
     * only exists in a host app that applies the Gradle plugin; it is never
     * on the SDK's own classpath. The reflective lookup must degrade to null
     * (which the engine treats as unseeded ordering) rather than throwing.
     * See DECISIONS.md D038.
     */
    @Test
    fun `absent generated seed resolves to null instead of throwing`() {
        assertThat(GeneratedBuildSeed.value).isNull()
    }
}
