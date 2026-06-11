package io.agewallet.sdk.demo.android

import android.app.Activity
import android.content.Intent
import android.os.Bundle

/**
 * Dedicated callback activity for the OIDC redirect URL.
 *
 * Mirrors the Flutter demo's pattern: the netlify intent-filter lives on THIS activity
 * (not MainActivity), so when Android resolves the {@code intent://} URL from the
 * netlify callback page, it routes here instead of bringing MainActivity (which hosts
 * the Chrome Custom Tab) to the front. Without this, MainActivity's singleTask launch
 * mode kills the Custom Tab when the intent fires — which destroys the Custom Tab's
 * session-cookie state mid-flow and causes /magic/complete to bounce back to
 * /user-login.
 *
 * After capturing the URL, this activity re-launches MainActivity with an ACTION_VIEW
 * intent carrying the URL data. MainActivity#onNewIntent picks it up via the existing
 * handleIntent() path and the SDK's handleCallback runs normally.
 */
class AgeWalletCallbackActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val url = intent?.data
        if (url != null) {
            val forward = Intent(Intent.ACTION_VIEW, url, this, MainActivity::class.java).apply {
                addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK
                        or Intent.FLAG_ACTIVITY_CLEAR_TOP
                        or Intent.FLAG_ACTIVITY_SINGLE_TOP
                )
            }
            startActivity(forward)
        }

        finishAndRemoveTask()
    }
}
