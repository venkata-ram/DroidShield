package dev.droidshield.gradleplugin

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes

class GuardedMethodClassVisitorTest {
    @Test
    fun `annotated method receives runtime trigger with explicit operation id`() {
        val transformed = transform(testClass(guarded = true, operationId = "checkout.submit"))
        val calls = runtimeCalls(transformed)

        assertThat(calls).containsExactly("checkout.submit")
    }

    @Test
    fun `unannotated method is not modified`() {
        val transformed = transform(testClass(guarded = false))

        assertThat(runtimeCalls(transformed)).isEmpty()
    }

    @Test
    fun `empty operation id falls back to class and JVM signature`() {
        val transformed = transform(testClass(guarded = true, operationId = ""))

        assertThat(runtimeCalls(transformed)).containsExactly("sample/Protected#execute()V")
    }

    private fun transform(original: ByteArray): ByteArray {
        val writer = ClassWriter(0)
        ClassReader(original).accept(GuardedMethodClassVisitor(writer), 0)
        return writer.toByteArray()
    }

    private fun runtimeCalls(bytes: ByteArray): List<String> {
        val calls = mutableListOf<String>()
        ClassReader(bytes).accept(
            object : ClassVisitor(Opcodes.ASM9) {
                override fun visitMethod(
                    access: Int,
                    name: String,
                    descriptor: String,
                    signature: String?,
                    exceptions: Array<out String>?,
                ): MethodVisitor = object : MethodVisitor(Opcodes.ASM9) {
                    private var lastString: String? = null

                    override fun visitLdcInsn(value: Any?) {
                        lastString = value as? String
                    }

                    override fun visitMethodInsn(
                        opcode: Int,
                        owner: String,
                        name: String,
                        descriptor: String,
                        isInterface: Boolean,
                    ) {
                        if (
                            opcode == Opcodes.INVOKESTATIC &&
                            owner == GUARD_RUNTIME_OWNER &&
                            name == "onGuardedMethodEntered"
                        ) {
                            calls += requireNotNull(lastString)
                        }
                    }
                }
            },
            0,
        )
        return calls
    }

    private fun testClass(guarded: Boolean, operationId: String = ""): ByteArray {
        val writer = ClassWriter(0)
        writer.visit(Opcodes.V17, Opcodes.ACC_PUBLIC, "sample/Protected", null, "java/lang/Object", null)
        val method = writer.visitMethod(Opcodes.ACC_PUBLIC, "execute", "()V", null, null)
        if (guarded) {
            method.visitAnnotation(GUARDED_ANNOTATION_DESCRIPTOR, false).apply {
                visit("value", operationId)
                visitEnd()
            }
        }
        method.visitCode()
        method.visitInsn(Opcodes.RETURN)
        method.visitMaxs(0, 1)
        method.visitEnd()
        writer.visitEnd()
        return writer.toByteArray()
    }
}
