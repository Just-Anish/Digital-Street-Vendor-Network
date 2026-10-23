package com.example.digitalstreetvendornetwork

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.digitalstreetvendornetwork.viewmodel.Order
import com.example.digitalstreetvendornetwork.viewmodel.VendorViewModel
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState

data class Product(
    var id: String = "",
    val name: String = "",
    val price: String = "",
    val description: String = "",
    val isAvailable: Boolean = true
)

data class VendorReview(val id: Int, val customerName: String, val rating: Int, val comment: String, val date: String)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VendorAppMainScreen(vendorId: String, onLogout: () -> Unit) {
    val viewModel: VendorViewModel = viewModel()
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Catalogue", "Location", "Orders", "Profile")

    val productList by viewModel.productList.collectAsState(initial = emptyList())
    val stallName by viewModel.stallName.collectAsState(initial = "Loading...")
    val isStallOpen by viewModel.isStallOpen.collectAsState(initial = false)
    val operatingHours by viewModel.operatingHours.collectAsState(initial = "")
    val upiId by viewModel.upiId.collectAsState(initial = "")
    val isBroadcastingLocation by viewModel.isBroadcastingLocation.collectAsState()
    val stallLat by viewModel.stallLat.collectAsState()
    val stallLng by viewModel.stallLng.collectAsState()
    val activeOrders by viewModel.activeOrders.collectAsState()
    val todayEarnings by viewModel.todayEarnings.collectAsState()
    val completedOrderCount by viewModel.completedOrderCount.collectAsState()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(tabs[selectedTab], fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
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
                0 -> VendorCatalogueScreen(
                    productList = productList,
                    onAddProduct = { name, price, desc -> viewModel.addProduct(name, price, desc) },
                    onDeleteProduct = { id -> viewModel.deleteProduct(id) }
                )
                1 -> VendorLocationScreen(
                    isBroadcasting = isBroadcastingLocation,
                    stallLat = stallLat,
                    stallLng = stallLng,
                    onToggleBroadcast = { viewModel.toggleLocationBroadcast(it) },
                    onLocationChange = { lat, lng -> viewModel.updateLocation(lat, lng) }
                )
                2 -> VendorOrdersScreen(
                    orders = activeOrders,
                    onUpdateStatus = { orderId, newStatus -> viewModel.updateOrderStatus(orderId, newStatus) }
                )
                3 -> VendorProfileScreen(
                    stallName = stallName,
                    onStallNameChange = { viewModel.updateBusinessSettings(it, operatingHours, upiId) },
                    isStallOpen = isStallOpen,
                    onStallOpenChange = { viewModel.toggleStallStatus(it) },
                    operatingHours = operatingHours,
                    onOperatingHoursChange = { viewModel.updateBusinessSettings(stallName, it, upiId) },
                    upiId = upiId,
                    onUpiIdChange = { viewModel.updateBusinessSettings(stallName, operatingHours, it) },
                    todayEarnings = todayEarnings,
                    completedOrderCount = completedOrderCount,
                    onRefreshEarnings = { viewModel.calculateTodayEarnings() },
                    onLogout = onLogout
                )
            }
        }
    }
}

@Composable
fun VendorCatalogueScreen(
    productList: List<Product>,
    onAddProduct: (String, String, String) -> Unit,
    onDeleteProduct: (String) -> Unit
) {
    var showDialog by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf("") }
    var newPrice by remember { mutableStateOf("") }
    var newDesc by remember { mutableStateOf("") }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Add New Product") },
            text = {
                Column {
                    OutlinedTextField(value = newName, onValueChange = { newName = it }, label = { Text("Product Name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = newPrice, onValueChange = { newPrice = it }, label = { Text("Price (e.g. 50)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(value = newDesc, onValueChange = { newDesc = it }, label = { Text("Description") }, modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (newName.isNotBlank() && newPrice.isNotBlank()) {
                        onAddProduct(newName, if (newPrice.startsWith("₹")) newPrice else "₹$newPrice", newDesc)
                        newName = ""; newPrice = ""; newDesc = ""; showDialog = false
                    }
                }) { Text("Save") }
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
                            IconButton(onClick = { onDeleteProduct(product.id) }) {
                                Icon(Icons.Filled.Delete, contentDescription = "Delete Product", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun VendorLocationScreen(
    isBroadcasting: Boolean,
    stallLat: Double,
    stallLng: Double,
    onToggleBroadcast: (Boolean) -> Unit,
    onLocationChange: (lat: Double, lng: Double) -> Unit
) {
    val markerState = remember(stallLat, stallLng) {
        MarkerState(position = LatLng(stallLat, stallLng))
    }
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(LatLng(stallLat, stallLng), 15f)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Map fills the upper portion of the screen
        Box(modifier = Modifier.weight(1f)) {
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                onMapClick = { latLng ->
                    markerState.position = latLng
                    onLocationChange(latLng.latitude, latLng.longitude)
                }
            ) {
                Marker(
                    state = markerState,
                    title = "My Stall",
                    draggable = true,
                    onClick = { false }   // don't consume — let map click also fire
                )
            }
            // Marker drag listener: capture the final dropped position
            LaunchedEffect(markerState.position) {
                onLocationChange(markerState.position.latitude, markerState.position.longitude)
            }
        }

        // Broadcast toggle card at the bottom
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Broadcast Location",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (isBroadcasting) "Visible to customers on map" else "Hidden from customer map",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isBroadcasting) MaterialTheme.colorScheme.secondary else Color.Gray
                    )
                }
                Switch(
                    checked = isBroadcasting,
                    onCheckedChange = onToggleBroadcast
                )
            }
        }
    }
}

@Composable
fun VendorProfileScreen(
    stallName: String, onStallNameChange: (String) -> Unit,
    isStallOpen: Boolean, onStallOpenChange: (Boolean) -> Unit,
    operatingHours: String, onOperatingHoursChange: (String) -> Unit,
    upiId: String, onUpiIdChange: (String) -> Unit,
    todayEarnings: String,
    completedOrderCount: Int,
    onRefreshEarnings: () -> Unit,
    onLogout: () -> Unit
) {
    var currentSubScreen by remember { mutableStateOf("MAIN") }

    when (currentSubScreen) {
        "MAIN" -> {
            Column(modifier = Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Spacer(modifier = Modifier.height(16.dp))
                Box(modifier = Modifier.size(100.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer), contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.Storefront, contentDescription = null, modifier = Modifier.size(50.dp), tint = MaterialTheme.colorScheme.primary)
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(stallName, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                    Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(if (isStallOpen) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.error))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = if (isStallOpen) "Open • $operatingHours" else "Currently Closed", color = if (isStallOpen) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.error, fontWeight = FontWeight.Medium)
                }
                Spacer(modifier = Modifier.height(32.dp))
                Card(onClick = { currentSubScreen = "EARNINGS" }, modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp), shape = RoundedCornerShape(12.dp)) {
                    Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Star, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Earnings & Reviews", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            Text("View daily revenue & customer ratings", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                        }
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = Color.Gray)
                    }
                }
                Card(onClick = { currentSubScreen = "SETTINGS" }, modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp), shape = RoundedCornerShape(12.dp)) {
                    Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Settings, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Business Settings", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            Text("Stall timings, UPI ID & status", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                        }
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = Color.Gray)
                    }
                }
                Spacer(modifier = Modifier.weight(1f))
                Button(onClick = onLogout, modifier = Modifier.fillMaxWidth().height(50.dp), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error), shape = RoundedCornerShape(12.dp)) {
                    Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "Logout")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Close Shop (Logout)")
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
        "EARNINGS" -> VendorEarningsAndReviewsScreen(
            todayEarnings = todayEarnings,
            completedOrderCount = completedOrderCount,
            onRefreshEarnings = onRefreshEarnings,
            onBack = { currentSubScreen = "MAIN" }
        )
        "SETTINGS" -> VendorBusinessSettingsScreen(
            stallName, onStallNameChange, isStallOpen, onStallOpenChange, operatingHours, onOperatingHoursChange, upiId, onUpiIdChange, onBack = { currentSubScreen = "MAIN" }
        )
    }
}

@Composable
fun VendorEarningsAndReviewsScreen(
    todayEarnings: String,
    completedOrderCount: Int,
    onRefreshEarnings: () -> Unit,
    onBack: () -> Unit
) {
    // Trigger a fresh calculation each time the screen is entered
    LaunchedEffect(Unit) { onRefreshEarnings() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Back-bar
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            Text(
                "Earnings & Reviews",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }

        Text(
            text = "Today's Summary",
            style = MaterialTheme.typography.titleSmall,
            color = Color.Gray,
            modifier = Modifier.padding(start = 4.dp, bottom = 12.dp)
        )

        // ── Revenue card ───────────────────────────────────────────────────────
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 14.dp),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Total Revenue Today",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = todayEarnings,
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Filled.CurrencyRupee,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(30.dp)
                    )
                }
            }
        }

        // ── Completed orders card ────────────────────────────────────────────
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 14.dp),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFFE8F5E9)   // light green tint
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Orders Completed Today",
                        style = MaterialTheme.typography.labelLarge,
                        color = Color(0xFF388E3C).copy(alpha = 0.8f)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "$completedOrderCount",
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF2E7D32)
                    )
                }
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(
                            Color(0xFF2E7D32).copy(alpha = 0.12f),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Filled.CheckCircle,
                        contentDescription = null,
                        tint = Color(0xFF2E7D32),
                        modifier = Modifier.size(30.dp)
                    )
                }
            }
        }

        // ── Reviews teaser card ────────────────────────────────────────────────
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Customer Reviews",
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.Gray
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Coming in a future update",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.LightGray
                    )
                }
                Icon(
                    Icons.Filled.Star,
                    contentDescription = null,
                    tint = Color(0xFFFFC107),
                    modifier = Modifier.size(32.dp)
                )
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

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.primary) }
            Text("Business Settings", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text("Stall Status", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(if (isStallOpen) "Accepting customer orders" else "Hidden from customer map", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    }
                    Switch(checked = isStallOpen, onCheckedChange = onStallOpenChange)
                }
                HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
                OutlinedTextField(value = tempName, onValueChange = { tempName = it }, label = { Text("Stall Name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(value = tempHours, onValueChange = { tempHours = it }, label = { Text("Operating Hours") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(value = tempUpi, onValueChange = { tempUpi = it }, label = { Text("Merchant UPI ID") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(24.dp))
                Button(onClick = { onStallNameChange(tempName); onOperatingHoursChange(tempHours); onUpiIdChange(tempUpi); onBack() }, modifier = Modifier.fillMaxWidth().height(50.dp)) {
                    Text("Save Changes")
                }
            }
        }
    }
}

// ── Batch 4B: Real-Time Vendor Orders Screen ──────────────────────────────────

/**
 * Displays live orders received from Firestore (/orders where vendorId == uid).
 * Each card shows order ID, customer ID, item list, total, timestamp, and a
 * status badge. Action buttons drive the order lifecycle:
 *   PENDING   -> [Accept] -> PREPARING
 *   PREPARING -> [Complete] -> COMPLETED
 *   PENDING   -> [Decline] -> DECLINED
 *
 * Status writes are delegated to [VendorViewModel.updateOrderStatus] which
 * performs a Firestore merge-write on the /orders/{orderId} document.
 */
@Composable
fun VendorOrdersScreen(
    orders: List<Order>,
    onUpdateStatus: (orderId: String, newStatus: String) -> Unit
) {
    if (orders.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Filled.ShoppingCart,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = Color.LightGray
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    "No orders yet",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.Gray
                )
                Text(
                    "New customer orders will appear here in real-time.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.LightGray
                )
            }
        }
        return
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 80.dp)
    ) {
        items(orders, key = { it.orderId }) { order ->
            VendorOrderCard(order = order, onUpdateStatus = onUpdateStatus)
        }
    }
}

/**
 * A single order card rendered inside [VendorOrdersScreen].
 * Renders order details and context-appropriate action buttons based on
 * the current [Order.status].
 */
@Composable
fun VendorOrderCard(
    order: Order,
    onUpdateStatus: (orderId: String, newStatus: String) -> Unit
) {
    val (statusColor, statusLabel) = when (order.status.uppercase()) {
        "PENDING"   -> Pair(Color(0xFFFFA000), "Pending")
        "PREPARING" -> Pair(Color(0xFF1976D2), "Preparing")
        "COMPLETED" -> Pair(Color(0xFF2E7D32), "Completed")
        "DECLINED"  -> Pair(Color(0xFFD32F2F), "Declined")
        else        -> Pair(Color.Gray, order.status)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Order #${order.orderId.takeLast(6).uppercase()}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = statusColor.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = statusLabel,
                        color = statusColor,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(text = "Customer ID: ${order.customerId.take(12)}…", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            Spacer(modifier = Modifier.height(6.dp))
            Text(text = order.items.joinToString(" · "), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            Spacer(modifier = Modifier.height(6.dp))
            Text(text = order.totalAmount, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)

            // Context-sensitive action buttons
            when (order.status.uppercase()) {
                "PENDING" -> {
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { onUpdateStatus(order.orderId, "PREPARING") },
                            modifier = Modifier.weight(1f), shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Accept", fontWeight = FontWeight.Bold)
                        }
                        OutlinedButton(
                            onClick = { onUpdateStatus(order.orderId, "DECLINED") },
                            modifier = Modifier.weight(1f), shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                        ) {
                            Icon(Icons.Filled.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Decline", fontWeight = FontWeight.Bold)
                        }
                    }
                }
                "PREPARING" -> {
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = { onUpdateStatus(order.orderId, "COMPLETED") },
                        modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                    ) {
                        Icon(Icons.Filled.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Mark Complete", fontWeight = FontWeight.Bold)
                    }
                }
            }

            // NEW: Review Display Section
            order.rating?.let { rating ->
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    color = Color(0xFFFFF8E1),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Star, contentDescription = null, tint = Color(0xFFFFB300), modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("$rating/5 Stars", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                        }
                        if (!order.reviewText.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("\"${order.reviewText}\"", style = MaterialTheme.typography.bodySmall, color = Color.DarkGray)
                        }
                    }
                }
            }
        }
    }
}
