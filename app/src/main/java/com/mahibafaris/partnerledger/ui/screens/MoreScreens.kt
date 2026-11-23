package com.mahibafaris.partnerledger.ui.screens

import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mahibafaris.partnerledger.data.LedgerCalculator
import com.mahibafaris.partnerledger.data.LedgerRepository
import com.mahibafaris.partnerledger.export.ExportManager
import com.mahibafaris.partnerledger.model.*
import com.mahibafaris.partnerledger.ui.theme.BrandAccent
import com.mahibafaris.partnerledger.ui.theme.BrandSecondary
import com.mahibafaris.partnerledger.ui.theme.DatePickerField
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun MoreHubScreen(
    state: AppState,
    repo: LedgerRepository,
    onSettlePartner: (PartnerSummary) -> Unit,
    onAddPending: (PendingContribution) -> Unit,
    onCompletePending: (PendingContribution) -> Unit,
    onRestoreState: (AppState) -> Unit,
    onUpdateSecurity: (String, Boolean) -> Unit
) {
    var subScreen by remember { mutableStateOf("MENU") }
    val context = LocalContext.current

    when (subScreen) {
        "MENU" -> {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item { Text("More", fontSize = 22.sp, fontWeight = FontWeight.Bold) }
                item {
                    MoreMenuItem("Settle-Up (Reimbursements)", "Pay back partners who covered bills temporarily") {
                        subScreen = "SETTLE"
                    }
                }
                item {
                    MoreMenuItem("Pending Contributions", "Track promised partner investments & due dates") {
                        subScreen = "PENDING"
                    }
                }
                item {
                    MoreMenuItem("Backup & Restore", "Save encrypted .ptb backup (with receipt photos) or restore") {
                        subScreen = "BACKUP"
                    }
                }
                item {
                    MoreMenuItem("Export Accountant CSV", "Export full ledger + audit log spreadsheet") {
                        ExportManager.exportLedgerCsv(context, state)
                    }
                }
                item {
                    MoreMenuItem("Audit Log", "View history of all additions, edits, and deletions") {
                        subScreen = "AUDIT"
                    }
                }
                item {
                    MoreMenuItem("PIN & Fingerprint Lock", "Set PIN and toggle biometric unlock") {
                        subScreen = "PIN"
                    }
                }
            }
        }

        "SETTLE" -> SettleUpScreen(state, onSettlePartner) { subScreen = "MENU" }
        "PENDING" -> PendingContributionsScreen(state, onAddPending, onCompletePending) { subScreen = "MENU" }
        "BACKUP" -> BackupRestoreScreen(state, repo, onRestoreState) { subScreen = "MENU" }
        "AUDIT" -> AuditLogScreen(state) { subScreen = "MENU" }
        "PIN" -> PinSettingsScreen(
            currentPin = state.business?.pin ?: "",
            currentBio = state.business?.biometricEnabled ?: true,
            onUpdateSecurity = onUpdateSecurity,
            onBack = { subScreen = "MENU" }
        )
    }
}

@Composable
private fun MoreMenuItem(title: String, subtitle: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Spacer(Modifier.height(2.dp))
            Text(subtitle, fontSize = 12.sp, color = Color.Gray)
        }
    }
}

@Composable
private fun SubHeader(title: String, onBack: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back") }
        Text(title, fontSize = 20.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun SettleUpScreen(
    state: AppState,
    onSettlePartner: (PartnerSummary) -> Unit,
    onBack: () -> Unit
) {
    val biz = state.business ?: return
    val owedList = remember(state) {
        LedgerCalculator.compute(state).partnerSummaries.filter { it.owedByBusiness > 0.0 }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        SubHeader("Settle-Up", onBack)
        Spacer(Modifier.height(10.dp))
        if (owedList.isEmpty()) {
            Text("All caught up! Nobody is owed a reimbursement right now.", color = Color.Gray)
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(owedList) { ps ->
                    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(ps.partner.name, fontWeight = FontWeight.Bold)
                                Text(
                                    "Owed: ${formatMoney(biz.currency, ps.owedByBusiness)}",
                                    color = BrandAccent,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Button(onClick = { onSettlePartner(ps) }) { Text("Mark Paid") }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PendingContributionsScreen(
    state: AppState,
    onAddPending: (PendingContribution) -> Unit,
    onCompletePending: (PendingContribution) -> Unit,
    onBack: () -> Unit
) {
    val biz = state.business ?: return
    val pMap = remember(state.partners) { state.partners.associateBy { it.id } }
    var showAdd by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        SubHeader("Pending Contributions", onBack)
        Button(onClick = { showAdd = true }, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(Modifier.width(6.dp))
            Text("Add Pending Contribution")
        }
        Spacer(Modifier.height(12.dp))

        val open = state.pendingContributions.filter { !it.isCompleted }
        if (open.isEmpty()) {
            Text("No pending contributions.", color = Color.Gray)
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(open) { pc ->
                    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(pMap[pc.partnerId]?.name ?: "Partner", fontWeight = FontWeight.Bold)
                                Text(formatMoney(biz.currency, pc.amount), color = BrandSecondary, fontWeight = FontWeight.SemiBold)
                                Text("Due: ${pc.dueDate}", fontSize = 12.sp, color = Color.Gray)
                                if (pc.note.isNotBlank()) Text(pc.note, fontSize = 12.sp, color = Color.Gray)
                            }
                            FilledTonalButton(onClick = { onCompletePending(pc) }) {
                                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Received")
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAdd) {
        var partnerId by remember { mutableStateOf(state.partners.firstOrNull()?.id ?: "") }
        var amount by remember { mutableStateOf("") }
        var dueDate by remember { mutableStateOf(todayDateString()) }
        var note by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAdd = false },
            title = { Text("New Pending Contribution") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(state.partners) { p ->
                            FilterChip(
                                selected = partnerId == p.id,
                                onClick = { partnerId = p.id },
                                label = { Text(p.name) }
                            )
                        }
                    }
                    OutlinedTextField(
                        value = amount,
                        onValueChange = { amount = it },
                        label = { Text("Amount (${biz.currency})") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    DatePickerField(label = "Due Date", value = dueDate, onValueChange = { dueDate = it })
                    OutlinedTextField(
                        value = note,
                        onValueChange = { note = it },
                        label = { Text("Note (Optional)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    val amt = amount.toDoubleOrNull() ?: 0.0
                    if (partnerId.isNotBlank() && amt > 0.0) {
                        onAddPending(
                            PendingContribution(
                                partnerId = partnerId,
                                amount = amt,
                                dueDate = dueDate.ifBlank { todayDateString() },
                                note = note.trim()
                            )
                        )
                        showAdd = false
                    }
                }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { showAdd = false }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun BackupRestoreScreen(
    state: AppState,
    repo: LedgerRepository,
    onRestoreState: (AppState) -> Unit,
    onBack: () -> Unit
) {
    val biz = state.business ?: return
    val context = LocalContext.current
    var backupPassword by remember { mutableStateOf("") }
    var rawBackupText by remember { mutableStateOf<String?>(null) }
    var needsPassword by remember { mutableStateOf(false) }
    var restorePassword by remember { mutableStateOf("") }
    var statusMsg by remember { mutableStateOf<String?>(null) }

    fun tryRestore(raw: String, pwd: String) {
        ExportManager.decodeBackupPayload(raw, pwd).onSuccess { json ->
            try {
                val restored = repo.fromJson(json)
                needsPassword = false
                statusMsg = "Backup restored successfully!"
                onRestoreState(restored)
            } catch (e: Exception) {
                statusMsg = "Invalid backup file."
            }
        }.onFailure { err ->
            if (err.message == "PASSWORD_REQUIRED") {
                needsPassword = true
            } else {
                needsPassword = true
                statusMsg = "Wrong password."
            }
        }
    }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) {
            val raw = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() } ?: ""
            rawBackupText = raw
            restorePassword = ""
            tryRestore(raw, "")
        }
    }

    val receiptCount = state.entries.count { !it.isDeleted && it.receiptImageBase64.isNotBlank() }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SubHeader("Backup & Restore", onBack)

        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Save Backup (.ptb)", fontWeight = FontWeight.Bold)
                Text("Includes all partners, entries, $receiptCount receipt photos, statements, and audit logs.", fontSize = 12.sp, color = Color.Gray)
                OutlinedTextField(
                    value = backupPassword,
                    onValueChange = { backupPassword = it },
                    label = { Text("Encryption Password (Optional)") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Button(
                    onClick = { ExportManager.exportBackupFile(context, biz, repo.toJson(state), backupPassword) },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Save Backup to Drive / Share") }
            }
        }

        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Restore Backup (.ptb)", fontWeight = FontWeight.Bold)
                OutlinedButton(
                    onClick = { picker.launch(arrayOf("*/*")) },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Pick Backup File to Restore") }
            }
        }

        if (statusMsg != null) {
            Text(statusMsg!!, color = MaterialTheme.colorScheme.primary)
        }
    }

    if (needsPassword && rawBackupText != null) {
        AlertDialog(
            onDismissRequest = { needsPassword = false },
            title = { Text("Enter Backup Password") },
            text = {
                OutlinedTextField(
                    value = restorePassword,
                    onValueChange = { restorePassword = it },
                    label = { Text("Password") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(onClick = { tryRestore(rawBackupText!!, restorePassword) }) { Text("Restore") }
            },
            dismissButton = { TextButton(onClick = { needsPassword = false }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun AuditLogScreen(state: AppState, onBack: () -> Unit) {
    val fmt = remember { SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.US) }
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        SubHeader("Audit Log", onBack)
        Spacer(Modifier.height(10.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(state.auditLogs.sortedByDescending { it.timestamp }) { log ->
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp)) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("${log.action} • ${log.recordType}", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        if (log.oldValue.isNotBlank()) Text("Old: ${log.oldValue}", fontSize = 12.sp, color = Color.Gray)
                        if (log.newValue.isNotBlank()) Text("New: ${log.newValue}", fontSize = 12.sp)
                        Text(fmt.format(Date(log.timestamp)), fontSize = 11.sp, color = Color.Gray)
                    }
                }
            }
        }
    }
}

@Composable
private fun PinSettingsScreen(
    currentPin: String,
    currentBio: Boolean,
    onUpdateSecurity: (String, Boolean) -> Unit,
    onBack: () -> Unit
) {
    var newPin by remember { mutableStateOf(currentPin) }
    var bioEnabled by remember { mutableStateOf(currentBio) }
    var saved by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SubHeader("PIN & Fingerprint Lock", onBack)
        OutlinedTextField(
            value = newPin,
            onValueChange = {
                if (it.length <= 6) newPin = it.filter { ch -> ch.isDigit() }
                saved = false
            },
            label = { Text("PIN (leave blank to disable lock)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            visualTransformation = PasswordVisualTransformation(),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        if (newPin.isNotBlank() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Fingerprint / Biometric Unlock")
                Switch(checked = bioEnabled, onCheckedChange = { bioEnabled = it; saved = false })
            }
        }
        Button(
            onClick = {
                onUpdateSecurity(newPin, bioEnabled)
                saved = true
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Save Security Settings") }
        if (saved) {
            Text(
                if (newPin.isBlank()) "App lock disabled." else "Security settings saved.",
                color = BrandSecondary
            )
        }
    }
}
