package com.mahibafaris.partnerledger.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
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

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Partners", fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Button(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(Modifier.width(4.dp))
                Text("Add")
            }
        }

        Spacer(Modifier.height(12.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(totals.partnerSummaries) { ps ->
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(ps.partner.name, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Text(
                            "Capital Share: %.1f%% • Agreed: %.1f%%".format(
                                Locale.US, ps.capitalSharePct, ps.partner.agreedOwnershipPct
                            ),
                            fontSize = 12.sp,
                            color = Color.Gray
                        )

                        Spacer(Modifier.height(8.dp))
                        ThinDivider()
                        Spacer(Modifier.height(8.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column {
                                Text("Invested", fontSize = 11.sp, color = Color.Gray)
                                Text(formatMoney(biz.currency, ps.invested), fontWeight = FontWeight.SemiBold)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Withdrawn", fontSize = 11.sp, color = Color.Gray)
                                Text(formatMoney(biz.currency, ps.withdrawn), fontWeight = FontWeight.SemiBold)
                            }
                        }

                        Spacer(Modifier.height(8.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column {
                                Text("Profit Share", fontSize = 11.sp, color = Color.Gray)
                                Text(
                                    formatMoney(biz.currency, ps.profitShare),
                                    fontWeight = FontWeight.SemiBold,
                                    color = BrandSecondary
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Net Position", fontSize = 11.sp, color = Color.Gray)
                                Text(formatMoney(biz.currency, ps.netPosition), fontWeight = FontWeight.Bold)
                            }
                        }

                        if (ps.owedByBusiness > 0.0) {
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "Owed in Settle-Up: ${formatMoney(biz.currency, ps.owedByBusiness)}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = BrandAccent
                            )
                        }

                        Spacer(Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            OutlinedButton(
                                onClick = { ExportManager.sharePartnerAsImage(context, biz, ps) },
                                modifier = Modifier.weight(1f)
                            ) { Text("WhatsApp Image", fontSize = 12.sp) }
                            OutlinedButton(
                                onClick = {
                                    val partnerEntries = state.entries
                                        .filter {
                                            !it.isDeleted && (it.partnerId == ps.partner.id || it.paidByPartnerId == ps.partner.id)
                                        }
                                        .sortedByDescending { it.date + it.createdAt }
                                    ExportManager.sharePartnerStatementPdf(context, biz, ps, partnerEntries)
                                },
                                modifier = Modifier.weight(1f)
                            ) { Text("Share PDF", fontSize = 12.sp) }
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
            title = { Text("Add Partner") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Partner Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = pct,
                        onValueChange = { pct = it },
                        label = { Text("Agreed Ownership % (Optional)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (name.isNotBlank()) {
                        onAddPartner(Partner(name = name.trim(), agreedOwnershipPct = pct.toDoubleOrNull() ?: 0.0))
                        showAddDialog = false
                    }
                }) { Text("Add") }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) { Text("Cancel") }
            }
        )
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

    // Monthly Trend Data (last 6 months with activity)
    val monthlyTrend = remember(state) {
        val active = state.entries.filter { !it.isDeleted && it.date.length >= 7 }
        val months = active.map { it.date.substring(0, 7) }.distinct().sorted().takeLast(6)
        months.map { ym ->
            val rev = active.filter { it.type == EntryType.REVENUE && it.date.startsWith(ym) }.sumOf { it.amount }
            val exp = active.filter { it.type == EntryType.EXPENSE && it.date.startsWith(ym) }.sumOf { it.amount }
            Triple(ym.substring(5), rev, exp) // Month label e.g. "11"
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item { Text("Reports & Charts", fontSize = 22.sp, fontWeight = FontWeight.Bold) }

        // 1. Category Donut Chart
        item {
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Expenses by Category", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    if (expenseByCat.isEmpty()) {
                        Text("Add some expenses to see the category donut chart.", color = Color.Gray, fontSize = 13.sp)
                    } else {
                        val totalExp = expenseByCat.sumOf { it.value }.coerceAtLeast(0.01)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Canvas(modifier = Modifier.size(130.dp).padding(8.dp)) {
                                var startAngle = -90f
                                expenseByCat.forEachIndexed { idx, entry ->
                                    val sweep = ((entry.value / totalExp) * 360.0).toFloat()
                                    drawArc(
                                        color = ChartPalette[idx % ChartPalette.size],
                                        startAngle = startAngle,
                                        sweepAngle = sweep,
                                        useCenter = false,
                                        style = Stroke(width = 28f)
                                    )
                                    startAngle += sweep
                                }
                            }
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                expenseByCat.take(6).forEachIndexed { idx, (cat, amt) ->
                                    val pct = (amt / totalExp) * 100.0
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(10.dp)
                                                .background(ChartPalette[idx % ChartPalette.size], CircleShape)
                                        )
                                        Spacer(Modifier.width(6.dp))
                                        Column {
                                            Text("$cat (%.0f%%)".format(Locale.US, pct), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                            Text(formatMoney(biz.currency, amt), fontSize = 11.sp, color = Color.Gray)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 2. Monthly Revenue vs Expense Bar Chart
        item {
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Monthly Revenue vs Expenses", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(10.dp).background(BrandSecondary, CircleShape))
                            Spacer(Modifier.width(4.dp))
                            Text("Revenue", fontSize = 12.sp)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(10.dp).background(BrandDanger, CircleShape))
                            Spacer(Modifier.width(4.dp))
                            Text("Expenses", fontSize = 12.sp)
                        }
                    }

                    if (monthlyTrend.isEmpty()) {
                        Text("Log revenue or expenses to see monthly bars.", color = Color.Gray, fontSize = 13.sp)
                    } else {
                        val maxVal = monthlyTrend.maxOf { maxOf(it.second, it.third) }.coerceAtLeast(1.0)
                        Canvas(modifier = Modifier.fillMaxWidth().height(150.dp)) {
                            val groupWidth = size.width / monthlyTrend.size
                            val barW = (groupWidth * 0.28f).coerceAtMost(36f)
                            monthlyTrend.forEachIndexed { idx, (_, rev, exp) ->
                                val centerX = groupWidth * idx + groupWidth / 2f
                                val revH = ((rev / maxVal) * (size.height - 16f)).toFloat()
                                val expH = ((exp / maxVal) * (size.height - 16f)).toFloat()
                                drawRect(
                                    color = BrandSecondary,
                                    topLeft = Offset(centerX - barW - 3f, size.height - revH),
                                    size = Size(barW, revH)
                                )
                                drawRect(
                                    color = BrandDanger,
                                    topLeft = Offset(centerX + 3f, size.height - expH),
                                    size = Size(barW, expH)
                                )
                            }
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                            monthlyTrend.forEach { (monthLabel, _, _) ->
                                Text("M$monthLabel", fontSize = 11.sp, color = Color.Gray)
                            }
                        }
                    }
                }
            }
        }

        // 3. Revenue by Network / Source
        item {
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Revenue by Network / Source", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    if (revenueBySource.isEmpty()) {
                        Text("No revenue logged yet.", color = Color.Gray, fontSize = 13.sp)
                    } else {
                        revenueBySource.forEach { (src, amt) ->
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(src, fontSize = 14.sp)
                                Text(formatMoney(biz.currency, amt), fontWeight = FontWeight.SemiBold, color = BrandSecondary)
                            }
                        }
                    }
                }
            }
        }

        // 4. Generate Numbered Statement
        item {
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Generate Numbered Statement", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    DatePickerField(label = "As of Date", value = asOfDate, onValueChange = { asOfDate = it })
                    Button(
                        onClick = { onCreateStatement(asOfDate.ifBlank { todayDateString() }) },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Create Statement #${state.statements.size + 1}") }
                }
            }
        }

        // 5. Saved Statements with WhatsApp Image + PDF Share
        if (state.statements.isNotEmpty()) {
            item { Text("Saved Statements", fontWeight = FontWeight.Bold, fontSize = 16.sp) }
            items(state.statements.sortedByDescending { it.number }) { stmt ->
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text("Statement #${stmt.number}  •  As of ${stmt.asOfDate}", fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(2.dp))
                        Text(
                            "Profit: ${formatMoney(biz.currency, stmt.netProfit)} • Cash: ${formatMoney(biz.currency, stmt.currentBalance)}",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                        Spacer(Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            Button(
                                onClick = { ExportManager.shareStatementAsImage(context, biz, stmt) },
                                modifier = Modifier.weight(1f)
                            ) { Text("WhatsApp Image", fontSize = 12.sp) }
                            OutlinedButton(
                                onClick = { ExportManager.shareNumberedStatementPdf(context, biz, stmt) },
                                modifier = Modifier.weight(1f)
                            ) { Text("Share PDF", fontSize = 12.sp) }
                        }
                    }
                }
            }
        }
    }
}
