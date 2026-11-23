package com.mahibafaris.partnerledger.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.mahibafaris.partnerledger.data.LedgerRepository
import com.mahibafaris.partnerledger.export.ExportManager
import com.mahibafaris.partnerledger.model.*
import com.mahibafaris.partnerledger.ui.theme.AppColors
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs

@Composable
fun MoreHubScreen(
    biz: BusinessConfig,
    state: AppState,
    metrics: DashboardMetrics,
    repo: LedgerRepository,
    initialSubScreen: String = "MENU",
    onStateUpdated: (AppState) -> Unit
) {
    var current by remember(initialSubScreen) { mutableStateOf(initialSubScreen) }

    when (current) {
        "RECONCILE" -> ReconciliationScreen(biz, state, metrics, onStateUpdated) { current = "MENU" }
        "SETTLE_UP" -> SettleUpScreen(biz, state, metrics, onStateUpdated) { current = "MENU" }
        "COMMITMENTS" -> PendingContributionsScreen(biz, state, onStateUpdated) { current = "MENU" }
        "CASH_HEALTH" -> CashHealthScreen(biz, metrics) { current = "MENU" }
        "AUDIT" -> AuditLogScreen(state) { current = "MENU" }
        "BACKUP" -> BackupRestoreScreen(biz, state, repo, onStateUpdated) { current = "MENU" }
        "SETTINGS" -> SettingsScreen(biz, state, onStateUpdated) { current = "MENU" }
        "CHECKS" -> DataChecksScreen(biz, state) { current = "MENU" }
        else -> {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                item { Text("More Tools", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) }
                item { MoreMenuRow("Reconciliation", "Match app balance with real bank/cash", Icons.Default.AccountBalance) { current = "RECONCILE" } }
                item { MoreMenuRow("Settle-Up", "Reimburse partner-paid business expenses", Icons.Default.Handshake) { current = "SETTLE_UP" } }
                item { MoreMenuRow("Pending Contributions", "Track promised partner capital & due dates", Icons.Default.Schedule) { current = "COMMITMENTS" } }
                item { MoreMenuRow("Cash Health", "Burn rate, runway, gross margin & ROI", Icons.Default.TrendingUp) { current = "CASH_HEALTH" } }
                item { MoreMenuRow("Audit Log", "${state.auditLogs.size} chronological events (read-only)", Icons.Default.History) { current = "AUDIT" } }
                item { MoreMenuRow("Backup & Restore", "Encrypted Google Drive backup & restore", Icons.Default.CloudUpload) { current = "BACKUP" } }
                item { MoreMenuRow("Data Checks", "${metrics.missingProofCount} missing proofs • ${metrics.missingCategoryCount} uncategorized", Icons.Default.Flag) { current = "CHECKS" } }
                item { MoreMenuRow("Settings", "Theme, PIN lock, categories & optional rules", Icons.Default.Settings) { current = "SETTINGS" } }
            }
        }
    }
}

@Composable
private fun MoreMenuRow(title: String, sub: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Card(shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(14.dp))
            Column {
                Text(title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                Text(sub, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun SubScreenHeader(title: String, onBack: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
        Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun ReconciliationScreen(
    biz: BusinessConfig,
    state: AppState,
    metrics: DashboardMetrics,
    onStateUpdated: (AppState) -> Unit,
    onBack: () -> Unit
) {
    var actualText by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    val actualVal = actualText.toDoubleOrNull()

    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { SubScreenHeader("Bank / Cash Reconciliation", onBack) }
        item {
            Card(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("App Calculated Balance: ${biz.currency} %,.2f".format(metrics.currentBalance), fontWeight = FontWeight.Bold)
                    OutlinedTextField(
                        value = actualText,
                        onValueChange = { actualText = it },
                        label = { Text("Actual Bank + Cash Balance (${biz.currency})") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (actualVal != null) {
                        val diff = actualVal - metrics.currentBalance
                        val exact = abs(diff) < 0.01
                        Text(
                            if (exact) "Exact Match (0.00 difference)" else "Difference: ${biz.currency} %+,.2f".format(diff),
                            color = if (exact) AppColors.MoneyIn else AppColors.Warning,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    OutlinedTextField(value = note, onValueChange = { note = it }, label = { Text("Note (Optional)") }, modifier = Modifier.fillMaxWidth())
                    Button(
                        onClick = {
                            if (actualVal != null) {
                                val diff = actualVal - metrics.currentBalance
                                val rec = ReconciliationRecord(
                                    id = UUID.randomUUID().toString(),
                                    date = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()),
                                    actualBalance = actualVal,
                                    calculatedBalance = metrics.currentBalance,
                                    difference = diff,
                                    note = note.trim()
                                )
                                onStateUpdated(state.copy(reconciliations = listOf(rec) + state.reconciliations))
                                actualText = ""; note = ""
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Save Reconciliation") }
                }
            }
        }
        item { Text("Past Reconciliations", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
        items(state.reconciliations) { r ->
            Card(shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("${r.date} • Diff: ${biz.currency} %+,.2f".format(r.difference), fontWeight = FontWeight.Bold)
                    Text("Actual: %,.2f | App: %,.2f".format(r.actualBalance, r.calculatedBalance), style = MaterialTheme.typography.bodySmall)
                    if (r.note.isNotBlank()) Text(r.note, style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

@Composable
private fun SettleUpScreen(
    biz: BusinessConfig,
    state: AppState,
    metrics: DashboardMetrics,
    onStateUpdated: (AppState) -> Unit,
    onBack: () -> Unit
) {
    val owedList = metrics.partnerSummaries.filter { it.owedByBusiness > 0.0 }
    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { SubScreenHeader("Settle-Up", onBack) }
        if (owedList.isEmpty()) {
            item {
                Card(shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
                    Text("All settled! No unpaid partner-funded expenses.", modifier = Modifier.padding(16.dp))
                }
            }
        } else {
            items(owedList) { ps ->
                Card(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Business owes ${ps.partner.name}", fontWeight = FontWeight.Bold)
                        Text("${biz.currency} %,.2f".format(ps.owedByBusiness), style = MaterialTheme.typography.headlineSmall, color = AppColors.Warning, fontWeight = FontWeight.Bold)
                        Button(onClick = {
                            val updatedEntries = state.entries.map {
                                if (it.type == EntryType.EXPENSE && it.paidByPartnerId == ps.partner.id && !it.isSettled) {
                                    it.copy(isSettled = true, updatedAt = System.currentTimeMillis())
                                } else it
                            }
                            val log = AuditLogItem(
                                id = UUID.randomUUID().toString(),
                                recordType = "SettleUp",
                                recordId = ps.partner.id,
                                action = "SETTLE",
                                oldValue = "Owed ${biz.currency} ${ps.owedByBusiness}",
                                newValue = "Marked settled for ${ps.partner.name}"
                            )
                            onStateUpdated(state.copy(entries = updatedEntries, auditLogs = listOf(log) + state.auditLogs))
                        }) {
                            Text("Mark as Settled")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PendingContributionsScreen(
    biz: BusinessConfig,
    state: AppState,
    onStateUpdated: (AppState) -> Unit,
    onBack: () -> Unit
) {
    var partnerId by remember { mutableStateOf(state.partners.firstOrNull()?.id ?: "") }
    var amount by remember { mutableStateOf("") }
    var dueDate by remember { mutableStateOf(SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())) }
    val pMap = state.partners.associateBy { it.id }

    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { SubScreenHeader("Pending Contributions", onBack) }
        item {
            Card(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Add Commitment", fontWeight = FontWeight.Bold)
                    state.partners.forEach { p ->
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { partnerId = p.id }) {
                            RadioButton(selected = partnerId == p.id, onClick = { partnerId = p.id })
                            Text(p.name)
                        }
                    }
                    OutlinedTextField(value = amount, onValueChange = { amount = it }, label = { Text("Promised Amount (${biz.currency})") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = dueDate, onValueChange = { dueDate = it }, label = { Text("Due Date (YYYY-MM-DD)") }, modifier = Modifier.fillMaxWidth())
                    Button(
                        onClick = {
                            val amt = amount.toDoubleOrNull() ?: 0.0
                            if (partnerId.isNotBlank() && amt > 0.0) {
                                val c = Commitment(UUID.randomUUID().toString(), partnerId, amt, 0.0, dueDate.trim())
                                onStateUpdated(state.copy(commitments = listOf(c) + state.commitments))
                                amount = ""
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Save Commitment") }
                }
            }
        }
        items(state.commitments) { c ->
            val pct = if (c.promisedAmount > 0.0) (c.paidAmount / c.promisedAmount).toFloat().coerceIn(0f, 1f) else 0f
            val status = when {
                c.paidAmount >= c.promisedAmount -> "Paid"
                c.paidAmount > 0.0 -> "Partly Paid"
                else -> "Promised"
            }
            Card(shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(pMap[c.partnerId]?.name ?: "Partner", fontWeight = FontWeight.Bold)
                        Text(status, color = if (status == "Paid") AppColors.MoneyIn else AppColors.Warning, fontWeight = FontWeight.Bold)
                    }
                    Text("${biz.currency} %,.0f / %,.0f • Due ${c.dueDate}".format(c.paidAmount, c.promisedAmount), style = MaterialTheme.typography.bodySmall)
                    LinearProgressIndicator(progress = { pct }, modifier = Modifier.fillMaxWidth())
                    if (status != "Paid") {
                        OutlinedButton(onClick = {
                            val remaining = (c.promisedAmount - c.paidAmount).coerceAtLeast(0.0)
                            val inv = LedgerEntry(
                                id = UUID.randomUUID().toString(),
                                type = EntryType.INVESTMENT,
                                amount = remaining,
                                date = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()),
                                partnerId = c.partnerId,
                                note = "Fulfilled commitment (due ${c.dueDate})"
                            )
                            val updatedCommitments = state.commitments.map {
                                if (it.id == c.id) it.copy(paidAmount = it.promisedAmount) else it
                            }
                            onStateUpdated(state.copy(commitments = updatedCommitments, entries = listOf(inv) + state.entries))
                        }) { Text("Mark Paid & Record Investment") }
                    }
                }
            }
        }
    }
}

@Composable
private fun CashHealthScreen(biz: BusinessConfig, metrics: DashboardMetrics, onBack: () -> Unit) {
    val ch = metrics.cashHealth
    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { SubScreenHeader("Cash Health", onBack) }
        item {
            HealthMetricCard(
                "Net Burn Rate",
                if (ch.monthlyNetBurn <= 0.0) "Not burning cash" else "${biz.currency} %,.2f / mo".format(ch.monthlyNetBurn),
                "Monthly expenses minus monthly revenue. Partner investments & capital returns are excluded."
            )
        }
        item {
            HealthMetricCard(
                "Runway",
                ch.runwayMonths?.let { "%.1f months".format(it) } ?: "Not burning cash",
                "Current cash balance divided by net monthly burn."
            )
        }
        item {
            HealthMetricCard(
                "Gross Margin",
                ch.grossMarginPct?.let { "%.1f%%".format(it) } ?: "Need revenue entries",
                "(Revenue minus Direct Costs) divided by Revenue."
            )
        }
        item {
            HealthMetricCard(
                "Operating Expenses",
                "${biz.currency} %,.2f".format(ch.operatingExpenses),
                "Total expenses flagged as Operating (non-direct)."
            )
        }
        item {
            HealthMetricCard(
                "Business ROI",
                ch.businessRoiPct?.let { "%.1f%%".format(it) } ?: "Need capital entries",
                "Net profit divided by total capital invested."
            )
        }
    }
}

@Composable
private fun HealthMetricCard(title: String, value: String, formula: String) {
    Card(shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, style = MaterialTheme.typography.labelLarge)
            Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Text(formula, style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun AuditLogScreen(state: AppState, onBack: () -> Unit) {
    val context = LocalContext.current
    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                SubScreenHeader("Audit Log", onBack)
                TextButton(onClick = { ExportManager.exportLedgerCsv(context, state) }) { Text("Export") }
            }
        }
        items(state.auditLogs) { log ->
            val ts = SimpleDateFormat("dd MMM yyyy HH:mm", Locale.US).format(Date(log.timestamp))
            Card(shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("${log.action} • ${log.recordType} • $ts", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                    if (log.oldValue != "-") Text("Old: ${log.oldValue}", style = MaterialTheme.typography.bodySmall)
                    Text("New: ${log.newValue}", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
private fun BackupRestoreScreen(
    biz: BusinessConfig,
    state: AppState,
    repo: LedgerRepository,
    onStateUpdated: (AppState) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var password by remember { mutableStateOf("") }
    var pendingRaw by remember { mutableStateOf<String?>(null) }
    var pendingParsed by remember { mutableStateOf<AppState?>(null) }
    var restorePwd by remember { mutableStateOf("") }
    var restoreErr by remember { mutableStateOf<String?>(null) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) {
            val raw = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() } ?: ""
            pendingRaw = raw
            restorePwd = ""
            restoreErr = null
            val res = ExportManager.decodeBackupPayload(raw, "")
            if (res.isSuccess) {
                pendingParsed = repo.parseBackupJson(res.getOrThrow())
                if (pendingParsed == null) restoreErr = "Invalid backup file."
            } else {
                restoreErr = "Password-protected backup. Enter password below."
            }
        }
    }

    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { SubScreenHeader("Backup & Restore", onBack) }
        item {
            val lastStr = if (biz.lastBackupTimestamp == 0L) "Never" else SimpleDateFormat("dd MMM yyyy HH:mm", Locale.US).format(Date(biz.lastBackupTimestamp))
            Card(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Last Backup: $lastStr • Size: ~${repo.getDatabaseSizeKb()} KB", style = MaterialTheme.typography.bodySmall)
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Optional Backup Password (AES)") },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Button(
                        onClick = {
                            val updatedBiz = biz.copy(lastBackupTimestamp = System.currentTimeMillis())
                            val updatedState = state.copy(business = updatedBiz)
                            onStateUpdated(updatedState)
                            ExportManager.exportBackupFile(context, updatedBiz, repo.exportRawJson(updatedState), password)
                        },
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) { Text("Backup Now (Save to Google Drive)") }
                    OutlinedButton(
                        onClick = { picker.launch(arrayOf("*/*")) },
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) { Text("Restore from Backup File") }
                    Text("Tip: Keep several recent dated backups in Google Drive instead of overwriting one file.", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }

    if (pendingRaw != null) {
        AlertDialog(
            onDismissRequest = { pendingRaw = null; pendingParsed = null },
            title = { Text("Restore Backup") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (pendingParsed == null) {
                        Text(restoreErr ?: "Enter password:")
                        OutlinedTextField(value = restorePwd, onValueChange = { restorePwd = it }, label = { Text("Password") }, visualTransformation = PasswordVisualTransformation())
                        Button(onClick = {
                            val res = ExportManager.decodeBackupPayload(pendingRaw!!, restorePwd)
                            if (res.isSuccess) {
                                pendingParsed = repo.parseBackupJson(res.getOrThrow())
                                restoreErr = if (pendingParsed == null) "Corrupted file." else null
                            } else {
                                restoreErr = "Wrong password."
                            }
                        }) { Text("Decrypt") }
                    } else {
                        val t = pendingParsed!!
                        Text("Business: ${t.business?.businessName}")
                        Text("Partners: ${t.partners.size} • Transactions: ${t.entries.size}")
                        Text("Warning: Restoring replaces all current data on this phone.", color = AppColors.MoneyOut, fontWeight = FontWeight.Bold)
                    }
                }
            },
            confirmButton = {
                if (pendingParsed != null) {
                    Button(onClick = {
                        onStateUpdated(pendingParsed!!)
                        pendingRaw = null; pendingParsed = null
                        Toast.makeText(context, "Backup restored", Toast.LENGTH_SHORT).show()
                    }) { Text("Overwrite & Restore") }
                }
            },
            dismissButton = { TextButton(onClick = { pendingRaw = null; pendingParsed = null }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun SettingsScreen(
    biz: BusinessConfig,
    state: AppState,
    onStateUpdated: (AppState) -> Unit,
    onBack: () -> Unit
) {
    var pinInput by remember { mutableStateOf(biz.pin) }
    var rateInput by remember { mutableStateOf(state.settings.interestRatePct.toString()) }

    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { SubScreenHeader("Settings", onBack) }
        item {
            Card(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Appearance", fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ThemeMode.entries.forEach { mode ->
                            FilterChip(
                                selected = state.settings.themeMode == mode,
                                onClick = { onStateUpdated(state.copy(settings = state.settings.copy(themeMode = mode))) },
                                label = { Text(mode.label) }
                            )
                        }
                    }
                }
            }
        }
        item {
            Card(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("App Lock PIN", fontWeight = FontWeight.Bold)
                    OutlinedTextField(
                        value = pinInput,
                        onValueChange = { if (it.length <= 6) pinInput = it },
                        label = { Text("4–6 Digit PIN (leave empty to disable)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Button(onClick = { onStateUpdated(state.copy(business = biz.copy(pin = pinInput.trim()))) }) {
                        Text("Update PIN")
                    }
                }
            }
        }
        item {
            Card(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Optional Fairness Rules (Off by Default)", fontWeight = FontWeight.Bold)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Interest on Capital")
                        Switch(
                            checked = state.settings.interestOnCapitalEnabled,
                            onCheckedChange = { onStateUpdated(state.copy(settings = state.settings.copy(interestOnCapitalEnabled = it))) }
                        )
                    }
                    if (state.settings.interestOnCapitalEnabled) {
                        OutlinedTextField(
                            value = rateInput,
                            onValueChange = {
                                rateInput = it
                                it.toDoubleOrNull()?.let { r ->
                                    onStateUpdated(state.copy(settings = state.settings.copy(interestRatePct = r)))
                                }
                            },
                            label = { Text("Interest Rate %") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                        )
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Salary / Effort Credit")
                        Switch(
                            checked = state.settings.salaryCreditEnabled,
                            onCheckedChange = { onStateUpdated(state.copy(settings = state.settings.copy(salaryCreditEnabled = it))) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DataChecksScreen(biz: BusinessConfig, state: AppState, onBack: () -> Unit) {
    val active = state.entries.filter { !it.isDeleted }
    val missingProof = active.filter { (it.type == EntryType.INVESTMENT || it.type == EntryType.EXPENSE) && !it.hasProof }
    val missingCat = active.filter { it.type == EntryType.EXPENSE && it.categoryId.isNullOrBlank() }

    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item { SubScreenHeader("Data Checks", onBack) }
        item { Text("Missing Receipt Proof (${missingProof.size})", fontWeight = FontWeight.Bold) }
        items(missingProof) { e ->
            Card(shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                Text("${e.date} • ${e.type.label} • ${biz.currency} %,.2f".format(e.amount), modifier = Modifier.padding(12.dp), style = MaterialTheme.typography.bodySmall)
            }
        }
        item { Text("Uncategorized Expenses (${missingCat.size})", fontWeight = FontWeight.Bold) }
        items(missingCat) { e ->
            Card(shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                Text("${e.date} • ${biz.currency} %,.2f • ${e.vendorOrSource}".format(e.amount), modifier = Modifier.padding(12.dp), style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
