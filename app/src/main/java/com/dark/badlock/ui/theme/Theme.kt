package com.dark.badlock.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Define the colors to match Cool-Lock's dark aesthetic
val GoodLockBlue = Color(0xFF0A3D91)        // Dark Blue (matches InstallBlue)
val GoodLockBackground = Color(0xFF000000)  // Pure Black
val GoodLockCard = Color(0xFF2C2C2C)        // Lighter dark card
val GoodLockPurple = Color(0xFF8A2BE2)      // Electric Blue-Violet accent

private val LightColorScheme = lightColorScheme(
    primary = GoodLockBlue,
    secondary = GoodLockPurple,
    tertiary = GoodLockCard,
    background = GoodLockBackground,
    surface = GoodLockCard,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color.White,
    onSurface = Color.White,
)

private val DarkColorScheme = darkColorScheme(
    primary = GoodLockBlue,
    secondary = GoodLockPurple,
    tertiary = GoodLockCard,
    background = GoodLockBackground,
    surface = GoodLockCard,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color.White,
    onSurface = Color.White,
)

@Composable
fun BadlockTheme(
    darkTheme: Boolean = false, // You can make this dynamic if you want
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}