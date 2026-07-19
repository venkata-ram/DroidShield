package dev.droidshield.data.checks.emulator

import android.content.Context
import android.os.BatteryManager
import dev.droidshield.data.AndroidCheckContext
import dev.droidshield.domain.CheckContext
import dev.droidshield.domain.CheckResult
import dev.droidshield.domain.Severity
import dev.droidshield.domain.ThreatCategory
import dev.droidshield.domain.ThreatCheck

/**
 * CHECKS_SEED_LIST.md EMULATOR #9. Emulators frequently report a fixed,
 * always-plugged-in, always-full battery status.
 */
class BatteryStatusCheck : ThreatCheck {
    override val id: String = "emulator.battery_status"
    override val category: ThreatCategory = ThreatCategory.EMULATOR
    override val severity: Severity = Severity.LOW

    override fun evaluate(context: CheckContext): CheckResult {
        val androidContext = (context as AndroidCheckContext).androidContext
        val batteryManager = androidContext.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
            ?: return CheckResult(id, category, severity, detected = false)

        val level = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        val isCharging = batteryManager.isCharging

        val detected = level == 100 && isCharging

        return CheckResult(id, category, severity, detected, detail = "level=$level charging=$isCharging")
    }
}
