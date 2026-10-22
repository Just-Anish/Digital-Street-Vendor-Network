package com.example.digitalstreetvendornetwork

import android.app.Activity
import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.StringRes
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
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
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.wrap(newBase))
    }

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
    val context = LocalContext.current
    val currentLang = remember { LocaleHelper.getLanguage(context) }
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
        // Language switcher row at top: "English | हिंदी | मराठी"
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "English",
                color = if (currentLang == "en") MaterialTheme.colorScheme.primary else Color.Gray,
                fontWeight = if (currentLang == "en") FontWeight.Bold else FontWeight.Normal,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier
                    .clickable {
                        if (currentLang != "en") {
                            LocaleHelper.setLanguage(context, "en")
                            (context as? Activity)?.recreate()
                        }
                    }
                    .padding(horizontal = 6.dp, vertical = 4.dp)
            )
            Text(" | ", color = Color.Gray, style = MaterialTheme.typography.bodyMedium)
            Text(
                text = "हिंदी",
                color = if (currentLang == "hi") MaterialTheme.colorScheme.primary else Color.Gray,
                fontWeight = if (currentLang == "hi") FontWeight.Bold else FontWeight.Normal,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier
                    .clickable {
                        if (currentLang != "hi") {
                            LocaleHelper.setLanguage(context, "hi")
                            (context as? Activity)?.recreate()
                        }
                    }
                    .padding(horizontal = 6.dp, vertical = 4.dp)
            )
            Text(" | ", color = Color.Gray, style = MaterialTheme.typography.bodyMedium)
            Text(
                text = "मराठी",
                color = if (currentLang == "mr") MaterialTheme.colorScheme.primary else Color.Gray,
                fontWeight = if (currentLang == "mr") FontWeight.Bold else FontWeight.Normal,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier
                    .clickable {
                        if (currentLang != "mr") {
                            LocaleHelper.setLanguage(context, "mr")
                            (context as? Activity)?.recreate()
                        }
                    }
                    .padding(horizontal = 6.dp, vertical = 4.dp)
            )
        }

        Icon(Icons.Filled.Storefront, contentDescription = stringResource(R.string.cd_app_logo), modifier = Modifier.size(80.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(16.dp))
        Text(stringResource(R.string.app_title), style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
        Text(stringResource(R.string.app_tagline), style = MaterialTheme.typography.bodyLarge, color = Color.Gray)
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
                    label = { Text(stringResource(R.string.phone_number)) },
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
                    label = { Text(stringResource(R.string.password)) },
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
                            errorMessage = context.getString(R.string.error_empty_password)
                            return@Button
                        }
                        isLoading = true
                        errorMessage = null
                        scope.launch {
                            try {
                                val role = authRepo.login(phone, password)
                                onLoginSuccess(role)
                            } catch (e: Exception) {
                                errorMessage = context.getString(friendlyAuthError(e))
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
                        Text(stringResource(R.string.secure_login), fontSize = MaterialTheme.typography.titleMedium.fontSize)
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        TextButton(onClick = onNavigateToSignup) { Text(stringResource(R.string.create_new_account)) }
    }
}

// ── Signup Screen ─────────────────────────────────────────────────────────────

@Composable
fun SignupScreen(
    authRepo: FirebaseAuthRepository,
    onSignupSuccess: (String) -> Unit,
    onNavigateToLogin: () -> Unit
) {
    val context = LocalContext.current
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
        Text(stringResource(R.string.join_local_vends), style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
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
                        Text(stringResource(R.string.role_vendor))
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = selectedRole == "CUSTOMER", onClick = { selectedRole = "CUSTOMER" })
                        Text(stringResource(R.string.role_customer))
                    }
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it; errorMessage = null },
                    label = { Text(stringResource(R.string.full_name)) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it; errorMessage = null },
                    label = { Text(stringResource(R.string.phone)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it; errorMessage = null },
                    label = { Text(stringResource(R.string.password)) },
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
                        if (name.isBlank()) { errorMessage = context.getString(R.string.error_empty_name); return@Button }
                        if (phone.length != 10 || !phone.all { it.isDigit() }) { errorMessage = context.getString(R.string.error_invalid_phone_10_digit); return@Button }
                        if (password.isBlank()) { errorMessage = context.getString(R.string.error_empty_password); return@Button }
                        isLoading = true
                        errorMessage = null
                        scope.launch {
                            try {
                                val role = authRepo.signUp(name, phone, password, selectedRole)
                                onSignupSuccess(role)
                            } catch (e: Exception) {
                                errorMessage = context.getString(friendlyAuthError(e))
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
                        Text(stringResource(R.string.create_account))
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        TextButton(onClick = onNavigateToLogin) { Text(stringResource(R.string.already_have_account)) }
    }
}

// ── Utilities ─────────────────────────────────────────────────────────────────

/**
 * Converts Firebase exception messages into short, user-facing string resource IDs.
 * Avoids leaking internal Firebase error codes to the UI.
 */
@StringRes
private fun friendlyAuthError(e: Exception): Int {
    val msg = e.message ?: return R.string.error_unexpected
    return when {
        "EMAIL_EXISTS" in msg || "email address is already in use" in msg ->
            R.string.error_email_exists
        "INVALID_EMAIL" in msg ->
            R.string.error_invalid_phone
        "WRONG_PASSWORD" in msg || "password is invalid" in msg ->
            R.string.error_wrong_password
        "USER_NOT_FOUND" in msg || "no user record" in msg ->
            R.string.error_user_not_found
        "WEAK_PASSWORD" in msg || "Password should be" in msg ->
            R.string.error_weak_password
        "NETWORK_ERROR" in msg || "network" in msg.lowercase() ->
            R.string.error_network
        else -> R.string.error_auth_failed
    }
}