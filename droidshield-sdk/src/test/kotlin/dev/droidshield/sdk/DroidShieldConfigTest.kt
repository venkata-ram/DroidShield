package dev.droidshield.sdk

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class DroidShieldConfigTest {
    @Test
    fun `guarded method cooldown defaults to thirty seconds`() {
        assertThat(DroidShieldConfig().guardedMethodMinIntervalMillis).isEqualTo(30_000L)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `guarded method cooldown rejects negative values`() {
        DroidShieldConfig(guardedMethodMinIntervalMillis = -1L)
    }
}
