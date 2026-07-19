package dev.droidshield.domain.backend

import com.google.common.truth.Truth.assertThat
import dev.droidshield.domain.CheckResult
import dev.droidshield.domain.Severity
import dev.droidshield.domain.ThreatCategory
import org.junit.Test

class BackendDecisionContractTest {

    @Test
    fun `maps only detected checks and never includes detail`() {
        val results = listOf(
            CheckResult("clean", ThreatCategory.ROOT, Severity.LOW, detected = false, detail = "private"),
            CheckResult("z_hook", ThreatCategory.HOOK, Severity.CRITICAL, detected = true, detail = "/data/path"),
            CheckResult("a_root", ThreatCategory.ROOT, Severity.HIGH, detected = true, detail = "su path"),
        )

        val evidence = results.toDeviceEvidence(
            sdkVersion = "0.3.0",
            appPackageName = "com.example.app",
            appVersionName = "2.1",
            appVersionCode = 21,
            androidSdk = 35,
            context = EvidenceContext(
                installationId = "install-1",
                sessionId = "session-1",
                nonce = "nonce-1",
                collectedAtEpochMillis = 1234,
            ),
        )

        assertThat(evidence.schemaVersion).isEqualTo(1)
        assertThat(evidence.checksRun).isEqualTo(3)
        assertThat(evidence.triggeredChecks).containsExactly(
            TriggeredCheck("a_root", "ROOT", "HIGH"),
            TriggeredCheck("z_hook", "HOOK", "CRITICAL"),
        ).inOrder()
        assertThat(evidence.installationId).isEqualTo("install-1")
        assertThat(evidence.sessionId).isEqualTo("session-1")
        assertThat(evidence.nonce).isEqualTo("nonce-1")
        assertThat(evidence.collectedAtEpochMillis).isEqualTo(1234)
    }
}
