package com.example.ui.theme

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

private val DarkColorScheme = darkColorScheme(
    primary = OceanBlue,
    onPrimary = Color.White,
    primaryContainer = NavyLightSurface,
    onPrimaryContainer = Color.White,
    secondary = EmeraldGreen,
    onSecondary = Color.White,
    secondaryContainer = EmeraldGreenDark,
    onSecondaryContainer = EmeraldGreenLight,
    tertiary = AmberWarning,
    onTertiary = Color.White,
    background = DarkBackground,
    onBackground = DarkTextPrimary,
    surface = DarkSurface,
    onSurface = DarkTextPrimary,
    surfaceVariant = DarkCard,
    onSurfaceVariant = DarkTextSecondary,
    outline = DarkBorder,
    error = CrimsonDanger,
    onError = Color.White,
    errorContainer = Color(0xFF450A0A),
    onErrorContainer = CrimsonDangerLight
)

private val LightColorScheme = lightColorScheme(
    primary = NavyDark,
    onPrimary = Color.White,
    primaryContainer = OceanBlueLight,
    onPrimaryContainer = NavyDark,
    secondary = EmeraldGreen,
    onSecondary = Color.White,
    secondaryContainer = EmeraldGreenLight,
    onSecondaryContainer = EmeraldGreenDark,
    tertiary = AmberWarning,
    onTertiary = Color.White,
    background = SlateBackground,
    onBackground = SlateTextPrimary,
    surface = CardBackgroundWhite,
    onSurface = SlateTextPrimary,
    surfaceVariant = Color(0xFFF1F5F9),
    onSurfaceVariant = SlateTextSecondary,
    outline = SlateBorder,
    error = CrimsonDanger,
    onError = Color.White,
    errorContainer = CrimsonDangerLight,
    onErrorContainer = Color(0xFF7F1D1D)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Keep StockSaathi's intentional professional palette
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
