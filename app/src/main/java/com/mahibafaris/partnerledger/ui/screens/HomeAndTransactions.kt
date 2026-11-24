package com.mahibafaris.partnerledger.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.mahibafaris.partnerledger.data.LedgerCalculator
import com.mahibafaris.partnerledger.export.ExportManager
import com.mahibafaris.partnerledger.model.*
import com.mahibafaris.partnerledger.ui.theme.*
import java.util.Locale

@Composable
fun HomeDashboardScreen(
    state: AppState,
    onAddClick: (EntryType) -> Unit,
    onEntryClick: (LedgerEntry) -> Unit,
    onViewAllLedger: () -> Unit,
    onOpenProfile: () -> Unit
) {
    val biz = state.business ?: return
    val totals = remember(state) { LedgerCalculator.compute(state) }
    val recent = remember(state) {
        state.entries.filter { !it.isDeleted }.sortedByDescending { it.date + it.createdAt }.take(8)
    }
    val pMap = remember(state.partners) { state.partners.associateBy { it.id } }
    val cMap = remember(state.categories) { state.categories.associateBy { it.id } }
    val logoBitmap = remember(biz.logoBase64) {
        if (biz.logoBase64.isNotBlank()) ExportManager.decodeBase64ToBitmap(biz.logoBase64) else null
    }
    val bizInitials = remember(biz.businessName) {
        biz.businessName.trim().split(" ").filter { it.isNotBlank() }.take(2)
            .joinToString("") { it.take(1).uppercase(Locale.US) }.ifBlank { "PL" }
    }

    val animBalance by animateFloatAsState(
        targetValue = totals.currentBalance.toFloat(),
        animationSpec = tween(650, easing = FastOutSlowInEasing),
        label = "animBal"
    )
    val animInvested by animateFloatAsState(totals.totalInvested.toFloat(), tween(600), label = "animInv")
    val animRevenue by animateFloatAsState(totals.totalRevenue.toFloat(), tween(600), label = "animRev")
    val animExpenses by animateFloatAsState(totals.totalExpenses.toFloat(), tween(600), label = "animExp")
    val animWithdrawn by animateFloatAsState(totals.totalWithdrawn.toFloat(), tween(600), label = "animWd")

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(JetBlack)
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 14.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Header Row with Tappable Business Logo Avatar -> Profile
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f).clickable { onOpenProfile() }
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(HeroGradientBrush)
                            .border(1.5.dp, CyanElectric.copy(alpha = 0.55f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        if (logoBitmap != null) {
                            Image(
                                bitmap = logoBitmap.asImageBitmap(),
                                contentDescription = "Business Profile",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Text(bizInitials, color = CyanElectric, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
                        }
                    }
                    Column {
                        Text(biz.businessName, fontSize = 21.sp, fontWeight = FontWeight.ExtraBold, color = Color.White, maxLines = 1)
                        Text("Tap to edit profile", fontSize = 11.sp, color = LavenderMuted)
                    }
                }
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(AmethystElevated)
                        .border(1.dp, ElevatedBorderColor, RoundedCornerShape(50))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(Modifier.size(7.dp).clip(CircleShape).background(CyanElectric))
                    Text("Base: ${biz.currency}", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                }
            }
        }

        // Hero Gradient Card
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(HeroGradientBrush)
                    .border(1.dp, CyanElectric.copy(alpha = 0.34f), RoundedCornerShape(24.dp))
                    .padding(20.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Business Cash Balance", color = LavenderMuted, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(JetBlack.copy(alpha = 0.45f))
                                .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(50))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text("LIVE", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = CyanElectric)
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        formatMoney(biz.currency, animBalance.toDouble()),
                        color = Color.White,
                        fontSize = 30.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Spacer(Modifier.height(14.dp))
                    val profitColor = if (totals.netProfit >= 0) EmeraldSoft else CoralSoft
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(JetBlack.copy(alpha = 0.45f))
                            .border(1.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 12.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(Modifier.size(8.dp).clip(CircleShape).background(profitColor))
                        Text("Net Profit / Loss:", fontSize = 11.sp, color = LavenderMuted)
                        Text(
                            formatMoney(biz.currency, totals.netProfit),
                            color = profitColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }

        // 2x2 KPI Grid
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatCard("Total Invested", formatMoney(biz.currency, animInvested.toDouble()), EmeraldSoft, "↗", Modifier.weight(1f))
                StatCard("Total Revenue", formatMoney(biz.currency, animRevenue.toDouble()), EmeraldSoft, "$", Modifier.weight(1f))
            }
            Spacer(Modifier.height(10.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatCard("Total Expenses", formatMoney(biz.currency, animExpenses.toDouble()), CoralSoft, "↘", Modifier.weight(1f))
                StatCard("Total Withdrawn", formatMoney(biz.currency, animWithdrawn.toDouble()), AmberSoft, "⇄", Modifier.weight(1f))
            }
        }

        // Quick Add Chips
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Quick Add", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.White)
                Text("Tap to log", fontSize = 11.sp, color = LavenderMuted)
            }
            Spacer(Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(EntryType.values()) { t ->
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(AmethystElevated)
                            .border(1.dp, ElevatedBorderColor, RoundedCornerShape(50))
                            .clickable { onAddClick(t) }
                            .padding(horizontal = 14.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Text("+", color = CyanElectric, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
                        Text(t.label, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                    }
                }
            }
        }

        // Recent Activity
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Recent Activity", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.White)
                Text(
                    "View All",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = CyanElectric,
                    modifier = Modifier.clickable { onViewAllLedger() }
                )
            }
        }

        if (recent.isEmpty()) {
            item {
                FintechCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 18.dp) {
                    Text(
                        "No transactions yet. Tap a Quick Add button above to log your first entry.",
                        modifier = Modifier.padding(18.dp),
                        color = LavenderMuted,
                        fontSize = 13.sp
                    )
                }
            }
        } else {
            items(recent, key = { it.id }) { entry ->
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
fun StatCard(label: String, value: String, accent: Color, badge: String, modifier: Modifier = Modifier) {
    FintechCard(modifier = modifier, cornerRadius = 18.dp) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(label, fontSize = 11.sp, color = LavenderMuted, fontWeight = FontWeight.Medium)
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(accent.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(badge, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = accent)
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(value, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = accent, maxLines = 1)
        }
    }
}

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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(JetBlack)
            .imePadding()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(Modifier.height(14.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Ledger", fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
            Button(
                onClick = onAddClick,
                colors = ButtonDefaults.buttonColors(containerColor = CyanElectric, contentColor = JetBlack),
                shape = RoundedCornerShape(50),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Add", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }

        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = search,
            onValueChange = { search = it },
            placeholder = { Text("Search note, vendor, partner, date...", fontSize = 13.sp) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = LavenderMuted) },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            colors = darkFieldColors(),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(10.dp))

        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                FintechChip(label = "All", selected = filterType == null, onClick = { filterType = null })
            }
            items(EntryType.values()) { t ->
                FintechChip(
                    label = t.label,
                    selected = filterType == t,
                    onClick = { filterType = if (filterType == t) null else t }
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        if (filtered.isEmpty()) {
            FintechCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 18.dp) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("No matching entries", fontWeight = FontWeight.Bold, color = Color.White)
                    Spacer(Modifier.height(4.dp))
                    Text("Try adjusting your search or filter chips.", fontSize = 12.sp, color = LavenderMuted)
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                items(filtered, key = { it.id }) { entry ->
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
    val isPositive = entry.type == EntryType.INVESTMENT || entry.type == EntryType.REVENUE
    val color = when (entry.type) {
        EntryType.INVESTMENT, EntryType.REVENUE -> EmeraldSoft
        EntryType.EXPENSE -> CoralSoft
        EntryType.WITHDRAWAL -> AmberSoft
    }
    val prefix = if (isPositive) "+" else "-"
    val arrow = if (isPositive) "↓" else "↑"
    val origText = formatEntryOriginal(entry, baseCurrency)

    FintechCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 18.dp, onClick = onClick) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(color.copy(alpha = 0.14f))
                    .border(1.dp, color.copy(alpha = 0.30f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(arrow, color = color, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = buildString {
                            append(entry.type.label)
                            if (!categoryName.isNullOrBlank()) append(" • $categoryName")
                            else if (!partnerName.isNullOrBlank()) append(" • $partnerName")
                        },
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = Color.White
                    )
                    if (entry.receiptImageBase64.isNotBlank()) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(CyanElectric.copy(alpha = 0.15f))
                                .border(1.dp, CyanElectric.copy(alpha = 0.35f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 5.dp, vertical = 2.dp)
                        ) {
                            Text("[Receipt]", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = CyanElectric)
                        }
                    }
                }
                if (entry.vendorOrSource.isNotBlank()) {
                    Text(entry.vendorOrSource, fontSize = 12.sp, color = Color.White.copy(alpha = 0.8f))
                }
                Spacer(Modifier.height(3.dp))
                Text("${entry.date} • ${entry.paymentMethod}", fontSize = 11.sp, color = LavenderMuted)
                if (entry.note.isNotBlank()) {
                    Spacer(Modifier.height(3.dp))
                    Text(entry.note, fontSize = 11.sp, color = LavenderMuted)
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "$prefix${formatMoney(baseCurrency, entry.amount)}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = color
                )
                if (origText != null) {
                    Text(text = "($origText)", fontSize = 10.sp, color = LavenderMuted)
                }
                if (entry.type == EntryType.EXPENSE && paidByName != null) {
                    Spacer(Modifier.height(3.dp))
                    val modeTag = if (entry.countAsInvestment) "Investment" else "Reimburse"
                    Text(
                        text = "Paid by $paidByName ($modeTag)",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (entry.countAsInvestment) EmeraldSoft else AmberSoft
                    )
                }
            }
        }
    }
}

/**
 * Receipt View Dialog shown when tapping any entry on Ledger or Home.
 * Displays full breakdown + attached proof image + Share (WhatsApp PNG) + Edit buttons.
 */
@Composable
fun EntryReceiptDialog(
    state: AppState,
    entry: LedgerEntry,
    onDismiss: () -> Unit,
    onEdit: () -> Unit
) {
    val biz = state.business ?: return
    val context = LocalContext.current
    val partnerName = state.partners.find { it.id == entry.partnerId }?.name
    val paidByName = state.partners.find { it.id == entry.paidByPartnerId }?.name
    val categoryName = state.categories.find { it.id == entry.categoryId }?.name
    val proofBitmap = remember(entry.receiptImageBase64) {
        if (entry.receiptImageBase64.isNotBlank()) ExportManager.decodeBase64ToBitmap(entry.receiptImageBase64) else null
    }
    var showFullPhoto by remember { mutableStateOf(false) }

    val isPositive = entry.type == EntryType.INVESTMENT || entry.type == EntryType.REVENUE
    val amtColor = when (entry.type) {
        EntryType.INVESTMENT, EntryType.REVENUE -> EmeraldSoft
        EntryType.EXPENSE -> CoralSoft
        EntryType.WITHDRAWAL -> AmberSoft
    }
    val sign = if (isPositive) "+" else "-"

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.76f))
                .padding(horizontal = 16.dp, vertical = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth().heightIn(max = 680.dp),
                shape = RoundedCornerShape(26.dp),
                color = AmethystDeep,
                border = BorderStroke(1.dp, CyanElectric.copy(alpha = 0.35f))
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Top Receipt Banner
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(HeroGradientBrush)
                            .padding(horizontal = 18.dp, vertical = 16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Column {
                                Text("TRANSACTION RECEIPT", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = CyanElectric)
                                Spacer(Modifier.height(4.dp))
                                Text(biz.businessName, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                                Text("Ref #${entry.id.take(8).uppercase(Locale.US)}", fontSize = 11.sp, color = LavenderMuted)
                            }
                            IconButton(onClick = onDismiss) {
                                Icon(Icons.Default.Close, contentDescription = "Close", tint = LavenderMuted)
                            }
                        }
                    }

                    // Scrollable Receipt Body
                    Column(
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .verticalScroll(rememberScrollState())
                            .padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "$sign${formatMoney(biz.currency, entry.amount)}",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = amtColor
                        )
                        formatEntryOriginal(entry, biz.currency)?.let { orig ->
                            val rateClean = "%.4f".format(Locale.US, entry.exchangeRate).trimEnd('0').trimEnd('.')
                            Text("Original: $orig (Rate: $rateClean)", fontSize = 12.sp, color = LavenderMuted)
                        }

                        ThinDivider()

                        ReceiptDetailRow("Date", entry.date)
                        ReceiptDetailRow("Type", entry.type.label)
                        if (!categoryName.isNullOrBlank()) ReceiptDetailRow("Category", categoryName)
                        if (!partnerName.isNullOrBlank()) ReceiptDetailRow("Partner", partnerName)
                        if (entry.vendorOrSource.isNotBlank()) {
                            ReceiptDetailRow(
                                if (entry.type == EntryType.EXPENSE) "Vendor / Paid To" else "Source / Client",
                                entry.vendorOrSource
                            )
                        }
                        if (entry.type == EntryType.EXPENSE) {
                            val paidTxt = if (paidByName == null) "Business Cash"
                            else "$paidByName (${if (entry.countAsInvestment) "As Investment" else "Reimburse Later"})"
                            ReceiptDetailRow("Paid By", paidTxt)
                        }
                        ReceiptDetailRow("Payment Method", entry.paymentMethod)
                        if (entry.note.isNotBlank()) ReceiptDetailRow("Note", entry.note)

                        if (proofBitmap != null) {
                            ThinDivider()
                            Text("Attached Proof Photo", fontSize = 12.sp, color = LavenderMuted)
                            Image(
                                bitmap = proofBitmap.asImageBitmap(),
                                contentDescription = "Attached receipt photo",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(170.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .clickable { showFullPhoto = true }
                            )
                        }
                    }

                    ThinDivider()

                    // Bottom Action Bar: Share WhatsApp Image + Edit
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(AmethystElevated)
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                ExportManager.shareEntryReceiptImage(
                                    context = context,
                                    biz = biz,
                                    entry = entry,
                                    partnerName = partnerName,
                                    paidByName = paidByName,
                                    categoryName = categoryName
                                )
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = CyanElectric, contentColor = JetBlack),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Share Image", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                        OutlinedButton(
                            onClick = onEdit,
                            border = BorderStroke(1.dp, CyanElectric.copy(alpha = 0.6f)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = CyanElectric),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Edit", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }

    if (showFullPhoto && proofBitmap != null) {
        AlertDialog(
            onDismissRequest = { showFullPhoto = false },
            containerColor = AmethystElevated,
            title = { Text("Receipt Photo", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Image(
                    bitmap = proofBitmap.asImageBitmap(),
                    contentDescription = "Full receipt",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxWidth().heightIn(max = 420.dp)
                )
            },
            confirmButton = {
                TextButton(onClick = { showFullPhoto = false }) { Text("Close", color = CyanElectric) }
            }
        )
    }
}

@Composable
private fun ReceiptDetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Text(label, fontSize = 12.sp, color = LavenderMuted)
        Spacer(Modifier.width(12.dp))
        Text(value, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
    }
}

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

    // Uses user's prioritized currency list!
    val currencyOptions = remember(state.currencies, baseCurrency) {
        val list = if (state.currencies.isNotEmpty()) state.currencies else SUPPORTED_CURRENCIES
        if (list.contains(baseCurrency)) list else listOf(baseCurrency) + list
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

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.72f))
                .imePadding()
                .padding(horizontal = 14.dp, vertical = 20.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.90f),
                shape = RoundedCornerShape(26.dp),
                color = AmethystElevated,
                border = BorderStroke(1.dp, ElevatedBorderColor)
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp, vertical = 14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                if (existing == null) "Add Transaction" else "Edit Transaction",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Row {
                                if (existing != null && onDelete != null) {
                                    IconButton(onClick = { onDelete(existing) }) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = CoralSoft)
                                    }
                                }
                                IconButton(onClick = onDismiss) {
                                    Icon(Icons.Default.Close, contentDescription = "Close", tint = LavenderMuted)
                                }
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(EntryType.values()) { t ->
                                FintechChip(
                                    label = t.label,
                                    selected = type == t,
                                    onClick = {
                                        type = t
                                        if (t == EntryType.INVESTMENT || t == EntryType.WITHDRAWAL) {
                                            selectedCurrency = baseCurrency
                                        }
                                    }
                                )
                            }
                        }
                    }

                    ThinDivider()

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                            .padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        DatePickerField(label = "Date", value = date, onValueChange = { date = it })

                        AnimatedVisibility(
                            visible = type == EntryType.REVENUE || type == EntryType.EXPENSE,
                            enter = expandVertically() + fadeIn(),
                            exit = shrinkVertically() + fadeOut()
                        ) {
                            Column {
                                Text("Currency", fontSize = 12.sp, color = LavenderMuted)
                                Spacer(Modifier.height(6.dp))
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    items(currencyOptions) { cur ->
                                        FintechChip(
                                            label = cur,
                                            selected = selectedCurrency == cur,
                                            onClick = {
                                                selectedCurrency = cur
                                                if (cur == baseCurrency) {
                                                    origAmountText = ""
                                                    rateText = ""
                                                }
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        if (isForeignCurrency) {
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
                                shape = RoundedCornerShape(16.dp),
                                colors = darkFieldColors(),
                                modifier = Modifier.fillMaxWidth()
                            )
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
                                    shape = RoundedCornerShape(16.dp),
                                    colors = darkFieldColors(),
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
                                    shape = RoundedCornerShape(16.dp),
                                    colors = darkFieldColors(),
                                    modifier = Modifier.weight(1.2f)
                                )
                            }
                        } else {
                            OutlinedTextField(
                                value = baseAmountText,
                                onValueChange = { baseAmountText = it },
                                label = { Text("Amount ($baseCurrency)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true,
                                shape = RoundedCornerShape(16.dp),
                                colors = darkFieldColors(),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        if (type == EntryType.INVESTMENT || type == EntryType.WITHDRAWAL) {
                            Column {
                                Text("Partner", fontSize = 12.sp, color = LavenderMuted)
                                Spacer(Modifier.height(6.dp))
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    items(state.partners) { p ->
                                        FintechChip(label = p.name, selected = partnerId == p.id, onClick = { partnerId = p.id })
                                    }
                                }
                            }
                        }

                        if (type == EntryType.EXPENSE || type == EntryType.REVENUE) {
                            Column {
                                Text(
                                    if (type == EntryType.EXPENSE) "Expense Category" else "Revenue Category",
                                    fontSize = 12.sp,
                                    color = LavenderMuted
                                )
                                Spacer(Modifier.height(6.dp))
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    items(filteredCategories) { cat ->
                                        FintechChip(label = cat.name, selected = categoryId == cat.id, onClick = { categoryId = cat.id })
                                    }
                                }
                            }

                            OutlinedTextField(
                                value = vendorOrSource,
                                onValueChange = { vendorOrSource = it },
                                label = { Text(if (type == EntryType.EXPENSE) "Vendor / Paid To" else "Network / Client / Source") },
                                singleLine = true,
                                shape = RoundedCornerShape(16.dp),
                                colors = darkFieldColors(),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        if (type == EntryType.EXPENSE) {
                            Column {
                                Text("Paid By", fontSize = 12.sp, color = LavenderMuted)
                                Spacer(Modifier.height(6.dp))
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    item {
                                        FintechChip(label = "Business Cash", selected = paidByPartnerId == null, onClick = { paidByPartnerId = null })
                                    }
                                    items(state.partners) { p ->
                                        FintechChip(label = p.name, selected = paidByPartnerId == p.id, onClick = { paidByPartnerId = p.id })
                                    }
                                }
                            }

                            AnimatedVisibility(
                                visible = paidByPartnerId != null,
                                enter = expandVertically() + fadeIn(),
                                exit = shrinkVertically() + fadeOut()
                            ) {
                                val payerName = state.partners.find { it.id == paidByPartnerId }?.name ?: "Partner"
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(AmethystRoyal.copy(alpha = 0.55f))
                                        .border(1.dp, CyanElectric.copy(alpha = 0.45f), RoundedCornerShape(16.dp))
                                        .padding(14.dp)
                                ) {
                                    Column {
                                        Text(
                                            "How should $payerName's payment be recorded?",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                        Spacer(Modifier.height(8.dp))
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            FintechChip(
                                                label = "Count as Investment",
                                                selected = countAsInvestment,
                                                onClick = { countAsInvestment = true },
                                                modifier = Modifier.weight(1f)
                                            )
                                            FintechChip(
                                                label = "Reimburse Later",
                                                selected = !countAsInvestment,
                                                onClick = { countAsInvestment = false },
                                                modifier = Modifier.weight(1f)
                                            )
                                        }
                                        Spacer(Modifier.height(6.dp))
                                        Text(
                                            text = if (countAsInvestment)
                                                "Logs this expense AND adds it to $payerName's Total Invested in one step."
                                            else
                                                "Logs this expense and tracks a reimbursement owed to $payerName.",
                                            fontSize = 11.sp,
                                            color = LavenderMuted
                                        )
                                    }
                                }
                            }
                        }

                        Column {
                            Text("Payment Method", fontSize = 12.sp, color = LavenderMuted)
                            Spacer(Modifier.height(6.dp))
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(PAYMENT_METHODS) { m ->
                                    FintechChip(label = m, selected = paymentMethod == m, onClick = { paymentMethod = m })
                                }
                            }
                        }

                        Column {
                            Text("Receipt / Proof Photo (Optional)", fontSize = 12.sp, color = LavenderMuted)
                            Spacer(Modifier.height(6.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(
                                    onClick = { galleryLauncher.launch("image/*") },
                                    border = BorderStroke(1.dp, AmethystGlow.copy(alpha = 0.35f)),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.weight(1f)
                                ) { Text("Gallery", fontSize = 12.sp) }
                                OutlinedButton(
                                    onClick = { cameraLauncher.launch(null) },
                                    border = BorderStroke(1.dp, AmethystGlow.copy(alpha = 0.35f)),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.weight(1f)
                                ) { Text("Camera", fontSize = 12.sp) }
                            }
                            AnimatedVisibility(visible = receiptBitmap != null) {
                                Column {
                                    Spacer(Modifier.height(8.dp))
                                    receiptBitmap?.let { bmp ->
                                        Image(
                                            bitmap = bmp.asImageBitmap(),
                                            contentDescription = "Receipt photo",
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(130.dp)
                                                .clip(RoundedCornerShape(12.dp))
                                                .clickable { showFullReceipt = true }
                                        )
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        TextButton(onClick = { showFullReceipt = true }) { Text("View Full", color = CyanElectric) }
                                        TextButton(onClick = { receiptBase64 = "" }) { Text("Remove", color = CoralSoft) }
                                    }
                                }
                            }
                        }

                        OutlinedTextField(
                            value = note,
                            onValueChange = { note = it },
                            label = { Text("Note (Optional)") },
                            shape = RoundedCornerShape(16.dp),
                            colors = darkFieldColors(),
                            modifier = Modifier.fillMaxWidth()
                        )

                        if (error != null) {
                            Text(error!!, color = CoralSoft, fontSize = 12.sp)
                        }
                    }

                    ThinDivider()

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(AmethystDeep)
                            .padding(horizontal = 18.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = onDismiss) { Text("Cancel", color = LavenderMuted) }
                        Spacer(Modifier.width(8.dp))
                        Button(
                            onClick = {
                                val finalBaseAmount = baseAmountText.toDoubleOrNull()
                                val origAmt = origAmountText.toDoubleOrNull() ?: 0.0
                                val rateVal = rateText.toDoubleOrNull()
                                    ?: if (origAmt > 0.0 && finalBaseAmount != null) finalBaseAmount / origAmt else 1.0

                                if (finalBaseAmount == null || finalBaseAmount <= 0.0) {
                                    error = if (isForeignCurrency)
                                        "Enter the $selectedCurrency amount and either rate or final $baseCurrency."
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
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = CyanElectric, contentColor = JetBlack),
                            shape = RoundedCornerShape(14.dp)
                        ) { Text("Save", fontWeight = FontWeight.Bold) }
                    }
                }
            }
        }
    }

    if (showFullReceipt && receiptBitmap != null) {
        AlertDialog(
            onDismissRequest = { showFullReceipt = false },
            containerColor = AmethystElevated,
            title = { Text("Receipt Photo", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Image(
                    bitmap = receiptBitmap.asImageBitmap(),
                    contentDescription = "Full receipt",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxWidth().heightIn(max = 420.dp)
                )
            },
            confirmButton = {
                TextButton(onClick = { showFullReceipt = false }) { Text("Close", color = CyanElectric) }
            }
        )
    }
}
