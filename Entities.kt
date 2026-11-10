package com.example.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "products",
    indices = [
        Index(value = ["barcode"], unique = true),
        Index(value = ["name"]),
        Index(value = ["category"])
    ]
)
data class ProductEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val barcode: String,
    val name: String,
    val category: String,
    val currentStock: Int,
    val minStockThreshold: Int = 10,
    val purchaseCostPrice: Double, // Stores latest Weighted Average Cost (WAC)
    val sellingPrice: Double,
    val taxRatePercent: Double = 0.0,
    val unit: String = "pcs",
    val updatedAt: Long = System.currentTimeMillis(),
    val expiryDate: Long? = null,
    val supplierId: Long? = null,
    val supplierName: String? = null,
    val mfgDate: Long? = null,
    val batchNumber: String? = null,
    val imageUrl: String? = null
)

@Entity(
    tableName = "suppliers",
    indices = [Index(value = ["name"])]
)
data class SupplierEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val contactNumber: String,
    val address: String,
    val paymentTerms: String = "Net 30",
    val outstandingPayable: Double = 0.0,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "transactions",
    indices = [
        Index(value = ["idempotencyKey"], unique = true),
        Index(value = ["productId"]),
        Index(value = ["timestamp"]),
        Index(value = ["transactionType"])
    ]
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val transactionType: String, // "PURCHASE", "SALE", "SALE_RETURN", "PURCHASE_RETURN", "CANCELLATION"
    val idempotencyKey: String,
    val productId: Long = 0, // Primary item or 0 for multi-item invoices
    val productName: String = "",
    val barcode: String = "",
    val supplierId: Long? = null,
    val supplierName: String? = null,
    val customerId: Long? = null,
    val customerName: String? = null,
    val invoiceNumber: String = "",
    val quantity: Int = 0,
    val unitPrice: Double = 0.0,
    val subtotalAmount: Double = 0.0,
    val discountAmount: Double = 0.0,
    val billDiscountAmount: Double = 0.0,
    val taxAmount: Double = 0.0,
    val finalTotalAmount: Double,
    val paymentMethod: String = "CASH", // "CASH", "UPI", "CARD", "CREDIT", "SPLIT"
    val paymentStatus: String = "PAID", // "PAID", "PARTIAL", "UNPAID"
    val paidAmount: Double = 0.0,
    val balanceDue: Double = 0.0,
    val previousStock: Int = 0,
    val resultingStock: Int = 0,
    val parentTransactionId: Long? = null,
    val returnReason: String? = null,
    val returnCondition: String? = null, // "SELLABLE", "DAMAGED", "QUARANTINED"
    val storeId: String = "DEFAULT_STORE",
    val timestamp: Long = System.currentTimeMillis(),
    val status: String = "COMMITTED", // "COMMITTED", "CANCELLED"
    val notes: String? = null
)

@Entity(
    tableName = "transaction_items",
    indices = [
        Index(value = ["transactionId"]),
        Index(value = ["productId"])
    ]
)
data class TransactionItemEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val transactionId: Long,
    val invoiceNumber: String = "",
    val productId: Long,
    val productName: String,
    val barcode: String,
    val quantity: Int,
    val unitPrice: Double,
    val costBasis: Double, // Cost basis at the moment of sale/purchase (Critical for accurate historical COGS)
    val discountAmount: Double = 0.0,
    val taxRatePercent: Double = 0.0,
    val taxAmount: Double = 0.0,
    val totalAmount: Double,
    val returnedQuantity: Int = 0,
    val batchNumber: String? = null,
    val expiryDate: Long? = null,
    val unit: String = "pcs"
)

@Entity(
    tableName = "customers",
    indices = [Index(value = ["mobileNumber"])]
)
data class CustomerEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val mobileNumber: String,
    val email: String? = null,
    val address: String? = null,
    val creditLimit: Double = 5000.0,
    val outstandingBalance: Double = 0.0,
    val loyaltyPoints: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "customer_payments")
data class CustomerPaymentEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val customerId: Long,
    val customerName: String,
    val amount: Double,
    val paymentMethod: String = "CASH", // CASH, UPI, CARD, BANK_TRANSFER
    val notes: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "supplier_payments")
data class SupplierPaymentEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val supplierId: Long,
    val supplierName: String,
    val amount: Double,
    val paymentMethod: String = "BANK_TRANSFER",
    val notes: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "expenses",
    indices = [
        Index(value = ["date"]),
        Index(value = ["category"])
    ]
)
data class ExpenseEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val category: String, // Shop rent, Electricity, Employee salary, Transportation, Packaging, Internet, Maintenance, Marketing, Bank charges, Other
    val title: String,
    val amount: Double,
    val paymentMethod: String = "CASH",
    val date: Long = System.currentTimeMillis(),
    val notes: String? = null,
    val isRecurring: Boolean = false,
    val receiptUrl: String? = null
)

@Entity(
    tableName = "stock_adjustments",
    indices = [Index(value = ["productId"])]
)
data class StockAdjustmentEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val productId: Long,
    val productName: String,
    val barcode: String,
    val adjustmentType: String, // "DAMAGE", "AUDIT_DISCREPANCY", "SAMPLE", "EXPIRY_WRITE_OFF", "RESTOCK"
    val quantityDelta: Int, // Negative for reduction, positive for restock
    val previousStock: Int,
    val resultingStock: Int,
    val reason: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "cash_closings")
data class CashClosingEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val closingDate: Long = System.currentTimeMillis(),
    val openingCash: Double = 0.0,
    val expectedCash: Double = 0.0,
    val actualCash: Double = 0.0,
    val mismatchAmount: Double = 0.0,
    val notes: String? = null,
    val closedBy: String = "Owner"
)

@Entity(tableName = "held_bills")
data class HeldBillEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val billLabel: String,
    val customerName: String? = null,
    val itemsCount: Int = 0,
    val totalAmount: Double = 0.0,
    val itemsJson: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "audit_logs",
    indices = [Index(value = ["timestamp"])]
)
data class AuditLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val action: String,
    val entityType: String,
    val entityId: Long,
    val description: String,
    val verifiedStock: Int
)

@Entity(
    tableName = "promotion_rules",
    indices = [Index(value = ["ruleType"]), Index(value = ["isEnabled"])]
)
data class PromotionRuleEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val ruleName: String,
    val ruleType: String, // "SPEND_DISCOUNT", "SPEND_GIFT", "NEAR_EXPIRY_DISCOUNT", "CATEGORY_DISCOUNT", "BUNDLE_DISCOUNT"
    val minCartValue: Double = 0.0,
    val discountPercent: Double = 0.0,
    val fixedDiscountAmount: Double = 0.0,
    val freeGiftProductId: Long? = null,
    val freeGiftProductName: String? = null,
    val targetProductId: Long? = null,
    val targetCategory: String? = null,
    val maxNearExpiryDays: Int = 14,
    val minMarginPercent: Double = 5.0, // Minimum profit margin percentage required
    val isEnabled: Boolean = true,
    val description: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

data class SmartOffer(
    val id: String,
    val ruleId: Long? = null,
    val title: String,
    val subtitle: String,
    val offerType: String, // "DISCOUNT", "FREE_GIFT", "EXPIRY_CLEARANCE", "BUNDLE", "NUDGE"
    val discountAmount: Double = 0.0,
    val discountPercent: Double = 0.0,
    val giftProductId: Long? = null,
    val giftProductName: String? = null,
    val giftProductPrice: Double = 0.0,
    val nudgeAmountNeeded: Double = 0.0,
    val expectedMarginPercent: Double = 0.0,
    val badgeText: String = "Special Offer",
    val reason: String = "",
    val isApplied: Boolean = false
)
