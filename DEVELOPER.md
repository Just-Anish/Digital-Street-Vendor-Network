# Digital Street Vendor Network - Developer Documentation

## 1. Project Overview & Implemented Features

**Local Vends** ("Digital Street Vendor Network") is an Android application built using Jetpack Compose that digitizes neighborhood street markets. The app provides specialized user interfaces for two user roles: **Street Vendors** (managing products, location broadcast, incoming orders, and daily earnings) and **Customers** (discovering nearby stalls on Google Maps, searching products, interactive cart ordering, bookmarking favorites, and viewing order history).

### Current Implemented Features

#### Authentication & Role Routing
- **Firebase Auth:** Email+password authentication via Firebase Authentication. Phone numbers are submitted in the UI; the app derives a synthetic email `<phone>@localvends.app` so the UX remains phone-centric.
- **Firestore User Profiles:** Registration writes `name`, `phone`, and `role` to `/users/{uid}` in Cloud Firestore. Login reads the stored `role` from that document to determine routing — the user does not select their role at login.
- **Session Restore:** On cold-start, `MainActivity` checks `FirebaseAuth.currentUser`; if a session exists it fetches the Firestore role and bypasses the login screen directly to `MAIN`.
- **Logout:** Calls `FirebaseAuth.signOut()` before resetting in-memory routing state.
- **Client-Side Validation:** 10-digit phone number check and non-blank field validation remain in [MainActivity.kt](file:///Users/anish/Digital-Street-Vendor-Network/app/src/main/java/com/example/digitalstreetvendornetwork/MainActivity.kt) before any Firebase call is made.
- **Error Handling:** `friendlyAuthError()` in `MainActivity.kt` maps Firebase exception messages to concise, user-facing strings shown inline below the form fields.

#### Vendor Portal ([VendorScreens.kt](file:///Users/anish/Digital-Street-Vendor-Network/app/src/main/java/com/example/digitalstreetvendornetwork/VendorScreens.kt))
- **Catalogue Management (`VendorCatalogueScreen`):**
  - View product list with name, price, description, and availability badge.
  - Add, edit, and delete products with input validation (duplicate name check, positive numeric price).
  - Toggle live product availability (`isAvailable`).
- **Location Broadcasting (`VendorLocationScreen`):**
  - Interactive Google Map view centered on Mumbai (`19.0760, 72.8777`).
  - Draggable/clickable map marker to update stall coordinates.
  - Toggle switch for location broadcasting (`isBroadcastingLocation`).
- **Live Order Management (`VendorOrdersScreen`):**
  - Tabbed status filtering: `All`, `Pending`, `Preparing`, `Completed`, `Declined`.
  - Order cards showing customer details, phone number, ordered items, total amount, order time, payment method (`UPI` / `Cash on Pickup`), delivery type (`Stall Pickup` / `Nearby Handover`), and special notes.
  - Interactive status transitions:
    - Accept order: moves status from `PENDING` to `PREPARING`.
    - Complete order: moves status to `COMPLETED` and automatically syncs with today's revenue and completed order counter.
    - Decline order: moves status to `DECLINED`.
  - Order Simulation: "Simulate Incoming Order" button generates randomized mock orders to test the vendor workflow.
  - Bottom navigation badge indicating real-time count of pending orders.
- **Profile & Business Settings (`VendorProfileScreen`):**
  - Top-level profile showing stall name, open/closed status pill, and operating hours.
  - Sub-screen navigation (`"MAIN"`, `"EARNINGS"`, `"SETTINGS"`):
    - `VendorEarningsAndReviewsScreen`: Displays synced daily revenue, completed order counter, average rating, rating distribution bars, and customer reviews.
    - `VendorBusinessSettingsScreen`: Editable business name, stall open/closed toggle, operating hours, and UPI ID with save confirmation.
  - "Close Shop" logout action.

#### Customer Portal ([CustomerScreens.kt](file:///Users/anish/Digital-Street-Vendor-Network/app/src/main/java/com/example/digitalstreetvendornetwork/CustomerScreens.kt))
- **Vendor Discovery (`CustomerDiscoverScreen`):**
  - Google Map displaying discoverable vendor markers in Mumbai.
  - Real-time search by vendor name, category, or individual product names.
  - Category pill filter: `All`, `Vegetables`, `Fruits`, `Street Food`.
  - Vendor Preview Bottom Sheet: Tap a vendor pin to view stall details, operating hours, and full category-based menu items.
  - Interactive Cart & Ordering:
    - Live quantity steppers (`+` / `-`) for menu items with instant item count and total price calculation.
    - Expandable checkout bottom sheet with delivery mode selector (`Pickup` / `Nearby Handover`), special instructions field, and order summary.
    - Place Order action with instant confirmation dialog and live dispatch into the Customer Orders state.
- **Favourites (`CustomerFavouritesScreen`):**
  - Displays vendors bookmarked by the customer (`favouriteVendorIds`).
  - One-tap favorite toggle directly from the vendor details sheet.
  - Empty state with shortcut to discover vendors.
- **My Orders (`CustomerOrdersScreen`):**
  - Chronological list of active and completed orders (`CustomerOrder`).
  - Displays order ID, vendor name, item summary, total price, date/time, and status badges (`Preparing`, `Completed`).
- **Profile & History (`CustomerProfileScreen`):**
  - Customer avatar, name, and phone number display.
  - Sub-screens for Order History (`CustomerOrderHistoryScreen`) and Account Settings (`CustomerSettingsScreen` to update name and phone number).
  - Logout action.

---

## 2. Architecture & Major Components

The project uses a single-module (`:app`) structure with Jetpack Compose for declarative UI, incorporating `FirebaseAuthRepository` for authentication and `VendorViewModel` for Firestore synchronization. Dependency injection (Hilt/Koin) and local caching (Room) are not yet integrated.

```
app/src/main/
├── AndroidManifest.xml                         # Permissions, Google Maps API key meta-data, Activity declaration
├── google-services.json                        # Firebase project config (package: com.example.digitalstreetvendornetwork)
└── java/com/example/digitalstreetvendornetwork/
    ├── MainActivity.kt                         # App entry point, auth composables, session restore, top-level routing
    ├── FirebaseAuthRepository.kt               # Firebase Auth + Firestore encapsulation (sign-up, login, logout, role fetch)
    ├── VendorScreens.kt                        # Vendor domain: data models, catalogue, map, orders, profile
    ├── CustomerScreens.kt                      # Customer domain: data models, discovery map, cart, orders, profile
    ├── viewmodel/
    │   ├── VendorViewModel.kt                  # StateFlow ViewModel; owns /vendors/{vendorId} settings, products, and location broadcast
    │   └── CustomerViewModel.kt                # StateFlow ViewModel; listens to /vendors and exposes activeVendors for the discovery map
    └── ui/theme/
        ├── Color.kt                            # Material 3 template color definitions
        ├── Theme.kt                            # DigitalStreetVendorNetworkTheme definition
        └── Type.kt                             # Material 3 typography definitions
```

### Component Responsibilities

| Component | File | Responsibilities |
| :--- | :--- | :--- |
| `MainActivity` | [MainActivity.kt](file:///Users/anish/Digital-Street-Vendor-Network/app/src/main/java/com/example/digitalstreetvendornetwork/MainActivity.kt) | Host activity, session restore on cold-start, top-level screen switching, `LoginScreen` and `SignupScreen` composables. Passes Firebase `vendorId` (UID) to `VendorAppMainScreen`. |
| `FirebaseAuthRepository` | [FirebaseAuthRepository.kt](file:///Users/anish/Digital-Street-Vendor-Network/app/src/main/java/com/example/digitalstreetvendornetwork/FirebaseAuthRepository.kt) | Wraps `FirebaseAuth` and `FirebaseFirestore`; exposes `signUp`, `login`, `logout`, `fetchRole` as suspend functions. Single instance created with `remember {}` in `MainActivity`. |
| `VendorViewModel` | [viewmodel/VendorViewModel.kt](file:///Users/anish/Digital-Street-Vendor-Network/app/src/main/java/com/example/digitalstreetvendornetwork/viewmodel/VendorViewModel.kt) | Handles Cloud Firestore sync for the `/vendors/{vendorId}` document and its `/products` subcollection. Exposes `settings` fields (`stallName`, `isStallOpen`, `operatingHours`, `upiId`), `catalogueProducts`, `catalogueLoading`, `isBroadcastingLocation`, `stallLat`, and `stallLng` as `StateFlow`s. CRUD: `addProduct()`, `deleteProduct()`, `toggleProductAvailability()`, `updateLocation()`, `toggleLocationBroadcast()`. All backed by Firestore snapshot listeners or merge writes. Instantiated via `viewModel()` in `VendorAppMainScreen` (vendorId sourced from `FirebaseAuth`). |
| `CustomerViewModel` | [viewmodel/CustomerViewModel.kt](file:///Users/anish/Digital-Street-Vendor-Network/app/src/main/java/com/example/digitalstreetvendornetwork/viewmodel/CustomerViewModel.kt) | Attaches a real-time `SnapshotListener` to the `/vendors` Firestore collection. Filters to vendors where `isStallOpen == true`, `isBroadcastingLocation == true`, and valid `lat`/`lng` coordinates exist. Exposes `activeVendors: StateFlow<List<ActiveVendor>>` consumed by `CustomerDiscoverScreen` to render live map markers. **Batch 4A:** Adds `Order` data class (orderId, customerId, vendorId, items, totalAmount, status, timestamp), `placeOrder(vendorId, items, quantities, totalAmount)` which writes a new document to `/orders` in Firestore and patches back the generated `orderId`, and `orderPlacementResult: StateFlow<OrderPlacementResult>` (sealed: `Idle` / `Success(orderId)` / `Error(message)`) for UI feedback. `clearOrderResult()` resets the flow after consumption. Instantiated via `viewModel()` in `CustomerDiscoverScreen`. |
| `VendorAppMainScreen` | [VendorScreens.kt](file:///Users/anish/Digital-Street-Vendor-Network/app/src/main/java/com/example/digitalstreetvendornetwork/VendorScreens.kt) | Vendor Scaffold, bottom navigation, state hoisting for in-memory data (orders, earnings); delegates Business Settings, Catalogue, and Location state to `VendorViewModel`. Receives `vendorId: String` (Firebase UID) from `MainActivity`. |
| `CustomerAppMainScreen` | [CustomerScreens.kt](file:///Users/anish/Digital-Street-Vendor-Network/app/src/main/java/com/example/digitalstreetvendornetwork/CustomerScreens.kt) | Customer Scaffold, bottom navigation (Discover, Favourites, My Orders, Profile), state hoisting for favorites, orders, customer profile, and cart. |
| Data Models | `VendorScreens.kt`, `CustomerScreens.kt`, `viewmodel/` | In-memory: `Product`, `VendorReview`, `VendorOrder`, `VendorOrderItem`, `VendorOrderStatus`, `CustomerOrder`, `DiscoverableVendor` (hardcoded demo), `VendorMenuItem`. Firestore-sourced: `ActiveVendor` (vendorId, stallName, lat, lng) in `CustomerViewModel.kt`. **Batch 4A:** `Order` data class (orderId, customerId, vendorId, items: List\<String\>, totalAmount, status, timestamp) added to `CustomerViewModel.kt`; written to `/orders/{auto-id}` on checkout. `SaveResult` in `VendorViewModel.kt`; `OrderPlacementResult` (sealed) in `CustomerViewModel.kt`. |

---

## 3. UI, Navigation, State Management & Data Flow

### Navigation
- **Top-Level:** Managed via `currentScreen` state (`"LOGIN"`, `"SIGNUP"`, `"MAIN"`) inside [MainActivity.kt](file:///Users/anish/Digital-Street-Vendor-Network/app/src/main/java/com/example/digitalstreetvendornetwork/MainActivity.kt).
- **Tab Navigation:** Managed via `selectedTab` integer state inside both `VendorAppMainScreen` and `CustomerAppMainScreen` with Material 3 `NavigationBar` and `Scaffold`.
- **Sub-Screen Navigation:** Managed via local `currentSubScreen` strings inside `VendorProfileScreen` (`"MAIN"`, `"EARNINGS"`, `"SETTINGS"`) and `CustomerProfileScreen` (`"MAIN"`, `"HISTORY"`, `"SETTINGS"`).
- *Note:* The dependency `androidx.navigation:navigation-compose:2.7.7` is declared in `app/build.gradle.kts` but is not yet wired into the screen flow.

### State Management & Data Flow
- **State Mechanism:** Pure Compose state using `remember { mutableStateOf(...) }`, `mutableStateListOf(...)`, and `mutableStateMapOf(...)`, complemented by Kotlin `StateFlow` in ViewModels.
- **Hoisting:**
  - In `VendorAppMainScreen`, state for `productList`, `ordersList`, `todayEarnings`, `completedOrdersCount`, and `currentVendorLocation` are hoisted at the root scaffold and passed down via callbacks. Completing an order in `VendorOrdersScreen` directly modifies `ordersList` and increments `todayEarnings` and `completedOrdersCount`.
  - In `CustomerAppMainScreen`, `favouriteVendorIds` and `customerOrders` are hoisted at the root scaffold. Placing an order in `CustomerDiscoverScreen` appends a new `CustomerOrder` directly into `customerOrders` **and** calls `CustomerViewModel.placeOrder()` which persists the order to Firestore `/orders`.
- **Vendor Settings Hoisting & Firestore Sync:** Business Settings (Stall Name, Open Status, Operating Hours, UPI ID) are hoisted in `VendorViewModel` using Kotlin `StateFlow`. UI interactions in `VendorScreens.kt` (`VendorAppMainScreen` and `VendorBusinessSettingsScreen`) trigger Firestore updates via `VendorViewModel` instead of relying on local in-memory state.
- **Persistence Boundary:** User profiles and Vendor Business Settings are persisted in Cloud Firestore. **Batch 4A:** Customer orders are now persisted to `/orders/{auto-id}` in Firestore on placement. Remaining domain state (cart, favourites) remains transient in memory and resets on app restart.

### Theme & Styling
- [MainActivity.kt](file:///Users/anish/Digital-Street-Vendor-Network/app/src/main/java/com/example/digitalstreetvendornetwork/MainActivity.kt) defines an inline `MaterialTheme` with brand colors (`primary = Color(0xFFE65100)` deep orange, `secondary = Color(0xFF2E7D32)` forest green, `surfaceVariant = Color(0xFFFFF3E0)` cream).
- [ui/theme/Theme.kt](file:///Users/anish/Digital-Street-Vendor-Network/app/src/main/java/com/example/digitalstreetvendornetwork/ui/theme/Theme.kt) contains standard template color schemes (`Purple40`, `Purple80`) and is currently unused by `MainActivity`.

---

## 4. APIs, Persistence, Dependencies & Configuration

### Dependencies
Defined across [gradle/libs.versions.toml](file:///Users/anish/Digital-Street-Vendor-Network/gradle/libs.versions.toml) and [app/build.gradle.kts](file:///Users/anish/Digital-Street-Vendor-Network/app/build.gradle.kts):

| Dependency | Version | Purpose |
| :--- | :--- | :--- |
| Android Gradle Plugin (AGP) | `9.3.3` | Android build toolchain |
| Kotlin | `2.2.10` | Programming language with Compose compiler plugin |
| `com.google.gms:google-services` | `4.5.0` | Google Services Gradle plugin (applied in root `build.gradle.kts`) |
| Compose BOM | `2026.02.01` | Bill of Materials for Compose dependencies |
| `androidx.compose.material3:material3` | Via BOM | Material 3 UI design system |
| `androidx.compose.material:material-icons-extended` | Unversioned (BOM) | Complete icon library (Storefront, Cart, Map, etc.) |
| `com.google.maps.android:maps-compose` | `4.3.3` | Google Maps composables (`GoogleMap`, `Marker`) |
| `com.google.android.gms:play-services-maps` | `18.2.0` | Google Play Services Maps SDK |
| Firebase BOM | `33.7.0` | Bill of Materials for Firebase dependencies. Pinned to `33.7.0` — the `35.x` series ships Kotlin metadata 2.4.0 which is incompatible with this project's Kotlin `2.2.10` compiler. |
| `com.google.firebase:firebase-auth` | `23.1.0` (via BOM) | Firebase Authentication — email+password sign-in and session management |
| `com.google.firebase:firebase-firestore` | `25.1.1` (via BOM) | Cloud Firestore — user profile document storage (`/users/{uid}`) |
| `com.google.firebase:firebase-analytics` | Via BOM | Firebase Analytics (added with initial Firebase setup) |
| `androidx.lifecycle:lifecycle-viewmodel-compose` | `2.8.7` | `viewModel()` composable factory for `VendorViewModel` |
| `org.jetbrains.kotlinx:kotlinx-coroutines-play-services` | `1.9.0` | `await()` extension for Firebase `Task<T>` in coroutines |
| `androidx.navigation:navigation-compose` | `2.7.7` | Compose navigation library (*present, not yet used*) |
| `junit:junit` | `4.13.2` | Local unit test framework |

### Configuration & Permissions
- **Target SDK:** `37`, **Compile SDK:** `37`, **Min SDK:** `24`, **Java Version:** `11`.
- **Manifest Permissions ([AndroidManifest.xml](file:///Users/anish/Digital-Street-Vendor-Network/app/src/main/AndroidManifest.xml)):**
  - `android.permission.INTERNET`
  - `android.permission.ACCESS_NETWORK_STATE`
- **Google Maps Key:** Configured via `<meta-data android:name="com.google.android.geo.API_KEY" android:value="..." />` inside `app/src/main/AndroidManifest.xml`.
- **Note on Location Permissions:** `ACCESS_FINE_LOCATION` and `ACCESS_COARSE_LOCATION` are **not** currently declared in the manifest. Maps use static/user-dragged coordinates without device GPS access.

---

## 5. Build, Run, and Test Instructions

### Prerequisites
- JDK 11 or higher
- Android SDK with platform tools installed for API 37
- Configured Android Emulator or connected physical device running Android 7.0 (API 24) or higher

### Gradle Commands

```bash
# Build the debug APK
./gradlew assembleDebug

# Run local unit tests (12 tests in ExampleUnitTest.kt)
./gradlew testDebugUnitTest

# Run instrumented tests on an emulator/device
./gradlew connectedDebugAndroidTest

# Install debug build to a connected device/emulator
./gradlew installDebug

# Clean build cache
./gradlew clean
```

### Existing Test Suite
- [app/src/test/java/com/example/digitalstreetvendornetwork/ExampleUnitTest.kt](file:///Users/anish/Digital-Street-Vendor-Network/app/src/test/java/com/example/digitalstreetvendornetwork/ExampleUnitTest.kt): 12 unit tests validating vendor search queries, case-insensitivity, whitespace trimming, category filtering, numeric price validation, and duplicate product name detection.
- [app/src/androidTest/java/com/example/digitalstreetvendornetwork/ExampleInstrumentedTest.kt](file:///Users/anish/Digital-Street-Vendor-Network/app/src/androidTest/java/com/example/digitalstreetvendornetwork/ExampleInstrumentedTest.kt): Verifies package context initialization.

---

## 6. Current Limitations, Incomplete Features & Next Steps

### Implemented vs. Partial vs. Missing

| Feature Area | Status | Current Code State |
| :--- | :--- | :--- |
| **Vendor Catalogue** | Implemented | CRUD operations (`addProduct`, `deleteProduct`, `toggleProductAvailability`) persist to `/vendors/{vendorId}/products` in Firestore via `VendorViewModel`. Real-time `SnapshotListener` keeps `VendorCatalogueScreen` up to date. Duplicate-name and positive-price validation remain in the UI layer. |
| **Vendor Orders** | Implemented | Status filtering, card actions, live earnings sync, simulated orders in `VendorOrdersScreen`. |
| **Vendor Map Display** | Implemented | Google Maps with marker in `VendorLocationScreen`. |
| **Vendor Business Settings** | Implemented | `stallName`, `isStallOpen`, `operatingHours`, `upiId` persisted to `/vendors/{vendorId}` in Firestore via `VendorViewModel`. Toggle writes instantly; Save writes all fields in a single merge. Document loaded on ViewModel init. |
| **Customer Search & Cart** | Implemented | Search by vendor/product, category filter, bottom sheet menu, interactive quantity steppers, checkout modal in `CustomerDiscoverScreen`. Live markers from Firestore (`CustomerViewModel.activeVendors`) overlay the map alongside hardcoded demo vendors. |
| **Customer Orders & Favourites** | Implemented | In-memory listing and real-time addition from checkout in `CustomerOrdersScreen` and `CustomerFavouritesScreen`. |
| **Device GPS / Live Location** | Implemented | Vendor stall coordinates (`lat`, `lng`) and `isBroadcastingLocation` are persisted to `/vendors/{vendorId}` in Firestore via `VendorViewModel.updateLocation()` and `toggleLocationBroadcast()`. `VendorLocationScreen` renders a Google Map with a tappable/draggable marker; toggling the switch writes instantly. No device GPS or `FusedLocationProviderClient` — coordinates are set manually by the vendor on the map. |
| **Authentication & Users** | Implemented | Firebase Auth email+password (phone-derived email). Firestore `/users/{uid}` stores `name`, `phone`, `role`. Session restored on cold-start via `FirebaseAuth.currentUser`. |
| **Data Persistence** | Partial | User identity, role, Business Settings, Vendor Catalogue, and stall location/broadcast status persist via Cloud Firestore. **Batch 4A:** Customer orders now write live to `/orders/{auto-id}` in Firestore on placement. Cart and favourites remain in-memory. |
| **Architecture (ViewModel)** | Partial | `VendorViewModel` manages Business Settings, Catalogue, and Location broadcast using `StateFlow` backed by Firestore. `CustomerViewModel` provides live vendor markers from Firestore and **Batch 4A:** exposes `placeOrder()` and `orderPlacementResult: StateFlow<OrderPlacementResult>` for Firestore-backed order creation. Orders and remaining Customer flows (favourites, profile) still hoist state in Compose composables without ViewModels. |
| **Cross-Role Order Sync** | Partial | **Batch 4A:** Customer orders placed via checkout are now written to the `/orders` Firestore collection (fields: `orderId`, `customerId`, `vendorId`, `items`, `totalAmount`, `status: PENDING`, `timestamp`). Vendor-side real-time listening to this collection (SnapshotListener on `/orders?vendorId==uid`) is the remaining next step for full cross-role sync. |
| **Secure API Key Management** | Missing | Maps API key is embedded directly in `AndroidManifest.xml` rather than loaded from `local.properties` via `secrets-gradle-plugin` or `BuildConfig`. |
| **Theme Alignment** | Missing | `ui/theme/Theme.kt` contains unused scaffold colors while `MainActivity.kt` declares custom colors inline. |

### Recommended Next Steps for Developers
1. **Vendor Orders Firestore Sync:** Attach a `SnapshotListener` to `/orders` filtered by `vendorId == currentVendorId` so new Customer orders (written by `CustomerViewModel.placeOrder()` since Batch 4A) appear in the Vendor's Orders tab in real-time.
2. **Upgrade Kotlin to 2.4+:** Allows upgrading the Firebase BOM to `35.x`. Both `libs.versions.toml` (`kotlin`) and the AGP Kotlin plugin need updating together.
3. **Secrets Management:** Extract the Google Maps API key from `AndroidManifest.xml` into `local.properties` and inject via `secrets-gradle-plugin` or manifest placeholders.
4. **Theme Consolidation:** Move the brand palette from `MainActivity.kt` into `ui/theme/Color.kt` and wrap the app in `DigitalStreetVendorNetworkTheme`.
5. **GPS Auto-Location:** Add `ACCESS_FINE_LOCATION` permission and use `FusedLocationProviderClient` to auto-fill the vendor's initial map position rather than requiring a manual pin.
6. **Adopt Navigation Compose:** Replace manual `when (currentScreen)` and `selectedTab` integer routing with typed Jetpack Compose navigation.
