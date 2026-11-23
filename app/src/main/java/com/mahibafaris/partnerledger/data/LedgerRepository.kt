package com.mahibafaris.partnerledger.data

import android.content.Context
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.mahibafaris.partnerledger.model.*
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class LedgerRepository(private val context: Context) {
    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()
    private val dbFile = File(context.filesDir, "partner_ledger_db_v2.json")

    fun loadState(): AppState {
        if (!dbFile.exists()) return AppState()
        return try {
            gson.fromJson(dbFile.readText(), AppState::class.java) ?: AppState()
        } catch (e: Exception) {
            AppState()
        }
    }

    fun saveState(state: AppState) {
        dbFile.writeText(gson.toJson(state))
    }

    fun getDatabaseSizeKb(): Long {
        return if (dbFile.exists()) (dbFile.length() / 1024L).coerceAtLeast(1L) else 1L
    }

    fun exportRawJson(state: AppState): String = gson.toJson(state)

    fun parseBackupJson(raw: String): AppState? {
        return try {
            val parsed = gson.fromJson(raw, AppState::class.java)
            if (parsed?.business != null) parsed else null
        } catch (e: Exception) {
            null
        }
    }

    fun computeMetrics(state: AppState): DashboardMetrics {
        val active = state.entries.filter { !it.isDeleted }
        val catMap = state.categories.associateBy { it.id }

        val totalInvested = active.filter { it.type == EntryType.INVESTMENT }.sumOf { it.amount }
        val totalExpenses = active.filter { it.type == EntryType.EXPENSE }.sumOf { it.amount }
        val totalRevenue = active.filter { it.type == EntryType.REVENUE }.sumOf { it.amount }
        val totalWithdrawn = active.filter { it.type == EntryType.WITHDRAWAL }.sumOf { it.amount }

        val netProfit = totalRevenue - totalExpenses
        val currentBalance = totalInvested + totalRevenue - totalExpenses - totalWithdrawn

        val thisMonthKey = SimpleDateFormat("yyyy-MM", Locale.US).format(Date())
        val thisMonthRev = active.filter { it.type == EntryType.REVENUE && it.date.startsWith(thisMonthKey) }.sumOf { it.amount }
        val thisMonthExp = active.filter { it.type == EntryType.EXPENSE && it.date.startsWith(thisMonthKey) }.sumOf { it.amount }
        val thisMonthProfit = thisMonthRev - thisMonthExp

        val missingProofCount = active.count {
            (it.type == EntryType.INVESTMENT || it.type == EntryType.EXPENSE) && !it.hasProof
        }
        val missingCategoryCount = active.count {
            it.type == EntryType.EXPENSE && it.categoryId.isNullOrBlank()
        }

        // Net capital per partner (investments minus capital returns)
        val totalNetCapital = state.partners.sumOf { p ->
            val inv = active.filter { it.type == EntryType.INVESTMENT && it.partnerId == p.id }.sumOf { it.amount }
            val capRet = active.filter {
                it.type == EntryType.WITHDRAWAL && it.partnerId == p.id && it.withdrawalType == WithdrawalType.CAPITAL_RETURN
            }.sumOf { it.amount }
            (inv - capRet).coerceAtLeast(0.0)
        }

        val summaries = state.partners.map { partner ->
            val pInv = active.filter { it.type == EntryType.INVESTMENT && it.partnerId == partner.id }.sumOf { it.amount }
            val pWithdrawn = active.filter { it.type == EntryType.WITHDRAWAL && it.partnerId == partner.id }.sumOf { it.amount }
            val pCapRet = active.filter {
                it.type == EntryType.WITHDRAWAL && it.partnerId == partner.id && it.withdrawalType == WithdrawalType.CAPITAL_RETURN
            }.sumOf { it.amount }

            val netCap = (pInv - pCapRet).coerceAtLeast(0.0)
            val capSharePct = if (totalNetCapital > 0.0) (netCap / totalNetCapital) * 100.0 else 0.0
            val effectivePct = if (partner.agreedOwnershipPct > 0.0) partner.agreedOwnershipPct else capSharePct

            val interestCredit = if (state.settings.interestOnCapitalEnabled) {
                pInv * (state.settings.interestRatePct / 100.0)
            } else 0.0

            val salaryCredit = if (state.settings.salaryCreditEnabled) {
                partner.monthlySalaryCredit
            } else 0.0

            val profitShare = netProfit * (effectivePct / 100.0)
            val netPosition = pInv - pWithdrawn + profitShare + interestCredit + salaryCredit
            val roiPct = if (pInv > 0.0) (profitShare / pInv) * 100.0 else 0.0
            val gap = if (partner.agreedOwnershipPct > 0.0) capSharePct - partner.agreedOwnershipPct else 0.0

            val owedByBusiness = active.filter {
                it.type == EntryType.EXPENSE && it.paidByPartnerId == partner.id && !it.isSettled
            }.sumOf { it.amount }

            PartnerSummary(
                partner = partner,
                invested = pInv,
                withdrawn = pWithdrawn,
                capitalSharePct = capSharePct,
                ownershipGapPct = gap,
                profitShare = profitShare,
                interestCredit = interestCredit,
                salaryCredit = salaryCredit,
                netPosition = netPosition,
                roiPct = roiPct,
                owedByBusiness = owedByBusiness
            )
        }

        val overdrawn = summaries.filter { it.netPosition < 0.0 }.map { it.partner.name }

        // Monthly buckets
        val months = active.mapNotNull { if (it.date.length >= 7) it.date.substring(0, 7) else null }.distinct().sorted()
        val buckets = months.map { m ->
            val mEntries = active.filter { it.date.startsWith(m) }
            MonthlyBucket(
                monthKey = m,
                invested = mEntries.filter { it.type == EntryType.INVESTMENT }.sumOf { it.amount },
                expenses = mEntries.filter { it.type == EntryType.EXPENSE }.sumOf { it.amount },
                revenue = mEntries.filter { it.type == EntryType.REVENUE }.sumOf { it.amount }
            )
        }

        // Cash Health (investments are NOT revenue; capital withdrawals are NOT expenses)
        val monthsCount = buckets.size.coerceAtLeast(1)
        val avgMonthlyExp = if (buckets.isEmpty()) 0.0 else totalExpenses / monthsCount
        val avgMonthlyRev = if (buckets.isEmpty()) 0.0 else totalRevenue / monthsCount
        val netBurn = avgMonthlyExp - avgMonthlyRev
        val runway = if (netBurn > 0.0 && currentBalance > 0.0) currentBalance / netBurn else null

        val directCosts = active.filter {
            it.type == EntryType.EXPENSE && catMap[it.categoryId]?.isDirectCost == true
        }.sumOf { it.amount }
        val operatingExpenses = totalExpenses - directCosts
        val fixedCosts = active.filter {
            it.type == EntryType.EXPENSE && catMap[it.categoryId]?.isFixed == true
        }.sumOf { it.amount }
        val variableCosts = totalExpenses - fixedCosts

        val grossMargin = if (totalRevenue > 0.0) ((totalRevenue - directCosts) / totalRevenue) * 100.0 else null
        val bizRoi = if (totalInvested > 0.0) (netProfit / totalInvested) * 100.0 else null

        val lastRecDiff = state.reconciliations.firstOrNull()?.difference

        return DashboardMetrics(
            totalInvested = totalInvested,
            totalExpenses = totalExpenses,
            totalRevenue = totalRevenue,
            totalWithdrawn = totalWithdrawn,
            netProfit = netProfit,
            currentBalance = currentBalance,
            thisMonthProfit = thisMonthProfit,
            missingProofCount = missingProofCount,
            missingCategoryCount = missingCategoryCount,
            overdrawnPartners = overdrawn,
            lastReconciliationDiff = lastRecDiff,
            partnerSummaries = summaries,
            monthlyBuckets = buckets,
            cashHealth = CashHealthMetrics(
                monthlyNetBurn = netBurn,
                runwayMonths = runway,
                grossMarginPct = grossMargin,
                operatingExpenses = operatingExpenses,
                fixedCosts = fixedCosts,
                variableCosts = variableCosts,
                businessRoiPct = bizRoi,
                monthsOfData = buckets.size,
                avgMonthlyExpense = avgMonthlyExp
            )
        )
    }

    fun generateNextStatement(state: AppState, asOfDate: String): Pair<AppState, StatementSnapshot> {
        val biz = state.business ?: error("Business not configured")
        val metrics = computeMetrics(state)
        val num = biz.nextStatementNumber
        val lastStmtTime = state.statements.maxOfOrNull { it.generatedAt } ?: 0L
        val recentLogs = state.auditLogs.filter { it.timestamp > lastStmtTime }
        val edits = recentLogs.count { it.action == "EDIT" }
        val deletes = recentLogs.count { it.action == "DELETE" }
        val adds = recentLogs.count { it.action == "ADD" }
        val prevLabel = if (num > 1) "Statement #${num - 1}" else "start"
        val auditSummary = "$adds additions, $edits edits, $deletes deletions since $prevLabel"

        val lines = metrics.partnerSummaries.map { ps ->
            PartnerStatementLine(
                partnerName = ps.partner.name,
                invested = ps.invested,
                withdrawn = ps.withdrawn,
                capitalSharePct = ps.capitalSharePct,
                agreedOwnershipPct = ps.partner.agreedOwnershipPct,
                profitShare = ps.profitShare,
                netPosition = ps.netPosition
            )
        }

        val snapshot = StatementSnapshot(
            number = num,
            asOfDate = asOfDate,
            generatedAt = System.currentTimeMillis(),
            totalInvested = metrics.totalInvested,
            totalExpenses = metrics.totalExpenses,
            totalRevenue = metrics.totalRevenue,
            totalWithdrawn = metrics.totalWithdrawn,
            currentBalance = metrics.currentBalance,
            netProfit = metrics.netProfit,
            auditSummary = auditSummary,
            partnerLines = lines
        )

        val updated = state.copy(
            business = biz.copy(nextStatementNumber = num + 1),
            statements = listOf(snapshot) + state.statements
        )
        saveState(updated)
        return updated to snapshot
    }
}
