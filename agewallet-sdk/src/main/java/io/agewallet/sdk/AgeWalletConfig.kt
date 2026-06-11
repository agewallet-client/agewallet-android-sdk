package io.agewallet.sdk

/**
 * Result of an age verification callback.
 */
enum class AgeWalletResult {
    /** Verification completed successfully. */
    SUCCESS,

    /** User denied consent on the AgeWallet screen. */
    DENIED,

    /** Verification process failed (identity check unsuccessful). */
    FAILED
}

/**
 * Configuration for AgeWallet SDK.
 *
 * @param clientId Your client ID from the AgeWallet dashboard
 * @param redirectUri Your app's deep link callback URL
 * @param endpoints Optional custom endpoint configuration
 * @param metadata Optional opaque per-verification string (max 4096 bytes) attached to every verification
 */
data class AgeWalletConfig(
    val clientId: String,
    val redirectUri: String,
    val endpoints: AgeWalletEndpoints = AgeWalletEndpoints(),
    val metadata: String? = null
) {
    init {
        require(clientId.isNotBlank()) { "[AgeWallet] Missing clientId" }
        require(redirectUri.isNotBlank()) { "[AgeWallet] Missing redirectUri" }
        if (metadata != null) {
            require(metadata.toByteArray(Charsets.UTF_8).size <= AgeWallet.METADATA_MAX_BYTES) {
                "[AgeWallet] metadata exceeds ${AgeWallet.METADATA_MAX_BYTES}-byte limit"
            }
        }
    }
}

/**
 * Custom endpoint configuration.
 */
data class AgeWalletEndpoints(
    val auth: String = "https://app.agewallet.io/user/authorize",
    val token: String = "https://app.agewallet.io/user/token",
    val userinfo: String = "https://app.agewallet.io/user/userinfo"
)

/**
 * Stored verification state.
 */
internal data class VerificationState(
    val accessToken: String,
    val expiresAt: Long,
    val isVerified: Boolean,
    val metadata: String? = null
) {
    val isExpired: Boolean
        get() = System.currentTimeMillis() >= expiresAt
}

/**
 * OIDC state stored during authorization flow.
 */
internal data class OidcState(
    val state: String,
    val verifier: String,
    val nonce: String
)
