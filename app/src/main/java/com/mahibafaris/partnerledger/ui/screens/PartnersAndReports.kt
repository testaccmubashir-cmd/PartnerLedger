package com.mahibafaris.partnerledger.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mahibafaris.partnerledger.data.LedgerCalculator
import com.mahibafaris.partnerledger.export.ExportManager
import com.mahibafaris.partnerledger.model.*
import com.mahibafaris.partnerledger.ui.theme.*
import java.util.Locale

@Composable
fun PartnersScreen(
    state: AppState,
    onAddPartner: (Partner) -> Unit
) {
    val biz = state.business ?: return
    val context = LocalContext.current
    val totals = remember(state) { LedgerCalculator.compute(state) }
    var showAddDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(JetBlack)
            .padding(horizontal = 16.dp)
    ) {
        Spacer(Modifier.height(14.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Partners", fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                Text("${state.partners.size} active equity holders", fontSize = 12.sp, color = LavenderMuted)
            }
            Button(
                onClick = { showAddDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = CyanElectric, contentColor = JetBlack),
                shape = RoundedCornerShape(50),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Add", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }

        Spacer(Modifier.height(14.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            items(totals.partnerSummaries, key = { it.partner.id }) { ps ->
                val initials = ps.partner.name
                    .trim()
                    .split(" ")
                    .filter { it.isNotBlank() }
                    .take(2)
                    .joinToString("") { it.take(1).uppercase(Locale.US) }
                    .ifBlank { ps.partner.name.take(2).uppercase(Locale.US) }

                val animShare by animateFloatAsState(
                    targetValue = (ps.capitalSharePct.toFloat() / 100f).coerceIn(0.02f, 1f),
                    animationSpec = tween(700, easing = FastOutSlowInEasing),
                    label = "shareBar"
                )

                FintechCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 24.dp) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(HeroGradientBrush)
                                    .border(1.dp, CyanElectric.copy(alpha = 0.4f), RoundedCornerShape(14.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(initials, color = CyanElectric, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
                            }
                            Column {
                                Text(ps.partner.name, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                Text(
                                    "Capital Share: %.1f%% • Agreed: %.1f%%".format(
                                        Locale.US, ps.capitalSharePct, ps.partner.agreedOwnershipPct
                                    ),
                                    fontSize = 11.sp,
                                    color = LavenderMuted
                                )
                            }
                        }

                        // Animated Electric Cyan Capital Share Progress Bar
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(50))
                                .background(JetBlack)
                                .border(0.5.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(50))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth(animShare)
                                    .clip(RoundedCornerShape(50))
                                    .background(Brush.horizontalGradient(listOf(AmethystGlow, CyanElectric)))
                            )
                        }

                        // 2x2 Dark Stat Tiles
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            PartnerStatTile("INVESTED", formatMoney(biz.currency, ps.invested), Color.White, Modifier.weight(1f))
                            PartnerStatTile("WITHDRAWN", formatMoney(biz.currency, ps.withdrawn), Color.White, Modifier.weight(1f))
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            PartnerStatTile(
                                "PROFIT SHARE",
                                formatMoney(biz.currency, ps.profitShare),
                                if (ps.profitShare < 0) CoralSoft else EmeraldSoft,
                                Modifier.weight(1f)
                            )
                            PartnerStatTile("NET POSITION", formatMoney(biz.currency, ps.netPosition), CyanElectric, Modifier.weight(1f))
                        }

                        if (ps.owedByBusiness > 0.0) {
                            Text(
                                "Owed in Settle-Up: ${formatMoney(biz.currency, ps.owedByBusiness)}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = AmberSoft
                            )
                        }

                        // Action Buttons
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                            OutlinedButton(
                                onClick = { ExportManager.sharePartnerAsImage(context, biz, ps) },
                                border = BorderStroke(1.dp, ElevatedBorderColor),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = AmethystElevated,
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.weight(1f)
                            ) { Text("WhatsApp Image", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) }
                            OutlinedButton(
                                onClick = {
                                    val partnerEntries = state.entries
                                        .filter {
                                            !it.isDeleted && (it.partnerId == ps.partner.id || it.paidByPartnerId == ps.partner.id)
                                        }
                                        .sortedByDescending { it.date + it.createdAt }
                                    ExportManager.sharePartnerStatementPdf(context, biz, ps, partnerEntries)
                                },
                                border = BorderStroke(1.dp, ElevatedBorderColor),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = AmethystElevated,
                                    contentColor = CyanElectric
                                ),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.weight(1f)
                            ) { Text("Share PDF", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        var name by remember { mutableStateOf("") }
        var pct by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            containerColor = AmethystElevated,
            title = { Text("Add Partner", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Partner Name") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        colors = darkFieldColors(),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = pct,
                        onValueChange = { pct = it },
                        label = { Text("Agreed Ownership % (Optional)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        colors = darkFieldColors(),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (name.isNotBlank()) {
                            onAddPartner(Partner(name = name.trim(), agreedOwnershipPct = pct.toDoubleOrNull() ?: 0.0))
                            showAddDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CyanElectric, contentColor = JetBlack)
                ) { Text("Add", fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) { Text("Cancel", color = LavenderMuted) }
            }
        )
    }
}

@Composable
private fun PartnerStatTile(label: String, value: String, valueColor: Color, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(JetBlack.copy(alpha = 0.8f))
            .border(1.dp, AmethystGlow.copy(alpha = 0.16f), RoundedCornerShape(14.dp))
            .padding(10.dp)
    ) {
        Text(label, fontSize = 10.sp, color = LavenderMuted, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(4.dp))
        Text(value, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = valueColor, maxLines = 1)
    }
}

@Composable
fun ReportsScreen(
    state: AppState,
    onCreateStatement: (String) -> Unit
) {
    val biz = state.business ?: return
    val context = LocalContext.current
    var asOfDate by remember { mutableStateOf(todayDateString()) }

    val cMap = remember(state.categories) { state.categories.associateBy { it.id } }

    val expenseByCat = remember(state) {
        state.entries
            .filter { !it.isDeleted && it.type == EntryType.EXPENSE }
            .groupBy { cMap[it.categoryId]?.name ?: "Uncategorized" }
            .mapValues { it.value.sumOf { e -> e.amount } }
            .entries.sortedByDescending { it.value }
    }

    val revenueBySource = remember(state) {
        state.entries
            .filter { !it.isDeleted && it.type == EntryType.REVENUE }
            .groupBy { it.vendorOrSource.ifBlank { cMap[it.categoryId]?.name ?: "Other Revenue" } }
            .mapValues { it.value.sumOf { e -> e.amount } }
            .entries.sortedByDescending { it.value }
    }

    val monthlyTrend = remember(state) {
        val active = state.entries.filter { !it.isDeleted && it.date.length >= 7 }
        val months = active.map { it.date.substring(0, 7) }.distinct().sorted().takeLast(6)
        months.map { ym ->
            val rev = active.filter { it.type == EntryType.REVENUE && it.date.startsWith(ym) }.sumOf { it.amount }
            val exp = active.filter { it.type == EntryType.EXPENSE && it.date.startsWith(ym) }.sumOf { it.amount }
            Triple(ym.substring(5), rev, exp)
        }
    }

    // Trigger chart entrance animation
    var chartStarted by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { chartStarted = true }
    val chartProgress by animateFloatAsState(
        targetValue = if (chartStarted) 1f else 0f,
        animationSpec = tween(800, easing = FastOutSlowInEasing),
        label = "chartAnim"
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(JetBlack)
            .imePadding()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 14.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text("Reports & Charts", fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
            Text("Financial breakdown & audit statements", fontSize = 12.sp, color = LavenderMuted)
        }

        // 1. Animated Category Donut Chart
        item {
            FintechCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 24.dp) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Expenses by Category", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.White)
                    if (expenseByCat.isEmpty()) {
                        Text("Add some expenses to see the category donut chart.", color = LavenderMuted, fontSize = 13.sp)
                    } else {
                        val totalExp = expenseByCat.sumOf { it.value }.coerceAtLeast(0.01)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Box(modifier = Modifier.size(126.dp), contentAlignment = Alignment.Center) {
                                Canvas(modifier = Modifier.fillMaxSize().padding(8.dp)) {
                                    drawArc(
                                        color = AmethystElevated,
                                        startAngle = 0f,
                                        sweepAngle = 360f,
                                        useCenter = false,
                                        style = Stroke(width = 26f)
                                    )
                                    var startAngle = -90f
                                    expenseByCat.forEachIndexed { idx, entry ->
                                        val sweep = ((entry.value / totalExp) * 360.0).toFloat() * chartProgress
                                        drawArc(
                                            color = ChartPalette[idx % ChartPalette.size],
                                            startAngle = startAngle,
                                            sweepAngle = sweep,
                                            useCenter = false,
                                            style = Stroke(width = 26f, cap = StrokeCap.Round)
                                        )
                                        startAngle += sweep
                                    }
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("100%", fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, color = Color.White)
                                    Text("EXPENSES", fontSize = 9.sp, color = LavenderMuted)
                                }
                            }
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                expenseByCat.take(6).forEachIndexed { idx, (cat, amt) ->
                                    val pct = (amt / totalExp) * 100.0
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(10.dp)
                                                .clip(CircleShape)
                                                .background(ChartPalette[idx % ChartPalette.size])
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        Column {
                                            Text("$cat (%.0f%%)".format(Locale.US, pct), fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                                            Text(formatMoney(biz.currency, amt), fontSize = 11.sp, color = LavenderMuted)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 2. Animated Monthly Revenue vs Expenses Bar Chart
        item {
            FintechCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 24.dp) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Monthly Revenue vs Expenses", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.White)
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(9.dp).clip(CircleShape).background(EmeraldSoft))
                            Spacer(Modifier.width(6.dp))
                            Text("Revenue", fontSize = 11.sp, color = LavenderMuted)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(9.dp).clip(CircleShape).background(CoralSoft))
                            Spacer(Modifier.width(6.dp))
                            Text("Expenses", fontSize = 11.sp, color = LavenderMuted)
                        }
                    }

                    if (monthlyTrend.isEmpty()) {
                        Text("Log revenue or expenses to see monthly bars.", color = LavenderMuted, fontSize = 13.sp)
                    } else {
                        val maxVal = monthlyTrend.maxOf { maxOf(it.second, it.third) }.coerceAtLeast(1.0)
                        Canvas(modifier = Modifier.fillMaxWidth().height(150.dp)) {
                            val dash = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                            for (i in 0..3) {
                                val y = size.height * (i / 3f)
                                drawLine(
                                    color = AmethystGlow.copy(alpha = 0.22f),
                                    start = Offset(0f, y),
                                    end = Offset(size.width, y),
                                    strokeWidth = 1.5f,
                                    pathEffect = if (i < 3) dash else null
                                )
                            }

                            val groupWidth = size.width / monthlyTrend.size
                            val barW = (groupWidth * 0.24f).coerceAtMost(38f)
                            monthlyTrend.forEachIndexed { idx, (_, rev, exp) ->
                                val centerX = groupWidth * idx + groupWidth / 2f
                                val revH = (((rev / maxVal) * (size.height - 14f)).toFloat() * chartProgress).coerceAtLeast(4f)
                                val expH = (((exp / maxVal) * (size.height - 14f)).toFloat() * chartProgress).coerceAtLeast(4f)
                                drawRoundRect(
                                    color = EmeraldSoft,
                                    topLeft = Offset(centerX - barW - 4f, size.height - revH),
                                    size = Size(barW, revH),
                                    cornerRadius = CornerRadius(10f, 10f)
                                )
                                drawRoundRect(
                                    color = CoralSoft,
                                    topLeft = Offset(centerX + 4f, size.height - expH),
                                    size = Size(barW, expH),
                                    cornerRadius = CornerRadius(10f, 10f)
                                )
                            }
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                            monthlyTrend.forEach { (monthLabel, _, _) ->
                                Text("M$monthLabel", fontSize = 11.sp, color = LavenderMuted, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }
        }

        // 3. Revenue by Network / Source
        item {
            FintechCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 24.dp) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Revenue by Network / Source", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.White)
                    if (revenueBySource.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(JetBlack.copy(alpha = 0.7f))
                                .border(1.dp, AmethystGlow.copy(alpha = 0.22f), RoundedCornerShape(14.dp))
                                .padding(18.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No revenue logged yet.", color = LavenderMuted, fontSize = 12.sp)
                        }
                    } else {
                        revenueBySource.forEach { (src, amt) ->
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(src, fontSize = 13.sp, color = Color.White)
                                Text(formatMoney(biz.currency, amt), fontWeight = FontWeight.Bold, color = EmeraldSoft, fontSize = 13.sp)
                            }
                        }
                    }
                }
            }
        }

        // 4. Generate Numbered Statement
        item {
            FintechCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 24.dp) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Generate Numbered Statement", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.White)
                    DatePickerField(label = "As of Date", value = asOfDate, onValueChange = { asOfDate = it })
                    Button(
                        onClick = { onCreateStatement(asOfDate.ifBlank { todayDateString() }) },
                        colors = ButtonDefaults.buttonColors(containerColor = CyanElectric, contentColor = JetBlack),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) { Text("Create Statement #${state.statements.size + 1}", fontWeight = FontWeight.Bold) }
                }
            }
        }

        // 5. Saved Statements
        if (state.statements.isNotEmpty()) {
            item { Text("Saved Statements", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White) }
            items(state.statements.sortedByDescending { it.number }, key = { it.id }) { stmt ->
                FintechCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 18.dp) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text("Statement #${stmt.number}  •  As of ${stmt.asOfDate}", fontWeight = FontWeight.Bold, color = Color.White)
                        Spacer(Modifier.height(3.dp))
                        Text(
                            "Profit: ${formatMoney(biz.currency, stmt.netProfit)} • Cash: ${formatMoney(biz.currency, stmt.currentBalance)}",
                            fontSize = 12.sp,
                            color = LavenderMuted
                        )
                        Spacer(Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            Button(
                                onClick = { ExportManager.shareStatementAsImage(context, biz, stmt) },
                                colors = ButtonDefaults.buttonColors(containerColor = CyanElectric, contentColor = JetBlack),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            ) { Text("WhatsApp Image", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                            OutlinedButton(
                                onClick = { ExportManager.shareNumberedStatementPdf(context, biz, stmt) },
                                border = BorderStroke(1.dp, CyanElectric.copy(alpha = 0.55f)),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = CyanElectric),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            ) { Text("Share PDF", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                        }
                    }
                }
            }
        }
    }
}
