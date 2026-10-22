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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.digitalstreetvendornetwork.ui.components.EmptyState
import com.example.digitalstreetvendornetwork.ui.components.OrderProgressTracker
import com.example.digitalstreetvendornetwork.ui.components.SwipeToDeleteContainer
import com.example.digitalstreetvendornetwork.ui.theme.*
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

// Data Models
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

    val tabs = listOf(
        stringResource(R.string.tab_discover),
        stringResource(R.string.tab_favourites),
        stringResource(R.string.tab_my_orders),
        stringResource(R.string.tab_profile)
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
                    onExploreClick = { selectedTab = 0 },
                    onRemoveFavourite = { id -> favouriteVendorIds.remove(id) }
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

    // Cart & Checkout States
    var activeOrderingVendor by remember { mutableStateOf<DiscoverableVendor?>(null) }
    val cartQuantities = remember { mutableStateMapOf<String, Int>() }
    var isCheckoutStep by remember { mutableStateOf(false) } // Toggles between Menu and Checkout views
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
            liveVendors.forEach { vendor ->
                Marker(
                    state = MarkerState(position = LatLng(vendor.lat, vendor.lng)),
                    title = vendor.stallName, snippet = "Live • Open Now",
                    onClick = {
                        selectedVendor = DiscoverableVendor(
                            id = vendor.vendorId,
                            name = vendor.stallName, category = "Live Vendor",
                            products = emptyList(), location = LatLng(vendor.lat, vendor.lng),
                            description = "Live • Open Now"
                        )
                        true
                    }
                )
            }
        }

        // Top Search Bar Overlay
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp).align(Alignment.TopCenter)) {
            OutlinedTextField(
                value = searchQuery, onValueChange = { searchQuery = it }, placeholder = { Text(stringResource(R.string.search_placeholder)) },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = stringResource(R.string.search_placeholder)) },
                trailingIcon = { if (searchQuery.isNotEmpty()) { IconButton(onClick = { searchQuery = "" }) { Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.cd_close)) } } },
                modifier = Modifier.fillMaxWidth().background(Color.White, RoundedCornerShape(32.dp)),
                shape = RoundedCornerShape(32.dp), singleLine = true, colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = Color.Transparent, focusedBorderColor = MaterialTheme.colorScheme.primary)
            )
            Spacer(modifier = Modifier.height(12.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(filters) { filter ->
                    val filterLabel = when (filter) {
                        "All" -> stringResource(R.string.filter_all)
                        "Vegetables" -> stringResource(R.string.category_vegetables)
                        "Fruits" -> stringResource(R.string.category_fruits)
                        "Street Food" -> stringResource(R.string.category_street_food)
                        else -> filter
                    }
                    FilterChip(
                        selected = selectedFilter == filter, onClick = { selectedFilter = filter }, label = { Text(filterLabel) },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = MaterialTheme.colorScheme.primary, selectedLabelColor = Color.White)
                    )
                }
            }
            if (visibleVendors.isEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        EmptyState(
                            icon = Icons.Filled.SearchOff,
                            title = stringResource(R.string.no_vendors_found),
                            message = stringResource(R.string.no_vendors_sub),
                            actionLabel = stringResource(R.string.clear_search),
                            onAction = {
                                searchQuery = ""
                                selectedFilter = "All"
                            }
                        )
                    }
                }
            }
        }

        // Vendor Details Preview Card (when tapping a pin)
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
                        IconButton(onClick = { selectedVendor = null }) { Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.cd_close)) }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = vendor.description, style = MaterialTheme.typography.bodyMedium, color = Color.DarkGray)
                    Spacer(modifier = Modifier.height(14.dp))
                    Button(
                        onClick = {
                            activeOrderingVendor = vendor
                            cartQuantities.clear()
                            isCheckoutStep = false // Start at the Menu view
                            deliveryOption = "Pickup"
                            specialInstructions = ""
                        },
                        modifier = Modifier.fillMaxWidth().height(48.dp), shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(imageVector = Icons.Filled.ShoppingBag, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = stringResource(R.string.view_menu_order), fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }

        // Interactive 2-Step Bottom Sheet (Menu -> Checkout)
        if (activeOrderingVendor != null) {
            val vendor = activeOrderingVendor!!
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

            // 1. Add this state variable right before the ModalBottomSheet
            val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

            // 2. Add the sheetState parameter to the sheet
            ModalBottomSheet(
                onDismissRequest = {
                    activeOrderingVendor = null
                    cartQuantities.clear()
                    isCheckoutStep = false
                },
                sheetState = sheetState, // <-- This forces it to open fully
                containerColor = Color.White,
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
            ) {
                // ... Keep your Column(modifier = Modifier.fillMaxHeight(0.9f)) exactly as it is below here
                Column(modifier = Modifier.fillMaxHeight(0.9f)) {

                    if (!isCheckoutStep) {
                        // ─── STEP 1: MENU VIEW ──────────────────────────────────────
                        Column(modifier = Modifier.weight(1f).padding(horizontal = 20.dp).verticalScroll(rememberScrollState())) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(text = vendor.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                                    Text(text = vendor.description, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                }
                                IconButton(onClick = { activeOrderingVendor = null; cartQuantities.clear() }) { Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.cd_close), tint = Color.Gray) }
                            }
                            Spacer(modifier = Modifier.height(14.dp))
                            HorizontalDivider(color = MenuDivider)
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(text = stringResource(R.string.menu_items), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(8.dp))

                            if (isLoadingMenu) {
                                Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                            } else if (realMenu.isEmpty()) {
                                Text(stringResource(R.string.no_products_yet), modifier = Modifier.padding(vertical = 16.dp), color = Color.Gray)
                            } else {
                                realMenu.forEach { item ->
                                    val quantity = cartQuantities[item.name] ?: 0
                                    Card(
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp), shape = RoundedCornerShape(14.dp),
                                        colors = CardDefaults.cardColors(containerColor = if (quantity > 0) CartItemHighlight.copy(alpha = 0.5f) else CartItemDefault),
                                        elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp)
                                    ) {
                                        Row(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Box(modifier = Modifier.size(14.dp).border(1.2.dp, VegIndicatorGreen, RoundedCornerShape(3.dp)), contentAlignment = Alignment.Center) {
                                                        Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(VegIndicatorGreen))
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
                                                ) { Text(stringResource(R.string.add_to_cart), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium) }
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

                        // Menu Footer: The "Go To Checkout" Button
                        if (totalItems > 0) {
                            Surface(modifier = Modifier.fillMaxWidth(), shadowElevation = 16.dp, color = Color.White) {
                                Column(modifier = Modifier.padding(20.dp)) {
                                    Button(
                                        onClick = { isCheckoutStep = true }, // MOVES TO CHECKOUT VIEW
                                        modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(14.dp), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                    ) {
                                        Text(text = stringResource(R.string.checkout_items_total, totalItems, subtotal), fontSize = MaterialTheme.typography.titleMedium.fontSize, fontWeight = FontWeight.Bold, color = Color.White)
                                    }
                                    Spacer(modifier = Modifier.height(16.dp))
                                }
                            }
                        }

                    } else {
                        // ─── STEP 2: CHECKOUT VIEW ──────────────────────────────────────
                        Column(modifier = Modifier.weight(1f).padding(horizontal = 20.dp).verticalScroll(rememberScrollState())) {
                            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = { isCheckoutStep = false }, modifier = Modifier.padding(end = 8.dp)) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.cd_back), tint = MaterialTheme.colorScheme.primary)
                                }
                                Text(text = stringResource(R.string.order_summary), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.height(14.dp))

                            Text(text = stringResource(R.string.delivery_option), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(8.dp))

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                FilterChip(selected = deliveryOption == "Pickup", onClick = { deliveryOption = "Pickup" }, label = { Text(stringResource(R.string.stall_pickup_free)) }, shape = RoundedCornerShape(12.dp))
                                FilterChip(selected = deliveryOption == "Nearby", onClick = { deliveryOption = "Nearby" }, label = { Text(stringResource(R.string.nearby_delivery_fee)) }, shape = RoundedCornerShape(12.dp))
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            OutlinedTextField(value = specialInstructions, onValueChange = { specialInstructions = it }, placeholder = { Text(stringResource(R.string.special_instructions_hint)) }, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth(), singleLine = true)

                            Spacer(modifier = Modifier.height(24.dp))
                            Text(text = stringResource(R.string.order_summary), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(8.dp))

                            Surface(shape = RoundedCornerShape(14.dp), color = BillSummaryBackground, modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text(stringResource(R.string.items_subtotal, totalItems), style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                                        Text("₹$subtotal", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text(stringResource(R.string.delivery_fee), style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                                        Text(if (deliveryFee == 0) stringResource(R.string.free) else "₹10", style = MaterialTheme.typography.bodyMedium, color = if (deliveryFee == 0) StatusCompleted else Color.DarkGray)
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    HorizontalDivider(color = BillDividerColor, thickness = 0.8.dp)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                        Text(stringResource(R.string.total_payable), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                        Text("₹$totalPayable", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                                    }
                                }
                            }
                        }

                        // Checkout Footer: Final Place Order Button
                        Surface(modifier = Modifier.fillMaxWidth(), shadowElevation = 16.dp, color = Color.White) {
                            Column(modifier = Modifier.padding(20.dp)) {
                                Button(
                                    onClick = {
                                        val itemsSummary = cartQuantities.entries.joinToString(", ") { "${it.value}x ${it.key}" }
                                        val newOrder = CustomerOrder(id = "ORD-${(1000..9999).random()}", vendorName = vendor.name, items = itemsSummary, total = "₹$totalPayable", date = "Today, Just now", status = "Pending")

                                        customerViewModel.placeOrder(vendorId = vendor.id, items = realMenu, quantities = cartQuantities.toMap(), totalAmount = "₹$totalPayable")
                                        onPlaceOrder(newOrder)
                                        activeOrderingVendor = null
                                        cartQuantities.clear()
                                        isCheckoutStep = false
                                    },
                                    modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(14.dp), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                ) {
                                    Icon(Icons.Filled.ShoppingBag, contentDescription = null, tint = Color.White)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(text = stringResource(R.string.place_order), fontSize = MaterialTheme.typography.titleMedium.fontSize, fontWeight = FontWeight.Bold, color = Color.White)
                                }
                                Spacer(modifier = Modifier.height(16.dp))
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
                title = { Text(stringResource(R.string.order_placed_title), fontWeight = FontWeight.Bold) },
                text = { Text(stringResource(R.string.order_placed_msg, "")) },
                confirmButton = { Button(onClick = { showOrderSuccess = false }) { Text(stringResource(R.string.ok)) } }
            )
        }
        orderErrorMessage?.let { errMsg ->
            AlertDialog(
                onDismissRequest = { orderErrorMessage = null },
                icon = { Icon(Icons.Filled.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(48.dp)) },
                title = { Text(stringResource(R.string.order_failed_title), fontWeight = FontWeight.Bold) },
                text = { Text(errMsg) },
                confirmButton = { Button(onClick = { orderErrorMessage = null }) { Text(stringResource(R.string.ok)) } }
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
    var showLanguageDialog by remember { mutableStateOf(false) }

    when (currentSubScreen) {
        "MAIN" -> {
            Column(modifier = Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Spacer(modifier = Modifier.height(24.dp))
                Box(modifier = Modifier.size(100.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer), contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.Person, contentDescription = stringResource(R.string.tab_profile), modifier = Modifier.size(50.dp), tint = MaterialTheme.colorScheme.primary)
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(name, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Text(phone, style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                Spacer(modifier = Modifier.height(32.dp))
                ProfileOptionCard(icon = Icons.Filled.History, title = stringResource(R.string.order_history)) { currentSubScreen = "HISTORY" }
                ProfileOptionCard(icon = Icons.Filled.Settings, title = stringResource(R.string.account_settings)) { currentSubScreen = "SETTINGS" }
                ProfileOptionCard(icon = Icons.Filled.Language, title = stringResource(R.string.language)) { showLanguageDialog = true }
                Spacer(modifier = Modifier.weight(1f))
                Button(
                    onClick = onLogout, modifier = Modifier.fillMaxWidth().height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error), shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = stringResource(R.string.cd_logout))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.logout), fontSize = MaterialTheme.typography.titleMedium.fontSize)
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
            if (showLanguageDialog) {
                LanguageSelectionDialog(onDismiss = { showLanguageDialog = false })
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
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = Color.Gray)
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
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.cd_back), tint = MaterialTheme.colorScheme.primary) }
            Text(stringResource(R.string.order_history), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
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
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.cd_back), tint = MaterialTheme.colorScheme.primary) }
            Text(stringResource(R.string.account_settings), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
        Card(
            modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                OutlinedTextField(
                    value = tempName, onValueChange = { tempName = it }, label = { Text(stringResource(R.string.full_name)) }, singleLine = true,
                    shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = tempPhone, onValueChange = { tempPhone = it }, label = { Text(stringResource(R.string.phone_number)) }, singleLine = true,
                    shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = { if (tempName.isNotBlank() && tempPhone.isNotBlank()) onSave(tempName, tempPhone) },
                    modifier = Modifier.fillMaxWidth().height(50.dp), shape = RoundedCornerShape(12.dp)
                ) { Text(stringResource(R.string.save_changes)) }
            }
        }
    }
}

@Composable
fun CustomerFavouritesScreen(
    favouriteVendorIds: List<String>,
    onExploreClick: () -> Unit,
    onRemoveFavourite: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {
        Text(text = stringResource(R.string.my_favourite_vendors), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(20.dp))
        if (favouriteVendorIds.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                EmptyState(
                    icon = Icons.Filled.FavoriteBorder,
                    title = stringResource(R.string.no_favourites_title),
                    message = stringResource(R.string.no_favourites_msg),
                    actionLabel = stringResource(R.string.explore_vendors),
                    onAction = onExploreClick
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(favouriteVendorIds, key = { it }) { vendorId ->
                    SwipeToDeleteContainer(
                        onDelete = { onRemoveFavourite(vendorId) },
                        icon = Icons.Filled.HeartBroken,
                        modifier = Modifier.animateItem()
                    ) {
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(imageVector = Icons.Filled.Favorite, contentDescription = null, tint = Color.Red)
                                Spacer(modifier = Modifier.width(12.dp))
                                val vendorName = if (vendorId.startsWith("dummy_")) stringResource(R.string.demo_vendor) else stringResource(R.string.live_local_vendor)
                                Column {
                                    Text(text = vendorName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    Text(text = stringResource(R.string.saved_to_favourites), style = MaterialTheme.typography.bodySmall, color = Color.Gray)
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
fun CustomerOrdersScreen(orders: List<CustomerOrder> = emptyList()) {
    val defaultOrders = remember {
        listOf(
            CustomerOrder("ORD001", "Raju Vegetables", "Tomatoes, Potatoes", "₹120", "18 Oct 2026", "Completed"),
            CustomerOrder("ORD002", "Mumbai Chaat Corner", "Pani Puri, Bhel Puri", "₹90", "19 Oct 2026", "Preparing"),
            CustomerOrder("ORD003", "Mumbai Fresh Fruits", "Apples, Bananas", "₹180", "20 Oct 2026", "Pending")
        )
    }
    val displayOrders = if (orders.isNotEmpty()) orders else defaultOrders
    val statusColors = mapOf("Completed" to StatusCompleted, "Preparing" to BrandOrange, "Pending" to StatusPendingNeutral)
    val orderFilters = listOf("All", "Pending", "Preparing", "Completed")
    var selectedOrderFilter by remember { mutableStateOf("All") }
    val filteredOrders = remember(displayOrders, selectedOrderFilter) {
        if (selectedOrderFilter == "All") displayOrders else displayOrders.filter { it.status == selectedOrderFilter }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(text = stringResource(R.string.my_orders), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text(text = stringResource(R.string.demo_orders_notice), style = MaterialTheme.typography.bodySmall, color = Color.Gray)
        Spacer(modifier = Modifier.height(16.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(orderFilters) { filter ->
                val filterLabel = when (filter) {
                    "All" -> stringResource(R.string.filter_all)
                    "Pending" -> stringResource(R.string.status_pending)
                    "Preparing" -> stringResource(R.string.status_preparing)
                    "Completed" -> stringResource(R.string.status_completed)
                    else -> filter
                }
                FilterChip(selected = selectedOrderFilter == filter, onClick = { selectedOrderFilter = filter }, label = { Text(filterLabel) })
            }
        }
        Spacer(modifier = Modifier.height(16.dp))

        if (filteredOrders.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 32.dp),
                contentAlignment = Alignment.Center
            ) {
                EmptyState(
                    icon = Icons.Filled.Inbox,
                    title = stringResource(R.string.no_orders_yet),
                    message = stringResource(R.string.no_orders_match),
                    actionLabel = stringResource(R.string.show_all_orders),
                    onAction = { selectedOrderFilter = "All" }
                )
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(filteredOrders.size) { index ->
                    val order = filteredOrders[index]
                    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text(text = order.id, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                val statusLabel = when (order.status.uppercase()) {
                                    "PENDING" -> stringResource(R.string.status_pending)
                                    "PREPARING" -> stringResource(R.string.status_preparing)
                                    "COMPLETED" -> stringResource(R.string.status_completed)
                                    "DECLINED" -> stringResource(R.string.status_declined)
                                    else -> order.status
                                }
                                Text(text = statusLabel, color = statusColors[order.status] ?: Color.Gray, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(text = order.vendorName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(text = order.items, style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                            Spacer(modifier = Modifier.height(12.dp))
                            OrderProgressTracker(status = order.status)
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
}