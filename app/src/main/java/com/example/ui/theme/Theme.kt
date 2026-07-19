package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

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

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // Force dark theme for slate dark request
    dynamicColor: Boolean = false, // Disable dynamic colors to preserve custom Slate styling
    content: @Composable () -> Unit,
) {
    val colorScheme = SlateColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
