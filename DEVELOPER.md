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

The project uses a single-module (`:app`) structure with Jetpack Compose for declarative UI. There is currently no `ViewModel`, repository layer, dependency injection (Hilt/Koin), or database. All state resides in composable memory.

```
app/src/main/
├── AndroidManifest.xml                         # Permissions, Google Maps API key meta-data, Activity declaration
├── google-services.json                        # Firebase project config (package: com.example.digitalstreetvendornetwork)
└── java/com/example/digitalstreetvendornetwork/
    ├── MainActivity.kt                         # App entry point, auth composables, session restore, top-level routing
    ├── FirebaseAuthRepository.kt               # Firebase Auth + Firestore encapsulation (sign-up, login, logout, role fetch)
    ├── VendorScreens.kt                        # Vendor domain: data models, catalogue, map, orders, profile
    ├── CustomerScreens.kt                      # Customer domain: data models, discovery map, cart, orders, profile
    └── ui/theme/
        ├── Color.kt                            # Material 3 template color definitions
        ├── Theme.kt                            # DigitalStreetVendorNetworkTheme definition
        └── Type.kt                             # Material 3 typography definitions
```

### Component Responsibilities

| Component | File | Responsibilities |
| :--- | :--- | :--- |
| `MainActivity` | [MainActivity.kt](file:///Users/anish/Digital-Street-Vendor-Network/app/src/main/java/com/example/digitalstreetvendornetwork/MainActivity.kt) | Host activity, session restore on cold-start, top-level screen switching, `LoginScreen` and `SignupScreen` composables. |
| `FirebaseAuthRepository` | [FirebaseAuthRepository.kt](file:///Users/anish/Digital-Street-Vendor-Network/app/src/main/java/com/example/digitalstreetvendornetwork/FirebaseAuthRepository.kt) | Wraps `FirebaseAuth` and `FirebaseFirestore`; exposes `signUp`, `login`, `logout`, `fetchRole` as suspend functions. Single instance created with `remember {}` in `MainActivity`. |
| `VendorAppMainScreen` | [VendorScreens.kt](file:///Users/anish/Digital-Street-Vendor-Network/app/src/main/java/com/example/digitalstreetvendornetwork/VendorScreens.kt) | Vendor Scaffold, bottom navigation (Catalogue, Location, Orders, Profile), state hoisting for products, orders, earnings, settings, and coordinates. |
| `CustomerAppMainScreen` | [CustomerScreens.kt](file:///Users/anish/Digital-Street-Vendor-Network/app/src/main/java/com/example/digitalstreetvendornetwork/CustomerScreens.kt) | Customer Scaffold, bottom navigation (Discover, Favourites, My Orders, Profile), state hoisting for favorites, orders, customer profile, and cart. |
| Data Models | `VendorScreens.kt`, `CustomerScreens.kt` | In-memory representations: `Product`, `VendorReview`, `VendorOrder`, `VendorOrderItem`, `VendorOrderStatus`, `CustomerOrder`, `DiscoverableVendor`, `VendorMenuItem`. |

---

## 3. UI, Navigation, State Management & Data Flow

### Navigation
- **Top-Level:** Managed via `currentScreen` state (`"LOGIN"`, `"SIGNUP"`, `"MAIN"`) inside [MainActivity.kt](file:///Users/anish/Digital-Street-Vendor-Network/app/src/main/java/com/example/digitalstreetvendornetwork/MainActivity.kt).
- **Tab Navigation:** Managed via `selectedTab` integer state inside both `VendorAppMainScreen` and `CustomerAppMainScreen` with Material 3 `NavigationBar` and `Scaffold`.
- **Sub-Screen Navigation:** Managed via local `currentSubScreen` strings inside `VendorProfileScreen` (`"MAIN"`, `"EARNINGS"`, `"SETTINGS"`) and `CustomerProfileScreen` (`"MAIN"`, `"HISTORY"`, `"SETTINGS"`).
- *Note:* The dependency `androidx.navigation:navigation-compose:2.7.7` is declared in `app/build.gradle.kts` but is not yet wired into the screen flow.

### State Management & Data Flow
- **State Mechanism:** Pure Compose state using `remember { mutableStateOf(...) }`, `mutableStateListOf(...)`, and `mutableStateMapOf(...)`.
- **Hoisting:**
  - In `VendorAppMainScreen`, state for `productList`, `ordersList`, `todayEarnings`, `completedOrdersCount`, `currentVendorLocation`, and business settings are hoisted at the root scaffold and passed down via callbacks. Completing an order in `VendorOrdersScreen` directly modifies `ordersList` and increments `todayEarnings` and `completedOrdersCount`.
  - In `CustomerAppMainScreen`, `favouriteVendorIds` and `customerOrders` are hoisted at the root scaffold. Placing an order in `CustomerDiscoverScreen` appends a new `CustomerOrder` directly into `customerOrders`.
- **Persistence Boundary:** State is entirely transient in memory. Navigating between Vendor and Customer roles or killing the process resets all state to default seed data.

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
| **Vendor Catalogue** | Implemented | In-memory CRUD with validation in `VendorCatalogueScreen`. |
| **Vendor Orders** | Implemented | Status filtering, card actions, live earnings sync, simulated orders in `VendorOrdersScreen`. |
| **Vendor Map Display** | Implemented | Google Maps with marker in `VendorLocationScreen`. |
| **Customer Search & Cart** | Implemented | Search by vendor/product, category filter, bottom sheet menu, interactive quantity steppers, checkout modal in `CustomerDiscoverScreen`. |
| **Customer Orders & Favourites** | Implemented | In-memory listing and real-time addition from checkout in `CustomerOrdersScreen` and `CustomerFavouritesScreen`. |
| **Device GPS / Live Location** | Partial | Location toggle and coordinates state exist, but no `ACCESS_FINE_LOCATION` permission or `FusedLocationProviderClient` integration. |
| **Cross-Role Order Sync** | Partial | Both roles have complete order workflows, but orders placed by a Customer are stored in customer memory and do not appear in the Vendor's order list. |
| **Authentication & Users** | Implemented | Firebase Auth email+password (phone-derived email). Firestore `/users/{uid}` stores `name`, `phone`, `role`. Session restored on cold-start via `FirebaseAuth.currentUser`. |
| **Data Persistence** | Partial | User identity and role persist via Firebase. All other state (catalogue, orders, cart) is in-memory and resets on app restart. |
| **Secure API Key Management** | Missing | Maps API key is embedded directly in `AndroidManifest.xml` rather than loaded from `local.properties` via `secrets-gradle-plugin` or `BuildConfig`. |
| **Architecture (ViewModel/MVI)** | Missing | UI composables manage their own business logic directly without ViewModels or repositories (except `FirebaseAuthRepository`). |
| **Theme Alignment** | Missing | `ui/theme/Theme.kt` contains unused scaffold colors while `MainActivity.kt` declares custom colors inline. |
| **Cross-Role Order Sync** | Partial | Both roles have complete order workflows, but orders placed by a Customer are stored in customer memory and do not appear in the Vendor's order list. |

### Recommended Next Steps for Developers
1. **Upgrade Kotlin to 2.4+:** Allows upgrading the Firebase BOM to `35.x` to use the latest Firebase SDK features. Both `libs.versions.toml` (`kotlin`) and AGP Kotlin plugin need updating together.
2. **Firestore Order Sync:** Write `VendorOrder` and `CustomerOrder` documents to Firestore so that an order placed by a Customer is visible in real-time on the Vendor's Orders tab.
3. **Secrets Management:** Extract the Google Maps API key from `AndroidManifest.xml` into `local.properties` and inject it dynamically using `secrets-gradle-plugin` or manifest placeholders.
4. **Theme Consolidation:** Move the brand palette from `MainActivity.kt` into `ui/theme/Color.kt` and wrap the app in `DigitalStreetVendorNetworkTheme` to support system dark mode and dynamic colors.
5. **Architecture Migration:** Introduce `ViewModel` classes (e.g. `VendorViewModel`, `CustomerViewModel`) and Kotlin `StateFlow` to survive configuration changes and separate UI from domain logic.
6. **Persist App State:** Migrate in-memory catalogue, orders, and settings to Firestore so data survives across app restarts.
7. **GPS Location Services:** Add runtime location permissions (`ACCESS_FINE_LOCATION`) and use `FusedLocationProviderClient` to display the vendor's actual street position.
8. **Adopt Navigation Compose:** Replace manual `when (currentScreen)` and `selectedTab` integer state with typed Jetpack Compose navigation.
