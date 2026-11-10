package com.example.util

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.example.data.database.StockDatabase
import com.example.data.model.CustomerEntity
import com.example.data.model.ExpenseEntity
import com.example.data.model.ProductEntity
import com.example.data.model.SupplierEntity
import com.example.data.model.TransactionEntity
import com.example.data.model.TransactionItemEntity
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object BackupUtils {

    suspend fun createLocalBackup(context: Context, db: StockDatabase): String {
        val products = db.productDao().getAllProductsList()
        val suppliers = db.supplierDao().getAllSuppliersList()
        val customers = db.customerDao().getAllCustomersList()
        val transactions = db.transactionDao().getAllTransactionsList()
        val items = db.transactionItemDao().getAllItemsSync()
        val expenses = db.expenseDao().getAllExpensesList()

        val root = JSONObject().apply {
            put("version", 3)
            put("timestamp", System.currentTimeMillis())
            put("appName", "StockSaathi")
            put("productsCount", products.size)
            put("transactionsCount", transactions.size)

            val pArray = JSONArray()
            for (p in products) {
                pArray.put(JSONObject().apply {
                    put("id", p.id)
                    put("barcode", p.barcode)
                    put("name", p.name)
                    put("category", p.category)
                    put("currentStock", p.currentStock)
                    put("minStockThreshold", p.minStockThreshold)
                    put("purchaseCostPrice", p.purchaseCostPrice)
                    put("sellingPrice", p.sellingPrice)
                    put("taxRatePercent", p.taxRatePercent)
                    put("unit", p.unit)
                    put("expiryDate", p.expiryDate ?: JSONObject.NULL)
                })
            }
            put("products", pArray)

            val sArray = JSONArray()
            for (s in suppliers) {
                sArray.put(JSONObject().apply {
                    put("id", s.id)
                    put("name", s.name)
                    put("contactNumber", s.contactNumber)
                    put("address", s.address)
                    put("paymentTerms", s.paymentTerms)
                    put("outstandingPayable", s.outstandingPayable)
                })
            }
            put("suppliers", sArray)

            val cArray = JSONArray()
            for (c in customers) {
                cArray.put(JSONObject().apply {
                    put("id", c.id)
                    put("name", c.name)
                    put("mobileNumber", c.mobileNumber)
                    put("email", c.email ?: JSONObject.NULL)
                    put("address", c.address ?: JSONObject.NULL)
                    put("creditLimit", c.creditLimit)
                    put("outstandingBalance", c.outstandingBalance)
                    put("loyaltyPoints", c.loyaltyPoints)
                })
            }
            put("customers", cArray)

            val tArray = JSONArray()
            for (t in transactions) {
                tArray.put(JSONObject().apply {
                    put("id", t.id)
                    put("transactionType", t.transactionType)
                    put("idempotencyKey", t.idempotencyKey)
                    put("invoiceNumber", t.invoiceNumber)
                    put("productId", t.productId)
                    put("productName", t.productName)
                    put("barcode", t.barcode)
                    put("supplierId", t.supplierId ?: JSONObject.NULL)
                    put("customerId", t.customerId ?: JSONObject.NULL)
                    put("quantity", t.quantity)
                    put("unitPrice", t.unitPrice)
                    put("discountAmount", t.discountAmount)
                    put("finalTotalAmount", t.finalTotalAmount)
                    put("paymentMethod", t.paymentMethod)
                    put("paymentStatus", t.paymentStatus)
                    put("timestamp", t.timestamp)
                    put("status", t.status)
                })
            }
            put("transactions", tArray)

            val eArray = JSONArray()
            for (e in expenses) {
                eArray.put(JSONObject().apply {
                    put("id", e.id)
                    put("category", e.category)
                    put("title", e.title)
                    put("amount", e.amount)
                    put("paymentMethod", e.paymentMethod)
                    put("date", e.date)
                })
            }
            put("expenses", eArray)
        }

        val backupContent = root.toString(2)
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val fileName = "StockSaathi_Backup_$timeStamp.json"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val resolver = context.contentResolver
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, "application/json")
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/StockSaathi/Backups")
            }
            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                ?: throw RuntimeException("Failed to create backup entry in MediaStore.")

            resolver.openOutputStream(uri)?.use { os ->
                os.write(backupContent.toByteArray(Charsets.UTF_8))
                os.flush()
            }
            return "Downloads/StockSaathi/Backups/$fileName"
        } else {
            val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "StockSaathi/Backups")
            dir.mkdirs()
            val file = File(dir, fileName)
            FileOutputStream(file).use { fos ->
                fos.write(backupContent.toByteArray(Charsets.UTF_8))
                fos.flush()
            }
            return file.absolutePath
        }
    }
}
