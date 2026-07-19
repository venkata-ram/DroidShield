#include <jni.h>
#include <sys/ptrace.h>
#include <sys/wait.h>
#include <unistd.h>
#include <signal.h>
#include <csetjmp>
#include <atomic>
#include <link.h>
#include <dlfcn.h>
#include <cstdint>
#include <cstdio>
#include <string>
#include <algorithm>

// Scaffold health-check — proves the JNI bridge builds and loads. No
// security signal on its own.
extern "C" JNIEXPORT jboolean JNICALL
Java_dev_droidshield_nativelayer_NativeBridge_isLoaded(JNIEnv *, jobject) {
    return JNI_TRUE;
}

// CHECKS_SEED_LIST.md DEBUGGER #6 — ptrace self-attach.
//
// This previously ran PTRACE_TRACEME in the app process itself and then
// tried to undo it with ptrace(PTRACE_DETACH, 0, ...). That detach never
// worked: PTRACE_DETACH must be issued *by the tracer*, against the
// tracee's pid — a tracee cannot untrace itself, and pid 0 is not a valid
// target. So on every clean device the check left the app permanently
// traced by its parent (zygote). A traced process stops on the next
// signal delivery and stays stopped until its tracer reaps it, and zygote
// never will, so the app could freeze outright; it also blocked any
// later legitimate debugger attach for the life of the process.
//
// The detection is now done from a short-lived forked child instead, so
// the app process is never put into a traced state at all. If the child
// can PTRACE_ATTACH to its parent, nothing else holds the parent's single
// tracer slot; if it cannot, we corroborate via TracerPid before calling
// it a detection, because an attach can also be refused for reasons that
// have nothing to do with a debugger (Yama ptrace_scope=1 denies tracing
// a non-descendant, which is exactly the child->parent direction here).
namespace {
// Reads the TracerPid field of /proc/<pid>/status. Returns -1 if it can't
// be determined, 0 when untraced, otherwise the tracer's pid.
int read_tracer_pid(pid_t pid) {
    char path[64];
    snprintf(path, sizeof(path), "/proc/%d/status", pid);
    FILE *status = fopen(path, "r");
    if (status == nullptr) {
        return -1;
    }

    int tracerPid = -1;
    char line[256];
    while (fgets(line, sizeof(line), status) != nullptr) {
        if (sscanf(line, "TracerPid: %d", &tracerPid) == 1) {
            break;
        }
    }
    fclose(status);
    return tracerPid;
}
}  // namespace

extern "C" JNIEXPORT jboolean JNICALL
Java_dev_droidshield_nativelayer_NativeBridge_ptraceSelfAttachDetectsTracer(JNIEnv *, jobject) {
    const pid_t parent = getpid();

    int pipeFds[2];
    if (pipe(pipeFds) != 0) {
        return JNI_FALSE;
    }

    const pid_t child = fork();
    if (child < 0) {
        close(pipeFds[0]);
        close(pipeFds[1]);
        return JNI_FALSE;
    }

    if (child == 0) {
        // Child: async-signal-safe work only, then _exit (never exit(),
        // which would run the parent's atexit handlers in this copy).
        close(pipeFds[0]);

        char traced = 0;
        if (ptrace(PTRACE_ATTACH, parent, nullptr, nullptr) == -1) {
            // Attach refused. Only a confirmed non-zero TracerPid that
            // isn't us counts as a real tracer; anything else (Yama,
            // SELinux, unreadable /proc) is inconclusive, not a threat.
            const int tracerPid = read_tracer_pid(parent);
            traced = (tracerPid > 0 && tracerPid != getpid()) ? 1 : 0;
        } else {
            // We now own the parent's tracer slot; hand it straight back.
            waitpid(parent, nullptr, 0);
            ptrace(PTRACE_DETACH, parent, nullptr, nullptr);
        }

        ssize_t ignored = write(pipeFds[1], &traced, 1);
        (void) ignored;
        close(pipeFds[1]);
        _exit(0);
    }

    close(pipeFds[1]);
    char result = 0;
    const ssize_t bytesRead = read(pipeFds[0], &result, 1);
    close(pipeFds[0]);
    waitpid(child, nullptr, 0);

    if (bytesRead != 1) {
        return JNI_FALSE;
    }
    return result ? JNI_TRUE : JNI_FALSE;
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

// CHECKS_SEED_LIST.md HOOK #5 — native code checksum/integrity comparison.
// Frida rewrites bytes of the executable segment in memory on attach
// without touching the .so file on disk, so a mismatch between an
// executable segment's on-disk bytes and its in-memory bytes is a runtime-
// tampering signal. We use dl_iterate_phdr to find this library's own
// first executable PT_LOAD segment and its real file offset/size, rather
// than guessing an offset — this guarantees the memory range we read is
// actually mapped and executable (it's the segment dlopen mapped code
// from), instead of risking an out-of-bounds read.
namespace {
struct ExecSegmentInfo {
    bool found = false;
    uintptr_t base = 0;
    std::string path;
    ElfW(Off) fileOffset = 0;
    ElfW(Addr) vaddr = 0;
    ElfW(Xword) fileSize = 0;
};

int find_exec_segment(struct dl_phdr_info *info, size_t, void *data) {
    auto *out = static_cast<ExecSegmentInfo *>(data);
    std::string name(info->dlpi_name ? info->dlpi_name : "");
    if (name.find("libdroidshield.so") == std::string::npos) {
        return 0;
    }

    for (int i = 0; i < info->dlpi_phnum; i++) {
        const ElfW(Phdr) &phdr = info->dlpi_phdr[i];
        if (phdr.p_type == PT_LOAD && (phdr.p_flags & PF_X) != 0) {
            out->found = true;
            out->base = info->dlpi_addr;
            out->path = name;
            out->fileOffset = phdr.p_offset;
            out->vaddr = phdr.p_vaddr;
            out->fileSize = phdr.p_filesz;
            return 1;  // stop iterating
        }
    }
    return 0;
}

// FNV-1a — small, dependency-free, sufficient to detect byte-level
// mismatch; not used for any cryptographic purpose.
uint32_t fnv1a(const uint8_t *data, size_t len) {
    uint32_t hash = 0x811c9dc5u;
    for (size_t i = 0; i < len; i++) {
        hash ^= data[i];
        hash *= 0x01000193u;
    }
    return hash;
}
}  // namespace

extern "C" JNIEXPORT jboolean JNICALL
Java_dev_droidshield_nativelayer_NativeBridge_nativeCodeChecksumMismatch(JNIEnv *, jobject) {
    ExecSegmentInfo segment;
    dl_iterate_phdr(find_exec_segment, &segment);
    if (!segment.found || segment.path.empty() || segment.fileSize == 0) {
        // Couldn't locate our own segment — report "no anomaly" rather
        // than a false positive from an inconclusive lookup.
        return JNI_FALSE;
    }

    // Cap the sample so this stays a cheap, bounded read on both sides.
    const size_t sampleSize = std::min<ElfW(Xword)>(segment.fileSize, 4096);

    FILE *file = fopen(segment.path.c_str(), "rb");
    if (file == nullptr) {
        return JNI_FALSE;
    }
    std::string onDisk(sampleSize, '\0');
    fseek(file, static_cast<long>(segment.fileOffset), SEEK_SET);
    size_t read = fread(onDisk.data(), 1, sampleSize, file);
    fclose(file);
    if (read != sampleSize) {
        return JNI_FALSE;
    }

    const auto *inMemory = reinterpret_cast<const uint8_t *>(segment.base + segment.vaddr);

    uint32_t diskChecksum = fnv1a(reinterpret_cast<const uint8_t *>(onDisk.data()), sampleSize);
    uint32_t memChecksum = fnv1a(inMemory, sampleSize);

    return diskChecksum != memChecksum ? JNI_TRUE : JNI_FALSE;
}

// CHECKS_SEED_LIST.md HOOK #6 — trampoline / inline-hook detection.
// Heuristic, arm64-only (droidshield's production ABIs per DECISIONS.md
// D013): a normal function prologue almost always opens with a stack-frame
// setup instruction (STP/SUB family). Many inline-hooking frameworks,
// including Frida's arm64 Interceptor, instead overwrite the first
// instruction(s) with an absolute-address load-and-branch trampoline
// (LDR literal into a scratch register, encoding with top byte 0x58/0x18,
// followed by BR). This is a heuristic, not a certainty — legitimate
// compiler output essentially never starts this way, but a determined
// attacker can craft a hook that mimics a normal prologue. Combine with
// other HOOK checks, per the seed list's own framing.
extern "C" JNIEXPORT jboolean JNICALL
Java_dev_droidshield_nativelayer_NativeBridge_trampolineHookDetected(JNIEnv *, jobject) {
#if defined(__aarch64__)
    void *openSymbol = dlsym(RTLD_DEFAULT, "open");
    if (openSymbol == nullptr) {
        return JNI_FALSE;
    }

    auto firstInstruction = *reinterpret_cast<const uint32_t *>(openSymbol);
    uint8_t topByte = static_cast<uint8_t>(firstInstruction >> 24);
    bool looksLikeLdrLiteralTrampoline = (topByte == 0x58) || (topByte == 0x18);

    return looksLikeLdrLiteralTrampoline ? JNI_TRUE : JNI_FALSE;
#else
    return JNI_FALSE;
#endif
}
