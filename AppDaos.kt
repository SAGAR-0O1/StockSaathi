package com.example.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
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
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductDao {
    @Query("SELECT * FROM products ORDER BY name ASC")
    fun getAllProducts(): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products ORDER BY name ASC")
    suspend fun getAllProductsList(): List<ProductEntity>

    @Query("SELECT * FROM products WHERE id = :id LIMIT 1")
    suspend fun getProductById(id: Long): ProductEntity?

    @Query("SELECT * FROM products WHERE barcode = :barcode LIMIT 1")
    suspend fun getProductByBarcode(barcode: String): ProductEntity?

    @Query("SELECT * FROM products WHERE name LIKE '%' || :query || '%' OR barcode LIKE '%' || :query || '%' ORDER BY name ASC")
    fun searchProducts(query: String): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products WHERE currentStock <= minStockThreshold ORDER BY currentStock ASC")
    fun getLowStockProducts(): Flow<List<ProductEntity>>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertProduct(product: ProductEntity): Long

    @Update
    suspend fun updateProduct(product: ProductEntity): Int

    @Query("UPDATE products SET currentStock = :newStock, updatedAt = :timestamp WHERE id = :id")
    suspend fun updateStock(id: Long, newStock: Int, timestamp: Long): Int

    @Query("UPDATE products SET currentStock = :newStock, purchaseCostPrice = :newCostPrice, updatedAt = :timestamp WHERE id = :id")
    suspend fun updateStockAndCost(id: Long, newStock: Int, newCostPrice: Double, timestamp: Long): Int

    @Query("UPDATE products SET expiryDate = :expiryDate, updatedAt = :timestamp WHERE id = :id")
    suspend fun updateExpiryDate(id: Long, expiryDate: Long?, timestamp: Long): Int

    @Delete
    suspend fun deleteProduct(product: ProductEntity): Int

    @Query("DELETE FROM products")
    suspend fun deleteAllProducts(): Int
}

@Dao
interface SupplierDao {
    @Query("SELECT * FROM suppliers ORDER BY name ASC")
    fun getAllSuppliers(): Flow<List<SupplierEntity>>

    @Query("SELECT * FROM suppliers ORDER BY name ASC")
    suspend fun getAllSuppliersList(): List<SupplierEntity>

    @Query("SELECT * FROM suppliers WHERE id = :id LIMIT 1")
    suspend fun getSupplierById(id: Long): SupplierEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSupplier(supplier: SupplierEntity): Long

    @Update
    suspend fun updateSupplier(supplier: SupplierEntity): Int

    @Query("UPDATE suppliers SET outstandingPayable = :newPayable WHERE id = :id")
    suspend fun updateOutstandingPayable(id: Long, newPayable: Double): Int

    @Query("DELETE FROM suppliers")
    suspend fun deleteAllSuppliers(): Int
}

@Dao
interface CustomerDao {
    @Query("SELECT * FROM customers ORDER BY name ASC")
    fun getAllCustomers(): Flow<List<CustomerEntity>>

    @Query("SELECT * FROM customers ORDER BY name ASC")
    suspend fun getAllCustomersList(): List<CustomerEntity>

    @Query("SELECT * FROM customers WHERE id = :id LIMIT 1")
    suspend fun getCustomerById(id: Long): CustomerEntity?

    @Query("SELECT * FROM customers WHERE mobileNumber = :mobile LIMIT 1")
    suspend fun getCustomerByMobile(mobile: String): CustomerEntity?

    @Query("SELECT * FROM customers WHERE name LIKE '%' || :query || '%' OR mobileNumber LIKE '%' || :query || '%' ORDER BY name ASC")
    fun searchCustomers(query: String): Flow<List<CustomerEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomer(customer: CustomerEntity): Long

    @Update
    suspend fun updateCustomer(customer: CustomerEntity): Int

    @Query("UPDATE customers SET outstandingBalance = :balance WHERE id = :id")
    suspend fun updateCustomerBalance(id: Long, balance: Double): Int

    @Query("UPDATE customers SET loyaltyPoints = loyaltyPoints + :points WHERE id = :id")
    suspend fun addLoyaltyPoints(id: Long, points: Int): Int

    @Query("DELETE FROM customers")
    suspend fun deleteAllCustomers(): Int
}

@Dao
interface CustomerPaymentDao {
    @Query("SELECT * FROM customer_payments ORDER BY timestamp DESC")
    fun getAllPayments(): Flow<List<CustomerPaymentEntity>>

    @Query("SELECT * FROM customer_payments WHERE customerId = :customerId ORDER BY timestamp DESC")
    fun getPaymentsForCustomer(customerId: Long): Flow<List<CustomerPaymentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPayment(payment: CustomerPaymentEntity): Long

    @Query("DELETE FROM customer_payments")
    suspend fun deleteAllCustomerPayments(): Int
}

@Dao
interface SupplierPaymentDao {
    @Query("SELECT * FROM supplier_payments ORDER BY timestamp DESC")
    fun getAllPayments(): Flow<List<SupplierPaymentEntity>>

    @Query("SELECT * FROM supplier_payments WHERE supplierId = :supplierId ORDER BY timestamp DESC")
    fun getPaymentsForSupplier(supplierId: Long): Flow<List<SupplierPaymentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPayment(payment: SupplierPaymentEntity): Long

    @Query("DELETE FROM supplier_payments")
    suspend fun deleteAllSupplierPayments(): Int
}

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    fun getAllTransactions(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    suspend fun getAllTransactionsList(): List<TransactionEntity>

    @Query("SELECT * FROM transactions WHERE timestamp >= :start AND timestamp <= :end ORDER BY timestamp DESC")
    fun getTransactionsBetween(start: Long, end: Long): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE timestamp >= :start AND timestamp <= :end ORDER BY timestamp DESC")
    suspend fun getTransactionsBetweenList(start: Long, end: Long): List<TransactionEntity>

    @Query("SELECT * FROM transactions WHERE idempotencyKey = :key LIMIT 1")
    suspend fun getTransactionByIdempotency(key: String): TransactionEntity?

    @Query("SELECT * FROM transactions WHERE id = :id LIMIT 1")
    suspend fun getTransactionById(id: Long): TransactionEntity?

    @Query("SELECT * FROM transactions WHERE invoiceNumber = :inv LIMIT 1")
    suspend fun getTransactionByInvoiceNumber(inv: String): TransactionEntity?

    @Query("SELECT * FROM transactions WHERE customerId = :customerId ORDER BY timestamp DESC")
    fun getTransactionsForCustomer(customerId: Long): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE supplierId = :supplierId ORDER BY timestamp DESC")
    fun getTransactionsForSupplier(supplierId: Long): Flow<List<TransactionEntity>>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertTransaction(tx: TransactionEntity): Long

    @Query("UPDATE transactions SET status = :status WHERE id = :id")
    suspend fun updateTransactionStatus(id: Long, status: String): Int

    @Query("UPDATE transactions SET paymentStatus = :paymentStatus, paidAmount = :paidAmount, balanceDue = :balanceDue WHERE id = :id")
    suspend fun updatePaymentStatus(id: Long, paymentStatus: String, paidAmount: Double, balanceDue: Double): Int

    @Query("DELETE FROM transactions")
    suspend fun deleteAllTransactions(): Int
}

@Dao
interface TransactionItemDao {
    @Query("SELECT * FROM transaction_items WHERE transactionId = :txId")
    fun getItemsForTransaction(txId: Long): Flow<List<TransactionItemEntity>>

    @Query("SELECT * FROM transaction_items WHERE transactionId = :txId")
    suspend fun getItemsForTransactionSync(txId: Long): List<TransactionItemEntity>

    @Query("SELECT * FROM transaction_items")
    fun getAllItems(): Flow<List<TransactionItemEntity>>

    @Query("SELECT * FROM transaction_items")
    suspend fun getAllItemsSync(): List<TransactionItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItems(items: List<TransactionItemEntity>): List<Long>

    @Query("UPDATE transaction_items SET returnedQuantity = returnedQuantity + :additionalReturned WHERE id = :itemId")
    suspend fun recordItemReturn(itemId: Long, additionalReturned: Int): Int

    @Query("DELETE FROM transaction_items")
    suspend fun deleteAllTransactionItems(): Int
}

@Dao
interface ExpenseDao {
    @Query("SELECT * FROM expenses ORDER BY date DESC")
    fun getAllExpenses(): Flow<List<ExpenseEntity>>

    @Query("SELECT * FROM expenses ORDER BY date DESC")
    suspend fun getAllExpensesList(): List<ExpenseEntity>

    @Query("SELECT * FROM expenses WHERE date >= :start AND date <= :end ORDER BY date DESC")
    fun getExpensesBetween(start: Long, end: Long): Flow<List<ExpenseEntity>>

    @Query("SELECT * FROM expenses WHERE date >= :start AND date <= :end ORDER BY date DESC")
    suspend fun getExpensesBetweenList(start: Long, end: Long): List<ExpenseEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpense(expense: ExpenseEntity): Long

    @Update
    suspend fun updateExpense(expense: ExpenseEntity): Int

    @Delete
    suspend fun deleteExpense(expense: ExpenseEntity): Int

    @Query("DELETE FROM expenses")
    suspend fun deleteAllExpenses(): Int
}

@Dao
interface StockAdjustmentDao {
    @Query("SELECT * FROM stock_adjustments ORDER BY timestamp DESC")
    fun getAllStockAdjustments(): Flow<List<StockAdjustmentEntity>>

    @Query("SELECT * FROM stock_adjustments WHERE productId = :productId ORDER BY timestamp DESC")
    fun getAdjustmentsForProduct(productId: Long): Flow<List<StockAdjustmentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStockAdjustment(adjustment: StockAdjustmentEntity): Long

    @Query("DELETE FROM stock_adjustments")
    suspend fun deleteAllStockAdjustments(): Int
}

@Dao
interface CashClosingDao {
    @Query("SELECT * FROM cash_closings ORDER BY closingDate DESC")
    fun getAllCashClosings(): Flow<List<CashClosingEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCashClosing(closing: CashClosingEntity): Long

    @Query("DELETE FROM cash_closings")
    suspend fun deleteAllCashClosings(): Int
}

@Dao
interface HeldBillDao {
    @Query("SELECT * FROM held_bills ORDER BY timestamp DESC")
    fun getAllHeldBills(): Flow<List<HeldBillEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHeldBill(bill: HeldBillEntity): Long

    @Query("DELETE FROM held_bills WHERE id = :id")
    suspend fun deleteHeldBill(id: Long): Int

    @Query("DELETE FROM held_bills")
    suspend fun deleteAllHeldBills(): Int
}

@Dao
interface AuditLogDao {
    @Query("SELECT * FROM audit_logs ORDER BY timestamp DESC LIMIT 300")
    fun getAuditLogs(): Flow<List<AuditLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAuditLog(log: AuditLogEntity): Long

    @Query("DELETE FROM audit_logs")
    suspend fun deleteAllAuditLogs(): Int
}

@Dao
interface PromotionRuleDao {
    @Query("SELECT * FROM promotion_rules ORDER BY minCartValue ASC")
    fun getAllPromotionRules(): Flow<List<PromotionRuleEntity>>

    @Query("SELECT * FROM promotion_rules WHERE isEnabled = 1 ORDER BY minCartValue ASC")
    fun getActivePromotionRules(): Flow<List<PromotionRuleEntity>>

    @Query("SELECT * FROM promotion_rules WHERE isEnabled = 1 ORDER BY minCartValue ASC")
    suspend fun getActivePromotionRulesList(): List<PromotionRuleEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPromotionRule(rule: PromotionRuleEntity): Long

    @Update
    suspend fun updatePromotionRule(rule: PromotionRuleEntity): Int

    @Delete
    suspend fun deletePromotionRule(rule: PromotionRuleEntity): Int

    @Query("UPDATE promotion_rules SET isEnabled = :isEnabled WHERE id = :id")
    suspend fun togglePromotionRule(id: Long, isEnabled: Boolean): Int

    @Query("DELETE FROM promotion_rules")
    suspend fun deleteAllPromotionRules(): Int
}
