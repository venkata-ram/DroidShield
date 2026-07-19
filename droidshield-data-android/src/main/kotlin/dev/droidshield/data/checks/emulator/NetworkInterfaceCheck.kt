package dev.droidshield.data.checks.emulator

import dev.droidshield.domain.CheckContext
import dev.droidshield.domain.CheckResult
import dev.droidshield.domain.Severity
import dev.droidshield.domain.ThreatCategory
import dev.droidshield.domain.ThreatCheck
import java.net.NetworkInterface
import java.util.Collections

/**
 * CHECKS_SEED_LIST.md EMULATOR #7. Android Studio's AVD networking stack
 * conventionally names its interface `eth0` with the well-known
 * `10.0.2.x` guest address range — a real device on Wi-Fi/cellular almost
 * never presents this combination.
 */
class NetworkInterfaceCheck : ThreatCheck {
    override val id: String = "emulator.network_interface"
    override val category: ThreatCategory = ThreatCategory.EMULATOR
    override val severity: Severity = Severity.LOW

    override fun evaluate(context: CheckContext): CheckResult {
        val detected = try {
            Collections.list(NetworkInterface.getNetworkInterfaces()).any { iface ->
                iface.name == "eth0" &&
                    Collections.list(iface.inetAddresses).any { it.hostAddress?.startsWith("10.0.2.") == true }
            }
        } catch (e: Exception) {
            false
        }

        return CheckResult(id, category, severity, detected)
    }
}
