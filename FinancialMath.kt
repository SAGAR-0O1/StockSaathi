package com.example.util

import com.example.data.model.ExpenseEntity
import com.example.data.model.TransactionEntity
import com.example.data.model.TransactionItemEntity
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.Locale

object FinancialMath {

    fun roundMoney(value: Double): Double {
        if (value.isNaN() || value.isInfinite()) return 0.0
        return BigDecimal.valueOf(value)
            .setScale(2, RoundingMode.HALF_UP)
            .toDouble()
    }

    fun roundToPlaces(value: Double, places: Int = 1): Double {
        if (value.isNaN() || value.isInfinite()) return 0.0
        return BigDecimal.valueOf(value)
            .setScale(places, RoundingMode.HALF_UP)
            .toDouble()
    }

    fun formatInr(value: Double): String {
        return "₹" + String.format(Locale.US, "%,.2f", roundMoney(value))
    }

    /**
     * Calculates new Weighted Average Cost (WAC) upon new procurement.
     * WAC = ((Old Stock * Old WAC) + (New Purchase Qty * New Unit Cost)) / (Old Stock + New Purchase Qty)
     */
    fun calculateWeightedAverageCost(
        currentStock: Int,
        currentWac: Double,
        purchaseQuantity: Int,
        newPurchasePrice: Double
    ): Double {
        if (currentStock <= 0) {
            return roundMoney(newPurchasePrice)
        }
        val currentTotalValuation = currentStock * currentWac
        val newIncomingValuation = purchaseQuantity * newPurchasePrice
        val totalUnits = currentStock + purchaseQuantity
        if (totalUnits <= 0) return roundMoney(newPurchasePrice)

        return roundMoney((currentTotalValuation + newIncomingValuation) / totalUnits)
    }

    data class ComprehensiveFinancialStatement(
        val grossSales: Double,
        val itemDiscounts: Double,
        val billDiscounts: Double,
        val totalDiscounts: Double,
        val salesReturns: Double,
        val netSales: Double,
        val gstCollected: Double, // Strictly tax liability, NOT business revenue!
        val cogs: Double, // Historical cost basis of goods sold
        val grossProfit: Double, // Net Sales - COGS
        val grossMarginPercent: Double,
        val operatingExpenses: Double,
        val netProfit: Double, // Gross Profit - Operating Expenses
        val netMarginPercent: Double,
        val totalPurchases: Double,
        val gstPaidOnPurchases: Double,
        val totalUnitsSold: Int,
        val totalBillsCount: Int,
        val cashInflow: Double,
        val cashOutflow: Double,
        val netCashFlow: Double
    )

    fun calculateFinancials(
        transactions: List<TransactionEntity>,
        transactionItems: List<TransactionItemEntity>,
        expenses: List<ExpenseEntity>,
        startTime: Long = 0L,
        endTime: Long = Long.MAX_VALUE
    ): ComprehensiveFinancialStatement {
        val periodTx = transactions.filter {
            it.status == "COMMITTED" && it.timestamp in startTime..endTime
        }
        val periodExpenses = expenses.filter { it.date in startTime..endTime }
        val itemsByTxId = transactionItems.groupBy { it.transactionId }

        var grossSales = 0.0
        var itemDiscounts = 0.0
        var billDiscounts = 0.0
        var salesReturns = 0.0
        var gstCollected = 0.0
        var cogs = 0.0
        var totalPurchases = 0.0
        var gstPaidOnPurchases = 0.0
        var totalUnitsSold = 0
        var totalBillsCount = 0

        var cashInflow = 0.0
        var cashOutflow = 0.0

        for (tx in periodTx) {
            when (tx.transactionType) {
                "SALE" -> {
                    totalBillsCount++
                    billDiscounts += tx.billDiscountAmount

                    val items = itemsByTxId[tx.id]
                    if (!items.isNullOrEmpty()) {
                        for (item in items) {
                            val lineGross = item.quantity * item.unitPrice
                            grossSales += lineGross
                            itemDiscounts += item.discountAmount
                            gstCollected += item.taxAmount
                            cogs += (item.quantity * item.costBasis)
                            totalUnitsSold += item.quantity
                        }
                    } else {
                        // Single item fallback for backward compatibility
                        val lineGross = tx.quantity * tx.unitPrice
                        grossSales += lineGross
                        itemDiscounts += tx.discountAmount
                        gstCollected += tx.taxAmount
                        cogs += (tx.quantity * tx.unitPrice) // Fallback if no items table yet
                        totalUnitsSold += tx.quantity
                    }

                    // Cash flow tracking
                    if (tx.paymentMethod == "CASH") {
                        cashInflow += tx.paidAmount
                    } else if (tx.paymentMethod == "SPLIT") {
                        cashInflow += tx.paidAmount // Split cash portion
                    }
                }

                "SALE_RETURN" -> {
                    salesReturns += tx.finalTotalAmount
                    val items = itemsByTxId[tx.id]
                    if (!items.isNullOrEmpty()) {
                        for (item in items) {
                            cogs -= (item.quantity * item.costBasis)
                            totalUnitsSold -= item.quantity
                        }
                    } else {
                        totalUnitsSold -= tx.quantity
                    }
                    if (tx.paymentMethod == "CASH") {
                        cashOutflow += tx.finalTotalAmount
                    }
                }

                "PURCHASE" -> {
                    totalPurchases += tx.finalTotalAmount
                    gstPaidOnPurchases += tx.taxAmount
                    if (tx.paymentMethod == "CASH") {
                        cashOutflow += tx.paidAmount
                    }
                }

                "PURCHASE_RETURN" -> {
                    totalPurchases -= tx.finalTotalAmount
                    if (tx.paymentMethod == "CASH") {
                        cashInflow += tx.finalTotalAmount
                    }
                }
            }
        }

        var operatingExpenses = 0.0
        for (exp in periodExpenses) {
            operatingExpenses += exp.amount
            if (exp.paymentMethod == "CASH") {
                cashOutflow += exp.amount
            }
        }

        grossSales = roundMoney(grossSales)
        itemDiscounts = roundMoney(itemDiscounts)
        billDiscounts = roundMoney(billDiscounts)
        val totalDiscounts = roundMoney(itemDiscounts + billDiscounts)
        salesReturns = roundMoney(salesReturns)
        val netSales = roundMoney(maxOf(0.0, grossSales - totalDiscounts - salesReturns))
        gstCollected = roundMoney(gstCollected)
        cogs = roundMoney(maxOf(0.0, cogs))
        val grossProfit = roundMoney(netSales - cogs)
        val grossMarginPercent = if (netSales > 0) roundMoney((grossProfit / netSales) * 100.0) else 0.0

        operatingExpenses = roundMoney(operatingExpenses)
        val netProfit = roundMoney(grossProfit - operatingExpenses)
        val netMarginPercent = if (netSales > 0) roundMoney((netProfit / netSales) * 100.0) else 0.0

        totalPurchases = roundMoney(totalPurchases)
        gstPaidOnPurchases = roundMoney(gstPaidOnPurchases)
        cashInflow = roundMoney(cashInflow)
        cashOutflow = roundMoney(cashOutflow)
        val netCashFlow = roundMoney(cashInflow - cashOutflow)

        return ComprehensiveFinancialStatement(
            grossSales = grossSales,
            itemDiscounts = itemDiscounts,
            billDiscounts = billDiscounts,
            totalDiscounts = totalDiscounts,
            salesReturns = salesReturns,
            netSales = netSales,
            gstCollected = gstCollected,
            cogs = cogs,
            grossProfit = grossProfit,
            grossMarginPercent = grossMarginPercent,
            operatingExpenses = operatingExpenses,
            netProfit = netProfit,
            netMarginPercent = netMarginPercent,
            totalPurchases = totalPurchases,
            gstPaidOnPurchases = gstPaidOnPurchases,
            totalUnitsSold = totalUnitsSold,
            totalBillsCount = totalBillsCount,
            cashInflow = cashInflow,
            cashOutflow = cashOutflow,
            netCashFlow = netCashFlow
        )
    }
}
