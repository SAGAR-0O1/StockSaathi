package com.example.data.ai

import com.example.data.model.ProductEntity
import com.example.data.model.TransactionEntity
import com.example.data.model.TransactionItemEntity
import java.util.Calendar
import java.util.Locale
import kotlin.math.roundToInt

data class ProductForecast(
    val productId: Long,
    val productName: String,
    val barcode: String,
    val category: String,
    val currentStock: Int,
    val unitsSold7d: Int,
    val unitsSold30d: Int,
    val unitsSold90d: Int,
    val dailyRunRate: Double,
    val daysOfInventoryRemaining: Double,
    val expectedDemandNext14d: Int,
    val expectedDemandNext30d: Int,
    val recommendedPurchaseQty: Int,
    val stockoutRisk: RiskLevel,
    val overstockRisk: Boolean,
    val isSlowMoving: Boolean,
    val confidenceLevel: String, // "High (Based on 90d velocity)", "Moderate (Limited history)", "Preliminary"
    val explanation: String
)

enum class RiskLevel(val label: String) {
    CRITICAL("CRITICAL: Out of Stock or Emptying in < 3 days"),
    HIGH("HIGH: Depleting within 7 days"),
    MODERATE("MODERATE: Reorder window approaching"),
    LOW("LOW: Healthy inventory buffer")
}

data class DemandForecastSummary(
    val generatedAt: Long = System.currentTimeMillis(),
    val totalProductsAnalyzed: Int,
    val criticalStockoutsCount: Int,
    val recommendedProcurementSpend: Double,
    val forecasts: List<ProductForecast>
)

class DemandForecastingEngine {

    fun generateForecasts(
        products: List<ProductEntity>,
        transactions: List<TransactionEntity>,
        transactionItems: List<TransactionItemEntity>,
        supplierLeadTimeDays: Int = 3
    ): DemandForecastSummary {
        val now = System.currentTimeMillis()
        val dayMillis = 86400000L

        val t7d = now - (7 * dayMillis)
        val t30d = now - (30 * dayMillis)
        val t90d = now - (90 * dayMillis)

        // Filter valid committed sale items
        val salesTxIds = transactions.filter { it.transactionType == "SALE" && it.status == "COMMITTED" }
            .associateBy { it.id }

        val validItems = transactionItems.filter { salesTxIds.containsKey(it.transactionId) }
        val itemsByProduct = validItems.groupBy { it.productId }

        // Also check transactions directly for backward-compatibility single item sales
        val directTxByProduct = transactions.filter { it.transactionType == "SALE" && it.status == "COMMITTED" && it.productId > 0 }
            .groupBy { it.productId }

        val forecasts = mutableListOf<ProductForecast>()
        var criticalCount = 0
        var totalProcurementEstimate = 0.0

        for (product in products) {
            val pItems = itemsByProduct[product.id] ?: emptyList()
            val pDirect = directTxByProduct[product.id] ?: emptyList()

            var sold7d = 0
            var sold30d = 0
            var sold90d = 0

            if (pItems.isNotEmpty()) {
                for (item in pItems) {
                    val tx = salesTxIds[item.transactionId] ?: continue
                    val qty = item.quantity
                    if (tx.timestamp >= t7d) sold7d += qty
                    if (tx.timestamp >= t30d) sold30d += qty
                    if (tx.timestamp >= t90d) sold90d += qty
                }
            } else {
                for (tx in pDirect) {
                    val qty = tx.quantity
                    if (tx.timestamp >= t7d) sold7d += qty
                    if (tx.timestamp >= t30d) sold30d += qty
                    if (tx.timestamp >= t90d) sold90d += qty
                }
            }

            // Weighted daily velocity: give higher weight to recent 7d velocity
            // velocity = (0.5 * (7d / 7)) + (0.3 * (30d / 30)) + (0.2 * (90d / 90))
            val v7 = sold7d / 7.0
            val v30 = sold30d / 30.0
            val v90 = sold90d / 90.0
            val dailyRunRate = if (sold90d > 0) {
                (0.5 * v7) + (0.3 * v30) + (0.2 * v90)
            } else if (sold7d > 0) {
                v7
            } else {
                0.0
            }

            val daysRemaining = if (dailyRunRate > 0.0) {
                product.currentStock / dailyRunRate
            } else if (product.currentStock == 0) {
                0.0
            } else {
                999.0
            }

            val expectedDemand14d = (dailyRunRate * 14).roundToInt()
            val expectedDemand30d = (dailyRunRate * 30).roundToInt()

            // Recommended purchase quantity to cover lead time + 14-day safety buffer
            val bufferDays = 14 + supplierLeadTimeDays
            val idealStock = (dailyRunRate * bufferDays).roundToInt()
            val recommendedQty = maxOf(0, idealStock - product.currentStock)

            val riskLevel = when {
                product.currentStock <= 0 -> RiskLevel.CRITICAL
                daysRemaining <= 3.0 -> RiskLevel.CRITICAL
                daysRemaining <= 7.0 -> RiskLevel.HIGH
                daysRemaining <= 14.0 -> RiskLevel.MODERATE
                else -> RiskLevel.LOW
            }

            if (riskLevel == RiskLevel.CRITICAL) criticalCount++

            val isOverstock = daysRemaining > 60.0 && product.currentStock > 20
            val isSlowMoving = sold30d == 0 && product.currentStock > 0

            val confidence = when {
                sold90d >= 30 -> "High (Based on 90-day velocity)"
                sold30d >= 10 -> "Moderate (30-day velocity pattern)"
                else -> "Preliminary (Limited transaction history)"
            }

            val explanation = when (riskLevel) {
                RiskLevel.CRITICAL -> {
                    if (product.currentStock == 0) {
                        "Currently out of stock! Lost demand estimated at ${(dailyRunRate * 7).roundToInt()} units/week."
                    } else {
                        "Only ${String.format(Locale.US, "%.1f", daysRemaining)} days of inventory left at daily velocity of ${String.format(Locale.US, "%.1f", dailyRunRate)} units/day."
                    }
                }
                RiskLevel.HIGH -> "Stock will run out in ~${daysRemaining.roundToInt()} days before next standard order cycle."
                RiskLevel.MODERATE -> "Inventory is healthy today but reorder trigger is recommended within the week."
                RiskLevel.LOW -> {
                    if (isOverstock) "Overstocked: ~${daysRemaining.roundToInt()} days of stock on hand. Avoid fresh purchases."
                    else "Optimal inventory level maintained."
                }
            }

            totalProcurementEstimate += (recommendedQty * product.purchaseCostPrice)

            forecasts.add(
                ProductForecast(
                    productId = product.id,
                    productName = product.name,
                    barcode = product.barcode,
                    category = product.category,
                    currentStock = product.currentStock,
                    unitsSold7d = sold7d,
                    unitsSold30d = sold30d,
                    unitsSold90d = sold90d,
                    dailyRunRate = dailyRunRate,
                    daysOfInventoryRemaining = daysRemaining,
                    expectedDemandNext14d = expectedDemand14d,
                    expectedDemandNext30d = expectedDemand30d,
                    recommendedPurchaseQty = recommendedQty,
                    stockoutRisk = riskLevel,
                    overstockRisk = isOverstock,
                    isSlowMoving = isSlowMoving,
                    confidenceLevel = confidence,
                    explanation = explanation
                )
            )
        }

        // Sort: Critical first, then High, then Moderate
        forecasts.sortBy {
            when (it.stockoutRisk) {
                RiskLevel.CRITICAL -> 0
                RiskLevel.HIGH -> 1
                RiskLevel.MODERATE -> 2
                RiskLevel.LOW -> 3
            }
        }

        return DemandForecastSummary(
            totalProductsAnalyzed = products.size,
            criticalStockoutsCount = criticalCount,
            recommendedProcurementSpend = totalProcurementEstimate,
            forecasts = forecasts
        )
    }
}
