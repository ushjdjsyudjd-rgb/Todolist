package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val SlateColorScheme = darkColorScheme(
    primary = Sky400,
    secondary = Slate500,
    tertiary = Rose500,
    background = Slate900,
    surface = Slate800,
    onBackground = Slate50,
    onSurface = Slate100,
    onPrimary = Slate900,
    surfaceVariant = Slate700,
    onSurfaceVariant = Slate300
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF6750A4),
    secondary = Color(0xFF625B71),
    tertiary = Color(0xFF7D5260),
    background = Color(0xFFFFFFFF),
    surface = Color(0xFFFBF8FF),
    onBackground = Color(0xFF1C1B1E),
    onSurface = Color(0xFF1C1B1E),
    onPrimary = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFE7E0EC),
    onSurfaceVariant = Color(0xFF49454F)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) SlateColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

