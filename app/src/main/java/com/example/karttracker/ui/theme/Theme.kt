package com.example.karttracker.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// --- LIGHT THEME ---
private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF2563EB),        // Blue 600
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDBEAFE), // Blue 100
    onPrimaryContainer = Color(0xFF1E3A8A),

    secondary = Color(0xFF64748B),      // Slate 500
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE2E8F0), // Slate 200
    onSecondaryContainer = Color(0xFF1E293B),

    tertiary = Color(0xFFF59E0B),       // Amber 500
    onTertiary = Color.Black,
    tertiaryContainer = Color(0xFFFEF3C7), // Amber 100
    onTertiaryContainer = Color(0xFF78350F),

    background = Color(0xFFF7FAFC),
    onBackground = Color(0xFF1A202C),

    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1A202C),
    surfaceVariant = Color(0xFFE2E8F0),
    onSurfaceVariant = Color(0xFF334155),

    error = Color(0xFFDC2626),          // Red 600
    onError = Color.White,
    errorContainer = Color(0xFFFEE2E2),
    onErrorContainer = Color(0xFF7F1D1D),

    outline = Color.LightGray,
    outlineVariant = Color(0xFFCBD5E1),
    scrim = Color.Black,
    inverseSurface = Color(0xFF1A202C),
    inverseOnSurface = Color(0xFFF7FAFC),
    inversePrimary = Color(0xFF60A5FA),
)


// --- DARK THEME ---
private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF60A5FA),        // Blue 400
    onPrimary = Color.Black,
    primaryContainer = Color(0xFF1E3A8A), // Blue 900
    onPrimaryContainer = Color(0xFFDBEAFE),

    secondary = Color(0xFF94A3B8),      // Slate 400
    onSecondary = Color.Black,
    secondaryContainer = Color(0xFF1E293B), // Slate 800
    onSecondaryContainer = Color(0xFFE2E8F0),

    tertiary = Color(0xFFFBBF24),       // Amber 400
    onTertiary = Color.Black,
    tertiaryContainer = Color(0xFF78350F), // Amber 900
    onTertiaryContainer = Color(0xFFFEF3C7),

    background = Color(0xFF1A202C),     // Gray 900
    onBackground = Color(0xFFF7FAFC),

    surface = Color(0xFF111827),        // Gray 950
    onSurface = Color(0xFFF7FAFC),
    surfaceVariant = Color(0xFF334155),
    onSurfaceVariant = Color(0xFFCBD5E1),

    error = Color(0xFFF87171),          // Red 400
    onError = Color.Black,
    errorContainer = Color(0xFF7F1D1D),
    onErrorContainer = Color(0xFFFEE2E2),

    outline = Color(0xFF64748B),        // Slate 500
    outlineVariant = Color(0xFF475569),
    scrim = Color.Black,
    inverseSurface = Color(0xFFF7FAFC),
    inverseOnSurface = Color(0xFF1A202C),
    inversePrimary = Color(0xFF2563EB),
)



@Composable
fun KartTrackerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}