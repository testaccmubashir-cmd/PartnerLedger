package com.mahibafaris.partnerledger.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.mahibafaris.partnerledger.model.*
import com.mahibafaris.partnerledger.ui.theme.AppColors
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs

@Composable
fun PinLockScreen(
    expectedPin: String,
    businessName: String,
    onUnlocked: () -> Unit
) {
    var entered by remember { mutableStateOf("") }
    var showError by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Default.Lock,
            contentDescription = null,
            modifier = Modifier.size(52.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.height(12.dp))
        Text(businessName, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text("Enter your Finance Admin PIN", style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(20.dp))

        OutlinedTextField(
            value = entered,
            onValueChange = {
                if (it.length <= 6) {
                    entered = it
                    showError = false
                    if (it == expectedPin) onUnlocked()
                }
            },
            label = { Text("PIN") },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            isError = showError,
            singleLine = true
        )
        if (showError) {
            Spacer(Modifier.height(6.dp))
            Text("Incorrect PIN. Try again.", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = { if (entered == expectedPin) onUnlocked() else showError = true },
            modifier = Modifier.fillMaxWidth(0.6f).height(48.dp)
        ) {
            Text("Unlock Ledger")
        }
    }
}

@Composable
fun SetupWizardScreen(
    onFinish: (BusinessConfig, List<Partner>, List<LedgerEntry>, List<Category>) -> Unit
) {
    var step by remember { mutableIntStateOf(1) }

    // Step 1
    var bizName by remember { mutableStateOf("") }
    var currency by remember { mutableStateOf("PKR") }
    var startDate by remember { mutableStateOf(SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())) }
    var adminName by remember { mutableStateOf("") }

    // Step 2
    val partnerDrafts = remember { mutableStateListOf<Triple<Partner, Double, String>>() }
    var pName by remember { mutableStateOf("") }
    var pContact by remember { mutableStateOf("") }
    var pAgreed by remember { mutableStateOf("") }
    var pOpening by remember { mutableStateOf("") }

    // Step 3
    val selectedCategories = remember { mutableStateListOf<Category>().apply { addAll(defaultCategories()) } }
    var customCat by remember { mutableStateOf("") }

    // Step 4
    var pin by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("Step $step of 4", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        LinearProgressIndicator(
            progress = { step / 4f },
            modifier = Modifier.fillMaxWidth()
        )

        when (step) {
            1 -> {
                Text("Business Details", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text("Everything stays offline on your phone. No sign-up or server needed.", style = MaterialTheme.typography.bodySmall)
                OutlinedTextField(value = bizName, onValueChange = { bizName = it }, label = { Text("Business Name *") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = currency, onValueChange = { currency = it }, label = { Text("Currency (PKR, USD, AED) *") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = startDate, onValueChange = { startDate = it }, label = { Text("Start Date (YYYY-MM-DD) *") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = adminName, onValueChange = { adminName = it }, label = { Text("Finance Admin Name *") }, modifier = Modifier.fillMaxWidth())

                Button(
                    onClick = { if (bizName.isNotBlank() && adminName.isNotBlank()) step = 2 },
                    modifier = Modifier.fillMaxWidth().height(50.dp)
                ) {
                    Text("Next: Add Partners")
                }
            }
            2 -> {
                Text("Add Partners", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                val totalAgreed = partnerDrafts.sumOf { it.first.agreedOwnershipPct }
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (abs(totalAgreed - 100.0) < 0.01) AppColors.MoneyInBg else AppColors.WarningBg
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = if (abs(totalAgreed - 100.0) < 0.01) "Agreed Ownership Total: 100.0% (Balanced)"
                        else "Agreed Ownership Total: %.1f%% (Usually adds up to 100%%)".format(totalAgreed),
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = if (abs(totalAgreed - 100.0) < 0.01) AppColors.MoneyIn else AppColors.Warning
                    )
                }

                partnerDrafts.forEachIndexed { idx, item ->
                    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                        Row(
                            modifier = Modifier.padding(12.dp).fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(item.first.name, fontWeight = FontWeight.Bold)
                                Text("Agreed: %.1f%% • Opening: $currency %,.0f".format(item.first.agreedOwnershipPct, item.second), style = MaterialTheme.typography.bodySmall)
                            }
                            TextButton(onClick = { partnerDrafts.removeAt(idx) }) { Text("Remove") }
                        }
                    }
                }

                OutlinedTextField(value = pName, onValueChange = { pName = it }, label = { Text("Partner Name") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = pContact, onValueChange = { pContact = it }, label = { Text("Phone or Email (Optional)") }, modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = pAgreed,
                        onValueChange = { pAgreed = it },
                        label = { Text("Agreed %") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = pOpening,
                        onValueChange = { pOpening = it },
                        label = { Text("Opening ($currency)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f)
                    )
                }
                OutlinedButton(
                    onClick = {
                        if (pName.isNotBlank()) {
                            val p = Partner(
                                id = UUID.randomUUID().toString(),
                                name = pName.trim(),
                                contact = pContact.trim(),
                                agreedOwnershipPct = pAgreed.toDoubleOrNull() ?: 0.0
                            )
                            partnerDrafts.add(Triple(p, pOpening.toDoubleOrNull() ?: 0.0, startDate))
                            pName = ""; pContact = ""; pAgreed = ""; pOpening = ""
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("+ Add Partner to List")
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { step = 1 }, modifier = Modifier.weight(1f).height(48.dp)) { Text("Back") }
                    Button(
                        onClick = { if (partnerDrafts.isNotEmpty()) step = 3 },
                        modifier = Modifier.weight(2f).height(48.dp)
                    ) { Text("Next: Categories") }
                }
            }
            3 -> {
                Text("Expense Categories", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text("Standard categories are pre-loaded. Add any custom ones you need.", style = MaterialTheme.typography.bodySmall)

                selectedCategories.forEach { cat ->
                    Text("• ${cat.name}", style = MaterialTheme.typography.bodyMedium)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = customCat,
                        onValueChange = { customCat = it },
                        label = { Text("Custom Category") },
                        modifier = Modifier.weight(1f)
                    )
                    Button(onClick = {
                        if (customCat.isNotBlank()) {
                            selectedCategories.add(Category(UUID.randomUUID().toString(), customCat.trim()))
                            customCat = ""
                        }
                    }) { Text("Add") }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { step = 2 }, modifier = Modifier.weight(1f).height(48.dp)) { Text("Back") }
                    Button(onClick = { step = 4 }, modifier = Modifier.weight(2f).height(48.dp)) { Text("Next: Security") }
                }
            }
            4 -> {
                Text("App Lock (Optional)", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text("Set a 4–6 digit PIN to lock the ledger when opening the app, or leave blank to skip.", style = MaterialTheme.typography.bodySmall)
                OutlinedTextField(
                    value = pin,
                    onValueChange = { if (it.length <= 6) pin = it },
                    label = { Text("4–6 Digit PIN (Optional)") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { step = 3 }, modifier = Modifier.weight(1f).height(50.dp)) { Text("Back") }
                    Button(
                        onClick = {
                            val config = BusinessConfig(
                                businessName = bizName.trim(),
                                currency = currency.trim().ifBlank { "PKR" },
                                startDate = startDate.trim(),
                                adminName = adminName.trim(),
                                pin = pin.trim()
                            )
                            val partners = partnerDrafts.map { it.first }
                            val openingEntries = partnerDrafts.filter { it.second > 0.0 }.map { (partner, amt, dt) ->
                                LedgerEntry(
                                    id = UUID.randomUUID().toString(),
                                    type = EntryType.INVESTMENT,
                                    amount = amt,
                                    date = dt,
                                    partnerId = partner.id,
                                    paymentMethod = "Bank Transfer",
                                    note = "Opening investment",
                                    hasProof = false
                                )
                            }
                            onFinish(config, partners, openingEntries, selectedCategories.toList())
                        },
                        modifier = Modifier.weight(2f).height(50.dp)
                    ) { Text("Finish & Open Dashboard") }
                }
            }
        }
    }
}
