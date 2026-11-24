package com.mahibafaris.partnerledger

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mahibafaris.partnerledger.data.LedgerCalculator
import com.mahibafaris.partnerledger.data.LedgerRepository
import com.mahibafaris.partnerledger.model.*
import com.mahibafaris.partnerledger.ui.screens.*
import com.mahibafaris.partnerledger.ui.theme.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = android.graphics.Color.parseColor("#050505")
        window.navigationBarColor = android.graphics.Color.parseColor("#0C0514")
        val repo = LedgerRepository(applicationContext)
        setContent {
            PartnerLedgerTheme {
                PartnerLedgerApp(repo)
            }
        }
    }
}

@Composable
fun PartnerLedgerApp(repo: LedgerRepository) {
    var state by remember { mutableStateOf(repo.load()) }
    var unlocked by remember { mutableStateOf(state.business?.pin.isNullOrBlank()) }
    var selectedTab by remember { mutableStateOf(0) }
    var pendingMoreSubScreen by remember { mutableStateOf("MENU") }

    // Receipt viewer vs Edit dialog states
    var viewingReceiptEntry by remember { mutableStateOf<LedgerEntry?>(null) }
    var showEntryDialog by remember { mutableStateOf(false) }
    var dialogInitialType by remember { mutableStateOf(EntryType.EXPENSE) }
    var editingEntry by remember { mutableStateOf<LedgerEntry?>(null) }

    fun updateState(newState: AppState) {
        state = newState
        repo.save(newState)
    }

    val biz = state.business
    if (biz == null) {
        WelcomeAndSetupFlow(
            repo = repo,
            onSetupComplete = { newState ->
                updateState(newState)
                unlocked = true
            },
            onRestoreComplete = { restoredState ->
                updateState(restoredState)
                unlocked = restoredState.business?.pin.isNullOrBlank()
            }
        )
        return
    }

    if (!unlocked && biz.pin.isNotBlank()) {
        PinLockScreen(
            expectedPin = biz.pin,
            businessName = biz.businessName,
            biometricEnabled = biz.biometricEnabled,
            onUnlocked = { unlocked = true }
        )
        return
    }

    val navItemColors = NavigationBarItemDefaults.colors(
        selectedIconColor = CyanElectric,
        selectedTextColor = CyanElectric,
        indicatorColor = CyanElectric.copy(alpha = 0.16f),
        unselectedIconColor = LavenderMuted,
        unselectedTextColor = LavenderMuted
    )

    Scaffold(
        containerColor = JetBlack,
        bottomBar = {
            NavigationBar(
                containerColor = NavBarBg,
                tonalElevation = 0.dp,
                modifier = Modifier.border(0.5.dp, AmethystGlow.copy(alpha = 0.25f))
            ) {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    colors = navItemColors,
                    icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                    label = { Text("Home") }
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    colors = navItemColors,
                    icon = { Icon(Icons.Default.List, contentDescription = "Ledger") },
                    label = { Text("Ledger") }
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    colors = navItemColors,
                    icon = { Icon(Icons.Default.Person, contentDescription = "Partners") },
                    label = { Text("Partners") }
                )
                NavigationBarItem(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    colors = navItemColors,
                    icon = { Icon(Icons.Default.Info, contentDescription = "Reports") },
                    label = { Text("Reports") }
                )
                NavigationBarItem(
                    selected = selectedTab == 4,
                    onClick = { selectedTab = 4 },
                    colors = navItemColors,
                    icon = { Icon(Icons.Default.MoreVert, contentDescription = "More") },
                    label = { Text("More") }
                )
            }
        }
    ) { pad ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(JetBlack)
                .padding(pad)
        ) {
            AnimatedContent(
                targetState = selectedTab,
                transitionSpec = {
                    val dir = if (targetState > initialState) 1 else -1
                    (fadeIn(tween(220)) + slideInHorizontally(tween(220)) { dir * (it / 10) }) togetherWith
                        (fadeOut(tween(160)) + slideOutHorizontally(tween(160)) { -dir * (it / 10) })
                },
                label = "mainTabAnim"
            ) { tab ->
                when (tab) {
                    0 -> HomeDashboardScreen(
                        state = state,
                        onAddClick = { t ->
                            dialogInitialType = t
                            editingEntry = null
                            showEntryDialog = true
                        },
                        onEntryClick = { e -> viewingReceiptEntry = e },
                        onViewAllLedger = { selectedTab = 1 },
                        onOpenProfile = {
                            pendingMoreSubScreen = "PROFILE"
                            selectedTab = 4
                        }
                    )
                    1 -> TransactionsListScreen(
                        state = state,
                        onAddClick = {
                            dialogInitialType = EntryType.EXPENSE
                            editingEntry = null
                            showEntryDialog = true
                        },
                        onEntryClick = { e -> viewingReceiptEntry = e }
                    )
                    2 -> PartnersScreen(
                        state = state,
                        onAddPartner = { p ->
                            val log = AuditLog(action = "CREATE", recordType = "PARTNER", recordId = p.id, newValue = p.name)
                            updateState(state.copy(partners = state.partners + p, auditLogs = state.auditLogs + log))
                        }
                    )
                    3 -> ReportsScreen(
                        state = state,
                        onCreateStatement = { asOf ->
                            val totals = LedgerCalculator.compute(state, asOf)
                            val lines = totals.partnerSummaries.map { ps ->
                                PartnerStatementLine(
                                    partnerId = ps.partner.id,
                                    partnerName = ps.partner.name,
                                    invested = ps.invested,
                                    withdrawn = ps.withdrawn,
                                    capitalSharePct = ps.capitalSharePct,
                                    agreedOwnershipPct = ps.partner.agreedOwnershipPct,
                                    profitShare = ps.profitShare,
                                    netPosition = ps.netPosition
                                )
                            }
                            val nextNum = (state.statements.maxOfOrNull { it.number } ?: 0) + 1
                            val stmt = StatementSnapshot(
                                number = nextNum,
                                asOfDate = asOf,
                                totalInvested = totals.totalInvested,
                                totalRevenue = totals.totalRevenue,
                                totalExpenses = totals.totalExpenses,
                                totalWithdrawn = totals.totalWithdrawn,
                                netProfit = totals.netProfit,
                                currentBalance = totals.currentBalance,
                                partnerLines = lines,
                                auditSummary = "${state.auditLogs.size} audit events recorded up to $asOf."
                            )
                            updateState(state.copy(statements = state.statements + stmt))
                        }
                    )
                    4 -> MoreHubScreen(
                        state = state,
                        repo = repo,
                        initialSubScreen = pendingMoreSubScreen,
                        onConsumeInitialSubScreen = { pendingMoreSubScreen = "MENU" },
                        onSettlePartner = { ps ->
                            val updatedEntries = state.entries.map { e ->
                                if (!e.isDeleted && e.type == EntryType.EXPENSE && e.paidByPartnerId == ps.partner.id && !e.countAsInvestment) {
                                    e.copy(paidByPartnerId = null, note = (e.note + " [Settled]").trim())
                                } else e
                            }
                            val log = AuditLog(
                                action = "SETTLE",
                                recordType = "REIMBURSEMENT",
                                recordId = ps.partner.id,
                                newValue = "Settled ${formatMoney(biz.currency, ps.owedByBusiness)} to ${ps.partner.name}"
                            )
                            updateState(state.copy(entries = updatedEntries, auditLogs = state.auditLogs + log))
                        },
                        onAddPending = { pc ->
                            updateState(state.copy(pendingContributions = state.pendingContributions + pc))
                        },
                        onCompletePending = { pc ->
                            val entry = LedgerEntry(
                                type = EntryType.INVESTMENT,
                                date = todayDateString(),
                                amount = pc.amount,
                                partnerId = pc.partnerId,
                                note = pc.note.ifBlank { "Pending contribution received" }
                            )
                            val updatedPending = state.pendingContributions.map {
                                if (it.id == pc.id) it.copy(isCompleted = true) else it
                            }
                            val log = AuditLog(
                                action = "CREATE",
                                recordType = "INVESTMENT",
                                recordId = entry.id,
                                newValue = "Completed pending ${formatMoney(biz.currency, pc.amount)}"
                            )
                            updateState(
                                state.copy(
                                    entries = state.entries + entry,
                                    pendingContributions = updatedPending,
                                    auditLogs = state.auditLogs + log
                                )
                            )
                        },
                        onUpdateCategories = { updatedCats ->
                            updateState(state.copy(categories = updatedCats))
                        },
                        onUpdateCurrencies = { updatedCurs ->
                            updateState(state.copy(currencies = updatedCurs))
                        },
                        onUpdateProfile = { updatedBiz ->
                            updateState(state.copy(business = updatedBiz))
                        },
                        onRestoreState = { restored -> updateState(restored) },
                        onUpdateSecurity = { newPin, bio ->
                            updateState(state.copy(business = biz.copy(pin = newPin, biometricEnabled = bio)))
                        }
                    )
                }
            }
        }
    }

    // 1. Receipt Viewer Dialog (when tapping an entry on Ledger or Home)
    viewingReceiptEntry?.let { entry ->
        EntryReceiptDialog(
            state = state,
            entry = entry,
            onDismiss = { viewingReceiptEntry = null },
            onEdit = {
                viewingReceiptEntry = null
                editingEntry = entry
                dialogInitialType = entry.type
                showEntryDialog = true
            }
        )
    }

    // 2. Add / Edit Entry Dialog
    if (showEntryDialog) {
        AddEditEntryDialog(
            state = state,
            initialType = dialogInitialType,
            existing = editingEntry,
            onDismiss = { showEntryDialog = false },
            onSave = { entry ->
                val isEdit = editingEntry != null
                val newList = if (isEdit) {
                    state.entries.map { if (it.id == entry.id) entry else it }
                } else {
                    state.entries + entry
                }
                val orig = formatEntryOriginal(entry, biz.currency)?.let { " ($it)" } ?: ""
                val log = AuditLog(
                    action = if (isEdit) "EDIT" else "CREATE",
                    recordType = entry.type.name,
                    recordId = entry.id,
                    oldValue = editingEntry?.let { formatMoney(biz.currency, it.amount) } ?: "",
                    newValue = "${formatMoney(biz.currency, entry.amount)}$orig"
                )
                updateState(state.copy(entries = newList, auditLogs = state.auditLogs + log))
                showEntryDialog = false
            },
            onDelete = { toDelete ->
                val newList = state.entries.map {
                    if (it.id == toDelete.id) it.copy(isDeleted = true) else it
                }
                val log = AuditLog(
                    action = "DELETE",
                    recordType = toDelete.type.name,
                    recordId = toDelete.id,
                    oldValue = formatMoney(biz.currency, toDelete.amount)
                )
                updateState(state.copy(entries = newList, auditLogs = state.auditLogs + log))
                showEntryDialog = false
            }
        )
    }
}
