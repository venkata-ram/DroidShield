package dev.droidshield.sdk.checks

import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoSet
import dev.droidshield.data.checks.debugger.DebuggableFlagCheck
import dev.droidshield.data.checks.debugger.DebuggerConnectedCheck
import dev.droidshield.data.checks.debugger.JdwpCommCheck
import dev.droidshield.data.checks.debugger.TimingAnomalyCheck
import dev.droidshield.data.checks.debugger.TracerPidCheck
import dev.droidshield.data.checks.debugger.WaitingForDebuggerCheck
import dev.droidshield.domain.ThreatCheck
import dev.droidshield.nativelayer.checks.PtraceSelfAttachCheck
import dev.droidshield.nativelayer.checks.SignalHandlerAnomalyCheck

/** Includes both Kotlin-layer and native-layer DEBUGGER checks — see DECISIONS.md D002. */
@Module
object DebuggerChecksModule {
    @Provides @IntoSet
    fun providesDebuggerConnectedCheck(): ThreatCheck = DebuggerConnectedCheck()

    @Provides @IntoSet
    fun providesDebuggableFlagCheck(): ThreatCheck = DebuggableFlagCheck()

    @Provides @IntoSet
    fun providesTracerPidCheck(): ThreatCheck = TracerPidCheck()

    @Provides @IntoSet
    fun providesWaitingForDebuggerCheck(): ThreatCheck = WaitingForDebuggerCheck()

    @Provides @IntoSet
    fun providesTimingAnomalyCheck(): ThreatCheck = TimingAnomalyCheck()

    @Provides @IntoSet
    fun providesJdwpCommCheck(): ThreatCheck = JdwpCommCheck()

    @Provides @IntoSet
    fun providesPtraceSelfAttachCheck(): ThreatCheck = PtraceSelfAttachCheck()

    @Provides @IntoSet
    fun providesSignalHandlerAnomalyCheck(): ThreatCheck = SignalHandlerAnomalyCheck()
}
