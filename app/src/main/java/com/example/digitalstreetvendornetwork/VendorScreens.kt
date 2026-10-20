package com.example.digitalstreetvendornetwork

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
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

enum class VendorOrderStatus {
    PENDING,
    PREPARING,
    COMPLETED,
    DECLINED
}

data class VendorOrderItem(
    val name: String,
    val quantity: Int,
    val price: String
)

data class VendorOrder(
    val id: String,
    val customerName: String,
    val customerPhone: String,
    val items: List<VendorOrderItem>,
    val totalAmount: Int,
    val orderTime: String,
    val paymentType: String = "UPI (Paid)",
    val deliveryType: String = "Stall Pickup",
    val specialNotes: String = "",
    val status: VendorOrderStatus = VendorOrderStatus.PENDING
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VendorAppMainScreen(
    onLogout: () -> Unit,
    onLanguageChange: (AppLanguage) -> Unit = {}
) {
    val t = LocalStrings.current
    val currentLang = LocalAppLanguage.current
    var showLanguageDialog by remember { mutableStateOf(false) }

    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf(t.catalogue, t.liveLocation, t.orders, t.profile)

    // Hoisted State for Catalogue
    val productList = remember { mutableStateListOf(
        Product(1, "Fresh Vada Pav", "₹15", "Hot and spicy Mumbai style vada pav served with green chutney."),
        Product(2, "Cutting Chai", "₹10", "Strong ginger tea, perfect for the evening."),
        Product(3, "Misal Pav", "₹40", "Spicy sprout curry with bread, garnished with onions and farsan.")
    )}

    // Hoisted State for Live Orders
    val ordersList = remember {
        mutableStateListOf(
            VendorOrder(
                id = "ORD-4081",
                customerName = "Aarav Sharma",
                customerPhone = "+91 98201 12345",
                items = listOf(
                    VendorOrderItem("Fresh Vada Pav", 2, "₹30"),
                    VendorOrderItem("Cutting Chai", 1, "₹10")
                ),
                totalAmount = 40,
                orderTime = "Just now",
                paymentType = "UPI (Paid)",
                deliveryType = "Stall Pickup",
                specialNotes = "Make it extra spicy with green chutney",
                status = VendorOrderStatus.PENDING
            ),
            VendorOrder(
                id = "ORD-4080",
                customerName = "Pooja Deshmukh",
                customerPhone = "+91 98190 67890",
                items = listOf(
                    VendorOrderItem("Misal Pav", 1, "₹40"),
                    VendorOrderItem("Cutting Chai", 2, "₹20")
                ),
                totalAmount = 60,
                orderTime = "6 mins ago",
                paymentType = "Cash on Pickup",
                deliveryType = "Stall Pickup",
                specialNotes = "Less farsan please",
                status = VendorOrderStatus.PENDING
            ),
            VendorOrder(
                id = "ORD-4078",
                customerName = "Vikram Patil",
                customerPhone = "+91 97654 32109",
                items = listOf(
                    VendorOrderItem("Fresh Vada Pav", 3, "₹45")
                ),
                totalAmount = 45,
                orderTime = "14 mins ago",
                paymentType = "UPI (Paid)",
                deliveryType = "Nearby Handover",
                specialNotes = "Waiting near the ATM across the road",
                status = VendorOrderStatus.PREPARING
            ),
            VendorOrder(
                id = "ORD-4075",
                customerName = "Neha Kulkarni",
                customerPhone = "+91 99887 76655",
                items = listOf(
                    VendorOrderItem("Misal Pav", 2, "₹80")
                ),
                totalAmount = 80,
                orderTime = "28 mins ago",
                paymentType = "UPI (Paid)",
                deliveryType = "Stall Pickup",
                status = VendorOrderStatus.COMPLETED
            )
        )
    }

    // Hoisted State for Earnings Sync
    var todayEarnings by remember { mutableIntStateOf(1450) }
    var completedOrdersCount by remember { mutableIntStateOf(42) }

    // Hoisted State for Business Settings
    var stallName by remember { mutableStateOf("Raju's Fresh Vends") }
    var isStallOpen by remember { mutableStateOf(true) }
    var operatingHours by remember { mutableStateOf("8:00 AM - 9:00 PM") }
    var upiId by remember { mutableStateOf("rajuvends@okaxis") }

    // Hoisted State for Location (defaults to Mumbai)
    var currentVendorLocation by remember { mutableStateOf(LatLng(19.0760, 72.8777)) }
    var isBroadcastingLocation by remember { mutableStateOf(false) }

    val pendingOrdersCount = ordersList.count { it.status == VendorOrderStatus.PENDING }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(tabs[selectedTab], fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                actions = {
                    LanguageSwitchPill(
                        currentLanguage = currentLang,
                        onClick = { showLanguageDialog = true },
                        modifier = Modifier.padding(end = 8.dp)
                    )
                }
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
                            if (index == 2 && pendingOrdersCount > 0) {
                                BadgedBox(
                                    badge = {
                                        Badge(containerColor = MaterialTheme.colorScheme.primary) {
                                            Text("$pendingOrdersCount", color = Color.White)
                                        }
                                    }
                                ) {
                                    Icon(imageVector = icon, contentDescription = title)
                                }
                            } else {
                                Icon(imageVector = icon, contentDescription = title)
                            }
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
                2 -> VendorOrdersScreen(
                    ordersList = ordersList,
                    onAcceptOrder = { order ->
                        val index = ordersList.indexOfFirst { it.id == order.id }
                        if (index != -1) {
                            ordersList[index] = order.copy(status = VendorOrderStatus.PREPARING)
                        }
                    },
                    onCompleteOrder = { order ->
                        val index = ordersList.indexOfFirst { it.id == order.id }
                        if (index != -1) {
                            ordersList[index] = order.copy(status = VendorOrderStatus.COMPLETED)
                            todayEarnings += order.totalAmount
                            completedOrdersCount += 1
                        }
                    },
                    onDeclineOrder = { order ->
                        val index = ordersList.indexOfFirst { it.id == order.id }
                        if (index != -1) {
                            ordersList[index] = order.copy(status = VendorOrderStatus.DECLINED)
                        }
                    },
                    onSimulateOrder = {
                        val nextNum = ordersList.size + 4082
                        val newOrder = VendorOrder(
                            id = "ORD-$nextNum",
                            customerName = listOf("Karan Malhotra", "Sneha Roy", "Sameer Joshi", "Ananya Verma").random(),
                            customerPhone = "+91 9" + (100000000..999999999).random(),
                            items = listOf(
                                VendorOrderItem("Fresh Vada Pav", (1..3).random(), "₹30"),
                                VendorOrderItem("Cutting Chai", (1..2).random(), "₹10")
                            ),
                            totalAmount = listOf(25, 40, 55, 70).random(),
                            orderTime = "Just now",
                            paymentType = listOf("UPI (Paid)", "Cash on Pickup").random(),
                            deliveryType = "Stall Pickup",
                            specialNotes = listOf("Pack extra pav", "No garlic if possible", "Keep it spicy", "").random(),
                            status = VendorOrderStatus.PENDING
                        )
                        ordersList.add(0, newOrder)
                    }
                )
                3 -> VendorProfileScreen(
                    stallName = stallName,
                    onStallNameChange = { stallName = it },
                    isStallOpen = isStallOpen,
                    onStallOpenChange = { isStallOpen = it },
                    operatingHours = operatingHours,
                    onOperatingHoursChange = { operatingHours = it },
                    upiId = upiId,
                    onUpiIdChange = { upiId = it },
                    todayEarnings = todayEarnings,
                    completedOrdersCount = completedOrdersCount,
                    onLanguageClick = { showLanguageDialog = true },
                    onLogout = onLogout
                )
            }
        }
    }

    if (showLanguageDialog) {
        LanguageSelectionDialog(
            currentLanguage = currentLang,
            onLanguageSelected = onLanguageChange,
            onDismissRequest = { showLanguageDialog = false }
        )
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
    var productToDelete by remember { mutableStateOf<Product?>(null) }
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

    if (productToDelete != null) {
        AlertDialog(
            onDismissRequest = { productToDelete = null },
            icon = {
                Icon(
                    imageVector = Icons.Outlined.DeleteOutline,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(28.dp)
                )
            },
            title = {
                Text(
                    text = "Delete Product",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to remove \"${productToDelete?.name}\" from your catalogue? This action cannot be undone.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val item = productToDelete
                        if (item != null) {
                            productList.remove(item)
                        }
                        productToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Delete", fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { productToDelete = null },
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Cancel")
                }
            },
            shape = RoundedCornerShape(20.dp)
        )
    }

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showDialog = true },
                icon = { Icon(Icons.Filled.Add, contentDescription = null, tint = Color.White) },
                text = { Text("Add Item", fontWeight = FontWeight.Bold, color = Color.White) },
                containerColor = MaterialTheme.colorScheme.primary,
                shape = RoundedCornerShape(16.dp),
                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 4.dp, pressedElevation = 8.dp)
            )
        }
    ) { paddingValues ->
        if (productList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Fastfood,
                            contentDescription = null,
                            modifier = Modifier.size(36.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No Items in Catalogue",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Tap '+ Add Item' to add products to your menu.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(top = 16.dp, bottom = 88.dp)
            ) {
                items(productList, key = { it.id }) { product ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            // Top Row: Food Icon, Name & Description, Price
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(56.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(
                                            if (product.isAvailable)
                                                MaterialTheme.colorScheme.surfaceVariant
                                            else
                                                Color(0xFFF1F1F1)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Fastfood,
                                        contentDescription = null,
                                        tint = if (product.isAvailable)
                                            MaterialTheme.colorScheme.primary
                                        else
                                            Color.Gray,
                                        modifier = Modifier.size(26.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(14.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = product.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (product.isAvailable) Color.Unspecified else Color.Gray
                                    )
                                    Spacer(modifier = Modifier.height(3.dp))
                                    Text(
                                        text = product.description,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.Gray,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                Spacer(modifier = Modifier.width(10.dp))

                                Text(
                                    text = product.price,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = if (product.isAvailable)
                                        MaterialTheme.colorScheme.secondary
                                    else
                                        Color.Gray
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))
                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                                thickness = 0.8.dp
                            )
                            Spacer(modifier = Modifier.height(10.dp))

                            // Bottom Action Bar: Availability Toggle Pill & Delete Button
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                // Status / Availability Pill
                                Surface(
                                    shape = RoundedCornerShape(20.dp),
                                    color = if (product.isAvailable) Color(0xFFE8F5E9) else Color(0xFFFFEBEE)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(start = 10.dp, end = 4.dp, top = 2.dp, bottom = 2.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .clip(CircleShape)
                                                .background(
                                                    if (product.isAvailable) Color(0xFF2E7D32) else Color(0xFFD32F2F)
                                                )
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = if (product.isAvailable) "Available" else "Sold Out",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = if (product.isAvailable) Color(0xFF2E7D32) else Color(0xFFD32F2F)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Switch(
                                            checked = product.isAvailable,
                                            onCheckedChange = { newAvailability ->
                                                val index = productList.indexOfFirst { it.id == product.id }
                                                if (index != -1) {
                                                    productList[index] = product.copy(isAvailable = newAvailability)
                                                }
                                            },
                                            modifier = Modifier.scale(0.8f),
                                            colors = SwitchDefaults.colors(
                                                checkedThumbColor = Color.White,
                                                checkedTrackColor = Color(0xFF2E7D32),
                                                uncheckedThumbColor = Color.White,
                                                uncheckedTrackColor = Color(0xFFB0BEC5),
                                                checkedBorderColor = Color.Transparent,
                                                uncheckedBorderColor = Color.Transparent
                                            )
                                        )
                                    }
                                }

                                // Delete Action Button
                                IconButton(
                                    onClick = { productToDelete = product },
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFFFEBEE))
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.DeleteOutline,
                                        contentDescription = "Delete ${product.name}",
                                        tint = Color(0xFFD32F2F),
                                        modifier = Modifier.size(20.dp)
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
    todayEarnings: Int = 1450,
    completedOrdersCount: Int = 42,
    onLanguageClick: () -> Unit = {},
    onLogout: () -> Unit
) {
    val t = LocalStrings.current
    val currentLang = LocalAppLanguage.current
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
                        text = if (isStallOpen) "${t.stallOpen} • $operatingHours" else t.stallClosed,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isStallOpen) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.height(28.dp))

                Card(
                    onClick = { currentSubScreen = "EARNINGS" },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Star, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Earnings & Ledger", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            Text("View daily revenue, ledger & reviews", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                        }
                        Icon(Icons.Filled.KeyboardArrowRight, contentDescription = null, tint = Color.Gray)
                    }
                }

                Card(
                    onClick = onLanguageClick,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Translate, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(t.language, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            Text("${currentLang.displayName} (${currentLang.nativeName})", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                        }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                text = t.changeLanguage,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                Card(
                    onClick = { currentSubScreen = "SETTINGS" },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
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
                    Text("${t.closeStall} (${t.logout})", fontSize = MaterialTheme.typography.titleMedium.fontSize)
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
        "EARNINGS" -> VendorEarningsAndReviewsScreen(
            todayEarnings = todayEarnings,
            completedOrdersCount = completedOrdersCount,
            onBack = { currentSubScreen = "MAIN" }
        )
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
fun VendorEarningsAndReviewsScreen(
    todayEarnings: Int = 1450,
    completedOrdersCount: Int = 42,
    onBack: () -> Unit
) {
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
                    Text("₹%,d".format(todayEarnings), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Orders Completed", style = MaterialTheme.typography.bodyMedium, color = Color.DarkGray)
                    Text("$completedOrdersCount", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
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

@Composable
fun VendorOrdersScreen(
    ordersList: List<VendorOrder>,
    onAcceptOrder: (VendorOrder) -> Unit,
    onCompleteOrder: (VendorOrder) -> Unit,
    onDeclineOrder: (VendorOrder) -> Unit,
    onSimulateOrder: () -> Unit
) {
    val t = LocalStrings.current
    var selectedFilter by remember { mutableStateOf("Pending") }
    val filterItems = listOf(
        "Pending" to t.pending,
        "Preparing" to t.preparing,
        "Completed" to t.completed,
        "All" to t.all
    )

    val pendingCount = ordersList.count { it.status == VendorOrderStatus.PENDING }
    val preparingCount = ordersList.count { it.status == VendorOrderStatus.PREPARING }
    val completedCount = ordersList.count { it.status == VendorOrderStatus.COMPLETED }

    val filteredOrders = remember(ordersList, selectedFilter) {
        when (selectedFilter) {
            "Pending" -> ordersList.filter { it.status == VendorOrderStatus.PENDING }
            "Preparing" -> ordersList.filter { it.status == VendorOrderStatus.PREPARING }
            "Completed" -> ordersList.filter { it.status == VendorOrderStatus.COMPLETED }
            else -> ordersList
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF9F9FB))
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(12.dp))

        // Demo simulation banner + quick metrics
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = t.incomingOrders,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "$pendingCount ${t.pending} • $preparingCount ${t.preparing}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }

            OutlinedButton(
                onClick = onSimulateOrder,
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(
                    imageVector = Icons.Filled.AddAlert,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(t.simulateOrder, style = MaterialTheme.typography.labelMedium)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Filter chips row
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(filterItems) { (filterKey, filterLabel) ->
                val count = when (filterKey) {
                    "Pending" -> pendingCount
                    "Preparing" -> preparingCount
                    "Completed" -> completedCount
                    else -> ordersList.size
                }
                val label = "$filterLabel ($count)"
                val isSelected = selectedFilter == filterKey

                FilterChip(
                    selected = isSelected,
                    onClick = { selectedFilter = filterKey },
                    label = {
                        Text(
                            text = label,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    shape = RoundedCornerShape(20.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = Color.White
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (filteredOrders.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = when (selectedFilter) {
                                "Pending" -> Icons.Filled.AccessTime
                                "Preparing" -> Icons.Filled.RestaurantMenu
                                else -> Icons.Filled.DoneAll
                            },
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No $selectedFilter Orders",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = when (selectedFilter) {
                            "Pending" -> "New orders from customers will appear here in real time."
                            "Preparing" -> "Accept pending orders to start preparing them."
                            else -> "Completed orders will be logged here."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(14.dp),
                contentPadding = PaddingValues(bottom = 96.dp)
            ) {
                items(filteredOrders, key = { it.id }) { order ->
                    VendorOrderCard(
                        order = order,
                        onAccept = { onAcceptOrder(order) },
                        onComplete = { onCompleteOrder(order) },
                        onDecline = { onDeclineOrder(order) }
                    )
                }
            }
        }
    }
}

@Composable
fun VendorOrderCard(
    order: VendorOrder,
    onAccept: () -> Unit,
    onComplete: () -> Unit,
    onDecline: () -> Unit
) {
    val t = LocalStrings.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header: ID + Time + Status Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = order.id,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "• ${order.orderTime}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }

                val (statusBg, statusColor, statusText) = when (order.status) {
                    VendorOrderStatus.PENDING -> Triple(Color(0xFFFFF3E0), Color(0xFFE65100), t.pending)
                    VendorOrderStatus.PREPARING -> Triple(Color(0xFFE3F2FD), Color(0xFF1976D2), t.preparing)
                    VendorOrderStatus.COMPLETED -> Triple(Color(0xFFE8F5E9), Color(0xFF2E7D32), t.completed)
                    VendorOrderStatus.DECLINED -> Triple(Color(0xFFFFEBEE), Color(0xFFD32F2F), t.cancel)
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = statusBg
                ) {
                    Text(
                        text = statusText,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = statusColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Customer Name & Contact & Delivery Type
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = order.customerName.take(1).uppercase(),
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = order.customerName,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = order.customerPhone,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFF5F5F5)
                ) {
                    Text(
                        text = order.deliveryType,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.DarkGray
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = Color(0xFFF0F0F0), thickness = 0.8.dp)
            Spacer(modifier = Modifier.height(10.dp))

            // Items List
            order.items.forEach { item ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${item.quantity}x ${item.name}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = item.price,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Special Instructions
            if (order.specialNotes.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFFFF9C4).copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Note: ${order.specialNotes}",
                        modifier = Modifier.padding(8.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF5D4037)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = Color(0xFFF0F0F0), thickness = 0.8.dp)
            Spacer(modifier = Modifier.height(10.dp))

            // Footer: Payment Type & Total Amount
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (order.paymentType.contains("UPI")) Color(0xFFE8F5E9) else Color(0xFFFFF3E0)
                ) {
                    Text(
                        text = order.paymentType,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Medium,
                        color = if (order.paymentType.contains("UPI")) Color(0xFF2E7D32) else Color(0xFFE65100)
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Total: ",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray
                    )
                    Text(
                        text = "₹${order.totalAmount}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }

            // Action Buttons
            when (order.status) {
                VendorOrderStatus.PENDING -> {
                    Spacer(modifier = Modifier.height(14.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = onDecline,
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Icon(Icons.Filled.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(t.cancel)
                        }

                        Button(
                            onClick = onAccept,
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(t.acceptOrder)
                        }
                    }
                }

                VendorOrderStatus.PREPARING -> {
                    Spacer(modifier = Modifier.height(14.dp))
                    Button(
                        onClick = onComplete,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondary
                        )
                    ) {
                        Icon(Icons.Filled.DoneAll, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(t.completeOrder)
                    }
                }

                VendorOrderStatus.COMPLETED -> {
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.CheckCircle,
                            contentDescription = null,
                            tint = Color(0xFF2E7D32),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Order Fulfilled & Settled",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color(0xFF2E7D32),
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                VendorOrderStatus.DECLINED -> {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Order was declined",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                }
            }
        }
    }
}