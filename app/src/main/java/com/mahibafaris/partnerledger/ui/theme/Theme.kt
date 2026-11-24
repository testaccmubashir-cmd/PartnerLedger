package com.mahibafaris.partnerledger.ui.theme

import android.app.DatePickerDialog
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Calendar
import java.util.Locale

// 60 / 30 / 10 Dark Fintech Palette (matching Couponovo Redesign)
val JetBlack = Color(0xFF050505)           // 60% Primary Background
val AmethystDeep = Color(0xFF160826)       // 30% Card Surface
val AmethystElevated = Color(0xFF220B3A)   // Elevated Surface / Modals / Chips
val AmethystRoyal = Color(0xFF3B0A6B)      // Hero Gradient Top
val AmethystGlow = Color(0xFF8A4FFF)       // Subtle Borders & Chart Accent
val NavBarBg = Color(0xFF0C0514)           // Bottom Nav Surface

val CyanElectric = Color(0xFF00E5FF)       // 10% Primary Accent / CTAs / Active
val EmeraldSoft = Color(0xFF00E676)        // Positive / Revenue / Invested
val CoralSoft = Color(0xFFFF4D6D)          // Expenses / Negative
val AmberSoft = Color(0xFFFFB74D)          // Withdrawals / Settle-Up
val LavenderMuted = Color(0xFFA699B8)      // Secondary Text

// Aliases for compatibility
val BrandPrimary = CyanElectric
val BrandSecondary = EmeraldSoft
val BrandAccent = AmberSoft
val BrandDanger = CoralSoft
val BrandSurface = JetBlack

val CardBorderColor = AmethystGlow.copy(alpha = 0.24f)
val ElevatedBorderColor = AmethystGlow.copy(alpha = 0.34f)

val HeroGradientBrush = Brush.linearGradient(
    colors = listOf(AmethystRoyal, Color(0xFF240744), Color(0xFF16062B))
)

val ChartPalette = listOf(
    CyanElectric,
    AmethystGlow,
    EmeraldSoft,
    CoralSoft,
    AmberSoft,
    Color(0xFF38BDF8),
    Color(0xFFC084FC)
)

private val DarkFintechColors = darkColorScheme(
    primary = CyanElectric,
    onPrimary = JetBlack,
    secondary = AmethystGlow,
    onSecondary = Color.White,
    tertiary = EmeraldSoft,
    background = JetBlack,
    onBackground = Color.White,
    surface = AmethystDeep,
    onSurface = Color.White,
    surfaceVariant = AmethystElevated,
    onSurfaceVariant = LavenderMuted,
    outline = AmethystGlow.copy(alpha = 0.32f),
    error = CoralSoft,
    onError = Color.White
)

@Composable
fun PartnerLedgerTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkFintechColors,
        content = content
    )
}

@Composable
fun darkFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = CyanElectric,
    unfocusedBorderColor = AmethystGlow.copy(alpha = 0.30f),
    focusedContainerColor = JetBlack,
    unfocusedContainerColor = JetBlack,
    focusedTextColor = Color.White,
    unfocusedTextColor = Color.White,
    cursorColor = CyanElectric,
    focusedLabelColor = CyanElectric,
    unfocusedLabelColor = LavenderMuted,
    focusedPlaceholderColor = LavenderMuted.copy(alpha = 0.6f),
    unfocusedPlaceholderColor = LavenderMuted.copy(alpha = 0.6f)
)

@Composable
fun FintechCard(
    modifier: Modifier = Modifier,
    elevated: Boolean = false,
    cornerRadius: Dp = 20.dp,
    borderColor: Color = if (elevated) ElevatedBorderColor else CardBorderColor,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(cornerRadius)
    val bg = if (elevated) AmethystElevated else AmethystDeep
    val clickMod = if (onClick != null) Modifier.clickable { onClick() } else Modifier
    Surface(
        modifier = modifier
            .clip(shape)
            .then(clickMod),
        shape = shape,
        color = bg,
        border = BorderStroke(1.dp, borderColor)
    ) {
        Column(content = content)
    }
}

@Composable
fun FintechChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bg by animateColorAsState(
        targetValue = if (selected) CyanElectric else JetBlack,
        animationSpec = tween(180),
        label = "chipBg"
    )
    val txt by animateColorAsState(
        targetValue = if (selected) JetBlack else LavenderMuted,
        animationSpec = tween(180),
        label = "chipTxt"
    )
    val borderCol by animateColorAsState(
        targetValue = if (selected) CyanElectric else AmethystGlow.copy(alpha = 0.28f),
        animationSpec = tween(180),
        label = "chipBorder"
    )

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(bg)
            .border(1.dp, borderCol, RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = txt,
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            maxLines = 1
        )
    }
}

@Composable
fun ThinDivider(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(AmethystGlow.copy(alpha = 0.20f))
    )
}

@Composable
fun DatePickerField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    fun openCalendar() {
        val cal = Calendar.getInstance()
        val parts = value.split("-")
        val initYear = parts.getOrNull(0)?.toIntOrNull() ?: cal.get(Calendar.YEAR)
        val initMonth = (parts.getOrNull(1)?.toIntOrNull()?.minus(1)) ?: cal.get(Calendar.MONTH)
        val initDay = parts.getOrNull(2)?.toIntOrNull() ?: cal.get(Calendar.DAY_OF_MONTH)

        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val formatted = String.format(Locale.US, "%04d-%02d-%02d", year, month + 1, dayOfMonth)
                onValueChange(formatted)
            },
            initYear,
            initMonth.coerceIn(0, 11),
            initDay.coerceIn(1, 31)
        ).show()
    }

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        placeholder = { Text("YYYY-MM-DD") },
        singleLine = true,
        shape = RoundedCornerShape(16.dp),
        colors = darkFieldColors(),
        trailingIcon = {
            IconButton(onClick = { openCalendar() }) {
                Icon(
                    imageVector = Icons.Default.DateRange,
                    contentDescription = "Select date",
                    tint = CyanElectric
                )
            }
        },
        modifier = modifier.fillMaxWidth()
    )
}
