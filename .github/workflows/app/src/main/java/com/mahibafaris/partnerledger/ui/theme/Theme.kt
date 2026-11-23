package com.mahibafaris.partnerledger.ui.theme

import android.app.DatePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import java.util.Calendar
import java.util.Locale

val BrandPrimary = Color(0xFF1B4D3E)
val BrandSecondary = Color(0xFF2E7D32)
val BrandAccent = Color(0xFFD97706)
val BrandDanger = Color(0xFFC62828)
val BrandSurface = Color(0xFFF6F8F7)

val ChartPalette = listOf(
    Color(0xFF1B4D3E),
    Color(0xFFD97706),
    Color(0xFF2563EB),
    Color(0xFFDC2626),
    Color(0xFF7C3AED),
    Color(0xFF0891B2),
    Color(0xFF65A30D)
)

private val LightColors = lightColorScheme(
    primary = BrandPrimary,
    onPrimary = Color.White,
    secondary = BrandSecondary,
    onSecondary = Color.White,
    background = BrandSurface,
    surface = Color.White,
    onBackground = Color(0xFF111827),
    onSurface = Color(0xFF111827),
    error = BrandDanger
)

@Composable
fun PartnerLedgerTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColors,
        content = content
    )
}

@Composable
fun ThinDivider(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(Color(0xFFE5E7EB))
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
        trailingIcon = {
            IconButton(onClick = { openCalendar() }) {
                Icon(
                    imageVector = Icons.Default.DateRange,
                    contentDescription = "Select date from calendar",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        },
        modifier = modifier.fillMaxWidth()
    )
}
