package com.mahibafaris.partnerledger.data

import android.content.Context
import com.mahibafaris.partnerledger.model.*
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID

class LedgerRepository(private val context: Context) {

    private val storageFile: File
        get() = File(context.filesDir, "partnerledger_state_v2.json")

    fun load(): AppState {
        return try {
            if (!storageFile.exists()) return AppState()
            val text = storageFile.readText()
            if (text.isBlank()) AppState() else fromJson(text)
        } catch (e: Exception) {
            AppState()
        }
    }

    fun save(state: AppState) {
        storageFile.writeText(toJson(state))
    }

    fun toJson(state: AppState): String {
        val root = JSONObject()
        state.business?.let { b ->
            root.put("business", JSONObject().apply {
                put("businessName", b.businessName)
                put("adminName", b.adminName)
                put("currency", b.currency)
                put("startDate", b.startDate)
                put("pin", b.pin)
                put("biometricEnabled", b.biometricEnabled)
            })
        }

        root.put("partners", JSONArray().apply {
            state.partners.forEach { p ->
                put(JSONObject().apply {
                    put("id", p.id)
                    put("name", p.name)
                    put("agreedOwnershipPct", p.agreedOwnershipPct)
                    put("role", p.role)
                    put("active", p.active)
                })
            }
        })

        root.put("categories", JSONArray().apply {
            state.categories.forEach { c ->
                put(JSONObject().apply {
                    put("id", c.id)
                    put("name", c.name)
                    put("forType", c.forType.name)
                })
            }
        })

        root.put("entries", JSONArray().apply {
            state.entries.forEach { e ->
                put(JSONObject().apply {
                    put("id", e.id)
                    put("type", e.type.name)
                    put("date", e.date)
                    put("amount", e.amount)
                    put("partnerId", e.partnerId ?: JSONObject.NULL)
                    put("categoryId", e.categoryId ?: JSONObject.NULL)
                    put("vendorOrSource", e.vendorOrSource)
                    put("paymentMethod", e.paymentMethod)
                    put("paidByPartnerId", e.paidByPartnerId ?: JSONObject.NULL)
                    put("countAsInvestment", e.countAsInvestment)
                    put("originalCurrency", e.originalCurrency)
                    put("originalAmount", e.originalAmount)
                    put("exchangeRate", e.exchangeRate)
                    put("hasProof", e.hasProof || e.receiptImageBase64.isNotBlank())
                    put("receiptImageBase64", e.receiptImageBase64)
                    put("note", e.note)
                    put("isDeleted", e.isDeleted)
                    put("createdAt", e.createdAt)
                })
            }
        })

        root.put("pendingContributions", JSONArray().apply {
            state.pendingContributions.forEach { pc ->
                put(JSONObject().apply {
                    put("id", pc.id)
                    put("partnerId", pc.partnerId)
                    put("amount", pc.amount)
                    put("dueDate", pc.dueDate)
                    put("note", pc.note)
                    put("isCompleted", pc.isCompleted)
                })
            }
        })

        root.put("statements", JSONArray().apply {
            state.statements.forEach { s ->
                put(JSONObject().apply {
                    put("id", s.id)
                    put("number", s.number)
                    put("asOfDate", s.asOfDate)
                    put("totalInvested", s.totalInvested)
                    put("totalRevenue", s.totalRevenue)
                    put("totalExpenses", s.totalExpenses)
                    put("totalWithdrawn", s.totalWithdrawn)
                    put("netProfit", s.netProfit)
                    put("currentBalance", s.currentBalance)
                    put("auditSummary", s.auditSummary)
                    put("createdAt", s.createdAt)
                    put("partnerLines", JSONArray().apply {
                        s.partnerLines.forEach { pl ->
                            put(JSONObject().apply {
                                put("partnerId", pl.partnerId)
                                put("partnerName", pl.partnerName)
                                put("invested", pl.invested)
                                put("withdrawn", pl.withdrawn)
                                put("capitalSharePct", pl.capitalSharePct)
                                put("agreedOwnershipPct", pl.agreedOwnershipPct)
                                put("profitShare", pl.profitShare)
                                put("netPosition", pl.netPosition)
                            })
                        }
                    })
                })
            }
        })

        root.put("auditLogs", JSONArray().apply {
            state.auditLogs.forEach { log ->
                put(JSONObject().apply {
                    put("id", log.id)
                    put("action", log.action)
                    put("recordType", log.recordType)
                    put("recordId", log.recordId)
                    put("oldValue", log.oldValue)
                    put("newValue", log.newValue)
                    put("timestamp", log.timestamp)
                })
            }
        })

        return root.toString()
    }

    fun fromJson(json: String): AppState {
        val root = JSONObject(json)

        val business = if (root.has("business") && !root.isNull("business")) {
            val b = root.getJSONObject("business")
            BusinessConfig(
                businessName = b.optString("businessName", ""),
                adminName = b.optString("adminName", ""),
                currency = b.optString("currency", "PKR"),
                startDate = b.optString("startDate", todayDateString()),
                pin = b.optString("pin", ""),
                biometricEnabled = b.optBoolean("biometricEnabled", true)
            )
        } else null

        val partners = mutableListOf<Partner>()
        val pArr = root.optJSONArray("partners") ?: JSONArray()
        for (i in 0 until pArr.length()) {
            val p = pArr.getJSONObject(i)
            partners.add(
                Partner(
                    id = p.optString("id", UUID.randomUUID().toString()),
                    name = p.optString("name", "Partner"),
                    agreedOwnershipPct = p.optDouble("agreedOwnershipPct", 0.0),
                    role = p.optString("role", "Partner"),
                    active = p.optBoolean("active", true)
                )
            )
        }

        val categories = mutableListOf<Category>()
        val cArr = root.optJSONArray("categories")
        if (cArr != null && cArr.length() > 0) {
            for (i in 0 until cArr.length()) {
                val c = cArr.getJSONObject(i)
                val ft = runCatching { EntryType.valueOf(c.optString("forType", "EXPENSE")) }
                    .getOrDefault(EntryType.EXPENSE)
                categories.add(
                    Category(
                        id = c.optString("id", UUID.randomUUID().toString()),
                        name = c.optString("name", "General"),
                        forType = ft
                    )
                )
            }
        } else {
            categories.addAll(defaultCategories())
        }

        val entries = mutableListOf<LedgerEntry>()
        val eArr = root.optJSONArray("entries") ?: JSONArray()
        for (i in 0 until eArr.length()) {
            val e = eArr.getJSONObject(i)
            val et = runCatching { EntryType.valueOf(e.optString("type", "EXPENSE")) }
                .getOrDefault(EntryType.EXPENSE)
            val imgB64 = e.optString("receiptImageBase64", "")
            entries.add(
                LedgerEntry(
                    id = e.optString("id", UUID.randomUUID().toString()),
                    type = et,
                    date = e.optString("date", todayDateString()),
                    amount = e.optDouble("amount", 0.0),
                    partnerId = if (e.isNull("partnerId")) null else e.optString("partnerId").ifBlank { null },
                    categoryId = if (e.isNull("categoryId")) null else e.optString("categoryId").ifBlank { null },
                    vendorOrSource = e.optString("vendorOrSource", ""),
                    paymentMethod = e.optString("paymentMethod", "Bank Transfer"),
                    paidByPartnerId = if (e.isNull("paidByPartnerId")) null else e.optString("paidByPartnerId").ifBlank { null },
                    countAsInvestment = e.optBoolean("countAsInvestment", false),
                    originalCurrency = e.optString("originalCurrency", ""),
                    originalAmount = e.optDouble("originalAmount", 0.0),
                    exchangeRate = e.optDouble("exchangeRate", 1.0),
                    hasProof = e.optBoolean("hasProof", false) || imgB64.isNotBlank(),
                    receiptImageBase64 = imgB64,
                    note = e.optString("note", ""),
                    isDeleted = e.optBoolean("isDeleted", false),
                    createdAt = e.optLong("createdAt", System.currentTimeMillis())
                )
            )
        }

        val pending = mutableListOf<PendingContribution>()
        val pcArr = root.optJSONArray("pendingContributions") ?: JSONArray()
        for (i in 0 until pcArr.length()) {
            val pc = pcArr.getJSONObject(i)
            pending.add(
                PendingContribution(
                    id = pc.optString("id", UUID.randomUUID().toString()),
                    partnerId = pc.optString("partnerId", ""),
                    amount = pc.optDouble("amount", 0.0),
                    dueDate = pc.optString("dueDate", todayDateString()),
                    note = pc.optString("note", ""),
                    isCompleted = pc.optBoolean("isCompleted", false)
                )
            )
        }

        val statements = mutableListOf<StatementSnapshot>()
        val sArr = root.optJSONArray("statements") ?: JSONArray()
        for (i in 0 until sArr.length()) {
            val s = sArr.getJSONObject(i)
            val lines = mutableListOf<PartnerStatementLine>()
            val plArr = s.optJSONArray("partnerLines") ?: JSONArray()
            for (j in 0 until plArr.length()) {
                val pl = plArr.getJSONObject(j)
                lines.add(
                    PartnerStatementLine(
                        partnerId = pl.optString("partnerId", ""),
                        partnerName = pl.optString("partnerName", ""),
                        invested = pl.optDouble("invested", 0.0),
                        withdrawn = pl.optDouble("withdrawn", 0.0),
                        capitalSharePct = pl.optDouble("capitalSharePct", 0.0),
                        agreedOwnershipPct = pl.optDouble("agreedOwnershipPct", 0.0),
                        profitShare = pl.optDouble("profitShare", 0.0),
                        netPosition = pl.optDouble("netPosition", 0.0)
                    )
                )
            }
            statements.add(
                StatementSnapshot(
                    id = s.optString("id", UUID.randomUUID().toString()),
                    number = s.optInt("number", i + 1),
                    asOfDate = s.optString("asOfDate", todayDateString()),
                    totalInvested = s.optDouble("totalInvested", 0.0),
                    totalRevenue = s.optDouble("totalRevenue", 0.0),
                    totalExpenses = s.optDouble("totalExpenses", 0.0),
                    totalWithdrawn = s.optDouble("totalWithdrawn", 0.0),
                    netProfit = s.optDouble("netProfit", 0.0),
                    currentBalance = s.optDouble("currentBalance", 0.0),
                    partnerLines = lines,
                    auditSummary = s.optString("auditSummary", ""),
                    createdAt = s.optLong("createdAt", System.currentTimeMillis())
                )
            )
        }

        val logs = mutableListOf<AuditLog>()
        val lArr = root.optJSONArray("auditLogs") ?: JSONArray()
        for (i in 0 until lArr.length()) {
            val l = lArr.getJSONObject(i)
            logs.add(
                AuditLog(
                    id = l.optString("id", UUID.randomUUID().toString()),
                    action = l.optString("action", ""),
                    recordType = l.optString("recordType", ""),
                    recordId = l.optString("recordId", ""),
                    oldValue = l.optString("oldValue", ""),
                    newValue = l.optString("newValue", ""),
                    timestamp = l.optLong("timestamp", System.currentTimeMillis())
                )
            )
        }

        return AppState(
            business = business,
            partners = partners,
            categories = categories,
            entries = entries,
            pendingContributions = pending,
            statements = statements,
            auditLogs = logs
        )
    }
}

object LedgerCalculator {

    data class BusinessTotals(
        val totalInvested: Double,
        val totalRevenue: Double,
        val totalExpenses: Double,
        val totalWithdrawn: Double,
        val netProfit: Double,
        val currentBalance: Double,
        val partnerSummaries: List<PartnerSummary>
    )

    fun compute(state: AppState, asOfDate: String? = null): BusinessTotals {
        val activeEntries = state.entries.filter { e ->
            !e.isDeleted && (asOfDate == null || e.date <= asOfDate)
        }

        val directInvestments = activeEntries.filter { it.type == EntryType.INVESTMENT }
        val expenseInvestments = activeEntries.filter {
            it.type == EntryType.EXPENSE && it.paidByPartnerId != null && it.countAsInvestment
        }
        val totalInvested = directInvestments.sumOf { it.amount } + expenseInvestments.sumOf { it.amount }

        val totalRevenue = activeEntries.filter { it.type == EntryType.REVENUE }.sumOf { it.amount }
        val totalExpenses = activeEntries.filter { it.type == EntryType.EXPENSE }.sumOf { it.amount }
        val totalWithdrawn = activeEntries.filter { it.type == EntryType.WITHDRAWAL }.sumOf { it.amount }
        val netProfit = totalRevenue - totalExpenses

        val unpaidReimbursements = activeEntries.filter {
            it.type == EntryType.EXPENSE && it.paidByPartnerId != null && !it.countAsInvestment
        }.sumOf { it.amount }

        val currentBalance = totalInvested + totalRevenue - (totalExpenses - unpaidReimbursements) - totalWithdrawn
        val totalAgreedPct = state.partners.sumOf { it.agreedOwnershipPct }

        val summaries = state.partners.map { partner ->
            val pDirectInv = directInvestments.filter { it.partnerId == partner.id }.sumOf { it.amount }
            val pExpInv = expenseInvestments.filter { it.paidByPartnerId == partner.id }.sumOf { it.amount }
            val pInvested = pDirectInv + pExpInv

            val pWithdrawn = activeEntries
                .filter { it.type == EntryType.WITHDRAWAL && it.partnerId == partner.id }
                .sumOf { it.amount }

            val capShare = if (totalInvested > 0.0) (pInvested / totalInvested) * 100.0 else 0.0

            val profitSharePct = when {
                totalAgreedPct > 0.0 -> (partner.agreedOwnershipPct / totalAgreedPct) * 100.0
                totalInvested > 0.0 -> capShare
                state.partners.isNotEmpty() -> 100.0 / state.partners.size
                else -> 0.0
            }

            val profitShare = netProfit * (profitSharePct / 100.0)
            val netPosition = pInvested + profitShare - pWithdrawn
            val roiPct = if (pInvested > 0.0) (profitShare / pInvested) * 100.0 else 0.0

            val owed = activeEntries
                .filter { it.type == EntryType.EXPENSE && it.paidByPartnerId == partner.id && !it.countAsInvestment }
                .sumOf { it.amount }

            PartnerSummary(
                partner = partner,
                invested = pInvested,
                withdrawn = pWithdrawn,
                capitalSharePct = capShare,
                profitShare = profitShare,
                roiPct = roiPct,
                netPosition = netPosition,
                owedByBusiness = owed
            )
        }

        return BusinessTotals(
            totalInvested = totalInvested,
            totalRevenue = totalRevenue,
            totalExpenses = totalExpenses,
            totalWithdrawn = totalWithdrawn,
            netProfit = netProfit,
            currentBalance = currentBalance,
            partnerSummaries = summaries
        )
    }
}
