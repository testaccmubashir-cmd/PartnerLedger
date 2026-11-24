package com.mahibafaris.partnerledger.model

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

enum class EntryType(val label: String) {
    INVESTMENT("Investment"),
    EXPENSE("Expense"),
    REVENUE("Revenue"),
    WITHDRAWAL("Withdrawal")
}

data class BusinessConfig(
    val businessName: String = "",
    val adminName: String = "",
    val currency: String = "PKR",
    val startDate: String = todayDateString(),
    val pin: String = "",
    val biometricEnabled: Boolean = true,
    val logoBase64: String = ""
)

data class Partner(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val agreedOwnershipPct: Double = 0.0,
    val role: String = "Partner",
    val active: Boolean = true
)

data class Category(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val forType: EntryType = EntryType.EXPENSE
)

data class LedgerEntry(
    val id: String = UUID.randomUUID().toString(),
    val type: EntryType,
    val date: String = todayDateString(),
    val amount: Double,
    val partnerId: String? = null,
    val categoryId: String? = null,
    val vendorOrSource: String = "",
    val paymentMethod: String = "Bank Transfer",
    val paidByPartnerId: String? = null,
    val countAsInvestment: Boolean = false,
    val originalCurrency: String = "",
    val originalAmount: Double = 0.0,
    val exchangeRate: Double = 1.0,
    val hasProof: Boolean = false,
    val receiptImageBase64: String = "",
    val note: String = "",
    val isDeleted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

data class PendingContribution(
    val id: String = UUID.randomUUID().toString(),
    val partnerId: String,
    val amount: Double,
    val dueDate: String = todayDateString(),
    val note: String = "",
    val isCompleted: Boolean = false
)

data class AuditLog(
    val id: String = UUID.randomUUID().toString(),
    val action: String,
    val recordType: String,
    val recordId: String,
    val oldValue: String = "",
    val newValue: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

data class PartnerStatementLine(
    val partnerId: String,
    val partnerName: String,
    val invested: Double,
    val withdrawn: Double,
    val capitalSharePct: Double,
    val agreedOwnershipPct: Double,
    val profitShare: Double,
    val netPosition: Double
)

data class StatementSnapshot(
    val id: String = UUID.randomUUID().toString(),
    val number: Int,
    val asOfDate: String,
    val totalInvested: Double,
    val totalRevenue: Double,
    val totalExpenses: Double,
    val totalWithdrawn: Double,
    val netProfit: Double,
    val currentBalance: Double,
    val partnerLines: List<PartnerStatementLine>,
    val auditSummary: String,
    val createdAt: Long = System.currentTimeMillis()
)

data class PartnerSummary(
    val partner: Partner,
    val invested: Double,
    val withdrawn: Double,
    val capitalSharePct: Double,
    val profitShare: Double,
    val roiPct: Double,
    val netPosition: Double,
    val owedByBusiness: Double
)

data class AppState(
    val business: BusinessConfig? = null,
    val partners: List<Partner> = emptyList(),
    val categories: List<Category> = defaultCategories(),
    val currencies: List<String> = SUPPORTED_CURRENCIES,
    val entries: List<LedgerEntry> = emptyList(),
    val pendingContributions: List<PendingContribution> = emptyList(),
    val statements: List<StatementSnapshot> = emptyList(),
    val auditLogs: List<AuditLog> = emptyList()
)

fun defaultCategories(): List<Category> = listOf(
    Category("cat_hosting", "Hosting & Domains", EntryType.EXPENSE),
    Category("cat_content", "Content & Writers", EntryType.EXPENSE),
    Category("cat_ads", "Ads & Marketing", EntryType.EXPENSE),
    Category("cat_tools", "Software & Tools", EntryType.EXPENSE),
    Category("cat_ops", "Operations & Misc", EntryType.EXPENSE),
    Category("cat_adnet", "Ad Networks", EntryType.REVENUE),
    Category("cat_aff", "Affiliate Commissions", EntryType.REVENUE),
    Category("cat_client", "Client / Direct Sales", EntryType.REVENUE),
    Category("cat_rev_other", "Other Revenue", EntryType.REVENUE)
)

val SUPPORTED_CURRENCIES = listOf("PKR", "USD", "EUR", "GBP", "AED", "SAR", "CAD", "AUD")

val PAYMENT_METHODS = listOf("Bank Transfer", "Cash", "Mobile Wallet", "Card", "Payoneer / Wise", "Other")

fun todayDateString(): String = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

fun formatMoney(currency: String, amount: Double): String {
    val sign = if (amount < 0) "-" else ""
    return "$sign$currency %,.2f".format(Locale.US, kotlin.math.abs(amount))
}

fun formatEntryOriginal(entry: LedgerEntry, baseCurrency: String): String? {
    if (entry.originalCurrency.isNotBlank() && entry.originalCurrency != baseCurrency && entry.originalAmount > 0.0) {
        return "${entry.originalCurrency} %,.2f".format(Locale.US, entry.originalAmount)
    }
    return null
}
