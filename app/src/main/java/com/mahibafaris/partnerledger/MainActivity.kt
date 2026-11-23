package com.mahibafaris.partnerledger

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mahibafaris.partnerledger.data.LedgerRepository
import com.mahibafaris.partnerledger.model.*
import com.mahibafaris.partnerledger.ui.screens.*
import com.mahibafaris.partnerledger.ui.theme.PartnerLedgerTheme
import java.util.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val repo = LedgerRepository(this)
        setContent {
            var appState by remember { mutableStateOf(repo.loadState()) }
            PartnerLedgerTheme(themeMode = appState.settings.themeMode) {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    PartnerLedgerRoot(
                        repo = repo,
                        appState = appState,
                        onUpdateState = { newState ->
                            appState = newState
                            repo.saveState(newState)
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PartnerLedgerRoot(
    repo: LedgerRepository,
    appState: AppState,
    onUpdateState: (AppState) -> Unit
) {
    val context = LocalContext.current
    var unlocked by remember { mutableStateOf(appState.business?.pin.isNullOrBlank()) }
    var selectedTab by remember { mutableIntStateOf(0) } // 0=Home, 1=Tx, 2=Partners, 3=Reports, 4=More
    var moreSubScreen by remember { mutableStateOf("MENU") }

    var showAddSheet by remember { mutableStateOf(false) }
    var activeAddType by remember { mutableStateOf<EntryType?>(null) }
    var editingEntry by remember { mutableStateOf<LedgerEntry?>(null) }
    var detailEntryId by remember { mutableStateOf<String?>(null) }
    var detailPartnerId by remember { mutableStateOf<String?>(null) }

    val biz = appState.business
    if (biz == null) {
        SetupWizardScreen { config, partners, openingEntries, categories ->
            val logs = partners.map {
                AuditLogItem(UUID.randomUUID().toString(), "Partner", it.id, "ADD", "-", "Added ${it.name} (${it.agreedOwnershipPct}%)")
            } + openingEntries.map {
                AuditLogItem(UUID.randomUUID().toString(), "INVESTMENT", it.id, "ADD", "-", "Opening ${config.currency} ${it.amount}")
            }
            onUpdateState(
                appState.copy(
                    business = config,
                    partners = partners,
                    entries = openingEntries,
                    categories = categories,
                    auditLogs = logs
                )
            )
            unlocked = true
        }
        return
    }

    if (!unlocked && biz.pin.isNotBlank()) {
        PinLockScreen(expectedPin = biz.pin, businessName = biz.businessName) { unlocked = true }
        return
    }

    val metrics = remember(appState) { repo.computeMetrics(appState) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(biz.businessName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("Offline • ${biz.adminName} • ${biz.currency}", style = MaterialTheme.typography.labelSmall)
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(selected = selectedTab == 0, onClick = { selectedTab = 0; detailEntryId = null; detailPartnerId = null }, icon = { Icon(Icons.Default.Home, null) }, label = { Text("Home") })
                NavigationBarItem(selected = selectedTab == 1, onClick = { selectedTab = 1; detailEntryId = null; detailPartnerId = null }, icon = { Icon(Icons.Default.ReceiptLong, null) }, label = { Text("Ledger") })
                NavigationBarItem(selected = selectedTab == 2, onClick = { selectedTab = 2; detailEntryId = null; detailPartnerId = null }, icon = { Icon(Icons.Default.People, null) }, label = { Text("Partners") })
                NavigationBarItem(selected = selectedTab == 3, onClick = { selectedTab = 3; detailEntryId = null; detailPartnerId = null }, icon = { Icon(Icons.Default.Assessment, null) }, label = { Text("Reports") })
                NavigationBarItem(selected = selectedTab == 4, onClick = { selectedTab = 4; moreSubScreen = "MENU"; detailEntryId = null; detailPartnerId = null }, icon = { Icon(Icons.Default.MoreHoriz, null) }, label = { Text("More") })
            }
        },
        floatingActionButton = {
            if ((selectedTab == 0 || selectedTab == 1) && detailEntryId == null && detailPartnerId == null) {
                ExtendedFloatingActionButton(
                    onClick = { showAddSheet = true },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.White,
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    text = { Text("Add") }
                )
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            val detailEntry = appState.entries.firstOrNull { it.id == detailEntryId }
            val detailPartner = metrics.partnerSummaries.firstOrNull { it.partner.id == detailPartnerId }

            when {
                detailEntry != null -> {
                    TransactionDetailScreen(
                        biz = biz,
                        state = appState,
                        entry = detailEntry,
                        onBack = { detailEntryId = null },
                        onEdit = {
                            editingEntry = it
                            activeAddType = it.type
                        },
                        onSoftDelete = { target ->
                            val updatedEntries = appState.entries.map {
                                if (it.id == target.id) it.copy(isDeleted = true, updatedAt = System.currentTimeMillis()) else it
                            }
                            val log = AuditLogItem(
                                UUID.randomUUID().toString(), target.type.name, target.id, "DELETE",
                                "${target.type.label} ${biz.currency} ${target.amount} (${target.date})", "SOFT_DELETED"
                            )
                            onUpdateState(appState.copy(entries = updatedEntries, auditLogs = listOf(log) + appState.auditLogs))
                            detailEntryId = null
                            Toast.makeText(context, "Record archived & logged", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
                detailPartner != null -> {
                    PartnerDetailScreen(biz = biz, state = appState, ps = detailPartner, onBack = { detailPartnerId = null })
                }
                selectedTab == 0 -> {
                    HomeDashboardScreen(
                        biz = biz,
                        state = appState,
                        metrics = metrics,
                        onQuickAdd = { showAddSheet = true },
                        onOpenReconcile = { moreSubScreen = "RECONCILE"; selectedTab = 4 },
                        onOpenStatements = { selectedTab = 3 },
                        onOpenBackup = { moreSubScreen = "BACKUP"; selectedTab = 4 },
                        onOpenAllTransactions = { selectedTab = 1 },
                        onOpenPartner = { detailPartnerId = it },
                        onOpenEntry = { detailEntryId = it.id }
                    )
                }
                selectedTab == 1 -> {
                    TransactionsListScreen(biz = biz, state = appState, onOpenEntry = { detailEntryId = it.id })
                }
                selectedTab == 2 -> {
                    PartnersListScreen(
                        biz = biz,
                        metrics = metrics,
                        onSelectPartner = { detailPartnerId = it },
                        onAddPartner = { partner, opening ->
                            val newEntries = if (opening > 0.0) {
                                listOf(LedgerEntry(UUID.randomUUID().toString(), EntryType.INVESTMENT, opening, biz.startDate, partner.id, note = "Opening investment")) + appState.entries
                            } else appState.entries
                            val log = AuditLogItem(UUID.randomUUID().toString(), "Partner", partner.id, "ADD", "-", "Added ${partner.name}")
                            onUpdateState(appState.copy(partners = appState.partners + partner, entries = newEntries, auditLogs = listOf(log) + appState.auditLogs))
                        }
                    )
                }
                selectedTab == 3 -> {
                    ReportsHubScreen(biz = biz, state = appState, metrics = metrics, repo = repo, onStateUpdated = onUpdateState)
                }
                selectedTab == 4 -> {
                    MoreHubScreen(biz = biz, state = appState, metrics = metrics, repo = repo, initialSubScreen = moreSubScreen, onStateUpdated = onUpdateState)
                }
            }
        }
    }

    if (showAddSheet) {
        ModalBottomSheet(onDismissRequest = { showAddSheet = false }) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Add Record", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                EntryType.entries.forEach { t ->
                    ListItem(
                        headlineContent = { Text(t.label, fontWeight = FontWeight.SemiBold) },
                        leadingContent = {
                            Icon(
                                when (t) {
                                    EntryType.INVESTMENT -> Icons.Default.AddCircle
                                    EntryType.EXPENSE -> Icons.Default.Payments
                                    EntryType.REVENUE -> Icons.Default.TrendingUp
                                    EntryType.WITHDRAWAL -> Icons.Default.AccountBalance
                                },
                                contentDescription = null
                            )
                        },
                        modifier = Modifier.clickable {
                            showAddSheet = false
                            editingEntry = null
                            activeAddType = t
                        }
                    )
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }

    if (activeAddType != null) {
        AddEditTransactionDialog(
            biz = biz,
            state = appState,
            metrics = metrics,
            initialType = activeAddType!!,
            existing = editingEntry,
            onDismiss = { activeAddType = null; editingEntry = null },
            onSave = { saved, addAnother ->
                val updatedSettings = appState.settings.copy(
                    lastUsedPartnerId = saved.partnerId ?: appState.settings.lastUsedPartnerId,
                    lastUsedCategoryId = saved.categoryId ?: appState.settings.lastUsedCategoryId
                )
                if (editingEntry == null) {
                    val log = AuditLogItem(UUID.randomUUID().toString(), saved.type.name, saved.id, "ADD", "-", "${saved.type.label} ${biz.currency} ${saved.amount} (${saved.date})")
                    onUpdateState(appState.copy(settings = updatedSettings, entries = listOf(saved) + appState.entries, auditLogs = listOf(log) + appState.auditLogs))
                } else {
                    val old = editingEntry!!
                    val updated = appState.entries.map { if (it.id == saved.id) saved else it }
                    val log = AuditLogItem(UUID.randomUUID().toString(), saved.type.name, saved.id, "EDIT", "${old.amount} (${old.date})", "${saved.amount} (${saved.date})")
                    onUpdateState(appState.copy(settings = updatedSettings, entries = updated, auditLogs = listOf(log) + appState.auditLogs))
                }
                Toast.makeText(context, "Saved", Toast.LENGTH_SHORT).show()
                if (!addAnother) {
                    activeAddType = null
                    editingEntry = null
                } else {
                    editingEntry = null
                }
            }
        )
    }
}
