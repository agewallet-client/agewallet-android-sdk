package io.agewallet.sdk

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.browser.customtabs.CustomTabsIntent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * AgeWallet SDK for Android applications.
 *
 * Provides age verification via OIDC/PKCE flow.
 *
 * Example usage:
 * ```kotlin
 * val ageWallet = AgeWallet(context, AgeWalletConfig(
 *     clientId = "your-client-id",
 *     redirectUri = "https://yourapp.com/callback"
 * ))
 *
 * if (!ageWallet.isVerified()) {
 *     ageWallet.startVerification(activity)
 * }
 * ```
 */
class AgeWallet(
    context: Context,
    private val config: AgeWalletConfig
) {
    companion object {
        private const val TAG = "AgeWallet"
    }

    private val storage = Storage(context.applicationContext)

    /**
     * Check if the user is currently verified.
     * Returns true if verified and not expired, false otherwise.
     */
    fun isVerified(): Boolean {
        return storage.getVerification()?.isVerified == true
    }

    /**
     * Start the verification flow.
     * Opens Chrome Custom Tabs to the AgeWallet authorization page.
     *
     * @param context Activity context for launching the browser
     */
    fun startVerification(context: Context) {
        // Generate PKCE parameters
        val verifier = Security.generateVerifier()
        val challenge = Security.generateChallenge(verifier)
        val state = Security.generateState()
        val nonce = Security.generateNonce()

        // Store OIDC state for callback validation
        storage.setOidcState(OidcState(state, verifier, nonce))

        // Build authorization URL
        val authUrl = buildAuthUrl(challenge, state, nonce)

        // Open Chrome Custom Tabs
        val customTabsIntent = CustomTabsIntent.Builder()
            .setShowTitle(true)
            .build()

        customTabsIntent.launchUrl(context, Uri.parse(authUrl))
    }

    /**
     * Handle callback intent from deep link.
     * Call this from your activity's onCreate or onNewIntent.
     *
     * @param intent The intent containing the callback URL
     * @return AgeWalletResult indicating the outcome
     */
    suspend fun handleCallback(intent: Intent): AgeWalletResult {
        val uri = intent.data ?: return AgeWalletResult.FAILED

        // Check if this is our callback
        if (!uri.toString().startsWith(config.redirectUri)) {
            return AgeWalletResult.FAILED
        }

        val code = uri.getQueryParameter("code")
        val state = uri.getQueryParameter("state")
        val error = uri.getQueryParameter("error")
        val errorDescription = uri.getQueryParameter("error_description")

        // Handle error response
        if (error != null) {
            Log.e(TAG, "Authorization error: $error - $errorDescription")
            storage.clearOidcState()
            return if (errorDescription == "The user denied the request") AgeWalletResult.DENIED else AgeWalletResult.FAILED
        }

        // Validate required parameters
        if (code == null || state == null) {
            Log.e(TAG, "Missing code or state in callback")
            storage.clearOidcState()
            return AgeWalletResult.FAILED
        }

        // Validate state matches stored state
        val storedOidc = storage.getOidcState()
        if (storedOidc == null || storedOidc.state != state) {
            Log.e(TAG, "Invalid state or session expired")
            storage.clearOidcState()
            return AgeWalletResult.FAILED
        }

        return try {
            // Exchange code for tokens
            val tokenResponse = exchangeCode(code, storedOidc.verifier)
            if (tokenResponse == null) {
                storage.clearOidcState()
                return AgeWalletResult.FAILED
            }

            // Fetch user info to verify age claim
            val userInfo = fetchUserInfo(tokenResponse.accessToken)
            if (userInfo == null) {
                storage.clearOidcState()
                return AgeWalletResult.FAILED
            }

            // Check age_verified claim
            if (!userInfo.ageVerified) {
                Log.e(TAG, "Age verification failed")
                storage.clearOidcState()
                return AgeWalletResult.FAILED
            }

            // Store verification state
            storage.setVerification(
                VerificationState(
                    accessToken = tokenResponse.accessToken,
                    expiresAt = System.currentTimeMillis() + (tokenResponse.expiresIn * 1000),
                    isVerified = true
                )
            )

            storage.clearOidcState()
            AgeWalletResult.SUCCESS
        } catch (e: Exception) {
            Log.e(TAG, "Error during token exchange", e)
            storage.clearOidcState()
            AgeWalletResult.FAILED
        }
    }

    /**
     * Handle callback URL string directly.
     */
    suspend fun handleCallback(url: String): AgeWalletResult {
        val intent = Intent().apply {
            data = Uri.parse(url)
        }
        return handleCallback(intent)
    }

    /**
     * Clear all verification state (logout).
     */
    fun clearVerification() {
        storage.clearVerification()
        storage.clearOidcState()
    }

    private fun buildAuthUrl(challenge: String, state: String, nonce: String): String {
        val params = mapOf(
            "response_type" to "code",
            "client_id" to config.clientId,
            "redirect_uri" to config.redirectUri,
            "scope" to "openid age",
            "state" to state,
            "code_challenge" to challenge,
            "code_challenge_method" to "S256",
            "nonce" to nonce
        )

        val queryString = params.entries.joinToString("&") { (key, value) ->
            "${URLEncoder.encode(key, "UTF-8")}=${URLEncoder.encode(value, "UTF-8")}"
        }

        return "${config.endpoints.auth}?$queryString"
    }

    private suspend fun exchangeCode(code: String, verifier: String): TokenResponse? =
        withContext(Dispatchers.IO) {
            try {
                val url = URL(config.endpoints.token)
                val connection = url.openConnection() as HttpURLConnection

                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
                connection.doOutput = true

                val params = mapOf(
                    "grant_type" to "authorization_code",
                    "client_id" to config.clientId,
                    "redirect_uri" to config.redirectUri,
                    "code" to code,
                    "code_verifier" to verifier
                )

                val body = params.entries.joinToString("&") { (key, value) ->
                    "${URLEncoder.encode(key, "UTF-8")}=${URLEncoder.encode(value, "UTF-8")}"
                }

                OutputStreamWriter(connection.outputStream).use { writer ->
                    writer.write(body)
                }

                if (connection.responseCode != 200) {
                    Log.e(TAG, "Token exchange failed: ${connection.responseCode}")
                    return@withContext null
                }

                val response = connection.inputStream.bufferedReader().use(BufferedReader::readText)
                val json = JSONObject(response)

                TokenResponse(
                    accessToken = json.getString("access_token"),
                    expiresIn = json.optInt("expires_in", 3600)
                )
            } catch (e: Exception) {
                Log.e(TAG, "Token exchange error", e)
                null
            }
        }

    private suspend fun fetchUserInfo(accessToken: String): UserInfo? =
        withContext(Dispatchers.IO) {
            try {
                val url = URL(config.endpoints.userinfo)
                val connection = url.openConnection() as HttpURLConnection

                connection.setRequestProperty("Authorization", "Bearer $accessToken")

                if (connection.responseCode != 200) {
                    Log.e(TAG, "UserInfo fetch failed: ${connection.responseCode}")
                    return@withContext null
                }

                val response = connection.inputStream.bufferedReader().use(BufferedReader::readText)
                val json = JSONObject(response)

                UserInfo(
                    ageVerified = json.optBoolean("age_verified", false)
                )
            } catch (e: Exception) {
                Log.e(TAG, "UserInfo fetch error", e)
                null
            }
        }

    private data class TokenResponse(
        val accessToken: String,
        val expiresIn: Int
    )

    private data class UserInfo(
        val ageVerified: Boolean
    )
}
