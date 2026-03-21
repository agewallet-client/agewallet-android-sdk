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
 */
data class AgeWalletConfig(
    val clientId: String,
    val redirectUri: String,
    val endpoints: AgeWalletEndpoints = AgeWalletEndpoints()
) {
    init {
        require(clientId.isNotBlank()) { "[AgeWallet] Missing clientId" }
        require(redirectUri.isNotBlank()) { "[AgeWallet] Missing redirectUri" }
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
    val isVerified: Boolean
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
