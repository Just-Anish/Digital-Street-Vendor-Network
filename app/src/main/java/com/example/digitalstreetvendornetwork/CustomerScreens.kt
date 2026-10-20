package com.example.digitalstreetvendornetwork

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState

// Data Model for Orders
data class CustomerOrder(val id: Int, val vendorName: String, val items: String, val total: String, val date: String, val status: String)
data class MenuItem(val name: String, val price: String)

@Composable
fun CustomerAppMainScreen(onLogout: () -> Unit) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Discover", "My Orders", "Profile")

    var customerName by remember { mutableStateOf("Local Shopper") }
    var customerPhone by remember { mutableStateOf("+91 9876543210") }

    Scaffold(
        bottomBar = {
            NavigationBar {
                tabs.forEachIndexed { index, title ->
                    NavigationBarItem(
                        icon = { Icon(imageVector = when(index) { 0 -> Icons.Filled.Search; 1 -> Icons.Filled.List; else -> Icons.Filled.Person }, contentDescription = title) },
                        label = { Text(title) }, selected = selectedTab == index, onClick = { selectedTab = index }
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            when (selectedTab) {
                0 -> CustomerDiscoverScreen()
                1 -> CustomerOrderHistoryScreen(onBack = { selectedTab = 0 })
                2 -> CustomerProfileScreen(
                    name = customerName,
                    phone = customerPhone,
                    onNameChange = { customerName = it },
                    onPhoneChange = { customerPhone = it },
                    onLogout = onLogout
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerDiscoverScreen() {
    val mumbaiLocation = LatLng(19.0760, 72.8777)
    val cameraPositionState = rememberCameraPositionState { position = CameraPosition.fromLatLngZoom(mumbaiLocation, 12f) }

    var searchQuery by remember { mutableStateOf("") }
    val filters = listOf("All", "Vegetables", "Fruits", "Street Food")
    var selectedFilter by remember { mutableStateOf(filters[0]) }

    // State for the Checkout Bottom Sheet
    var selectedVendorName by remember { mutableStateOf<String?>(null) }
    var showOrderSuccess by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState
        ) {
            Marker(
                state = MarkerState(position = LatLng(19.1070, 72.8377)),
                title = "Raju Vegetables",
                snippet = "Open till 8PM",
                onClick = {
                    selectedVendorName = "Raju Vegetables"
                    true // Consumes the click event
                }
            )
            Marker(
                state = MarkerState(position = LatLng(19.0860, 72.8877)),
                title = "Vada Pav Stall",
                snippet = "Hot Snacks",
                onClick = {
                    selectedVendorName = "Vada Pav Stall"
                    true
                }
            )
        }

        Column(modifier = Modifier.fillMaxWidth().padding(16.dp).align(Alignment.TopCenter)) {
            OutlinedTextField(
                value = searchQuery, onValueChange = { searchQuery = it }, placeholder = { Text("Search for vendors or items...") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = "Search") }, modifier = Modifier.fillMaxWidth().background(Color.White, RoundedCornerShape(32.dp)),
                shape = RoundedCornerShape(32.dp), singleLine = true, colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = Color.Transparent, focusedBorderColor = MaterialTheme.colorScheme.primary)
            )
            Spacer(modifier = Modifier.height(12.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(filters) { filter ->
                    FilterChip(
                        selected = selectedFilter == filter, onClick = { selectedFilter = filter }, label = { Text(filter) },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = MaterialTheme.colorScheme.primary, selectedLabelColor = Color.White)
                    )
                }
            }
        }

        // Bottom Sheet for Checkout Flow
        if (selectedVendorName != null) {
            ModalBottomSheet(
                onDismissRequest = { selectedVendorName = null },
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                Column(modifier = Modifier.padding(24.dp).fillMaxWidth()) {
                    Text(selectedVendorName ?: "", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                    Text("Verified Local Vendor", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.secondary)

                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Menu / Catalogue", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))

                    val dummyMenu = if (selectedVendorName == "Raju Vegetables") {
                        listOf(MenuItem("1kg Farm Tomatoes", "₹40"), MenuItem("500g Fresh Onions", "₹30"))
                    } else {
                        listOf(MenuItem("Hot Vada Pav", "₹15"), MenuItem("Cutting Chai", "₹10"))
                    }

                    dummyMenu.forEach { item ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(item.name, style = MaterialTheme.typography.bodyLarge)
                            Text(item.price, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = {
                            selectedVendorName = null
                            showOrderSuccess = true
                        },
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Place Order via UPI", fontSize = MaterialTheme.typography.titleMedium.fontSize)
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }

        // Success Confirmation Dialog
        if (showOrderSuccess) {
            AlertDialog(
                onDismissRequest = { showOrderSuccess = false },
                icon = { Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(48.dp)) },
                title = { Text("Order Placed!") },
                text = { Text("The vendor has received your order request. You can track its status in the 'My Orders' tab.") },
                confirmButton = {
                    Button(onClick = { showOrderSuccess = false }) {
                        Text("Awesome")
                    }
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerProfileScreen(
    name: String,
    phone: String,
    onNameChange: (String) -> Unit,
    onPhoneChange: (String) -> Unit,
    onLogout: () -> Unit
) {
    var currentSubScreen by remember { mutableStateOf("MAIN") }

    when (currentSubScreen) {
        "MAIN" -> {
            Column(modifier = Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Spacer(modifier = Modifier.height(24.dp))
                Box(modifier = Modifier.size(100.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer), contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.Person, contentDescription = "Profile", modifier = Modifier.size(50.dp), tint = MaterialTheme.colorScheme.primary)
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(name, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Text(phone, style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                Spacer(modifier = Modifier.height(32.dp))

                ProfileOptionCard(icon = Icons.Filled.History, title = "Order History") { currentSubScreen = "HISTORY" }
                ProfileOptionCard(icon = Icons.Filled.Settings, title = "Account Settings") { currentSubScreen = "SETTINGS" }

                Spacer(modifier = Modifier.weight(1f))
                Button(
                    onClick = onLogout, modifier = Modifier.fillMaxWidth().height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error), shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Filled.ExitToApp, contentDescription = "Logout")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Logout", fontSize = MaterialTheme.typography.titleMedium.fontSize)
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
        "HISTORY" -> CustomerOrderHistoryScreen(onBack = { currentSubScreen = "MAIN" })
        "SETTINGS" -> CustomerSettingsScreen(
            currentName = name, currentPhone = phone,
            onSave = { newName, newPhone ->
                onNameChange(newName)
                onPhoneChange(newPhone)
                currentSubScreen = "MAIN"
            },
            onBack = { currentSubScreen = "MAIN" }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileOptionCard(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = title, tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(16.dp))
            Text(title, style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.weight(1f))
            Icon(Icons.Filled.KeyboardArrowRight, contentDescription = null, tint = Color.Gray)
        }
    }
}

@Composable
fun CustomerOrderHistoryScreen(onBack: () -> Unit) {
    val orders = listOf(
        CustomerOrder(1, "Raju's Fresh Vends", "2x Vada Pav, 1x Chai", "₹40", "Today, 10:30 AM", "Delivered"),
        CustomerOrder(2, "Gopi Vegetables", "1kg Tomatoes, 500g Onions", "₹120", "Yesterday", "Picked Up")
    )

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
            IconButton(onClick = onBack) {
                Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.primary)
            }
            Text("Order History", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(orders) { order ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(order.vendorName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(order.total, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(order.items, style = MaterialTheme.typography.bodyMedium, color = Color.DarkGray)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(order.date, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                            Text(order.status, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CustomerSettingsScreen(
    currentName: String,
    currentPhone: String,
    onSave: (String, String) -> Unit,
    onBack: () -> Unit
) {
    var tempName by remember { mutableStateOf(currentName) }
    var tempPhone by remember { mutableStateOf(currentPhone) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
            IconButton(onClick = onBack) {
                Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.primary)
            }
            Text("Account Settings", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                OutlinedTextField(
                    value = tempName, onValueChange = { tempName = it },
                    label = { Text("Full Name") }, singleLine = true,
                    shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = tempPhone, onValueChange = { tempPhone = it },
                    label = { Text("Phone Number") }, singleLine = true,
                    shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = { if (tempName.isNotBlank() && tempPhone.isNotBlank()) onSave(tempName, tempPhone) },
                    modifier = Modifier.fillMaxWidth().height(50.dp), shape = RoundedCornerShape(12.dp)
                ) { Text("Save Changes") }
            }
        }
    }
}