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
    primary = PostalBlueDark,
    onPrimary = OnPostalBlueDark,
    primaryContainer = PostalBlueContainerDark,
    onPrimaryContainer = OnPostalBlueContainerDark,
    secondary = AmberAccentDark,
    onSecondary = Color.Black,
    secondaryContainer = AmberAccentContainerDark,
    onSecondaryContainer = OnAmberAccentContainerDark,
    background = SurfaceDark,
    surface = SurfaceDark,
    surfaceVariant = CardDark,
    outline = OutlineDark,
    onBackground = TextPrimaryDark,
    onSurface = TextPrimaryDark,
    onSurfaceVariant = TextSecondaryDark
)

private val LightColorScheme = lightColorScheme(
    primary = PostalBlue,
    onPrimary = Color.White,
    primaryContainer = PostalBlueContainer,
    onPrimaryContainer = OnPostalBlueContainer,
    secondary = AmberAccent,
    onSecondary = Color.White,
    secondaryContainer = AmberAccentContainer,
    onSecondaryContainer = OnAmberAccentContainer,
    background = SurfaceLight,
    surface = SurfaceLight,
    surfaceVariant = CardLight,
    outline = OutlineLight,
    onBackground = TextPrimaryLight,
    onSurface = TextPrimaryLight,
    onSurfaceVariant = TextSecondaryLight
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Use our handcrafted cohesive palette by default
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

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
