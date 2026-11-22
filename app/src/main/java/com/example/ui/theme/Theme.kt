package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val SlipBlogLightColorScheme = lightColorScheme(
    primary = NavyDeep,
    onPrimary = SurfaceWhite,
    primaryContainer = SkyTint,
    onPrimaryContainer = NavyDeep,
    secondary = ElectricSapphire,
    onSecondary = SurfaceWhite,
    secondaryContainer = SurfaceSubtle,
    onSecondaryContainer = NavyDeep,
    tertiary = EmeraldSuccess,
    onTertiary = SurfaceWhite,
    tertiaryContainer = EmeraldTint,
    onTertiaryContainer = NavyDeep,
    background = BackgroundCrisp,
    onBackground = NavyDeep,
    surface = SurfaceWhite,
    onSurface = SlateInk,
    surfaceVariant = SurfaceSubtle,
    onSurfaceVariant = SlateMuted,
    outline = BorderLight,
    outlineVariant = BorderLight,
    error = CoralError,
    onError = SurfaceWhite,
    errorContainer = CoralTint,
    onErrorContainer = CoralError
)

private val SlipBlogDarkColorScheme = darkColorScheme(
    primary = ElectricSky,
    onPrimary = NavyDeep,
    primaryContainer = NavyElevated,
    onPrimaryContainer = SurfaceWhite,
    secondary = ElectricSapphire,
    onSecondary = SurfaceWhite,
    secondaryContainer = NavySurface,
    onSecondaryContainer = SurfaceWhite,
    tertiary = EmeraldSuccess,
    onTertiary = SurfaceWhite,
    background = NavyDeep,
    onBackground = SurfaceWhite,
    surface = NavySurface,
    onSurface = SurfaceWhite,
    surfaceVariant = NavyElevated,
    onSurfaceVariant = Color(0xFF94A3B8),
    outline = Color(0xFF233858),
    outlineVariant = Color(0xFF1D314F),
    error = Color(0xFFF87171),
    onError = NavyDeep
)

@Composable
fun SlipBlogTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) SlipBlogDarkColorScheme else SlipBlogLightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = SlipBlogTypography,
        content = content
    )
}
