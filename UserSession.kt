package com.example.util

enum class UserRole(val displayName: String, val description: String) {
    OWNER("Store Owner", "Full application access and settings"),
    MANAGER("Store Manager", "Inventory, purchases, sales, and analytics"),
    CASHIER("Cashier / Billing", "Sales POS and customer billing only"),
    ACCOUNTANT("Accountant", "Expenses, payments, and financial reports")
}

object UserSession {
    var currentRole: UserRole = UserRole.OWNER
    var currentStoreName: String = "StockSaathi Main Store"
    var isPinLockEnabled: Boolean = false
    var currentPin: String = "1234"

    fun canAccessInventory(): Boolean {
        return currentRole in listOf(UserRole.OWNER, UserRole.MANAGER)
    }

    fun canAccessSales(): Boolean {
        return currentRole in listOf(UserRole.OWNER, UserRole.MANAGER, UserRole.CASHIER)
    }

    fun canAccessPurchases(): Boolean {
        return currentRole in listOf(UserRole.OWNER, UserRole.MANAGER)
    }

    fun canAccessReports(): Boolean {
        return currentRole in listOf(UserRole.OWNER, UserRole.MANAGER, UserRole.ACCOUNTANT)
    }

    fun canAccessExpenses(): Boolean {
        return currentRole in listOf(UserRole.OWNER, UserRole.ACCOUNTANT)
    }

    fun canDeleteStoreData(): Boolean {
        return currentRole == UserRole.OWNER
    }
}
