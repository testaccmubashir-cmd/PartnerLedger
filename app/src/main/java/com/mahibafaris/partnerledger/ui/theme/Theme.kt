package com.mahibafaris.partnerledger.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.mahibafaris.partnerledger.model.ThemeMode

object AppColors {
    val DeepTeal = Color(0xFF0F4C5C)
    val DeepTealLight = Color(0xFF2B7A8B)
    val MoneyIn = Color(0xFF15803D)
    val MoneyInBg = Color(0xFFDCFCE7)
    val MoneyOut = Color(0xFFB91C1C)
    val MoneyOutBg = Color(0xFFFEE2E2)
    val Warning = Color(0xFFB45309)
    val WarningBg = Color(0xFFFEF3C7)
    val DarkSurface = Color(0xFF14191F)
    val DarkCard = Color(0xFF1E252D)
}

private val LightColors = lightColorScheme(
    primary = AppColors.DeepTeal,
    onPrimary = Color.White,
    secondary = AppColors.DeepTealLight,
    tertiary = AppColors.Warning,
    background = Color(0xFFF4F6F8),
    surface = Color.White,
    surfaceVariant = Color(0xFFE9EEF2),
    onBackground = Color(0xFF111827),
    onSurface = Color(0xFF111827)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF4FB3C8),
    onPrimary = Color(0xFF06252E),
    secondary = Color(0xFF7DD3E8),
    tertiary = Color(0xFFFBBF24),
    background = AppColors.DarkSurface,
    surface = AppColors.DarkCard,
    surfaceVariant = Color(0xFF28323D),
    onBackground = Color(0xFFF3F4F6),
    onSurface = Color(0xFFF3F4F6)
)

@Composable
fun PartnerLedgerTheme(
    themeMode: ThemeMode,
    content: @Composable () -> Unit
) {
    val useDark = when (themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }
    MaterialTheme(
        colorScheme = if (useDark) DarkColors else LightColors,
        content = content
    )
}
