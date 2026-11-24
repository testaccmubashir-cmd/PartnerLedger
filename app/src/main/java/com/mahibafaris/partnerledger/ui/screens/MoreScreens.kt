package com.mahibafaris.partnerledger.ui.screens

import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.mahibafaris.partnerledger.data.LedgerCalculator
import com.mahibafaris.partnerledger.data.LedgerRepository
import com.mahibafaris.partnerledger.export.ExportManager
import com.mahibafaris.partnerledger.model.*
import com.mahibafaris.partnerledger.ui.theme.*
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

    AnimatedContent(
        targetState = subScreen,
        transitionSpec = {
            if (targetState != "MENU") {
                (fadeIn(tween(220)) + slideInHorizontally { it / 5 }) togetherWith
                    (fadeOut(tween(160)) + slideOutHorizontally { -it / 5 })
            } else {
                (fadeIn(tween(220)) + slideInHorizontally { -it / 5 }) togetherWith
                    (fadeOut(tween(160)) + slideOutHorizontally { it / 5 })
            }
        },
        label = "moreSubNav"
    ) { screen ->
        when (screen) {
            "MENU" -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(JetBlack)
                        .padding(horizontal = 16.dp),
                    contentPadding = PaddingValues(top = 14.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item {
                        Text("More", fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                        Text("Tools, backups & security controls", fontSize = 12.sp, color = LavenderMuted)
                        Spacer(Modifier.height(4.dp))
                    }
                    item {
                        MoreMenuItem("⇄", "Settle-Up (Reimbursements)", "Pay back partners who covered bills temporarily") {
                            subScreen = "SETTLE"
                        }
                    }
                    item {
                        MoreMenuItem("◷", "Pending Contributions", "Track promised partner investments & due dates") {
                            subScreen = "PENDING"
                        }
                    }
                    item {
                        MoreMenuItem("☁", "Backup & Restore", "Save encrypted .ptb backup (with receipt photos) or restore") {
                            subScreen = "BACKUP"
                        }
                    }
                    item {
                        MoreMenuItem("▤", "Export Accountant CSV", "Export full ledger + audit log spreadsheet") {
                            ExportManager.exportLedgerCsv(context, state)
                        }
                    }
                    item {
                        MoreMenuItem("☰", "Audit Log", "View history of all additions, edits, and deletions") {
                            subScreen = "AUDIT"
                        }
                    }
                    item {
                        MoreMenuItem("🔒", "PIN & Fingerprint Lock", "Set PIN and toggle biometric unlock") {
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
}

@Composable
private fun MoreMenuItem(glyph: String, title: String, subtitle: String, onClick: () -> Unit) {
    FintechCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 18.dp, onClick = onClick) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(AmethystElevated)
                    .border(1.dp, ElevatedBorderColor, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(glyph, fontSize = 16.sp, color = CyanElectric, fontWeight = FontWeight.Bold)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.White)
                Spacer(Modifier.height(2.dp))
                Text(subtitle, fontSize = 11.sp, color = LavenderMuted)
            }
            Icon(Icons.Default.KeyboardArrowRight, contentDescription = null, tint = LavenderMuted)
        }
    }
}

@Composable
private fun SubHeader(title: String, onBack: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(AmethystElevated)
                .border(1.dp, ElevatedBorderColor, RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
        }
        Text(title, fontSize = 19.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
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

    Column(modifier = Modifier.fillMaxSize().background(JetBlack).padding(horizontal = 16.dp)) {
        SubHeader("Settle-Up", onBack)
        if (owedList.isEmpty()) {
            FintechCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 22.dp) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("All Settled Up", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = EmeraldSoft)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "No pending partner reimbursements right now.",
                        color = LavenderMuted,
                        fontSize = 12.sp
                    )
                }
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(owedList) { ps ->
                    FintechCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 18.dp) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(ps.partner.name, fontWeight = FontWeight.Bold, color = Color.White)
                                Text(
                                    "Owed: ${formatMoney(biz.currency, ps.owedByBusiness)}",
                                    color = AmberSoft,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp
                                )
                            }
                            Button(
                                onClick = { onSettlePartner(ps) },
                                colors = ButtonDefaults.buttonColors(containerColor = CyanElectric, contentColor = JetBlack),
                                shape = RoundedCornerShape(12.dp)
                            ) { Text("Mark Paid", fontWeight = FontWeight.Bold) }
                        }
                    }
                }
            }
        }
    }
}

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

    Column(modifier = Modifier.fillMaxSize().background(JetBlack).padding(horizontal = 16.dp)) {
        SubHeader("Pending Contributions", onBack)
        Button(
            onClick = { showAdd = true },
            colors = ButtonDefaults.buttonColors(containerColor = CyanElectric, contentColor = JetBlack),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth().height(48.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(Modifier.width(6.dp))
            Text("Add Pending Contribution", fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(14.dp))

        val open = state.pendingContributions.filter { !it.isCompleted }
        if (open.isEmpty()) {
            FintechCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 22.dp) {
                Box(modifier = Modifier.fillMaxWidth().padding(28.dp), contentAlignment = Alignment.Center) {
                    Text("No pending contributions.", color = LavenderMuted, fontSize = 13.sp)
                }
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(open, key = { it.id }) { pc ->
                    FintechCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 18.dp) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(pMap[pc.partnerId]?.name ?: "Partner", fontWeight = FontWeight.Bold, color = Color.White)
                                Text(formatMoney(biz.currency, pc.amount), color = CyanElectric, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text("Due: ${pc.dueDate}", fontSize = 11.sp, color = LavenderMuted)
                                if (pc.note.isNotBlank()) Text(pc.note, fontSize = 11.sp, color = LavenderMuted)
                            }
                            Button(
                                onClick = { onCompletePending(pc) },
                                colors = ButtonDefaults.buttonColors(containerColor = EmeraldSoft, contentColor = JetBlack),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Received", fontWeight = FontWeight.Bold, fontSize = 12.sp)
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

        Dialog(
            onDismissRequest = { showAdd = false },
            properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.72f))
                    .imePadding()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    color = AmethystElevated,
                    border = BorderStroke(1.dp, ElevatedBorderColor)
                ) {
                    Column(
                        modifier = Modifier
                            .verticalScroll(rememberScrollState())
                            .padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("New Pending Contribution", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(state.partners) { p ->
                                FintechChip(label = p.name, selected = partnerId == p.id, onClick = { partnerId = p.id })
                            }
                        }
                        OutlinedTextField(
                            value = amount,
                            onValueChange = { amount = it },
                            label = { Text("Amount (${biz.currency})") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp),
                            colors = darkFieldColors(),
                            modifier = Modifier.fillMaxWidth()
                        )
                        DatePickerField(label = "Due Date", value = dueDate, onValueChange = { dueDate = it })
                        OutlinedTextField(
                            value = note,
                            onValueChange = { note = it },
                            label = { Text("Note (Optional)") },
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp),
                            colors = darkFieldColors(),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            TextButton(onClick = { showAdd = false }) { Text("Cancel", color = LavenderMuted) }
                            Spacer(Modifier.width(8.dp))
                            Button(
                                onClick = {
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
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = CyanElectric, contentColor = JetBlack),
                                shape = RoundedCornerShape(14.dp)
                            ) { Text("Save", fontWeight = FontWeight.Bold) }
                        }
                    }
                }
            }
        }
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
            needsPassword = true
            if (err.message != "PASSWORD_REQUIRED") statusMsg = "Wrong password."
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(JetBlack)
            .verticalScroll(rememberScrollState())
            .imePadding()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        SubHeader("Backup & Restore", onBack)

        FintechCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 22.dp) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Save Backup (.ptb)", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 15.sp)
                Text(
                    "Includes all partners, entries, $receiptCount receipt photos, statements, and audit logs.",
                    fontSize = 12.sp,
                    color = LavenderMuted
                )
                OutlinedTextField(
                    value = backupPassword,
                    onValueChange = { backupPassword = it },
                    label = { Text("Encryption Password (Optional)") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    colors = darkFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )
                Button(
                    onClick = { ExportManager.exportBackupFile(context, biz, repo.toJson(state), backupPassword) },
                    colors = ButtonDefaults.buttonColors(containerColor = CyanElectric, contentColor = JetBlack),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) { Text("Save Backup to Drive / Share", fontWeight = FontWeight.Bold) }
            }
        }

        FintechCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 22.dp) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Restore Backup (.ptb)", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 15.sp)
                OutlinedButton(
                    onClick = { picker.launch(arrayOf("*/*")) },
                    border = BorderStroke(1.dp, CyanElectric.copy(alpha = 0.65f)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = CyanElectric),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) { Text("Pick Backup File to Restore", fontWeight = FontWeight.Bold) }
            }
        }

        if (statusMsg != null) {
            Text(statusMsg!!, color = CyanElectric, fontSize = 13.sp)
        }
    }

    if (needsPassword && rawBackupText != null) {
        AlertDialog(
            onDismissRequest = { needsPassword = false },
            containerColor = AmethystElevated,
            title = { Text("Enter Backup Password", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = restorePassword,
                    onValueChange = { restorePassword = it },
                    label = { Text("Password") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    colors = darkFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = { tryRestore(rawBackupText!!, restorePassword) },
                    colors = ButtonDefaults.buttonColors(containerColor = CyanElectric, contentColor = JetBlack)
                ) { Text("Restore", fontWeight = FontWeight.Bold) }
            },
            dismissButton = { TextButton(onClick = { needsPassword = false }) { Text("Cancel", color = LavenderMuted) } }
        )
    }
}

@Composable
private fun AuditLogScreen(state: AppState, onBack: () -> Unit) {
    val fmt = remember { SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.US) }
    Column(modifier = Modifier.fillMaxSize().background(JetBlack).padding(horizontal = 16.dp)) {
        SubHeader("Audit Log", onBack)
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            items(state.auditLogs.sortedByDescending { it.timestamp }, key = { it.id }) { log ->
                val accent = when {
                    log.action == "DELETE" || log.recordType == "EXPENSE" -> CoralSoft
                    else -> EmeraldSoft
                }
                FintechCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 16.dp) {
                    Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
                        Box(modifier = Modifier.width(4.dp).fillMaxHeight().background(accent))
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text("${log.action} • ${log.recordType}", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.White)
                            if (log.oldValue.isNotBlank()) Text("Old: ${log.oldValue}", fontSize = 12.sp, color = LavenderMuted)
                            if (log.newValue.isNotBlank()) Text("New: ${log.newValue}", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                            Spacer(Modifier.height(2.dp))
                            Text(fmt.format(Date(log.timestamp)), fontSize = 11.sp, color = LavenderMuted)
                        }
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(JetBlack)
            .verticalScroll(rememberScrollState())
            .imePadding()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        SubHeader("PIN & Fingerprint Lock", onBack)
        FintechCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 22.dp) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
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
                    shape = RoundedCornerShape(16.dp),
                    colors = darkFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )
                if (newPin.isNotBlank() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Fingerprint / Biometric Unlock", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            Text("Use device biometrics on launch", color = LavenderMuted, fontSize = 11.sp)
                        }
                        Switch(
                            checked = bioEnabled,
                            onCheckedChange = { bioEnabled = it; saved = false },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = JetBlack,
                                checkedTrackColor = CyanElectric,
                                uncheckedThumbColor = LavenderMuted,
                                uncheckedTrackColor = JetBlack
                            )
                        )
                    }
                }
                Button(
                    onClick = {
                        onUpdateSecurity(newPin, bioEnabled)
                        saved = true
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CyanElectric, contentColor = JetBlack),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) { Text("Save Security Settings", fontWeight = FontWeight.Bold) }

                if (saved) {
                    Text(
                        if (newPin.isBlank()) "App lock disabled." else "Security settings saved.",
                        color = EmeraldSoft,
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}
