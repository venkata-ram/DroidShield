package dev.droidshield.sample

import android.app.Activity
import android.os.Bundle
import android.widget.TextView

/**
 * Exists only so the sample app has something for the launcher to point
 * at — DroidShield itself is a headless library with no Activity
 * dependency (DECISIONS.md D015); this Activity is purely a demo-app
 * convenience, not part of the SDK. Check results are logged by
 * [SampleApplication] under the "DroidShieldSample" logcat tag.
 */
class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(
            TextView(this).apply {
                text = "DroidShield sample running.\nSee logcat tag \"DroidShieldSample\" for check results."
                setPadding(48, 96, 48, 48)
            },
        )
    }
}
