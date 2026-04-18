package com.itzjok3r.metubeapp.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// ────────────────────────────────────────────────────────────────────────────
// Color Palette — Premium dark/light scheme inspired by MeTube's identity
// ────────────────────────────────────────────────────────────────────────────

// Primary: Vibrant teal-cyan for action elements
private val PrimaryDark = Color(0xFF4DD0E1)
private val PrimaryLight = Color(0xFF00838F)
private val OnPrimaryDark = Color(0xFF003738)
private val OnPrimaryLight = Color(0xFFFFFFFF)

// Secondary: Warm amber for accents and highlights
private val SecondaryDark = Color(0xFFFFB74D)
private val SecondaryLight = Color(0xFFE65100)

// Background/Surface: Deep charcoal for dark, clean white for light
private val BackgroundDark = Color(0xFF0F1419)
private val SurfaceDark = Color(0xFF1A2029)
private val BackgroundLight = Color(0xFFF8FAFB)
private val SurfaceLight = Color(0xFFFFFFFF)

// Error state: Coral red
private val ErrorColor = Color(0xFFEF5350)

// Status colors used throughout the app
val StatusDownloading = Color(0xFF4DD0E1)
val StatusCompleted = Color(0xFF66BB6A)
val StatusPending = Color(0xFFFFB74D)
val StatusError = Color(0xFFEF5350)

/**
 * Dark color scheme — primary display mode for a media download app.
 */
private val DarkColorScheme = darkColorScheme(
    primary = PrimaryDark,
    onPrimary = OnPrimaryDark,
    primaryContainer = Color(0xFF004D52),
    onPrimaryContainer = Color(0xFFB2EBF2),
    secondary = SecondaryDark,
    onSecondary = Color(0xFF422C00),
    secondaryContainer = Color(0xFF5F4100),
    onSecondaryContainer = Color(0xFFFFDDB3),
    tertiary = Color(0xFFCE93D8),
    onTertiary = Color(0xFF3B0048),
    background = BackgroundDark,
    onBackground = Color(0xFFE1E3E6),
    surface = SurfaceDark,
    onSurface = Color(0xFFE1E3E6),
    surfaceVariant = Color(0xFF263040),
    onSurfaceVariant = Color(0xFFBCC7D4),
    outline = Color(0xFF3D4D5C),
    error = ErrorColor,
    onError = Color(0xFF690005),
)

/**
 * Light color scheme — clean and professional.
 */
private val LightColorScheme = lightColorScheme(
    primary = PrimaryLight,
    onPrimary = OnPrimaryLight,
    primaryContainer = Color(0xFFB2EBF2),
    onPrimaryContainer = Color(0xFF002022),
    secondary = SecondaryLight,
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFFFDDB3),
    onSecondaryContainer = Color(0xFF2A1800),
    tertiary = Color(0xFF7B1FA2),
    onTertiary = Color(0xFFFFFFFF),
    background = BackgroundLight,
    onBackground = Color(0xFF1A1C1E),
    surface = SurfaceLight,
    onSurface = Color(0xFF1A1C1E),
    surfaceVariant = Color(0xFFE8ECF0),
    onSurfaceVariant = Color(0xFF444A52),
    outline = Color(0xFFBCC4CC),
    error = ErrorColor,
    onError = Color(0xFFFFFFFF),
)

/**
 * Custom typography for a premium media app feel.
 */
private val AppTypography = Typography(
    headlineLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 36.sp,
        letterSpacing = 0.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = 32.sp,
        letterSpacing = 0.sp
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.15.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.25.sp
    ),
    bodySmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.4.sp
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    )
)

/**
 * MeTube Client theme composable.
 *
 * Supports dark/light mode toggle from the ViewModel, with automatic fallback
 * to system theme. Uses Material3 dynamic colors on Android 12+ devices.
 *
 * @param darkTheme    Whether to use dark mode. Defaults to system preference.
 * @param dynamicColor Whether to use Material You dynamic colors (Android 12+).
 * @param content      The composable content to theme.
 */
@Composable
fun MeTubeClientTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        // Dynamic color on Android 12+ (disabled by default to maintain brand identity)
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        content = content
    )
}
