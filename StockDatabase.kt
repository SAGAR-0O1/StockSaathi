package com.example.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.dao.AuditLogDao
import com.example.data.dao.CashClosingDao
import com.example.data.dao.CustomerDao
import com.example.data.dao.CustomerPaymentDao
import com.example.data.dao.ExpenseDao
import com.example.data.dao.HeldBillDao
import com.example.data.dao.ProductDao
import com.example.data.dao.PromotionRuleDao
import com.example.data.dao.StockAdjustmentDao
import com.example.data.dao.SupplierDao
import com.example.data.dao.SupplierPaymentDao
import com.example.data.dao.TransactionDao
import com.example.data.dao.TransactionItemDao
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

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE products ADD COLUMN expiryDate INTEGER DEFAULT NULL")
    }
}

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Upgrade transactions table
        db.execSQL("ALTER TABLE transactions ADD COLUMN invoiceNumber TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE transactions ADD COLUMN customerId INTEGER DEFAULT NULL")
        db.execSQL("ALTER TABLE transactions ADD COLUMN customerName TEXT DEFAULT NULL")
        db.execSQL("ALTER TABLE transactions ADD COLUMN subtotalAmount REAL NOT NULL DEFAULT 0.0")
        db.execSQL("ALTER TABLE transactions ADD COLUMN billDiscountAmount REAL NOT NULL DEFAULT 0.0")
        db.execSQL("ALTER TABLE transactions ADD COLUMN paymentMethod TEXT NOT NULL DEFAULT 'CASH'")
        db.execSQL("ALTER TABLE transactions ADD COLUMN paymentStatus TEXT NOT NULL DEFAULT 'PAID'")
        db.execSQL("ALTER TABLE transactions ADD COLUMN paidAmount REAL NOT NULL DEFAULT 0.0")
        db.execSQL("ALTER TABLE transactions ADD COLUMN balanceDue REAL NOT NULL DEFAULT 0.0")
        db.execSQL("ALTER TABLE transactions ADD COLUMN parentTransactionId INTEGER DEFAULT NULL")
        db.execSQL("ALTER TABLE transactions ADD COLUMN returnReason TEXT DEFAULT NULL")
        db.execSQL("ALTER TABLE transactions ADD COLUMN returnCondition TEXT DEFAULT NULL")
        db.execSQL("ALTER TABLE transactions ADD COLUMN storeId TEXT NOT NULL DEFAULT 'DEFAULT_STORE'")

        // Upgrade suppliers table
        db.execSQL("ALTER TABLE suppliers ADD COLUMN outstandingPayable REAL NOT NULL DEFAULT 0.0")

        // Create transaction_items table
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `transaction_items` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `transactionId` INTEGER NOT NULL,
                `invoiceNumber` TEXT NOT NULL,
                `productId` INTEGER NOT NULL,
                `productName` TEXT NOT NULL,
                `barcode` TEXT NOT NULL,
                `quantity` INTEGER NOT NULL,
                `unitPrice` REAL NOT NULL,
                `costBasis` REAL NOT NULL,
                `discountAmount` REAL NOT NULL,
                `taxRatePercent` REAL NOT NULL,
                `taxAmount` REAL NOT NULL,
                `totalAmount` REAL NOT NULL,
                `returnedQuantity` INTEGER NOT NULL,
                `batchNumber` TEXT,
                `expiryDate` INTEGER,
                `unit` TEXT NOT NULL
            )
        """.trimIndent())
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_transaction_items_transactionId` ON `transaction_items` (`transactionId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_transaction_items_productId` ON `transaction_items` (`productId`)")

        // Create customers table
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `customers` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `name` TEXT NOT NULL,
                `mobileNumber` TEXT NOT NULL,
                `email` TEXT,
                `address` TEXT,
                `creditLimit` REAL NOT NULL,
                `outstandingBalance` REAL NOT NULL,
                `loyaltyPoints` INTEGER NOT NULL,
                `createdAt` INTEGER NOT NULL
            )
        """.trimIndent())
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_customers_mobileNumber` ON `customers` (`mobileNumber`)")

        // Create customer_payments table
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `customer_payments` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `customerId` INTEGER NOT NULL,
                `customerName` TEXT NOT NULL,
                `amount` REAL NOT NULL,
                `paymentMethod` TEXT NOT NULL,
                `notes` TEXT,
                `timestamp` INTEGER NOT NULL
            )
        """.trimIndent())

        // Create supplier_payments table
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `supplier_payments` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `supplierId` INTEGER NOT NULL,
                `supplierName` TEXT NOT NULL,
                `amount` REAL NOT NULL,
                `paymentMethod` TEXT NOT NULL,
                `notes` TEXT,
                `timestamp` INTEGER NOT NULL
            )
        """.trimIndent())

        // Create expenses table
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `expenses` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `category` TEXT NOT NULL,
                `title` TEXT NOT NULL,
                `amount` REAL NOT NULL,
                `paymentMethod` TEXT NOT NULL,
                `date` INTEGER NOT NULL,
                `notes` TEXT,
                `isRecurring` INTEGER NOT NULL,
                `receiptUrl` TEXT
            )
        """.trimIndent())
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_expenses_date` ON `expenses` (`date`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_expenses_category` ON `expenses` (`category`)")

        // Create stock_adjustments table
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `stock_adjustments` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `productId` INTEGER NOT NULL,
                `productName` TEXT NOT NULL,
                `barcode` TEXT NOT NULL,
                `adjustmentType` TEXT NOT NULL,
                `quantityDelta` INTEGER NOT NULL,
                `previousStock` INTEGER NOT NULL,
                `resultingStock` INTEGER NOT NULL,
                `reason` TEXT NOT NULL,
                `timestamp` INTEGER NOT NULL
            )
        """.trimIndent())
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_stock_adjustments_productId` ON `stock_adjustments` (`productId`)")

        // Create cash_closings table
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `cash_closings` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `closingDate` INTEGER NOT NULL,
                `openingCash` REAL NOT NULL,
                `expectedCash` REAL NOT NULL,
                `actualCash` REAL NOT NULL,
                `mismatchAmount` REAL NOT NULL,
                `notes` TEXT,
                `closedBy` TEXT NOT NULL
            )
        """.trimIndent())

        // Create held_bills table
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `held_bills` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `billLabel` TEXT NOT NULL,
                `customerName` TEXT,
                `itemsCount` INTEGER NOT NULL,
                `totalAmount` REAL NOT NULL,
                `itemsJson` TEXT NOT NULL,
                `timestamp` INTEGER NOT NULL
            )
        """.trimIndent())
    }
}

val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Upgrade products table with batch, supplier, mfg, and image info
        db.execSQL("ALTER TABLE products ADD COLUMN supplierId INTEGER DEFAULT NULL")
        db.execSQL("ALTER TABLE products ADD COLUMN supplierName TEXT DEFAULT NULL")
        db.execSQL("ALTER TABLE products ADD COLUMN mfgDate INTEGER DEFAULT NULL")
        db.execSQL("ALTER TABLE products ADD COLUMN batchNumber TEXT DEFAULT NULL")
        db.execSQL("ALTER TABLE products ADD COLUMN imageUrl TEXT DEFAULT NULL")

        // Create promotion_rules table
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `promotion_rules` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `ruleName` TEXT NOT NULL,
                `ruleType` TEXT NOT NULL,
                `minCartValue` REAL NOT NULL,
                `discountPercent` REAL NOT NULL,
                `fixedDiscountAmount` REAL NOT NULL,
                `freeGiftProductId` INTEGER,
                `freeGiftProductName` TEXT,
                `targetProductId` INTEGER,
                `targetCategory` TEXT,
                `maxNearExpiryDays` INTEGER NOT NULL,
                `minMarginPercent` REAL NOT NULL,
                `isEnabled` INTEGER NOT NULL,
                `description` TEXT NOT NULL,
                `createdAt` INTEGER NOT NULL
            )
        """.trimIndent())
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_promotion_rules_ruleType` ON `promotion_rules` (`ruleType`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_promotion_rules_isEnabled` ON `promotion_rules` (`isEnabled`)")
    }
}

@Database(
    entities = [
        ProductEntity::class,
        SupplierEntity::class,
        TransactionEntity::class,
        TransactionItemEntity::class,
        CustomerEntity::class,
        CustomerPaymentEntity::class,
        SupplierPaymentEntity::class,
        ExpenseEntity::class,
        StockAdjustmentEntity::class,
        CashClosingEntity::class,
        HeldBillEntity::class,
        AuditLogEntity::class,
        PromotionRuleEntity::class
    ],
    version = 4,
    exportSchema = false
)
abstract class StockDatabase : RoomDatabase() {
    abstract fun productDao(): ProductDao
    abstract fun supplierDao(): SupplierDao
    abstract fun customerDao(): CustomerDao
    abstract fun customerPaymentDao(): CustomerPaymentDao
    abstract fun supplierPaymentDao(): SupplierPaymentDao
    abstract fun transactionDao(): TransactionDao
    abstract fun transactionItemDao(): TransactionItemDao
    abstract fun expenseDao(): ExpenseDao
    abstract fun stockAdjustmentDao(): StockAdjustmentDao
    abstract fun cashClosingDao(): CashClosingDao
    abstract fun heldBillDao(): HeldBillDao
    abstract fun auditLogDao(): AuditLogDao
    abstract fun promotionRuleDao(): PromotionRuleDao

    companion object {
        @Volatile
        private var INSTANCE: StockDatabase? = null

        fun getInstance(context: Context): StockDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    StockDatabase::class.java,
                    "stocksaathi_db"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
