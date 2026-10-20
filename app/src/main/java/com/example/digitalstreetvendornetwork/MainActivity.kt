package com.example.digitalstreetvendornetwork

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            var currentLanguage by remember { mutableStateOf(AppLanguage.ENGLISH) }
            val currentTranslation = getAppTranslation(currentLanguage)

            CompositionLocalProvider(
                LocalAppLanguage provides currentLanguage,
                LocalStrings provides currentTranslation
            ) {
                MaterialTheme(
                    colorScheme = lightColorScheme(
                        primary = Color(0xFFE65100),
                        onPrimary = Color.White,
                        primaryContainer = Color(0xFFFFECE0),
                        onPrimaryContainer = Color(0xFF4E1600),
                        secondary = Color(0xFF2E7D32),
                        onSecondary = Color.White,
                        surfaceVariant = Color(0xFFFFF3E0),
                        error = Color(0xFFD32F2F)
                    )
                ) {
                    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                        var currentScreen by remember { mutableStateOf("LOGIN") }
                        var loggedInRole by remember { mutableStateOf("") }

                        when (currentScreen) {
                            "LOGIN" -> LoginScreen(
                                onLoginSuccess = { role -> loggedInRole = role; currentScreen = "MAIN" },
                                onNavigateToSignup = { currentScreen = "SIGNUP" },
                                onLanguageChange = { currentLanguage = it }
                            )
                            "SIGNUP" -> SignupScreen(
                                onSignupSuccess = { role -> loggedInRole = role; currentScreen = "MAIN" },
                                onNavigateToLogin = { currentScreen = "LOGIN" },
                                onLanguageChange = { currentLanguage = it }
                            )
                            "MAIN" -> {
                                val handleLogout = {
                                    currentScreen = "LOGIN"
                                    loggedInRole = ""
                                }
                                if (loggedInRole == "VENDOR") {
                                    VendorAppMainScreen(
                                        onLogout = handleLogout,
                                        onLanguageChange = { currentLanguage = it }
                                    )
                                } else {
                                    CustomerAppMainScreen(
                                        onLogout = handleLogout,
                                        onLanguageChange = { currentLanguage = it }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LoginScreen(
    onLoginSuccess: (String) -> Unit,
    onNavigateToSignup: () -> Unit,
    onLanguageChange: (AppLanguage) -> Unit
) {
    val t = LocalStrings.current
    val currentLang = LocalAppLanguage.current
    var showLanguageDialog by remember { mutableStateOf(false) }

    var phone by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var phoneError by remember { mutableStateOf(false) }
    var selectedRole by remember { mutableStateOf("VENDOR") }

    if (showLanguageDialog) {
        LanguageSelectionDialog(
            currentLanguage = currentLang,
            onLanguageSelected = onLanguageChange,
            onDismissRequest = { showLanguageDialog = false }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            LanguageSwitchPill(
                currentLanguage = currentLang,
                onClick = { showLanguageDialog = true }
            )
        }

        Spacer(modifier = Modifier.height(12.dp))
        Icon(
            Icons.Filled.Storefront,
            contentDescription = "App Logo",
            modifier = Modifier.size(72.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(t.appName, style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
        Text(t.tagline, style = MaterialTheme.typography.bodyLarge, color = Color.Gray)
        Spacer(modifier = Modifier.height(28.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .selectableGroup()
                        .padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = selectedRole == "VENDOR", onClick = { selectedRole = "VENDOR" })
                        Text(t.vendor, style = MaterialTheme.typography.bodyMedium)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = selectedRole == "CUSTOMER", onClick = { selectedRole = "CUSTOMER" })
                        Text(t.customer, style = MaterialTheme.typography.bodyMedium)
                    }
                }
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it; phoneError = false },
                    label = { Text(t.phoneNumber) },
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
                    onValueChange = { password = it },
                    label = { Text(t.password) },
                    leadingIcon = { Icon(Icons.Filled.Lock, contentDescription = null) },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = {
                        if (phone.length == 10 && phone.all { it.isDigit() } && password.isNotBlank()) {
                            onLoginSuccess(selectedRole)
                        } else {
                            phoneError = true
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(t.secureLogin, fontSize = MaterialTheme.typography.titleMedium.fontSize)
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        TextButton(onClick = onNavigateToSignup) {
            Text(t.createNewAccount)
        }
    }
}

@Composable
fun SignupScreen(
    onSignupSuccess: (String) -> Unit,
    onNavigateToLogin: () -> Unit,
    onLanguageChange: (AppLanguage) -> Unit
) {
    val t = LocalStrings.current
    val currentLang = LocalAppLanguage.current
    var showLanguageDialog by remember { mutableStateOf(false) }

    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var selectedRole by remember { mutableStateOf("VENDOR") }

    if (showLanguageDialog) {
        LanguageSelectionDialog(
            currentLanguage = currentLang,
            onLanguageSelected = onLanguageChange,
            onDismissRequest = { showLanguageDialog = false }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            LanguageSwitchPill(
                currentLanguage = currentLang,
                onClick = { showLanguageDialog = true }
            )
        }

        Spacer(modifier = Modifier.height(12.dp))
        Text(t.appName, style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(20.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .selectableGroup()
                        .padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = selectedRole == "VENDOR", onClick = { selectedRole = "VENDOR" })
                        Text(t.vendor)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = selectedRole == "CUSTOMER", onClick = { selectedRole = "CUSTOMER" })
                        Text(t.customer)
                    }
                }
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(t.fullName) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text(t.phoneNumber) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text(t.password) },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = {
                        if (name.isNotBlank() && phone.length == 10 && password.isNotBlank()) {
                            onSignupSuccess(selectedRole)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(t.createAccount)
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        TextButton(onClick = onNavigateToLogin) {
            Text(t.alreadyHaveAccount)
        }
    }
}