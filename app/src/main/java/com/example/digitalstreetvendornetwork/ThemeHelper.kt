package com.example.digitalstreetvendornetwork

import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.digitalstreetvendornetwork.ui.theme.StatusCompleted
import com.example.digitalstreetvendornetwork.ui.theme.StatusCompletedDark
import com.example.digitalstreetvendornetwork.ui.theme.StatusDeclined
import com.example.digitalstreetvendornetwork.ui.theme.StatusDeclinedDark
import com.example.digitalstreetvendornetwork.ui.theme.StatusPending
import com.example.digitalstreetvendornetwork.ui.theme.StatusPendingDark
import com.example.digitalstreetvendornetwork.ui.theme.StatusPendingNeutral
import com.example.digitalstreetvendornetwork.ui.theme.StatusPreparing
import com.example.digitalstreetvendornetwork.ui.theme.StatusPreparingDark

/**
 * Supported in-app theme modes.
 */
enum class AppThemeMode(val storageKey: String) {
    SYSTEM("system"),
    LIGHT("light"),
    DARK("dark")
}

/**
 * Persistence helper for app theme, sharing the "locale_prefs" SharedPreferences with LocaleHelper.
 */
object ThemeHelper {
    private const val PREFS_NAME = "locale_prefs"
    private const val KEY_THEME = "selected_theme"

    fun getThemeMode(context: Context): AppThemeMode {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return when (prefs.getString(KEY_THEME, AppThemeMode.SYSTEM.storageKey)) {
            AppThemeMode.LIGHT.storageKey -> AppThemeMode.LIGHT
            AppThemeMode.DARK.storageKey -> AppThemeMode.DARK
            else -> AppThemeMode.SYSTEM
        }
    }

    fun setThemeMode(context: Context, mode: AppThemeMode) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_THEME, mode.storageKey).apply()
    }
}

val LocalThemeMode = compositionLocalOf { AppThemeMode.SYSTEM }
val LocalOnThemeModeChange = compositionLocalOf<(AppThemeMode) -> Unit> { {} }
val LocalIsDarkTheme = compositionLocalOf { false }

/**
 * Returns a high-contrast status color that stays vibrant and legible
 * on both light surfaces and dark surfaces.
 */
@Composable
fun getAdaptiveStatusColor(status: String): Color {
    val isDark = LocalIsDarkTheme.current
    return when (status.uppercase()) {
        "PENDING" -> if (isDark) StatusPendingDark else StatusPending
        "PREPARING" -> if (isDark) StatusPreparingDark else StatusPreparing
        "COMPLETED" -> if (isDark) StatusCompletedDark else StatusCompleted
        "DECLINED" -> if (isDark) StatusDeclinedDark else StatusDeclined
        else -> if (isDark) MaterialTheme.colorScheme.onSurfaceVariant else StatusPendingNeutral
    }
}

/**
 * Dialog enabling the user to choose between System default, Light, and Dark themes.
 * Changes apply instantly in-memory without activity recreation.
 */
@Composable
fun ThemeSelectionDialog(
    onDismiss: () -> Unit
) {
    val currentThemeMode = LocalThemeMode.current
    val onThemeChange = LocalOnThemeModeChange.current
    var selectedMode by remember { mutableStateOf(currentThemeMode) }

    val options = listOf(
        AppThemeMode.SYSTEM to stringResource(R.string.theme_system),
        AppThemeMode.LIGHT to stringResource(R.string.theme_light),
        AppThemeMode.DARK to stringResource(R.string.theme_dark)
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.select_theme)) },
        text = {
            Column {
                options.forEach { (mode, label) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedMode = mode }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedMode == mode,
                            onClick = { selectedMode = mode }
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(label, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (selectedMode != currentThemeMode) {
                        onThemeChange(selectedMode)
                    }
                    onDismiss()
                }
            ) {
                Text(stringResource(R.string.ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

/**
 * Official Google Maps Night style JSON.
 */
const val DARK_MAP_STYLE_JSON = """[
  {"elementType": "geometry", "stylers": [{"color": "#242f3e"}]},
  {"elementType": "labels.text.stroke", "stylers": [{"color": "#242f3e"}]},
  {"elementType": "labels.text.fill", "stylers": [{"color": "#746855"}]},
  {"featureType": "administrative.locality", "elementType": "labels.text.fill", "stylers": [{"color": "#d59563"}]},
  {"featureType": "poi", "elementType": "labels.text.fill", "stylers": [{"color": "#d59563"}]},
  {"featureType": "poi.park", "elementType": "geometry", "stylers": [{"color": "#263c3f"}]},
  {"featureType": "poi.park", "elementType": "labels.text.fill", "stylers": [{"color": "#6b9a76"}]},
  {"featureType": "road", "elementType": "geometry", "stylers": [{"color": "#38414e"}]},
  {"featureType": "road", "elementType": "geometry.stroke", "stylers": [{"color": "#212a37"}]},
  {"featureType": "road", "elementType": "labels.text.fill", "stylers": [{"color": "#9ca5b3"}]},
  {"featureType": "road.highway", "elementType": "geometry", "stylers": [{"color": "#746855"}]},
  {"featureType": "road.highway", "elementType": "geometry.stroke", "stylers": [{"color": "#1f2835"}]},
  {"featureType": "road.highway", "elementType": "labels.text.fill", "stylers": [{"color": "#f3d19c"}]},
  {"featureType": "water", "elementType": "geometry", "stylers": [{"color": "#17263c"}]},
  {"featureType": "water", "elementType": "labels.text.fill", "stylers": [{"color": "#515c6d"}]},
  {"featureType": "water", "elementType": "labels.text.stroke", "stylers": [{"color": "#17263c"}]}
]"""
