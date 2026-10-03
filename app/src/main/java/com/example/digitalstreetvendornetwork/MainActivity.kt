package com.example.digitalstreetvendornetwork

import android.os.Bundle
import android.util.Patterns
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
//Adding for commit issue
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                // Tracks which screen to show: "LOGIN", "SIGNUP", or "MAIN"
                var currentScreen by remember { mutableStateOf("LOGIN") }

                when (currentScreen) {
                    "LOGIN" -> LoginScreen(
                        onLoginSuccess = { currentScreen = "MAIN" },
                        onNavigateToSignup = { currentScreen = "SIGNUP" }
                    )
                    "SIGNUP" -> SignupScreen(
                        onSignupSuccess = { currentScreen = "MAIN" },
                        onNavigateToLogin = { currentScreen = "LOGIN" }
                    )
                    "MAIN" -> VendorAppMainScreen()
                }
            }
        }
    }
}

@Composable
fun LoginScreen(onLoginSuccess: () -> Unit, onNavigateToSignup: () -> Unit) {
    var phone by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var phoneError by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Vendor Login", style = MaterialTheme.typography.headlineLarge)
        Spacer(modifier = Modifier.height(32.dp))

        OutlinedTextField(
            value = phone,
            onValueChange = {
                phone = it
                phoneError = false
            },
            label = { Text("Phone Number") },
            singleLine = true,
            isError = phoneError,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            supportingText = { if (phoneError) Text("Enter a valid 10-digit number") }
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = {
                val isPhoneValid = phone.length == 10 && phone.all { it.isDigit() }
                if (isPhoneValid && password.isNotBlank()) {
                    onLoginSuccess()
                } else {
                    phoneError = !isPhoneValid
                }
            },
            modifier = Modifier.fillMaxWidth(0.8f)
        ) {
            Text("Login")
        }

        Spacer(modifier = Modifier.height(16.dp))

        TextButton(onClick = onNavigateToSignup) {
            Text("Don't have an account? Sign up")
        }
    }
}

@Composable
fun SignupScreen(onSignupSuccess: () -> Unit, onNavigateToLogin: () -> Unit) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    var phoneError by remember { mutableStateOf(false) }
    var emailError by remember { mutableStateOf(false) }
    var nameError by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Vendor Registration", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(32.dp))

        OutlinedTextField(
            value = name,
            onValueChange = { name = it; nameError = false },
            label = { Text("Business / Vendor Name") },
            isError = nameError,
            singleLine = true,
            supportingText = { if (nameError) Text("Name cannot be empty") }
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = phone,
            onValueChange = { phone = it; phoneError = false },
            label = { Text("Phone Number") },
            isError = phoneError,
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            supportingText = { if (phoneError) Text("Enter a valid 10-digit number") }
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { email = it; emailError = false },
            label = { Text("Email Address") },
            isError = emailError,
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            supportingText = { if (emailError) Text("Enter a valid email address") }
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = {
                val isNameValid = name.isNotBlank()
                val isPhoneValid = phone.length == 10 && phone.all { it.isDigit() }
                val isEmailValid = Patterns.EMAIL_ADDRESS.matcher(email).matches()

                nameError = !isNameValid
                phoneError = !isPhoneValid
                emailError = !isEmailValid

                if (isNameValid && isPhoneValid && isEmailValid && password.isNotBlank()) {
                    onSignupSuccess()
                }
            },
            modifier = Modifier.fillMaxWidth(0.8f)
        ) {
            Text("Create Account")
        }

        Spacer(modifier = Modifier.height(16.dp))

        TextButton(onClick = onNavigateToLogin) {
            Text("Already have an account? Login")
        }
    }
}

@Composable
fun VendorAppMainScreen() {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Catalogue", "Location", "Orders")

    Scaffold(
        bottomBar = {
            NavigationBar {
                tabs.forEachIndexed { index, title ->
                    NavigationBarItem(
                        icon = {
                            val icon = when(index) {
                                0 -> Icons.Filled.Home
                                1 -> Icons.Filled.List
                                else -> Icons.Filled.Settings
                            }
                            Icon(imageVector = icon, contentDescription = title)
                        },
                        label = { Text(title) },
                        selected = selectedTab == index,
                        onClick = { selectedTab = index }
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentAlignment = Alignment.Center
        ) {
            when (selectedTab) {
                0 -> Text("Vendor Catalogue Storefront", style = MaterialTheme.typography.titleLarge)
                1 -> Text("Location & Map Setup", style = MaterialTheme.typography.titleLarge)
                2 -> Text("Incoming Local Orders", style = MaterialTheme.typography.titleLarge)
            }
        }
    }
}