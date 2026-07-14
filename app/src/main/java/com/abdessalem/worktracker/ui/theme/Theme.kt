package com.abdessalem.worktracker.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

val WorkGreen = Color(0xFF4EE17D)
val WorkAmber = Color(0xFFFFB02E)
val WorkCoral = Color(0xFFFF625F)
val WorkBlue = Color(0xFF63A9FF)
val WorkBackground = Color(0xFF07111E)
val WorkSurface = Color(0xFF101D2E)
val WorkSurfaceVariant = Color(0xFF17263A)
val WorkText = Color(0xFFF5F8FC)
val WorkMuted = Color(0xFF9EABC0)

private val DarkColors = darkColorScheme(
    primary = WorkGreen,
    onPrimary = WorkBackground,
    secondary = WorkAmber,
    onSecondary = WorkBackground,
    tertiary = WorkBlue,
    error = WorkCoral,
    background = WorkBackground,
    onBackground = WorkText,
    surface = WorkSurface,
    onSurface = WorkText,
    surfaceVariant = WorkSurfaceVariant,
    onSurfaceVariant = WorkMuted,
    outline = Color(0xFF33465E)
)

private val LightColors = lightColorScheme(
    primary = Color(0xFF087A3C),
    secondary = Color(0xFF8A5A00),
    tertiary = Color(0xFF1F63AD),
    error = Color(0xFFB3261E),
    background = Color(0xFFF5F8FC),
    onBackground = Color(0xFF111827),
    surface = Color.White,
    onSurface = Color(0xFF111827),
    surfaceVariant = Color(0xFFE7EDF4),
    onSurfaceVariant = Color(0xFF48576A)
)

@Composable
fun WorkTrackerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val colors = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && darkTheme -> dynamicDarkColorScheme(context)
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> dynamicLightColorScheme(context)
        darkTheme -> DarkColors
        else -> LightColors
    }
    MaterialTheme(colorScheme = colors, content = content)
}
