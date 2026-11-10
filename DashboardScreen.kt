package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.TransactionEntity
import com.example.ui.DashboardPeriod
import com.example.ui.MainViewModel
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.AmberWarningLight
import com.example.ui.theme.CrimsonDanger
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.EmeraldGreenDark
import com.example.ui.theme.EmeraldGreenLight
import com.example.ui.theme.NavyDark
import com.example.ui.theme.OceanBlue
import com.example.ui.theme.OceanBlueLight
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Receipt

@Composable
fun DashboardScreen(
    viewModel: MainViewModel,
    onNavigateToSales: () -> Unit,
    onNavigateToPurchase: () -> Unit,
    onNavigateToInventory: () -> Unit,
    onNavigateToAi: () -> Unit,
    onNavigateToReports: () -> Unit,
    onNavigateToExpiry: () -> Unit,
    modifier: Modifier = Modifier
) {
    val metrics by viewModel.dashboardMetrics.collectAsStateWithLifecycle()
    val selectedPeriod by viewModel.selectedPeriod.collectAsStateWithLifecycle()
    val transactions by viewModel.allTransactions.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("dashboard_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // App Header & Store Identity
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "StockSaathi BI",
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                        color = NavyDark
                    )
                    Text(
                        text = "Senior AI Inventory & Business Intelligence",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = EmeraldGreenLight,
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(EmeraldGreen)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "SQLite ACID Verified",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = EmeraldGreenDark
                        )
                    }
                }
            }
        }

        // Period Filter Chips
        item {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(DashboardPeriod.values()) { period ->
                    FilterChip(
                        selected = period == selectedPeriod,
                        onClick = { viewModel.setPeriod(period) },
                        label = { Text(period.label) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = NavyDark,
                            selectedLabelColor = Color.White
                        ),
                        shape = RoundedCornerShape(16.dp)
                    )
                }
            }
        }

        // Low Stock Urgent Alert Banner
        if (metrics.lowStockCount > 0 || metrics.outOfStockCount > 0) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = AmberWarningLight),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigateToInventory() }
                        .testTag("low_stock_banner")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(AmberWarning.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = AmberWarning)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Inventory Health Alert",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = NavyDark
                            )
                            Text(
                                text = "${metrics.outOfStockCount} out-of-stock, ${metrics.lowStockCount} low threshold. Tap to review.",
                                style = MaterialTheme.typography.bodySmall,
                                color = NavyDark.copy(alpha = 0.8f)
                            )
                        }
                        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = AmberWarning)
                    }
                }
            }
        }

        // Primary Financial KPI Cards (Revenue vs Profit clearly distinguished!)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Revenue Card
                MetricCard(
                    title = "Verified Revenue",
                    subtitle = "Sales Volume",
                    value = "₹" + String.format(Locale.US, "%,.2f", metrics.totalRevenue),
                    secondaryInfo = "${metrics.totalSalesUnits} units sold",
                    icon = Icons.Default.TrendingUp,
                    accentColor = OceanBlue,
                    bgColor = OceanBlueLight,
                    modifier = Modifier.weight(1f)
                )

                // Net Profit Card (Distinguished from Revenue)
                MetricCard(
                    title = "Net Profit (मुनाफा)",
                    subtitle = "Margin: ${String.format(Locale.US, "%.1f", metrics.profitMarginPercent)}%",
                    value = "₹" + String.format(Locale.US, "%,.2f", metrics.netProfit),
                    secondaryInfo = "After COGS deduction",
                    icon = Icons.Default.ReceiptLong,
                    accentColor = EmeraldGreen,
                    bgColor = EmeraldGreenLight,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Total Purchases
                MetricCard(
                    title = "Total Purchases",
                    subtitle = "Stock Procurement",
                    value = "₹" + String.format(Locale.US, "%,.2f", metrics.totalPurchases),
                    secondaryInfo = "Vendor Outflow",
                    icon = Icons.Default.Inventory,
                    accentColor = NavyDark,
                    bgColor = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.weight(1f)
                )

                // Stock Valuation
                MetricCard(
                    title = "Stock Value (Cost)",
                    subtitle = "Locked Capital",
                    value = "₹" + String.format(Locale.US, "%,.2f", metrics.totalStockValuationCost),
                    secondaryInfo = "Retail: ₹${String.format(Locale.US, "%,.0f", metrics.totalStockValuationRetail)}",
                    icon = Icons.Default.Inventory,
                    accentColor = AmberWarning,
                    bgColor = AmberWarningLight,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Quick Actions Row
        item {
            Text(
                text = "Store Operations & Actions",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                QuickActionButton(
                    title = "New Sale",
                    subtitle = "POS Billing",
                    icon = Icons.Default.AddShoppingCart,
                    color = EmeraldGreen,
                    onClick = onNavigateToSales,
                    modifier = Modifier.weight(1f)
                )
                QuickActionButton(
                    title = "Stock In",
                    subtitle = "Purchase",
                    icon = Icons.Default.Inventory,
                    color = OceanBlue,
                    onClick = onNavigateToPurchase,
                    modifier = Modifier.weight(1f)
                )
                QuickActionButton(
                    title = "Reports",
                    subtitle = "Analysis & P&L",
                    icon = Icons.Default.Assessment,
                    color = NavyDark,
                    onClick = onNavigateToReports,
                    modifier = Modifier.weight(1f)
                )
                QuickActionButton(
                    title = "Expiry",
                    subtitle = "Shelf-Life",
                    icon = Icons.Default.Alarm,
                    color = AmberWarning,
                    onClick = onNavigateToExpiry,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Recent Transactions Section with Rollback & Slip Option
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Recent Transactions Audit",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Tap to view slip",
                    style = MaterialTheme.typography.labelSmall,
                    color = OceanBlue
                )
            }
        }

        val recentTx = transactions.take(6)
        if (recentTx.isEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(modifier = Modifier.padding(20.dp), contentAlignment = Alignment.Center) {
                        Text("No transactions recorded yet. Tap 'New Sale' or 'Stock In' to begin.")
                    }
                }
            }
        } else {
            items(recentTx, key = { it.id }) { tx ->
                TransactionCard(
                    transaction = tx,
                    onViewSlipClick = { viewModel.showSlip(tx) },
                    onCancelClick = { viewModel.prepareCancelConfirmation(tx) }
                )
            }
        }
    }
}

@Composable
private fun MetricCard(
    title: String,
    subtitle: String,
    value: String,
    secondaryInfo: String,
    icon: ImageVector,
    accentColor: Color,
    bgColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(bgColor),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(18.dp))
                }
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = secondaryInfo,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                color = accentColor
            )
        }
    }
}

@Composable
private fun QuickActionButton(
    title: String,
    subtitle: String,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.08f)),
        modifier = modifier.clickable { onClick() }
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun TransactionCard(
    transaction: TransactionEntity,
    onViewSlipClick: () -> Unit,
    onCancelClick: () -> Unit
) {
    val isSale = transaction.transactionType == "SALE"
    val isCancelled = transaction.status == "CANCELLED"
    val timeFormat = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())

    Card(
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onViewSlipClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(
                        if (isCancelled) MaterialTheme.colorScheme.surfaceVariant
                        else if (isSale) EmeraldGreenLight else OceanBlueLight
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (isSale) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                    contentDescription = null,
                    tint = if (isCancelled) Color.Gray else if (isSale) EmeraldGreen else OceanBlue,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = transaction.productName,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                    )
                    if (isCancelled) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = CrimsonDanger.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = "CANCELLED",
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                                color = CrimsonDanger
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${transaction.transactionType} • ${transaction.quantity} units • Stock: ${transaction.previousStock} ➔ ${transaction.resultingStock}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = timeFormat.format(Date(transaction.timestamp)),
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = (if (isSale) "+₹" else "-₹") + String.format(Locale.US, "%.2f", transaction.finalTotalAmount),
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = if (isCancelled) Color.Gray else if (isSale) EmeraldGreenDark else NavyDark
                    )
                )
                if (!isCancelled) {
                    IconButton(
                        onClick = onCancelClick,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            Icons.Default.Undo,
                            contentDescription = "Reverse Transaction",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}
