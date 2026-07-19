// Pure Kotlin JVM module — no Android dependency, by design.
// See ARCHITECTURE.md §2/§3: this is what makes the ThreatCheck contract
// testable on the JVM without an emulator, and the module every new
// contributor reads first.
plugins {
    alias(libs.plugins.kotlin.jvm)
}

// Pinned to 17, not the ambient JDK, so the build is reproducible across
// contributor machines regardless of what `java -version` resolves to
// locally (AGP 8.7.x also requires JDK 17+ to run, so this matches the
// floor the rest of the project needs anyway). See DECISIONS.md D017.
kotlin {
    jvmToolchain(17)
}

dependencies {
    testImplementation(libs.junit)
    testImplementation(libs.truth)
}
