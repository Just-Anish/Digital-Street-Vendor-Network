package com.example.digitalstreetvendornetwork
import android.os.Bundle
import android.util.Patterns
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                // Tracks which screen to show
                var currentScreen by remember { mutableStateOf("LOGIN") }
                // Tracks the logged-in user's role
                var loggedInRole by remember { mutableStateOf("") }

                when (currentScreen) {
                    "LOGIN" -> LoginScreen(
                        onLoginSuccess = { role ->
                            loggedInRole = role
                            currentScreen = "MAIN"
                        },
                        onNavigateToSignup = { currentScreen = "SIGNUP" }
                    )
                    "SIGNUP" -> SignupScreen(
                        onSignupSuccess = { role ->
                            loggedInRole = role
                            currentScreen = "MAIN"
                        },
                        onNavigateToLogin = { currentScreen = "LOGIN" }
                    )
                    "MAIN" -> {
                        if (loggedInRole == "VENDOR") {
                            VendorAppMainScreen()
                        } else {
                            CustomerAppMainScreen()
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LoginScreen(onLoginSuccess: (String) -> Unit, onNavigateToSignup: () -> Unit) {
    var phone by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var phoneError by remember { mutableStateOf(false) }
    var selectedRole by remember { mutableStateOf("VENDOR") } // Default to Vendor

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Welcome Back", style = MaterialTheme.typography.headlineLarge)
        Spacer(modifier = Modifier.height(24.dp))

        // Role Selection
        Row(
            modifier = Modifier.fillMaxWidth().selectableGroup(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(
                    selected = selectedRole == "VENDOR",
                    onClick = { selectedRole = "VENDOR" }
                )
                Text("Vendor")
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(
                    selected = selectedRole == "CUSTOMER",
                    onClick = { selectedRole = "CUSTOMER" }
                )
                Text("Customer")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = phone,
            onValueChange = { phone = it; phoneError = false },
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
                    onLoginSuccess(selectedRole)
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
fun SignupScreen(onSignupSuccess: (String) -> Unit, onNavigateToLogin: () -> Unit) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var selectedRole by remember { mutableStateOf("VENDOR") }

    var phoneError by remember { mutableStateOf(false) }
    var emailError by remember { mutableStateOf(false) }
    var nameError by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Create Account", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(24.dp))

        // Role Selection
        Row(
            modifier = Modifier.fillMaxWidth().selectableGroup(),
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

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = name,
            onValueChange = { name = it; nameError = false },
            label = { Text("Full Name / Business Name") },
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
                    onSignupSuccess(selectedRole)
                }
            },
            modifier = Modifier.fillMaxWidth(0.8f)
        ) {
            Text("Sign Up")
        }

        Spacer(modifier = Modifier.height(16.dp))

        TextButton(onClick = onNavigateToLogin) {
            Text("Already have an account? Login")
        }
    }
}

// --- VENDOR DASHBOARD ---

data class Product(val id: Int, val name: String, val price: String, val description: String)

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
        Box(modifier = Modifier.padding(innerPadding)) {
            when (selectedTab) {
                0 -> VendorCatalogueScreen()
                1 -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Vendor Location Map", style = MaterialTheme.typography.titleLarge) }
                2 -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Pending Customer Orders", style = MaterialTheme.typography.titleLarge) }
            }
        }
    }
}

@Composable
fun VendorCatalogueScreen() {
    val productList = listOf(
        Product(1, "Fresh Vada Pav", "₹15", "Hot and spicy Mumbai style vada pav"),
        Product(2, "Cutting Chai", "₹10", "Strong ginger tea"),
        Product(3, "Misal Pav", "₹40", "Spicy sprout curry with bread")
    )

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { /* TODO: Open Add Item dialog */ },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Add Product")
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Text("Your Active Catalogue", style = MaterialTheme.typography.headlineMedium)
                Spacer(modifier = Modifier.height(16.dp))
            }
            items(productList) { product ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(product.name, style = MaterialTheme.typography.titleMedium)
                            Text(product.price, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(product.description, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }
}

// --- CUSTOMER DASHBOARD ---

@Composable
fun CustomerAppMainScreen() {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Discover", "My Orders", "Profile")

    Scaffold(
        bottomBar = {
            NavigationBar {
                tabs.forEachIndexed { index, title ->
                    NavigationBarItem(
                        icon = {
                            val icon = when(index) {
                                0 -> Icons.Filled.Search
                                1 -> Icons.Filled.List
                                else -> Icons.Filled.Person
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
                0 -> Text("Map: Discover Nearby Vendors", style = MaterialTheme.typography.titleLarge)
                1 -> Text("Track My Pickups & Deliveries", style = MaterialTheme.typography.titleLarge)
                2 -> Text("Customer Profile & Settings", style = MaterialTheme.typography.titleLarge)
            }
        }
    }
}