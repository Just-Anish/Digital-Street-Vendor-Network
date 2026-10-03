package com.example.digitalstreetvendornetwork
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                VendorAppMainScreen()
            }
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
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
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