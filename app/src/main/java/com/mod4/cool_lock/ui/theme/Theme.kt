package com.mod4.cool_lock.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

// Fallback for devices below Android 12, where wallpaper-based dynamic color isn't available.
private val FallbackDarkColors = darkColorSchemeOf()
private val FallbackLightColors = lightColorSchemeOf()

private fun darkColorSchemeOf() = androidx.compose.material3.darkColorScheme(
    primary = CoolPrimary, onPrimary = CoolOnPrimary,
    primaryContainer = CoolPrimaryContainer, onPrimaryContainer = CoolOnPrimaryContainer,
    secondary = CoolSecondary, onSecondary = CoolOnSecondary,
    secondaryContainer = CoolSecondaryContainer, onSecondaryContainer = CoolOnSecondaryContainer,
    tertiary = CoolTertiary, onTertiary = CoolOnTertiary,
    tertiaryContainer = CoolTertiaryContainer, onTertiaryContainer = CoolOnTertiaryContainer,
    background = CoolBackground, onBackground = CoolOnBackground,
    surface = CoolBackground, onSurface = CoolOnBackground,
    surfaceVariant = CoolSurfaceVariant, onSurfaceVariant = CoolOnSurfaceVariant,
    surfaceContainerLowest = CoolSurfaceContainerLowest, surfaceContainerLow = CoolSurfaceContainerLow,
    surfaceContainer = CoolSurfaceContainer, surfaceContainerHigh = CoolSurfaceContainerHigh,
    surfaceContainerHighest = CoolSurfaceContainerHighest,
    outline = CoolOutline, outlineVariant = CoolOutlineVariant,
    surfaceTint = Color.Transparent
)

private fun lightColorSchemeOf() = androidx.compose.material3.lightColorScheme(
    primary = CoolPrimaryContainer, secondary = CoolSecondaryContainer, tertiary = CoolTertiaryContainer
)

// Expressive shape scale: big, friendly corners
private val CoolLockShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(16.dp),
    medium = RoundedCornerShape(22.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(40.dp)
)

/**
 * Colors are seeded from the device's wallpaper (Material You) on Android 12+ via
 * dynamicDarkColorScheme/dynamicLightColorScheme, matching whatever accent the system picked.
 * Older devices fall back to Cool-Lock's own fixed violet palette.
 */
@Composable
fun CoolLockTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val context = LocalContext.current
    val colorScheme = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        darkTheme -> FallbackDarkColors
        else -> FallbackLightColors
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Color.Transparent.toArgb()
            window.navigationBarColor = Color.Transparent.toArgb()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                window.isNavigationBarContrastEnforced = false
            }
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = CoolLockShapes,
        content = content
    )
}
