package dev.droidshield.data.checks.emulator

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.telephony.TelephonyManager
import androidx.core.content.ContextCompat
import dev.droidshield.data.AndroidCheckContext
import dev.droidshield.domain.CheckContext
import dev.droidshield.domain.CheckResult
import dev.droidshield.domain.Severity
import dev.droidshield.domain.ThreatCategory
import dev.droidshield.domain.ThreatCheck

/**
 * CHECKS_SEED_LIST.md EMULATOR #3. Deliberately avoids reading the IMEI —
 * that requires `READ_PHONE_STATE` and is heavily restricted (throws
 * `SecurityException` on API 29+ for non-privileged apps) for a signal
 * this weak. Instead reads `networkOperatorName`/`line1Number`, which are
 * either unrestricted or degrade to null rather than an exception, and
 * compares against well-known AVD dummy values (classic AVD phone number
 * `15555215554`, operator name `"Android"`).
 */
class TelephonyPropertiesCheck : ThreatCheck {
    override val id: String = "emulator.telephony_properties"
    override val category: ThreatCategory = ThreatCategory.EMULATOR
    override val severity: Severity = Severity.LOW

    override fun evaluate(context: CheckContext): CheckResult {
        val androidContext = (context as AndroidCheckContext).androidContext
        val telephonyManager =
            androidContext.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
                ?: return CheckResult(id, category, severity, detected = false)

        val operatorName = runCatching { telephonyManager.networkOperatorName }.getOrNull()
        val operatorSuspicious = operatorName.equals("android", ignoreCase = true)

        val lineNumberSuspicious = if (hasReadPhoneStatePermission(androidContext)) {
            @Suppress("MissingPermission")
            val line1Number = runCatching { telephonyManager.line1Number }.getOrNull()
            line1Number == "15555215554"
        } else {
            false
        }

        return CheckResult(
            id,
            category,
            severity,
            detected = operatorSuspicious || lineNumberSuspicious,
            detail = "operatorName=$operatorName",
        )
    }

    private fun hasReadPhoneStatePermission(androidContext: android.content.Context): Boolean =
        ContextCompat.checkSelfPermission(androidContext, Manifest.permission.READ_PHONE_STATE) ==
            PackageManager.PERMISSION_GRANTED
}
