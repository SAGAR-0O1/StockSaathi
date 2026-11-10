package com.example.util

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.print.PrintAttributes
import android.print.PrintManager
import android.provider.MediaStore
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.data.model.TransactionEntity
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ExportUtils {

    /**
     * Saves raw bytes or text to Downloads/StockSaathi directory.
     * Uses MediaStore.Downloads on Android 10+ (API 29+) and direct File on API 24-28.
     */
    fun saveFileToDownloads(
        context: Context,
        fileName: String,
        mimeType: String,
        content: ByteArray
    ): Uri? {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                    put(MediaStore.MediaColumns.RELATIVE_PATH, "Download/StockSaathi")
                    put(MediaStore.MediaColumns.IS_PENDING, 1)
                }

                val resolver = context.contentResolver
                val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                    ?: return null

                resolver.openOutputStream(uri)?.use { outputStream ->
                    outputStream.write(content)
                    outputStream.flush()
                }

                contentValues.clear()
                contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                resolver.update(uri, contentValues, null, null)
                return uri
            } else {
                val downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val targetDir = File(downloadDir, "StockSaathi")
                if (!targetDir.exists()) {
                    targetDir.mkdirs()
                }
                val targetFile = File(targetDir, fileName)
                FileOutputStream(targetFile).use { out ->
                    out.write(content)
                    out.flush()
                }
                return Uri.fromFile(targetFile)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    /**
     * Saves CSV with UTF-8 Byte Order Mark (\uFEFF) for Microsoft Excel compatibility.
     */
    fun saveCsvReport(
        context: Context,
        fileName: String,
        csvContent: String
    ): Boolean {
        return try {
            val bom = byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte())
            val textBytes = csvContent.toByteArray(Charsets.UTF_8)
            val combined = ByteArray(bom.size + textBytes.size)
            System.arraycopy(bom, 0, combined, 0, bom.size)
            System.arraycopy(textBytes, 0, combined, bom.size, textBytes.size)

            val uri = saveFileToDownloads(context, fileName, "text/csv", combined)
            if (uri != null) {
                Toast.makeText(context, "Saved to Downloads/StockSaathi/$fileName", Toast.LENGTH_LONG).show()
                true
            } else {
                Toast.makeText(context, "Failed to save CSV file", Toast.LENGTH_SHORT).show()
                false
            }
        } catch (e: Exception) {
            Toast.makeText(context, "Error saving CSV: ${e.message}", Toast.LENGTH_SHORT).show()
            false
        }
    }

    /**
     * Prints an HTML document using Android PrintManager.
     */
    fun printHtml(context: Context, jobName: String, htmlContent: String) {
        try {
            val webView = WebView(context)
            webView.webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView, url: String) {
                    val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager
                    if (printManager != null) {
                        val printAdapter = webView.createPrintDocumentAdapter(jobName)
                        val attributes = PrintAttributes.Builder()
                            .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
                            .setColorMode(PrintAttributes.COLOR_MODE_COLOR)
                            .build()
                        printManager.print(jobName, printAdapter, attributes)
                    }
                }
            }
            webView.loadDataWithBaseURL(null, htmlContent, "text/html; charset=UTF-8", "UTF-8", null)
        } catch (e: Exception) {
            Toast.makeText(context, "Printing error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Formats slip number as S-000123 (Sale) or P-000123 (Purchase).
     */
    fun getSlipNumber(tx: TransactionEntity): String {
        val prefix = if (tx.transactionType == "SALE") "S" else "P"
        return String.format(Locale.US, "%s-%06d", prefix, tx.id)
    }

    /**
     * Generates a printable, styled HTML slip for a transaction.
     */
    fun generateSlipHtml(tx: TransactionEntity, storeName: String = "StockSaathi Store"): String {
        val slipNo = getSlipNumber(tx)
        val dateFormat = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
        val dateStr = dateFormat.format(Date(tx.timestamp))
        val isCancelled = tx.status == "CANCELLED"
        val isSale = tx.transactionType == "SALE"

        val cancelledWatermark = if (isCancelled) {
            """<div style="position:absolute;top:35%;left:15%;transform:rotate(-30deg);font-size:55px;font-weight:900;color:rgba(220,38,38,0.22);border:5px dashed #DC2626;padding:12px 28px;text-align:center;letter-spacing:6px;">CANCELLED</div>"""
        } else ""

        return """
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="utf-8">
            <style>
                body { font-family: 'Segoe UI', Roboto, sans-serif; margin: 24px; color: #1e293b; position: relative; }
                .slip-container { max-width: 500px; margin: 0 auto; border: 1px solid #cbd5e1; border-radius: 12px; padding: 24px; box-shadow: 0 4px 6px -1px rgba(0,0,0,0.1); }
                .header { text-align: center; border-bottom: 2px dashed #94a3b8; padding-bottom: 14px; margin-bottom: 16px; }
                .store-title { font-size: 22px; font-weight: bold; color: #0f1b2b; letter-spacing: 0.5px; }
                .subtitle { font-size: 13px; color: #64748b; margin-top: 4px; }
                .badge { display: inline-block; padding: 4px 10px; font-size: 12px; font-weight: bold; border-radius: 6px; background-color: ${if (isSale) "#dcfce7; color: #15803d" else "#e0f2fe; color: #0369a1"}; margin-top: 8px; }
                .meta-row { display: flex; justify-content: space-between; font-size: 13px; margin: 6px 0; }
                .meta-label { color: #64748b; }
                .meta-val { font-weight: 600; color: #0f172a; }
                table { width: 100%; border-collapse: collapse; margin: 16px 0; font-size: 13px; }
                th { background-color: #f1f5f9; text-align: left; padding: 8px 10px; color: #334155; font-size: 12px; border-bottom: 1px solid #cbd5e1; }
                td { padding: 10px 10px; border-bottom: 1px solid #e2e8f0; }
                .total-section { border-top: 2px dashed #94a3b8; padding-top: 12px; margin-top: 12px; }
                .total-row { display: flex; justify-content: space-between; font-size: 16px; font-weight: bold; color: #0f172a; }
                .footer { text-align: center; font-size: 12px; color: #94a3b8; margin-top: 20px; }
            </style>
        </head>
        <body>
            <div class="slip-container">
                $cancelledWatermark
                <div class="header">
                    <div class="store-title">$storeName</div>
                    <div class="subtitle">Smart Inventory & Business Billing</div>
                    <div class="badge">${if (isSale) "TAX INVOICE / SALE SLIP" else "GOODS INWARD / PURCHASE SLIP"}</div>
                </div>

                <div class="meta-row">
                    <span class="meta-label">Slip Number:</span>
                    <span class="meta-val">$slipNo</span>
                </div>
                <div class="meta-row">
                    <span class="meta-label">Date & Time:</span>
                    <span class="meta-val">$dateStr</span>
                </div>
                ${if (!tx.supplierName.isNullOrBlank()) """
                <div class="meta-row">
                    <span class="meta-label">Supplier / Party:</span>
                    <span class="meta-val">${tx.supplierName}</span>
                </div>""" else ""}
                <div class="meta-row">
                    <span class="meta-label">Status:</span>
                    <span class="meta-val" style="color: ${if (isCancelled) "#dc2626" else "#15803d"}; font-weight:bold;">${tx.status}</span>
                </div>

                <table>
                    <thead>
                        <tr>
                            <th>Item Description</th>
                            <th style="text-align:center;">Qty</th>
                            <th style="text-align:right;">Rate</th>
                            <th style="text-align:right;">Total</th>
                        </tr>
                    </thead>
                    <tbody>
                        <tr>
                            <td>
                                <strong>${tx.productName}</strong><br>
                                <span style="font-size:11px;color:#64748b;">SKU: ${tx.barcode}</span>
                            </td>
                            <td style="text-align:center;">${tx.quantity}</td>
                            <td style="text-align:right;">₹${String.format(Locale.US, "%.2f", tx.unitPrice)}</td>
                            <td style="text-align:right;">₹${String.format(Locale.US, "%.2f", tx.quantity * tx.unitPrice)}</td>
                        </tr>
                    </tbody>
                </table>

                <div class="total-section">
                    ${if (tx.discountAmount > 0) """
                    <div class="meta-row">
                        <span class="meta-label">Discount:</span>
                        <span class="meta-val" style="color:#dc2626;">- ₹${String.format(Locale.US, "%.2f", tx.discountAmount)}</span>
                    </div>""" else ""}
                    ${if (tx.taxAmount > 0) """
                    <div class="meta-row">
                        <span class="meta-label">Tax / GST:</span>
                        <span class="meta-val">+ ₹${String.format(Locale.US, "%.2f", tx.taxAmount)}</span>
                    </div>""" else ""}
                    <div class="total-row">
                        <span>Net Payable:</span>
                        <span>₹${String.format(Locale.US, "%,.2f", tx.finalTotalAmount)}</span>
                    </div>
                </div>

                <div class="footer">
                    Thank you for your business! • StockSaathi Ledger Verified
                </div>
            </div>
        </body>
        </html>
        """.trimIndent()
    }

    /**
     * Formats slip as text for WhatsApp / SMS sharing.
     */
    fun formatSlipShareText(tx: TransactionEntity, storeName: String = "StockSaathi Store"): String {
        val slipNo = getSlipNumber(tx)
        val dateFormat = SimpleDateFormat("dd/MM/yyyy hh:mm a", Locale.getDefault())
        val dateStr = dateFormat.format(Date(tx.timestamp))
        val isSale = tx.transactionType == "SALE"

        val sb = StringBuilder()
        sb.appendLine("🧾 *$storeName*")
        sb.appendLine(if (isSale) "*TAX INVOICE / SALE SLIP*" else "*PURCHASE SLIP*")
        if (tx.status == "CANCELLED") {
            sb.appendLine("⚠️ *[TRANSACTION CANCELLED / REVERSED]*")
        }
        sb.appendLine("─────────────────────")
        sb.appendLine("Slip No: $slipNo")
        sb.appendLine("Date: $dateStr")
        if (!tx.supplierName.isNullOrBlank()) {
            sb.appendLine("Supplier: ${tx.supplierName}")
        }
        sb.appendLine("─────────────────────")
        sb.appendLine("Item: ${tx.productName}")
        sb.appendLine("Barcode: ${tx.barcode}")
        sb.appendLine("Qty: ${tx.quantity}  x  ₹${String.format(Locale.US, "%.2f", tx.unitPrice)}")
        if (tx.discountAmount > 0) {
            sb.appendLine("Discount: -₹${String.format(Locale.US, "%.2f", tx.discountAmount)}")
        }
        if (tx.taxAmount > 0) {
            sb.appendLine("Tax: +₹${String.format(Locale.US, "%.2f", tx.taxAmount)}")
        }
        sb.appendLine("─────────────────────")
        sb.appendLine("*Total Amount: ₹${String.format(Locale.US, "%,.2f", tx.finalTotalAmount)}*")
        sb.appendLine("Stock: ${tx.previousStock} ➔ ${tx.resultingStock}")
        sb.appendLine("─────────────────────")
        sb.appendLine("Generated via StockSaathi BI Assistant")
        return sb.toString()
    }

    fun shareText(context: Context, text: String, title: String = "Share Slip") {
        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, text)
            type = "text/plain"
        }
        val shareIntent = Intent.createChooser(sendIntent, title)
        context.startActivity(shareIntent)
    }
}
