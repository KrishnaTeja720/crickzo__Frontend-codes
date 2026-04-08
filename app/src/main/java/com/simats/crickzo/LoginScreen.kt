package com.simats.crickzo

import android.content.Context
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.simats.crickzo.ui.theme.PrimaryBlue
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(
    onSignUpClick: () -> Unit,
    onForgotPasswordClick: () -> Unit,
    onLoginSuccess: (String, String, Int) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val apiService = RetrofitClient.apiService
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }

    fun handleLogin() {
        if (email.isBlank() || password.isBlank()) {
            coroutineScope.launch { snackbarHostState.showSnackbar("Please enter email and password") }
            return
        }

        isLoading = true
        coroutineScope.launch {
            try {
                val request = LoginRequest(email, password)
                val response = apiService.login(request)
                if (response.isSuccessful) {
                    val body = response.body()
                    if (body?.status == "success") {
                        val userId = body.userId ?: -1
                        val name = body.name ?: "User"
                        
                        val sharedPrefs = context.getSharedPreferences("criczo_prefs", Context.MODE_PRIVATE)
                        sharedPrefs.edit()
                            .putInt("user_id", userId)
                            .putString("user_name", name)
                            .putString("user_email", email)
                            .apply()
                        
                        onLoginSuccess(name, email, userId)
                    } else {
                        snackbarHostState.showSnackbar(body?.message ?: "Login failed")
                    }
                } else {
                    snackbarHostState.showSnackbar("Error: ${response.message()}")
                }
            } catch (e: Exception) {
                snackbarHostState.showSnackbar("Network error: ${e.localizedMessage}")
            } finally {
                isLoading = false
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(PrimaryBlue)
                .padding(padding)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(30.dp))

            // Logo Row with SIMATS, CRICZO, and SSE
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // SIMATS Logo (Top Left)
                Image(
                    painter = painterResource(id = R.drawable.simats),
                    contentDescription = "SIMATS Logo",
                    modifier = Modifier.size(70.dp),
                    contentScale = ContentScale.Fit
                )

                // CRICZO Logo (Middle)
                Image(
                    painter = painterResource(id = R.drawable.crickzo_logo),
                    contentDescription = "Criczo Logo",
                    modifier = Modifier.size(110.dp),
                    contentScale = ContentScale.Fit
                )

                // SSE Logo (Top Right)
                Image(
                    painter = painterResource(id = R.drawable.sse),
                    contentDescription = "SSE Logo",
                    modifier = Modifier.size(70.dp),
                    contentScale = ContentScale.Fit
                )
            }

            Spacer(modifier = Modifier.height(20.dp))
            
            Text(
                text = "User Login", 
                color = Color.White, 
                fontSize = 32.sp, 
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Watch Live Matches & AI Predictions", 
                color = Color.White.copy(alpha = 0.8f), 
                fontSize = 16.sp
            )

            Spacer(modifier = Modifier.height(40.dp))

            // White Content Card
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = Color.White,
                shape = RoundedCornerShape(topStart = 40.dp, topEnd = 40.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(horizontal = 24.dp, vertical = 32.dp)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Welcome Back", 
                        color = Color.Black, 
                        fontSize = 28.sp, 
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Sign in to watch live cricket matches", 
                        color = Color.Gray, 
                        fontSize = 14.sp,
                        modifier = Modifier.padding(top = 8.dp)
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    // Email Field
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "Email Address", 
                            fontSize = 14.sp, 
                            fontWeight = FontWeight.Bold, 
                            color = Color.Black
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = email, 
                            onValueChange = { email = it }, 
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("Enter your email", color = Color.LightGray) },
                            leadingIcon = { Icon(Icons.Default.Email, null, tint = Color.Gray) },
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = PrimaryBlue,
                                unfocusedBorderColor = Color(0xFFE0E0E0),
                                unfocusedContainerColor = Color(0xFFF9F9F9),
                                focusedContainerColor = Color(0xFFF9F9F9)
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Password Field
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "Password", 
                            fontSize = 14.sp, 
                            fontWeight = FontWeight.Bold, 
                            color = Color.Black
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = password, 
                            onValueChange = { password = it }, 
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("Enter your password", color = Color.LightGray) },
                            leadingIcon = { Icon(Icons.Default.Lock, null, tint = Color.Gray) },
                            trailingIcon = {
                                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                    Icon(
                                        if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff, 
                                        null, 
                                        tint = Color.Gray
                                    )
                                }
                            },
                            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = PrimaryBlue,
                                unfocusedBorderColor = Color(0xFFE0E0E0),
                                unfocusedContainerColor = Color(0xFFF9F9F9),
                                focusedContainerColor = Color(0xFFF9F9F9)
                            )
                        )
                    }

                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                        TextButton(onClick = onForgotPasswordClick) {
                            Text(text = "Forgot Password?", color = PrimaryBlue, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = { handleLogin() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        enabled = !isLoading,
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                        } else {
                            Text(
                                text = "Sign In", 
                                fontSize = 18.sp, 
                                fontWeight = FontWeight.Bold, 
                                color = Color.White
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "Don't have an account?", color = Color.Gray)
                        TextButton(onClick = onSignUpClick) {
                            Text(text = "Sign Up", color = PrimaryBlue, fontWeight = FontWeight.Bold)
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        text = "Powered by SIMATS Engineering", 
                        color = Color.LightGray, 
                        fontSize = 12.sp,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                }
            }
        }
    }
}
