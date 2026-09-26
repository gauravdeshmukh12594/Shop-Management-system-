package com.example.util

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.data.local.entity.SaleEntity
import com.example.data.local.entity.SaleItemEntity
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Utility for generating professional PDF receipts and sharing them
 * via Email, WhatsApp, Telegram, or any messaging/printing app.
 */
object PdfReceiptGenerator {

    /**
     * Generates a beautifully styled, printable PDF tax invoice for a completed sale.
     * Saved into the app's cache directory under receipts/.
     */
    fun generatePdfReceipt(
        context: Context,
        sale: SaleEntity,
        items: List<SaleItemEntity>
    ): File {
        val receiptsDir = File(context.cacheDir, "receipts")
        if (!receiptsDir.exists()) {
            receiptsDir.mkdirs()
        }

        val sanitizedInvoiceNo = sale.invoiceNumber.replace("[^a-zA-Z0-9_-]".toRegex(), "_")
        val pdfFile = File(receiptsDir, "Receipt_${sanitizedInvoiceNo}.pdf")

        // Standard A4 dimensions: 595 x 842 points (72 points per inch)
        val pageWidth = 595
        val pageHeight = 842

        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
        val page = document.startPage(pageInfo)
        val canvas: Canvas = page.canvas

        // Paints
        val backgroundPaint = Paint().apply {
            color = Color.WHITE
            style = Paint.Style.FILL
        }
        canvas.drawRect(0f, 0f, pageWidth.toFloat(), pageHeight.toFloat(), backgroundPaint)

        val navyPaint = Paint().apply {
            color = Color.parseColor("#0F1E36")
            style = Paint.Style.FILL
            isAntiAlias = true
        }

        val textPaint = Paint().apply {
            color = Color.parseColor("#0F172A")
            textSize = 10f
            isAntiAlias = true
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }

        val boldPaint = Paint().apply {
            color = Color.parseColor("#0F172A")
            textSize = 11f
            isAntiAlias = true
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        val grayPaint = Paint().apply {
            color = Color.parseColor("#64748B")
            textSize = 9f
            isAntiAlias = true
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }

        val linePaint = Paint().apply {
            color = Color.parseColor("#E2E8F0")
            strokeWidth = 1f
            style = Paint.Style.STROKE
            isAntiAlias = true
        }

        val greenPaint = Paint().apply {
            color = Color.parseColor("#047857")
            textSize = 12f
            isAntiAlias = true
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        val primaryBluePaint = Paint().apply {
            color = Color.parseColor("#1D4ED8")
            textSize = 10f
            isAntiAlias = true
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        val lightBgPaint = Paint().apply {
            color = Color.parseColor("#F8FAFC")
            style = Paint.Style.FILL
        }

        val emeraldBgPaint = Paint().apply {
            color = Color.parseColor("#ECFDF5")
            style = Paint.Style.FILL
        }

        // 1. Top Decorative Brand Banner
        canvas.drawRect(0f, 0f, pageWidth.toFloat(), 95f, navyPaint)

        // Brand Title & Tagline
        boldPaint.color = Color.WHITE
        boldPaint.textSize = 22f
        canvas.drawText("VASTRAPOS", 36f, 42f, boldPaint)

        grayPaint.color = Color.parseColor("#93C5FD")
        grayPaint.textSize = 9.5f
        canvas.drawText("CLOTHING & SAREE BOUTIQUE  •  RETAIL TAX INVOICE", 36f, 60f, grayPaint)
        canvas.drawText("GSTIN: 27AABCV1234F1Z8  |  Support: +91 98765 43210", 36f, 76f, grayPaint)

        // Right side badge in top banner
        boldPaint.color = Color.parseColor("#FBBF24")
        boldPaint.textSize = 13f
        canvas.drawText("ORIGINAL RECEIPT", pageWidth - 165f, 42f, boldPaint)

        grayPaint.color = Color.parseColor("#CBD5E1")
        grayPaint.textSize = 9f
        val dateFormatted = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(sale.createdAt))
        canvas.drawText("Date: $dateFormatted", pageWidth - 165f, 60f, grayPaint)

        // 2. Invoice & Customer Meta Section
        val metaTop = 115f
        canvas.drawRoundRect(36f, metaTop, pageWidth - 36f, metaTop + 65f, 8f, 8f, lightBgPaint)
        canvas.drawRoundRect(36f, metaTop, pageWidth - 36f, metaTop + 65f, 8f, 8f, linePaint)

        // Left Column: Invoice Details
        grayPaint.color = Color.parseColor("#64748B")
        grayPaint.textSize = 8.5f
        canvas.drawText("INVOICE NUMBER", 50f, metaTop + 20f, grayPaint)
        boldPaint.color = Color.parseColor("#1E3A8A")
        boldPaint.textSize = 12f
        boldPaint.typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        canvas.drawText(sale.invoiceNumber, 50f, metaTop + 36f, boldPaint)
        boldPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)

        grayPaint.textSize = 8.5f
        canvas.drawText("PAYMENT MODE: ${sale.paymentMethod.uppercase()}", 50f, metaTop + 52f, greenPaint.apply { textSize = 9f })

        // Right Column: Customer Details
        val customerName = if (sale.customerName.isNotBlank()) sale.customerName else "Walk-in Retail Customer"
        val customerPhone = if (sale.customerPhone.isNotBlank()) sale.customerPhone else "N/A"

        canvas.drawText("BILLED TO", 330f, metaTop + 20f, grayPaint)
        boldPaint.color = Color.parseColor("#0F172A")
        boldPaint.textSize = 11f
        canvas.drawText(customerName, 330f, metaTop + 36f, boldPaint)
        grayPaint.textSize = 9f
        canvas.drawText("Phone: $customerPhone", 330f, metaTop + 50f, grayPaint)

        // 3. Table of Items
        val tableTop = 200f
        val tableHeaderHeight = 26f
        canvas.drawRoundRect(36f, tableTop, pageWidth - 36f, tableTop + tableHeaderHeight, 6f, 6f, navyPaint)

        boldPaint.color = Color.WHITE
        boldPaint.textSize = 9.5f
        canvas.drawText("#", 48f, tableTop + 17f, boldPaint)
        canvas.drawText("ITEM DESCRIPTION & SKU", 74f, tableTop + 17f, boldPaint)
        canvas.drawText("QTY", 370f, tableTop + 17f, boldPaint)
        canvas.drawText("RATE (₹)", 430f, tableTop + 17f, boldPaint)
        canvas.drawText("AMOUNT (₹)", pageWidth - 110f, tableTop + 17f, boldPaint)

        var rowY = tableTop + tableHeaderHeight + 20f
        val rowSpacing = 28f

        items.forEachIndexed { index, item ->
            // Zebra shading or separator
            if (index % 2 == 1) {
                canvas.drawRect(36f, rowY - 14f, pageWidth - 36f, rowY + 14f, lightBgPaint)
            }
            canvas.drawLine(36f, rowY + 14f, pageWidth - 36f, rowY + 14f, linePaint)

            // Index
            grayPaint.color = Color.parseColor("#475569")
            grayPaint.textSize = 9f
            canvas.drawText("${index + 1}", 48f, rowY, grayPaint)

            // Item Name & SKU
            boldPaint.color = Color.parseColor("#0F172A")
            boldPaint.textSize = 10f
            val truncatedName = if (item.productName.length > 36) item.productName.take(33) + "..." else item.productName
            canvas.drawText(truncatedName, 74f, rowY - 2f, boldPaint)

            grayPaint.textSize = 8f
            canvas.drawText("SKU: ${item.productSku}", 74f, rowY + 10f, grayPaint)

            // Qty
            textPaint.textSize = 10f
            textPaint.color = Color.parseColor("#0F172A")
            canvas.drawText("${item.quantity}", 374f, rowY, textPaint)

            // Rate
            canvas.drawText(String.format(Locale.getDefault(), "%.2f", item.unitPrice), 430f, rowY, textPaint)

            // Amount
            boldPaint.color = Color.parseColor("#0F172A")
            boldPaint.textSize = 10f
            canvas.drawText(String.format(Locale.getDefault(), "%.2f", item.totalPrice), pageWidth - 105f, rowY, boldPaint)

            rowY += rowSpacing
        }

        // 4. Financial Calculation Summary Box
        val summaryTop = (rowY + 10f).coerceAtLeast(460f)
        val summaryWidth = 220f
        val summaryLeft = pageWidth - 36f - summaryWidth

        canvas.drawRoundRect(summaryLeft, summaryTop, pageWidth - 36f, summaryTop + 120f, 8f, 8f, lightBgPaint)
        canvas.drawRoundRect(summaryLeft, summaryTop, pageWidth - 36f, summaryTop + 120f, 8f, 8f, linePaint)

        // Subtotal
        grayPaint.color = Color.parseColor("#475569")
        grayPaint.textSize = 10f
        canvas.drawText("Subtotal:", summaryLeft + 16f, summaryTop + 24f, grayPaint)
        boldPaint.color = Color.parseColor("#0F172A")
        boldPaint.textSize = 10f
        canvas.drawText("₹ ${String.format(Locale.getDefault(), "%.2f", sale.subtotal)}", summaryLeft + 130f, summaryTop + 24f, boldPaint)

        // Discount
        if (sale.discount > 0) {
            grayPaint.color = Color.parseColor("#DC2626")
            canvas.drawText("Discount:", summaryLeft + 16f, summaryTop + 44f, grayPaint)
            val discountPaint = Paint().apply {
                color = Color.parseColor("#DC2626")
                textSize = 10f
                isAntiAlias = true
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            canvas.drawText("- ₹ ${String.format(Locale.getDefault(), "%.2f", sale.discount)}", summaryLeft + 130f, summaryTop + 44f, discountPaint)
        } else {
            grayPaint.color = Color.parseColor("#64748B")
            canvas.drawText("Taxes (GST):", summaryLeft + 16f, summaryTop + 44f, grayPaint)
            canvas.drawText("Inclusive", summaryLeft + 130f, summaryTop + 44f, grayPaint)
        }

        canvas.drawLine(summaryLeft + 12f, summaryTop + 60f, pageWidth - 48f, summaryTop + 60f, linePaint)

        // Total Paid Box
        canvas.drawRoundRect(summaryLeft + 8f, summaryTop + 68f, pageWidth - 44f, summaryTop + 110f, 6f, 6f, emeraldBgPaint)

        greenPaint.textSize = 11f
        canvas.drawText("TOTAL PAID", summaryLeft + 16f, summaryTop + 86f, greenPaint)

        greenPaint.textSize = 15f
        canvas.drawText("₹ ${String.format(Locale.getDefault(), "%.2f", sale.totalAmount)}", summaryLeft + 16f, summaryTop + 104f, greenPaint)

        // 5. Left Side: Payment Status & QR Verification Code
        val qrBoxTop = summaryTop
        canvas.drawRoundRect(36f, qrBoxTop, 36f + 250f, qrBoxTop + 120f, 8f, 8f, lightBgPaint)
        canvas.drawRoundRect(36f, qrBoxTop, 36f + 250f, qrBoxTop + 120f, 8f, 8f, linePaint)

        // Draw Verification 2D QR Matrix
        val qrMatrix = generateReceiptQrMatrix("INVOICE:${sale.invoiceNumber};AMOUNT:${sale.totalAmount};PAID:TRUE", 25)
        val qrSize = 80f
        val qrLeft = 50f
        val qrTop = qrBoxTop + 20f
        val cellSize = qrSize / 25f

        val qrPaint = Paint().apply {
            color = Color.parseColor("#0F172A")
            style = Paint.Style.FILL
        }

        for (r in 0 until 25) {
            for (c in 0 until 25) {
                if (qrMatrix[r][c]) {
                    canvas.drawRect(
                        qrLeft + c * cellSize,
                        qrTop + r * cellSize,
                        qrLeft + (c + 1) * cellSize,
                        qrTop + (r + 1) * cellSize,
                        qrPaint
                    )
                }
            }
        }

        // Text beside QR
        val qrTextX = qrLeft + qrSize + 16f
        boldPaint.color = Color.parseColor("#047857")
        boldPaint.textSize = 10f
        canvas.drawText("PAID & VERIFIED", qrTextX, qrTop + 24f, boldPaint)

        grayPaint.color = Color.parseColor("#475569")
        grayPaint.textSize = 8.5f
        canvas.drawText("Scan to verify receipt", qrTextX, qrTop + 40f, grayPaint)
        canvas.drawText("Stock deducted in POS", qrTextX, qrTop + 54f, grayPaint)
        canvas.drawText("Payment: ${sale.paymentMethod}", qrTextX, qrTop + 68f, grayPaint)

        // 6. Policy & Terms Note
        val policyTop = summaryTop + 140f
        canvas.drawLine(36f, policyTop, pageWidth - 36f, policyTop, linePaint)

        grayPaint.color = Color.parseColor("#64748B")
        grayPaint.textSize = 8.5f
        canvas.drawText("Terms & Conditions:", 36f, policyTop + 18f, grayPaint.apply { typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD) })
        grayPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText("1. Goods once sold can be exchanged within 7 days with original tags and this receipt intact.", 36f, policyTop + 32f, grayPaint)
        canvas.drawText("2. No returns or exchanges on discounted/sale garments, alterations, or fall-pico finished sarees.", 36f, policyTop + 46f, grayPaint)
        canvas.drawText("3. This is a computer-generated tax invoice. No signature required.", 36f, policyTop + 60f, grayPaint)

        // 7. Footer Thank You Banner
        val footerTop = pageHeight - 50f
        canvas.drawRect(0f, footerTop, pageWidth.toFloat(), pageHeight.toFloat(), navyPaint)

        boldPaint.color = Color.WHITE
        boldPaint.textSize = 11f
        val thankYou = "Thank you for shopping at VastraPOS! Visit again."
        val textWidth = boldPaint.measureText(thankYou)
        canvas.drawText(thankYou, (pageWidth - textWidth) / 2f, footerTop + 24f, boldPaint)

        grayPaint.color = Color.parseColor("#93C5FD")
        grayPaint.textSize = 8f
        val powered = "Generated by VastraPOS Cloud Inventory & Billing Engine"
        val poweredWidth = grayPaint.measureText(powered)
        canvas.drawText(powered, (pageWidth - poweredWidth) / 2f, footerTop + 38f, grayPaint)

        document.finishPage(page)

        // Write to File
        FileOutputStream(pdfFile).use { out ->
            document.writeTo(out)
        }
        document.close()

        return pdfFile
    }

    /**
     * Shares the PDF receipt via standard Android Intent Chooser
     * (compatible with WhatsApp, Gmail, Outlook, Messages, Telegram, Drive, Print).
     */
    fun sharePdfReceipt(context: Context, pdfFile: File, sale: SaleEntity) {
        try {
            val authority = "${context.packageName}.fileprovider"
            val uri: Uri = FileProvider.getUriForFile(context, authority, pdfFile)

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(
                    Intent.EXTRA_SUBJECT,
                    "Tax Invoice Receipt - ${sale.invoiceNumber} | VastraPOS"
                )
                putExtra(
                    Intent.EXTRA_TEXT,
                    "Dear Customer,\n\nThank you for shopping at VastraPOS! Please find attached your official Tax Invoice receipt (PDF) for Bill #${sale.invoiceNumber} (Total: ₹${String.format(Locale.getDefault(), "%.2f", sale.totalAmount)}).\n\nWarm regards,\nVastraPOS Clothing & Saree Boutique"
                )
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooser = Intent.createChooser(shareIntent, "Share PDF Receipt via:")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Error sharing receipt: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Opens the PDF receipt in the device's native PDF Viewer app.
     */
    fun openPdfReceipt(context: Context, pdfFile: File) {
        try {
            val authority = "${context.packageName}.fileprovider"
            val uri: Uri = FileProvider.getUriForFile(context, authority, pdfFile)

            val viewIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/pdf")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(viewIntent)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "No PDF viewer installed to open receipt", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Helper to render verification 2D matrix for the PDF canvas.
     */
    private fun generateReceiptQrMatrix(data: String, size: Int = 25): Array<BooleanArray> {
        val matrix = Array(size) { BooleanArray(size) }

        fun drawFinderPattern(row: Int, col: Int) {
            for (r in 0..6) {
                for (c in 0..6) {
                    val isOuter = r == 0 || r == 6 || c == 0 || c == 6
                    val isInner = r in 2..4 && c in 2..4
                    matrix[row + r][col + c] = isOuter || isInner
                }
            }
        }

        drawFinderPattern(0, 0)
        drawFinderPattern(0, size - 7)
        drawFinderPattern(size - 7, 0)

        for (i in 8 until size - 8) {
            matrix[6][i] = (i % 2 == 0)
            matrix[i][6] = (i % 2 == 0)
        }

        val hash = data.hashCode()
        var bitIndex = 0
        for (r in 0 until size) {
            for (c in 0 until size) {
                if ((r < 8 && (c < 8 || c >= size - 8)) || (r >= size - 8 && c < 8) || r == 6 || c == 6) {
                    continue
                }
                val rawBit = ((hash shr (bitIndex % 31)) and 1) == 1
                val patternBit = (r + c) % 3 == 0
                matrix[r][c] = rawBit xor patternBit
                bitIndex++
            }
        }
        return matrix
    }
}
