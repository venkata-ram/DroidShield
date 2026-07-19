package dev.droidshield.domain

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class CheckResultTest {

    private class FakeCheck(
        override val id: String = "fake.check",
        override val category: ThreatCategory = ThreatCategory.ROOT,
        override val severity: Severity = Severity.HIGH,
        private val detected: Boolean,
    ) : ThreatCheck {
        override fun evaluate(context: CheckContext): CheckResult =
            CheckResult(id, category, severity, detected)
    }

    private object FakeContext : CheckContext

    @Test
    fun `evaluate returns detected result matching the check's own metadata`() {
        val check = FakeCheck(detected = true)

        val result = check.evaluate(FakeContext)

        assertThat(result.checkId).isEqualTo("fake.check")
        assertThat(result.category).isEqualTo(ThreatCategory.ROOT)
        assertThat(result.severity).isEqualTo(Severity.HIGH)
        assertThat(result.detected).isTrue()
    }

    @Test
    fun `evaluate can report a clean result`() {
        val check = FakeCheck(detected = false)

        val result = check.evaluate(FakeContext)

        assertThat(result.detected).isFalse()
    }
}
