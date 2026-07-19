package dev.droidshield.data.checks.hook

import dev.droidshield.domain.CheckContext
import dev.droidshield.domain.CheckResult
import dev.droidshield.domain.Severity
import dev.droidshield.domain.ThreatCategory
import dev.droidshield.domain.ThreatCheck
import java.net.InetSocketAddress
import java.net.Socket

/**
 * CHECKS_SEED_LIST.md HOOK #2 — frida-server's default listening ports.
 * Requires the host app to hold `android.permission.INTERNET`, even for a
 * loopback connection — without it, [isPortOpen] fails closed (returns
 * false) rather than throwing, so this degrades to a silent no-op on an
 * app that hasn't declared the permission instead of crashing the engine.
 */
class FridaPortCheck : ThreatCheck {
    override val id: String = "hook.frida_default_port"
    override val category: ThreatCategory = ThreatCategory.HOOK
    override val severity: Severity = Severity.MEDIUM

    private val fridaPorts = listOf(27042, 27043)
    private val connectTimeoutMillis = 200

    override fun evaluate(context: CheckContext): CheckResult {
        val openPort = fridaPorts.firstOrNull { port -> isPortOpen(port) }
        return CheckResult(id, category, severity, detected = openPort != null, detail = openPort?.toString())
    }

    private fun isPortOpen(port: Int): Boolean = try {
        Socket().use { socket ->
            socket.connect(InetSocketAddress("127.0.0.1", port), connectTimeoutMillis)
            true
        }
    } catch (e: Exception) {
        false
    }
}
