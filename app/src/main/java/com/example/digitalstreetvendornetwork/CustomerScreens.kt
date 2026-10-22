package com.example.digitalstreetvendornetwork

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.digitalstreetvendornetwork.viewmodel.ActiveVendor
import com.example.digitalstreetvendornetwork.viewmodel.CustomerViewModel
import com.example.digitalstreetvendornetwork.viewmodel.OrderPlacementResult
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.firebase.firestore.FirebaseFirestore

// Data Model for Orders
data class CustomerOrder(val id: String, val vendorName: String, val items: String, val total: String, val date: String, val status: String)
data class MenuItem(val name: String, val price: String)

data class VendorMenuItem(
    val id: Int,
    val name: String,
    val description: String,
    val price: Int,
    val isVeg: Boolean = true
)

fun getVendorMenu(category: String): List<VendorMenuItem> {
    return when (category) {
        "Street Food" -> listOf(
            VendorMenuItem(1, "Fresh Vada Pav", "Hot Mumbai style vada pav with spicy green chutney", 15),
            VendorMenuItem(2, "Cutting Chai", "Strong ginger tea, brewed fresh with cardamom", 10),
            VendorMenuItem(3, "Crispy Samosa", "Crisp spiced potato pastry with tamarind chutney", 20),
            VendorMenuItem(4, "Special Misal Pav", "Spicy sprout curry topped with farsan and soft pav", 40)
        )
        "Vegetables" -> listOf(
            VendorMenuItem(5, "Farm Fresh Tomatoes (1kg)", "Locally sourced ripe juicy tomatoes", 40),
            VendorMenuItem(6, "Fresh Potatoes (1kg)", "Grade-A washed cooking potatoes", 30),
            VendorMenuItem(7, "Red Onions (1kg)", "Crisp high-grade red onions", 35),
            VendorMenuItem(8, "Organic Spinach (Palak)", "Fresh leafy green bunch", 25)
        )
        "Fruits" -> listOf(
            VendorMenuItem(9, "Shimla Apples (500g)", "Crisp sweet Himalayan apples", 80),
            VendorMenuItem(10, "Robusta Bananas (1 Dozen)", "Naturally ripened sweet bananas", 50),
            VendorMenuItem(11, "Nagpur Oranges (1kg)", "Juicy sweet citrus oranges", 60),
            VendorMenuItem(12, "Fresh Papaya (1 pc)", "Ripe golden sweet papaya", 45)
        )
        else -> listOf(
            VendorMenuItem(13, "Mumbai Pani Puri (6 pcs)", "Crispy puris with tangy spiced water", 30),
            VendorMenuItem(14, "Sukha Bhel Puri", "Crispy puffed rice with peanuts & chutneys", 35),
            VendorMenuItem(15, "Special Sev Puri", "Puris loaded with potato, onions and fine sev", 40),
            VendorMenuItem(16, "Dahi Puri", "Puris filled with sweet spiced curd & pomegranate", 50)
        )
    }
}

// CHANGED: id is now a String so it aligns with Firebase UIDs
data class DiscoverableVendor(
    val id: String,
    val name: String,
    val category: String,
    val products: List<String>,
    val location: LatLng,
    val description: String
)

fun searchVendors(vendors: List<DiscoverableVendor>, query: String): List<DiscoverableVendor> {
    val searchText = query.trim()
    if (searchText.isEmpty()) return vendors
    return vendors.filter { vendor ->
        vendor.name.contains(searchText, ignoreCase = true) ||
                vendor.category.contains(searchText, ignoreCase = true) ||
                vendor.products.any { it.contains(searchText, ignoreCase = true) }
    }
}

@Composable
fun CustomerAppMainScreen(onLogout: () -> Unit) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val favouriteVendorIds = remember { mutableStateListOf<String>() }

    val customerOrders = remember {
        mutableStateListOf(
            CustomerOrder("ORD-4081", "Raju's Fresh Vends", "2x Fresh Vada Pav, 1x Cutting Chai", "₹40", "Today, 10:30 AM", "Preparing"),
            CustomerOrder("ORD-4075", "Mumbai Chaat Corner", "1x Pani Puri, 1x Bhel Puri", "₹65", "Yesterday", "Completed"),
            CustomerOrder("ORD-4062", "Mumbai Fresh Fruits", "1kg Shimla Apples", "₹160", "18 Oct 2026", "Completed")
        )
    }

    val tabs = listOf("Discover", "Favourites", "My Orders", "Profile")
    var customerName by remember { mutableStateOf("Local Shopper") }
    var customerPhone by remember { mutableStateOf("+91 9876543210") }

    Scaffold(
        bottomBar = {
            NavigationBar {
                tabs.forEachIndexed { index, title ->
                    NavigationBarItem(
                        icon = {
                            val icon = when (index) {
                                0 -> Icons.Filled.Search
                                1 -> Icons.Filled.Favorite
                                2 -> Icons.Filled.List
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
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            when (selectedTab) {
                0 -> CustomerDiscoverScreen(
                    favouriteVendorIds = favouriteVendorIds,
                    onPlaceOrder = { newOrder -> customerOrders.add(0, newOrder) }
                )
                1 -> CustomerFavouritesScreen(
                    favouriteVendorIds = favouriteVendorIds,
                    onExploreClick = { selectedTab = 0 }
                )
                2 -> CustomerOrdersScreen(orders = customerOrders)
                3 -> CustomerProfileScreen(
                    name = customerName, phone = customerPhone,
                    onNameChange = { customerName = it }, onPhoneChange = { customerPhone = it },
                    onLogout = onLogout
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerDiscoverScreen(favouriteVendorIds: MutableList<String>, onPlaceOrder: (CustomerOrder) -> Unit = {}) {
    val customerViewModel: CustomerViewModel = viewModel()
    val liveVendors by customerViewModel.activeVendors.collectAsState()
    val orderPlacementResult by customerViewModel.orderPlacementResult.collectAsState()

    val mumbaiLocation = LatLng(19.0760, 72.8777)
    val cameraPositionState = rememberCameraPositionState { position = CameraPosition.fromLatLngZoom(mumbaiLocation, 12f) }

    val vendors = remember {
        listOf(
            DiscoverableVendor("dummy_1", "Raju Vegetables", "Vegetables", listOf("Tomatoes", "Potatoes", "Onions"), LatLng(19.1070, 72.8377), "Open till 8PM"),
            DiscoverableVendor("dummy_2", "Vada Pav Stall", "Street Food", listOf("Vada Pav", "Samosa", "Chai"), LatLng(19.0860, 72.8877), "Hot Snacks"),
            DiscoverableVendor("dummy_3", "Mumbai Fresh Fruits", "Fruits", listOf("Apples", "Bananas", "Oranges", "Mangoes"), LatLng(19.0990, 72.8460), "Fresh seasonal fruits"),
            DiscoverableVendor("dummy_4", "Green Basket Vegetables", "Vegetables", listOf("Spinach", "Carrots", "Cauliflower", "Tomatoes"), LatLng(19.1120, 72.8620), "Fresh vegetables at affordable prices"),
            DiscoverableVendor("dummy_5", "Mumbai Chaat Corner", "Street Food", listOf("Pani Puri", "Bhel Puri", "Sev Puri"), LatLng(19.0830, 72.8500), "Authentic Mumbai street food")
        )
    }

    var searchQuery by remember { mutableStateOf("") }
    val filters = listOf("All", "Vegetables", "Fruits", "Street Food")
    var selectedFilter by remember { mutableStateOf("All") }
    val searchedVendors = remember(vendors, searchQuery) { searchVendors(vendors, searchQuery) }
    val visibleVendors = searchedVendors.filter { vendor -> selectedFilter == "All" || vendor.category == selectedFilter }

    var selectedVendor by remember { mutableStateOf<DiscoverableVendor?>(null) }
    var activeOrderingVendor by remember { mutableStateOf<DiscoverableVendor?>(null) }
    val cartQuantities = remember { mutableStateMapOf<String, Int>() }
    var deliveryOption by remember { mutableStateOf("Pickup") }
    var specialInstructions by remember { mutableStateOf("") }
    var showOrderSuccess by remember { mutableStateOf(false) }
    var orderErrorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(orderPlacementResult) {
        when (val result = orderPlacementResult) {
            is OrderPlacementResult.Success -> {
                showOrderSuccess = true
                customerViewModel.clearOrderResult()
            }
            is OrderPlacementResult.Error -> {
                orderErrorMessage = result.message
                customerViewModel.clearOrderResult()
            }
            OrderPlacementResult.Idle -> Unit
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        GoogleMap(modifier = Modifier.fillMaxSize(), cameraPositionState = cameraPositionState) {
            visibleVendors.forEach { vendor ->
                Marker(
                    state = MarkerState(position = vendor.location),
                    title = vendor.name, snippet = vendor.description,
                    onClick = { selectedVendor = vendor; true }
                )
            }
            // FIRESTORE SYNC: Drawing and attaching live Firebase markers
            liveVendors.forEach { vendor ->
                Marker(
                    state = MarkerState(position = LatLng(vendor.lat, vendor.lng)),
                    title = vendor.stallName, snippet = "Live • Open Now",
                    onClick = {
                        selectedVendor = DiscoverableVendor(
                            id = vendor.vendorId, // Passes the actual Firebase document ID
                            name = vendor.stallName, category = "Live Vendor",
                            products = emptyList(), location = LatLng(vendor.lat, vendor.lng),
                            description = "Live • Open Now"
                        )
                        true
                    }
                )
            }
        }

        Column(modifier = Modifier.fillMaxWidth().padding(16.dp).align(Alignment.TopCenter)) {
            OutlinedTextField(
                value = searchQuery, onValueChange = { searchQuery = it }, placeholder = { Text("Search for vendors or items...") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = "Search") },
                trailingIcon = { if (searchQuery.isNotEmpty()) { IconButton(onClick = { searchQuery = "" }) { Icon(Icons.Filled.Close, contentDescription = "Clear search") } } },
                modifier = Modifier.fillMaxWidth().background(Color.White, RoundedCornerShape(32.dp)),
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
            if (visibleVendors.isEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                    Text(text = "No vendors found. Try another search.", modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.bodyMedium)
                }
            }
        }

        selectedVendor?.let { vendor ->
            Card(
                modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(16.dp),
                shape = RoundedCornerShape(20.dp), elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = vendor.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            Text(text = vendor.category, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Medium)
                        }
                        IconButton(onClick = { if (vendor.id in favouriteVendorIds) favouriteVendorIds.remove(vendor.id) else favouriteVendorIds.add(vendor.id) }) {
                            val isFavourite = vendor.id in favouriteVendorIds
                            Icon(imageVector = if (isFavourite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder, contentDescription = null, tint = if (isFavourite) Color.Red else Color.Gray)
                        }
                        IconButton(onClick = { selectedVendor = null }) { Icon(Icons.Filled.Close, contentDescription = "Close vendor details") }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = vendor.description, style = MaterialTheme.typography.bodyMedium, color = Color.DarkGray)
                    Spacer(modifier = Modifier.height(14.dp))
                    Button(
                        onClick = {
                            activeOrderingVendor = vendor
                            cartQuantities.clear()
                            deliveryOption = "Pickup"
                            specialInstructions = ""
                        },
                        modifier = Modifier.fillMaxWidth().height(48.dp), shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(imageVector = Icons.Filled.ShoppingBag, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "View Menu & Order", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }

        if (activeOrderingVendor != null) {
            val vendor = activeOrderingVendor!!

            // FIRESTORE FETCH: Grab real catalogue items for the tapped vendor
            var realMenu by remember { mutableStateOf<List<VendorMenuItem>>(emptyList()) }
            var isLoadingMenu by remember { mutableStateOf(true) }

            LaunchedEffect(vendor.id) {
                isLoadingMenu = true
                if (vendor.id.startsWith("dummy")) {
                    realMenu = getVendorMenu(vendor.category)
                    isLoadingMenu = false
                } else {
                    FirebaseFirestore.getInstance().collection("vendors").document(vendor.id).collection("products").get()
                        .addOnSuccessListener { snapshot ->
                            realMenu = snapshot.documents.mapNotNull { doc ->
                                val name = doc.getString("name") ?: return@mapNotNull null
                                val priceStr = doc.getString("price") ?: "0"
                                val priceInt = priceStr.replace(Regex("[^0-9]"), "").toIntOrNull() ?: 0
                                val desc = doc.getString("description") ?: ""
                                val isAvail = doc.getBoolean("isAvailable") ?: true
                                if (isAvail) VendorMenuItem(id = doc.id.hashCode(), name = name, description = desc, price = priceInt) else null
                            }
                            isLoadingMenu = false
                        }
                        .addOnFailureListener { isLoadingMenu = false }
                }
            }

            val totalItems = cartQuantities.values.sum()
            val subtotal = cartQuantities.entries.sumOf { (name, count) -> val item = realMenu.firstOrNull { it.name == name }; (item?.price ?: 0) * count }
            val deliveryFee = if (deliveryOption == "Nearby") 10 else 0
            val totalPayable = subtotal + deliveryFee

            ModalBottomSheet(
                onDismissRequest = { activeOrderingVendor = null; cartQuantities.clear() },
                containerColor = Color.White, shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
            ) {
                // FIXED: Tall parent box with a flexible top (scrollable menu) and a rigid bottom (Sticky Footer Checkout)
                Column(modifier = Modifier.fillMaxHeight(0.9f)) {

                    // Top Section: Header & Scrollable List
                    Column(modifier = Modifier.weight(1f).padding(horizontal = 20.dp).verticalScroll(rememberScrollState())) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = vendor.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 2.dp)) {
                                    Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
                                        Text(text = vendor.category, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp))
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(text = "• ${vendor.description}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                }
                            }
                            IconButton(onClick = { activeOrderingVendor = null; cartQuantities.clear() }) { Icon(Icons.Filled.Close, contentDescription = "Close", tint = Color.Gray) }
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                        HorizontalDivider(color = Color(0xFFF0F0F0))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(text = "Select Items", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))

                        if (isLoadingMenu) {
                            Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                        } else if (realMenu.isEmpty()) {
                            Text("No items available in this catalogue right now.", modifier = Modifier.padding(vertical = 16.dp), color = Color.Gray)
                        } else {
                            realMenu.forEach { item ->
                                val quantity = cartQuantities[item.name] ?: 0
                                Card(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp), shape = RoundedCornerShape(14.dp),
                                    colors = CardDefaults.cardColors(containerColor = if (quantity > 0) Color(0xFFFFF8E1).copy(alpha = 0.5f) else Color(0xFFFAFAFA)),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp)
                                ) {
                                    Row(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Box(modifier = Modifier.size(14.dp).border(1.2.dp, Color(0xFF2E7D32), RoundedCornerShape(3.dp)), contentAlignment = Alignment.Center) {
                                                    Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(Color(0xFF2E7D32)))
                                                }
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(text = item.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                            }
                                            Spacer(modifier = Modifier.height(3.dp))
                                            Text(text = item.description, style = MaterialTheme.typography.bodySmall, color = Color.Gray, maxLines = 2)
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Text(text = "₹${item.price}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        if (quantity == 0) {
                                            OutlinedButton(
                                                onClick = { cartQuantities[item.name] = 1 }, shape = RoundedCornerShape(12.dp),
                                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                                                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                                            ) { Text("ADD +", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium) }
                                        } else {
                                            Row(modifier = Modifier.clip(RoundedCornerShape(20.dp)).background(MaterialTheme.colorScheme.primary).padding(horizontal = 4.dp, vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                                                IconButton(onClick = { if (quantity <= 1) cartQuantities.remove(item.name) else cartQuantities[item.name] = quantity - 1 }, modifier = Modifier.size(28.dp)) {
                                                    Icon(imageVector = Icons.Filled.Remove, contentDescription = "Decrease", tint = Color.White, modifier = Modifier.size(16.dp))
                                                }
                                                Text(text = "$quantity", fontWeight = FontWeight.Bold, color = Color.White, modifier = Modifier.padding(horizontal = 8.dp), style = MaterialTheme.typography.bodyMedium)
                                                IconButton(onClick = { cartQuantities[item.name] = quantity + 1 }, modifier = Modifier.size(28.dp)) {
                                                    Icon(imageVector = Icons.Filled.Add, contentDescription = "Increase", tint = Color.White, modifier = Modifier.size(16.dp))
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Bottom Section: Sticky Footer Checkout
                    if (totalItems > 0) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shadowElevation = 16.dp,
                            color = Color.White
                        ) {
                            Column(modifier = Modifier.padding(20.dp)) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    FilterChip(selected = deliveryOption == "Pickup", onClick = { deliveryOption = "Pickup" }, label = { Text("Stall Pickup (Free)") }, shape = RoundedCornerShape(12.dp))
                                    FilterChip(selected = deliveryOption == "Nearby", onClick = { deliveryOption = "Nearby" }, label = { Text("Nearby Delivery (+₹10)") }, shape = RoundedCornerShape(12.dp))
                                }
                                Spacer(modifier = Modifier.height(10.dp))
                                OutlinedTextField(value = specialInstructions, onValueChange = { specialInstructions = it }, placeholder = { Text("Special instructions (e.g. extra spicy)") }, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth(), singleLine = true)
                                Spacer(modifier = Modifier.height(16.dp))
                                Button(
                                    onClick = {
                                        val itemsSummary = cartQuantities.entries.joinToString(", ") { "${it.value}x ${it.key}" }
                                        val newOrder = CustomerOrder(id = "ORD-${(1000..9999).random()}", vendorName = vendor.name, items = itemsSummary, total = "₹$totalPayable", date = "Today, Just now", status = "Pending")

                                        customerViewModel.placeOrder(vendorId = vendor.id, items = realMenu, quantities = cartQuantities.toMap(), totalAmount = "₹$totalPayable")
                                        onPlaceOrder(newOrder)
                                        activeOrderingVendor = null
                                        cartQuantities.clear()
                                    },
                                    modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(14.dp), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                ) {
                                    Icon(Icons.Filled.ShoppingBag, contentDescription = null, tint = Color.White)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(text = "Place Order • ₹$totalPayable", fontSize = MaterialTheme.typography.titleMedium.fontSize, fontWeight = FontWeight.Bold, color = Color.White)
                                }
                                Spacer(modifier = Modifier.height(16.dp)) // padding for device bottom bar
                            }
                        }
                    }
                }
            }
        }

        if (showOrderSuccess) {
            AlertDialog(
                onDismissRequest = { showOrderSuccess = false },
                icon = { Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(48.dp)) },
                title = { Text("Order Placed Successfully!", fontWeight = FontWeight.Bold) },
                text = { Text("Your order has been written to Firestore and the vendor has received your request. Track its status in the ‘My Orders’ tab.") },
                confirmButton = { Button(onClick = { showOrderSuccess = false }) { Text("Awesome") } }
            )
        }

        orderErrorMessage?.let { errMsg ->
            AlertDialog(
                onDismissRequest = { orderErrorMessage = null },
                icon = { Icon(Icons.Filled.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(48.dp)) },
                title = { Text("Order Failed", fontWeight = FontWeight.Bold) },
                text = { Text(errMsg) },
                confirmButton = { Button(onClick = { orderErrorMessage = null }) { Text("OK") } }
            )
        }
    }
}

// ... [The rest of the file (Profile, Favorites, Orders) remains exactly the same] ...
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerProfileScreen(
    name: String, phone: String, onNameChange: (String) -> Unit, onPhoneChange: (String) -> Unit, onLogout: () -> Unit
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
            onSave = { newName, newPhone -> onNameChange(newName); onPhoneChange(newPhone); currentSubScreen = "MAIN" },
            onBack = { currentSubScreen = "MAIN" }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileOptionCard(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, onClick: () -> Unit) {
    Card(
        onClick = onClick, modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp), shape = RoundedCornerShape(12.dp)
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
        CustomerOrder("ORD001", "Raju's Fresh Vends", "2x Vada Pav, 1x Chai", "₹40", "Today, 10:30 AM", "Delivered"),
        CustomerOrder("ORD002", "Gopi Vegetables", "1kg Tomatoes, 500g Onions", "₹120", "Yesterday", "Picked Up")
    )
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
            IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.primary) }
            Text("Order History", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(orders) { order ->
                Card(
                    modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp),
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
fun CustomerSettingsScreen(currentName: String, currentPhone: String, onSave: (String, String) -> Unit, onBack: () -> Unit) {
    var tempName by remember { mutableStateOf(currentName) }
    var tempPhone by remember { mutableStateOf(currentPhone) }
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
            IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.primary) }
            Text("Account Settings", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
        Card(
            modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                OutlinedTextField(
                    value = tempName, onValueChange = { tempName = it }, label = { Text("Full Name") }, singleLine = true,
                    shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = tempPhone, onValueChange = { tempPhone = it }, label = { Text("Phone Number") }, singleLine = true,
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

@Composable
fun CustomerFavouritesScreen(favouriteVendorIds: List<String>, onExploreClick: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {
        Text(text = "My Favourite Vendors", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(20.dp))
        if (favouriteVendorIds.isEmpty()) {
            Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(imageVector = Icons.Filled.FavoriteBorder, contentDescription = null, modifier = Modifier.size(64.dp), tint = Color.Gray)
                Spacer(modifier = Modifier.height(12.dp))
                Text("No favourite vendors yet!")
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = onExploreClick) { Text("Explore Vendors") }
            }
        } else {
            favouriteVendorIds.forEach { vendorId ->
                Card(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Filled.Favorite, contentDescription = null, tint = Color.Red)
                        Spacer(modifier = Modifier.width(12.dp))
                        val vendorName = if (vendorId.startsWith("dummy_")) "Demo Vendor" else "Live Local Vendor"
                        Column {
                            Text(text = vendorName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(text = "Saved to Favourites", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CustomerOrdersScreen(orders: List<CustomerOrder> = emptyList()) {
    val defaultOrders = remember {
        listOf(
            CustomerOrder("ORD001", "Raju Vegetables", "Tomatoes, Potatoes", "₹120", "18 Oct 2026", "Completed"),
            CustomerOrder("ORD002", "Mumbai Chaat Corner", "Pani Puri, Bhel Puri", "₹90", "19 Oct 2026", "Preparing"),
            CustomerOrder("ORD003", "Mumbai Fresh Fruits", "Apples, Bananas", "₹180", "20 Oct 2026", "Pending")
        )
    }
    val displayOrders = if (orders.isNotEmpty()) orders else defaultOrders
    val statusColors = mapOf("Completed" to Color(0xFF2E7D32), "Preparing" to Color(0xFFE65100), "Pending" to Color(0xFF757575))
    val orderFilters = listOf("All", "Pending", "Preparing", "Completed")
    var selectedOrderFilter by remember { mutableStateOf("All") }
    val filteredOrders = remember(displayOrders, selectedOrderFilter) {
        if (selectedOrderFilter == "All") displayOrders else displayOrders.filter { it.status == selectedOrderFilter }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(text = "My Orders", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text(text = "Demo orders — sample data only", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
        Spacer(modifier = Modifier.height(16.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(orderFilters) { filter ->
                FilterChip(selected = selectedOrderFilter == filter, onClick = { selectedOrderFilter = filter }, label = { Text(filter) })
            }
        }
        Spacer(modifier = Modifier.height(16.dp))

        if (filteredOrders.isEmpty()) {
            Column(modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(imageVector = Icons.Filled.Inbox, contentDescription = null, modifier = Modifier.size(56.dp), tint = Color.Gray)
                Spacer(modifier = Modifier.height(12.dp))
                Text(text = "No orders found", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(text = "No orders match the selected status.", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = { selectedOrderFilter = "All" }) { Text("Show All Orders") }
            }
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(filteredOrders.size) { index ->
                val order = filteredOrders[index]
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text(text = order.id, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Text(text = order.status, color = statusColors[order.status] ?: Color.Gray, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(text = order.vendorName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = order.items, style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider()
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(text = order.date, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                            Text(text = order.total, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        }
    }
}