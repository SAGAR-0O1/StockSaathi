package com.example.data.repository

import androidx.room.withTransaction
import com.example.data.database.StockDatabase
import com.example.data.model.AuditLogEntity
import com.example.data.model.CashClosingEntity
import com.example.data.model.CustomerEntity
import com.example.data.model.CustomerPaymentEntity
import com.example.data.model.ExpenseEntity
import com.example.data.model.HeldBillEntity
import com.example.data.model.ProductEntity
import com.example.data.model.PromotionRuleEntity
import com.example.data.model.StockAdjustmentEntity
import com.example.data.model.SupplierEntity
import com.example.data.model.SupplierPaymentEntity
import com.example.data.model.TransactionEntity
import com.example.data.model.TransactionItemEntity
import com.example.util.FinancialMath
import kotlinx.coroutines.flow.Flow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

sealed class OperationResult<out T> {
    data class Success<out T>(val data: T, val verifiedMessage: String) : OperationResult<T>()
    data class Error(val errorMessage: String) : OperationResult<Nothing>()
}

data class TransactionReceipt(
    val transaction: TransactionEntity,
    val items: List<TransactionItemEntity> = emptyList(),
    val updatedProduct: ProductEntity? = null,
    val verifiedStock: Int = 0,
    val auditLogId: Long
)

data class SaleItemRequest(
    val productId: Long,
    val quantity: Int,
    val unitPrice: Double,
    val discountAmount: Double = 0.0,
    val taxRatePercent: Double = 0.0
)

data class PurchaseItemRequest(
    val productId: Long,
    val quantity: Int,
    val unitCostPrice: Double,
    val discountAmount: Double = 0.0,
    val taxPercent: Double = 0.0,
    val expiryDate: Long? = null,
    val batchNumber: String? = null
)

data class SaleReturnItemRequest(
    val transactionItemId: Long,
    val productId: Long,
    val quantityToReturn: Int,
    val unitRefundPrice: Double,
    val costBasis: Double
)

class InventoryRepository(private val db: StockDatabase) {
    val allProducts: Flow<List<ProductEntity>> = db.productDao().getAllProducts()
    val lowStockProducts: Flow<List<ProductEntity>> = db.productDao().getLowStockProducts()
    val allSuppliers: Flow<List<SupplierEntity>> = db.supplierDao().getAllSuppliers()
    val allCustomers: Flow<List<CustomerEntity>> = db.customerDao().getAllCustomers()
    val allTransactions: Flow<List<TransactionEntity>> = db.transactionDao().getAllTransactions()
    val allTransactionItems: Flow<List<TransactionItemEntity>> = db.transactionItemDao().getAllItems()
    val allExpenses: Flow<List<ExpenseEntity>> = db.expenseDao().getAllExpenses()
    val allStockAdjustments: Flow<List<StockAdjustmentEntity>> = db.stockAdjustmentDao().getAllStockAdjustments()
    val allCashClosings: Flow<List<CashClosingEntity>> = db.cashClosingDao().getAllCashClosings()
    val allHeldBills: Flow<List<HeldBillEntity>> = db.heldBillDao().getAllHeldBills()
    val allAuditLogs: Flow<List<AuditLogEntity>> = db.auditLogDao().getAuditLogs()
    val allPromotionRules: Flow<List<PromotionRuleEntity>> = db.promotionRuleDao().getAllPromotionRules()
    val activePromotionRules: Flow<List<PromotionRuleEntity>> = db.promotionRuleDao().getActivePromotionRules()

    suspend fun getProductById(id: Long): ProductEntity? = db.productDao().getProductById(id)
    suspend fun getProductByBarcode(barcode: String): ProductEntity? = db.productDao().getProductByBarcode(barcode)
    suspend fun searchProducts(query: String): Flow<List<ProductEntity>> = db.productDao().searchProducts(query)

    suspend fun getAllProductsSnapshot(): List<ProductEntity> = db.productDao().getAllProductsList()
    suspend fun getAllTransactionsSnapshot(): List<TransactionEntity> = db.transactionDao().getAllTransactionsList()
    suspend fun getAllTransactionItemsSnapshot(): List<TransactionItemEntity> = db.transactionItemDao().getAllItemsSync()
    suspend fun getItemsForTransaction(txId: Long): List<TransactionItemEntity> = db.transactionItemDao().getItemsForTransactionSync(txId)
    suspend fun getAllSuppliersSnapshot(): List<SupplierEntity> = db.supplierDao().getAllSuppliersList()
    suspend fun getAllCustomersSnapshot(): List<CustomerEntity> = db.customerDao().getAllCustomersList()
    suspend fun getAllExpensesSnapshot(): List<ExpenseEntity> = db.expenseDao().getAllExpensesList()

    private fun generateInvoiceNumber(prefix: String): String {
        val dateFormat = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US)
        val rand = (100..999).random()
        return "$prefix-${dateFormat.format(Date())}-$rand"
    }

    /**
     * Executes atomic multi-product sale invoice with Weighted Average Cost retention,
     * validation, customer udhaar handling, and audit logging.
     */
    suspend fun recordMultiItemSale(
        customerId: Long?,
        customerName: String?,
        items: List<SaleItemRequest>,
        billDiscountAmount: Double = 0.0,
        paymentMethod: String = "CASH", // "CASH", "UPI", "CARD", "CREDIT", "SPLIT"
        paidAmount: Double? = null,
        notes: String? = null,
        idempotencyKey: String = UUID.randomUUID().toString()
    ): OperationResult<TransactionReceipt> {
        if (items.isEmpty()) {
            return OperationResult.Error("Cannot generate invoice with zero items.")
        }

        try {
            val receipt = db.withTransaction {
                // Idempotency verification
                val existing = db.transactionDao().getTransactionByIdempotency(idempotencyKey)
                if (existing != null) {
                    throw IllegalStateException("Duplicate transaction prevented. Invoice $idempotencyKey is already committed.")
                }

                // Verify stock availability and collect entities
                val productMap = mutableMapOf<Long, ProductEntity>()
                for (req in items) {
                    if (req.quantity <= 0) {
                        throw IllegalArgumentException("Quantity must be greater than zero for all items.")
                    }
                    if (req.unitPrice < 0) {
                        throw IllegalArgumentException("Unit price cannot be negative.")
                    }
                    val prod = db.productDao().getProductById(req.productId)
                        ?: throw IllegalArgumentException("Product ID ${req.productId} not found in database.")

                    if (prod.currentStock < req.quantity) {
                        throw IllegalStateException("Insufficient stock for '${prod.name}': Available ${prod.currentStock} ${prod.unit}, requested ${req.quantity}.")
                    }
                    productMap[req.productId] = prod
                }

                val now = System.currentTimeMillis()
                val invoiceNum = generateInvoiceNumber("INV")

                var subtotal = 0.0
                var totalItemDiscounts = 0.0
                var totalTax = 0.0
                var totalQuantity = 0

                val transactionItemEntities = mutableListOf<TransactionItemEntity>()

                for (req in items) {
                    val prod = productMap[req.productId]!!
                    val lineBase = req.quantity * req.unitPrice
                    val taxable = maxOf(0.0, lineBase - req.discountAmount)
                    val tax = (taxable * req.taxRatePercent) / 100.0
                    val lineTotal = taxable + tax

                    subtotal += lineBase
                    totalItemDiscounts += req.discountAmount
                    totalTax += tax
                    totalQuantity += req.quantity

                    // Deduct stock immediately
                    val resultingStock = prod.currentStock - req.quantity
                    db.productDao().updateStock(prod.id, resultingStock, now)

                    transactionItemEntities.add(
                        TransactionItemEntity(
                            transactionId = 0, // Populated after parent insert
                            invoiceNumber = invoiceNum,
                            productId = prod.id,
                            productName = prod.name,
                            barcode = prod.barcode,
                            quantity = req.quantity,
                            unitPrice = req.unitPrice,
                            costBasis = prod.purchaseCostPrice, // Stores actual Weighted Average Cost basis at sale!
                            discountAmount = req.discountAmount,
                            taxRatePercent = req.taxRatePercent,
                            taxAmount = tax,
                            totalAmount = lineTotal,
                            unit = prod.unit
                        )
                    )
                }

                val totalDiscounts = totalItemDiscounts + billDiscountAmount
                val finalTotal = FinancialMath.roundMoney(maxOf(0.0, (subtotal - totalDiscounts) + totalTax))

                val actualPaid = paidAmount ?: if (paymentMethod == "CREDIT") 0.0 else finalTotal
                val balanceDue = FinancialMath.roundMoney(maxOf(0.0, finalTotal - actualPaid))
                val paymentStatus = when {
                    balanceDue <= 0.0 -> "PAID"
                    actualPaid > 0.0 -> "PARTIAL"
                    else -> "UNPAID"
                }

                // If customer is selected and balance remains, update customer balance
                var resolvedCustomerName = customerName
                if (customerId != null && customerId > 0) {
                    val customer = db.customerDao().getCustomerById(customerId)
                    if (customer != null) {
                        resolvedCustomerName = customer.name
                        if (balanceDue > 0.0) {
                            val newBalance = FinancialMath.roundMoney(customer.outstandingBalance + balanceDue)
                            if (customer.creditLimit > 0 && newBalance > customer.creditLimit) {
                                throw IllegalStateException("Customer credit limit exceeded: Current balance ₹${customer.outstandingBalance} + new due ₹$balanceDue > limit ₹${customer.creditLimit}")
                            }
                            db.customerDao().updateCustomerBalance(customer.id, newBalance)
                        }
                        // Award loyalty points (1 point per ₹100)
                        val pointsAwarded = (finalTotal / 100).toInt()
                        if (pointsAwarded > 0) {
                            db.customerDao().addLoyaltyPoints(customer.id, pointsAwarded)
                        }
                    }
                }

                // Primary transaction record
                val primaryProduct = productMap[items.first().productId]!!
                val parentTx = TransactionEntity(
                    transactionType = "SALE",
                    idempotencyKey = idempotencyKey,
                    productId = if (items.size == 1) primaryProduct.id else 0L,
                    productName = if (items.size == 1) primaryProduct.name else "Multi-Item Invoice (${items.size} items)",
                    barcode = if (items.size == 1) primaryProduct.barcode else "MULTI",
                    customerId = customerId,
                    customerName = resolvedCustomerName ?: "Walk-in Customer",
                    invoiceNumber = invoiceNum,
                    quantity = totalQuantity,
                    unitPrice = if (items.size == 1) items.first().unitPrice else 0.0,
                    subtotalAmount = subtotal,
                    discountAmount = totalItemDiscounts,
                    billDiscountAmount = billDiscountAmount,
                    taxAmount = totalTax,
                    finalTotalAmount = finalTotal,
                    paymentMethod = paymentMethod,
                    paymentStatus = paymentStatus,
                    paidAmount = actualPaid,
                    balanceDue = balanceDue,
                    previousStock = primaryProduct.currentStock,
                    resultingStock = primaryProduct.currentStock - items.first().quantity,
                    timestamp = now,
                    status = "COMMITTED",
                    notes = notes
                )

                val txId = db.transactionDao().insertTransaction(parentTx)
                if (txId <= 0) throw RuntimeException("Database error: Failed to save sale transaction.")

                // Insert child line items
                val readyItems = transactionItemEntities.map { it.copy(transactionId = txId) }
                db.transactionItemDao().insertItems(readyItems)

                // Audit Log
                val auditId = db.auditLogDao().insertAuditLog(
                    AuditLogEntity(
                        timestamp = now,
                        action = "SALE_INVOICE_COMMITTED",
                        entityType = "Invoice",
                        entityId = txId,
                        description = "Committed Sale $invoiceNum: ${items.size} lines, $totalQuantity units, ₹$finalTotal via $paymentMethod",
                        verifiedStock = totalQuantity
                    )
                )

                val savedTx = db.transactionDao().getTransactionById(txId)!!
                TransactionReceipt(
                    transaction = savedTx,
                    items = readyItems,
                    verifiedStock = totalQuantity,
                    auditLogId = auditId
                )
            }
            return OperationResult.Success(receipt, "Invoice #${receipt.transaction.invoiceNumber} successfully created and verified.")
        } catch (e: Exception) {
            return OperationResult.Error(e.message ?: "Failed to record sale invoice.")
        }
    }

    /**
     * Executes atomic multi-product purchase with Weighted Average Cost (WAC) updates.
     */
    suspend fun recordMultiItemPurchase(
        supplierId: Long?,
        supplierName: String?,
        items: List<PurchaseItemRequest>,
        paymentMethod: String = "BANK_TRANSFER",
        paidAmount: Double? = null,
        notes: String? = null,
        idempotencyKey: String = UUID.randomUUID().toString()
    ): OperationResult<TransactionReceipt> {
        if (items.isEmpty()) {
            return OperationResult.Error("Purchase invoice must have at least one product.")
        }

        try {
            val receipt = db.withTransaction {
                val existing = db.transactionDao().getTransactionByIdempotency(idempotencyKey)
                if (existing != null) {
                    throw IllegalStateException("Duplicate purchase invoice prevented. Key $idempotencyKey already exists.")
                }

                val now = System.currentTimeMillis()
                val invoiceNum = generateInvoiceNumber("PUR")

                var subtotal = 0.0
                var totalTax = 0.0
                var totalDiscounts = 0.0
                var totalQuantity = 0

                val transactionItemEntities = mutableListOf<TransactionItemEntity>()

                for (req in items) {
                    if (req.quantity <= 0) throw IllegalArgumentException("Quantity must be greater than zero.")
                    if (req.unitCostPrice < 0) throw IllegalArgumentException("Cost price cannot be negative.")

                    val prod = db.productDao().getProductById(req.productId)
                        ?: throw IllegalArgumentException("Product ID ${req.productId} does not exist.")

                    val lineBase = req.quantity * req.unitCostPrice
                    val taxable = maxOf(0.0, lineBase - req.discountAmount)
                    val tax = (taxable * req.taxPercent) / 100.0
                    val lineTotal = taxable + tax

                    subtotal += lineBase
                    totalDiscounts += req.discountAmount
                    totalTax += tax
                    totalQuantity += req.quantity

                    // Calculate new Weighted Average Cost (WAC)
                    val newWac = FinancialMath.calculateWeightedAverageCost(
                        currentStock = prod.currentStock,
                        currentWac = prod.purchaseCostPrice,
                        purchaseQuantity = req.quantity,
                        newPurchasePrice = req.unitCostPrice
                    )
                    val newStock = prod.currentStock + req.quantity

                    // Update stock and WAC cost atomically
                    db.productDao().updateStockAndCost(prod.id, newStock, newWac, now)
                    if (req.expiryDate != null) {
                        db.productDao().updateExpiryDate(prod.id, req.expiryDate, now)
                    }

                    transactionItemEntities.add(
                        TransactionItemEntity(
                            transactionId = 0,
                            invoiceNumber = invoiceNum,
                            productId = prod.id,
                            productName = prod.name,
                            barcode = prod.barcode,
                            quantity = req.quantity,
                            unitPrice = req.unitCostPrice,
                            costBasis = newWac,
                            discountAmount = req.discountAmount,
                            taxRatePercent = req.taxPercent,
                            taxAmount = tax,
                            totalAmount = lineTotal,
                            batchNumber = req.batchNumber,
                            expiryDate = req.expiryDate,
                            unit = prod.unit
                        )
                    )
                }

                val finalTotal = FinancialMath.roundMoney(maxOf(0.0, (subtotal - totalDiscounts) + totalTax))
                val actualPaid = paidAmount ?: finalTotal
                val balanceDue = FinancialMath.roundMoney(maxOf(0.0, finalTotal - actualPaid))
                val paymentStatus = when {
                    balanceDue <= 0.0 -> "PAID"
                    actualPaid > 0.0 -> "PARTIAL"
                    else -> "UNPAID"
                }

                // If supplier selected and balance remains, update supplier payable
                var resolvedSupplierName = supplierName
                if (supplierId != null && supplierId > 0) {
                    val supplier = db.supplierDao().getSupplierById(supplierId)
                    if (supplier != null) {
                        resolvedSupplierName = supplier.name
                        if (balanceDue > 0.0) {
                            val newPayable = FinancialMath.roundMoney(supplier.outstandingPayable + balanceDue)
                            db.supplierDao().updateOutstandingPayable(supplier.id, newPayable)
                        }
                    }
                }

                val firstProduct = db.productDao().getProductById(items.first().productId)!!
                val parentTx = TransactionEntity(
                    transactionType = "PURCHASE",
                    idempotencyKey = idempotencyKey,
                    productId = if (items.size == 1) firstProduct.id else 0L,
                    productName = if (items.size == 1) firstProduct.name else "Multi-Product Purchase (${items.size} items)",
                    barcode = if (items.size == 1) firstProduct.barcode else "MULTI",
                    supplierId = supplierId,
                    supplierName = resolvedSupplierName ?: "Direct Vendor",
                    invoiceNumber = invoiceNum,
                    quantity = totalQuantity,
                    unitPrice = if (items.size == 1) items.first().unitCostPrice else 0.0,
                    subtotalAmount = subtotal,
                    discountAmount = totalDiscounts,
                    taxAmount = totalTax,
                    finalTotalAmount = finalTotal,
                    paymentMethod = paymentMethod,
                    paymentStatus = paymentStatus,
                    paidAmount = actualPaid,
                    balanceDue = balanceDue,
                    previousStock = firstProduct.currentStock - items.first().quantity,
                    resultingStock = firstProduct.currentStock,
                    timestamp = now,
                    status = "COMMITTED",
                    notes = notes
                )

                val txId = db.transactionDao().insertTransaction(parentTx)
                if (txId <= 0) throw RuntimeException("Database error: Failed to record purchase.")

                val readyItems = transactionItemEntities.map { it.copy(transactionId = txId) }
                db.transactionItemDao().insertItems(readyItems)

                val auditId = db.auditLogDao().insertAuditLog(
                    AuditLogEntity(
                        timestamp = now,
                        action = "PURCHASE_INVOICE_COMMITTED",
                        entityType = "Invoice",
                        entityId = txId,
                        description = "Procured $totalQuantity units across ${items.size} SKUs for ₹$finalTotal (WAC updated)",
                        verifiedStock = totalQuantity
                    )
                )

                val savedTx = db.transactionDao().getTransactionById(txId)!!
                TransactionReceipt(
                    transaction = savedTx,
                    items = readyItems,
                    verifiedStock = totalQuantity,
                    auditLogId = auditId
                )
            }
            return OperationResult.Success(receipt, "Purchase #${receipt.transaction.invoiceNumber} recorded with Weighted Average Cost.")
        } catch (e: Exception) {
            return OperationResult.Error(e.message ?: "Failed to commit purchase.")
        }
    }

    /**
     * Backward-compatible single item purchase adapter
     */
    suspend fun recordPurchase(
        productId: Long,
        supplierId: Long?,
        quantity: Int,
        unitCostPrice: Double,
        discountAmount: Double = 0.0,
        taxPercent: Double = 0.0,
        expiryDate: Long? = null,
        idempotencyKey: String = UUID.randomUUID().toString(),
        notes: String? = null
    ): OperationResult<TransactionReceipt> {
        val req = PurchaseItemRequest(
            productId = productId,
            quantity = quantity,
            unitCostPrice = unitCostPrice,
            discountAmount = discountAmount,
            taxPercent = taxPercent,
            expiryDate = expiryDate
        )
        val sup = if (supplierId != null && supplierId > 0) db.supplierDao().getSupplierById(supplierId) else null
        return recordMultiItemPurchase(
            supplierId = supplierId,
            supplierName = sup?.name,
            items = listOf(req),
            notes = notes,
            idempotencyKey = idempotencyKey
        )
    }

    /**
     * Backward-compatible single item sale adapter
     */
    suspend fun recordSale(
        productId: Long,
        quantity: Int,
        sellingPrice: Double,
        discountAmount: Double = 0.0,
        idempotencyKey: String = UUID.randomUUID().toString(),
        notes: String? = null
    ): OperationResult<TransactionReceipt> {
        val prod = db.productDao().getProductById(productId)
            ?: return OperationResult.Error("Product not found.")
        val req = SaleItemRequest(
            productId = productId,
            quantity = quantity,
            unitPrice = sellingPrice,
            discountAmount = discountAmount,
            taxRatePercent = prod.taxRatePercent
        )
        return recordMultiItemSale(
            customerId = null,
            customerName = null,
            items = listOf(req),
            notes = notes,
            idempotencyKey = idempotencyKey
        )
    }

    /**
     * Process Partial or Full Sale Return with Condition Categorization
     */
    suspend fun recordSaleReturn(
        originalTransactionId: Long,
        returnItems: List<SaleReturnItemRequest>,
        returnCondition: String = "SELLABLE", // "SELLABLE", "DAMAGED", "QUARANTINED"
        refundPaymentMethod: String = "CASH",
        reason: String,
        idempotencyKey: String = UUID.randomUUID().toString()
    ): OperationResult<TransactionReceipt> {
        if (returnItems.isEmpty()) {
            return OperationResult.Error("No items selected for return.")
        }

        try {
            val receipt = db.withTransaction {
                val originalTx = db.transactionDao().getTransactionById(originalTransactionId)
                    ?: throw IllegalArgumentException("Original transaction #$originalTransactionId not found.")

                val now = System.currentTimeMillis()
                val returnInvoiceNum = generateInvoiceNumber("RET")

                var totalRefundAmount = 0.0
                var totalReturnUnits = 0

                val childReturnItems = mutableListOf<TransactionItemEntity>()

                for (req in returnItems) {
                    if (req.quantityToReturn <= 0) throw IllegalArgumentException("Return quantity must be positive.")

                    val prod = db.productDao().getProductById(req.productId)
                        ?: throw IllegalArgumentException("Product ID ${req.productId} not found.")

                    val lineRefund = req.quantityToReturn * req.unitRefundPrice
                    totalRefundAmount += lineRefund
                    totalReturnUnits += req.quantityToReturn

                    // If condition is SELLABLE, put stock back into inventory!
                    if (returnCondition == "SELLABLE") {
                        val newStock = prod.currentStock + req.quantityToReturn
                        db.productDao().updateStock(prod.id, newStock, now)
                    } else if (returnCondition == "DAMAGED") {
                        // Log damage adjustment without increasing sellable stock
                        db.stockAdjustmentDao().insertStockAdjustment(
                            StockAdjustmentEntity(
                                productId = prod.id,
                                productName = prod.name,
                                barcode = prod.barcode,
                                adjustmentType = "DAMAGE",
                                quantityDelta = 0,
                                previousStock = prod.currentStock,
                                resultingStock = prod.currentStock,
                                reason = "Returned damaged from customer: $reason",
                                timestamp = now
                            )
                        )
                    }

                    // Update returned quantity on original transaction item if item id is valid
                    if (req.transactionItemId > 0) {
                        db.transactionItemDao().recordItemReturn(req.transactionItemId, req.quantityToReturn)
                    }

                    childReturnItems.add(
                        TransactionItemEntity(
                            transactionId = 0,
                            invoiceNumber = returnInvoiceNum,
                            productId = prod.id,
                            productName = prod.name,
                            barcode = prod.barcode,
                            quantity = req.quantityToReturn,
                            unitPrice = req.unitRefundPrice,
                            costBasis = req.costBasis,
                            totalAmount = lineRefund,
                            returnedQuantity = req.quantityToReturn,
                            unit = prod.unit
                        )
                    )
                }

                // If customer had an outstanding balance, adjust it
                if (originalTx.customerId != null && originalTx.customerId > 0) {
                    val cust = db.customerDao().getCustomerById(originalTx.customerId)
                    if (cust != null && cust.outstandingBalance > 0.0) {
                        val adjustedBalance = FinancialMath.roundMoney(maxOf(0.0, cust.outstandingBalance - totalRefundAmount))
                        db.customerDao().updateCustomerBalance(cust.id, adjustedBalance)
                    }
                }

                val returnTx = TransactionEntity(
                    transactionType = "SALE_RETURN",
                    idempotencyKey = idempotencyKey,
                    productId = 0L,
                    productName = "Return for ${originalTx.invoiceNumber}",
                    barcode = "RETURN",
                    customerId = originalTx.customerId,
                    customerName = originalTx.customerName,
                    invoiceNumber = returnInvoiceNum,
                    quantity = totalReturnUnits,
                    finalTotalAmount = FinancialMath.roundMoney(totalRefundAmount),
                    paymentMethod = refundPaymentMethod,
                    paymentStatus = "PAID",
                    paidAmount = FinancialMath.roundMoney(totalRefundAmount),
                    parentTransactionId = originalTx.id,
                    returnReason = reason,
                    returnCondition = returnCondition,
                    timestamp = now,
                    status = "COMMITTED",
                    notes = "Return Reason: $reason (Condition: $returnCondition)"
                )

                val returnTxId = db.transactionDao().insertTransaction(returnTx)
                val readyChildItems = childReturnItems.map { it.copy(transactionId = returnTxId) }
                db.transactionItemDao().insertItems(readyChildItems)

                val auditId = db.auditLogDao().insertAuditLog(
                    AuditLogEntity(
                        timestamp = now,
                        action = "SALE_RETURN_PROCESSED",
                        entityType = "Return",
                        entityId = returnTxId,
                        description = "Processed return for ${originalTx.invoiceNumber}: $totalReturnUnits units, ₹$totalRefundAmount refund. Condition: $returnCondition",
                        verifiedStock = totalReturnUnits
                    )
                )

                val savedReturn = db.transactionDao().getTransactionById(returnTxId)!!
                TransactionReceipt(
                    transaction = savedReturn,
                    items = readyChildItems,
                    verifiedStock = totalReturnUnits,
                    auditLogId = auditId
                )
            }
            return OperationResult.Success(receipt, "Return #${receipt.transaction.invoiceNumber} recorded successfully.")
        } catch (e: Exception) {
            return OperationResult.Error(e.message ?: "Failed to process sale return.")
        }
    }

    /**
     * Reversal / Cancellation of transaction
     */
    suspend fun cancelTransaction(transactionId: Long, reason: String): OperationResult<Boolean> {
        try {
            db.withTransaction {
                val tx = db.transactionDao().getTransactionById(transactionId)
                    ?: throw IllegalArgumentException("Transaction #$transactionId not found.")
                if (tx.status == "CANCELLED") {
                    throw IllegalStateException("Transaction #$transactionId was already cancelled.")
                }

                val items = db.transactionItemDao().getItemsForTransactionSync(transactionId)
                val now = System.currentTimeMillis()

                if (items.isNotEmpty()) {
                    for (item in items) {
                        val prod = db.productDao().getProductById(item.productId) ?: continue
                        val newStock = when (tx.transactionType) {
                            "PURCHASE" -> maxOf(0, prod.currentStock - item.quantity)
                            "SALE" -> prod.currentStock + item.quantity
                            else -> prod.currentStock
                        }
                        db.productDao().updateStock(prod.id, newStock, now)
                    }
                } else if (tx.productId > 0) {
                    val prod = db.productDao().getProductById(tx.productId)
                    if (prod != null) {
                        val newStock = when (tx.transactionType) {
                            "PURCHASE" -> maxOf(0, prod.currentStock - tx.quantity)
                            "SALE" -> prod.currentStock + tx.quantity
                            else -> prod.currentStock
                        }
                        db.productDao().updateStock(prod.id, newStock, now)
                    }
                }

                db.transactionDao().updateTransactionStatus(transactionId, "CANCELLED")

                db.auditLogDao().insertAuditLog(
                    AuditLogEntity(
                        timestamp = now,
                        action = "TRANSACTION_CANCELLED",
                        entityType = "Transaction",
                        entityId = transactionId,
                        description = "Cancelled ${tx.transactionType} #${tx.id} (${tx.invoiceNumber}). Reason: $reason",
                        verifiedStock = 0
                    )
                )
            }
            return OperationResult.Success(true, "Transaction #$transactionId successfully reversed.")
        } catch (e: Exception) {
            return OperationResult.Error(e.message ?: "Failed to cancel transaction.")
        }
    }

    /**
     * Manual Stock Adjustment with mandatory reason
     */
    suspend fun recordStockAdjustment(
        productId: Long,
        adjustmentType: String, // "DAMAGE", "AUDIT_DISCREPANCY", "SAMPLE", "EXPIRY_WRITE_OFF", "RESTOCK"
        quantityDelta: Int,
        reason: String
    ): OperationResult<Boolean> {
        if (quantityDelta == 0) return OperationResult.Error("Adjustment quantity delta cannot be zero.")
        if (reason.isBlank()) return OperationResult.Error("Adjustment reason is required.")

        return try {
            db.withTransaction {
                val prod = db.productDao().getProductById(productId)
                    ?: throw IllegalArgumentException("Product ID $productId not found.")

                val newStock = prod.currentStock + quantityDelta
                if (newStock < 0) {
                    throw IllegalStateException("Adjustment would result in negative stock ($newStock).")
                }
                val now = System.currentTimeMillis()
                db.productDao().updateStock(prod.id, newStock, now)

                db.stockAdjustmentDao().insertStockAdjustment(
                    StockAdjustmentEntity(
                        productId = prod.id,
                        productName = prod.name,
                        barcode = prod.barcode,
                        adjustmentType = adjustmentType,
                        quantityDelta = quantityDelta,
                        previousStock = prod.currentStock,
                        resultingStock = newStock,
                        reason = reason,
                        timestamp = now
                    )
                )

                db.auditLogDao().insertAuditLog(
                    AuditLogEntity(
                        timestamp = now,
                        action = "STOCK_ADJUSTMENT",
                        entityType = "Product",
                        entityId = prod.id,
                        description = "Adjusted '${prod.name}' by $quantityDelta (${prod.currentStock} ➔ $newStock) due to $adjustmentType: $reason",
                        verifiedStock = newStock
                    )
                )
            }
            OperationResult.Success(true, "Stock adjustment recorded successfully.")
        } catch (e: Exception) {
            OperationResult.Error(e.message ?: "Failed to adjust stock.")
        }
    }

    // Customer Operations
    suspend fun createCustomer(customer: CustomerEntity): OperationResult<CustomerEntity> {
        if (customer.name.isBlank()) return OperationResult.Error("Customer name is required.")
        if (customer.mobileNumber.isBlank()) return OperationResult.Error("Mobile number is required.")

        return try {
            val id = db.customerDao().insertCustomer(customer)
            val saved = db.customerDao().getCustomerById(id)!!
            OperationResult.Success(saved, "Customer '${saved.name}' created.")
        } catch (e: Exception) {
            OperationResult.Error(e.message ?: "Failed to create customer.")
        }
    }

    suspend fun recordCustomerPayment(
        customerId: Long,
        amount: Double,
        paymentMethod: String,
        notes: String?
    ): OperationResult<Boolean> {
        if (amount <= 0.0) return OperationResult.Error("Payment amount must be greater than zero.")
        return try {
            db.withTransaction {
                val cust = db.customerDao().getCustomerById(customerId)
                    ?: throw IllegalArgumentException("Customer not found.")
                val newBalance = FinancialMath.roundMoney(maxOf(0.0, cust.outstandingBalance - amount))
                db.customerDao().updateCustomerBalance(customerId, newBalance)

                db.customerPaymentDao().insertPayment(
                    CustomerPaymentEntity(
                        customerId = customerId,
                        customerName = cust.name,
                        amount = amount,
                        paymentMethod = paymentMethod,
                        notes = notes
                    )
                )

                db.auditLogDao().insertAuditLog(
                    AuditLogEntity(
                        timestamp = System.currentTimeMillis(),
                        action = "CUSTOMER_PAYMENT_RECEIVED",
                        entityType = "Customer",
                        entityId = customerId,
                        description = "Received ₹$amount from '${cust.name}' via $paymentMethod. Outstanding balance now: ₹$newBalance",
                        verifiedStock = 0
                    )
                )
            }
            OperationResult.Success(true, "Payment of ₹$amount recorded.")
        } catch (e: Exception) {
            OperationResult.Error(e.message ?: "Failed to record payment.")
        }
    }

    // Supplier Operations
    suspend fun createSupplier(supplier: SupplierEntity): OperationResult<SupplierEntity> {
        if (supplier.name.isBlank()) return OperationResult.Error("Supplier name is required.")
        return try {
            val id = db.supplierDao().insertSupplier(supplier)
            val saved = db.supplierDao().getSupplierById(id)!!
            OperationResult.Success(saved, "Supplier '${saved.name}' added.")
        } catch (e: Exception) {
            OperationResult.Error(e.message ?: "Failed to add supplier.")
        }
    }

    suspend fun recordSupplierPayment(
        supplierId: Long,
        amount: Double,
        paymentMethod: String,
        notes: String?
    ): OperationResult<Boolean> {
        if (amount <= 0.0) return OperationResult.Error("Payment amount must be greater than zero.")
        return try {
            db.withTransaction {
                val sup = db.supplierDao().getSupplierById(supplierId)
                    ?: throw IllegalArgumentException("Supplier not found.")
                val newPayable = FinancialMath.roundMoney(maxOf(0.0, sup.outstandingPayable - amount))
                db.supplierDao().updateOutstandingPayable(supplierId, newPayable)

                db.supplierPaymentDao().insertPayment(
                    SupplierPaymentEntity(
                        supplierId = supplierId,
                        supplierName = sup.name,
                        amount = amount,
                        paymentMethod = paymentMethod,
                        notes = notes
                    )
                )

                db.auditLogDao().insertAuditLog(
                    AuditLogEntity(
                        timestamp = System.currentTimeMillis(),
                        action = "SUPPLIER_PAYMENT_MADE",
                        entityType = "Supplier",
                        entityId = supplierId,
                        description = "Paid ₹$amount to '${sup.name}' via $paymentMethod. Payable balance now: ₹$newPayable",
                        verifiedStock = 0
                    )
                )
            }
            OperationResult.Success(true, "Supplier payment of ₹$amount recorded.")
        } catch (e: Exception) {
            OperationResult.Error(e.message ?: "Failed to record supplier payment.")
        }
    }

    // Expense Operations
    suspend fun recordExpense(expense: ExpenseEntity): OperationResult<ExpenseEntity> {
        if (expense.title.isBlank()) return OperationResult.Error("Expense title is required.")
        if (expense.amount <= 0.0) return OperationResult.Error("Expense amount must be positive.")

        return try {
            val id = db.expenseDao().insertExpense(expense)
            val saved = expense.copy(id = id)
            db.auditLogDao().insertAuditLog(
                AuditLogEntity(
                    timestamp = System.currentTimeMillis(),
                    action = "EXPENSE_RECORDED",
                    entityType = "Expense",
                    entityId = id,
                    description = "Logged ₹${expense.amount} for '${expense.title}' under ${expense.category}",
                    verifiedStock = 0
                )
            )
            OperationResult.Success(saved, "Expense of ₹${saved.amount} recorded.")
        } catch (e: Exception) {
            OperationResult.Error(e.message ?: "Failed to record expense.")
        }
    }

    suspend fun deleteExpense(expense: ExpenseEntity): OperationResult<Boolean> {
        return try {
            db.expenseDao().deleteExpense(expense)
            OperationResult.Success(true, "Expense deleted.")
        } catch (e: Exception) {
            OperationResult.Error(e.message ?: "Failed to delete expense.")
        }
    }

    // Cash Closing Operations
    suspend fun recordDailyCashClosing(
        openingCash: Double,
        actualCash: Double,
        notes: String?,
        closedBy: String
    ): OperationResult<CashClosingEntity> {
        return try {
            val todayStart = SimpleDateFormat("yyyyMMdd", Locale.US).parse(SimpleDateFormat("yyyyMMdd", Locale.US).format(Date()))?.time ?: 0L
            val now = System.currentTimeMillis()

            val dayTx = db.transactionDao().getTransactionsBetweenList(todayStart, now).filter { it.status == "COMMITTED" }
            val dayExpenses = db.expenseDao().getExpensesBetweenList(todayStart, now)

            var cashSales = 0.0
            for (tx in dayTx) {
                if (tx.transactionType == "SALE" && (tx.paymentMethod == "CASH" || tx.paymentMethod == "SPLIT")) {
                    cashSales += tx.paidAmount
                } else if (tx.transactionType == "SALE_RETURN" && tx.paymentMethod == "CASH") {
                    cashSales -= tx.finalTotalAmount
                }
            }

            var cashExpenses = 0.0
            for (exp in dayExpenses) {
                if (exp.paymentMethod == "CASH") cashExpenses += exp.amount
            }

            val expectedCash = FinancialMath.roundMoney(openingCash + cashSales - cashExpenses)
            val mismatch = FinancialMath.roundMoney(actualCash - expectedCash)

            val closing = CashClosingEntity(
                closingDate = now,
                openingCash = openingCash,
                expectedCash = expectedCash,
                actualCash = actualCash,
                mismatchAmount = mismatch,
                notes = notes,
                closedBy = closedBy
            )
            val id = db.cashClosingDao().insertCashClosing(closing)
            OperationResult.Success(closing.copy(id = id), "Cash register closed. Mismatch: ₹$mismatch")
        } catch (e: Exception) {
            OperationResult.Error(e.message ?: "Failed to close cash register.")
        }
    }

    // Held Bills Operations
    suspend fun holdBill(label: String, customerName: String?, itemsJson: String, itemsCount: Int, totalAmount: Double): OperationResult<Long> {
        return try {
            val id = db.heldBillDao().insertHeldBill(
                HeldBillEntity(
                    billLabel = label,
                    customerName = customerName,
                    itemsCount = itemsCount,
                    totalAmount = totalAmount,
                    itemsJson = itemsJson
                )
            )
            OperationResult.Success(id, "Bill held as '$label'")
        } catch (e: Exception) {
            OperationResult.Error(e.message ?: "Failed to hold bill.")
        }
    }

    suspend fun resumeHeldBill(id: Long): OperationResult<Boolean> {
        return try {
            db.heldBillDao().deleteHeldBill(id)
            OperationResult.Success(true, "Held bill resumed.")
        } catch (e: Exception) {
            OperationResult.Error(e.message ?: "Failed to resume held bill.")
        }
    }

    // Product CRUD
    suspend fun createProduct(product: ProductEntity): OperationResult<ProductEntity> {
        if (product.barcode.isBlank()) return OperationResult.Error("Barcode cannot be blank.")
        if (product.name.isBlank()) return OperationResult.Error("Product name cannot be empty.")
        if (product.purchaseCostPrice < 0 || product.sellingPrice < 0) return OperationResult.Error("Prices cannot be negative.")

        return try {
            val result = db.withTransaction {
                val existing = db.productDao().getProductByBarcode(product.barcode.trim())
                if (existing != null) {
                    throw IllegalStateException("A product with barcode '${product.barcode}' already exists: ${existing.name}")
                }
                val newId = db.productDao().insertProduct(product.copy(barcode = product.barcode.trim()))
                val saved = db.productDao().getProductById(newId)!!
                db.auditLogDao().insertAuditLog(
                    AuditLogEntity(
                        timestamp = System.currentTimeMillis(),
                        action = "PRODUCT_CREATED",
                        entityType = "Product",
                        entityId = saved.id,
                        description = "Created product '${saved.name}' with barcode ${saved.barcode}, stock ${saved.currentStock}",
                        verifiedStock = saved.currentStock
                    )
                )
                saved
            }
            OperationResult.Success(result, "Product '${result.name}' successfully saved.")
        } catch (e: Exception) {
            OperationResult.Error(e.message ?: "Failed to create product.")
        }
    }

    suspend fun updateProduct(product: ProductEntity): OperationResult<ProductEntity> {
        return try {
            val rows = db.productDao().updateProduct(product.copy(updatedAt = System.currentTimeMillis()))
            if (rows > 0) {
                val updated = db.productDao().getProductById(product.id)!!
                OperationResult.Success(updated, "Product '${updated.name}' updated.")
            } else {
                OperationResult.Error("Product not found.")
            }
        } catch (e: Exception) {
            OperationResult.Error(e.message ?: "Failed to update product.")
        }
    }

    suspend fun updateProductExpiry(productId: Long, expiryDate: Long?): OperationResult<Boolean> {
        return try {
            val rows = db.productDao().updateExpiryDate(productId, expiryDate, System.currentTimeMillis())
            if (rows > 0) {
                OperationResult.Success(true, "Expiry date updated successfully.")
            } else {
                OperationResult.Error("Product not found.")
            }
        } catch (e: Exception) {
            OperationResult.Error(e.message ?: "Failed to update expiry date.")
        }
    }

    // Promotion Rules Operations
    suspend fun getActivePromotionRulesSnapshot(): List<PromotionRuleEntity> =
        db.promotionRuleDao().getActivePromotionRulesList()

    suspend fun savePromotionRule(rule: PromotionRuleEntity): OperationResult<PromotionRuleEntity> {
        if (rule.ruleName.isBlank()) return OperationResult.Error("Rule name cannot be empty.")
        return try {
            val id = if (rule.id == 0L) {
                db.promotionRuleDao().insertPromotionRule(rule)
            } else {
                db.promotionRuleDao().updatePromotionRule(rule)
                rule.id
            }
            OperationResult.Success(rule.copy(id = id), "Promotion rule '${rule.ruleName}' saved.")
        } catch (e: Exception) {
            OperationResult.Error(e.message ?: "Failed to save promotion rule.")
        }
    }

    suspend fun togglePromotionRule(id: Long, isEnabled: Boolean): OperationResult<Boolean> {
        return try {
            db.promotionRuleDao().togglePromotionRule(id, isEnabled)
            OperationResult.Success(true, if (isEnabled) "Promotion rule enabled." else "Promotion rule disabled.")
        } catch (e: Exception) {
            OperationResult.Error(e.message ?: "Failed to toggle promotion rule.")
        }
    }

    suspend fun deletePromotionRule(rule: PromotionRuleEntity): OperationResult<Boolean> {
        return try {
            db.promotionRuleDao().deletePromotionRule(rule)
            OperationResult.Success(true, "Promotion rule deleted.")
        } catch (e: Exception) {
            OperationResult.Error(e.message ?: "Failed to delete promotion rule.")
        }
    }

    suspend fun seedDefaultPromotionRulesIfEmpty() {
        val existing = db.promotionRuleDao().getActivePromotionRulesList()
        if (existing.isEmpty()) {
            val defaultRules = listOf(
                PromotionRuleEntity(
                    ruleName = "5% Off Above ₹500",
                    ruleType = "SPEND_DISCOUNT",
                    minCartValue = 500.0,
                    discountPercent = 5.0,
                    minMarginPercent = 8.0,
                    description = "Instant 5% discount on bills of ₹500 or more"
                ),
                PromotionRuleEntity(
                    ruleName = "8% Off Above ₹1,000",
                    ruleType = "SPEND_DISCOUNT",
                    minCartValue = 1000.0,
                    discountPercent = 8.0,
                    minMarginPercent = 10.0,
                    description = "Instant 8% discount on bills of ₹1,000 or more"
                ),
                PromotionRuleEntity(
                    ruleName = "Flat ₹150 Off Above ₹1,500",
                    ruleType = "SPEND_DISCOUNT",
                    minCartValue = 1500.0,
                    fixedDiscountAmount = 150.0,
                    minMarginPercent = 10.0,
                    description = "Flat ₹150 discount on bills of ₹1,500 or more"
                ),
                PromotionRuleEntity(
                    ruleName = "10% Super Saver Above ₹2,000",
                    ruleType = "SPEND_DISCOUNT",
                    minCartValue = 2000.0,
                    discountPercent = 10.0,
                    minMarginPercent = 12.0,
                    description = "Special 10% super saver discount on bills of ₹2,000 or more"
                ),
                PromotionRuleEntity(
                    ruleName = "Flat ₹350 Off Above ₹3,000",
                    ruleType = "SPEND_DISCOUNT",
                    minCartValue = 3000.0,
                    fixedDiscountAmount = 350.0,
                    minMarginPercent = 12.0,
                    description = "Mega retailer discount of ₹350 on orders above ₹3,000"
                )
            )
            for (rule in defaultRules) {
                db.promotionRuleDao().insertPromotionRule(rule)
            }
        }
    }

    suspend fun clearAllData(): OperationResult<Boolean> {
        return try {
            db.withTransaction {
                db.transactionItemDao().deleteAllTransactionItems()
                db.transactionDao().deleteAllTransactions()
                db.auditLogDao().deleteAllAuditLogs()
                db.productDao().deleteAllProducts()
                db.supplierDao().deleteAllSuppliers()
                db.customerDao().deleteAllCustomers()
                db.customerPaymentDao().deleteAllCustomerPayments()
                db.supplierPaymentDao().deleteAllSupplierPayments()
                db.expenseDao().deleteAllExpenses()
                db.stockAdjustmentDao().deleteAllStockAdjustments()
                db.cashClosingDao().deleteAllCashClosings()
                db.heldBillDao().deleteAllHeldBills()
                db.promotionRuleDao().deleteAllPromotionRules()
            }
            OperationResult.Success(true, "All store data has been completely erased.")
        } catch (e: Exception) {
            OperationResult.Error(e.message ?: "Failed to clear store data.")
        }
    }
}
