package com.example.digitalstreetvendornetwork

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.digitalstreetvendornetwork.ui.theme.DigitalStreetVendorNetworkTheme
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            DigitalStreetVendorNetworkTheme(darkTheme = false) {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    var currentScreen by remember { mutableStateOf("SPLASH") }
                    var loggedInRole by remember { mutableStateOf("") }

                    // Single repository instance shared across composables
                    val authRepo = remember { FirebaseAuthRepository() }

                    // Restore an existing Firebase session on cold-start
                    LaunchedEffect(Unit) {
                        coroutineScope {
                            val delayJob = async { delay(1500L) }
                            val restoreJob = async {
                                val user = authRepo.currentUser
                                if (user != null) {
                                    try {
                                        val role = authRepo.fetchRole(user.uid)
                                        role to true
                                    } catch (_: Exception) {
                                        // Firestore fetch failed — fall through to LOGIN
                                        authRepo.logout()
                                        "" to false
                                    }
                                } else {
                                    "" to false
                                }
                            }

                            delayJob.await()
                            val (role, success) = restoreJob.await()

                            if (success && role.isNotBlank()) {
                                loggedInRole = role
                                currentScreen = "MAIN"
                            } else {
                                currentScreen = "LOGIN"
                            }
                        }
                    }

                    Crossfade(targetState = currentScreen, label = "ScreenTransition") { screen ->
                        when (screen) {
                            "SPLASH" -> SplashScreen()
                            "LOGIN" -> LoginScreen(
                                authRepo = authRepo,
                                onLoginSuccess = { role -> loggedInRole = role; currentScreen = "MAIN" },
                                onNavigateToSignup = { currentScreen = "SIGNUP" }
                            )
                            "SIGNUP" -> SignupScreen(
                                authRepo = authRepo,
                                onSignupSuccess = { role -> loggedInRole = role; currentScreen = "MAIN" },
                                onNavigateToLogin = { currentScreen = "LOGIN" }
                            )
                            "MAIN" -> {
                                val handleLogout = {
                                    authRepo.logout()
                                    currentScreen = "LOGIN"
                                    loggedInRole = ""
                                }
                                val vendorId = authRepo.currentUser?.uid ?: ""
                                // Clean routing to our two separate architecture files
                                if (loggedInRole == "VENDOR") {
                                    VendorAppMainScreen(vendorId = vendorId, onLogout = handleLogout)
                                } else {
                                    CustomerAppMainScreen(onLogout = handleLogout)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ── Login Screen ──────────────────────────────────────────────────────────────

@Composable
fun LoginScreen(
    authRepo: FirebaseAuthRepository,
    onLoginSuccess: (String) -> Unit,
    onNavigateToSignup: () -> Unit
) {
    var phone by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var phoneError by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Filled.Storefront, contentDescription = "App Logo", modifier = Modifier.size(80.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(16.dp))
        Text("Local Vends", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
        Text("Your neighbourhood market, digitized.", style = MaterialTheme.typography.bodyLarge, color = Color.Gray)
        Spacer(modifier = Modifier.height(32.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it; phoneError = false; errorMessage = null },
                    label = { Text("Phone Number") },
                    leadingIcon = { Icon(Icons.Filled.Phone, contentDescription = null) },
                    singleLine = true,
                    isError = phoneError,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it; errorMessage = null },
                    label = { Text("Password") },
                    leadingIcon = { Icon(Icons.Filled.Lock, contentDescription = null) },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = errorMessage!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = {
                        if (phone.length != 10 || !phone.all { it.isDigit() }) {
                            phoneError = true
                            return@Button
                        }
                        if (password.isBlank()) {
                            errorMessage = "Password cannot be empty."
                            return@Button
                        }
                        isLoading = true
                        errorMessage = null
                        scope.launch {
                            try {
                                val role = authRepo.login(phone, password)
                                onLoginSuccess(role)
                            } catch (e: Exception) {
                                errorMessage = friendlyAuthError(e)
                            } finally {
                                isLoading = false
                            }
                        }
                    },
                    enabled = !isLoading,
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Text("Secure Login", fontSize = MaterialTheme.typography.titleMedium.fontSize)
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        TextButton(onClick = onNavigateToSignup) { Text("Create a new account") }
    }
}

// ── Signup Screen ─────────────────────────────────────────────────────────────

@Composable
fun SignupScreen(
    authRepo: FirebaseAuthRepository,
    onSignupSuccess: (String) -> Unit,
    onNavigateToLogin: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var selectedRole by remember { mutableStateOf("VENDOR") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Join Local Vends", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(24.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {

                // Role selector
                Row(
                    modifier = Modifier.fillMaxWidth().selectableGroup().padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = selectedRole == "VENDOR", onClick = { selectedRole = "VENDOR" })
                        Text("Vendor")
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = selectedRole == "CUSTOMER", onClick = { selectedRole = "CUSTOMER" })
                        Text("Customer")
                    }
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it; errorMessage = null },
                    label = { Text("Full Name") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it; errorMessage = null },
                    label = { Text("Phone") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it; errorMessage = null },
                    label = { Text("Password") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = errorMessage!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = {
                        if (name.isBlank()) { errorMessage = "Name cannot be empty."; return@Button }
                        if (phone.length != 10 || !phone.all { it.isDigit() }) { errorMessage = "Enter a valid 10-digit phone number."; return@Button }
                        if (password.isBlank()) { errorMessage = "Password cannot be empty."; return@Button }
                        isLoading = true
                        errorMessage = null
                        scope.launch {
                            try {
                                val role = authRepo.signUp(name, phone, password, selectedRole)
                                onSignupSuccess(role)
                            } catch (e: Exception) {
                                errorMessage = friendlyAuthError(e)
                            } finally {
                                isLoading = false
                            }
                        }
                    },
                    enabled = !isLoading,
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Text("Create Account")
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        TextButton(onClick = onNavigateToLogin) { Text("Already have an account? Login") }
    }
}

// ── Utilities ─────────────────────────────────────────────────────────────────

/**
 * Converts Firebase exception messages into short, user-facing strings.
 * Avoids leaking internal Firebase error codes to the UI.
 */
private fun friendlyAuthError(e: Exception): String {
    val msg = e.message ?: return "An unexpected error occurred. Please try again."
    return when {
        "EMAIL_EXISTS" in msg || "email address is already in use" in msg ->
            "This phone number is already registered. Please log in."
        "INVALID_EMAIL" in msg ->
            "Invalid phone number format. Please check and try again."
        "WRONG_PASSWORD" in msg || "password is invalid" in msg ->
            "Incorrect password. Please try again."
        "USER_NOT_FOUND" in msg || "no user record" in msg ->
            "No account found for this phone number. Please sign up."
        "WEAK_PASSWORD" in msg || "Password should be" in msg ->
            "Password must be at least 6 characters."
        "NETWORK_ERROR" in msg || "network" in msg.lowercase() ->
            "Network error. Please check your connection."
        else -> "Authentication failed. Please try again."
    }
}