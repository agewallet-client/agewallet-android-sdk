package io.agewallet.sdk.demo.android

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import io.agewallet.sdk.AgeWallet
import io.agewallet.sdk.AgeWalletConfig
import io.agewallet.sdk.AgeWalletEndpoints
import io.agewallet.sdk.AgeWalletResult
import io.agewallet.sdk.demo.android.ui.theme.AgeWalletDemoTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    companion object {
        // Auto-detected default metadata identifies the demo build to the dev/QA team
        // when verifications land on the server. Single source of truth for the label.
        private const val AUTO_DEFAULT_METADATA = "Android Native"
    }

    private lateinit var ageWallet: AgeWallet
    private var isVerified = mutableStateOf(false)
    private var isLoading = mutableStateOf(true)
    private var customAppend = mutableStateOf("")
    private var lastMetadata = mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize AgeWallet SDK using build-time-injected config (gitignored local.properties).
        ageWallet = AgeWallet(
            context = this,
            config = AgeWalletConfig(
                clientId = BuildConfig.AGEWALLET_CLIENT_ID,
                redirectUri = "https://agewallet-sdk-demo.netlify.app/callback",
                endpoints = AgeWalletEndpoints(
                    auth = BuildConfig.AGEWALLET_AUTH,
                    token = BuildConfig.AGEWALLET_TOKEN,
                    userinfo = BuildConfig.AGEWALLET_USERINFO
                ),
                metadata = AUTO_DEFAULT_METADATA
            )
        )

        // Check initial verification status
        checkVerification()

        // Handle deep link if app was opened via callback
        intent?.let { handleIntent(it) }

        setContent {
            AgeWalletDemoTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AgeVerificationScreen(
                        isVerified = isVerified.value,
                        isLoading = isLoading.value,
                        autoDefault = AUTO_DEFAULT_METADATA,
                        customAppend = customAppend.value,
                        lastMetadata = lastMetadata.value,
                        onCustomAppendChange = { customAppend.value = it },
                        onVerify = { startVerification() },
                        onClear = { clearVerification() }
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent) {
        intent.data?.let { uri ->
            if (uri.toString().startsWith("https://agewallet-sdk-demo.netlify.app/callback")) {
                isLoading.value = true
                lifecycleScope.launch {
                    val result = ageWallet.handleCallback(intent)
                    isVerified.value = result == AgeWalletResult.SUCCESS
                    isLoading.value = false
                    lastMetadata.value = ageWallet.getMetadata()
                    when (result) {
                        AgeWalletResult.DENIED -> Toast.makeText(
                            this@MainActivity,
                            "Age verification was cancelled.",
                            Toast.LENGTH_LONG
                        ).show()
                        AgeWalletResult.FAILED -> Toast.makeText(
                            this@MainActivity,
                            "Verification could not be completed. Please try again.",
                            Toast.LENGTH_LONG
                        ).show()
                        else -> Unit
                    }
                }
            }
        }
    }

    private fun checkVerification() {
        isVerified.value = ageWallet.isVerified()
        if (isVerified.value) {
            lastMetadata.value = ageWallet.getMetadata()
        }
        isLoading.value = false
    }

    private fun startVerification() {
        try {
            // If the user typed something in the "Custom metadata" field, append it to the auto-default
            // for THIS verification only (without changing the instance default). Otherwise the
            // instance default kicks in.
            val custom = customAppend.value.trim().takeIf { it.isNotEmpty() }
            val perCall = if (custom != null) "$AUTO_DEFAULT_METADATA | $custom" else null
            ageWallet.startVerification(this, metadata = perCall)
        } catch (e: IllegalArgumentException) {
            Toast.makeText(this, e.message ?: "Metadata too long", Toast.LENGTH_LONG).show()
        }
    }

    private fun clearVerification() {
        ageWallet.clearVerification()
        isVerified.value = false
        lastMetadata.value = null
    }
}

@Composable
fun AgeVerificationScreen(
    isVerified: Boolean,
    isLoading: Boolean,
    autoDefault: String,
    customAppend: String,
    lastMetadata: String?,
    onCustomAppendChange: (String) -> Unit,
    onVerify: () -> Unit,
    onClear: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        if (isLoading) {
            CircularProgressIndicator()
        } else if (isVerified) {
            VerifiedView(lastMetadata = lastMetadata, onClear = onClear)
        } else {
            UnverifiedView(
                autoDefault = autoDefault,
                customAppend = customAppend,
                onCustomAppendChange = onCustomAppendChange,
                onVerify = onVerify
            )
        }
    }
}

@Composable
fun UnverifiedView(
    autoDefault: String,
    customAppend: String,
    onCustomAppendChange: (String) -> Unit,
    onVerify: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .background(
                    color = Color(0xFF6366F1).copy(alpha = 0.1f),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(text = "🔒", fontSize = 36.sp)
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Age Verification Required",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1F2937)
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "You must verify your age to access this content.",
            fontSize = 14.sp,
            color = Color(0xFF6B7280),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(28.dp))

        // Auto-detected default metadata — informational, baked in at build time.
        Text(
            text = "Default metadata (auto)",
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF374151),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = autoDefault,
            fontSize = 14.sp,
            fontFamily = FontFamily.Monospace,
            color = Color(0xFF1F2937),
            modifier = Modifier
                .fillMaxWidth()
                .background(color = Color(0xFFF3F4F6), shape = RoundedCornerShape(8.dp))
                .padding(12.dp)
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Optional per-call append — concatenated to the default for THIS verification only.
        Text(
            text = "Custom metadata (optional, appended for this call only)",
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF374151),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(4.dp))
        OutlinedTextField(
            value = customAppend,
            onValueChange = onCustomAppendChange,
            placeholder = { Text("e.g. order-1234") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "If populated, sent as \"$autoDefault | <your text>\" for this verification only.",
            fontSize = 11.sp,
            color = Color(0xFF9CA3AF)
        )

        Spacer(modifier = Modifier.height(28.dp))

        Button(
            onClick = onVerify,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1))
        ) {
            Text(
                text = "Verify with AgeWallet",
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
fun VerifiedView(lastMetadata: String?, onClear: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .size(100.dp)
                .background(
                    color = Color(0xFF10B981).copy(alpha = 0.1f),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(text = "✓", fontSize = 48.sp, color = Color(0xFF10B981))
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "Age Verified",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1F2937)
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "You have successfully verified your age.",
            fontSize = 14.sp,
            color = Color(0xFF6B7280),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Metadata display
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = Color(0xFFF3F4F6),
                    shape = RoundedCornerShape(8.dp)
                )
                .padding(12.dp)
        ) {
            Text(
                text = "Metadata attached to current verification:",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF6B7280)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = lastMetadata ?: "(none)",
                fontSize = 13.sp,
                fontFamily = FontFamily.Monospace,
                color = Color(0xFF1F2937)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        OutlinedButton(
            onClick = onClear,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF6B7280))
        ) {
            Text(text = "Clear Verification", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}
