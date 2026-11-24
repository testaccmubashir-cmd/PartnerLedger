package com.mahibafaris.partnerledger.export

import android.content.Context
import android.content.Intent
import android.graphics.*
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.util.Base64
import androidx.core.content.FileProvider
import com.mahibafaris.partnerledger.model.*
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.*
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

object ExportManager {

    private fun csvCell(text: String): String {
        val cleaned = text.replace('"', '\'').replace(',', ' ').replace('\n', ' ')
        return "\"$cleaned\""
    }

    fun compressBitmapToBase64(bitmap: Bitmap, maxDim: Int = 900, quality: Int = 72): String {
        val ratio = minOf(maxDim.toFloat() / bitmap.width, maxDim.toFloat() / bitmap.height, 1f)
        val scaled = if (ratio < 1f) {
            Bitmap.createScaledBitmap(
                bitmap,
                (bitmap.width * ratio).toInt().coerceAtLeast(1),
                (bitmap.height * ratio).toInt().coerceAtLeast(1),
                true
            )
        } else bitmap

        val baos = ByteArrayOutputStream()
        scaled.compress(Bitmap.CompressFormat.JPEG, quality, baos)
        return Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP)
    }

    fun compressUriToBase64(context: Context, uri: Uri, maxDim: Int = 900, quality: Int = 72): String? {
        return try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val bmp = BitmapFactory.decodeStream(stream) ?: return null
                compressBitmapToBase64(bmp, maxDim, quality)
            }
        } catch (e: Exception) {
            null
        }
    }

    fun decodeBase64ToBitmap(base64: String): Bitmap? {
        if (base64.isBlank()) return null
        return try {
            val bytes = Base64.decode(base64, Base64.NO_WRAP)
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        } catch (e: Exception) {
            null
        }
    }

    // ---- WhatsApp-ready Dark Fintech Transaction Receipt Image ----
    fun shareEntryReceiptImage(
        context: Context,
        biz: BusinessConfig,
        entry: LedgerEntry,
        partnerName: String?,
        paidByName: String?,
        categoryName: String?
    ) {
        val proofBmp = decodeBase64ToBitmap(entry.receiptImageBase64)
        val rows = mutableListOf<Pair<String, String>>()
        rows.add("Date" to entry.date)
        rows.add("Type" to entry.type.label)
        if (!categoryName.isNullOrBlank()) rows.add("Category" to categoryName)
        if (!partnerName.isNullOrBlank()) rows.add("Partner" to partnerName)
        if (entry.vendorOrSource.isNotBlank()) {
            val lbl = if (entry.type == EntryType.EXPENSE) "Vendor / Paid To" else "Source / Client"
            rows.add(lbl to entry.vendorOrSource)
        }
        if (entry.type == EntryType.EXPENSE) {
            val paidText = if (paidByName == null) "Business Cash"
            else "$paidByName (${if (entry.countAsInvestment) "As Investment" else "Reimburse Later"})"
            rows.add("Paid By" to paidText)
        }
        rows.add("Payment Method" to entry.paymentMethod)
        formatEntryOriginal(entry, biz.currency)?.let { orig ->
            rows.add("Original Amount" to "$orig  @ ${"%.4f".format(Locale.US, entry.exchangeRate).trimEnd('0').trimEnd('.')}")
        }
        if (entry.note.isNotBlank()) rows.add("Note" to entry.note)

        val width = 1080
        val rowSpacing = 84
        val proofExtra = if (proofBmp != null) 460 else 0
        val height = (520 + rows.size * rowSpacing + proofExtra + 120).coerceAtLeast(900)

        val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        c.drawColor(Color.parseColor("#050505"))

        // Top Amethyst Gradient Header
        val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                0f, 0f, width.toFloat(), 260f,
                Color.parseColor("#3B0A6B"),
                Color.parseColor("#160826"),
                Shader.TileMode.CLAMP
            )
        }
        c.drawRect(0f, 0f, width.toFloat(), 260f, headerPaint)

        // Cyan bottom accent line
        val cyanLine = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#00E5FF"); strokeWidth = 4f }
        c.drawLine(0f, 260f, width.toFloat(), 260f, cyanLine)

        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE; textSize = 50f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val tagPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#00E5FF"); textSize = 28f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#A699B8"); textSize = 26f
        }

        val logoBmp = decodeBase64ToBitmap(biz.logoBase64)
        var textX = 60f
        if (logoBmp != null) {
            val dst = RectF(60f, 64f, 180f, 184f)
            val saveCount = c.save()
            val clipPath = Path().apply { addRoundRect(dst, 28f, 28f, Path.Direction.CW) }
            c.clipPath(clipPath)
            c.drawBitmap(logoBmp, null, dst, Paint(Paint.FILTER_BITMAP_FLAG))
            c.restoreToCount(saveCount)
            textX = 212f
        }

        c.drawText(biz.businessName.ifBlank { "PartnerLedger" }, textX, 118f, titlePaint)
        c.drawText("TRANSACTION RECEIPT", textX, 166f, tagPaint)
        c.drawText("Ref #${entry.id.take(8).uppercase(Locale.US)}  •  ${entry.date}", textX, 210f, subPaint)

        // Main Receipt Card
        val cardPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#160826") }
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#448A4FFF"); style = Paint.Style.STROKE; strokeWidth = 3f
        }
        val cardRect = RectF(48f, 300f, width - 48f, height - 100f)
        c.drawRoundRect(cardRect, 36f, 36f, cardPaint)
        c.drawRoundRect(cardRect, 36f, 36f, borderPaint)

        // Big Amount
        val isPositive = entry.type == EntryType.INVESTMENT || entry.type == EntryType.REVENUE
        val amtColor = when (entry.type) {
            EntryType.INVESTMENT, EntryType.REVENUE -> Color.parseColor("#00E676")
            EntryType.EXPENSE -> Color.parseColor("#FF4D6D")
            EntryType.WITHDRAWAL -> Color.parseColor("#FFB74D")
        }
        val sign = if (isPositive) "+" else "-"
        val amtPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = amtColor; textSize = 64f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        c.drawText("$sign${formatMoney(biz.currency, entry.amount)}", 92f, 404f, amtPaint)

        val divPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#338A4FFF"); strokeWidth = 2f
            pathEffect = DashPathEffect(floatArrayOf(14f, 10f), 0f)
        }
        c.drawLine(92f, 444f, width - 92f, 444f, divPaint)

        val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#A699B8"); textSize = 28f }
        val valPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE; textSize = 30f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.RIGHT
        }

        var y = 512f
        rows.forEach { (k, v) ->
            c.drawText(k, 92f, y, labelPaint)
            val safeV = if (v.length > 32) v.take(31) + "…" else v
            c.drawText(safeV, width - 92f, y, valPaint)
            y += rowSpacing
        }

        if (proofBmp != null) {
            y += 16f
            c.drawLine(92f, y - 24f, width - 92f, y - 24f, divPaint)
            c.drawText("Attached Proof", 92f, y + 12f, labelPaint)
            val imgTop = y + 36f
            val imgRect = RectF(92f, imgTop, width - 92f, imgTop + 360f)
            val sc = c.save()
            val clipP = Path().apply { addRoundRect(imgRect, 24f, 24f, Path.Direction.CW) }
            c.clipPath(clipP)
            c.drawBitmap(proofBmp, null, imgRect, Paint(Paint.FILTER_BITMAP_FLAG))
            c.restoreToCount(sc)
        }

        val footPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#A699B8"); textSize = 24f; textAlign = Paint.Align.CENTER
        }
        c.drawText("Generated offline by PartnerLedger", width / 2f, height - 42f, footPaint)

        val file = File(context.cacheDir, "Receipt_${entry.date}_${entry.id.take(6)}.png")
        FileOutputStream(file).use { bmp.compress(Bitmap.CompressFormat.PNG, 95, it) }
        shareFile(context, file, "image/png", "Share Receipt Image")
    }

    // ---- WhatsApp Shareable Image Statements (PNG) ----
    fun shareStatementAsImage(context: Context, biz: BusinessConfig, stmt: StatementSnapshot) {
        val width = 1080
        val rowHeight = 115
        val height = (760 + stmt.partnerLines.size * rowHeight + 140).coerceAtLeast(1080)
        val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        c.drawColor(Color.parseColor("#050505"))

        val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#3B0A6B") }
        c.drawRect(0f, 0f, width.toFloat(), 240f, headerPaint)

        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE; textSize = 52f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#A699B8"); textSize = 30f
        }
        val cyanPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#00E5FF"); textSize = 28f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        c.drawText(biz.businessName, 56f, 96f, titlePaint)
        c.drawText("STATEMENT #${stmt.number}  •  AS OF ${stmt.asOfDate}", 56f, 152f, cyanPaint)
        c.drawText("Prepared by ${biz.adminName}  |  Currency: ${biz.currency}", 56f, 200f, subPaint)

        val cardPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#160826") }
        val sectionPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE; textSize = 34f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#A699B8"); textSize = 28f
        }
        val valuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE; textSize = 32f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val greenVal = Paint(valuePaint).apply { color = Color.parseColor("#00E676") }
        val redVal = Paint(valuePaint).apply { color = Color.parseColor("#FF4D6D") }

        c.drawRoundRect(RectF(44f, 276f, width - 44f, 670f), 28f, 28f, cardPaint)
        c.drawText("Business Summary", 80f, 336f, sectionPaint)

        fun drawMetric(label: String, value: String, x: Float, y: Float, p: Paint = valuePaint) {
            c.drawText(label, x, y, labelPaint)
            c.drawText(value, x, y + 40f, p)
        }

        drawMetric("Total Invested", formatMoney(biz.currency, stmt.totalInvested), 80f, 396f, greenVal)
        drawMetric("Total Revenue", formatMoney(biz.currency, stmt.totalRevenue), 560f, 396f, greenVal)
        drawMetric("Total Expenses", formatMoney(biz.currency, stmt.totalExpenses), 80f, 496f, redVal)
        drawMetric("Total Withdrawn", formatMoney(biz.currency, stmt.totalWithdrawn), 560f, 496f)
        drawMetric(
            "Net Profit / Loss",
            formatMoney(biz.currency, stmt.netProfit),
            80f,
            596f,
            if (stmt.netProfit >= 0) greenVal else redVal
        )
        drawMetric("Cash Balance", formatMoney(biz.currency, stmt.currentBalance), 560f, 596f, valuePaint)

        val pTop = 706f
        val pBottom = pTop + 90f + stmt.partnerLines.size * rowHeight
        c.drawRoundRect(RectF(44f, pTop, width - 44f, pBottom), 28f, 28f, cardPaint)
        c.drawText("Partner Equity Breakdown", 80f, pTop + 56f, sectionPaint)

        val linePaint = Paint().apply { color = Color.parseColor("#338A4FFF"); strokeWidth = 2f }
        var y = pTop + 110f
        stmt.partnerLines.forEachIndexed { idx, p ->
            if (idx > 0) c.drawLine(80f, y - 34f, width - 80f, y - 34f, linePaint)
            c.drawText("${p.partnerName}  (Cap %.1f%% | Agreed %.1f%%)".format(Locale.US, p.capitalSharePct, p.agreedOwnershipPct), 80f, y, valuePaint)
            c.drawText(
                "Invested: ${formatMoney(biz.currency, p.invested)}   •   Profit: ${formatMoney(biz.currency, p.profitShare)}   •   Net: ${formatMoney(biz.currency, p.netPosition)}",
                80f,
                y + 38f,
                labelPaint
            )
            y += rowHeight
        }

        val footPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#A699B8"); textSize = 24f }
        c.drawText("Generated offline by PartnerLedger  •  ${stmt.auditSummary}", 56f, height - 48f, footPaint)

        val safe = biz.businessName.replace(Regex("[^A-Za-z0-9]"), "_").ifBlank { "PartnerLedger" }
        val file = File(context.cacheDir, "${safe}_Statement_${stmt.number}.png")
        FileOutputStream(file).use { bmp.compress(Bitmap.CompressFormat.PNG, 95, it) }
        shareFile(context, file, "image/png", "Share Statement #${stmt.number} Image")
    }

    fun sharePartnerAsImage(context: Context, biz: BusinessConfig, ps: PartnerSummary) {
        val width = 1080
        val height = 980
        val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        c.drawColor(Color.parseColor("#050505"))

        val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#3B0A6B") }
        c.drawRect(0f, 0f, width.toFloat(), 230f, headerPaint)

        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE; textSize = 50f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val cyanPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#00E5FF"); textSize = 30f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#A699B8"); textSize = 28f
        }
        val today = SimpleDateFormat("dd MMM yyyy", Locale.US).format(Date())

        c.drawText(ps.partner.name, 56f, 94f, titlePaint)
        c.drawText("PARTNER STATEMENT  •  ${biz.businessName}", 56f, 148f, cyanPaint)
        c.drawText("Date: $today  |  Currency: ${biz.currency}", 56f, 196f, subPaint)

        val cardPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#160826") }
        c.drawRoundRect(RectF(44f, 268f, width - 44f, 880f), 28f, 28f, cardPaint)

        val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#A699B8"); textSize = 30f }
        val valuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE; textSize = 38f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val greenVal = Paint(valuePaint).apply { color = Color.parseColor("#00E676") }

        fun row(label: String, value: String, y: Float, p: Paint = valuePaint) {
            c.drawText(label, 84f, y, labelPaint)
            c.drawText(value, 84f, y + 48f, p)
        }

        row("Total Invested", formatMoney(biz.currency, ps.invested), 344f, greenVal)
        row("Total Withdrawn", formatMoney(biz.currency, ps.withdrawn), 460f)
        row("Capital Share / Agreed Ownership", "%.1f%%  /  %.1f%%".format(Locale.US, ps.capitalSharePct, ps.partner.agreedOwnershipPct), 576f)
        row("Profit Share (ROI %.1f%%)".format(Locale.US, ps.roiPct), formatMoney(biz.currency, ps.profitShare), 692f, greenVal)
        row("Net Position", formatMoney(biz.currency, ps.netPosition), 808f, cyanPaint)

        val footPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#A699B8"); textSize = 24f }
        c.drawText("Generated offline by PartnerLedger", 56f, height - 40f, footPaint)

        val safe = ps.partner.name.replace(Regex("[^A-Za-z0-9]"), "_").ifBlank { "Partner" }
        val file = File(context.cacheDir, "Partner_${safe}_Card.png")
        FileOutputStream(file).use { bmp.compress(Bitmap.CompressFormat.PNG, 95, it) }
        shareFile(context, file, "image/png", "Share ${ps.partner.name} Image")
    }

    // ---- PDF Statements ----
    fun shareNumberedStatementPdf(context: Context, biz: BusinessConfig, stmt: StatementSnapshot) {
        val pdf = PdfDocument()
        val page = pdf.startPage(PdfDocument.PageInfo.Builder(595, 842, 1).create())
        val c = page.canvas

        val title = Paint().apply { textSize = 18f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD) }
        val head = Paint().apply { textSize = 13f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD) }
        val body = Paint().apply { textSize = 11f }
        val small = Paint().apply { textSize = 9.5f }

        var y = 46f
        c.drawText("${biz.businessName} - Statement #${stmt.number}", 40f, y, title); y += 20f
        c.drawText("As of ${stmt.asOfDate}  |  Prepared by ${biz.adminName}  |  Currency: ${biz.currency}", 40f, y, small); y += 28f

        c.drawText("Business Totals", 40f, y, head); y += 18f
        c.drawText("Total Capital Invested: ${biz.currency} %,.2f".format(Locale.US, stmt.totalInvested), 50f, y, body); y += 16f
        c.drawText("Total Revenue: ${biz.currency} %,.2f".format(Locale.US, stmt.totalRevenue), 50f, y, body); y += 16f
        c.drawText("Total Expenses: ${biz.currency} %,.2f".format(Locale.US, stmt.totalExpenses), 50f, y, body); y += 16f
        c.drawText("Total Withdrawn: ${biz.currency} %,.2f".format(Locale.US, stmt.totalWithdrawn), 50f, y, body); y += 16f
        c.drawText("Net Profit / Loss: ${biz.currency} %,.2f".format(Locale.US, stmt.netProfit), 50f, y, body); y += 16f
        c.drawText("Calculated Balance: ${biz.currency} %,.2f".format(Locale.US, stmt.currentBalance), 50f, y, head); y += 28f

        c.drawText("Partner Equity & Net Position", 40f, y, head); y += 18f
        stmt.partnerLines.forEach { p ->
            c.drawText(
                "${p.partnerName}: Invested %,.2f | Withdrawn %,.2f | Cap %.1f%% | Agreed %.1f%%".format(
                    Locale.US, p.invested, p.withdrawn, p.capitalSharePct, p.agreedOwnershipPct
                ),
                50f, y, body
            ); y += 15f
            c.drawText(
                "   Profit Share: ${biz.currency} %,.2f  |  Net Position: ${biz.currency} %,.2f".format(
                    Locale.US, p.profitShare, p.netPosition
                ),
                50f, y, small
            ); y += 20f
        }

        y += 8f
        c.drawText("Audit Log Summary", 40f, y, head); y += 18f
        c.drawText(stmt.auditSummary, 50f, y, body); y += 28f
        c.drawText("Immutable offline snapshot generated by PartnerLedger.", 40f, y, small)

        pdf.finishPage(page)
        val safe = biz.businessName.replace(Regex("[^A-Za-z0-9]"), "_").ifBlank { "PartnerLedger" }
        val file = File(context.cacheDir, "${safe}_Statement_${stmt.number}.pdf")
        FileOutputStream(file).use { pdf.writeTo(it) }
        pdf.close()
        shareFile(context, file, "application/pdf", "Share Statement #${stmt.number}")
    }

    fun sharePartnerStatementPdf(context: Context, biz: BusinessConfig, ps: PartnerSummary, entries: List<LedgerEntry>) {
        val pdf = PdfDocument()
        val page = pdf.startPage(PdfDocument.PageInfo.Builder(595, 842, 1).create())
        val c = page.canvas

        val title = Paint().apply { textSize = 17f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD) }
        val head = Paint().apply { textSize = 12.5f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD) }
        val body = Paint().apply { textSize = 10.5f }
        val small = Paint().apply { textSize = 9.5f }

        val today = SimpleDateFormat("dd MMM yyyy", Locale.US).format(Date())
        var y = 46f
        c.drawText("${biz.businessName} - Partner Statement", 40f, y, title); y += 20f
        c.drawText("Partner: ${ps.partner.name}  |  Date: $today  |  Currency: ${biz.currency}", 40f, y, small); y += 26f

        c.drawText("Summary", 40f, y, head); y += 18f
        c.drawText("Total Invested: ${biz.currency} %,.2f".format(Locale.US, ps.invested), 50f, y, body); y += 16f
        c.drawText("Total Withdrawn: ${biz.currency} %,.2f".format(Locale.US, ps.withdrawn), 50f, y, body); y += 16f
        c.drawText("Capital Share: %.1f%%  |  Agreed Ownership: %.1f%%".format(Locale.US, ps.capitalSharePct, ps.partner.agreedOwnershipPct), 50f, y, body); y += 16f
        c.drawText("Profit Share: ${biz.currency} %,.2f  |  ROI: %.1f%%".format(Locale.US, ps.profitShare, ps.roiPct), 50f, y, body); y += 16f
        c.drawText("Net Position: ${biz.currency} %,.2f".format(Locale.US, ps.netPosition), 50f, y, head); y += 16f
        if (ps.owedByBusiness > 0.0) {
            c.drawText("Reimbursement Owed (Settle-Up): ${biz.currency} %,.2f".format(Locale.US, ps.owedByBusiness), 50f, y, body); y += 16f
        }

        y += 12f
        c.drawText("Recent Activity", 40f, y, head); y += 18f
        entries.take(25).forEach { e ->
            if (y < 790f) {
                val orig = formatEntryOriginal(e, biz.currency)?.let { " ($it)" } ?: ""
                val tag = if (e.type == EntryType.EXPENSE && e.paidByPartnerId == ps.partner.id && e.countAsInvestment) " [As Inv]" else ""
                c.drawText("${e.date} | ${e.type.label}$tag | ${biz.currency} %,.2f$orig | ${e.note}".format(Locale.US, e.amount), 50f, y, small)
                y += 15f
            }
        }

        pdf.finishPage(page)
        val safe = ps.partner.name.replace(Regex("[^A-Za-z0-9]"), "_").ifBlank { "Partner" }
        val file = File(context.cacheDir, "Partner_${safe}_Statement.pdf")
        FileOutputStream(file).use { pdf.writeTo(it) }
        pdf.close()
        shareFile(context, file, "application/pdf", "Share ${ps.partner.name} Statement")
    }

    fun exportLedgerCsv(context: Context, state: AppState) {
        val biz = state.business ?: return
        val pMap = state.partners.associateBy { it.id }
        val cMap = state.categories.associateBy { it.id }
        val fmt = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)

        val sb = StringBuilder()
        sb.appendLine("ID,Status,Type,Date,Partner,Category,Vendor_Source,Amount,Currency,OrigCurrency,OrigAmount,ExchangeRate,Method,PaidBy,CountAsInvestment,HasReceiptPhoto,Note,EnteredAt")
        state.entries.forEach { e ->
            val status = if (e.isDeleted) "DELETED" else "ACTIVE"
            val pName = pMap[e.partnerId]?.name ?: ""
            val cName = cMap[e.categoryId]?.name ?: ""
            val paidBy = if (e.paidByPartnerId == null) "Business Cash" else (pMap[e.paidByPartnerId]?.name ?: "Partner")
            sb.appendLine(
                listOf(
                    e.id, status, e.type.name, e.date, csvCell(pName), csvCell(cName),
                    csvCell(e.vendorOrSource), e.amount.toString(), biz.currency,
                    e.originalCurrency.ifBlank { biz.currency },
                    if (e.originalAmount > 0.0) e.originalAmount.toString() else e.amount.toString(),
                    e.exchangeRate.toString(), e.paymentMethod, csvCell(paidBy),
                    e.countAsInvestment.toString(), e.receiptImageBase64.isNotBlank().toString(),
                    csvCell(e.note), fmt.format(Date(e.createdAt))
                ).joinToString(",")
            )
        }
        sb.appendLine()
        sb.appendLine("AUDIT_ID,ACTION,RECORD_TYPE,RECORD_ID,OLD_VALUE,NEW_VALUE,TIMESTAMP")
        state.auditLogs.forEach { log ->
            sb.appendLine(
                listOf(
                    log.id, log.action, log.recordType, log.recordId,
                    csvCell(log.oldValue), csvCell(log.newValue), fmt.format(Date(log.timestamp))
                ).joinToString(",")
            )
        }

        val safe = biz.businessName.replace(Regex("[^A-Za-z0-9]"), "_").ifBlank { "PartnerLedger" }
        val tag = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        val file = File(context.cacheDir, "${safe}_Ledger_${tag}.csv")
        file.writeText(sb.toString())
        shareFile(context, file, "text/csv", "Export Accountant CSV")
    }

    fun exportBackupFile(context: Context, biz: BusinessConfig, jsonContent: String, password: String) {
        val safe = biz.businessName.replace(Regex("[^A-Za-z0-9]"), "_").ifBlank { "PartnerLedger" }
        val tag = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        val file = File(context.cacheDir, "${safe}_backup_${tag}.ptb")
        val payload = if (password.isBlank()) "PLAIN::$jsonContent" else "ENC::" + encrypt(jsonContent, password)
        file.writeText(payload)
        shareFile(context, file, "application/octet-stream", "Save Backup to Google Drive")
    }

    fun decodeBackupPayload(raw: String, password: String): Result<String> {
        return when {
            raw.startsWith("PLAIN::") -> Result.success(raw.removePrefix("PLAIN::"))
            raw.startsWith("ENC::") -> {
                if (password.isBlank()) Result.failure(IllegalArgumentException("PASSWORD_REQUIRED"))
                else try {
                    Result.success(decrypt(raw.removePrefix("ENC::"), password))
                } catch (e: Exception) {
                    Result.failure(IllegalArgumentException("WRONG_PASSWORD"))
                }
            }
            else -> Result.success(raw)
        }
    }

    private fun deriveKey(password: String): SecretKeySpec {
        val digest = MessageDigest.getInstance("SHA-256")
        val bytes = digest.digest(password.toByteArray(StandardCharsets.UTF_8))
        return SecretKeySpec(bytes, "AES")
    }

    private fun encrypt(plain: String, password: String): String {
        val cipher = Cipher.getInstance("AES/ECB/PKCS5Padding")
        cipher.init(Cipher.ENCRYPT_MODE, deriveKey(password))
        val enc = cipher.doFinal(plain.toByteArray(StandardCharsets.UTF_8))
        return Base64.encodeToString(enc, Base64.NO_WRAP)
    }

    private fun decrypt(encoded: String, password: String): String {
        val cipher = Cipher.getInstance("AES/ECB/PKCS5Padding")
        cipher.init(Cipher.DECRYPT_MODE, deriveKey(password))
        val dec = Base64.decode(encoded, Base64.NO_WRAP)
        return String(cipher.doFinal(dec), StandardCharsets.UTF_8)
    }

    private fun shareFile(context: Context, file: File, mime: String, title: String) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mime
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, title))
    }
}
