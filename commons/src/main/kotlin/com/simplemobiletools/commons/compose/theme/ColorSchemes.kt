package com.simplemobiletools.commons.compose.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.simplemobiletools.commons.extensions.getContrastColor

internal val darkColorScheme = darkColorScheme(
    primary = color_primary,
    onPrimary = Color(color_primary.toArgb().getContrastColor()),
    secondary = color_primary_dark,
    onSecondary = Color(color_primary_dark.toArgb().getContrastColor()),
    tertiary = color_accent,
    onTertiary = Color(color_accent.toArgb().getContrastColor()),
)
internal val lightColorScheme = lightColorScheme(
    primary = color_primary,
    onPrimary = Color(color_primary.toArgb().getContrastColor()),
    secondary = color_primary_dark,
    onSecondary = Color(color_primary_dark.toArgb().getContrastColor()),
    tertiary = color_accent,
    onTertiary = Color(color_accent.toArgb().getContrastColor()),
)
