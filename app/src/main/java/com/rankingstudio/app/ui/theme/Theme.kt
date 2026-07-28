package com.rankingstudio.app.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = PrimarySandishBrown,
    onPrimary = OnPrimaryWhite,
    primaryContainer = PrimaryContainer,
    onPrimaryContainer = OnPrimaryContainer,
    secondary = SecondaryOliveGreen,
    onSecondary = OnSecondaryWhite,
    secondaryContainer = SecondaryContainer,
    tertiary = TertiaryBrown,
    onTertiary = OnTertiaryWhite,
    background = SurfacePaper,
    onBackground = InkCharcoal,
    surface = SurfacePaper,
    onSurface = InkCharcoal,
    surfaceVariant = SurfaceContainerHigh,
    onSurfaceVariant = OnSurfaceVariant,
    outline = OutlineBrown,
    outlineVariant = OutlineVariant,
    error = ErrorRed
)

private val DarkColorScheme = darkColorScheme(
    primary = OnPrimaryContainer,
    onPrimary = PrimarySandishBrown,
    primaryContainer = PrimarySandishBrown,
    onPrimaryContainer = OnPrimaryWhite,
    secondary = SecondaryContainer,
    onSecondary = SecondaryOliveGreen,
    background = InkCharcoal,
    onBackground = SurfacePaper,
    surface = InkCharcoal,
    onSurface = SurfacePaper,
    surfaceVariant = PrimarySandishBrown,
    onSurfaceVariant = CardboardTan
)

@Composable
fun RankingStudioTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = PapercraftTypography,
        shapes = PapercraftShapes,
        content = content
    )
}
