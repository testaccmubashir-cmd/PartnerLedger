package com.mahibafaris.partnerledger.model

enum class EntryType(val label: String) {
    INVESTMENT("Investment"),
    EXPENSE("Expense"),
    REVENUE("Revenue"),
    WITHDRAWAL("Withdrawal")
}

enum class WithdrawalType(val label: String) {
    PROFIT("Profit Withdrawal"),
    CAPITAL_RETURN("Capital Return")
}

enum class ThemeMode(val label: String) {
    SYSTEM("System"),
    LIGHT("Light"),
    DARK("Dark")
}

data class BusinessConfig(
    val businessName: String,
    val currency: String,
    val startDate: String,
    val adminName: String,
    val pin: String = "",
    val lastBackupTimestamp: Long = 0L,
    val nextStatementNumber: Int = 1
)

data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.LIGHT,
    val backupReminderDays: Int = 7,
    val interestOnCapitalEnabled: Boolean = false,
    val interestRatePct: Double = 0.0,
    val salaryCreditEnabled: Boolean = false,
    val lastUsedPartnerId: String? = null,
    val lastUsedCategoryId: String? = null
)

data class Partner(
    val id: String,
    val name: String,
    val contact: String = "",
    val agreedOwnershipPct: Double = 0.0,
    val monthlySalaryCredit: Double = 0.0
)

data class Category(
    val id: String,
    val name: String,
    val isDirectCost: Boolean = false,
    val isFixed: Boolean = false
)

data class LedgerEntry(
    val id: String,
    val type: EntryType,
    val amount: Double,
    val date: String, // YYYY-MM-DD
    val partnerId: String? = null,
    val categoryId: String? = null,
    val vendorOrSource: String = "",
    val paymentMethod: String = "Bank Transfer",
    val paidByPartnerId: String? = null, // null means Business Cash
    val isSettled: Boolean = false,
    val withdrawalType: WithdrawalType = WithdrawalType.PROFIT,
    val note: String = "",
    val hasProof: Boolean = false,
    val proofUri: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isDeleted: Boolean = false
)

data class Commitment(
    val id: String,
    val partnerId: String,
    val promisedAmount: Double,
    val paidAmount: Double = 0.0,
    val dueDate: String,
    val note: String = ""
)

data class ReconciliationRecord(
    val id: String,
    val date: String,
    val actualBalance: Double,
    val calculatedBalance: Double,
    val difference: Double,
    val note: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

data class AuditLogItem(
    val id: String,
    val recordType: String,
    val recordId: String,
    val action: String, // ADD, EDIT, DELETE, RESTORE, SETTLE
    val oldValue: String,
    val newValue: String,
    val timestamp: Long = System.currentTimeMillis()
)

data class PartnerStatementLine(
    val partnerName: String,
    val invested: Double,
    val withdrawn: Double,
    val capitalSharePct: Double,
    val agreedOwnershipPct: Double,
    val profitShare: Double,
    val netPosition: Double
)

data class StatementSnapshot(
    val number: Int,
    val asOfDate: String,
    val generatedAt: Long,
    val totalInvested: Double,
    val totalExpenses: Double,
    val totalRevenue: Double,
    val totalWithdrawn: Double,
    val currentBalance: Double,
    val netProfit: Double,
    val auditSummary: String,
    val partnerLines: List<PartnerStatementLine>
)

data class AppState(
    val business: BusinessConfig? = null,
    val settings: AppSettings = AppSettings(),
    val partners: List<Partner> = emptyList(),
    val categories: List<Category> = defaultCategories(),
    val entries: List<LedgerEntry> = emptyList(),
    val commitments: List<Commitment> = emptyList(),
    val reconciliations: List<ReconciliationRecord> = emptyList(),
    val auditLogs: List<AuditLogItem> = emptyList(),
    val statements: List<StatementSnapshot> = emptyList()
)

fun defaultCategories(): List<Category> = listOf(
    Category("cat_rent", "Rent", isDirectCost = false, isFixed = true),
    Category("cat_inv", "Inventory", isDirectCost = true, isFixed = false),
    Category("cat_mkt", "Marketing", isDirectCost = false, isFixed = false),
    Category("cat_sal", "Salaries", isDirectCost = false, isFixed = true),
    Category("cat_eqp", "Equipment", isDirectCost = false, isFixed = true),
    Category("cat_utl", "Utilities", isDirectCost = false, isFixed = true),
    Category("cat_trv", "Travel", isDirectCost = false, isFixed = false),
    Category("cat_sft", "Software", isDirectCost = false, isFixed = true),
    Category("cat_oth", "Other", isDirectCost = false, isFixed = false)
)

data class PartnerSummary(
    val partner: Partner,
    val invested: Double,
    val withdrawn: Double,
    val capitalSharePct: Double,
    val ownershipGapPct: Double,
    val profitShare: Double,
    val interestCredit: Double,
    val salaryCredit: Double,
    val netPosition: Double,
    val roiPct: Double,
    val owedByBusiness: Double
)

data class MonthlyBucket(
    val monthKey: String, // YYYY-MM
    val invested: Double,
    val expenses: Double,
    val revenue: Double
)

data class CashHealthMetrics(
    val monthlyNetBurn: Double,
    val runwayMonths: Double?, // null means "Not burning cash"
    val grossMarginPct: Double?,
    val operatingExpenses: Double,
    val fixedCosts: Double,
    val variableCosts: Double,
    val businessRoiPct: Double?,
    val monthsOfData: Int,
    val avgMonthlyExpense: Double
)

data class DashboardMetrics(
    val totalInvested: Double,
    val totalExpenses: Double,
    val totalRevenue: Double,
    val totalWithdrawn: Double,
    val netProfit: Double,
    val currentBalance: Double,
    val thisMonthProfit: Double,
    val missingProofCount: Int,
    val missingCategoryCount: Int,
    val overdrawnPartners: List<String>,
    val lastReconciliationDiff: Double?,
    val partnerSummaries: List<PartnerSummary>,
    val monthlyBuckets: List<MonthlyBucket>,
    val cashHealth: CashHealthMetrics
)
