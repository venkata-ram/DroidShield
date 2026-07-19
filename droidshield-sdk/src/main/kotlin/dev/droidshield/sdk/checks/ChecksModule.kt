package dev.droidshield.sdk.checks

import dagger.Module

/** ARCHITECTURE.md §6 — aggregates every category module into one `Set<ThreatCheck>`. */
@Module(
    includes = [
        RootChecksModule::class,
        DebuggerChecksModule::class,
        HookChecksModule::class,
        EmulatorChecksModule::class,
        TamperChecksModule::class,
    ],
)
object ChecksModule
