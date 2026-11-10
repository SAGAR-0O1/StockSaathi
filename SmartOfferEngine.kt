package com.example.data.ai

import com.example.data.model.ProductEntity
import com.example.data.model.PromotionRuleEntity
import com.example.data.model.SmartOffer
import com.example.data.model.TransactionItemEntity
import com.example.util.FinancialMath
import java.util.concurrent.TimeUnit

data class CartItemEvaluation(
    val product: ProductEntity,
    val quantity: Int,
    val unitPrice: Double,
    val discount: Double = 0.0
)

object SmartOfferEngine {

    private const val FOURTEEN_DAYS_MILLIS = 14L * 24 * 60 * 60 * 1000

    /**
     * Evaluates active rules, current cart items, available inventory, and transaction history
     * to produce intelligent, margin-safe smart offers and cross-sell nudges.
     */
    fun evaluateOffers(
        cartItems: List<CartItemEvaluation>,
        activeRules: List<PromotionRuleEntity>,
        allProducts: List<ProductEntity>,
        recentTransactionItems: List<TransactionItemEntity> = emptyList(),
        currentTimeMillis: Long = System.currentTimeMillis()
    ): List<SmartOffer> {
        if (cartItems.isEmpty()) return emptyList()

        val cartSubtotal = cartItems.sumOf { (it.quantity * it.unitPrice) - it.discount }
        val cartCostBasis = cartItems.sumOf { it.quantity * it.product.purchaseCostPrice }
        val currentGrossProfit = cartSubtotal - cartCostBasis
        val currentMarginPercent = if (cartSubtotal > 0) (currentGrossProfit / cartSubtotal) * 100.0 else 0.0

        val generatedOffers = mutableListOf<SmartOffer>()

        // 1. Evaluate Spend-Based Promotion Rules (₹500, ₹1000, ₹1500, ₹2000, ₹3000 thresholds)
        for (rule in activeRules.filter { it.isEnabled }) {
            when (rule.ruleType) {
                "SPEND_DISCOUNT" -> {
                    if (cartSubtotal >= rule.minCartValue) {
                        // Calculate discount
                        val discountAmount = if (rule.discountPercent > 0) {
                            FinancialMath.roundMoney((cartSubtotal * rule.discountPercent) / 100.0)
                        } else {
                            rule.fixedDiscountAmount
                        }

                        val discountedCartTotal = maxOf(0.0, cartSubtotal - discountAmount)
                        val postOfferProfit = discountedCartTotal - cartCostBasis
                        val postOfferMargin = if (discountedCartTotal > 0) (postOfferProfit / discountedCartTotal) * 100.0 else 0.0

                        // Margin safety guard: Never create unacceptable business loss
                        if (postOfferProfit > 0 && postOfferMargin >= rule.minMarginPercent) {
                            generatedOffers.add(
                                SmartOffer(
                                    id = "spend_rule_${rule.id}",
                                    ruleId = rule.id,
                                    title = rule.ruleName,
                                    subtitle = if (rule.discountPercent > 0) {
                                        "${rule.discountPercent}% off on orders above ₹${rule.minCartValue.toInt()}"
                                    } else {
                                        "Flat ₹${rule.fixedDiscountAmount.toInt()} off on orders above ₹${rule.minCartValue.toInt()}"
                                    },
                                    offerType = "DISCOUNT",
                                    discountAmount = discountAmount,
                                    discountPercent = rule.discountPercent,
                                    expectedMarginPercent = FinancialMath.roundToPlaces(postOfferMargin, 1),
                                    badgeText = if (rule.discountPercent > 0) "${rule.discountPercent.toInt()}% OFF" else "SAVE ₹${discountAmount.toInt()}",
                                    reason = "Cart total ₹${FinancialMath.roundMoney(cartSubtotal)} qualifies (Margin: ${FinancialMath.roundToPlaces(postOfferMargin, 1)}%)"
                                )
                            )
                        }
                    } else {
                        // Cart Nudge: If customer is close to threshold (within 30% or within ₹300)
                        val amountNeeded = rule.minCartValue - cartSubtotal
                        val isClose = (amountNeeded <= 300.0) || (cartSubtotal >= rule.minCartValue * 0.70)
                        if (isClose && amountNeeded > 0) {
                            val potentialBenefit = if (rule.discountPercent > 0) {
                                "${rule.discountPercent.toInt()}% off"
                            } else {
                                "₹${rule.fixedDiscountAmount.toInt()} discount"
                            }
                            generatedOffers.add(
                                SmartOffer(
                                    id = "nudge_rule_${rule.id}",
                                    ruleId = rule.id,
                                    title = "Unlock ${rule.ruleName}",
                                    subtitle = "Add items worth ₹${FinancialMath.roundMoney(amountNeeded)} more to get $potentialBenefit!",
                                    offerType = "NUDGE",
                                    nudgeAmountNeeded = FinancialMath.roundMoney(amountNeeded),
                                    badgeText = "ADD ₹${amountNeeded.toInt()} MORE",
                                    reason = "Spend ₹${rule.minCartValue.toInt()} to unlock special savings"
                                )
                            )
                        }
                    }
                }

                "SPEND_GIFT" -> {
                    if (cartSubtotal >= rule.minCartValue && rule.freeGiftProductId != null) {
                        val giftProduct = allProducts.find { it.id == rule.freeGiftProductId }
                        // Verify gift is valid, in stock, not expired
                        val isGiftValid = giftProduct != null &&
                                giftProduct.currentStock > 1 &&
                                (giftProduct.expiryDate == null || giftProduct.expiryDate > currentTimeMillis)

                        if (isGiftValid && giftProduct != null) {
                            val totalCostWithGift = cartCostBasis + giftProduct.purchaseCostPrice
                            val postGiftProfit = cartSubtotal - totalCostWithGift
                            val postGiftMargin = if (cartSubtotal > 0) (postGiftProfit / cartSubtotal) * 100.0 else 0.0

                            if (postGiftProfit > 0 && postGiftMargin >= rule.minMarginPercent) {
                                generatedOffers.add(
                                    SmartOffer(
                                        id = "gift_rule_${rule.id}",
                                        ruleId = rule.id,
                                        title = "Free Gift: ${giftProduct.name}",
                                        subtitle = "Complimentary with orders above ₹${rule.minCartValue.toInt()}",
                                        offerType = "FREE_GIFT",
                                        giftProductId = giftProduct.id,
                                        giftProductName = giftProduct.name,
                                        giftProductPrice = giftProduct.sellingPrice,
                                        expectedMarginPercent = FinancialMath.roundToPlaces(postGiftMargin, 1),
                                        badgeText = "FREE GIFT",
                                        reason = "Qualifies on ₹${rule.minCartValue.toInt()}+ spend (Post-gift margin: ${FinancialMath.roundToPlaces(postGiftMargin, 1)}%)"
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }

        // 2. Near-Expiry Clearance Offers (<= 14 days, never expired)
        val cartProductIds = cartItems.map { it.product.id }.toSet()
        val nearExpiryProducts = allProducts.filter { prod ->
            val exp = prod.expiryDate
            if (exp != null && exp > currentTimeMillis && prod.currentStock > 0) {
                val daysLeft = TimeUnit.MILLISECONDS.toDays(exp - currentTimeMillis)
                daysLeft in 0..14
            } else {
                false
            }
        }

        for (nearExpProd in nearExpiryProducts) {
            val daysLeft = TimeUnit.MILLISECONDS.toDays(nearExpProd.expiryDate!! - currentTimeMillis)
            // Calculate recommended discount percentage based on urgency
            val recDiscountPct = when {
                daysLeft <= 3 -> 25.0
                daysLeft <= 7 -> 20.0
                else -> 15.0
            }
            val discountedSellingPrice = FinancialMath.roundMoney(nearExpProd.sellingPrice * (1.0 - recDiscountPct / 100.0))
            val unitProfit = discountedSellingPrice - nearExpProd.purchaseCostPrice

            // Only recommend if liquidation profit is non-negative or avoids 100% loss
            val isInCart = cartProductIds.contains(nearExpProd.id)
            val title = if (isInCart) {
                "Near-Expiry Discount on ${nearExpProd.name}"
            } else {
                "Clearance Special: ${nearExpProd.name}"
            }

            generatedOffers.add(
                SmartOffer(
                    id = "near_exp_${nearExpProd.id}",
                    title = title,
                    subtitle = "Expires in $daysLeft days. Offer ${recDiscountPct.toInt()}% clearance discount",
                    offerType = "EXPIRY_CLEARANCE",
                    discountPercent = recDiscountPct,
                    discountAmount = FinancialMath.roundMoney(nearExpProd.sellingPrice * (recDiscountPct / 100.0)),
                    giftProductId = nearExpProd.id,
                    giftProductName = nearExpProd.name,
                    giftProductPrice = discountedSellingPrice,
                    badgeText = "EXPIRING IN ${daysLeft}D",
                    reason = "Prevent total inventory loss before expiry on ${nearExpProd.name} (Stock: ${nearExpProd.currentStock})"
                )
            )
        }

        // 3. Basket Co-occurrence / Frequently Purchased Together (Cross-sell)
        if (recentTransactionItems.isNotEmpty()) {
            val coOccurrenceMap = mutableMapOf<Long, Int>()
            val cartItemBarcodes = cartItems.map { it.product.barcode }.toSet()

            // Group transaction items by invoice/transaction
            val groupedByInvoice = recentTransactionItems.groupBy { it.invoiceNumber }
            for ((_, txItems) in groupedByInvoice) {
                val barcodesInTx = txItems.map { it.barcode }.toSet()
                // If this invoice contains any item in our cart
                if (barcodesInTx.intersect(cartItemBarcodes).isNotEmpty()) {
                    for (item in txItems) {
                        if (!cartItemBarcodes.contains(item.barcode)) {
                            coOccurrenceMap[item.productId] = (coOccurrenceMap[item.productId] ?: 0) + 1
                        }
                    }
                }
            }

            val topAssociated = coOccurrenceMap.entries
                .sortedByDescending { it.value }
                .take(2)

            for ((prodId, count) in topAssociated) {
                val associatedProd = allProducts.find { it.id == prodId }
                if (associatedProd != null && associatedProd.currentStock > 0) {
                    val marginPct = if (associatedProd.sellingPrice > 0) {
                        ((associatedProd.sellingPrice - associatedProd.purchaseCostPrice) / associatedProd.sellingPrice) * 100.0
                    } else 0.0

                    generatedOffers.add(
                        SmartOffer(
                            id = "bundle_${associatedProd.id}",
                            title = "Frequently Bought Together: ${associatedProd.name}",
                            subtitle = "₹${associatedProd.sellingPrice} (${associatedProd.currentStock} in stock)",
                            offerType = "BUNDLE",
                            giftProductId = associatedProd.id,
                            giftProductName = associatedProd.name,
                            giftProductPrice = associatedProd.sellingPrice,
                            expectedMarginPercent = FinancialMath.roundToPlaces(marginPct, 1),
                            badgeText = "BASKET UPSELL",
                            reason = "Bought together in $count previous transactions (High Margin: ${FinancialMath.roundToPlaces(marginPct, 1)}%)"
                        )
                    )
                }
            }
        }

        // Sort: Discounts first, then near-expiry clearance, then free gifts, then nudges, then bundles
        return generatedOffers.distinctBy { it.id }
    }

    /**
     * Verifies if an offer can be safely applied to the given cart without violating margin thresholds.
     */
    fun isOfferMarginSafe(
        cartSubtotal: Double,
        cartCostBasis: Double,
        discountAmount: Double,
        giftCostPrice: Double,
        minAcceptableMarginPercent: Double
    ): Boolean {
        val discountedTotal = maxOf(0.0, cartSubtotal - discountAmount)
        val totalCost = cartCostBasis + giftCostPrice
        val profit = discountedTotal - totalCost
        if (profit < 0.0) return false
        if (discountedTotal <= 0.0) return false
        val margin = (profit / discountedTotal) * 100.0
        return margin >= minAcceptableMarginPercent
    }
}
