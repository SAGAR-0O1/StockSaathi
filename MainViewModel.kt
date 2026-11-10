package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ai.BusinessMetricReport
import com.example.data.ai.CartItemEvaluation
import com.example.data.ai.DemandForecastSummary
import com.example.data.ai.DemandForecastingEngine
import com.example.data.ai.LanguageMode
import com.example.data.ai.SmartOfferEngine
import com.example.data.ai.StockSaathiAiEngine
import com.example.data.database.StockDatabase
import com.example.data.model.AuditLogEntity
import com.example.data.model.CustomerEntity
import com.example.data.model.ExpenseEntity
import com.example.data.model.HeldBillEntity
import com.example.data.model.ProductEntity
import com.example.data.model.PromotionRuleEntity
import com.example.data.model.SmartOffer
import com.example.data.model.StockAdjustmentEntity
import com.example.data.model.SupplierEntity
import com.example.data.model.TransactionEntity
import com.example.data.model.TransactionItemEntity
import com.example.data.repository.InventoryRepository
import com.example.data.repository.OperationResult
import com.example.data.repository.PurchaseItemRequest
import com.example.data.repository.SaleItemRequest
import com.example.data.repository.SaleReturnItemRequest
import com.example.util.BackupUtils
import com.example.util.FinancialMath
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.Calendar
import java.util.UUID

enum class DashboardPeriod(val label: String) {
    TODAY("Today / आज"),
    THIS_WEEK("This Week / हफ़्ता"),
    THIS_MONTH("This Month / महीना"),
    ALL_TIME("All Time / कुल")
}

data class CartItem(
    val product: ProductEntity,
    var quantity: Int,
    val unitPrice: Double,
    val discount: Double = 0.0
) {
    val totalAmount: Double
        get() = maxOf(0.0, (quantity * unitPrice) - discount)
}

data class PurchaseCartItem(
    val product: ProductEntity,
    var quantity: Int,
    var unitCostPrice: Double,
    var discount: Double = 0.0,
    var taxPercent: Double = 0.0,
    var expiryDate: Long? = null,
    var batchNumber: String? = null
) {
    val totalAmount: Double
        get() {
            val base = quantity * unitCostPrice
            val taxable = maxOf(0.0, base - discount)
            val tax = (taxable * taxPercent) / 100.0
            return taxable + tax
        }
}

data class ActionConfirmationData(
    val actionType: String, // "SALE", "PURCHASE", "RETURN", "CANCEL", "VOICE_ACTION"
    val title: String,
    val productName: String,
    val barcode: String,
    val quantity: Int,
    val unitPrice: Double,
    val discount: Double,
    val totalAmount: Double,
    val previousStock: Int,
    val resultingStock: Int,
    val supplierName: String? = null,
    val customerName: String? = null,
    val paymentMethod: String = "CASH",
    val onConfirmAction: suspend () -> Unit
)

data class AiChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val isUser: Boolean,
    val message: String,
    val timestamp: Long = System.currentTimeMillis()
)

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val db = StockDatabase.getInstance(application)
    val repository = InventoryRepository(db)
    private val aiEngine = StockSaathiAiEngine()
    private val forecastingEngine = DemandForecastingEngine()

    // Concurrency Mutex to prevent duplicate rapid submissions (Bug 1B)
    private val transactionMutex = Mutex()

    val allProducts: StateFlow<List<ProductEntity>> = repository.allProducts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val lowStockProducts: StateFlow<List<ProductEntity>> = repository.lowStockProducts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allSuppliers: StateFlow<List<SupplierEntity>> = repository.allSuppliers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allCustomers: StateFlow<List<CustomerEntity>> = repository.allCustomers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allTransactions: StateFlow<List<TransactionEntity>> = repository.allTransactions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allTransactionItems: StateFlow<List<TransactionItemEntity>> = repository.allTransactionItems
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allExpenses: StateFlow<List<ExpenseEntity>> = repository.allExpenses
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allStockAdjustments: StateFlow<List<StockAdjustmentEntity>> = repository.allStockAdjustments
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allHeldBills: StateFlow<List<HeldBillEntity>> = repository.allHeldBills
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allAuditLogs: StateFlow<List<AuditLogEntity>> = repository.allAuditLogs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allPromotionRules: StateFlow<List<PromotionRuleEntity>> = repository.allPromotionRules
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch(Dispatchers.IO) {
            repository.seedDefaultPromotionRulesIfEmpty()
        }
    }

    // Selected period for dashboard
    private val _selectedPeriod = MutableStateFlow(DashboardPeriod.TODAY)
    val selectedPeriod: StateFlow<DashboardPeriod> = _selectedPeriod.asStateFlow()

    // Computed metrics for dashboard
    val dashboardMetrics: StateFlow<BusinessMetricReport> = combine(
        allProducts,
        allTransactions,
        allSuppliers,
        _selectedPeriod
    ) { products, transactions, suppliers, period ->
        val now = System.currentTimeMillis()
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startTime = when (period) {
            DashboardPeriod.TODAY -> cal.timeInMillis
            DashboardPeriod.THIS_WEEK -> {
                cal.set(Calendar.DAY_OF_WEEK, cal.firstDayOfWeek)
                cal.timeInMillis
            }
            DashboardPeriod.THIS_MONTH -> {
                cal.set(Calendar.DAY_OF_MONTH, 1)
                cal.timeInMillis
            }
            DashboardPeriod.ALL_TIME -> 0L
        }
        aiEngine.computeMetrics(products, transactions, suppliers, startTime, now, period.label)
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        BusinessMetricReport(
            title = "Loading Audit...",
            periodLabel = "Today",
            totalRevenue = 0.0,
            totalCogs = 0.0,
            totalDiscounts = 0.0,
            netProfit = 0.0,
            profitMarginPercent = 0.0,
            totalPurchases = 0.0,
            totalTransactions = 0,
            totalSalesUnits = 0,
            totalStockValuationCost = 0.0,
            totalStockValuationRetail = 0.0,
            lowStockCount = 0,
            outOfStockCount = 0,
            topSellingItems = emptyList(),
            slowMovingItems = emptyList(),
            lowStockItems = emptyList(),
            supplierSpends = emptyMap()
        )
    )

    // AI Demand Forecast
    val demandForecast: StateFlow<DemandForecastSummary> = combine(
        allProducts,
        allTransactions,
        allTransactionItems
    ) { products, transactions, items ->
        forecastingEngine.generateForecasts(products, transactions, items)
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        DemandForecastSummary(
            totalProductsAnalyzed = 0,
            criticalStockoutsCount = 0,
            recommendedProcurementSpend = 0.0,
            forecasts = emptyList()
        )
    )

    // POS / Sales Cart State
    private val _cartItems = MutableStateFlow<Map<Long, CartItem>>(emptyMap())
    val cartItems: StateFlow<Map<Long, CartItem>> = _cartItems.asStateFlow()

    private val _selectedCustomer = MutableStateFlow<CustomerEntity?>(null)
    val selectedCustomer: StateFlow<CustomerEntity?> = _selectedCustomer.asStateFlow()

    private val _billDiscount = MutableStateFlow(0.0)
    val billDiscount: StateFlow<Double> = _billDiscount.asStateFlow()

    private val _paymentMethod = MutableStateFlow("CASH") // CASH, UPI, CARD, CREDIT, SPLIT
    val paymentMethod: StateFlow<String> = _paymentMethod.asStateFlow()

    // Smart Offers State
    private val _dismissedOfferIds = MutableStateFlow<Set<String>>(emptySet())
    val dismissedOfferIds: StateFlow<Set<String>> = _dismissedOfferIds.asStateFlow()

    val activeSmartOffers: StateFlow<List<SmartOffer>> = combine(
        _cartItems,
        allPromotionRules,
        allProducts,
        allTransactionItems,
        _dismissedOfferIds
    ) { cart, rules, products, txItems, dismissed ->
        val evaluations = cart.values.map {
            CartItemEvaluation(
                product = it.product,
                quantity = it.quantity,
                unitPrice = it.unitPrice,
                discount = it.discount
            )
        }
        val generated = SmartOfferEngine.evaluateOffers(
            cartItems = evaluations,
            activeRules = rules,
            allProducts = products,
            recentTransactionItems = txItems
        )
        generated.filter { it.id !in dismissed }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Procurement / Purchase Cart State
    private val _purchaseCartItems = MutableStateFlow<Map<Long, PurchaseCartItem>>(emptyMap())
    val purchaseCartItems: StateFlow<Map<Long, PurchaseCartItem>> = _purchaseCartItems.asStateFlow()

    private val _selectedPurchaseSupplier = MutableStateFlow<SupplierEntity?>(null)
    val selectedPurchaseSupplier: StateFlow<SupplierEntity?> = _selectedPurchaseSupplier.asStateFlow()

    private val _purchasePaymentMethod = MutableStateFlow("BANK_TRANSFER")
    val purchasePaymentMethod: StateFlow<String> = _purchasePaymentMethod.asStateFlow()

    // Confirmation Modal State
    private val _pendingConfirmation = MutableStateFlow<ActionConfirmationData?>(null)
    val pendingConfirmation: StateFlow<ActionConfirmationData?> = _pendingConfirmation.asStateFlow()

    // Transaction Slip Dialog State
    private val _activeSlipTransaction = MutableStateFlow<TransactionEntity?>(null)
    val activeSlipTransaction: StateFlow<TransactionEntity?> = _activeSlipTransaction.asStateFlow()

    // Snack/Toast notifications
    private val _uiEventMessage = MutableSharedFlow<String>()
    val uiEventMessage: SharedFlow<String> = _uiEventMessage.asSharedFlow()

    // AI Chat state
    private val _aiLanguageMode = MutableStateFlow(LanguageMode.AUTO)
    val aiLanguageMode: StateFlow<LanguageMode> = _aiLanguageMode.asStateFlow()

    private val _chatMessages = MutableStateFlow<List<AiChatMessage>>(
        listOf(
            AiChatMessage(
                isUser = false,
                message = """
**Namaste! Main StockSaathi hoon — Aapka Enterprise AI Inventory & Financial Intelligence Assistant.**

Sabhi calculations verified SQLite records se accurate hoti hain.
Aap pooch sakte hain:
• "Aaj ka Profit & Loss summary dikhao"
• "Customer ka Udhaar aur Supplier ka payable kitna hai?"
• "Konsa stock jaldi khatam hone wala hai? (Demand Forecast)"
• Voice / Text Sales: "Aaj 10 packet biscuit ki sale hui"
                """.trimIndent()
            )
        )
    )
    val chatMessages: StateFlow<List<AiChatMessage>> = _chatMessages.asStateFlow()

    fun setPeriod(period: DashboardPeriod) {
        _selectedPeriod.value = period
    }

    fun setAiLanguageMode(mode: LanguageMode) {
        _aiLanguageMode.value = mode
    }

    fun setSelectedCustomer(customer: CustomerEntity?) {
        _selectedCustomer.value = customer
    }

    fun setBillDiscount(discount: Double) {
        _billDiscount.value = maxOf(0.0, discount)
    }

    fun setPaymentMethod(method: String) {
        _paymentMethod.value = method
    }

    // --- POS Cart Operations with Multi-Item Support ---

    fun scanOrAddProductToCart(product: ProductEntity) {
        val current = _cartItems.value.toMutableMap()
        val existing = current[product.id]
        if (existing != null) {
            if (existing.quantity + 1 > product.currentStock) {
                emitMessage("Cannot add more: Stock limit reached (${product.currentStock} ${product.unit} available)")
                return
            }
            current[product.id] = existing.copy(quantity = existing.quantity + 1)
        } else {
            if (product.currentStock <= 0) {
                emitMessage("Cannot add: '${product.name}' is currently out of stock!")
                return
            }
            current[product.id] = CartItem(
                product = product,
                quantity = 1,
                unitPrice = product.sellingPrice
            )
        }
        _cartItems.value = current
    }

    fun updateCartItemQuantity(productId: Long, newQuantity: Int) {
        val current = _cartItems.value.toMutableMap()
        val item = current[productId] ?: return
        if (newQuantity <= 0) {
            current.remove(productId)
        } else {
            if (newQuantity > item.product.currentStock) {
                emitMessage("Cannot exceed available stock of ${item.product.currentStock} units")
                return
            }
            current[productId] = item.copy(quantity = newQuantity)
        }
        _cartItems.value = current
    }

    fun updateCartItemDiscount(productId: Long, discount: Double) {
        val current = _cartItems.value.toMutableMap()
        val item = current[productId] ?: return
        val maxDiscount = item.quantity * item.unitPrice
        val validDiscount = FinancialMath.roundMoney(minOf(maxDiscount, maxOf(0.0, discount)))
        current[productId] = item.copy(discount = validDiscount)
        _cartItems.value = current
    }

    fun clearCart() {
        _cartItems.value = emptyMap()
        _selectedCustomer.value = null
        _billDiscount.value = 0.0
    }

    fun handleScannedBarcodeForSales(barcode: String, onProductNotFound: (String) -> Unit = {}) {
        val trimmed = barcode.trim()
        val product = allProducts.value.find { it.barcode.equals(trimmed, ignoreCase = true) }
        if (product != null) {
            scanOrAddProductToCart(product)
            emitMessage("Scanned: ${product.name} (₹${product.sellingPrice})")
        } else {
            emitMessage("Barcode '$trimmed' not found in store.")
            onProductNotFound(trimmed)
        }
    }

    fun applySmartOffer(offer: SmartOffer) {
        when (offer.offerType) {
            "DISCOUNT" -> {
                if (offer.discountAmount > 0) {
                    _billDiscount.value = offer.discountAmount
                    emitMessage("Applied '${offer.title}': ₹${offer.discountAmount.toInt()} discount")
                }
            }
            "FREE_GIFT" -> {
                if (offer.giftProductId != null) {
                    val giftProd = allProducts.value.find { it.id == offer.giftProductId }
                    if (giftProd != null) {
                        val current = _cartItems.value.toMutableMap()
                        current[giftProd.id] = CartItem(
                            product = giftProd,
                            quantity = 1,
                            unitPrice = 0.0,
                            discount = 0.0
                        )
                        _cartItems.value = current
                        emitMessage("Added free gift: ${giftProd.name}")
                    }
                }
            }
            "EXPIRY_CLEARANCE" -> {
                if (offer.giftProductId != null) {
                    val prod = allProducts.value.find { it.id == offer.giftProductId }
                    if (prod != null) {
                        val current = _cartItems.value.toMutableMap()
                        val existing = current[prod.id]
                        if (existing != null) {
                            current[prod.id] = existing.copy(discount = offer.discountAmount)
                        } else {
                            current[prod.id] = CartItem(
                                product = prod,
                                quantity = 1,
                                unitPrice = offer.giftProductPrice,
                                discount = 0.0
                            )
                        }
                        _cartItems.value = current
                        emitMessage("Applied clearance pricing on ${prod.name}")
                    }
                }
            }
            "BUNDLE" -> {
                if (offer.giftProductId != null) {
                    val prod = allProducts.value.find { it.id == offer.giftProductId }
                    if (prod != null) {
                        scanOrAddProductToCart(prod)
                        emitMessage("Added upsell product: ${prod.name}")
                    }
                }
            }
        }
    }

    fun dismissSmartOffer(offer: SmartOffer) {
        _dismissedOfferIds.value = _dismissedOfferIds.value + offer.id
        emitMessage("Offer dismissed.")
    }

    /**
     * Prepares multi-item sale confirmation.
     * Bug 1B Fixed: Cart is NOT cleared here! It is preserved until DB transaction confirms.
     */
    fun prepareMultiItemSaleConfirmation() {
        val items = _cartItems.value.values.toList()
        if (items.isEmpty()) {
            emitMessage("Cart is empty. Scan or select products to checkout.")
            return
        }

        val totalUnits = items.sumOf { it.quantity }
        val subtotal = items.sumOf { it.quantity * it.unitPrice }
        val itemDiscounts = items.sumOf { it.discount }
        val bDiscount = _billDiscount.value
        val totalAmount = FinancialMath.roundMoney(maxOf(0.0, subtotal - (itemDiscounts + bDiscount)))

        val primaryName = if (items.size == 1) items.first().product.name else "${items.size} Products (${totalUnits} units)"
        val primaryBarcode = if (items.size == 1) items.first().product.barcode else "MULTI-ITEM"

        _pendingConfirmation.value = ActionConfirmationData(
            actionType = "SALE",
            title = "Confirm Multi-Item Sale",
            productName = primaryName,
            barcode = primaryBarcode,
            quantity = totalUnits,
            unitPrice = if (items.size == 1) items.first().unitPrice else 0.0,
            discount = itemDiscounts + bDiscount,
            totalAmount = totalAmount,
            previousStock = if (items.size == 1) items.first().product.currentStock else 0,
            resultingStock = if (items.size == 1) items.first().product.currentStock - items.first().quantity else 0,
            customerName = _selectedCustomer.value?.name ?: "Walk-in Customer",
            paymentMethod = _paymentMethod.value,
            onConfirmAction = {
                executeMultiItemSale()
            }
        )
    }

    /**
     * Executes the sale atomically.
     * Cart is cleared ONLY AFTER successful database commit!
     */
    private fun executeMultiItemSale() {
        viewModelScope.launch(Dispatchers.IO) {
            transactionMutex.withLock {
                val items = _cartItems.value.values.map {
                    SaleItemRequest(
                        productId = it.product.id,
                        quantity = it.quantity,
                        unitPrice = it.unitPrice,
                        discountAmount = it.discount,
                        taxRatePercent = it.product.taxRatePercent
                    )
                }
                if (items.isEmpty()) return@withLock

                val res = repository.recordMultiItemSale(
                    customerId = _selectedCustomer.value?.id,
                    customerName = _selectedCustomer.value?.name,
                    items = items,
                    billDiscountAmount = _billDiscount.value,
                    paymentMethod = _paymentMethod.value
                )

                when (res) {
                    is OperationResult.Success -> {
                        // CRITICAL FIX: Clear cart ONLY upon successful database persistence!
                        _cartItems.value = emptyMap()
                        _selectedCustomer.value = null
                        _billDiscount.value = 0.0
                        emitMessage(res.verifiedMessage)
                        _activeSlipTransaction.value = res.data.transaction
                    }
                    is OperationResult.Error -> {
                        // Cart data is untouched and preserved!
                        emitMessage("Sale Failed: ${res.errorMessage}")
                    }
                }
            }
        }
    }

    fun holdCurrentBill(label: String) {
        val items = _cartItems.value.values.toList()
        if (items.isEmpty()) {
            emitMessage("Cannot hold empty cart.")
            return
        }
        val total = items.sumOf { it.totalAmount }
        val json = items.map { "${it.product.id}:${it.quantity}:${it.unitPrice}:${it.discount}" }.joinToString(";")

        viewModelScope.launch(Dispatchers.IO) {
            val res = repository.holdBill(
                label = label.ifBlank { "Bill #${System.currentTimeMillis() % 1000}" },
                customerName = _selectedCustomer.value?.name,
                itemsJson = json,
                itemsCount = items.size,
                totalAmount = total
            )
            when (res) {
                is OperationResult.Success -> {
                    _cartItems.value = emptyMap()
                    _selectedCustomer.value = null
                    emitMessage("Bill held successfully.")
                }
                is OperationResult.Error -> emitMessage(res.errorMessage)
            }
        }
    }

    fun resumeBill(heldBill: HeldBillEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            val products = repository.getAllProductsSnapshot().associateBy { it.id }
            val map = mutableMapOf<Long, CartItem>()

            val parts = heldBill.itemsJson.split(";")
            for (p in parts) {
                val tokens = p.split(":")
                if (tokens.size >= 3) {
                    val pid = tokens[0].toLongOrNull() ?: continue
                    val qty = tokens[1].toIntOrNull() ?: 1
                    val price = tokens[2].toDoubleOrNull() ?: 0.0
                    val disc = if (tokens.size >= 4) tokens[3].toDoubleOrNull() ?: 0.0 else 0.0

                    val prod = products[pid]
                    if (prod != null) {
                        map[pid] = CartItem(
                            product = prod,
                            quantity = qty,
                            unitPrice = price,
                            discount = disc
                        )
                    }
                }
            }
            _cartItems.value = map
            repository.resumeHeldBill(heldBill.id)
            emitMessage("Resumed held bill '${heldBill.billLabel}'.")
        }
    }

    // --- Purchase Operations ---

    fun recordPurchase(
        productId: Long,
        supplierId: Long?,
        quantity: Int,
        unitCostPrice: Double,
        discountAmount: Double = 0.0,
        taxPercent: Double = 0.0,
        expiryDate: Long? = null,
        batchNumber: String? = null
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            transactionMutex.withLock {
                val req = PurchaseItemRequest(
                    productId = productId,
                    quantity = quantity,
                    unitCostPrice = unitCostPrice,
                    discountAmount = discountAmount,
                    taxPercent = taxPercent,
                    expiryDate = expiryDate,
                    batchNumber = batchNumber
                )
                val sup = if (supplierId != null && supplierId > 0) repository.getAllSuppliersSnapshot().find { it.id == supplierId } else null
                val res = repository.recordMultiItemPurchase(
                    supplierId = supplierId,
                    supplierName = sup?.name,
                    items = listOf(req)
                )
                when (res) {
                    is OperationResult.Success -> {
                        emitMessage(res.verifiedMessage)
                        _activeSlipTransaction.value = res.data.transaction
                    }
                    is OperationResult.Error -> emitMessage("Purchase Failed: ${res.errorMessage}")
                }
            }
        }
    }

    fun preparePurchaseConfirmation(
        product: ProductEntity,
        supplierId: Long?,
        supplierName: String?,
        quantity: Int,
        unitCost: Double,
        discount: Double = 0.0,
        taxPercent: Double = 0.0,
        expiryDate: Long? = null,
        batchNumber: String? = null
    ) {
        val base = quantity * unitCost
        val taxable = maxOf(0.0, base - discount)
        val tax = (taxable * taxPercent) / 100.0
        val total = taxable + tax

        _pendingConfirmation.value = ActionConfirmationData(
            actionType = "PURCHASE",
            title = "Confirm Stock Procurement",
            productName = product.name,
            barcode = product.barcode,
            quantity = quantity,
            unitPrice = unitCost,
            discount = discount,
            totalAmount = total,
            previousStock = product.currentStock,
            resultingStock = product.currentStock + quantity,
            supplierName = supplierName ?: "Direct / Cash Vendor",
            paymentMethod = "BANK_TRANSFER",
            onConfirmAction = {
                recordPurchase(
                    productId = product.id,
                    supplierId = supplierId,
                    quantity = quantity,
                    unitCostPrice = unitCost,
                    discountAmount = discount,
                    taxPercent = taxPercent,
                    expiryDate = expiryDate,
                    batchNumber = batchNumber
                )
            }
        )
    }

    // --- Multi-Item Purchase Cart Operations ---

    fun setSelectedPurchaseSupplier(supplier: SupplierEntity?) {
        _selectedPurchaseSupplier.value = supplier
    }

    fun setPurchasePaymentMethod(method: String) {
        _purchasePaymentMethod.value = method
    }

    fun scanOrAddProductToPurchaseCart(product: ProductEntity) {
        val current = _purchaseCartItems.value.toMutableMap()
        val existing = current[product.id]
        if (existing != null) {
            current[product.id] = existing.copy(quantity = existing.quantity + 1)
        } else {
            current[product.id] = PurchaseCartItem(
                product = product,
                quantity = 1,
                unitCostPrice = product.purchaseCostPrice,
                discount = 0.0,
                taxPercent = product.taxRatePercent,
                expiryDate = product.expiryDate,
                batchNumber = product.batchNumber
            )
        }
        _purchaseCartItems.value = current
    }

    fun handleScannedBarcodeForPurchase(barcode: String, onProductNotFound: (String) -> Unit = {}) {
        val trimmed = barcode.trim()
        val product = allProducts.value.find { it.barcode.equals(trimmed, ignoreCase = true) }
        if (product != null) {
            scanOrAddProductToPurchaseCart(product)
            emitMessage("Added '${product.name}' to Procurement Cart")
        } else {
            emitMessage("Barcode '$trimmed' not registered in store.")
            onProductNotFound(trimmed)
        }
    }

    fun updatePurchaseCartItemQuantity(productId: Long, newQuantity: Int) {
        val current = _purchaseCartItems.value.toMutableMap()
        if (newQuantity <= 0) {
            current.remove(productId)
        } else {
            val item = current[productId] ?: return
            current[productId] = item.copy(quantity = newQuantity)
        }
        _purchaseCartItems.value = current
    }

    fun updatePurchaseCartItemCostPrice(productId: Long, newCostPrice: Double) {
        val current = _purchaseCartItems.value.toMutableMap()
        val item = current[productId] ?: return
        current[productId] = item.copy(unitCostPrice = maxOf(0.0, newCostPrice))
        _purchaseCartItems.value = current
    }

    fun updatePurchaseCartItemDiscount(productId: Long, discount: Double) {
        val current = _purchaseCartItems.value.toMutableMap()
        val item = current[productId] ?: return
        current[productId] = item.copy(discount = maxOf(0.0, discount))
        _purchaseCartItems.value = current
    }

    fun updatePurchaseCartItemTax(productId: Long, taxPercent: Double) {
        val current = _purchaseCartItems.value.toMutableMap()
        val item = current[productId] ?: return
        current[productId] = item.copy(taxPercent = maxOf(0.0, taxPercent))
        _purchaseCartItems.value = current
    }

    fun updatePurchaseCartItemExpiry(productId: Long, expiryDate: Long?) {
        val current = _purchaseCartItems.value.toMutableMap()
        val item = current[productId] ?: return
        current[productId] = item.copy(expiryDate = expiryDate)
        _purchaseCartItems.value = current
    }

    fun updatePurchaseCartItemBatch(productId: Long, batchNumber: String?) {
        val current = _purchaseCartItems.value.toMutableMap()
        val item = current[productId] ?: return
        current[productId] = item.copy(batchNumber = batchNumber)
        _purchaseCartItems.value = current
    }

    fun clearPurchaseCart() {
        _purchaseCartItems.value = emptyMap()
        _selectedPurchaseSupplier.value = null
    }

    fun prepareMultiItemPurchaseConfirmation() {
        val items = _purchaseCartItems.value.values.toList()
        if (items.isEmpty()) {
            emitMessage("Procurement cart is empty. Add products to generate purchase invoice.")
            return
        }

        val totalUnits = items.sumOf { it.quantity }
        val totalAmount = FinancialMath.roundMoney(items.sumOf { it.totalAmount })
        val primaryName = if (items.size == 1) items.first().product.name else "${items.size} Products (${totalUnits} units)"
        val primaryBarcode = if (items.size == 1) items.first().product.barcode else "MULTI-PURCHASE"

        _pendingConfirmation.value = ActionConfirmationData(
            actionType = "PURCHASE",
            title = "Confirm Multi-Item Procurement",
            productName = primaryName,
            barcode = primaryBarcode,
            quantity = totalUnits,
            unitPrice = if (items.size == 1) items.first().unitCostPrice else 0.0,
            discount = items.sumOf { it.discount },
            totalAmount = totalAmount,
            previousStock = if (items.size == 1) items.first().product.currentStock else 0,
            resultingStock = if (items.size == 1) items.first().product.currentStock + items.first().quantity else 0,
            supplierName = _selectedPurchaseSupplier.value?.name ?: "Direct / Cash Vendor",
            paymentMethod = _purchasePaymentMethod.value,
            onConfirmAction = {
                executeMultiItemPurchase()
            }
        )
    }

    private fun executeMultiItemPurchase() {
        viewModelScope.launch(Dispatchers.IO) {
            transactionMutex.withLock {
                val requests = _purchaseCartItems.value.values.map {
                    PurchaseItemRequest(
                        productId = it.product.id,
                        quantity = it.quantity,
                        unitCostPrice = it.unitCostPrice,
                        discountAmount = it.discount,
                        taxPercent = it.taxPercent,
                        expiryDate = it.expiryDate,
                        batchNumber = it.batchNumber
                    )
                }
                if (requests.isEmpty()) return@withLock

                val res = repository.recordMultiItemPurchase(
                    supplierId = _selectedPurchaseSupplier.value?.id,
                    supplierName = _selectedPurchaseSupplier.value?.name,
                    items = requests,
                    paymentMethod = _purchasePaymentMethod.value
                )

                when (res) {
                    is OperationResult.Success -> {
                        _purchaseCartItems.value = emptyMap()
                        _selectedPurchaseSupplier.value = null
                        emitMessage(res.verifiedMessage)
                        _activeSlipTransaction.value = res.data.transaction
                    }
                    is OperationResult.Error -> {
                        emitMessage("Purchase Failed: ${res.errorMessage}")
                    }
                }
            }
        }
    }

    // --- Promotion Rules Management ---

    fun savePromotionRule(rule: PromotionRuleEntity, onComplete: (Boolean) -> Unit = {}) {
        viewModelScope.launch(Dispatchers.IO) {
            val res = repository.savePromotionRule(rule)
            when (res) {
                is OperationResult.Success -> {
                    emitMessage(res.verifiedMessage)
                    onComplete(true)
                }
                is OperationResult.Error -> {
                    emitMessage(res.errorMessage)
                    onComplete(false)
                }
            }
        }
    }

    fun togglePromotionRule(id: Long, isEnabled: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            val res = repository.togglePromotionRule(id, isEnabled)
            when (res) {
                is OperationResult.Success -> emitMessage(res.verifiedMessage)
                is OperationResult.Error -> emitMessage(res.errorMessage)
            }
        }
    }

    fun deletePromotionRule(rule: PromotionRuleEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            val res = repository.deletePromotionRule(rule)
            when (res) {
                is OperationResult.Success -> emitMessage(res.verifiedMessage)
                is OperationResult.Error -> emitMessage(res.errorMessage)
            }
        }
    }

    // --- Returns and Cancellations ---

    fun prepareCancelConfirmation(transaction: TransactionEntity) {
        _pendingConfirmation.value = ActionConfirmationData(
            actionType = "CANCEL",
            title = "Confirm Reversal / Cancellation",
            productName = transaction.productName,
            barcode = transaction.barcode,
            quantity = transaction.quantity,
            unitPrice = transaction.unitPrice,
            discount = transaction.discountAmount,
            totalAmount = transaction.finalTotalAmount,
            previousStock = transaction.resultingStock,
            resultingStock = transaction.previousStock,
            onConfirmAction = {
                executeCancellation(transaction.id, "User requested reversal from ledger")
            }
        )
    }

    private fun executeCancellation(transactionId: Long, reason: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val result = repository.cancelTransaction(transactionId, reason)
            when (result) {
                is OperationResult.Success -> {
                    emitMessage(result.verifiedMessage)
                    val current = _activeSlipTransaction.value
                    if (current?.id == transactionId) {
                        _activeSlipTransaction.value = current.copy(status = "CANCELLED")
                    }
                }
                is OperationResult.Error -> emitMessage("Reversal Failed: ${result.errorMessage}")
            }
        }
    }

    fun processSaleReturn(
        originalTransactionId: Long,
        productId: Long,
        quantity: Int,
        refundPrice: Double,
        costBasis: Double,
        condition: String, // SELLABLE, DAMAGED, QUARANTINED
        refundMethod: String,
        reason: String
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val req = SaleReturnItemRequest(
                transactionItemId = 0,
                productId = productId,
                quantityToReturn = quantity,
                unitRefundPrice = refundPrice,
                costBasis = costBasis
            )
            val res = repository.recordSaleReturn(
                originalTransactionId = originalTransactionId,
                returnItems = listOf(req),
                returnCondition = condition,
                refundPaymentMethod = refundMethod,
                reason = reason
            )
            when (res) {
                is OperationResult.Success -> {
                    emitMessage(res.verifiedMessage)
                    _activeSlipTransaction.value = res.data.transaction
                }
                is OperationResult.Error -> emitMessage(res.errorMessage)
            }
        }
    }

    // --- Stock Adjustments ---

    fun recordStockAdjustment(productId: Long, type: String, delta: Int, reason: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val res = repository.recordStockAdjustment(productId, type, delta, reason)
            when (res) {
                is OperationResult.Success -> emitMessage(res.verifiedMessage)
                is OperationResult.Error -> emitMessage(res.errorMessage)
            }
        }
    }

    // --- Customer & Supplier Operations ---

    fun addNewCustomer(customer: CustomerEntity, onComplete: (Boolean) -> Unit = {}) {
        viewModelScope.launch(Dispatchers.IO) {
            val res = repository.createCustomer(customer)
            when (res) {
                is OperationResult.Success -> {
                    emitMessage(res.verifiedMessage)
                    onComplete(true)
                }
                is OperationResult.Error -> {
                    emitMessage(res.errorMessage)
                    onComplete(false)
                }
            }
        }
    }

    fun recordCustomerPayment(customerId: Long, amount: Double, method: String, notes: String?) {
        viewModelScope.launch(Dispatchers.IO) {
            val res = repository.recordCustomerPayment(customerId, amount, method, notes)
            when (res) {
                is OperationResult.Success -> emitMessage(res.verifiedMessage)
                is OperationResult.Error -> emitMessage(res.errorMessage)
            }
        }
    }

    fun addNewSupplier(supplier: SupplierEntity, onComplete: (Boolean) -> Unit = {}) {
        viewModelScope.launch(Dispatchers.IO) {
            val res = repository.createSupplier(supplier)
            when (res) {
                is OperationResult.Success -> {
                    emitMessage(res.verifiedMessage)
                    onComplete(true)
                }
                is OperationResult.Error -> {
                    emitMessage(res.errorMessage)
                    onComplete(false)
                }
            }
        }
    }

    fun recordSupplierPayment(supplierId: Long, amount: Double, method: String, notes: String?) {
        viewModelScope.launch(Dispatchers.IO) {
            val res = repository.recordSupplierPayment(supplierId, amount, method, notes)
            when (res) {
                is OperationResult.Success -> emitMessage(res.verifiedMessage)
                is OperationResult.Error -> emitMessage(res.errorMessage)
            }
        }
    }

    // --- Expenses Operations ---

    fun addExpense(expense: ExpenseEntity, onComplete: (Boolean) -> Unit = {}) {
        viewModelScope.launch(Dispatchers.IO) {
            val res = repository.recordExpense(expense)
            when (res) {
                is OperationResult.Success -> {
                    emitMessage(res.verifiedMessage)
                    onComplete(true)
                }
                is OperationResult.Error -> {
                    emitMessage(res.errorMessage)
                    onComplete(false)
                }
            }
        }
    }

    fun deleteExpense(expense: ExpenseEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteExpense(expense)
            emitMessage("Expense deleted.")
        }
    }

    // --- Cash Closing Register ---

    fun recordDailyCashClosing(openingCash: Double, actualCash: Double, notes: String?, closedBy: String = "Store Owner") {
        viewModelScope.launch(Dispatchers.IO) {
            val res = repository.recordDailyCashClosing(openingCash, actualCash, notes, closedBy)
            when (res) {
                is OperationResult.Success -> emitMessage(res.verifiedMessage)
                is OperationResult.Error -> emitMessage(res.errorMessage)
            }
        }
    }

    // --- Product & Expiry CRUD ---

    fun addNewProduct(product: ProductEntity, onComplete: (Boolean) -> Unit = {}) {
        viewModelScope.launch(Dispatchers.IO) {
            val res = repository.createProduct(product)
            when (res) {
                is OperationResult.Success -> {
                    emitMessage(res.verifiedMessage)
                    onComplete(true)
                }
                is OperationResult.Error -> {
                    emitMessage(res.errorMessage)
                    onComplete(false)
                }
            }
        }
    }

    fun updateProduct(product: ProductEntity, onComplete: (Boolean) -> Unit = {}) {
        viewModelScope.launch(Dispatchers.IO) {
            val res = repository.updateProduct(product)
            when (res) {
                is OperationResult.Success -> {
                    emitMessage(res.verifiedMessage)
                    onComplete(true)
                }
                is OperationResult.Error -> {
                    emitMessage(res.errorMessage)
                    onComplete(false)
                }
            }
        }
    }

    fun updateProductExpiry(productId: Long, expiryDate: Long?) {
        viewModelScope.launch(Dispatchers.IO) {
            val res = repository.updateProductExpiry(productId, expiryDate)
            when (res) {
                is OperationResult.Success -> emitMessage(res.verifiedMessage)
                is OperationResult.Error -> emitMessage(res.errorMessage)
            }
        }
    }

    // --- Backup & Restore ---

    fun createBackup() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val path = BackupUtils.createLocalBackup(getApplication(), db)
                emitMessage("Backup created successfully: $path")
            } catch (e: Exception) {
                emitMessage("Backup failed: ${e.message}")
            }
        }
    }

    fun clearAllStoreData(onComplete: (Boolean) -> Unit = {}) {
        viewModelScope.launch(Dispatchers.IO) {
            val res = repository.clearAllData()
            when (res) {
                is OperationResult.Success -> {
                    emitMessage(res.verifiedMessage)
                    _cartItems.value = emptyMap()
                    _activeSlipTransaction.value = null
                    onComplete(true)
                }
                is OperationResult.Error -> {
                    emitMessage(res.errorMessage)
                    onComplete(false)
                }
            }
        }
    }

    // --- AI Chat & Voice Assistant ---

    fun sendAiUserQuery(query: String) {
        if (query.isBlank()) return
        val userMsg = AiChatMessage(isUser = true, message = query.trim())
        _chatMessages.value = _chatMessages.value + userMsg

        viewModelScope.launch(Dispatchers.Default) {
            val products = repository.getAllProductsSnapshot()
            val transactions = repository.getAllTransactionsSnapshot()
            val suppliers = repository.getAllSuppliersSnapshot()
            val customers = repository.getAllCustomersSnapshot()
            val expenses = repository.getAllExpensesSnapshot()
            val items = repository.getAllTransactionItemsSnapshot()

            // Check if user provided voice command like "Aaj 10 packet biscuit ki sale hui"
            val voiceAction = aiEngine.parseVoiceCommand(query, products)
            if (voiceAction != null && voiceAction.matchedProduct != null && voiceAction.confidence >= 0.5) {
                val prod = voiceAction.matchedProduct
                val qty = voiceAction.detectedQuantity
                val price = voiceAction.detectedPrice ?: prod.sellingPrice

                _pendingConfirmation.value = ActionConfirmationData(
                    actionType = voiceAction.actionType,
                    title = "Confirm Voice Assistant Action",
                    productName = prod.name,
                    barcode = prod.barcode,
                    quantity = qty,
                    unitPrice = price,
                    discount = 0.0,
                    totalAmount = qty * price,
                    previousStock = prod.currentStock,
                    resultingStock = if (voiceAction.actionType == "SALE") prod.currentStock - qty else prod.currentStock + qty,
                    onConfirmAction = {
                        if (voiceAction.actionType == "SALE") {
                            scanOrAddProductToCart(prod.copy(sellingPrice = price))
                            prepareMultiItemSaleConfirmation()
                        } else {
                            recordPurchase(prod.id, null, qty, price)
                        }
                    }
                )
            }

            val answer = aiEngine.analyzeAndAnswer(
                userQuery = query,
                selectedLang = _aiLanguageMode.value,
                products = products,
                transactions = transactions,
                suppliers = suppliers,
                customers = customers,
                expenses = expenses,
                transactionItems = items
            )

            val botMsg = AiChatMessage(isUser = false, message = answer)
            _chatMessages.value = _chatMessages.value + botMsg
        }
    }

    fun showSlip(transaction: TransactionEntity) {
        _activeSlipTransaction.value = transaction
    }

    fun dismissSlip() {
        _activeSlipTransaction.value = null
    }

    fun dismissConfirmation() {
        _pendingConfirmation.value = null
    }

    private fun emitMessage(msg: String) {
        viewModelScope.launch {
            _uiEventMessage.emit(msg)
        }
    }
}
