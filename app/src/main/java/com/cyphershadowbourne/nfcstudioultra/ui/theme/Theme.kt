package com.cyphershadowbourne.nfcstudioultra.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val UltraColorScheme = darkColorScheme(
    primary = NeonCyan,
    secondary = NeonBlue,
    tertiary = NeonMagenta,
    background = DeepNavy,
    surface = PanelSurface,
    onPrimary = TextPrimary,
    onSecondary = TextPrimary,
    onTertiary = TextPrimary,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    onSurfaceVariant = TextMuted,
    outline = PanelBorder
)

@Composable
fun NfcStudioUltraTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = UltraColorScheme,
        typography = Typography,
        content = content
    )
}
