package dev.droidshield.nativelayer

/**
 * Scaffold only — proves the JNI bridge builds and loads. See
 * `src/main/cpp/jni_bridge.cpp` and DECISIONS.md D023. Real native checks
 * (ptrace anti-debug, /proc/self/maps scanning) land here in a later pass.
 */
object NativeBridge {
    init {
        System.loadLibrary("droidshield")
    }

    external fun isLoaded(): Boolean
}
