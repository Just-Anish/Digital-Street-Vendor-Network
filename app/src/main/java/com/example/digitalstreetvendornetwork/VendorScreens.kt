package com.example.digitalstreetvendornetwork

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState

// Data Models

data class Product(
    val id: Int,
    val name: String,
    val price: String,
    val description: String,
    val isAvailable: Boolean = true
)
data class VendorReview(val id: Int, val customerName: String, val rating: Int, val comment: String, val date: String)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VendorAppMainScreen(onLogout: () -> Unit) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Catalogue", "Location", "Orders", "Profile")

    // Hoisted State for Catalogue
    val productList = remember { mutableStateListOf(
        Product(1, "Fresh Vada Pav", "₹15", "Hot and spicy Mumbai style vada pav served with green chutney."),
        Product(2, "Cutting Chai", "₹10", "Strong ginger tea, perfect for the evening."),
        Product(3, "Misal Pav", "₹40", "Spicy sprout curry with bread, garnished with onions and farsan.")
    )}

    // Hoisted State for Business Settings
    var stallName by remember { mutableStateOf("Raju's Fresh Vends") }
    var isStallOpen by remember { mutableStateOf(true) }
    var operatingHours by remember { mutableStateOf("8:00 AM - 9:00 PM") }
    var upiId by remember { mutableStateOf("rajuvends@okaxis") }

    // Hoisted State for Location (defaults to Mumbai)
    var currentVendorLocation by remember { mutableStateOf(LatLng(19.0760, 72.8777)) }
    var isBroadcastingLocation by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(tabs[selectedTab], fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            )
        },
        bottomBar = {
            NavigationBar {
                tabs.forEachIndexed { index, title ->
                    NavigationBarItem(
                        icon = {
                            val icon = when(index) {
                                0 -> Icons.Filled.Home
                                1 -> Icons.Filled.LocationOn
                                2 -> Icons.Filled.ShoppingCart
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
        Box(modifier = Modifier.padding(innerPadding)) {
            when (selectedTab) {
                0 -> VendorCatalogueScreen(productList)
                1 -> VendorLocationScreen(
                    currentLocation = currentVendorLocation,
                    onLocationChange = { currentVendorLocation = it },
                    isBroadcasting = isBroadcastingLocation,
                    onBroadcastToggle = { isBroadcastingLocation = it }
                )
                2 -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("No Active Orders") }
                3 -> VendorProfileScreen(
                    stallName = stallName,
                    onStallNameChange = { stallName = it },
                    isStallOpen = isStallOpen,
                    onStallOpenChange = { isStallOpen = it },
                    operatingHours = operatingHours,
                    onOperatingHoursChange = { operatingHours = it },
                    upiId = upiId,
                    onUpiIdChange = { upiId = it },
                    onLogout = onLogout
                )
            }
        }
    }
}

@Composable
fun VendorLocationScreen(
    currentLocation: LatLng,
    onLocationChange: (LatLng) -> Unit,
    isBroadcasting: Boolean,
    onBroadcastToggle: (Boolean) -> Unit
) {
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(currentLocation, 14f)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Interactive Google Map
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            onMapClick = { latLng ->
                // Allow vendor to tap anywhere to move their pin
                onLocationChange(latLng)
            }
        ) {
            Marker(
                state = MarkerState(position = currentLocation),
                title = "Your Stall Location",
                snippet = if (isBroadcasting) "Currently Broadcasting" else "Tap map to move pin"
            )
        }

        // Overlay Control Card at the bottom
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .align(Alignment.BottomCenter),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = if (isBroadcasting) "Broadcasting Live Location" else "Location Hidden",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isBroadcasting) MaterialTheme.colorScheme.secondary else Color.Gray
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Tap anywhere on the map to set your exact stall coordinates.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = { onBroadcastToggle(!isBroadcasting) },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isBroadcasting) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = if (isBroadcasting) Icons.Filled.LocationOff else Icons.Filled.LocationOn,
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (isBroadcasting) "Stop Broadcasting" else "Start Broadcasting", fontSize = MaterialTheme.typography.titleMedium.fontSize)
                }
            }
        }
    }
}

// ... [The rest of VendorCatalogueScreen, VendorProfileScreen, VendorEarningsAndReviewsScreen, and VendorBusinessSettingsScreen remain exactly the same as Step 1] ...

@Composable
fun VendorCatalogueScreen(productList: MutableList<Product>) {
    var showDialog by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf("") }
    var newPrice by remember { mutableStateOf("") }
    var newDesc by remember { mutableStateOf("") }
    var nameError by remember { mutableStateOf(false) }
    var priceError by remember { mutableStateOf(false) }

var nameErrorMessage by remember { mutableStateOf("") }
var priceErrorMessage by remember { mutableStateOf("") }


    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Add New Product") },
            text = {
                Column {
                    OutlinedTextField(
                        value = newName, onValueChange = { newName = it; nameError = false },
                        label = { Text("Product Name") }, isError = nameError,
                        
supportingText = {
    if (nameError) Text(nameErrorMessage)
},

                        singleLine = true, modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newPrice, onValueChange = { newPrice = it; priceError = false },
                        label = { Text("Price (e.g. 50)") }, isError = priceError,
                        
supportingText = {
    if (priceError) Text(priceErrorMessage)
},

                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true, modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newDesc, onValueChange = { newDesc = it },
                        label = { Text("Description") }, modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            
confirmButton = {
    Button(
        onClick = {
            val cleanName = newName.trim()
            val cleanDescription = newDesc.trim()
            val cleanPrice = newPrice
                .trim()
                .removePrefix("₹")
                .trim()

            val priceValue = cleanPrice.toDoubleOrNull()

            val isDuplicate = productList.any {
                it.name.trim().equals(
                    cleanName,
                    ignoreCase = true
                )
            }

            nameError = cleanName.isEmpty() || isDuplicate
            priceError = priceValue == null ||
                    !priceValue.isFinite() ||
                    priceValue <= 0.0

            nameErrorMessage = when {
                cleanName.isEmpty() ->
                    "Product name cannot be empty"
                isDuplicate ->
                    "This product already exists"
                else -> ""
            }

            priceErrorMessage = if (priceError) {
                "Enter a valid price greater than ₹0"
            } else {
                ""
            }

            if (!nameError && !priceError) {
                val validPrice = requireNotNull(priceValue)

                val nextId =
                    (productList.maxOfOrNull { it.id } ?: 0) + 1

                val formattedPrice = "₹" +
                    java.math.BigDecimal.valueOf(validPrice)
                        .stripTrailingZeros()
                        .toPlainString()

                productList.add(
                    Product(
                        id = nextId,
                        name = cleanName,
                        price = formattedPrice,
                        description = cleanDescription
                    )
                )

                newName = ""
                newPrice = ""
                newDesc = ""
                showDialog = false
            }
        }
    ) {
        Text("Save")
    }
},

            dismissButton = { TextButton(onClick = { showDialog = false }) { Text("Cancel") } }
        )
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { showDialog = true }, containerColor = MaterialTheme.colorScheme.primary) {
                Icon(Icons.Filled.Add, contentDescription = "Add Product", tint = Color.White)
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(paddingValues).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp), contentPadding = PaddingValues(top = 16.dp, bottom = 80.dp)
        ) {
            items(productList) { product ->
                Card(
                    modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(60.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) {
                            Icon(Icons.Filled.Fastfood, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(product.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(product.description, style = MaterialTheme.typography.bodySmall, color = Color.Gray, maxLines = 2)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(horizontalAlignment = Alignment.End) {
                            Text(product.price, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                            IconButton(onClick = { productList.remove(product) }) {

Spacer(modifier = Modifier.height(8.dp))

Row(
    verticalAlignment = Alignment.CenterVertically
) {
    Text(
        text = if (product.isAvailable) "Available" else "Sold Out",
        style = MaterialTheme.typography.labelSmall,
        color = if (product.isAvailable) {
            MaterialTheme.colorScheme.secondary
        } else {
            MaterialTheme.colorScheme.error
        }
    )

    Spacer(modifier = Modifier.width(4.dp))

    Switch(
        checked = product.isAvailable,
        onCheckedChange = { newAvailability ->
            val index = productList.indexOfFirst {
                it.id == product.id
            }

            if (index != -1) {
                productList[index] = product.copy(
                    isAvailable = newAvailability
                )
            }
        }
    )
}

                                Icon(Icons.Filled.Delete, contentDescription = "Delete Product", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VendorProfileScreen(
    stallName: String,
    onStallNameChange: (String) -> Unit,
    isStallOpen: Boolean,
    onStallOpenChange: (Boolean) -> Unit,
    operatingHours: String,
    onOperatingHoursChange: (String) -> Unit,
    upiId: String,
    onUpiIdChange: (String) -> Unit,
    onLogout: () -> Unit
) {
    var currentSubScreen by remember { mutableStateOf("MAIN") }

    when (currentSubScreen) {
        "MAIN" -> {
            Column(
                modifier = Modifier.fillMaxSize().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(16.dp))

                Box(
                    modifier = Modifier.size(100.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Storefront, contentDescription = "Business Profile", modifier = Modifier.size(50.dp), tint = MaterialTheme.colorScheme.primary)
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(stallName, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)

                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                    Box(
                        modifier = Modifier.size(10.dp).clip(CircleShape)
                            .background(if (isStallOpen) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.error)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isStallOpen) "Open • $operatingHours" else "Currently Closed",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isStallOpen) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                Card(
                    onClick = { currentSubScreen = "EARNINGS" },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Star, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Earnings & Reviews", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            Text("View daily revenue & customer ratings", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                        }
                        Icon(Icons.Filled.KeyboardArrowRight, contentDescription = null, tint = Color.Gray)
                    }
                }

                Card(
                    onClick = { currentSubScreen = "SETTINGS" },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Settings, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Business Settings", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            Text("Stall timings, UPI ID & status", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                        }
                        Icon(Icons.Filled.KeyboardArrowRight, contentDescription = null, tint = Color.Gray)
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                Button(
                    onClick = onLogout,
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Filled.ExitToApp, contentDescription = "Logout")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Close Shop (Logout)", fontSize = MaterialTheme.typography.titleMedium.fontSize)
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
        "EARNINGS" -> VendorEarningsAndReviewsScreen(onBack = { currentSubScreen = "MAIN" })
        "SETTINGS" -> VendorBusinessSettingsScreen(
            stallName = stallName, onStallNameChange = onStallNameChange,
            isStallOpen = isStallOpen, onStallOpenChange = onStallOpenChange,
            operatingHours = operatingHours, onOperatingHoursChange = onOperatingHoursChange,
            upiId = upiId, onUpiIdChange = onUpiIdChange,
            onBack = { currentSubScreen = "MAIN" }
        )
    }
}

@Composable
fun VendorEarningsAndReviewsScreen(onBack: () -> Unit) {
    val reviews = remember { listOf(
        VendorReview(1, "Aarav Sharma", 5, "Best Vada Pav near the station! Always served piping hot.", "Today"),
        VendorReview(2, "Priya Patel", 4, "Chai is super refreshing, very hygienic stall setup.", "Yesterday"),
        VendorReview(3, "Rohan Mehta", 5, "Misal Pav was spicy and authentic. Great value for money!", "3 days ago")
    )}

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
            IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.primary) }
            Text("Earnings & Reviews", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }

        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
            Row(modifier = Modifier.fillMaxWidth().padding(20.dp), horizontalArrangement = Arrangement.SpaceAround) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Today's Earnings", style = MaterialTheme.typography.bodyMedium, color = Color.DarkGray)
                    Text("₹1,450", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Orders Completed", style = MaterialTheme.typography.bodyMedium, color = Color.DarkGray)
                    Text("42", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Avg Rating", style = MaterialTheme.typography.bodyMedium, color = Color.DarkGray)
                    Text("4.8 ★", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = Color(0xFFFFA000))
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        Text("Recent Customer Feedback", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(12.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(reviews) { review ->
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(review.customerName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Text(review.date, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Row { repeat(review.rating) { Icon(Icons.Filled.Star, contentDescription = null, tint = Color(0xFFFFA000), modifier = Modifier.size(16.dp)) } }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(review.comment, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}

@Composable
fun VendorBusinessSettingsScreen(
    stallName: String, onStallNameChange: (String) -> Unit,
    isStallOpen: Boolean, onStallOpenChange: (Boolean) -> Unit,
    operatingHours: String, onOperatingHoursChange: (String) -> Unit,
    upiId: String, onUpiIdChange: (String) -> Unit,
    onBack: () -> Unit
) {
    var tempName by remember { mutableStateOf(stallName) }
    var tempHours by remember { mutableStateOf(operatingHours) }
    var tempUpi by remember { mutableStateOf(upiId) }
    var nameError by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
            IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.primary) }
            Text("Business Settings", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }

        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text("Stall Status", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(if (isStallOpen) "Accepting customer orders" else "Hidden from customer map", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    }
                    Switch(checked = isStallOpen, onCheckedChange = onStallOpenChange)
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

                OutlinedTextField(value = tempName, onValueChange = { tempName = it; nameError = false }, label = { Text("Stall / Business Name") }, isError = nameError, supportingText = { if (nameError) Text("Business name cannot be empty") }, singleLine = true, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(value = tempHours, onValueChange = { tempHours = it }, label = { Text("Operating Hours") }, singleLine = true, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(value = tempUpi, onValueChange = { tempUpi = it }, label = { Text("Merchant UPI ID") }, singleLine = true, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = {
                        if (tempName.isNotBlank()) {
                            onStallNameChange(tempName)
                            onOperatingHoursChange(tempHours)
                            onUpiIdChange(tempUpi)
                            onBack()
                        } else { nameError = true }
                    },
                    modifier = Modifier.fillMaxWidth().height(50.dp), shape = RoundedCornerShape(12.dp)
                ) { Text("Save Changes") }
            }
        }
    }
}