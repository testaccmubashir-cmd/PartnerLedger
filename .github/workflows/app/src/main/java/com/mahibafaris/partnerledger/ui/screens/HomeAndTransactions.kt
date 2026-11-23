package com.mahibafaris.partnerledger.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mahibafaris.partnerledger.data.LedgerCalculator
import com.mahibafaris.partnerledger.export.ExportManager
import com.mahibafaris.partnerledger.model.*
import com.mahibafaris.partnerledger.ui.theme.BrandAccent
import com.mahibafaris.partnerledger.ui.theme.BrandDanger
import com.mahibafaris.partnerledger.ui.theme.BrandSecondary
import com.mahibafaris.partnerledger.ui.theme.DatePickerField
import java.util.Locale

@Composable
fun HomeDashboardScreen(
    state: AppState,
    onAddClick: (EntryType) -> Unit,
    onEntryClick: (LedgerEntry) -> Unit
) {
    val biz = state.business ?: return
    val totals = remember(state) { LedgerCalculator.compute(state) }
    val recent = remember(state) {
        state.entries.filter { !it.isDeleted }.sortedByDescending { it.date + it.createdAt }.take(8)
    }
    val pMap = remember(state.partners) { state.partners.associateBy { it.id } }
    val cMap = remember(state.categories) { state.categories.associateBy { it.id } }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(biz.businessName, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Text("Base currency: ${biz.currency}", fontSize = 13.sp, color = Color.Gray)
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("Business Cash Balance", color = Color.White.copy(alpha = 0.85f), fontSize = 13.sp)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        formatMoney(biz.currency, totals.currentBalance),
                        color = Color.White,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(10.dp))
                    val profitColor = if (totals.netProfit >= 0) Color(0xFFA7F3D0) else Color(0xFFFCA5A5)
                    Text(
                        "Net Profit / Loss: ${formatMoney(biz.currency, totals.netProfit)}",
                        color = profitColor,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    )
                }
            }
        }

        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatCard("Total Invested", formatMoney(biz.currency, totals.totalInvested), BrandSecondary, Modifier.weight(1f))
                StatCard("Total Revenue", formatMoney(biz.currency, totals.totalRevenue), BrandSecondary, Modifier.weight(1f))
            }
            Spacer(Modifier.height(10.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatCard("Total Expenses", formatMoney(biz.currency, totals.totalExpenses), BrandDanger, Modifier.weight(1f))
                StatCard("Total Withdrawn", formatMoney(biz.currency, totals.totalWithdrawn), BrandAccent, Modifier.weight(1f))
            }
        }

        item {
            Text("Quick Add", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Spacer(Modifier.height(6.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(EntryType.values()) { t ->
                    FilledTonalButton(onClick = { onAddClick(t) }) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(t.label)
                    }
                }
            }
        }

        item { Text("Recent Activity", fontWeight = FontWeight.Bold, fontSize = 16.sp) }

        if (recent.isEmpty()) {
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        "No transactions yet. Tap + Add to log your first expense, revenue, or investment.",
                        modifier = Modifier.padding(16.dp),
                        color = Color.Gray
                    )
                }
            }
        } else {
            items(recent) { entry ->
                EntryRowCard(
                    entry = entry,
                    baseCurrency = biz.currency,
                    partnerName = pMap[entry.partnerId]?.name,
                    paidByName = pMap[entry.paidByPartnerId]?.name,
                    categoryName = cMap[entry.categoryId]?.name,
                    onClick = { onEntryClick(entry) }
                )
            }
        }
    }
}

@Composable
fun StatCard(label: String, value: String, accent: Color, modifier: Modifier = Modifier) {
    Card(modifier = modifier, shape = RoundedCornerShape(12.dp)) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(label, fontSize = 12.sp, color = Color.Gray)
            Spacer(Modifier.height(4.dp))
            Text(value, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = accent)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionsListScreen(
    state: AppState,
    onAddClick: () -> Unit,
    onEntryClick: (LedgerEntry) -> Unit
) {
    val biz = state.business ?: return
    var search by remember { mutableStateOf("") }
    var filterType by remember { mutableStateOf<EntryType?>(null) }

    val pMap = remember(state.partners) { state.partners.associateBy { it.id } }
    val cMap = remember(state.categories) { state.categories.associateBy { it.id } }

    val filtered = remember(state.entries, search, filterType) {
        state.entries
            .filter { !it.isDeleted }
            .filter { filterType == null || it.type == filterType }
            .filter { e ->
                if (search.isBlank()) true
                else {
                    val q = search.lowercase(Locale.US)
                    e.note.lowercase(Locale.US).contains(q) ||
                        e.vendorOrSource.lowercase(Locale.US).contains(q) ||
                        (pMap[e.partnerId]?.name?.lowercase(Locale.US)?.contains(q) == true) ||
                        (pMap[e.paidByPartnerId]?.name?.lowercase(Locale.US)?.contains(q) == true) ||
                        (cMap[e.categoryId]?.name?.lowercase(Locale.US)?.contains(q) == true) ||
                        e.date.contains(q)
                }
            }
            .sortedByDescending { it.date + it.createdAt }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Ledger", fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Button(onClick = onAddClick) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(Modifier.width(4.dp))
                Text("Add")
            }
        }

        Spacer(Modifier.height(10.dp))

        OutlinedTextField(
            value = search,
            onValueChange = { search = it },
            placeholder = { Text("Search note, vendor, partner, date...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(8.dp))

        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                FilterChip(selected = filterType == null, onClick = { filterType = null }, label = { Text("All") })
            }
            items(EntryType.values()) { t ->
                FilterChip(
                    selected = filterType == t,
                    onClick = { filterType = if (filterType == t) null else t },
                    label = { Text(t.label) }
                )
            }
        }

        Spacer(Modifier.height(10.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(filtered) { entry ->
                EntryRowCard(
                    entry = entry,
                    baseCurrency = biz.currency,
                    partnerName = pMap[entry.partnerId]?.name,
                    paidByName = pMap[entry.paidByPartnerId]?.name,
                    categoryName = cMap[entry.categoryId]?.name,
                    onClick = { onEntryClick(entry) }
                )
            }
        }
    }
}

@Composable
fun EntryRowCard(
    entry: LedgerEntry,
    baseCurrency: String,
    partnerName: String?,
    paidByName: String?,
    categoryName: String?,
    onClick: () -> Unit
) {
    val color = when (entry.type) {
        EntryType.INVESTMENT, EntryType.REVENUE -> BrandSecondary
        EntryType.EXPENSE -> BrandDanger
        EntryType.WITHDRAWAL -> BrandAccent
    }
    val prefix = when (entry.type) {
        EntryType.INVESTMENT, EntryType.REVENUE -> "+"
        EntryType.EXPENSE, EntryType.WITHDRAWAL -> "-"
    }
    val origText = formatEntryOriginal(entry, baseCurrency)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = buildString {
                            append(entry.type.label)
                            if (!categoryName.isNullOrBlank()) append(" • $categoryName")
                            else if (!partnerName.isNullOrBlank()) append(" • $partnerName")
                            if (entry.receiptImageBase64.isNotBlank()) append("  [Receipt]")
                        },
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    if (entry.vendorOrSource.isNotBlank()) {
                        Text(entry.vendorOrSource, fontSize = 13.sp, color = Color.DarkGray)
                    }
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "$prefix${formatMoney(baseCurrency, entry.amount)}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = color
                    )
                    if (origText != null) {
                        Text(text = "($origText)", fontSize = 11.sp, color = Color.Gray)
                    }
                }
            }

            Spacer(Modifier.height(4.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("${entry.date} • ${entry.paymentMethod}", fontSize = 11.sp, color = Color.Gray)
                if (entry.type == EntryType.EXPENSE && paidByName != null) {
                    val modeTag = if (entry.countAsInvestment) "Investment" else "Reimburse"
                    Text(
                        text = "Paid by $paidByName ($modeTag)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (entry.countAsInvestment) BrandSecondary else BrandAccent
                    )
                }
            }

            if (entry.note.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(entry.note, fontSize = 12.sp, color = Color.Gray)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditEntryDialog(
    state: AppState,
    initialType: EntryType,
    existing: LedgerEntry?,
    onDismiss: () -> Unit,
    onSave: (LedgerEntry) -> Unit,
    onDelete: ((LedgerEntry) -> Unit)? = null
) {
    val biz = state.business ?: return
    val context = LocalContext.current
    val baseCurrency = biz.currency

    var type by remember { mutableStateOf(existing?.type ?: initialType) }
    var date by remember { mutableStateOf(existing?.date ?: todayDateString()) }

    val currencyOptions = remember(baseCurrency) {
        (listOf(baseCurrency) + SUPPORTED_CURRENCIES).distinct()
    }
    var selectedCurrency by remember {
        mutableStateOf(
            if (existing != null && existing.originalCurrency.isNotBlank()) existing.originalCurrency
            else baseCurrency
        )
    }
    val isForeignCurrency = (type == EntryType.REVENUE || type == EntryType.EXPENSE) && selectedCurrency != baseCurrency

    var baseAmountText by remember {
        mutableStateOf(existing?.amount?.let { if (it > 0) "%.2f".format(Locale.US, it) else "" } ?: "")
    }
    var origAmountText by remember {
        mutableStateOf(
            if (existing != null && existing.originalAmount > 0.0) "%.2f".format(Locale.US, existing.originalAmount)
            else ""
        )
    }
    var rateText by remember {
        mutableStateOf(
            if (existing != null && existing.exchangeRate > 0.0 && existing.originalCurrency.isNotBlank() && existing.originalCurrency != baseCurrency)
                "%.4f".format(Locale.US, existing.exchangeRate).trimEnd('0').trimEnd('.')
            else ""
        )
    }

    var partnerId by remember { mutableStateOf(existing?.partnerId ?: state.partners.firstOrNull()?.id) }
    var categoryId by remember { mutableStateOf(existing?.categoryId) }
    var vendorOrSource by remember { mutableStateOf(existing?.vendorOrSource ?: "") }
    var paymentMethod by remember { mutableStateOf(existing?.paymentMethod ?: PAYMENT_METHODS.first()) }
    var paidByPartnerId by remember { mutableStateOf(existing?.paidByPartnerId) }
    var countAsInvestment by remember { mutableStateOf(existing?.countAsInvestment ?: true) }
    var receiptBase64 by remember { mutableStateOf(existing?.receiptImageBase64 ?: "") }
    var showFullReceipt by remember { mutableStateOf(false) }
    var note by remember { mutableStateOf(existing?.note ?: "") }
    var error by remember { mutableStateOf<String?>(null) }

    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            val encoded = ExportManager.compressUriToBase64(context, uri)
            if (encoded != null) receiptBase64 = encoded
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bmp ->
        if (bmp != null) {
            receiptBase64 = ExportManager.compressBitmapToBase64(bmp, maxDim = 900, quality = 80)
        }
    }

    val receiptBitmap = remember(receiptBase64) {
        if (receiptBase64.isNotBlank()) ExportManager.decodeBase64ToBitmap(receiptBase64) else null
    }

    val filteredCategories = remember(state.categories, type) {
        state.categories.filter { it.forType == type }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(if (existing == null) "Add Transaction" else "Edit Transaction")
                if (existing != null && onDelete != null) {
                    IconButton(onClick = { onDelete(existing) }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = BrandDanger)
                    }
                }
            }
        },
        text = {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 520.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(EntryType.values()) { t ->
                            FilterChip(
                                selected = type == t,
                                onClick = {
                                    type = t
                                    if (t == EntryType.INVESTMENT || t == EntryType.WITHDRAWAL) {
                                        selectedCurrency = baseCurrency
                                    }
                                },
                                label = { Text(t.label, fontSize = 12.sp) }
                            )
                        }
                    }
                }

                item {
                    DatePickerField(label = "Date", value = date, onValueChange = { date = it })
                }

                if (type == EntryType.REVENUE || type == EntryType.EXPENSE) {
                    item {
                        Text("Currency", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color.Gray)
                        Spacer(Modifier.height(4.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(currencyOptions) { cur ->
                                FilterChip(
                                    selected = selectedCurrency == cur,
                                    onClick = {
                                        selectedCurrency = cur
                                        if (cur == baseCurrency) {
                                            origAmountText = ""
                                            rateText = ""
                                        }
                                    },
                                    label = { Text(cur, fontSize = 12.sp) }
                                )
                            }
                        }
                    }
                }

                if (isForeignCurrency) {
                    item {
                        OutlinedTextField(
                            value = origAmountText,
                            onValueChange = { newVal ->
                                origAmountText = newVal
                                val o = newVal.toDoubleOrNull()
                                val r = rateText.toDoubleOrNull()
                                if (o != null && r != null && r > 0.0) {
                                    baseAmountText = "%.2f".format(Locale.US, o * r)
                                }
                            },
                            label = { Text("Amount in $selectedCurrency") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = rateText,
                                onValueChange = { newRate ->
                                    rateText = newRate
                                    val o = origAmountText.toDoubleOrNull()
                                    val r = newRate.toDoubleOrNull()
                                    if (o != null && r != null && r > 0.0) {
                                        baseAmountText = "%.2f".format(Locale.US, o * r)
                                    }
                                },
                                label = { Text("Rate (1 $selectedCurrency)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = baseAmountText,
                                onValueChange = { newBase ->
                                    baseAmountText = newBase
                                    val o = origAmountText.toDoubleOrNull()
                                    val b = newBase.toDoubleOrNull()
                                    if (o != null && o > 0.0 && b != null) {
                                        rateText = "%.4f".format(Locale.US, b / o).trimEnd('0').trimEnd('.')
                                    }
                                },
                                label = { Text("Final $baseCurrency") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true,
                                modifier = Modifier.weight(1.2f)
                            )
                        }
                    }
                } else {
                    item {
                        OutlinedTextField(
                            value = baseAmountText,
                            onValueChange = { baseAmountText = it },
                            label = { Text("Amount ($baseCurrency)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                if (type == EntryType.INVESTMENT || type == EntryType.WITHDRAWAL) {
                    item {
                        Text("Partner", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color.Gray)
                        Spacer(Modifier.height(4.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(state.partners) { p ->
                                FilterChip(
                                    selected = partnerId == p.id,
                                    onClick = { partnerId = p.id },
                                    label = { Text(p.name) }
                                )
                            }
                        }
                    }
                }

                if (type == EntryType.EXPENSE || type == EntryType.REVENUE) {
                    item {
                        Text(
                            if (type == EntryType.EXPENSE) "Expense Category" else "Revenue Category",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.Gray
                        )
                        Spacer(Modifier.height(4.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(filteredCategories) { cat ->
                                FilterChip(
                                    selected = categoryId == cat.id,
                                    onClick = { categoryId = cat.id },
                                    label = { Text(cat.name, fontSize = 12.sp) }
                                )
                            }
                        }
                    }

                    item {
                        OutlinedTextField(
                            value = vendorOrSource,
                            onValueChange = { vendorOrSource = it },
                            label = {
                                Text(if (type == EntryType.EXPENSE) "Vendor / Paid To" else "Network / Client / Source")
                            },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                if (type == EntryType.EXPENSE) {
                    item {
                        Text("Paid By", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color.Gray)
                        Spacer(Modifier.height(4.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            item {
                                FilterChip(
                                    selected = paidByPartnerId == null,
                                    onClick = { paidByPartnerId = null },
                                    label = { Text("Business Cash") }
                                )
                            }
                            items(state.partners) { p ->
                                FilterChip(
                                    selected = paidByPartnerId == p.id,
                                    onClick = { paidByPartnerId = p.id },
                                    label = { Text(p.name) }
                                )
                            }
                        }
                    }

                    if (paidByPartnerId != null) {
                        val payerName = state.partners.find { it.id == paidByPartnerId }?.name ?: "Partner"
                        item {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFF0FDF4)),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text(
                                        "How should $payerName's payment be recorded?",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(Modifier.height(6.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        FilterChip(
                                            selected = countAsInvestment,
                                            onClick = { countAsInvestment = true },
                                            label = { Text("Count as Investment", fontSize = 12.sp) }
                                        )
                                        FilterChip(
                                            selected = !countAsInvestment,
                                            onClick = { countAsInvestment = false },
                                            label = { Text("Reimburse Later", fontSize = 12.sp) }
                                        )
                                    }
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        text = if (countAsInvestment)
                                            "Logs this expense AND adds it to $payerName's Total Invested in one step."
                                        else
                                            "Business owes $payerName this amount back in Settle-Up.",
                                        fontSize = 11.sp,
                                        color = Color.DarkGray
                                    )
                                }
                            }
                        }
                    }
                }

                // Payment Method
                item {
                    Text("Payment Method", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color.Gray)
                    Spacer(Modifier.height(4.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(PAYMENT_METHODS) { m ->
                            FilterChip(
                                selected = paymentMethod == m,
                                onClick = { paymentMethod = m },
                                label = { Text(m, fontSize = 12.sp) }
                            )
                        }
                    }
                }

                // Receipt Photo Attachment (Camera / Gallery + Preview)
                item {
                    Text("Receipt / Proof Photo (Optional)", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color.Gray)
                    Spacer(Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = { galleryLauncher.launch("image/*") },
                            modifier = Modifier.weight(1f)
                        ) { Text("Gallery", fontSize = 12.sp) }
                        OutlinedButton(
                            onClick = { cameraLauncher.launch(null) },
                            modifier = Modifier.weight(1f)
                        ) { Text("Camera", fontSize = 12.sp) }
                    }
                    if (receiptBitmap != null) {
                        Spacer(Modifier.height(8.dp))
                        Image(
                            bitmap = receiptBitmap.asImageBitmap(),
                            contentDescription = "Receipt photo",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(130.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { showFullReceipt = true }
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            TextButton(onClick = { showFullReceipt = true }) { Text("View Full") }
                            TextButton(onClick = { receiptBase64 = "" }) {
                                Text("Remove", color = BrandDanger)
                            }
                        }
                    }
                }

                item {
                    OutlinedTextField(
                        value = note,
                        onValueChange = { note = it },
                        label = { Text("Note (Optional)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                if (error != null) {
                    item { Text(error!!, color = MaterialTheme.colorScheme.error, fontSize = 12.sp) }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                val finalBaseAmount = baseAmountText.toDoubleOrNull()
                val origAmt = origAmountText.toDoubleOrNull() ?: 0.0
                val rateVal = rateText.toDoubleOrNull() ?: if (origAmt > 0.0 && finalBaseAmount != null) finalBaseAmount / origAmt else 1.0

                if (finalBaseAmount == null || finalBaseAmount <= 0.0) {
                    error = if (isForeignCurrency)
                        "Enter the $selectedCurrency amount and either the exchange rate or final $baseCurrency amount."
                    else
                        "Please enter a valid amount."
                    return@Button
                }
                if ((type == EntryType.INVESTMENT || type == EntryType.WITHDRAWAL) && partnerId == null) {
                    error = "Please select a partner."
                    return@Button
                }

                val entry = (existing ?: LedgerEntry(type = type, amount = finalBaseAmount)).copy(
                    type = type,
                    date = date.ifBlank { todayDateString() },
                    amount = finalBaseAmount,
                    partnerId = if (type == EntryType.INVESTMENT || type == EntryType.WITHDRAWAL) partnerId else null,
                    categoryId = if (type == EntryType.EXPENSE || type == EntryType.REVENUE) categoryId else null,
                    vendorOrSource = vendorOrSource.trim(),
                    paymentMethod = paymentMethod,
                    paidByPartnerId = if (type == EntryType.EXPENSE) paidByPartnerId else null,
                    countAsInvestment = if (type == EntryType.EXPENSE && paidByPartnerId != null) countAsInvestment else false,
                    originalCurrency = if (isForeignCurrency) selectedCurrency else "",
                    originalAmount = if (isForeignCurrency) origAmt else 0.0,
                    exchangeRate = if (isForeignCurrency) rateVal else 1.0,
                    hasProof = receiptBase64.isNotBlank(),
                    receiptImageBase64 = receiptBase64,
                    note = note.trim()
                )
                onSave(entry)
            }) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )

    if (showFullReceipt && receiptBitmap != null) {
        AlertDialog(
            onDismissRequest = { showFullReceipt = false },
            title = { Text("Receipt Photo") },
            text = {
                Image(
                    bitmap = receiptBitmap.asImageBitmap(),
                    contentDescription = "Full receipt",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 420.dp)
                )
            },
            confirmButton = {
                TextButton(onClick = { showFullReceipt = false }) { Text("Close") }
            }
        )
    }
}
