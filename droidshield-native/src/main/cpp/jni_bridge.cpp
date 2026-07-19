#include <jni.h>

// Scaffold only — proves the JNI bridge builds and loads. No real checks
// yet (ptrace anti-debug, /proc/self/maps scanning, etc. — see
// CHECKS_SEED_LIST.md and DECISIONS.md D023). Do not treat this as a
// security signal.
extern "C" JNIEXPORT jboolean JNICALL
Java_dev_droidshield_nativelayer_NativeBridge_isLoaded(JNIEnv *, jobject) {
    return JNI_TRUE;
}
