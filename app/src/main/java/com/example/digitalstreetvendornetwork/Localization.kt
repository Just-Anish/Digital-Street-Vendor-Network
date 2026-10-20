package com.example.digitalstreetvendornetwork

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog

enum class AppLanguage(
    val code: String,
    val displayName: String,
    val nativeName: String,
    val badgeText: String
) {
    ENGLISH("en", "English", "English", "EN"),
    HINDI("hi", "Hindi", "हिंदी", "हि"),
    MARATHI("mr", "Marathi", "मराठी", "मरा")
}

data class AppTranslation(
    val appName: String,
    val tagline: String,
    val vendor: String,
    val customer: String,
    val phoneNumber: String,
    val password: String,
    val fullName: String,
    val secureLogin: String,
    val createAccount: String,
    val createNewAccount: String,
    val alreadyHaveAccount: String,
    val selectLanguage: String,
    val changeLanguage: String,
    val languageSubtitle: String,

    // Vendor Navigation & Tabs
    val catalogue: String,
    val liveLocation: String,
    val orders: String,
    val profile: String,

    // Customer Navigation & Tabs
    val discover: String,
    val favourites: String,
    val myOrders: String,

    // Vendor Stall & Items
    val stallOpen: String,
    val stallClosed: String,
    val openStall: String,
    val closeStall: String,
    val addItem: String,
    val editItem: String,
    val deleteItem: String,
    val itemName: String,
    val itemPrice: String,
    val inStock: String,
    val outOfStock: String,
    val save: String,
    val cancel: String,

    // Vendor Order Management
    val pending: String,
    val preparing: String,
    val completed: String,
    val all: String,
    val acceptOrder: String,
    val markReady: String,
    val completeOrder: String,
    val incomingOrders: String,
    val simulateOrder: String,
    val noOrders: String,
    val totalEarnings: String,
    val todaysOrders: String,

    // Cart & Checkout
    val cart: String,
    val addToCart: String,
    val viewCart: String,
    val checkout: String,
    val placeOrder: String,
    val itemTotal: String,
    val taxesPackaging: String,
    val toPay: String,
    val pickupAtStall: String,
    val nearbyDelivery: String,
    val specialInstructions: String,
    val orderPlacedSuccess: String,
    val searchVendors: String,
    val nearbyVendors: String,

    // Profile & Settings
    val logout: String,
    val language: String,
    val upiId: String,
    val operatingHours: String,
    val editProfile: String
)

val EnglishTranslations = AppTranslation(
    appName = "Local Vends",
    tagline = "Your neighbourhood market, digitized.",
    vendor = "Vendor",
    customer = "Customer",
    phoneNumber = "Phone Number",
    password = "Password",
    fullName = "Full Name",
    secureLogin = "Secure Login",
    createAccount = "Create Account",
    createNewAccount = "Create a new account",
    alreadyHaveAccount = "Already have an account? Login",
    selectLanguage = "Select Language",
    changeLanguage = "Change Language",
    languageSubtitle = "Choose your preferred language for the app",

    catalogue = "Catalogue",
    liveLocation = "Live Location",
    orders = "Orders",
    profile = "Profile",

    discover = "Discover",
    favourites = "Favourites",
    myOrders = "My Orders",

    stallOpen = "Stall Open",
    stallClosed = "Stall Closed",
    openStall = "Open Stall",
    closeStall = "Close Stall",
    addItem = "Add Item",
    editItem = "Edit Item",
    deleteItem = "Delete Item",
    itemName = "Item Name",
    itemPrice = "Price (₹)",
    inStock = "In Stock",
    outOfStock = "Out of Stock",
    save = "Save",
    cancel = "Cancel",

    pending = "Pending",
    preparing = "Preparing",
    completed = "Completed",
    all = "All",
    acceptOrder = "Accept Order",
    markReady = "Mark Ready",
    completeOrder = "Complete Order",
    incomingOrders = "Incoming Orders",
    simulateOrder = "+ Test Incoming",
    noOrders = "No orders found",
    totalEarnings = "Total Earnings",
    todaysOrders = "Today's Orders",

    cart = "Your Cart",
    addToCart = "Add",
    viewCart = "View Cart",
    checkout = "Checkout",
    placeOrder = "Confirm & Place Order",
    itemTotal = "Item Total",
    taxesPackaging = "Packaging & Handling",
    toPay = "Total Amount",
    pickupAtStall = "Stall Pickup",
    nearbyDelivery = "Nearby Delivery",
    specialInstructions = "Special Instructions (e.g. less spicy, extra chutney)",
    orderPlacedSuccess = "Order Placed Successfully!",
    searchVendors = "Search street vendors, snacks...",
    nearbyVendors = "Nearby Street Vendors",

    logout = "Log Out",
    language = "Language",
    upiId = "UPI ID",
    operatingHours = "Operating Hours",
    editProfile = "Edit Profile"
)

val HindiTranslations = AppTranslation(
    appName = "लोकल वेंड्स",
    tagline = "आपका स्थानीय बाज़ार, अब डिजिटल।",
    vendor = "दुकानदार / विक्रेता",
    customer = "ग्राहक",
    phoneNumber = "फ़ोन नंबर",
    password = "पासवर्ड",
    fullName = "पूरा नाम",
    secureLogin = "सुरक्षित लॉगिन",
    createAccount = "खाता बनाएं",
    createNewAccount = "नया खाता बनाएं",
    alreadyHaveAccount = "पहले से खाता है? लॉगिन करें",
    selectLanguage = "भाषा चुनें",
    changeLanguage = "भाषा बदलें",
    languageSubtitle = "ऐप के लिए अपनी पसंदीदा भाषा चुनें",

    catalogue = "सामान सूची",
    liveLocation = "लाइव लोकेशन",
    orders = "ऑर्डर",
    profile = "प्रोफ़ाइल",

    discover = "दुकानें खोजें",
    favourites = "पसंदीदा",
    myOrders = "मेरे ऑर्डर",

    stallOpen = "दुकान खुली है",
    stallClosed = "दुकान बंद है",
    openStall = "दुकान खोलें",
    closeStall = "दुकान बंद करें",
    addItem = "सामान जोड़ें",
    editItem = "संशोधित करें",
    deleteItem = "हटाएं",
    itemName = "सामान का नाम",
    itemPrice = "कीमत (₹)",
    inStock = "उपलब्ध है",
    outOfStock = "समाप्त हो गया",
    save = "सुरक्षित करें",
    cancel = "रद्द करें",

    pending = "लंबित",
    preparing = "तैयार हो रहा है",
    completed = "पूर्ण",
    all = "सभी",
    acceptOrder = "स्वीकार करें",
    markReady = "तैयार चिह्नित करें",
    completeOrder = "ऑर्डर पूरा करें",
    incomingOrders = "आने वाले ऑर्डर",
    simulateOrder = "+ टेस्ट ऑर्डर",
    noOrders = "कोई ऑर्डर नहीं मिला",
    totalEarnings = "कुल कमाई",
    todaysOrders = "आज के ऑर्डर",

    cart = "आपकी टोकरी (कार्ट)",
    addToCart = "जोड़ें",
    viewCart = "कार्ट देखें",
    checkout = "चेकआउट",
    placeOrder = "ऑर्डर पक्का करें",
    itemTotal = "सामान का कुल मूल्य",
    taxesPackaging = "पैकिंग व हैंडलिंग",
    toPay = "कुल देय राशि",
    pickupAtStall = "स्टॉल से लें",
    nearbyDelivery = "आसपास डिलीवरी",
    specialInstructions = "विशेष निर्देश (उदा. कम तीखा, अतिरिक्त चटनी)",
    orderPlacedSuccess = "ऑर्डर सफलतापूर्वक दर्ज हुआ!",
    searchVendors = "विक्रेता, चाट, फल-सब्जियां खोजें...",
    nearbyVendors = "नज़दीकी स्ट्रीट वेंडर्स",

    logout = "लॉग आउट",
    language = "भाषा",
    upiId = "यूपीआई आईडी",
    operatingHours = "दुकान का समय",
    editProfile = "प्रोफ़ाइल बदलें"
)

val MarathiTranslations = AppTranslation(
    appName = "लोकल वेंड्स",
    tagline = "तुमचा स्थानिक बाजार, आता डिजिटल.",
    vendor = "विक्रेता / फेरीवाला",
    customer = "ग्राहक",
    phoneNumber = "फोन नंबर",
    password = "पासवर्ड",
    fullName = "पूर्ण नाव",
    secureLogin = "सुरक्षित लॉगिन",
    createAccount = "खाते तयार करा",
    createNewAccount = "नवीन खाते तयार करा",
    alreadyHaveAccount = "आधीच खाते आहे? लॉगिन करा",
    selectLanguage = "भाषा निवडा",
    changeLanguage = "भाषा बदला",
    languageSubtitle = "अ‍ॅपसाठी तुमची आवडती भाषा निवडा",

    catalogue = "मालाची यादी",
    liveLocation = "थेट स्थान",
    orders = "ऑर्डर्स",
    profile = "प्रोफाइल",

    discover = "स्टॉल्स शोधा",
    favourites = "आवडीचे",
    myOrders = "माझ्या ऑर्डर्स",

    stallOpen = "स्टॉल सुरू आहे",
    stallClosed = "स्टॉल बंद आहे",
    openStall = "स्टॉल सुरू करा",
    closeStall = "स्टॉल बंद करा",
    addItem = "नवीन वस्तू जोडा",
    editItem = "बदल करा",
    deleteItem = "हटवा",
    itemName = "वस्तूचे नाव",
    itemPrice = "दर (₹)",
    inStock = "उपलब्ध",
    outOfStock = "संपले आहे",
    save = "जतन करा",
    cancel = "रद्द करा",

    pending = "प्रलंबित",
    preparing = "तयार होत आहे",
    completed = "पूर्ण झाले",
    all = "सर्व",
    acceptOrder = "स्वीकारा",
    markReady = "तयार म्हणून चिन्हांकित करा",
    completeOrder = "ऑर्डर पूर्ण करा",
    incomingOrders = "नवीन ऑर्डर्स",
    simulateOrder = "+ चाचणी ऑर्डर",
    noOrders = "कोणत्याही ऑर्डर्स नाहीत",
    totalEarnings = "एकूण कमाई",
    todaysOrders = "आजच्या ऑर्डर्स",

    cart = "तुमची झोळी (कार्ट)",
    addToCart = "जोडा",
    viewCart = "कार्ट पहा",
    checkout = "चेकआउट",
    placeOrder = "ऑर्डर निश्चित करा",
    itemTotal = "वस्तूंचे एकूण मूल्य",
    taxesPackaging = "पॅकिंग आणि हाताळणी",
    toPay = "एकूण देय रक्कम",
    pickupAtStall = "स्टॉलवरून घ्या",
    nearbyDelivery = "जवळ पोहोचवा",
    specialInstructions = "विशेष सूचना (उदा. कमी तिखट, जास्त चटणी)",
    orderPlacedSuccess = "ऑर्डर यशस्वीरीत्या नोंदवली गेली!",
    searchVendors = "फेरीवाले किंवा खाद्यपदार्थ शोधा...",
    nearbyVendors = "जवळपासचे स्ट्रीट वेंडर्स",

    logout = "लॉग आऊट",
    language = "भाषा",
    upiId = "युपीआय आयडी",
    operatingHours = "कामाची वेळ",
    editProfile = "प्रोफाइल बदला"
)

fun getAppTranslation(language: AppLanguage): AppTranslation {
    return when (language) {
        AppLanguage.ENGLISH -> EnglishTranslations
        AppLanguage.HINDI -> HindiTranslations
        AppLanguage.MARATHI -> MarathiTranslations
    }
}

val LocalAppLanguage = compositionLocalOf { AppLanguage.ENGLISH }
val LocalStrings = compositionLocalOf { EnglishTranslations }

@Composable
fun LanguageSwitchPill(
    currentLanguage: AppLanguage,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Translate,
                contentDescription = "Language",
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "${currentLanguage.badgeText} • ${currentLanguage.nativeName}",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
fun LanguageSelectionDialog(
    currentLanguage: AppLanguage,
    onLanguageSelected: (AppLanguage) -> Unit,
    onDismissRequest: () -> Unit
) {
    val t = LocalStrings.current
    Dialog(onDismissRequest = onDismissRequest) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Translate,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = t.selectLanguage,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = t.languageSubtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )

                Spacer(modifier = Modifier.height(20.dp))

                AppLanguage.values().forEach { lang ->
                    val isSelected = lang == currentLanguage
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 5.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .clickable {
                                onLanguageSelected(lang)
                                onDismissRequest()
                            },
                        shape = RoundedCornerShape(14.dp),
                        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        border = if (isSelected) androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.LightGray.copy(alpha = 0.3f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = lang.badgeText,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = if (isSelected) Color.White else Color.DarkGray
                                    )
                                }
                                Column {
                                    Text(
                                        text = lang.nativeName,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                    )
                                    Text(
                                        text = lang.displayName,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.Gray
                                    )
                                }
                            }

                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Filled.Check,
                                    contentDescription = "Selected",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                TextButton(
                    onClick = onDismissRequest,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(t.cancel)
                }
            }
        }
    }
}
