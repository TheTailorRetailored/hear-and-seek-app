package com.bridgesdigital.hearandseek.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val HearAndSeekColors = darkColorScheme(
    primary = Lime,
    onPrimary = Night,
    secondary = Aqua,
    onSecondary = Night,
    background = Night,
    onBackground = SoftWhite,
    surface = DeepSurface,
    onSurface = SoftWhite,
    error = Danger,
)

@Composable
fun HearAndSeekTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = HearAndSeekColors,
        content = content,
    )
}
