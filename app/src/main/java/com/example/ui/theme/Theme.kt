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

private val DarkColorScheme =
  darkColorScheme(
    primary = StudioColors.Accent,
    onPrimary = StudioColors.Background,
    secondary = StudioColors.AccentSoft,
    onSecondary = StudioColors.Background,
    tertiary = StudioColors.Blue,
    background = StudioColors.Background,
    onBackground = StudioColors.Text,
    surface = StudioColors.Surface,
    onSurface = StudioColors.Text,
    surfaceVariant = StudioColors.SurfaceRaised,
    onSurfaceVariant = StudioColors.TextMuted,
    outline = StudioColors.Border,
    error = StudioColors.Danger,
    onError = StudioColors.Background,
  )

private val LightColorScheme =
  lightColorScheme(
    primary = Color(0xFF526A19),
    onPrimary = Color.White,
    secondary = Color(0xFF176B59),
    tertiary = Color(0xFF3C5C99),
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  // Dynamic color is available on Android 12+
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val colorScheme =
    when {
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }

      darkTheme -> DarkColorScheme
      else -> LightColorScheme
    }

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
