package com.example.data.ai

import com.example.data.model.CustomerEntity
import com.example.data.model.ExpenseEntity
import com.example.data.model.ProductEntity
import com.example.data.model.SupplierEntity
import com.example.data.model.TransactionEntity
import com.example.data.model.TransactionItemEntity
import com.example.util.FinancialMath
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.regex.Pattern

enum class LanguageMode {
    AUTO,
    ENGLISH,
    HINDI,
    HINGLISH
}

data class BusinessMetricReport(
    val title: String,
    val periodLabel: String,
    val totalRevenue: Double,
    val totalCogs: Double,
    val totalDiscounts: Double,
    val netProfit: Double,
    val profitMarginPercent: Double,
    val totalPurchases: Double,
    val totalTransactions: Int,
    val totalSalesUnits: Int,
    val totalStockValuationCost: Double,
    val totalStockValuationRetail: Double,
    val lowStockCount: Int,
    val outOfStockCount: Int,
    val topSellingItems: List<ProductSalesMetric>,
    val slowMovingItems: List<ProductSalesMetric>,
    val lowStockItems: List<ProductEntity>,
    val supplierSpends: Map<String, Double>
)

data class ProductSalesMetric(
    val productId: Long,
    val productName: String,
    val unitsSold: Int,
    val revenue: Double,
    val profit: Double
)

data class ParsedVoiceAction(
    val actionType: String, // "SALE", "PURCHASE", "QUERY"
    val detectedProductName: String?,
    val matchedProduct: ProductEntity?,
    val detectedQuantity: Int,
    val detectedPrice: Double?,
    val estimatedTotal: Double,
    val confidence: Double,
    val confirmationPrompt: String
)

class StockSaathiAiEngine {
    private val forecastingEngine = DemandForecastingEngine()

    fun computeMetrics(
        products: List<ProductEntity>,
        transactions: List<TransactionEntity>,
        suppliers: List<SupplierEntity>,
        startTime: Long = 0L,
        endTime: Long = Long.MAX_VALUE,
        periodLabel: String = "All Time"
    ): BusinessMetricReport {
        val validTx = transactions.filter {
            it.status == "COMMITTED" && it.timestamp in startTime..endTime
        }

        val productMap = products.associateBy { it.id }

        var totalRevenue = 0.0
        var totalCogs = 0.0
        var totalDiscounts = 0.0
        var totalPurchases = 0.0
        var salesUnits = 0

        val productSalesMap = mutableMapOf<Long, Pair<Int, Double>>() // productId -> (units, revenue)
        val supplierSpendMap = mutableMapOf<String, Double>()

        for (tx in validTx) {
            when (tx.transactionType) {
                "SALE" -> {
                    totalRevenue += tx.finalTotalAmount
                    totalDiscounts += (tx.discountAmount + tx.billDiscountAmount)
                    salesUnits += tx.quantity

                    val product = productMap[tx.productId]
                    val cost = product?.purchaseCostPrice ?: tx.unitPrice
                    totalCogs += (cost * tx.quantity)

                    val curr = productSalesMap.getOrDefault(tx.productId, Pair(0, 0.0))
                    productSalesMap[tx.productId] = Pair(curr.first + tx.quantity, curr.second + tx.finalTotalAmount)
                }
                "PURCHASE" -> {
                    totalPurchases += tx.finalTotalAmount
                    val supName = tx.supplierName ?: "Direct / Cash Supplier"
                    val currentSpend = supplierSpendMap.getOrDefault(supName, 0.0)
                    supplierSpendMap[supName] = currentSpend + tx.finalTotalAmount
                }
                "SALE_RETURN" -> {
                    totalRevenue -= tx.finalTotalAmount
                    salesUnits -= tx.quantity
                }
            }
        }

        totalRevenue = FinancialMath.roundMoney(maxOf(0.0, totalRevenue))
        totalCogs = FinancialMath.roundMoney(maxOf(0.0, totalCogs))
        val netProfit = FinancialMath.roundMoney(totalRevenue - totalCogs)
        val profitMarginPercent = if (totalRevenue > 0.0) {
            FinancialMath.roundMoney((netProfit / totalRevenue) * 100.0)
        } else {
            0.0
        }

        val topSelling = productSalesMap.entries
            .filter { it.key > 0L }
            .sortedByDescending { it.value.second }
            .take(5)
            .map { (pid, data) ->
                val prod = productMap[pid]
                val name = prod?.name ?: "Product #$pid"
                val cost = (prod?.purchaseCostPrice ?: 0.0) * data.first
                val profit = FinancialMath.roundMoney(data.second - cost)
                ProductSalesMetric(
                    productId = pid,
                    productName = name,
                    unitsSold = data.first,
                    revenue = FinancialMath.roundMoney(data.second),
                    profit = profit
                )
            }

        val lowStockList = products.filter { it.currentStock <= it.minStockThreshold }
        val outOfStockCount = products.count { it.currentStock <= 0 }

        var totalValuationCost = 0.0
        var totalValuationRetail = 0.0
        for (p in products) {
            totalValuationCost += (p.currentStock * p.purchaseCostPrice)
            totalValuationRetail += (p.currentStock * p.sellingPrice)
        }

        return BusinessMetricReport(
            title = "Store Audit Report",
            periodLabel = periodLabel,
            totalRevenue = totalRevenue,
            totalCogs = totalCogs,
            totalDiscounts = totalDiscounts,
            netProfit = netProfit,
            profitMarginPercent = profitMarginPercent,
            totalPurchases = totalPurchases,
            totalTransactions = validTx.size,
            totalSalesUnits = salesUnits,
            totalStockValuationCost = FinancialMath.roundMoney(totalValuationCost),
            totalStockValuationRetail = FinancialMath.roundMoney(totalValuationRetail),
            lowStockCount = lowStockList.size,
            outOfStockCount = outOfStockCount,
            topSellingItems = topSelling,
            slowMovingItems = emptyList(),
            lowStockItems = lowStockList,
            supplierSpends = supplierSpendMap
        )
    }

    // --- Verified AI Business Query Functions (Zero Hallucination) ---

    fun getSalesSummary(transactions: List<TransactionEntity>): String {
        val sales = transactions.filter { it.transactionType == "SALE" && it.status == "COMMITTED" }
        val totalRevenue = sales.sumOf { it.finalTotalAmount }
        val totalUnits = sales.sumOf { it.quantity }
        return "Total Verified Sales: ${FinancialMath.formatInr(totalRevenue)} across ${sales.size} bills ($totalUnits units sold)."
    }

    fun getPurchaseSummary(transactions: List<TransactionEntity>): String {
        val purchases = transactions.filter { it.transactionType == "PURCHASE" && it.status == "COMMITTED" }
        val totalSpent = purchases.sumOf { it.finalTotalAmount }
        val totalUnits = purchases.sumOf { it.quantity }
        return "Total Verified Procurement: ${FinancialMath.formatInr(totalSpent)} across ${purchases.size} supplier invoices ($totalUnits units received)."
    }

    fun getProductStock(query: String, products: List<ProductEntity>): String {
        val matches = products.filter {
            it.name.contains(query, ignoreCase = true) || it.barcode.contains(query, ignoreCase = true)
        }
        if (matches.isEmpty()) return "Product '$query' not found in inventory."
        val p = matches.first()
        return "'${p.name}' (Barcode: ${p.barcode}): Current stock is ${p.currentStock} ${p.unit}. Buying cost (WAC): ₹${p.purchaseCostPrice}, Selling price: ₹${p.sellingPrice}."
    }

    fun getLowStockProducts(products: List<ProductEntity>): String {
        val low = products.filter { it.currentStock <= it.minStockThreshold }
        if (low.isEmpty()) return "All inventory is currently above minimum safety thresholds."
        val lines = low.take(6).map { "• ${it.name}: ${it.currentStock} ${it.unit} left (Alert threshold: ${it.minStockThreshold})" }
        return "Found ${low.size} items needing restocking:\n" + lines.joinToString("\n")
    }

    fun getProfitAndLoss(
        products: List<ProductEntity>,
        transactions: List<TransactionEntity>,
        transactionItems: List<TransactionItemEntity>,
        expenses: List<ExpenseEntity>
    ): String {
        val stmt = FinancialMath.calculateFinancials(transactions, transactionItems, expenses)
        return """
P&L Financial Statement (Verified from Ledger):
• Gross Sales: ${FinancialMath.formatInr(stmt.grossSales)}
• Discounts Given: -${FinancialMath.formatInr(stmt.totalDiscounts)}
• Sales Returns: -${FinancialMath.formatInr(stmt.salesReturns)}
• Net Sales: ${FinancialMath.formatInr(stmt.netSales)}
• Cost of Goods Sold (COGS): -${FinancialMath.formatInr(stmt.cogs)}
• Gross Profit: ${FinancialMath.formatInr(stmt.grossProfit)} (Margin: ${stmt.grossMarginPercent}%)
• Operating Expenses: -${FinancialMath.formatInr(stmt.operatingExpenses)}
• Net Profit (मुनाफा): ${FinancialMath.formatInr(stmt.netProfit)} (Net Margin: ${stmt.netMarginPercent}%)
• GST Collected (Tax Liability, not income): ${FinancialMath.formatInr(stmt.gstCollected)}
        """.trimIndent()
    }

    fun getCustomerOutstanding(customers: List<CustomerEntity>): String {
        val withDue = customers.filter { it.outstandingBalance > 0.0 }
        if (withDue.isEmpty()) return "No customers currently have pending credit / Udhaar balances."
        val totalDue = withDue.sumOf { it.outstandingBalance }
        val lines = withDue.take(5).map { "• ${it.name} (${it.mobileNumber}): ${FinancialMath.formatInr(it.outstandingBalance)}" }
        return "Total Customer Receivables (Udhaar): ${FinancialMath.formatInr(totalDue)} from ${withDue.size} customers:\n" + lines.joinToString("\n")
    }

    fun getSupplierOutstanding(suppliers: List<SupplierEntity>): String {
        val withPayable = suppliers.filter { it.outstandingPayable > 0.0 }
        if (withPayable.isEmpty()) return "All supplier bills are fully cleared. No payables due."
        val totalPayable = withPayable.sumOf { it.outstandingPayable }
        val lines = withPayable.map { "• ${it.name}: ${FinancialMath.formatInr(it.outstandingPayable)}" }
        return "Total Supplier Payables: ${FinancialMath.formatInr(totalPayable)} across ${withPayable.size} vendors:\n" + lines.joinToString("\n")
    }

    fun getDemandForecast(
        products: List<ProductEntity>,
        transactions: List<TransactionEntity>,
        transactionItems: List<TransactionItemEntity>
    ): String {
        val forecast = forecastingEngine.generateForecasts(products, transactions, transactionItems)
        val critical = forecast.forecasts.filter { it.stockoutRisk == RiskLevel.CRITICAL }
        if (critical.isEmpty()) {
            return "Demand Forecasting: No critical stockout risks identified for the next 14 days across ${products.size} products."
        }
        val lines = critical.take(5).map { "• ${it.productName}: ${it.explanation} (Recommended purchase: ${it.recommendedPurchaseQty} units)" }
        return "Demand Forecast Alert: ${critical.size} products face critical stockout risk:\n" + lines.joinToString("\n")
    }

    /**
     * Parses voice-to-text or typed user actions (Rule 11: Voice-Based Shop Assistant)
     * e.g. "Aaj 10 packet biscuit ki sale hui", "Sale 5 Parle-G", "Purchase 20 Oil @ 120"
     */
    fun parseVoiceCommand(
        query: String,
        products: List<ProductEntity>
    ): ParsedVoiceAction? {
        val lower = query.lowercase(Locale.getDefault())

        val isSale = lower.contains("sale") || lower.contains("bikri") || lower.contains("becha") || lower.contains("sold")
        val isPurchase = lower.contains("purchase") || lower.contains("khareed") || lower.contains("buy") || lower.contains("stock in")

        if (!isSale && !isPurchase) return null

        // Extract quantity using regex
        val qtyMatcher = Pattern.compile("(\\d+)\\s*(packet|piece|pcs|unit|kg|ltr|box|taad)?").matcher(lower)
        val quantity = if (qtyMatcher.find()) {
            qtyMatcher.group(1)?.toIntOrNull() ?: 1
        } else {
            1
        }

        // Fuzzy match product with stemming
        var bestMatch: ProductEntity? = null
        var highestScore = 0
        for (prod in products) {
            val pName = prod.name.lowercase(Locale.getDefault())
            val words = pName.split(" ", "-", "(", ")").filter { it.length > 2 }
            var score = 0
            for (w in words) {
                val stem = w.trimEnd('s', 'e')
                if (lower.contains(w) || (stem.length >= 3 && lower.contains(stem))) score += 2
            }
            val catStem = prod.category.lowercase(Locale.getDefault()).trimEnd('s', 'e')
            if (lower.contains(catStem)) score += 1
            if (score > highestScore) {
                highestScore = score
                bestMatch = prod
            }
        }

        if (bestMatch == null) {
            return ParsedVoiceAction(
                actionType = if (isSale) "SALE" else "PURCHASE",
                detectedProductName = null,
                matchedProduct = null,
                detectedQuantity = quantity,
                detectedPrice = null,
                estimatedTotal = 0.0,
                confidence = 0.0,
                confirmationPrompt = "I heard the ${if (isSale) "sale" else "purchase"} quantity of $quantity, but couldn't locate the exact product in inventory. Please select the product manually."
            )
        }

        val price = if (isSale) bestMatch.sellingPrice else bestMatch.purchaseCostPrice
        val total = quantity * price
        val actionLabel = if (isSale) "Sale" else "Purchase"

        return ParsedVoiceAction(
            actionType = if (isSale) "SALE" else "PURCHASE",
            detectedProductName = bestMatch.name,
            matchedProduct = bestMatch,
            detectedQuantity = quantity,
            detectedPrice = price,
            estimatedTotal = total,
            confidence = if (highestScore >= 2) 0.85 else 0.5,
            confirmationPrompt = "Ready to record $actionLabel of $quantity ${bestMatch.unit} '${bestMatch.name}' at ₹$price each (Total: ₹$total). Please confirm to commit."
        )
    }

    /**
     * Answers queries in natural language (Hindi, Hinglish, English) using verified functions.
     */
    fun analyzeAndAnswer(
        userQuery: String,
        selectedLang: LanguageMode,
        products: List<ProductEntity>,
        transactions: List<TransactionEntity>,
        suppliers: List<SupplierEntity>,
        customers: List<CustomerEntity> = emptyList(),
        expenses: List<ExpenseEntity> = emptyList(),
        transactionItems: List<TransactionItemEntity> = emptyList()
    ): String {
        val q = userQuery.lowercase(Locale.getDefault())

        // 1. Profit & Loss / Financials
        if (q.contains("profit") || q.contains("munafa") || q.contains("faida") || q.contains("labh") || q.contains("p&l") || q.contains("loss")) {
            return getProfitAndLoss(products, transactions, transactionItems, expenses)
        }

        // 2. Sales / Revenue
        if (q.contains("sale") || q.contains("bikri") || q.contains("revenue") || q.contains("kamai")) {
            return getSalesSummary(transactions)
        }

        // 3. Purchase / Procurement
        if (q.contains("purchase") || q.contains("khareed") || q.contains("spend") || q.contains("vendor")) {
            return getPurchaseSummary(transactions)
        }

        // 4. Low Stock / Out of Stock
        if (q.contains("low") || q.contains("khatam") || q.contains("kam") || q.contains("empty") || q.contains("stockout")) {
            return getLowStockProducts(products)
        }

        // 5. Customer Udhaar / Receivables
        if (q.contains("udhaar") || q.contains("customer") || q.contains("receivable") || q.contains("baaki") || q.contains("credit")) {
            return getCustomerOutstanding(customers)
        }

        // 6. Supplier Payables
        if (q.contains("supplier") || q.contains("payable") || q.contains("dena")) {
            return getSupplierOutstanding(suppliers)
        }

        // 7. Demand Forecast
        if (q.contains("forecast") || q.contains("demand") || q.contains("prediction") || q.contains("reorder") || q.contains("kya mangwana")) {
            return getDemandForecast(products, transactions, transactionItems)
        }

        // 8. Specific product query
        for (p in products) {
            if (q.contains(p.name.lowercase(Locale.getDefault())) || q.contains(p.barcode)) {
                return getProductStock(p.name, products)
            }
        }

        // Default intelligence overview
        val stmt = FinancialMath.calculateFinancials(transactions, transactionItems, expenses)
        return """
StockSaathi Verified Business Overview:
• Total Net Revenue: ${FinancialMath.formatInr(stmt.netSales)}
• Realized Net Profit: ${FinancialMath.formatInr(stmt.netProfit)} (Margin: ${stmt.netMarginPercent}%)
• Active SKUs in Stock: ${products.size} (${products.count { it.currentStock <= it.minStockThreshold }} low-stock alerts)
• Customer Outstanding (Udhaar): ${FinancialMath.formatInr(customers.sumOf { it.outstandingBalance })}

You can ask me anything about sales, purchase orders, product stock, profit & loss, customer Udhaar, or AI demand forecasts.
        """.trimIndent()
    }
}
