package dev.droidshield.nativelayer

/**
 * JNI bridge to `droidshield-native/src/main/cpp/jni_bridge.cpp`. Kept as a
 * thin `external fun` surface — the actual [dev.droidshield.domain.ThreatCheck]
 * wrapper classes live alongside this object, not inside the native code
 * itself, so the C++ side stays free of the domain contract.
 */
object NativeBridge {
    init {
        System.loadLibrary("droidshield")
    }

    external fun isLoaded(): Boolean

    /** CHECKS_SEED_LIST.md DEBUGGER #6. */
    external fun ptraceSelfAttachDetectsTracer(): Boolean

    /** CHECKS_SEED_LIST.md DEBUGGER #8. */
    external fun sigtrapHandlerAnomalyDetected(): Boolean

    /** CHECKS_SEED_LIST.md HOOK #5. */
    external fun nativeCodeChecksumMismatch(): Boolean

    /** CHECKS_SEED_LIST.md HOOK #6. arm64 only — see jni_bridge.cpp. */
    external fun trampolineHookDetected(): Boolean
}
