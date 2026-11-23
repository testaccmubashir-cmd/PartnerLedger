package com.mahibafaris.partnerledger.ui.screens

import android.hardware.biometrics.BiometricPrompt
import android.net.Uri
import android.os.Build
import android.os.CancellationSignal
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
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
import com.mahibafaris.partnerledger.data.LedgerRepository
import com.mahibafaris.partnerledger.export.ExportManager
import com.mahibafaris.partnerledger.model.*
import com.mahibafaris.partnerledger.ui.theme.DatePickerField
import com.mahibafaris.partnerledger.ui.theme.ThinDivider

@Composable
fun WelcomeAndSetupFlow(
    repo: LedgerRepository,
    onSetupComplete: (AppState) -> Unit,
    onRestoreComplete: (AppState) -> Unit
) {
    var mode by remember { mutableStateOf("WELCOME") }

    if (mode == "WELCOME") {
        WelcomeScreen(
            repo = repo,
            onNewUser = { mode = "WIZARD" },
            onRestored = onRestoreComplete
        )
    } else {
        BusinessSetupWizard(
            onBackToWelcome = { mode = "WELCOME" },
            onComplete = onSetupComplete
        )
    }
}

@Composable
fun WelcomeScreen(
    repo: LedgerRepository,
    onNewUser: () -> Unit,
    onRestored: (AppState) -> Unit
) {
    val context = LocalContext.current
    var rawBackupText by remember { mutableStateOf<String?>(null) }
    var needsPassword by remember { mutableStateOf(false) }
    var passwordInput by remember { mutableStateOf("") }
    var pendingRestoreState by remember { mutableStateOf<AppState?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    fun tryDecode(raw: String, pwd: String) {
        val result = ExportManager.decodeBackupPayload(raw, pwd)
        result.onSuccess { json ->
            try {
                val parsed = repo.fromJson(json)
                if (parsed.business == null) {
                    errorMessage = "This backup file does not contain business data."
                } else {
                    needsPassword = false
                    errorMessage = null
                    pendingRestoreState = parsed
                }
            } catch (e: Exception) {
                errorMessage = "Could not read backup file. Make sure you picked a valid .ptb backup."
            }
        }.onFailure { err ->
            when (err.message) {
                "PASSWORD_REQUIRED" -> {
                    needsPassword = true
                    errorMessage = null
                }
                "WRONG_PASSWORD" -> {
                    needsPassword = true
                    errorMessage = "Wrong password. Please try again."
                }
                else -> errorMessage = "Failed to open backup file."
            }
        }
    }

    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) {
            try {
                val text = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() } ?: ""
                rawBackupText = text
                passwordInput = ""
                tryDecode(text, "")
            } catch (e: Exception) {
                errorMessage = "Could not open the selected file."
            }
        }
    }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "PartnerLedger",
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Offline finance & equity ledger for partners",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray
            )

            Spacer(Modifier.height(36.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("New User", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "Start a fresh business ledger, add partners, and track expenses, revenue, and shares.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                    Spacer(Modifier.height(14.dp))
                    Button(
                        onClick = onNewUser,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White,
                            contentColor = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Set Up New Business", fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(Modifier.height(18.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("Returning User", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "Import your saved .ptb backup file to bring back all partners, transactions, receipt photos, statements, and logs.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray
                    )
                    Spacer(Modifier.height(14.dp))
                    OutlinedButton(
                        onClick = {
                            errorMessage = null
                            filePicker.launch(arrayOf("*/*"))
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Import Backup File (.ptb)", fontWeight = FontWeight.Bold)
                    }
                }
            }

            if (errorMessage != null) {
                Spacer(Modifier.height(16.dp))
                Text(errorMessage!!, color = MaterialTheme.colorScheme.error)
            }
        }
    }

    if (needsPassword && rawBackupText != null) {
        AlertDialog(
            onDismissRequest = { needsPassword = false },
            title = { Text("Backup Password Required") },
            text = {
                Column {
                    Text("This backup file is encrypted. Enter the password you used when saving it.")
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = passwordInput,
                        onValueChange = { passwordInput = it },
                        label = { Text("Backup Password") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (errorMessage != null) {
                        Spacer(Modifier.height(8.dp))
                        Text(errorMessage!!, color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
                    }
                }
            },
            confirmButton = {
                Button(onClick = { tryDecode(rawBackupText!!, passwordInput) }) { Text("Unlock") }
            },
            dismissButton = {
                TextButton(onClick = { needsPassword = false }) { Text("Cancel") }
            }
        )
    }

    pendingRestoreState?.let { restored ->
        val biz = restored.business
        val receiptCount = restored.entries.count { !it.isDeleted && it.receiptImageBase64.isNotBlank() }
        AlertDialog(
            onDismissRequest = { pendingRestoreState = null },
            title = { Text("Confirm Backup Restore") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Business: ${biz?.businessName ?: "Unknown"}", fontWeight = FontWeight.Bold)
                    Text("Currency: ${biz?.currency ?: "PKR"}")
                    Text("Partners: ${restored.partners.size}")
                    Text("Transactions: ${restored.entries.count { !it.isDeleted }} active ($receiptCount with receipts)")
                    Text("Statements: ${restored.statements.size}")
                    Text("Audit Logs: ${restored.auditLogs.size}")
                }
            },
            confirmButton = {
                Button(onClick = {
                    val toSave = restored
                    pendingRestoreState = null
                    onRestored(toSave)
                }) { Text("Restore Everything") }
            },
            dismissButton = {
                TextButton(onClick = { pendingRestoreState = null }) { Text("Cancel") }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BusinessSetupWizard(
    onBackToWelcome: () -> Unit,
    onComplete: (AppState) -> Unit
) {
    var step by remember { mutableStateOf(1) }

    var businessName by remember { mutableStateOf("") }
    var adminName by remember { mutableStateOf("") }
    var currency by remember { mutableStateOf("PKR") }
    var startDate by remember { mutableStateOf(todayDateString()) }

    val partners = remember { mutableStateListOf<Partner>() }
    var newPartnerName by remember { mutableStateOf("") }
    var newPartnerPct by remember { mutableStateOf("") }

    val openingInvestments = remember { mutableStateMapOf<String, String>() }

    var pin by remember { mutableStateOf("") }
    var biometricEnabled by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Setup Business (Step $step of 4)") }) }
    ) { pad ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(pad)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            when (step) {
                1 -> {
                    item { Text("Business Details", fontSize = 18.sp, fontWeight = FontWeight.Bold) }
                    item {
                        OutlinedTextField(
                            value = businessName,
                            onValueChange = { businessName = it },
                            label = { Text("Business Name") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    item {
                        OutlinedTextField(
                            value = adminName,
                            onValueChange = { adminName = it },
                            label = { Text("Your Name (Admin)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    item {
                        Text("Main Business Currency", fontWeight = FontWeight.Medium)
                        Spacer(Modifier.height(6.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(SUPPORTED_CURRENCIES) { cur ->
                                FilterChip(
                                    selected = currency == cur,
                                    onClick = { currency = cur },
                                    label = { Text(cur) }
                                )
                            }
                        }
                    }
                    item {
                        DatePickerField(
                            label = "Business Start Date",
                            value = startDate,
                            onValueChange = { startDate = it }
                        )
                    }
                }

                2 -> {
                    item {
                        Text("Add Partners", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Text(
                            "Add everyone in the partnership. Ownership % is optional.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                    }
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = newPartnerName,
                                onValueChange = { newPartnerName = it },
                                label = { Text("Partner Name") },
                                singleLine = true,
                                modifier = Modifier.weight(1.5f)
                            )
                            OutlinedTextField(
                                value = newPartnerPct,
                                onValueChange = { newPartnerPct = it },
                                label = { Text("% Share") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = {
                                val trimmed = newPartnerName.trim()
                                if (trimmed.isNotEmpty()) {
                                    partners.add(
                                        Partner(
                                            name = trimmed,
                                            agreedOwnershipPct = newPartnerPct.toDoubleOrNull() ?: 0.0
                                        )
                                    )
                                    newPartnerName = ""
                                    newPartnerPct = ""
                                    error = null
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text("Add Partner")
                        }
                    }
                    items(partners) { p ->
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(p.name, fontWeight = FontWeight.Bold)
                                    Text("Agreed Ownership: ${p.agreedOwnershipPct}%", fontSize = 12.sp, color = Color.Gray)
                                }
                                IconButton(onClick = { partners.remove(p) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Remove")
                                }
                            }
                        }
                    }
                }

                3 -> {
                    item {
                        Text("Opening Capital (Optional)", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Text(
                            "If partners already put cash into the business, enter it here. Otherwise tap Next.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                    }
                    items(partners) { p ->
                        OutlinedTextField(
                            value = openingInvestments[p.id] ?: "",
                            onValueChange = { openingInvestments[p.id] = it },
                            label = { Text("${p.name}'s Opening Investment ($currency)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                4 -> {
                    item {
                        Text("App Lock (Optional)", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Text(
                            "Set a 4-digit PIN to protect your ledger, or leave blank to skip.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                    }
                    item {
                        OutlinedTextField(
                            value = pin,
                            onValueChange = { if (it.length <= 6) pin = it.filter { ch -> ch.isDigit() } },
                            label = { Text("4-Digit PIN (Optional)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                            visualTransformation = PasswordVisualTransformation(),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    if (pin.isNotBlank() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Allow Fingerprint / Biometric Unlock")
                                Switch(checked = biometricEnabled, onCheckedChange = { biometricEnabled = it })
                            }
                        }
                    }
                }
            }

            if (error != null) {
                item { Text(error!!, color = MaterialTheme.colorScheme.error) }
            }

            item {
                Spacer(Modifier.height(12.dp))
                ThinDivider()
                Spacer(Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    OutlinedButton(onClick = {
                        error = null
                        if (step == 1) onBackToWelcome() else step -= 1
                    }) { Text(if (step == 1) "Back" else "Previous") }

                    Button(onClick = {
                        error = null
                        when (step) {
                            1 -> if (businessName.isBlank()) error = "Please enter a business name." else step = 2
                            2 -> if (partners.isEmpty()) error = "Add at least one partner." else step = 3
                            3 -> step = 4
                            4 -> {
                                val biz = BusinessConfig(
                                    businessName = businessName.trim(),
                                    adminName = adminName.trim().ifBlank { partners.first().name },
                                    currency = currency,
                                    startDate = startDate.ifBlank { todayDateString() },
                                    pin = pin,
                                    biometricEnabled = biometricEnabled
                                )
                                val initialEntries = mutableListOf<LedgerEntry>()
                                val initialLogs = mutableListOf<AuditLog>()
                                partners.forEach { p ->
                                    val amt = openingInvestments[p.id]?.toDoubleOrNull() ?: 0.0
                                    if (amt > 0.0) {
                                        val entry = LedgerEntry(
                                            type = EntryType.INVESTMENT,
                                            date = biz.startDate,
                                            amount = amt,
                                            partnerId = p.id,
                                            note = "Opening capital"
                                        )
                                        initialEntries.add(entry)
                                        initialLogs.add(
                                            AuditLog(
                                                action = "CREATE",
                                                recordType = "INVESTMENT",
                                                recordId = entry.id,
                                                newValue = "${p.name}: ${formatMoney(biz.currency, amt)}"
                                            )
                                        )
                                    }
                                }
                                onComplete(
                                    AppState(
                                        business = biz,
                                        partners = partners.toList(),
                                        categories = defaultCategories(),
                                        entries = initialEntries,
                                        auditLogs = initialLogs
                                    )
                                )
                            }
                        }
                    }) { Text(if (step == 4) "Finish Setup" else "Next") }
                }
            }
        }
    }
}

@Composable
fun PinLockScreen(
    expectedPin: String,
    businessName: String,
    biometricEnabled: Boolean,
    onUnlocked: () -> Unit
) {
    val context = LocalContext.current
    var entered by remember { mutableStateOf("") }
    var wrong by remember { mutableStateOf(false) }
    var bioStatus by remember { mutableStateOf<String?>(null) }

    fun launchBiometric() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return
        try {
            val prompt = BiometricPrompt.Builder(context)
                .setTitle("Unlock $businessName")
                .setSubtitle("Use your fingerprint or face")
                .setNegativeButton("Use PIN", context.mainExecutor) { _, _ -> }
                .build()
            prompt.authenticate(
                CancellationSignal(),
                context.mainExecutor,
                object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult?) {
                        onUnlocked()
                    }
                    override fun onAuthenticationError(errorCode: Int, errString: CharSequence?) {
                        if (errorCode != BiometricPrompt.BIOMETRIC_ERROR_USER_CANCELED &&
                            errorCode != BiometricPrompt.BIOMETRIC_ERROR_NEGATIVE_BUTTON
                        ) {
                            bioStatus = errString?.toString()
                        }
                    }
                }
            )
        } catch (e: Exception) {
            bioStatus = "Biometric unlock unavailable on this device. Use your PIN."
        }
    }

    LaunchedEffect(biometricEnabled) {
        if (biometricEnabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            launchBiometric()
        }
    }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Default.Lock,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(48.dp)
            )
            Spacer(Modifier.height(12.dp))
            Text(businessName, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            Text("Enter your PIN or use Fingerprint", color = Color.Gray)
            Spacer(Modifier.height(20.dp))

            OutlinedTextField(
                value = entered,
                onValueChange = {
                    entered = it.filter { ch -> ch.isDigit() }
                    wrong = false
                    if (entered == expectedPin) onUnlocked()
                },
                label = { Text("PIN") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            if (wrong) {
                Spacer(Modifier.height(8.dp))
                Text("Incorrect PIN", color = MaterialTheme.colorScheme.error)
            }
            if (bioStatus != null) {
                Spacer(Modifier.height(8.dp))
                Text(bioStatus!!, color = Color.Gray, fontSize = 12.sp)
            }

            Spacer(Modifier.height(16.dp))
            Button(
                onClick = { if (entered == expectedPin) onUnlocked() else wrong = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Unlock with PIN")
            }

            if (biometricEnabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                Spacer(Modifier.height(10.dp))
                OutlinedButton(
                    onClick = { launchBiometric() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Unlock with Fingerprint")
                }
            }
        }
    }
}
