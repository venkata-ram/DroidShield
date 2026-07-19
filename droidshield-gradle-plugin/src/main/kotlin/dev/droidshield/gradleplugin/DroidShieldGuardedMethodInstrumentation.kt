package dev.droidshield.gradleplugin

import com.android.build.api.instrumentation.AsmClassVisitorFactory
import com.android.build.api.instrumentation.ClassContext
import com.android.build.api.instrumentation.ClassData
import com.android.build.api.instrumentation.InstrumentationParameters
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.objectweb.asm.AnnotationVisitor
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes

internal const val GUARDED_ANNOTATION_DESCRIPTOR = "Ldev/droidshield/sdk/DroidShieldGuarded;"
internal const val GUARD_RUNTIME_OWNER = "dev/droidshield/sdk/DroidShieldGuardRuntime"

interface DroidShieldInstrumentationParameters : InstrumentationParameters {
    @get:Input
    val enabled: Property<Boolean>
}

/** AGP entry point for project-class instrumentation. Dependency classes are deliberately excluded. */
abstract class DroidShieldGuardedMethodClassVisitorFactory :
    AsmClassVisitorFactory<DroidShieldInstrumentationParameters> {

    override fun isInstrumentable(classData: ClassData): Boolean = parameters.get().enabled.get()

    override fun createClassVisitor(classContext: ClassContext, nextClassVisitor: ClassVisitor): ClassVisitor =
        GuardedMethodClassVisitor(nextClassVisitor)
}

internal class GuardedMethodClassVisitor(nextClassVisitor: ClassVisitor) :
    ClassVisitor(Opcodes.ASM9, nextClassVisitor) {

    private lateinit var owner: String

    override fun visit(
        version: Int,
        access: Int,
        name: String,
        signature: String?,
        superName: String?,
        interfaces: Array<out String>?,
    ) {
        owner = name
        super.visit(version, access, name, signature, superName, interfaces)
    }

    override fun visitMethod(
        access: Int,
        name: String,
        descriptor: String,
        signature: String?,
        exceptions: Array<out String>?,
    ): MethodVisitor {
        val delegate = super.visitMethod(access, name, descriptor, signature, exceptions)
        return GuardedMethodVisitor(delegate, "$owner#$name$descriptor")
    }
}

private class GuardedMethodVisitor(
    delegate: MethodVisitor,
    private val defaultOperationId: String,
) : MethodVisitor(Opcodes.ASM9, delegate) {

    private var guarded = false
    private var operationId = defaultOperationId

    override fun visitAnnotation(descriptor: String, visible: Boolean): AnnotationVisitor? {
        val delegate = super.visitAnnotation(descriptor, visible)
        if (descriptor != GUARDED_ANNOTATION_DESCRIPTOR) return delegate

        guarded = true
        return object : AnnotationVisitor(Opcodes.ASM9, delegate) {
            override fun visit(name: String?, value: Any?) {
                if (name == "value" && value is String && value.isNotBlank()) {
                    operationId = value
                }
                super.visit(name, value)
            }
        }
    }

    override fun visitCode() {
        super.visitCode()
        if (!guarded) return

        visitLdcInsn(operationId)
        visitMethodInsn(
            Opcodes.INVOKESTATIC,
            GUARD_RUNTIME_OWNER,
            "onGuardedMethodEntered",
            "(Ljava/lang/String;)V",
            false,
        )
    }
}
