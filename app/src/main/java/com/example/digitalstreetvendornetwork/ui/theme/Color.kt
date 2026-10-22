package com.example.digitalstreetvendornetwork.ui.theme

import androidx.compose.ui.graphics.Color

// ── Brand Colors (mirrors the palette previously inlined in MainActivity) ────
val BrandOrange = Color(0xFFE65100)               // primary
val BrandOrangeContainer = Color(0xFFFFECE0)      // primaryContainer
val BrandOnPrimaryContainer = Color(0xFF4E1600)   // onPrimaryContainer
val BrandOnOrangeContainer = BrandOnPrimaryContainer
val BrandGreen = Color(0xFF2E7D32)               // secondary
val BrandSurfaceVariant = Color(0xFFFFF3E0)       // surfaceVariant
val BrandError = Color(0xFFD32F2F)                // error

// ── Order Status Colors ───────────────────────────────────────────────────────
val StatusPending = Color(0xFFFFA000)
val StatusPreparing = Color(0xFF1976D2)
val StatusCompleted = Color(0xFF2E7D32)
val StatusDeclined = Color(0xFFD32F2F)
val StatusPendingNeutral = Color(0xFF757575)

// ── Vendor Analytics & Ratings ────────────────────────────────────────────────
val RatingStar = Color(0xFFFFC107)
val EarningsCardTint = Color(0xFFE8F5E9)          // light green tint
val EarningsLabelGreen = Color(0xFF388E3C)

// ── Customer UI & Cart ────────────────────────────────────────────────────────
val MenuDivider = Color(0xFFF0F0F0)
val CartItemHighlight = Color(0xFFFFF8E1)
val CartItemDefault = Color(0xFFFAFAFA)
val VegIndicatorGreen = Color(0xFF2E7D32)
val BillSummaryBackground = Color(0xFFF9F9FB)
val BillDividerColor = Color(0xFFE0E0E0)

// ── Template colors used by DarkColorScheme (to be refactored in a later step) ─
val Purple80 = Color(0xFFD0BCFF)
val PurpleGrey80 = Color(0xFFCCC2DC)
val Pink80 = Color(0xFFEFB8C8)