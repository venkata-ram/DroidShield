package dev.droidshield.data.checks.emulator

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorManager
import dev.droidshield.data.AndroidCheckContext
import dev.droidshield.domain.CheckContext
import dev.droidshield.domain.CheckResult
import dev.droidshield.domain.Severity
import dev.droidshield.domain.ThreatCategory
import dev.droidshield.domain.ThreatCheck

/** CHECKS_SEED_LIST.md EMULATOR #4. */
class SensorAbsenceCheck : ThreatCheck {
    override val id: String = "emulator.sensor_absence"
    override val category: ThreatCategory = ThreatCategory.EMULATOR
    override val severity: Severity = Severity.LOW

    private val requiredSensorTypes = listOf(Sensor.TYPE_ACCELEROMETER, Sensor.TYPE_GYROSCOPE, Sensor.TYPE_PROXIMITY)

    override fun evaluate(context: CheckContext): CheckResult {
        val androidContext = (context as AndroidCheckContext).androidContext
        val sensorManager = androidContext.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
            ?: return CheckResult(id, category, severity, detected = false)

        val missingCount = requiredSensorTypes.count { sensorManager.getDefaultSensor(it) == null }
        val detected = missingCount >= 2

        return CheckResult(id, category, severity, detected, detail = "missingSensors=$missingCount")
    }
}
