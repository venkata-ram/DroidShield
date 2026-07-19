#include <jni.h>
#include <sys/ptrace.h>
#include <signal.h>
#include <csetjmp>
#include <atomic>

// Scaffold health-check — proves the JNI bridge builds and loads. No
// security signal on its own.
extern "C" JNIEXPORT jboolean JNICALL
Java_dev_droidshield_nativelayer_NativeBridge_isLoaded(JNIEnv *, jobject) {
    return JNI_TRUE;
}

// CHECKS_SEED_LIST.md DEBUGGER #6 — ptrace self-attach. If a debugger (or
// a ptrace-based tool like Frida) is already attached to this process,
// PTRACE_TRACEME fails because a process can only be traced by one tracer
// at a time. On success we immediately detach so this check doesn't itself
// leave the process in a traced state for the rest of its life.
extern "C" JNIEXPORT jboolean JNICALL
Java_dev_droidshield_nativelayer_NativeBridge_ptraceSelfAttachDetectsTracer(JNIEnv *, jobject) {
    long result = ptrace(PTRACE_TRACEME, 0, nullptr, nullptr);
    if (result == -1) {
        // Already traced by something else.
        return JNI_TRUE;
    }
    ptrace(PTRACE_DETACH, 0, nullptr, nullptr);
    return JNI_FALSE;
}

// CHECKS_SEED_LIST.md DEBUGGER #8 — native breakpoint / signal handler
// check. Installs a SIGTRAP handler, raises SIGTRAP, and verifies the
// handler actually fired. A ptrace-attached debugger commonly intercepts
// SIGTRAP itself (e.g. to implement breakpoints) before it reaches this
// process's own handler, so a handler that never fires is the anomaly
// this check looks for.
namespace {
std::atomic<bool> g_sigtrap_handler_fired{false};

void sigtrap_handler(int) {
    g_sigtrap_handler_fired.store(true);
}
}  // namespace

extern "C" JNIEXPORT jboolean JNICALL
Java_dev_droidshield_nativelayer_NativeBridge_sigtrapHandlerAnomalyDetected(JNIEnv *, jobject) {
    g_sigtrap_handler_fired.store(false);

    struct sigaction newAction{};
    struct sigaction oldAction{};
    newAction.sa_handler = sigtrap_handler;
    sigemptyset(&newAction.sa_mask);
    newAction.sa_flags = 0;

    if (sigaction(SIGTRAP, &newAction, &oldAction) != 0) {
        // Couldn't install the handler at all — not evidence of tampering,
        // just report "no anomaly" rather than a false positive.
        return JNI_FALSE;
    }

    raise(SIGTRAP);

    bool fired = g_sigtrap_handler_fired.load();
    sigaction(SIGTRAP, &oldAction, nullptr);

    return fired ? JNI_FALSE : JNI_TRUE;
}
