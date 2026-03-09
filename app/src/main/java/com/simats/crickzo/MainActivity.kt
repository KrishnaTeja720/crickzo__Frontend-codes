package com.simats.crickzo

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.work.*
import com.simats.crickzo.ui.theme.CrickzoTheme
import java.util.*
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {
    
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            scheduleScoreUpdate()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            scheduleScoreUpdate()
        }

        setContent {
            CrickzoTheme {
                var currentScreen by remember { mutableStateOf("splash") }
                var userEmail by remember { mutableStateOf("cricket.fan@email.com") }
                var userName by remember { mutableStateOf("Cricket Fan") }
                var otpSubtitle by remember { mutableStateOf("Complete Your Registration") }

                when (currentScreen) {
                    "splash" -> {
                        MainSplashScreen(
                            onFinished = { currentScreen = "login" }
                        )
                    }
                    "login" -> {
                        LoginScreen(
                            onSignUpClick = { currentScreen = "signup" },
                            onForgotPasswordClick = { currentScreen = "forgot_password" },
                            onLoginClick = { currentScreen = "home" }
                        )
                    }
                    "signup" -> {
                        SignUpScreen(
                            onBackToLogin = { currentScreen = "login" },
                            onCreateAccount = { email, name ->
                                userEmail = email
                                userName = name
                                otpSubtitle = "Complete Your Registration"
                                currentScreen = "verify_otp"
                            }
                        )
                    }
                    "forgot_password" -> {
                        ForgotPasswordScreen(
                            onBackToLogin = { currentScreen = "login" },
                            onSendCode = { email ->
                                userEmail = email
                                otpSubtitle = "Verify Your Identity"
                                currentScreen = "verify_otp"
                            }
                        )
                    }
                    "verify_otp" -> {
                        VerifyOtpScreen(
                            email = userEmail,
                            subtitle = otpSubtitle,
                            onBack = { 
                                if (otpSubtitle == "Verify Your Identity") {
                                    currentScreen = "forgot_password"
                                } else {
                                    currentScreen = "signup"
                                }
                            },
                            onVerify = { 
                                currentScreen = "login" 
                            }
                        )
                    }
                    "home" -> {
                        HomeScreen(
                            userName = userName,
                            userEmail = userEmail,
                            onUpdateProfile = { name, email ->
                                userName = name
                                userEmail = email
                            },
                            onLogout = { currentScreen = "login" }
                        )
                    }
                }
            }
        }
    }

    private fun scheduleScoreUpdate() {
        val calendar = Calendar.getInstance()
        val now = calendar.timeInMillis
        
        calendar.set(Calendar.HOUR_OF_DAY, 9)
        calendar.set(Calendar.MINUTE, 30)
        calendar.set(Calendar.SECOND, 0)
        
        if (calendar.timeInMillis <= now) {
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }
        
        val delay = calendar.timeInMillis - now
        
        val scoreUpdateRequest = PeriodicWorkRequestBuilder<ScoreUpdateWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "DailyScoreUpdate",
            ExistingPeriodicWorkPolicy.UPDATE,
            scoreUpdateRequest
        )
    }
}
