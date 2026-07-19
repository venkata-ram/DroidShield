package dev.droidshield.sdk.checks

import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoSet
import dev.droidshield.data.checks.hook.FridaNamedPipeCheck
import dev.droidshield.data.checks.hook.FridaPortCheck
import dev.droidshield.data.checks.hook.LsposedZygiskCheck
import dev.droidshield.data.checks.hook.ProcMapsSignatureCheck
import dev.droidshield.data.checks.hook.SuspiciousLibraryCheck
import dev.droidshield.data.checks.hook.XposedArtifactCheck
import dev.droidshield.domain.ThreatCheck
import dev.droidshield.nativelayer.checks.NativeCodeChecksumCheck
import dev.droidshield.nativelayer.checks.TrampolineHookCheck

/** Includes both Kotlin-layer and native-layer HOOK checks — see DECISIONS.md D002. */
@Module
object HookChecksModule {
    @Provides @IntoSet
    fun providesProcMapsSignatureCheck(): ThreatCheck = ProcMapsSignatureCheck()

    @Provides @IntoSet
    fun providesFridaPortCheck(): ThreatCheck = FridaPortCheck()

    @Provides @IntoSet
    fun providesFridaNamedPipeCheck(): ThreatCheck = FridaNamedPipeCheck()

    @Provides @IntoSet
    fun providesSuspiciousLibraryCheck(): ThreatCheck = SuspiciousLibraryCheck()

    @Provides @IntoSet
    fun providesXposedArtifactCheck(): ThreatCheck = XposedArtifactCheck()

    @Provides @IntoSet
    fun providesLsposedZygiskCheck(): ThreatCheck = LsposedZygiskCheck()

    @Provides @IntoSet
    fun providesNativeCodeChecksumCheck(): ThreatCheck = NativeCodeChecksumCheck()

    @Provides @IntoSet
    fun providesTrampolineHookCheck(): ThreatCheck = TrampolineHookCheck()
}
