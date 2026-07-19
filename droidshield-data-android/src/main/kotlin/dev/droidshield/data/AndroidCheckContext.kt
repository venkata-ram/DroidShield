package dev.droidshield.data

import android.content.Context
import dev.droidshield.domain.CheckContext

/**
 * Concrete, Android-backed [CheckContext]. See DECISIONS.md D018 for why
 * this wrapper lives here and not in `droidshield-domain`.
 */
class AndroidCheckContext(val androidContext: Context) : CheckContext
