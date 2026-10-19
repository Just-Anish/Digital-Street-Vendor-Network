# DEVELOPER.md — Digital Street Vendor Network

> A developer reference for contributors unfamiliar with the codebase.
> Reflects the code **as it exists**, not intended future functionality.

---

## 1. Project Overview

**Digital Street Vendor Network** (display name: *Local Vends*) is a single-module
Android application built with Jetpack Compose. Its stated purpose is to give
Mumbai street vendors a lightweight digital presence — a product catalogue and
live-location broadcast — that lets them retain customers even when they move
stalls.

The app supports two user roles, selected at login: **Vendor** and **Customer**.
Each role gets its own screen graph. There is no back-end, database, or network
layer in the current codebase; all state is ephemeral in-memory Compose state
that resets on process death.

---

## 2. Repository Layout

```
Digital-Street-Vendor-Network/
├── app/
│   ├── build.gradle.kts          # Module-level Gradle: SDK versions, deps
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml
│       │   ├── java/com/example/digitalstreetvendornetwork/
│       │   │   ├── MainActivity.kt        # Entry point, auth screens, top-level routing
│       │   │   ├── VendorScreens.kt       # All vendor-side composables + data models
│       │   │   └── CustomerScreens.kt     # All customer-side composables
│       │   └── res/
│       │       └── values/
│       │           ├── strings.xml        # Only app_name defined
│       │           ├── colors.xml
│       │           └── themes.xml
│       ├── test/                          # Scaffold unit test (ExampleUnitTest.kt)
│       └── androidTest/                   # Scaffold instrumented test (ExampleInstrumentedTest.kt)
├── gradle/
│   └── libs.versions.toml        # Version catalog
├── build.gradle.kts              # Root Gradle (no code, just plugin declarations)
├── settings.gradle.kts           # Single-module project; root name = "Digital Street Vendor Network"
└── gradle.properties             # JVM heap, configuration cache, Kotlin style
```

---

## 3. Architecture

### Pattern
There is **no formal architecture pattern** (no ViewModel, no Repository, no
Clean Architecture layers). All logic and state live directly in Composable
functions using `remember`/`mutableStateOf`.

### Top-level routing (`MainActivity.kt`)
`MainActivity` uses a single `currentScreen: String` state variable and a
`loggedInRole: String` variable to drive a `when` expression that replaces the
entire window content:

```
LOGIN  ─onSuccess(role)──▶  MAIN (VendorAppMainScreen | CustomerAppMainScreen)
  ▲                               │
  └──────onLogout◀────────────────┘
SIGNUP ─onSuccess(role)──▶  MAIN
```

`Navigation Compose` is declared as a dependency but **is not used** anywhere in
the codebase. Routing is implemented manually with `mutableStateOf` strings.

### Vendor-side sub-navigation (`VendorScreens.kt`)
`VendorAppMainScreen` owns:
- A `selectedTab: Int` for the bottom `NavigationBar` (tabs: Catalogue, Location, Orders, Profile).
- Hoisted state for `productList`, `stallName`, `isStallOpen`, `operatingHours`,
  `upiId`, `currentVendorLocation`, and `isBroadcastingLocation`.

`VendorProfileScreen` has its own inner `currentSubScreen: String` state
(`"MAIN"`, `"EARNINGS"`, `"SETTINGS"`) that renders sub-screens as a nested
`when`.

### Customer-side sub-navigation (`CustomerScreens.kt`)
`CustomerAppMainScreen` owns a `selectedTab: Int` for the bottom `NavigationBar`
(tabs: Discover, My Orders, Profile). No further nesting.

### Data models
Two `data class` declarations in `VendorScreens.kt`:

| Class | Fields |
|---|---|
| `Product` | `id: Int`, `name: String`, `price: String`, `description: String` |
| `VendorReview` | `id: Int`, `customerName: String`, `rating: Int`, `comment: String`, `date: String` |

There are no Customer-side data models defined.

---

## 4. Implemented Features

### Auth screens (`MainActivity.kt`)
| Feature | Status |
|---|---|
| Role selection (Vendor / Customer) via `RadioButton` | ✅ Implemented |
| Login with 10-digit phone + non-blank password | ✅ Implemented (client-side only) |
| Phone format validation on login | ✅ Implemented |
| Signup with name + 10-digit phone + password | ✅ Implemented (client-side only) |
| Navigate between Login ↔ Signup | ✅ Implemented |
| Actual credential storage or verification | ❌ Missing — any valid-format input succeeds |

### Vendor — Catalogue tab (`VendorCatalogueScreen`)
| Feature | Status |
|---|---|
| Display product list in `LazyColumn` with name, description, price | ✅ Implemented |
| Add product via `AlertDialog` form (name, price, description) | ✅ Implemented |
| Delete product with trash icon | ✅ Implemented |
| Persisting catalogue across sessions | ❌ Missing |
| Edit existing product | ❌ Missing |

The product list is pre-seeded with three hardcoded items (Vada Pav, Cutting
Chai, Misal Pav).

### Vendor — Location tab (`VendorLocationScreen`)
| Feature | Status |
|---|---|
| Google Map rendered with `maps-compose` | ✅ Implemented |
| Vendor can tap map to reposition their marker | ✅ Implemented |
| "Start / Stop Broadcasting" toggle button | ✅ UI toggle implemented |
| Actual location sharing with other users | ❌ Missing — toggle only changes local UI state |
| GPS / device location as initial pin | ❌ Missing — hardcoded to `LatLng(19.0760, 72.8777)` (Mumbai) |

### Vendor — Orders tab
Shows a static `Text("No Active Orders")` placeholder. No order model or
functionality is implemented.

### Vendor — Profile tab (`VendorProfileScreen`)
| Feature | Status |
|---|---|
| Display stall name and open/closed status | ✅ Implemented |
| Navigate to Earnings & Reviews sub-screen | ✅ Implemented |
| Navigate to Business Settings sub-screen | ✅ Implemented |
| Logout | ✅ Implemented |

**Earnings & Reviews (`VendorEarningsAndReviewsScreen`):**
- Displays a hardcoded summary card: ₹1,450 today, 42 orders, 4.8 ★ avg.
- Displays three hardcoded `VendorReview` objects in a `LazyColumn`.
- No real data source.

**Business Settings (`VendorBusinessSettingsScreen`):**
- Editable fields: stall name, operating hours, UPI ID.
- Open/closed `Switch`.
- Changes are propagated upward via callbacks to `VendorAppMainScreen` state.
- Changes are **not persisted** beyond the current session.

### Customer — Discover tab (`CustomerDiscoverScreen`)
| Feature | Status |
|---|---|
| Google Map rendered | ✅ Implemented |
| Two hardcoded vendor markers on the map | ✅ Implemented |
| Search bar (visual) | ✅ Implemented |
| Filter chips (All, Vegetables, Fruits, Street Food) | ✅ Implemented (UI only) |
| Search / filter actually filtering map markers | ❌ Missing |
| Showing real vendor locations | ❌ Missing |

### Customer — My Orders tab
Shows a static `Text("Track My Pickups & Deliveries")` placeholder. No
implementation.

### Customer — Profile tab (`CustomerProfileScreen`)
| Feature | Status |
|---|---|
| Static avatar + hardcoded name ("Local Shopper") and phone | ✅ Implemented |
| Menu cards: Order History, My Ratings & Reviews, Account Settings | ✅ Implemented (visual only) |
| Logout | ✅ Implemented |
| Any of the menu cards navigating somewhere | ❌ Missing |

---

## 5. UI and Theming

### Color scheme
Defined inline in `MainActivity.onCreate` (not in the `ui/theme` package):
- Primary: `#E65100` (deep orange)
- Secondary: `#2E7D32` (green)
- Surface Variant: `#FFF3E0` (light orange tint)
- Error: `#D32F2F`

The `ui/theme/Theme.kt` (`DigitalStreetVendorNetworkTheme`) and `ui/theme/Color.kt`
(Material 3 purple defaults) exist but are **not used** — `MainActivity` calls
`MaterialTheme(colorScheme = lightColorScheme(...))` directly.

### Typography
`ui/theme/Type.kt` exists (generated scaffold). The app uses
`MaterialTheme.typography.*` without any custom font configuration.

---

## 6. Dependencies

| Library | Version | Purpose |
|---|---|---|
| Jetpack Compose BOM | `2026.02.01` | Compose version alignment |
| `androidx.compose.material3` | via BOM | UI components |
| `material-icons-extended` | via BOM | Extended icon set |
| `androidx.activity:activity-compose` | `1.13.0` | `setContent` bridge |
| `androidx.lifecycle:lifecycle-runtime-ktx` | `2.11.0` | Lifecycle awareness |
| `androidx.core:core-ktx` | `1.19.1` | Android core extensions |
| `androidx.navigation:navigation-compose` | `2.7.7` | **Declared but unused** |
| `com.google.maps.android:maps-compose` | `4.3.3` | Compose wrapper for Maps SDK |
| `com.google.android.gms:play-services-maps` | `18.2.0` | Google Maps SDK |

Test deps: JUnit 4, AndroidX JUnit, Espresso, Compose UI Test (scaffold only).

---

## 7. Configuration

### Google Maps API Key
The `AndroidManifest.xml` contains:

```xml
<meta-data
    android:name="com.google.android.geo.API_KEY"
    android:value="YOUR_API_KEY_HERE" />
```

**The placeholder value is committed to source.** The map will not render without
a real key. To supply one:

1. Create a key at [Google Cloud Console](https://console.cloud.google.com/) with
   **Maps SDK for Android** enabled.
2. Replace `YOUR_API_KEY_HERE` in `app/src/main/AndroidManifest.xml`, **or**
   store it in `local.properties` and inject it via `buildConfigField` in
   `app/build.gradle.kts` (preferred; keeps secrets out of source control).

### Permissions declared
- `android.permission.INTERNET`
- `android.permission.ACCESS_NETWORK_STATE`

No runtime location permissions (`ACCESS_FINE_LOCATION` / `ACCESS_COARSE_LOCATION`)
are declared or requested, despite the location broadcast concept.

---

## 8. Build and Run

### Prerequisites
- Android Studio Meerkat (2024.3+) or compatible
- JDK 11 (set via `compileOptions` in `app/build.gradle.kts`)
- Android SDK: `compileSdk 37`, `minSdk 24`, `targetSdk 37`

### Build
```bash
# Debug APK
./gradlew :app:assembleDebug

# Install on connected device / emulator
./gradlew :app:installDebug
```

### Run from Android Studio
Open the project root in Android Studio, select a device/emulator (API 24+),
and click **Run**.

> The map renders blank grey tiles until a valid Google Maps API key is
> configured (see §7).

### Tests
```bash
# Unit tests (scaffold only — ExampleUnitTest.kt)
./gradlew :app:test

# Instrumented tests (scaffold only — ExampleInstrumentedTest.kt)
./gradlew :app:connectedAndroidTest
```

Neither test file contains meaningful assertions beyond the generated scaffold.

---

## 9. Current Limitations and Next Steps

### Critical gaps

| Gap | Impact |
|---|---|
| No back-end / API | All data is in-process memory; nothing survives app restart |
| Maps API key is a placeholder | Maps render blank for any fresh build |
| No location permissions declared | GPS-based vendor positioning is impossible |
| `navigation-compose` imported but unused | Dead dependency; routing is ad-hoc `mutableStateOf` strings |
| `DigitalStreetVendorNetworkTheme` is unused | Theme infrastructure is disconnected from the app |

### Stubbed / placeholder UI (no logic)
- Vendor "Orders" tab
- Customer "My Orders" tab
- Customer profile menu items (Order History, Ratings, Account Settings)
- Customer search bar and filter chips
- Earnings & Reviews data (all hardcoded strings)
- Customer profile name and phone (hardcoded)

### Recommended next steps

1. **Add a real Maps API key** (inject via `local.properties` + `buildConfigField`)
   so the map works during development.
2. **Wire `Navigation Compose`** to replace the manual `when`/`mutableStateOf`
   routing, enabling proper back-stack support.
3. **Introduce ViewModels** (`androidx.lifecycle:lifecycle-viewmodel-compose`) to
   separate UI state from composable lifecycle.
4. **Declare and request location permissions** (`ACCESS_FINE_LOCATION`) and use
   `FusedLocationProviderClient` to seed the vendor's map pin from the real device
   position.
5. **Add persistence** — at minimum Room or DataStore for the product catalogue
   and business settings.
6. **Implement a back-end** — Firebase Firestore + Firebase Auth is a natural fit
   for real-time vendor-location sharing and authentication.
7. **Activate `DigitalStreetVendorNetworkTheme`** — replace the inline
   `MaterialTheme(colorScheme = lightColorScheme(...))` call in `MainActivity`
   with the existing theme composable.
8. **Write meaningful tests** — the `test/` and `androidTest/` source sets are
   empty scaffold files.
