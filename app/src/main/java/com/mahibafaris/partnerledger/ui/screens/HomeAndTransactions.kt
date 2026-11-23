package com.mahibafaris.partnerledger.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.mahibafaris.partnerledger.model.*
import com.mahibafaris.partnerledger.ui.theme.AppColors
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs

@Composable
fun HomeDashboardScreen(
    biz: BusinessConfig,
    state: AppState,
    metrics: DashboardMetrics,
    onQuickAdd: () -> Unit,
    onOpenReconcile: () -> Unit,
    onOpenStatements: () -> Unit,
    onOpenBackup: () -> Unit,
    onOpenAllTransactions: () -> Unit,
    onOpenPartner: (String) -> Unit,
    onOpenEntry: (LedgerEntry) -> Unit
) {
    var dismissBackupBanner by remember { mutableStateOf(false) }
    var dismissDataBanner by remember { mutableStateOf(false) }

    val daysSinceBackup = if (biz.lastBackupTimestamp == 0L) 999L else {
        (System.currentTimeMillis() - biz.lastBackupTimestamp) / (1000L * 60 * 60 * 24)
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 88.dp)
    ) {
        // Alert Banners (dismissible)
        if (!dismissBackupBanner && daysSinceBackup >= state.settings.backupReminderDays) {
            item {
                AlertBannerCard(
                    text = if (biz.lastBackupTimestamp == 0L) "No Google Drive backup yet. Tap to back up now."
                    else "Backup overdue ($daysSinceBackup days). Tap to back up.",
                    bgColor = AppColors.WarningBg,
                    fgColor = AppColors.Warning,
                    onClick = onOpenBackup,
                    onDismiss = { dismissBackupBanner = true }
                )
            }
        }
        if (!dismissDataBanner && (metrics.missingProofCount > 0 || metrics.missingCategoryCount > 0)) {
            item {
                AlertBannerCard(
                    text = "${metrics.missingProofCount} entries missing receipt proof • ${metrics.missingCategoryCount} uncategorized",
                    bgColor = AppColors.MoneyOutBg,
                    fgColor = AppColors.MoneyOut,
                    onClick = onOpenAllTransactions,
                    onDismiss = { dismissDataBanner = true }
                )
            }
        }
        if (metrics.currentBalance < 0.0) {
            item {
                AlertBannerCard(
                    text = "Warning: Calculated cash balance is negative (${biz.currency} %,.2f)".format(metrics.currentBalance),
                    bgColor = AppColors.MoneyOutBg,
                    fgColor = AppColors.MoneyOut,
                    onClick = onOpenReconcile,
                    onDismiss = null
                )
            }
        }
        if (metrics.overdrawnPartners.isNotEmpty()) {
            item {
                AlertBannerCard(
                    text = "Over-withdrawal alert: ${metrics.overdrawnPartners.joinToString(", ")} withdrew more than their share.",
                    bgColor = AppColors.WarningBg,
                    fgColor = AppColors.Warning,
                    onClick = {},
                    onDismiss = null
                )
            }
        }

        // Hero Balance Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text("Current Balance", color = Color.White.copy(alpha = 0.8f), style = MaterialTheme.typography.labelLarge)
                    Text(
                        "${biz.currency} %,.2f".format(metrics.currentBalance),
                        color = Color.White,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(14.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        HeroStat("Capital In", "${biz.currency} %,.0f".format(metrics.totalInvested))
                        HeroStat("Revenue", "${biz.currency} %,.0f".format(metrics.totalRevenue))
                        HeroStat("Expenses", "${biz.currency} %,.0f".format(metrics.totalExpenses))
                    }
                }
            }
        }

        // Quick Actions Row
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                QuickActionChip("Add", Icons.Default.Add, Modifier.weight(1f), onQuickAdd)
                QuickActionChip("Reconcile", Icons.Default.AccountBalance, Modifier.weight(1f), onOpenReconcile)
                QuickActionChip("Statement", Icons.Default.PictureAsPdf, Modifier.weight(1f), onOpenStatements)
                QuickActionChip("Backup", Icons.Default.CloudUpload, Modifier.weight(1f), onOpenBackup)
            }
        }

        // Business Health Strip
        item {
            val burnText = if (metrics.cashHealth.monthlyNetBurn <= 0.0) "Not burning" else "${biz.currency} %,.0f/mo".format(metrics.cashHealth.monthlyNetBurn)
            val runwayText = metrics.cashHealth.runwayMonths?.let { "%.1f mo".format(it) } ?: "Not burning"
            Card(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.padding(14.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Net Burn", style = MaterialTheme.typography.labelSmall)
                        Text(burnText, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                    }
                    Column {
                        Text("Runway", style = MaterialTheme.typography.labelSmall)
                        Text(runwayText, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                    }
                    Column {
                        Text("This Month", style = MaterialTheme.typography.labelSmall)
                        Text(
                            "${if (metrics.thisMonthProfit >= 0) "+" else ""}${biz.currency} %,.0f".format(metrics.thisMonthProfit),
                            fontWeight = FontWeight.Bold,
                            color = if (metrics.thisMonthProfit >= 0) AppColors.MoneyIn else AppColors.MoneyOut,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }

        // Horizontal Partner Cards
        item {
            Text("Partners", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(metrics.partnerSummaries) { ps ->
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.width(220.dp).clickable { onOpenPartner(ps.partner.id) }
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(ps.partner.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                                if (ps.partner.agreedOwnershipPct > 0.0 && abs(ps.ownershipGapPct) >= 1.0) {
                                    Text("Gap %+.1f%%".format(ps.ownershipGapPct), color = AppColors.Warning, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                }
                            }
                            Text("Invested: ${biz.currency} %,.0f".format(ps.invested), style = MaterialTheme.typography.bodySmall)
                            Text("Withdrawn: ${biz.currency} %,.0f".format(ps.withdrawn), style = MaterialTheme.typography.bodySmall)
                            Text("Cap: %.1f%% • Agreed: %.1f%%".format(ps.capitalSharePct, ps.partner.agreedOwnershipPct), style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }

        // Recent Transactions (Last 5)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Recent Transactions", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                TextButton(onClick = onOpenAllTransactions) { Text("View all") }
            }
        }

        val recent = state.entries.filter { !it.isDeleted }.take(5)
        if (recent.isEmpty()) {
            item {
                Card(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                    Text(
                        "No transactions yet. Tap the + button below to record your first investment or expense.",
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        } else {
            val pMap = state.partners.associateBy { it.id }
            val cMap = state.categories.associateBy { it.id }
            items(recent, key = { it.id }) { entry ->
                TransactionRowCard(biz, entry, pMap[entry.partnerId]?.name, cMap[entry.categoryId]?.name) {
                    onOpenEntry(entry)
                }
            }
        }
    }
}

@Composable
private fun HeroStat(label: String, value: String) {
    Column {
        Text(label, color = Color.White.copy(alpha = 0.75f), style = MaterialTheme.typography.labelSmall)
        Text(value, color = Color.White, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun QuickActionChip(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier, onClick: () -> Unit) {
    Card(
        shape = RoundedCornerShape(12.dp),
        modifier = modifier.clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, contentDescription = label, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            Spacer(Modifier.height(4.dp))
            Text(label, style = MaterialTheme.typography.labelSmall, maxLines = 1)
        }
    }
}

@Composable
fun AlertBannerCard(
    text: String,
    bgColor: Color,
    fgColor: Color,
    onClick: () -> Unit,
    onDismiss: (() -> Unit)?
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = bgColor),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(12.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Warning, contentDescription = null, tint = fgColor, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(text, color = fgColor, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
            if (onDismiss != null) {
                IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Dismiss", tint = fgColor, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

@Composable
fun TransactionRowCard(
    biz: BusinessConfig,
    entry: LedgerEntry,
    partnerName: String?,
    categoryName: String?,
    onClick: () -> Unit
) {
    val isIn = entry.type == EntryType.INVESTMENT || entry.type == EntryType.REVENUE
    val sign = if (isIn) "+" else "-"
    val amtColor = if (isIn) AppColors.MoneyIn else AppColors.MoneyOut
    val subtitle = listOfNotNull(entry.date, partnerName, categoryName, entry.vendorOrSource.ifBlank { null }).joinToString(" • ")

    Card(
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(14.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(entry.type.label, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                    if (entry.isDeleted) {
                        Text("DELETED", color = AppColors.MoneyOut, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    }
                    if ((entry.type == EntryType.INVESTMENT || entry.type == EntryType.EXPENSE) && !entry.hasProof) {
                        Text("Missing proof", color = AppColors.Warning, style = MaterialTheme.typography.labelSmall)
                    }
                }
                Text(subtitle, style = MaterialTheme.typography.bodySmall)
                if (entry.note.isNotBlank()) {
                    Text(entry.note, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                }
            }
            Text(
                "$sign${biz.currency} %,.2f".format(entry.amount),
                color = amtColor,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleSmall
            )
        }
    }
}

@Composable
fun TransactionsListScreen(
    biz: BusinessConfig,
    state: AppState,
    onOpenEntry: (LedgerEntry) -> Unit
) {
    var query by remember { mutableStateOf("") }
    var filterType by remember { mutableStateOf<EntryType?>(null) }
    var onlyMissingProof by remember { mutableStateOf(false) }
    var showDeleted by remember { mutableStateOf(false) }

    val pMap = remember(state.partners) { state.partners.associateBy { it.id } }
    val cMap = remember(state.categories) { state.categories.associateBy { it.id } }

    val filtered = state.entries.filter { e ->
        val delMatch = if (showDeleted) e.isDeleted else !e.isDeleted
        val typeMatch = filterType == null || e.type == filterType
        val proofMatch = !onlyMissingProof || ((e.type == EntryType.INVESTMENT || e.type == EntryType.EXPENSE) && !e.hasProof)
        val qMatch = query.isBlank() ||
            e.note.contains(query, ignoreCase = true) ||
            e.vendorOrSource.contains(query, ignoreCase = true) ||
            (pMap[e.partnerId]?.name?.contains(query, ignoreCase = true) == true) ||
            (cMap[e.categoryId]?.name?.contains(query, ignoreCase = true) == true)
        delMatch && typeMatch && proofMatch && qMatch
    }

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            placeholder = { Text("Search partner, vendor, note...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Spacer(Modifier.height(8.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            item { FilterChip(selected = filterType == null, onClick = { filterType = null }, label = { Text("All") }) }
            items(EntryType.entries) { t ->
                FilterChip(selected = filterType == t, onClick = { filterType = t }, label = { Text(t.label) })
            }
            item { FilterChip(selected = onlyMissingProof, onClick = { onlyMissingProof = !onlyMissingProof }, label = { Text("Missing Proof") }) }
            item { FilterChip(selected = showDeleted, onClick = { showDeleted = !showDeleted }, label = { Text("Deleted") }) }
        }
        Spacer(Modifier.height(8.dp))
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(bottom = 88.dp)
        ) {
            if (filtered.isEmpty()) {
                item {
                    Card(shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
                        Text("No matching transactions found.", modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.bodySmall)
                    }
                }
            } else {
                items(filtered, key = { it.id }) { entry ->
                    TransactionRowCard(biz, entry, pMap[entry.partnerId]?.name, cMap[entry.categoryId]?.name) {
                        onOpenEntry(entry)
                    }
                }
            }
        }
    }
}

@Composable
fun TransactionDetailScreen(
    biz: BusinessConfig,
    state: AppState,
    entry: LedgerEntry,
    onBack: () -> Unit,
    onEdit: (LedgerEntry) -> Unit,
    onSoftDelete: (LedgerEntry) -> Unit
) {
    var confirmDelete by remember { mutableStateOf(false) }
    val pMap = state.partners.associateBy { it.id }
    val cMap = state.categories.associateBy { it.id }
    val logsForRecord = state.auditLogs.filter { it.recordId == entry.id }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
            Text("Transaction Details", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }

        Card(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("${entry.type.label} • ${biz.currency} %,.2f".format(entry.amount), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text("Date: ${entry.date}")
                if (entry.partnerId != null) Text("Partner: ${pMap[entry.partnerId]?.name ?: "-"}")
                if (entry.categoryId != null) Text("Category: ${cMap[entry.categoryId]?.name ?: "-"}")
                if (entry.vendorOrSource.isNotBlank()) Text("Vendor / Source: ${entry.vendorOrSource}")
                Text("Payment Method: ${entry.paymentMethod}")
                if (entry.type == EntryType.EXPENSE) {
                    val payer = if (entry.paidByPartnerId == null) "Business Cash" else (pMap[entry.paidByPartnerId]?.name ?: "Partner")
                    Text("Paid By: $payer")
                }
                if (entry.type == EntryType.WITHDRAWAL) Text("Withdrawal Type: ${entry.withdrawalType.label}")
                Text("Receipt Attached: ${if (entry.hasProof) "Yes" else "No (Missing Proof)"}")
                if (entry.note.isNotBlank()) Text("Note: ${entry.note}")
            }
        }

        if (!entry.isDeleted) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(onClick = { onEdit(entry) }, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.Edit, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Edit")
                }
                OutlinedButton(
                    onClick = { confirmDelete = true },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = AppColors.MoneyOut)
                ) {
                    Icon(Icons.Default.DeleteOutline, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Delete")
                }
            }
        }

        Text("Edit History (${logsForRecord.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        logsForRecord.forEach { log ->
            val ts = SimpleDateFormat("dd MMM yyyy HH:mm", Locale.US).format(Date(log.timestamp))
            Card(shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("${log.action} • $ts", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                    if (log.oldValue != "-") Text("Old: ${log.oldValue}", style = MaterialTheme.typography.bodySmall)
                    Text("New: ${log.newValue}", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Soft-Delete Record?") },
            text = { Text("This hides the entry from active totals, keeps it under the 'Deleted' filter, and logs the deletion in the Audit Log.") },
            confirmButton = {
                Button(
                    onClick = {
                        confirmDelete = false
                        onSoftDelete(entry)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AppColors.MoneyOut)
                ) { Text("Soft Delete") }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } }
        )
    }
}

@Composable
fun AddEditTransactionDialog(
    biz: BusinessConfig,
    state: AppState,
    metrics: DashboardMetrics,
    initialType: EntryType,
    existing: LedgerEntry?,
    onDismiss: () -> Unit,
    onSave: (LedgerEntry, Boolean) -> Unit // Boolean = addAnother
) {
    var type by remember { mutableStateOf(existing?.type ?: initialType) }
    var amount by remember { mutableStateOf(existing?.amount?.toString() ?: "") }
    var date by remember { mutableStateOf(existing?.date ?: SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())) }
    var partnerId by remember { mutableStateOf(existing?.partnerId ?: state.settings.lastUsedPartnerId ?: state.partners.firstOrNull()?.id) }
    var categoryId by remember { mutableStateOf(existing?.categoryId ?: state.settings.lastUsedCategoryId ?: state.categories.firstOrNull()?.id) }
    var paidByPartnerId by remember { mutableStateOf<String?>(existing?.paidByPartnerId) }
    var vendorOrSource by remember { mutableStateOf(existing?.vendorOrSource ?: "") }
    var method by remember { mutableStateOf(existing?.paymentMethod ?: "Bank Transfer") }
    var withdrawalType by remember { mutableStateOf(existing?.withdrawalType ?: WithdrawalType.PROFIT) }
    var note by remember { mutableStateOf(existing?.note ?: "") }
    var hasProof by remember { mutableStateOf(existing?.hasProof ?: false) }

    val parsedAmount = amount.toDoubleOrNull() ?: 0.0
    val selectedPartnerSummary = metrics.partnerSummaries.firstOrNull { it.partner.id == partnerId }
    val isOverWithdrawal = type == EntryType.WITHDRAWAL &&
        selectedPartnerSummary != null &&
        parsedAmount > selectedPartnerSummary.netPosition

    fun buildEntry(): LedgerEntry = LedgerEntry(
        id = existing?.id ?: UUID.randomUUID().toString(),
        type = type,
        amount = parsedAmount,
        date = date.trim(),
        partnerId = if (type == EntryType.INVESTMENT || type == EntryType.WITHDRAWAL) partnerId else null,
        categoryId = if (type == EntryType.EXPENSE) categoryId else null,
        vendorOrSource = vendorOrSource.trim(),
        paymentMethod = method,
        paidByPartnerId = if (type == EntryType.EXPENSE) paidByPartnerId else null,
        withdrawalType = withdrawalType,
        note = note.trim(),
        hasProof = hasProof,
        createdAt = existing?.createdAt ?: System.currentTimeMillis(),
        updatedAt = System.currentTimeMillis()
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing == null) "Add ${type.label}" else "Edit ${type.label}") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (existing != null) {
                    Text(
                        "Audit notice: Every edit is recorded with old & new values.",
                        color = AppColors.Warning,
                        style = MaterialTheme.typography.labelSmall
                    )
                }

                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("Amount (${biz.currency}) *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = date,
                    onValueChange = { date = it },
                    label = { Text("Date (YYYY-MM-DD) *") },
                    modifier = Modifier.fillMaxWidth()
                )

                if (type == EntryType.INVESTMENT || type == EntryType.WITHDRAWAL) {
                    Text("Partner:", style = MaterialTheme.typography.labelMedium)
                    state.partners.forEach { p ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth().clickable { partnerId = p.id }
                        ) {
                            RadioButton(selected = partnerId == p.id, onClick = { partnerId = p.id })
                            Text(p.name)
                        }
                    }
                }

                if (type == EntryType.WITHDRAWAL) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        WithdrawalType.entries.forEach { wt ->
                            FilterChip(selected = withdrawalType == wt, onClick = { withdrawalType = wt }, label = { Text(wt.label) })
                        }
                    }
                    if (isOverWithdrawal) {
                        Card(colors = CardDefaults.cardColors(containerColor = AppColors.WarningBg)) {
                            Text(
                                "Warning: Amount exceeds ${selectedPartnerSummary?.partner?.name}'s net position (${biz.currency} %,.2f). You can still save.".format(selectedPartnerSummary?.netPosition ?: 0.0),
                                color = AppColors.Warning,
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                    }
                }

                if (type == EntryType.EXPENSE) {
                    Text("Category:", style = MaterialTheme.typography.labelMedium)
                    state.categories.forEach { c ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth().clickable { categoryId = c.id }
                        ) {
                            RadioButton(selected = categoryId == c.id, onClick = { categoryId = c.id })
                            Text(c.name)
                        }
                    }
                    OutlinedTextField(value = vendorOrSource, onValueChange = { vendorOrSource = it }, label = { Text("Vendor") }, modifier = Modifier.fillMaxWidth())

                    Text("Paid By:", style = MaterialTheme.typography.labelMedium)
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable { paidByPartnerId = null }) {
                        RadioButton(selected = paidByPartnerId == null, onClick = { paidByPartnerId = null })
                        Text("Business Cash")
                    }
                    state.partners.forEach { p ->
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable { paidByPartnerId = p.id }) {
                            RadioButton(selected = paidByPartnerId == p.id, onClick = { paidByPartnerId = p.id })
                            Text("${p.name} (Personal pocket)")
                        }
                    }
                    if (paidByPartnerId != null) {
                        Text(
                            "Helper: Partner-paid expenses automatically appear in Settle-Up.",
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }

                if (type == EntryType.REVENUE) {
                    OutlinedTextField(value = vendorOrSource, onValueChange = { vendorOrSource = it }, label = { Text("Revenue Source") }, modifier = Modifier.fillMaxWidth())
                }

                if (type == EntryType.INVESTMENT || type == EntryType.EXPENSE) {
                    val methods = listOf("Bank Transfer", "Cash", "Mobile Wallet", "Cheque", "Other")
                    Text("Payment Method:", style = MaterialTheme.typography.labelMedium)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(methods) { m ->
                            FilterChip(selected = method == m, onClick = { method = m }, label = { Text(m) })
                        }
                    }
                }

                OutlinedTextField(value = note, onValueChange = { note = it }, label = { Text("Note") }, modifier = Modifier.fillMaxWidth())

                if (type != EntryType.WITHDRAWAL) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = hasProof, onCheckedChange = { hasProof = it })
                        Text("Receipt / Proof Attached", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (existing == null) {
                    OutlinedButton(onClick = { if (parsedAmount > 0.0) onSave(buildEntry(), true) }) {
                        Text("Save & +1")
                    }
                }
                Button(onClick = { if (parsedAmount > 0.0) onSave(buildEntry(), false) }) {
                    Text("Save")
                }
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
