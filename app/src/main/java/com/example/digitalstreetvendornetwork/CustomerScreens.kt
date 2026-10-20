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
data class CustomerOrder(val id: String, val vendorName: String, val items: String, val total: String, val date: String, val status: String)
data class MenuItem(val name: String, val price: String)

data class DiscoverableVendor(
    val id: Int,
    val name: String,
    val category: String,
    val products: List<String>,
    val location: LatLng,
    val description: String
)

fun searchVendors(
    vendors: List<DiscoverableVendor>,
    query: String
): List<DiscoverableVendor> {
    val searchText = query.trim()

    if (searchText.isEmpty()) return vendors

    return vendors.filter { vendor ->
        vendor.name.contains(searchText, ignoreCase = true) ||
        vendor.category.contains(searchText, ignoreCase = true) ||
        vendor.products.any {
            it.contains(searchText, ignoreCase = true)
        }
    }
}


@Composable
fun CustomerAppMainScreen(onLogout: () -> Unit) {
    var selectedTab by remember { mutableIntStateOf(0) }

    val favouriteVendorIds = remember {
        mutableStateListOf<Int>()
    }

    val tabs = listOf(
        "Discover",
        "Favourites",
        "My Orders",
        "Profile"
    )

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
                            Icon(
                                imageVector = icon,
                                contentDescription = title
                            )
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
                .padding(innerPadding)
        ) {
            when (selectedTab) {
                0 -> CustomerDiscoverScreen(
                    favouriteVendorIds = favouriteVendorIds
                )
                1 -> CustomerFavouritesScreen(
                    favouriteVendorIds = favouriteVendorIds,
                    onExploreClick = { selectedTab = 0 }
                )
                2 -> CustomerOrdersScreen()
                3 -> CustomerProfileScreen(
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
fun CustomerDiscoverScreen(
    favouriteVendorIds: MutableList<Int>
) {
    val mumbaiLocation = LatLng(19.0760, 72.8777)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(mumbaiLocation, 12f)
    }

    val vendors = remember {
        listOf(
            DiscoverableVendor(
                id = 1,
                name = "Raju Vegetables",
                category = "Vegetables",
                products = listOf("Tomatoes", "Potatoes", "Onions"),
                location = LatLng(19.1070, 72.8377),
                description = "Open till 8PM"
            ),
            DiscoverableVendor(
                id = 2,
                name = "Vada Pav Stall",
                category = "Street Food",
                products = listOf("Vada Pav", "Samosa", "Chai"),
                location = LatLng(19.0860, 72.8877),
                description = "Hot Snacks"
            ),

            DiscoverableVendor(
                id = 3,
                name = "Mumbai Fresh Fruits",
                category = "Fruits",
                products = listOf("Apples", "Bananas", "Oranges", "Mangoes"),
                location = LatLng(19.0990, 72.8460),
                description = "Fresh seasonal fruits"
),
            DiscoverableVendor(
                id = 4,
                name = "Green Basket Vegetables",
                category = "Vegetables",
                products = listOf("Spinach", "Carrots", "Cauliflower", "Tomatoes"),
                location = LatLng(19.1120, 72.8620),
                description = "Fresh vegetables at affordable prices"
),
            DiscoverableVendor(
                id = 5,
                name = "Mumbai Chaat Corner",
                category = "Street Food",
                products = listOf("Pani Puri", "Bhel Puri", "Sev Puri"),
                location = LatLng(19.0830, 72.8500),
                description = "Authentic Mumbai street food"
)

        )
    }

    var searchQuery by remember { mutableStateOf("") }

    val filters = listOf(
        "All",
        "Vegetables",
        "Fruits",
        "Street Food"
    )

    var selectedFilter by remember {
        mutableStateOf("All")
    }

    val searchedVendors = remember(vendors, searchQuery) {
        searchVendors(vendors, searchQuery)
    }

    val visibleVendors = searchedVendors.filter { vendor ->
        selectedFilter == "All" ||
        vendor.category == selectedFilter
    }


    var selectedVendor by remember {
        mutableStateOf<DiscoverableVendor?>(null)
    }

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

visibleVendors.forEach { vendor ->
    Marker(
        state = MarkerState(position = vendor.location),
        title = vendor.name,
        snippet = vendor.description,
        onClick = {
            selectedVendor = vendor
            true
        }
    )
}

        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .align(Alignment.TopCenter)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = {
                    Text("Search for vendors or items...")
                },
                leadingIcon = {
                    Icon(
                        Icons.Filled.Search,
                        contentDescription = "Search"
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(
                            onClick = { searchQuery = "" }
                        ) {
                            Icon(
                                Icons.Filled.Close,
                                contentDescription = "Clear search"
                            )
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Color.White,
                        RoundedCornerShape(32.dp)
                    ),
                shape = RoundedCornerShape(32.dp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = Color.Transparent,
                    focusedBorderColor =
                        MaterialTheme.colorScheme.primary
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filters) { filter ->
                    FilterChip(
                        selected = selectedFilter == filter,
                        onClick = {
                            selectedFilter = filter
                        },
                        label = { Text(filter) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor =
                                MaterialTheme.colorScheme.primary,
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }

            if (visibleVendors.isEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White
                    )
                ) {
                    Text(
                        text = "No vendors found. Try another search.",
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }


        selectedVendor?.let { vendor ->
            Card(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(
                    defaultElevation = 8.dp
                ),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = vendor.name,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = vendor.category,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }


IconButton(
    onClick = {
        if (vendor.id in favouriteVendorIds) {
            favouriteVendorIds.remove(vendor.id)
        } else {
            favouriteVendorIds.add(vendor.id)
        }
    }
) {
    val isFavourite = vendor.id in favouriteVendorIds

    Icon(
        imageVector = if (isFavourite) {
            Icons.Filled.Favorite
        } else {
            Icons.Filled.FavoriteBorder
        },
        contentDescription = if (isFavourite) {
            "Remove from favourites"
        } else {
            "Add to favourites"
        },
        tint = if (isFavourite) Color.Red else Color.Gray
    )
}

                        IconButton(onClick = { selectedVendor = null }) {
                            Icon(
                                Icons.Filled.Close,
                                contentDescription = "Close vendor details"
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(text = vendor.description)

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Available Products",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    vendor.products.forEach { product ->
                        Text(
                            text = "• $product",
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
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
        CustomerOrder("ORD001", "Raju's Fresh Vends", "2x Vada Pav, 1x Chai", "₹40", "Today, 10:30 AM", "Delivered"),
        CustomerOrder("ORD002", "Gopi Vegetables", "1kg Tomatoes, 500g Onions", "₹120", "Yesterday", "Picked Up")
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
@Composable
fun CustomerFavouritesScreen(
    favouriteVendorIds: List<Int>,
    onExploreClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
    ) {
        Text(
            text = "My Favourite Vendors",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(20.dp))

        if (favouriteVendorIds.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Filled.FavoriteBorder,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = Color.Gray
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text("No favourite vendors yet!")

                Spacer(modifier = Modifier.height(16.dp))

                Button(onClick = onExploreClick) {
                    Text("Explore Vendors")
                }
            }
        } else {
            favouriteVendorIds.forEach { vendorId ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Favorite,
                            contentDescription = null,
                            tint = Color.Red
                        )

                        Spacer(modifier = Modifier.width(12.dp))


val vendorName = when (vendorId) {
    1 -> "Raju Vegetables"
    2 -> "Vada Pav Stall"
    3 -> "Mumbai Fresh Fruits"
    4 -> "Green Basket Vegetables"
    5 -> "Mumbai Chaat Corner"
    else -> "Unknown Vendor"
}

Column {
    Text(
        text = vendorName,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold
    )

    Text(
        text = "Saved to Favourites",
        style = MaterialTheme.typography.bodySmall,
        color = Color.Gray
    )
}
                    }

                }
            }
}
    }
}

@Composable
fun CustomerOrdersScreen() {
    val orders = remember {
        listOf(
            CustomerOrder(
                "ORD001",
                "Raju Vegetables",
                "Tomatoes, Potatoes",
                "₹120",
                "18 Oct 2026",
                "Completed"
            ),
            CustomerOrder(
                "ORD002",
                "Mumbai Chaat Corner",
                "Pani Puri, Bhel Puri",
                "₹90",
                "19 Oct 2026",
                "Preparing"
            ),
            CustomerOrder(
                "ORD003",
                "Mumbai Fresh Fruits",
                "Apples, Bananas",
                "₹180",
                "20 Oct 2026",
                "Pending"
            )
        )
    }

    val statusColors = mapOf(
        "Completed" to Color(0xFF2E7D32),
        "Preparing" to Color(0xFFE65100),
        "Pending" to Color(0xFF757575)
    )

val orderFilters = listOf(
    "All",
    "Pending",
    "Preparing",
    "Completed"
)

var selectedOrderFilter by remember {
    mutableStateOf("All")
}

val filteredOrders = remember(orders, selectedOrderFilter) {
    if (selectedOrderFilter == "All") {
        orders
    } else {
        orders.filter { order ->
            order.status == selectedOrderFilter
        }
    }
}


    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "My Orders",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = "Demo orders — sample data only",
            style = MaterialTheme.typography.bodySmall,
            color = Color.Gray
        )

Spacer(modifier = Modifier.height(16.dp))

LazyRow(
    horizontalArrangement = Arrangement.spacedBy(8.dp)
) {
    items(orderFilters) { filter ->
        FilterChip(
            selected = selectedOrderFilter == filter,
            onClick = {
                selectedOrderFilter = filter
            },
            label = {
                Text(filter)
            }
        )
    }
}

        Spacer(modifier = Modifier.height(16.dp))

if (filteredOrders.isEmpty()) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Filled.Inbox,
            contentDescription = null,
            modifier = Modifier.size(56.dp),
            tint = Color.Gray
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "No orders found",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = "No orders match the selected status.",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Gray
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = { selectedOrderFilter = "All" }
        ) {
            Text("Show All Orders")
        }
    }
}

        androidx.compose.foundation.lazy.LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(filteredOrders.size) { index ->
                val order = filteredOrders[index]

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(
                        defaultElevation = 2.dp
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement =
                                Arrangement.SpaceBetween,
                            verticalAlignment =
                                Alignment.CenterVertically
                        ) {
                            Text(
                                text = order.id,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )

                            Text(
                                text = order.status,
                                color = statusColors[order.status]
                                    ?: Color.Gray,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = order.vendorName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = order.items,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Gray
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        HorizontalDivider()

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement =
                                Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = order.date,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray
                            )

                            Text(
                                text = order.total,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }
}
