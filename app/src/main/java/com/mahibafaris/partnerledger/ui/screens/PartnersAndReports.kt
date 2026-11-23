package com.mahibafaris.partnerledger.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.mahibafaris.partnerledger.data.LedgerRepository
import com.mahibafaris.partnerledger.export.ExportManager
import com.mahibafaris.partnerledger.model.*
import com.mahibafaris.partnerledger.ui.theme.AppColors
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs

@Composable
fun PartnersListScreen(
    biz: BusinessConfig,
    metrics: DashboardMetrics,
    onSelectPartner: (String) -> Unit,
    onAddPartner: (Partner, Double) -> Unit
) {
    var showAddDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 88.dp)
    ) {
        item {
            Button(onClick = { showAddDialog = true }, modifier = Modifier.fillMaxWidth().height(48.dp)) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text("Add Partner")
            }
        }
        items(metrics.partnerSummaries) { ps ->
            Card(
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth().clickable { onSelectPartner(ps.partner.id) }
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(ps.partner.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Net: ${biz.currency} %,.2f".format(ps.netPosition),
                            fontWeight = FontWeight.Bold,
                            color = if (ps.netPosition >= 0) AppColors.MoneyIn else AppColors.MoneyOut
                        )
                    }
                    Text("Invested: ${biz.currency} %,.2f • Withdrawn: ${biz.currency} %,.2f".format(ps.invested, ps.withdrawn), style = MaterialTheme.typography.bodySmall)
                    Text("Capital Share: %.1f%% • Agreed: %.1f%% • ROI: %.1f%%".format(ps.capitalSharePct, ps.partner.agreedOwnershipPct, ps.roiPct), style = MaterialTheme.typography.bodySmall)
                    if (ps.partner.agreedOwnershipPct > 0.0 && abs(ps.ownershipGapPct) >= 1.0) {
                        Text("Ownership Gap: %+.1f%%".format(ps.ownershipGapPct), color = AppColors.Warning, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        var name by remember { mutableStateOf("") }
        var contact by remember { mutableStateOf("") }
        var agreed by remember { mutableStateOf("") }
        var opening by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Add Partner") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name *") })
                    OutlinedTextField(value = contact, onValueChange = { contact = it }, label = { Text("Phone / Email") })
                    OutlinedTextField(value = agreed, onValueChange = { agreed = it }, label = { Text("Agreed Ownership %") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
                    OutlinedTextField(value = opening, onValueChange = { opening = it }, label = { Text("Opening Investment (${biz.currency})") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (name.isNotBlank()) {
                        onAddPartner(
                            Partner(UUID.randomUUID().toString(), name.trim(), contact.trim(), agreed.toDoubleOrNull() ?: 0.0),
                            opening.toDoubleOrNull() ?: 0.0
                        )
                        showAddDialog = false
                    }
                }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { showAddDialog = false }) { Text("Cancel") } }
        )
    }
}

@Composable
fun PartnerDetailScreen(
    biz: BusinessConfig,
    state: AppState,
    ps: PartnerSummary,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val partnerEntries = state.entries.filter { !it.isDeleted && it.partnerId == ps.partner.id }
    val partnerCommitments = state.commitments.filter { it.partnerId == ps.partner.id }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
            Text(ps.partner.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }

        Card(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Invested: ${biz.currency} %,.2f".format(ps.invested), fontWeight = FontWeight.SemiBold)
                Text("Withdrawn: ${biz.currency} %,.2f".format(ps.withdrawn))
                Text("Capital Share: %.1f%%  |  Agreed Ownership: %.1f%%".format(ps.capitalSharePct, ps.partner.agreedOwnershipPct))
                Text("Profit Share: ${biz.currency} %,.2f  |  ROI: %.1f%%".format(ps.profitShare, ps.roiPct))
                if (state.settings.interestOnCapitalEnabled) {
                    Text("Interest on Capital (${state.settings.interestRatePct}%): ${biz.currency} %,.2f".format(ps.interestCredit))
                }
                if (state.settings.salaryCreditEnabled) {
                    Text("Salary / Effort Credit: ${biz.currency} %,.2f".format(ps.salaryCredit))
                }
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                Text("Net Position: ${biz.currency} %,.2f".format(ps.netPosition), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                if (ps.owedByBusiness > 0.0) {
                    Text("Settle-Up Balance: Business owes ${biz.currency} %,.2f".format(ps.owedByBusiness), color = AppColors.Warning, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        Button(
            onClick = { ExportManager.sharePartnerStatementPdf(context, biz, ps, partnerEntries) },
            modifier = Modifier.fillMaxWidth().height(48.dp)
        ) {
            Icon(Icons.Default.PictureAsPdf, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Export Partner Statement (PDF)")
        }

        if (partnerCommitments.isNotEmpty()) {
            Text("Pending Commitments", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            partnerCommitments.forEach { c ->
                Card(shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Promised: ${biz.currency} %,.2f • Paid: ${biz.currency} %,.2f".format(c.promisedAmount, c.paidAmount), fontWeight = FontWeight.SemiBold)
                        Text("Due: ${c.dueDate}", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }

        Text("Contribution & Withdrawal Timeline (${partnerEntries.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        if (partnerEntries.isEmpty()) {
            Text("No activity recorded for this partner yet.", style = MaterialTheme.typography.bodySmall)
        } else {
            partnerEntries.forEach { e ->
                Card(shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.padding(12.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text("${e.type.label} • ${e.date}", fontWeight = FontWeight.SemiBold)
                            if (e.note.isNotBlank()) Text(e.note, style = MaterialTheme.typography.bodySmall)
                        }
                        Text(
                            "${if (e.type == EntryType.INVESTMENT) "+" else "-"}${biz.currency} %,.2f".format(e.amount),
                            fontWeight = FontWeight.Bold,
                            color = if (e.type == EntryType.INVESTMENT) AppColors.MoneyIn else AppColors.MoneyOut
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ReportsHubScreen(
    biz: BusinessConfig,
    state: AppState,
    metrics: DashboardMetrics,
    repo: LedgerRepository,
    onStateUpdated: (AppState) -> Unit
) {
    val context = LocalContext.current
    var subView by remember { mutableStateOf("HUB") } // HUB, STATEMENTS, CATEGORIES, TRENDS

    when (subView) {
        "STATEMENTS" -> NumberedStatementsScreen(biz, state, repo, onStateUpdated) { subView = "HUB" }
        "CATEGORIES" -> CategoryBreakdownScreen(biz, state, metrics) { subView = "HUB" }
        "TRENDS" -> TrendsComparisonScreen(biz, metrics) { subView = "HUB" }
        else -> {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                item { Text("Reports & Exports", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) }
                item {
                    ReportHubCard("Numbered PDF Statements", "Generate Statement #${biz.nextStatementNumber} with immutable snapshot & audit summary", Icons.Default.PictureAsPdf) {
                        subView = "STATEMENTS"
                    }
                }
                item {
                    ReportHubCard("Expenses by Category", "Ranked category breakdown & percentages", Icons.Default.PieChart) {
                        subView = "CATEGORIES"
                    }
                }
                item {
                    ReportHubCard("Trends & Forecast", "Month-by-month totals, period comparison, and next-month estimate", Icons.Default.TrendingUp) {
                        subView = "TRENDS"
                    }
                }
                item {
                    ReportHubCard("Full Ledger + Audit Export (CSV / Excel)", "Share complete accountant-ready spreadsheet", Icons.Default.TableChart) {
                        ExportManager.exportLedgerCsv(context, state)
                    }
                }
            }
        }
    }
}

@Composable
private fun ReportHubCard(title: String, subtitle: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Card(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
            Spacer(Modifier.width(14.dp))
            Column {
                Text(title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                Text(subtitle, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun NumberedStatementsScreen(
    biz: BusinessConfig,
    state: AppState,
    repo: LedgerRepository,
    onStateUpdated: (AppState) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var asOfDate by remember { mutableStateOf(SimpleDateFormat("dd MMM yyyy", Locale.US).format(Date())) }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
                Text("Numbered Statements", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }
        }
        item {
            Card(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Next Number: Statement #${biz.nextStatementNumber}", fontWeight = FontWeight.Bold)
                    OutlinedTextField(value = asOfDate, onValueChange = { asOfDate = it }, label = { Text("As of Date") }, modifier = Modifier.fillMaxWidth())
                    Button(
                        onClick = {
                            val (updated, snap) = repo.generateNextStatement(state, asOfDate.trim())
                            onStateUpdated(updated)
                            ExportManager.shareNumberedStatementPdf(context, updated.business!!, snap)
                        },
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        Text("Generate & Share Statement #${biz.nextStatementNumber}")
                    }
                }
            }
        }
        item { Text("Statement History (Locked Snapshots)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
        items(state.statements) { s ->
            Card(shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
                Row(modifier = Modifier.padding(14.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Statement #${s.number} • ${s.asOfDate}", fontWeight = FontWeight.Bold)
                        Text("Balance: ${biz.currency} %,.2f".format(s.currentBalance), style = MaterialTheme.typography.bodySmall)
                        Text(s.auditSummary, style = MaterialTheme.typography.labelSmall)
                    }
                    IconButton(onClick = { ExportManager.shareNumberedStatementPdf(context, biz, s) }) {
                        Icon(Icons.Default.Share, contentDescription = "Share")
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryBreakdownScreen(biz: BusinessConfig, state: AppState, metrics: DashboardMetrics, onBack: () -> Unit) {
    val activeExp = state.entries.filter { !it.isDeleted && it.type == EntryType.EXPENSE }
    val catMap = state.categories.associateBy { it.id }
    val ranked = activeExp.groupBy { it.categoryId }
        .map { (id, list) -> (catMap[id]?.name ?: "Uncategorized") to list.sumOf { it.amount } }
        .sortedByDescending { it.second }

    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
                Text("Expenses by Category", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }
        }
        if (ranked.isEmpty()) {
            item { Text("No expenses recorded yet.") }
        } else {
            items(ranked) { (name, sum) ->
                val pct = if (metrics.totalExpenses > 0.0) (sum / metrics.totalExpenses).toFloat() else 0f
                Card(shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(name, fontWeight = FontWeight.Bold)
                            Text("${biz.currency} %,.2f (%.1f%%)".format(sum, pct * 100f), fontWeight = FontWeight.SemiBold)
                        }
                        LinearProgressIndicator(progress = { pct }, modifier = Modifier.fillMaxWidth())
                    }
                }
            }
        }
    }
}

@Composable
private fun TrendsComparisonScreen(biz: BusinessConfig, metrics: DashboardMetrics, onBack: () -> Unit) {
    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
                Text("Trends & Forecast", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }
        }
        item {
            Card(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Next-Month Spending Forecast (Estimate)", fontWeight = FontWeight.Bold)
                    Text("${biz.currency} %,.2f".format(metrics.cashHealth.avgMonthlyExpense), style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
                    Text("Plain average of ${metrics.cashHealth.monthsOfData} recorded month(s).", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
        items(metrics.monthlyBuckets.reversed()) { b ->
            Card(shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(b.monthKey, fontWeight = FontWeight.Bold)
                    Text("Invested: ${biz.currency} %,.0f  •  Revenue: ${biz.currency} %,.0f  •  Expenses: ${biz.currency} %,.0f".format(b.invested, b.revenue, b.expenses), style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}
