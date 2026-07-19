package dev.droidshield.domain.backend

import dev.droidshield.domain.CheckResult

/**
 * Transport-neutral request body for a backend risk-decision endpoint.
 *
 * The models in this package intentionally have no Retrofit, Gson, Moshi, or
 * kotlinx-serialization dependency. A host app can use the converter it
 * already uses and post this object directly as its Retrofit `@Body`.
 *
 * [detail][CheckResult.detail] is deliberately not included: check details can
 * contain local paths or process names and should not leave the device by
 * default.
 */
data class DeviceEvidence(
    val schemaVersion: Int = CURRENT_SCHEMA_VERSION,
    val sdkVersion: String,
    val appPackageName: String,
    val appVersionName: String?,
    val appVersionCode: Long,
    val androidSdk: Int,
    val collectedAtEpochMillis: Long,
    val checksRun: Int,
    val triggeredChecks: List<TriggeredCheck>,
    /** Opaque identifiers supplied by the host app; DroidShield never creates or persists them. */
    val installationId: String? = null,
    val sessionId: String? = null,
    /** A server-issued, single-use value is recommended when replay protection is required. */
    val nonce: String? = null,
) {
    companion object {
        const val CURRENT_SCHEMA_VERSION: Int = 1
    }
}

/** A detected signal in the stable, language-neutral backend payload. */
data class TriggeredCheck(
    val checkId: String,
    val category: String,
    val severity: String,
)

/**
 * Optional per-request values owned by the integrating app.
 *
 * Authentication belongs in normal HTTP headers; do not put access tokens in
 * this object. The SDK simply copies these opaque values into [DeviceEvidence].
 */
data class EvidenceContext(
    val installationId: String? = null,
    val sessionId: String? = null,
    val nonce: String? = null,
    val collectedAtEpochMillis: Long = System.currentTimeMillis(),
)

/** Values a backend may return when deciding how a protected operation proceeds. */
enum class RiskVerdict {
    ALLOW,
    MONITOR,
    STEP_UP,
    LIMIT,
    BLOCK,
}

/** Transport-neutral response body for a backend risk-decision endpoint. */
data class RiskDecision(
    val verdict: RiskVerdict,
    /** Opaque support/audit reference. The backend should not expose its rule reasoning. */
    val referenceId: String,
    /** Optional expiry after which the app should request a fresh decision. */
    val expiresAtEpochMillis: Long? = null,
)

/**
 * Converts raw check results into the safe subset intended for a backend.
 * Public for apps that run checks separately and want to build evidence later.
 */
fun List<CheckResult>.toDeviceEvidence(
    sdkVersion: String,
    appPackageName: String,
    appVersionName: String?,
    appVersionCode: Long,
    androidSdk: Int,
    context: EvidenceContext = EvidenceContext(),
): DeviceEvidence = DeviceEvidence(
    sdkVersion = sdkVersion,
    appPackageName = appPackageName,
    appVersionName = appVersionName,
    appVersionCode = appVersionCode,
    androidSdk = androidSdk,
    collectedAtEpochMillis = context.collectedAtEpochMillis,
    checksRun = size,
    triggeredChecks = asSequence()
        .filter(CheckResult::detected)
        .map { result ->
            TriggeredCheck(
                checkId = result.checkId,
                category = result.category.name,
                severity = result.severity.name,
            )
        }
        .sortedBy(TriggeredCheck::checkId)
        .toList(),
    installationId = context.installationId,
    sessionId = context.sessionId,
    nonce = context.nonce,
)
