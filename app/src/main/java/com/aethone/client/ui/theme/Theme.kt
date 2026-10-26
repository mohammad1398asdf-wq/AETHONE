package com.aethone.client.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.aethone.client.data.ThemeMode

/** Extra colors that the design needs beyond Material's scheme. */
data class AethoneExtras(
    val background: Color,
    val surface: Color,
    val surfaceVariant: Color,
    val text: Color,
    val textDim: Color,
    val connected: Color,
    val disconnected: Color,
    val connecting: Color,
    val mintWash: Color,
    val blushWash: Color,
    val glow: Color,
    val iconBgDown: Color,
    val iconBgUp: Color,
)

private val LightExtras = AethoneExtras(
    background = LBackground,
    surface = LSurface,
    surfaceVariant = LSurfaceVariant,
    text = LText,
    textDim = LTextDim,
    connected = LConnected,
    disconnected = LDisconnected,
    connecting = LConnecting,
    mintWash = LMintWash,
    blushWash = LBlushWash,
    glow = LMintWash,
    iconBgDown = LMintWash,
    iconBgUp = LBlushWash,
)

private val DarkExtras = AethoneExtras(
    background = DBackground,
    surface = DSurface,
    surfaceVariant = DSurfaceHigh,
    text = DText,
    textDim = DTextDim,
    connected = DConnected,
    disconnected = DDisconnected,
    connecting = LConnecting,
    mintWash = DTealDark,
    blushWash = DRedDark,
    glow = DGlowTeal,
    iconBgDown = DTealDark,
    iconBgUp = DRedDark,
)

val LocalAethoneExtras = staticCompositionLocalOf { LightExtras }

private val LightScheme = lightColorScheme(
    primary = LConnected,
    onPrimary = Color.White,
    secondary = BrandGreenDark,
    background = LBackground,
    onBackground = LText,
    surface = LSurface,
    onSurface = LText,
    surfaceVariant = LSurfaceVariant,
    onSurfaceVariant = LTextDim,
    error = LDisconnected,
    outline = Color(0xFFE2E8F0),
)

private val DarkScheme = darkColorScheme(
    primary = DConnected,
    onPrimary = Color(0xFF062B18),
    secondary = DConnected,
    background = DBackground,
    onBackground = DText,
    surface = DSurface,
    onSurface = DText,
    surfaceVariant = DSurfaceHigh,
    onSurfaceVariant = DTextDim,
    error = DDisconnected,
    outline = Color(0xFF323945),
)

@Composable
fun AethoneTheme(
    themeMode: ThemeMode,
    content: @Composable () -> Unit,
) {
    val dark = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val extras = if (dark) DarkExtras else LightExtras
    CompositionLocalProvider(LocalAethoneExtras provides extras) {
        MaterialTheme(
            colorScheme = if (dark) DarkScheme else LightScheme,
            typography = AethoneTypography,
            content = content,
        )
    }
}

/** Shorthand accessor for the extra palette. */
object Aethone {
    val extras: AethoneExtras
        @Composable get() = LocalAethoneExtras.current
}
