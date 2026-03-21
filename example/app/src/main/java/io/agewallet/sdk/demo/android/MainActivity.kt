package io.agewallet.sdk.demo.android

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import io.agewallet.sdk.AgeWallet
import io.agewallet.sdk.AgeWalletConfig
import io.agewallet.sdk.AgeWalletResult
import io.agewallet.sdk.demo.android.ui.theme.AgeWalletDemoTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private lateinit var ageWallet: AgeWallet
    private var isVerified = mutableStateOf(false)
    private var isLoading = mutableStateOf(true)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize AgeWallet SDK
        ageWallet = AgeWallet(
            context = this,
            config = AgeWalletConfig(
                clientId = "239472f9-3398-47ea-ad13-fe9502a0eb33",
                redirectUri = "https://agewallet-sdk-demo.netlify.app/callback"
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
        isLoading.value = false
    }

    private fun startVerification() {
        ageWallet.startVerification(this)
    }

    private fun clearVerification() {
        ageWallet.clearVerification()
        isVerified.value = false
    }
}

@Composable
fun AgeVerificationScreen(
    isVerified: Boolean,
    isLoading: Boolean,
    onVerify: () -> Unit,
    onClear: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        if (isLoading) {
            CircularProgressIndicator()
        } else if (isVerified) {
            VerifiedView(onClear = onClear)
        } else {
            UnverifiedView(onVerify = onVerify)
        }
    }
}

@Composable
fun UnverifiedView(onVerify: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Lock icon
        Box(
            modifier = Modifier
                .size(100.dp)
                .background(
                    color = Color(0xFF6366F1).copy(alpha = 0.1f),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "\uD83D\uDD12",
                fontSize = 40.sp
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "Age Verification Required",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1F2937)
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "You must verify your age to access this content.",
            fontSize = 16.sp,
            color = Color(0xFF6B7280),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onVerify,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF6366F1)
            )
        ) {
            Text(
                text = "Verify with AgeWallet",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
fun VerifiedView(onClear: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Checkmark icon
        Box(
            modifier = Modifier
                .size(100.dp)
                .background(
                    color = Color(0xFF10B981).copy(alpha = 0.1f),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "\u2713",
                fontSize = 48.sp,
                color = Color(0xFF10B981)
            )
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
            fontSize = 16.sp,
            color = Color(0xFF6B7280),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

        OutlinedButton(
            onClick = onClear,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = Color(0xFF6B7280)
            )
        ) {
            Text(
                text = "Clear Verification",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
