package com.mahibafaris.partnerledger.ui.screens

import android.hardware.biometrics.BiometricPrompt
import android.net.Uri
import android.os.Build
import android.os.CancellationSignal
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mahibafaris.partnerledger.data.LedgerRepository
import com.mahibafaris.partnerledger.export.ExportManager
import com.mahibafaris.partnerledger.model.*
import com.mahibafaris.partnerledger.ui.theme.*

@Composable
fun WelcomeAndSetupFlow(
    repo: LedgerRepository,
    onSetupComplete: (AppState) -> Unit,
    onRestoreComplete: (AppState) -> Unit
) {
    var mode by remember { mutableStateOf("WELCOME") }

    AnimatedContent(
        targetState = mode,
        transitionSpec = {
            (fadeIn(tween(240)) + slideInHorizontally { it / 6 }) togetherWith
                (fadeOut(tween(180)) + slideOutHorizontally { -it / 6 })
        },
        label = "welcomeFlow"
    ) { current ->
        if (current == "WELCOME") {
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

    Surface(modifier = Modifier.fillMaxSize(), color = JetBlack) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "PartnerLedger",
                fontSize = 32.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "Offline finance & equity ledger for partners",
                fontSize = 13.sp,
                color = LavenderMuted
            )

            Spacer(Modifier.height(34.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(HeroGradientBrush)
                    .border(1.dp, CyanElectric.copy(alpha = 0.35f), RoundedCornerShape(24.dp))
                    .padding(20.dp)
            ) {
                Column {
                    Text("New User", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "Start a fresh business ledger, add partners, and track expenses, revenue, and shares.",
                        fontSize = 13.sp,
                        color = LavenderMuted
                    )
                    Spacer(Modifier.height(16.dp))
                    Button(
                        onClick = onNewUser,
                        colors = ButtonDefaults.buttonColors(containerColor = CyanElectric, contentColor = JetBlack),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Set Up New Business", fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            FintechCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 24.dp) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("Returning User", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "Import your saved .ptb backup file to bring back partners, transactions, receipt photos, and logs.",
                        fontSize = 13.sp,
                        color = LavenderMuted
                    )
                    Spacer(Modifier.height(16.dp))
                    OutlinedButton(
                        onClick = {
                            errorMessage = null
                            filePicker.launch(arrayOf("*/*"))
                        },
                        border = BorderStroke(1.dp, CyanElectric.copy(alpha = 0.65f)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = CyanElectric),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Import Backup File (.ptb)", fontWeight = FontWeight.Bold)
                    }
                }
            }

            if (errorMessage != null) {
                Spacer(Modifier.height(16.dp))
                Text(errorMessage!!, color = CoralSoft, fontSize = 13.sp)
            }
        }
    }

    if (needsPassword && rawBackupText != null) {
        AlertDialog(
            onDismissRequest = { needsPassword = false },
            containerColor = AmethystElevated,
            titleContentColor = Color.White,
            textContentColor = LavenderMuted,
            title = { Text("Backup Password Required", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Enter the password used when saving this backup.")
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = passwordInput,
                        onValueChange = { passwordInput = it },
                        label = { Text("Backup Password") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        colors = darkFieldColors(),
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (errorMessage != null) {
                        Spacer(Modifier.height(8.dp))
                        Text(errorMessage!!, color = CoralSoft, fontSize = 12.sp)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { tryDecode(rawBackupText!!, passwordInput) },
                    colors = ButtonDefaults.buttonColors(containerColor = CyanElectric, contentColor = JetBlack)
                ) { Text("Unlock", fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { needsPassword = false }) { Text("Cancel", color = LavenderMuted) }
            }
        )
    }

    pendingRestoreState?.let { restored ->
        val biz = restored.business
        val receiptCount = restored.entries.count { !it.isDeleted && it.receiptImageBase64.isNotBlank() }
        AlertDialog(
            onDismissRequest = { pendingRestoreState = null },
            containerColor = AmethystElevated,
            titleContentColor = Color.White,
            textContentColor = Color.White,
            title = { Text("Confirm Backup Restore", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Business: ${biz?.businessName ?: "Unknown"}", fontWeight = FontWeight.Bold, color = CyanElectric)
                    Text("Currency: ${biz?.currency ?: "PKR"}", color = LavenderMuted)
                    Text("Partners: ${restored.partners.size}", color = LavenderMuted)
                    Text("Transactions: ${restored.entries.count { !it.isDeleted }} active ($receiptCount receipts)", color = LavenderMuted)
                    Text("Statements: ${restored.statements.size}", color = LavenderMuted)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val toSave = restored
                        pendingRestoreState = null
                        onRestored(toSave)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CyanElectric, contentColor = JetBlack)
                ) { Text("Restore Everything", fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { pendingRestoreState = null }) { Text("Cancel", color = LavenderMuted) }
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
    val context = LocalContext.current
    var step by remember { mutableStateOf(1) }

    var businessName by remember { mutableStateOf("") }
    var adminName by remember { mutableStateOf("") }
    var currency by remember { mutableStateOf("PKR") }
    var startDate by remember { mutableStateOf(todayDateString()) }
    var logoBase64 by remember { mutableStateOf("") }

    val logoPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            val encoded = ExportManager.compressUriToBase64(context, uri, maxDim = 400, quality = 80)
            if (encoded != null) logoBase64 = encoded
        }
    }
    val logoBitmap = remember(logoBase64) {
        if (logoBase64.isNotBlank()) ExportManager.decodeBase64ToBitmap(logoBase64) else null
    }

    val partners = remember { mutableStateListOf<Partner>() }
    var newPartnerName by remember { mutableStateOf("") }
    var newPartnerPct by remember { mutableStateOf("") }

    val openingInvestments = remember { mutableStateMapOf<String, String>() }

    var pin by remember { mutableStateOf("") }
    var biometricEnabled by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    Scaffold(
        containerColor = JetBlack,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = JetBlack, titleContentColor = Color.White),
                title = {
                    Column {
                        Text("Setup Business", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text("Step $step of 4", fontSize = 12.sp, color = CyanElectric)
                    }
                }
            )
        }
    ) { pad ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(pad)
                .imePadding()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            when (step) {
                1 -> {
                    item { Text("Business Details", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White) }
                    item {
                        // Optional Business Logo Picker
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(70.dp)
                                    .clip(CircleShape)
                                    .background(AmethystElevated)
                                    .border(1.5.dp, CyanElectric.copy(alpha = 0.55f), CircleShape)
                                    .clickable { logoPicker.launch("image/*") },
                                contentAlignment = Alignment.Center
                            ) {
                                if (logoBitmap != null) {
                                    Image(
                                        bitmap = logoBitmap.asImageBitmap(),
                                        contentDescription = "Business Logo",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    Icon(Icons.Default.Add, contentDescription = "Add Logo", tint = CyanElectric)
                                }
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Business Logo (Optional)", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                                Text(
                                    "Tap the circle to pick a logo, or skip and add it anytime from your Profile.",
                                    fontSize = 11.sp,
                                    color = LavenderMuted
                                )
                                if (logoBase64.isNotBlank()) {
                                    Text(
                                        "Remove logo",
                                        fontSize = 11.sp,
                                        color = CoralSoft,
                                        modifier = Modifier.padding(top = 4.dp).clickable { logoBase64 = "" }
                                    )
                                }
                            }
                        }
                    }
                    item {
                        OutlinedTextField(
                            value = businessName,
                            onValueChange = { businessName = it },
                            label = { Text("Business Name") },
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp),
                            colors = darkFieldColors(),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    item {
                        OutlinedTextField(
                            value = adminName,
                            onValueChange = { adminName = it },
                            label = { Text("Your Name (Admin)") },
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp),
                            colors = darkFieldColors(),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    item {
                        Text("Main Business Currency", fontSize = 13.sp, color = LavenderMuted)
                        Spacer(Modifier.height(6.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(SUPPORTED_CURRENCIES) { cur ->
                                FintechChip(label = cur, selected = currency == cur, onClick = { currency = cur })
                            }
                        }
                    }
                    item {
                        DatePickerField(label = "Business Start Date", value = startDate, onValueChange = { startDate = it })
                    }
                }

                2 -> {
                    item {
                        Text("Add Partners", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Text("Ownership % is optional.", fontSize = 12.sp, color = LavenderMuted)
                    }
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = newPartnerName,
                                onValueChange = { newPartnerName = it },
                                label = { Text("Partner Name") },
                                singleLine = true,
                                shape = RoundedCornerShape(16.dp),
                                colors = darkFieldColors(),
                                modifier = Modifier.weight(1.5f)
                            )
                            OutlinedTextField(
                                value = newPartnerPct,
                                onValueChange = { newPartnerPct = it },
                                label = { Text("% Share") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true,
                                shape = RoundedCornerShape(16.dp),
                                colors = darkFieldColors(),
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = {
                                val trimmed = newPartnerName.trim()
                                if (trimmed.isNotEmpty()) {
                                    partners.add(Partner(name = trimmed, agreedOwnershipPct = newPartnerPct.toDoubleOrNull() ?: 0.0))
                                    newPartnerName = ""
                                    newPartnerPct = ""
                                    error = null
                                }
                            },
                            border = BorderStroke(1.dp, CyanElectric.copy(alpha = 0.6f)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = CyanElectric),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text("Add Partner", fontWeight = FontWeight.Bold)
                        }
                    }
                    items(partners) { p ->
                        FintechCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 16.dp) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(p.name, fontWeight = FontWeight.Bold, color = Color.White)
                                    Text("Agreed Ownership: ${p.agreedOwnershipPct}%", fontSize = 12.sp, color = LavenderMuted)
                                }
                                IconButton(onClick = { partners.remove(p) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Remove", tint = CoralSoft)
                                }
                            }
                        }
                    }
                }

                3 -> {
                    item {
                        Text("Opening Capital (Optional)", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Text("Enter initial cash invested, or skip.", fontSize = 12.sp, color = LavenderMuted)
                    }
                    items(partners) { p ->
                        OutlinedTextField(
                            value = openingInvestments[p.id] ?: "",
                            onValueChange = { openingInvestments[p.id] = it },
                            label = { Text("${p.name}'s Opening Investment ($currency)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp),
                            colors = darkFieldColors(),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                4 -> {
                    item {
                        Text("App Lock (Optional)", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Text("Set a 4-digit PIN or leave blank to skip.", fontSize = 12.sp, color = LavenderMuted)
                    }
                    item {
                        OutlinedTextField(
                            value = pin,
                            onValueChange = { if (it.length <= 6) pin = it.filter { ch -> ch.isDigit() } },
                            label = { Text("4-Digit PIN (Optional)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                            visualTransformation = PasswordVisualTransformation(),
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp),
                            colors = darkFieldColors(),
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
                                Text("Fingerprint / Biometric Unlock", color = Color.White)
                                Switch(
                                    checked = biometricEnabled,
                                    onCheckedChange = { biometricEnabled = it },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = JetBlack,
                                        checkedTrackColor = CyanElectric
                                    )
                                )
                            }
                        }
                    }
                }
            }

            if (error != null) {
                item { Text(error!!, color = CoralSoft, fontSize = 13.sp) }
            }

            item {
                Spacer(Modifier.height(12.dp))
                ThinDivider()
                Spacer(Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    OutlinedButton(
                        onClick = {
                            error = null
                            if (step == 1) onBackToWelcome() else step -= 1
                        },
                        border = BorderStroke(1.dp, AmethystGlow.copy(alpha = 0.4f)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = LavenderMuted),
                        shape = RoundedCornerShape(14.dp)
                    ) { Text(if (step == 1) "Back" else "Previous") }

                    Button(
                        onClick = {
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
                                        biometricEnabled = biometricEnabled,
                                        logoBase64 = logoBase64
                                    )
                                    val orderedCurrencies = (listOf(currency) + SUPPORTED_CURRENCIES).distinct()
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
                                            currencies = orderedCurrencies,
                                            entries = initialEntries,
                                            auditLogs = initialLogs
                                        )
                                    )
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CyanElectric, contentColor = JetBlack),
                        shape = RoundedCornerShape(14.dp)
                    ) { Text(if (step == 4) "Finish Setup" else "Next", fontWeight = FontWeight.Bold) }
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
                        if (errorCode != BiometricPrompt.BIOMETRIC_ERROR_USER_CANCELED) {
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

    Surface(modifier = Modifier.fillMaxSize(), color = JetBlack) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
                .padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(AmethystElevated)
                    .border(1.dp, CyanElectric.copy(alpha = 0.4f), RoundedCornerShape(20.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Lock, contentDescription = null, tint = CyanElectric, modifier = Modifier.size(30.dp))
            }
            Spacer(Modifier.height(16.dp))
            Text(businessName, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
            Spacer(Modifier.height(4.dp))
            Text("Enter your PIN or use Fingerprint", fontSize = 13.sp, color = LavenderMuted)
            Spacer(Modifier.height(24.dp))

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
                shape = RoundedCornerShape(16.dp),
                colors = darkFieldColors(),
                modifier = Modifier.fillMaxWidth()
            )

            if (wrong) {
                Spacer(Modifier.height(8.dp))
                Text("Incorrect PIN", color = CoralSoft, fontSize = 13.sp)
            }
            if (bioStatus != null) {
                Spacer(Modifier.height(8.dp))
                Text(bioStatus!!, color = LavenderMuted, fontSize = 12.sp)
            }

            Spacer(Modifier.height(16.dp))
            Button(
                onClick = { if (entered == expectedPin) onUnlocked() else wrong = true },
                colors = ButtonDefaults.buttonColors(containerColor = CyanElectric, contentColor = JetBlack),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) { Text("Unlock with PIN", fontWeight = FontWeight.Bold) }

            if (biometricEnabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                Spacer(Modifier.height(10.dp))
                OutlinedButton(
                    onClick = { launchBiometric() },
                    border = BorderStroke(1.dp, CyanElectric.copy(alpha = 0.55f)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = CyanElectric),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) { Text("Unlock with Fingerprint", fontWeight = FontWeight.Bold) }
            }
        }
    }
}
