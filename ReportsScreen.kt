package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.ProductEntity
import com.example.data.model.TransactionEntity
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
import com.example.util.ExportUtils
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

enum class PeriodBucket(val title: String) {
    DAILY("Day-wise (31d)"),
    WEEKLY("Week-wise (13w)"),
    MONTHLY("Month-wise (12m)"),
    YEARLY("Year-wise (5y)")
}

enum class ReportTimeFilter(val label: String, val days: Int?) {
    TODAY("Today", 1),
    DAYS_7("7 Days", 7),
    DAYS_30("30 Days", 30),
    YEAR_1("1 Year", 365),
    ALL("All Time", null)
}

data class PeriodicTrendRow(
    val bucketLabel: String,
    val startTime: Long,
    val endTime: Long,
    var salesRevenue: Double = 0.0,
    var costOfGoods: Double = 0.0,
    var netProfit: Double = 0.0,
    var marginPercent: Double = 0.0,
    var purchaseSpend: Double = 0.0,
    var unitsSold: Int = 0,
    var billCount: Int = 0
)

data class CategoryReportRow(
    val categoryName: String,
    var salesRevenue: Double = 0.0,
    var sharePercent: Double = 0.0,
    var netProfit: Double = 0.0,
    var unitsSold: Int = 0,
    var purchaseSpend: Double = 0.0,
    var currentStock: Int = 0,
    var stockValueAtCost: Double = 0.0
)

data class ProductReportRow(
    val product: ProductEntity,
    var salesRevenue: Double = 0.0,
    var netProfit: Double = 0.0,
    var unitsSold: Int = 0,
    var unitsPurchased: Int = 0,
    var purchaseSpend: Double = 0.0,
    val currentStock: Int = product.currentStock
)

@androidx.compose.material3.ExperimentalMaterial3Api
@Composable
fun ReportsScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val products by viewModel.allProducts.collectAsStateWithLifecycle()
    val transactions by viewModel.allTransactions.collectAsStateWithLifecycle()
    val transactionItems by viewModel.allTransactionItems.collectAsStateWithLifecycle()
    val expenses by viewModel.allExpenses.collectAsStateWithLifecycle()

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabTitles = listOf("Periodic Trends", "Category Analysis", "Product Velocity", "P&L & Cash Flow")

    // Filter out cancelled transactions for all calculations
    val committedTx = remember(transactions) {
        transactions.filter { it.status == "COMMITTED" }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("reports_screen")
    ) {
        // Tab Row
        PrimaryTabRow(
            selectedTabIndex = selectedTabIndex,
            containerColor = NavyDark,
            contentColor = Color.White
        ) {
            tabTitles.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTabIndex == index,
                    onClick = { selectedTabIndex = index },
                    text = {
                        Text(
                            text = title,
                            fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 11.sp
                        )
                    }
                )
            }
        }

        when (selectedTabIndex) {
            0 -> PeriodicTrendsTab(committedTx, products)
            1 -> CategoryAnalysisTab(committedTx, products)
            2 -> ProductPerformanceTab(committedTx, products)
            3 -> PandLTab(committedTx, transactionItems, expenses)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// TAB 1: PERIODIC TRENDS (Day / Week / Month / Year)
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun PeriodicTrendsTab(
    transactions: List<TransactionEntity>,
    products: List<ProductEntity>
) {
    val context = LocalContext.current
    var selectedBucket by remember { mutableStateOf(PeriodBucket.DAILY) }

    val productCostMap = remember(products) { products.associate { it.id to it.purchaseCostPrice } }

    val trendRows = remember(transactions, productCostMap, selectedBucket) {
        generatePeriodicBuckets(selectedBucket, transactions, productCostMap)
    }

    val totalSales = trendRows.sumOf { it.salesRevenue }
    val totalCogs = trendRows.sumOf { it.costOfGoods }
    val totalProfit = trendRows.sumOf { it.netProfit }
    val totalPurchases = trendRows.sumOf { it.purchaseSpend }
    val totalUnits = trendRows.sumOf { it.unitsSold }
    val totalBills = trendRows.sumOf { it.billCount }
    val overallMargin = if (totalSales > 0) (totalProfit / totalSales) * 100.0 else 0.0

    val maxSales = trendRows.maxOfOrNull { it.salesRevenue }?.takeIf { it > 0 } ?: 1.0

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Bucket Selection Chips & Export Buttons
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(PeriodBucket.values()) { bucket ->
                        FilterChip(
                            selected = selectedBucket == bucket,
                            onClick = { selectedBucket = bucket },
                            label = { Text(bucket.title, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = OceanBlue,
                                selectedLabelColor = Color.White
                            ),
                            shape = RoundedCornerShape(14.dp)
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedButton(
                        onClick = {
                            val csv = buildPeriodicCsv(selectedBucket.title, trendRows, totalSales, totalProfit, totalPurchases)
                            val fileName = "Periodic_${selectedBucket.name.lowercase()}_report.csv"
                            ExportUtils.saveCsvReport(context, fileName, csv)
                        },
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Excel", fontSize = 11.sp)
                    }

                    OutlinedButton(
                        onClick = {
                            val html = buildPeriodicHtml(selectedBucket.title, trendRows, totalSales, totalCogs, totalProfit, overallMargin, totalPurchases)
                            ExportUtils.printHtml(context, "Periodic_Report", html)
                        },
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Print", fontSize = 11.sp)
                    }
                }
            }
        }

        // Summary KPI Card
        item {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "Summary (${selectedBucket.title})",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = NavyDark
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Total Sales", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("₹" + String.format(Locale.US, "%,.2f", totalSales), style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = OceanBlue))
                        }
                        Column {
                            Text("Net Profit", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("₹" + String.format(Locale.US, "%,.2f", totalProfit), style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = EmeraldGreenDark))
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Margin %", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("${String.format(Locale.US, "%.1f", overallMargin)}%", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = NavyDark))
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("COGS: ₹${String.format(Locale.US, "%,.0f", totalCogs)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("Purchases: ₹${String.format(Locale.US, "%,.0f", totalPurchases)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("$totalUnits units ($totalBills bills)", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold))
                    }
                }
            }
        }

        // Table Header
        item {
            Text(
                text = "Detailed Periodic Breakdown",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
            )
        }

        // Items with Visual Bar
        items(trendRows) { row ->
            Card(
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = row.bucketLabel,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "Sales: ₹" + String.format(Locale.US, "%,.2f", row.salesRevenue),
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = OceanBlue)
                        )
                    }

                    // Visual Bar
                    Spacer(modifier = Modifier.height(6.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color.LightGray.copy(alpha = 0.3f))
                    ) {
                        val fraction = (row.salesRevenue / maxSales).toFloat().coerceIn(0f, 1f)
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(fraction)
                                .clip(RoundedCornerShape(4.dp))
                                .background(if (row.netProfit >= 0) EmeraldGreen else CrimsonDanger)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Cost: ₹${String.format(Locale.US, "%.0f", row.costOfGoods)}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            "Profit: ₹${String.format(Locale.US, "%.2f", row.netProfit)} (${String.format(Locale.US, "%.1f", row.marginPercent)}%)",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (row.netProfit >= 0) EmeraldGreenDark else CrimsonDanger
                            )
                        )
                        Text("Buy: ₹${String.format(Locale.US, "%.0f", row.purchaseSpend)}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("${row.unitsSold}u / ${row.billCount}b", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// TAB 2: CATEGORY-WISE ANALYSIS
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun CategoryAnalysisTab(
    transactions: List<TransactionEntity>,
    products: List<ProductEntity>
) {
    val context = LocalContext.current
    var selectedFilter by remember { mutableStateOf(ReportTimeFilter.DAYS_30) }

    val categoryRows = remember(transactions, products, selectedFilter) {
        val now = System.currentTimeMillis()
        val minTime = selectedFilter.days?.let { now - (it.toLong() * 86_400_000L) } ?: 0L

        val filteredTx = transactions.filter { it.timestamp >= minTime }
        val productMap = products.associateBy { it.id }
        val categoryMap = mutableMapOf<String, CategoryReportRow>()

        // Initialize with all product categories
        for (p in products) {
            val cat = p.category.ifBlank { "Uncategorized" }
            val existing = categoryMap.getOrPut(cat) { CategoryReportRow(categoryName = cat) }
            existing.currentStock += p.currentStock
            existing.stockValueAtCost += (p.currentStock * p.purchaseCostPrice)
        }

        var totalStoreSales = 0.0

        for (tx in filteredTx) {
            val prod = productMap[tx.productId]
            val cat = prod?.category?.ifBlank { "Uncategorized" } ?: "Uncategorized"
            val row = categoryMap.getOrPut(cat) { CategoryReportRow(categoryName = cat) }

            if (tx.transactionType == "SALE") {
                row.salesRevenue += tx.finalTotalAmount
                row.unitsSold += tx.quantity
                val cogs = (prod?.purchaseCostPrice ?: tx.unitPrice) * tx.quantity
                row.netProfit += (tx.finalTotalAmount - cogs)
                totalStoreSales += tx.finalTotalAmount
            } else if (tx.transactionType == "PURCHASE") {
                row.purchaseSpend += tx.finalTotalAmount
            }
        }

        // Calculate share %
        categoryMap.values.forEach { row ->
            row.sharePercent = if (totalStoreSales > 0) (row.salesRevenue / totalStoreSales) * 100.0 else 0.0
        }

        categoryMap.values.sortedByDescending { it.salesRevenue }
    }

    val totalCategorySales = categoryRows.sumOf { it.salesRevenue }
    val totalCategoryProfit = categoryRows.sumOf { it.netProfit }
    val totalStockVal = categoryRows.sumOf { it.stockValueAtCost }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Filter & Export Buttons
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(ReportTimeFilter.values()) { f ->
                        FilterChip(
                            selected = selectedFilter == f,
                            onClick = { selectedFilter = f },
                            label = { Text(f.label, fontSize = 11.sp) },
                            shape = RoundedCornerShape(14.dp)
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedButton(
                        onClick = {
                            val csv = buildCategoryCsv(selectedFilter.label, categoryRows)
                            ExportUtils.saveCsvReport(context, "Category_Report_${selectedFilter.name.lowercase()}.csv", csv)
                        },
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Excel", fontSize = 11.sp)
                    }

                    OutlinedButton(
                        onClick = {
                            val html = buildCategoryHtml(selectedFilter.label, categoryRows, totalCategorySales, totalCategoryProfit, totalStockVal)
                            ExportUtils.printHtml(context, "Category_Report", html)
                        },
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Print", fontSize = 11.sp)
                    }
                }
            }
        }

        // Summary Card
        item {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Categories", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("${categoryRows.size} active", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                    }
                    Column {
                        Text("Period Sales", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("₹${String.format(Locale.US, "%,.2f", totalCategorySales)}", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = OceanBlue))
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Total Stock Value", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("₹${String.format(Locale.US, "%,.2f", totalStockVal)}", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = AmberWarning))
                    }
                }
            }
        }

        items(categoryRows) { cat ->
            Card(
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Category, contentDescription = null, tint = OceanBlue, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(cat.categoryName, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                        }
                        Text(
                            text = "Share: ${String.format(Locale.US, "%.1f", cat.sharePercent)}%",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = OceanBlue)
                        )
                    }

                    // Visual Progress for Share %
                    Spacer(modifier = Modifier.height(6.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(Color.LightGray.copy(alpha = 0.3f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth((cat.sharePercent / 100.0).toFloat().coerceIn(0f, 1f))
                                .clip(RoundedCornerShape(3.dp))
                                .background(OceanBlue)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Sales: ₹${String.format(Locale.US, "%,.0f", cat.salesRevenue)}", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold))
                        Text(
                            "Profit: ₹${String.format(Locale.US, "%,.0f", cat.netProfit)}",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, color = EmeraldGreenDark)
                        )
                        Text("${cat.unitsSold} units sold", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Purchases: ₹${String.format(Locale.US, "%,.0f", cat.purchaseSpend)}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("On-Hand: ${cat.currentStock} units (₹${String.format(Locale.US, "%,.0f", cat.stockValueAtCost)})", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// TAB 3: PRODUCT-WISE PERFORMANCE
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun ProductPerformanceTab(
    transactions: List<TransactionEntity>,
    products: List<ProductEntity>
) {
    val context = LocalContext.current
    var selectedFilter by remember { mutableStateOf(ReportTimeFilter.DAYS_30) }
    var searchQuery by remember { mutableStateOf("") }

    val productRows = remember(transactions, products, selectedFilter, searchQuery) {
        val now = System.currentTimeMillis()
        val minTime = selectedFilter.days?.let { now - (it.toLong() * 86_400_000L) } ?: 0L
        val filteredTx = transactions.filter { it.timestamp >= minTime }

        val txByProduct = filteredTx.groupBy { it.productId }

        products.map { p ->
            val pTx = txByProduct[p.id] ?: emptyList()
            val salesTx = pTx.filter { it.transactionType == "SALE" }
            val purTx = pTx.filter { it.transactionType == "PURCHASE" }

            val salesRev = salesTx.sumOf { it.finalTotalAmount }
            val unitsSold = salesTx.sumOf { it.quantity }
            val cogs = unitsSold * p.purchaseCostPrice
            val profit = salesRev - cogs

            val unitsPur = purTx.sumOf { it.quantity }
            val purSpend = purTx.sumOf { it.finalTotalAmount }

            ProductReportRow(
                product = p,
                salesRevenue = salesRev,
                netProfit = profit,
                unitsSold = unitsSold,
                unitsPurchased = unitsPur,
                purchaseSpend = purSpend,
                currentStock = p.currentStock
            )
        }.filter {
            searchQuery.isBlank() ||
                    it.product.name.contains(searchQuery, ignoreCase = true) ||
                    it.product.barcode.contains(searchQuery, ignoreCase = true)
        }.sortedByDescending { it.salesRevenue }
    }

    val zeroSalesCount = productRows.count { it.unitsSold == 0 }
    val totalRevenue = productRows.sumOf { it.salesRevenue }
    val totalProfit = productRows.sumOf { it.netProfit }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Period Filter & Export Buttons
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(ReportTimeFilter.values()) { f ->
                        FilterChip(
                            selected = selectedFilter == f,
                            onClick = { selectedFilter = f },
                            label = { Text(f.label, fontSize = 11.sp) },
                            shape = RoundedCornerShape(14.dp)
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedButton(
                        onClick = {
                            val csv = buildProductCsv(selectedFilter.label, productRows)
                            ExportUtils.saveCsvReport(context, "Product_Report_${selectedFilter.name.lowercase()}.csv", csv)
                        },
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Excel", fontSize = 11.sp)
                    }

                    OutlinedButton(
                        onClick = {
                            val html = buildProductHtml(selectedFilter.label, productRows, totalRevenue, totalProfit, zeroSalesCount)
                            ExportUtils.printHtml(context, "Product_Report", html)
                        },
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Print", fontSize = 11.sp)
                    }
                }
            }
        }

        // Search Field
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search product name or barcode...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Summary Card
        item {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Total SKUs", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("${productRows.size} products", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                    }
                    Column {
                        Text("Total Sales", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("₹${String.format(Locale.US, "%,.2f", totalRevenue)}", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = OceanBlue))
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Zero Sales SKUs", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            "$zeroSalesCount SKUs",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (zeroSalesCount > 0) CrimsonDanger else EmeraldGreenDark
                            )
                        )
                    }
                }
            }
        }

        items(productRows, key = { it.product.id }) { row ->
            val hasZeroSales = row.unitsSold == 0
            Card(
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (hasZeroSales) AmberWarningLight.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(row.product.name, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                            Text(
                                "SKU: ${row.product.barcode} • Cat: ${row.product.category}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        if (hasZeroSales) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = AmberWarning.copy(alpha = 0.2f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Warning, contentDescription = null, tint = AmberWarning, modifier = Modifier.size(12.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        "ZERO SALES",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp),
                                        color = AmberWarning
                                    )
                                }
                            }
                        } else {
                            Text(
                                "₹${String.format(Locale.US, "%,.2f", row.salesRevenue)}",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = OceanBlue)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Units Sold: ${row.unitsSold} ${row.product.unit}", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold))
                        Text(
                            "Net Profit: ₹${String.format(Locale.US, "%,.2f", row.netProfit)}",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (row.netProfit >= 0) EmeraldGreenDark else CrimsonDanger
                            )
                        )
                        Text("In-Stock: ${row.currentStock} ${row.product.unit}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Purchased: ${row.unitsPurchased} units (₹${String.format(Locale.US, "%,.0f", row.purchaseSpend)})", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("Cost: ₹${String.format(Locale.US, "%.2f", row.product.purchaseCostPrice)} • Sell: ₹${String.format(Locale.US, "%.2f", row.product.sellingPrice)}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// HELPER CALCULATIONS & EXPORT BUILDERS
// ─────────────────────────────────────────────────────────────────────────────
private fun generatePeriodicBuckets(
    bucket: PeriodBucket,
    transactions: List<TransactionEntity>,
    productCostMap: Map<Long, Double>
): List<PeriodicTrendRow> {
    val rows = mutableListOf<PeriodicTrendRow>()
    val cal = Calendar.getInstance()
    val now = System.currentTimeMillis()

    when (bucket) {
        PeriodBucket.DAILY -> {
            val dateFormat = SimpleDateFormat("dd MMM", Locale.getDefault())
            for (i in 30 downTo 0) {
                val c = Calendar.getInstance().apply {
                    add(Calendar.DAY_OF_YEAR, -i)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                val start = c.timeInMillis
                c.add(Calendar.DAY_OF_YEAR, 1)
                val end = c.timeInMillis - 1
                rows.add(PeriodicTrendRow(dateFormat.format(Date(start)), start, end))
            }
        }
        PeriodBucket.WEEKLY -> {
            val dateFormat = SimpleDateFormat("dd MMM", Locale.getDefault())
            for (i in 12 downTo 0) {
                val c = Calendar.getInstance().apply {
                    add(Calendar.WEEK_OF_YEAR, -i)
                    set(Calendar.DAY_OF_WEEK, firstDayOfWeek)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                val start = c.timeInMillis
                c.add(Calendar.DAY_OF_YEAR, 7)
                val end = c.timeInMillis - 1
                rows.add(PeriodicTrendRow("Week of " + dateFormat.format(Date(start)), start, end))
            }
        }
        PeriodBucket.MONTHLY -> {
            val dateFormat = SimpleDateFormat("MMM yyyy", Locale.getDefault())
            for (i in 11 downTo 0) {
                val c = Calendar.getInstance().apply {
                    add(Calendar.MONTH, -i)
                    set(Calendar.DAY_OF_MONTH, 1)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                val start = c.timeInMillis
                c.add(Calendar.MONTH, 1)
                val end = c.timeInMillis - 1
                rows.add(PeriodicTrendRow(dateFormat.format(Date(start)), start, end))
            }
        }
        PeriodBucket.YEARLY -> {
            val dateFormat = SimpleDateFormat("yyyy", Locale.getDefault())
            for (i in 4 downTo 0) {
                val c = Calendar.getInstance().apply {
                    add(Calendar.YEAR, -i)
                    set(Calendar.DAY_OF_YEAR, 1)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                val start = c.timeInMillis
                c.add(Calendar.YEAR, 1)
                val end = c.timeInMillis - 1
                rows.add(PeriodicTrendRow(dateFormat.format(Date(start)), start, end))
            }
        }
    }

    // Populate calculations for each row
    for (row in rows) {
        val txInBucket = transactions.filter { it.timestamp in row.startTime..row.endTime }
        var sales = 0.0
        var cogs = 0.0
        var purchases = 0.0
        var units = 0
        var bills = 0

        for (tx in txInBucket) {
            if (tx.transactionType == "SALE") {
                sales += tx.finalTotalAmount
                units += tx.quantity
                bills++
                val cost = productCostMap[tx.productId] ?: tx.unitPrice
                cogs += (cost * tx.quantity)
            } else if (tx.transactionType == "PURCHASE") {
                purchases += tx.finalTotalAmount
            }
        }

        row.salesRevenue = sales
        row.costOfGoods = cogs
        row.netProfit = sales - cogs
        row.marginPercent = if (sales > 0) (row.netProfit / sales) * 100.0 else 0.0
        row.purchaseSpend = purchases
        row.unitsSold = units
        row.billCount = bills
    }

    return rows
}

private fun buildPeriodicCsv(
    periodName: String,
    rows: List<PeriodicTrendRow>,
    totalSales: Double,
    totalProfit: Double,
    totalPurchases: Double
): String {
    val sb = StringBuilder()
    sb.appendLine("StockSaathi - Periodic Trends Report ($periodName)")
    sb.appendLine("Generated At: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())}")
    sb.appendLine("Period,Sales Revenue (INR),COGS (INR),Net Profit (INR),Margin %,Purchases (INR),Units Sold,Number of Bills")
    for (r in rows) {
        sb.appendLine("\"${r.bucketLabel}\",${r.salesRevenue},${r.costOfGoods},${r.netProfit},${String.format(Locale.US, "%.1f", r.marginPercent)},${r.purchaseSpend},${r.unitsSold},${r.billCount}")
    }
    sb.appendLine("\"TOTAL\",$totalSales,,$totalProfit,,$totalPurchases,")
    return sb.toString()
}

private fun buildPeriodicHtml(
    periodName: String,
    rows: List<PeriodicTrendRow>,
    totalSales: Double,
    totalCogs: Double,
    totalProfit: Double,
    marginPct: Double,
    totalPurchases: Double
): String {
    val tableRows = rows.joinToString("\n") { r ->
        """<tr>
            <td><strong>${r.bucketLabel}</strong></td>
            <td style="text-align:right;">₹${String.format(Locale.US, "%,.2f", r.salesRevenue)}</td>
            <td style="text-align:right;">₹${String.format(Locale.US, "%,.2f", r.costOfGoods)}</td>
            <td style="text-align:right;color:${if (r.netProfit >= 0) "#15803d" else "#dc2626"};font-weight:bold;">₹${String.format(Locale.US, "%,.2f", r.netProfit)}</td>
            <td style="text-align:center;">${String.format(Locale.US, "%.1f", r.marginPercent)}%</td>
            <td style="text-align:right;">₹${String.format(Locale.US, "%,.2f", r.purchaseSpend)}</td>
            <td style="text-align:center;">${r.unitsSold}</td>
            <td style="text-align:center;">${r.billCount}</td>
        </tr>"""
    }

    return """<!DOCTYPE html><html><head><meta charset="utf-8">
    <style>
        body { font-family: sans-serif; margin: 20px; color: #1e293b; }
        h2 { color: #0f1b2b; margin-bottom: 4px; }
        .summary { display: flex; gap: 20px; margin: 16px 0; padding: 12px; background: #f8fafc; border: 1px solid #e2e8f0; border-radius: 8px; }
        table { width: 100%; border-collapse: collapse; font-size: 13px; margin-top: 10px; }
        th { background: #0f1b2b; color: white; padding: 8px; text-align: left; }
        td { padding: 8px; border-bottom: 1px solid #e2e8f0; }
        tr:nth-child(even) { background-color: #f8fafc; }
    </style></head><body>
    <h2>StockSaathi - Periodic Performance Audit ($periodName)</h2>
    <p style="font-size:12px;color:#64748b;">Generated: ${SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date())}</p>
    <div class="summary">
        <div><strong>Total Sales:</strong> ₹${String.format(Locale.US, "%,.2f", totalSales)}</div>
        <div><strong>Total COGS:</strong> ₹${String.format(Locale.US, "%,.2f", totalCogs)}</div>
        <div><strong>Net Profit:</strong> ₹${String.format(Locale.US, "%,.2f", totalProfit)} ($marginPct%)</div>
        <div><strong>Total Purchases:</strong> ₹${String.format(Locale.US, "%,.2f", totalPurchases)}</div>
    </div>
    <table>
        <thead><tr><th>Time Bucket</th><th style="text-align:right;">Sales</th><th style="text-align:right;">COGS</th><th style="text-align:right;">Profit</th><th>Margin</th><th style="text-align:right;">Purchases</th><th>Units</th><th>Bills</th></tr></thead>
        <tbody>$tableRows</tbody>
    </table>
    </body></html>"""
}

private fun buildCategoryCsv(filter: String, rows: List<CategoryReportRow>): String {
    val sb = StringBuilder()
    sb.appendLine("StockSaathi - Category-Wise Performance Report ($filter)")
    sb.appendLine("Category,Sales (INR),Share %,Profit (INR),Units Sold,Purchase Spend (INR),Current Stock,Stock Value (INR)")
    for (r in rows) {
        sb.appendLine("\"${r.categoryName}\",${r.salesRevenue},${String.format(Locale.US, "%.1f", r.sharePercent)},${r.netProfit},${r.unitsSold},${r.purchaseSpend},${r.currentStock},${r.stockValueAtCost}")
    }
    return sb.toString()
}

private fun buildCategoryHtml(
    filter: String,
    rows: List<CategoryReportRow>,
    totalSales: Double,
    totalProfit: Double,
    totalStockVal: Double
): String {
    val tableRows = rows.joinToString("\n") { r ->
        """<tr>
            <td><strong>${r.categoryName}</strong></td>
            <td style="text-align:right;">₹${String.format(Locale.US, "%,.2f", r.salesRevenue)}</td>
            <td style="text-align:center;">${String.format(Locale.US, "%.1f", r.sharePercent)}%</td>
            <td style="text-align:right;color:#15803d;font-weight:bold;">₹${String.format(Locale.US, "%,.2f", r.netProfit)}</td>
            <td style="text-align:center;">${r.unitsSold}</td>
            <td style="text-align:right;">₹${String.format(Locale.US, "%,.2f", r.purchaseSpend)}</td>
            <td style="text-align:center;">${r.currentStock}</td>
            <td style="text-align:right;">₹${String.format(Locale.US, "%,.2f", r.stockValueAtCost)}</td>
        </tr>"""
    }

    return """<!DOCTYPE html><html><head><meta charset="utf-8">
    <style>
        body { font-family: sans-serif; margin: 20px; color: #1e293b; }
        h2 { color: #0f1b2b; }
        table { width: 100%; border-collapse: collapse; font-size: 13px; margin-top: 10px; }
        th { background: #0f1b2b; color: white; padding: 8px; text-align: left; }
        td { padding: 8px; border-bottom: 1px solid #e2e8f0; }
        tr:nth-child(even) { background-color: #f8fafc; }
    </style></head><body>
    <h2>StockSaathi - Category Performance Analysis ($filter)</h2>
    <p>Total Sales: <strong>₹${String.format(Locale.US, "%,.2f", totalSales)}</strong> | Total Profit: <strong>₹${String.format(Locale.US, "%,.2f", totalProfit)}</strong> | Stock Value: <strong>₹${String.format(Locale.US, "%,.2f", totalStockVal)}</strong></p>
    <table>
        <thead><tr><th>Category</th><th style="text-align:right;">Sales</th><th>Share</th><th style="text-align:right;">Profit</th><th>Units</th><th style="text-align:right;">Purchases</th><th>Stock</th><th style="text-align:right;">Stock Value</th></tr></thead>
        <tbody>$tableRows</tbody>
    </table>
    </body></html>"""
}

private fun buildProductCsv(filter: String, rows: List<ProductReportRow>): String {
    val sb = StringBuilder()
    sb.appendLine("StockSaathi - Product-Wise Performance Report ($filter)")
    sb.appendLine("Product Name,Barcode,Category,Sales (INR),Net Profit (INR),Units Sold,Units Purchased,Purchase Spend (INR),Current Stock")
    for (r in rows) {
        sb.appendLine("\"${r.product.name}\",\"${r.product.barcode}\",\"${r.product.category}\",${r.salesRevenue},${r.netProfit},${r.unitsSold},${r.unitsPurchased},${r.purchaseSpend},${r.currentStock}")
    }
    return sb.toString()
}

private fun buildProductHtml(
    filter: String,
    rows: List<ProductReportRow>,
    totalSales: Double,
    totalProfit: Double,
    zeroCount: Int
): String {
    val tableRows = rows.joinToString("\n") { r ->
        val zeroAlert = if (r.unitsSold == 0) """<span style="color:#d97706;font-weight:bold;">[ZERO SALES]</span>""" else ""
        """<tr>
            <td><strong>${r.product.name}</strong> $zeroAlert<br><span style="font-size:11px;color:#64748b;">${r.product.barcode} | ${r.product.category}</span></td>
            <td style="text-align:right;">₹${String.format(Locale.US, "%,.2f", r.salesRevenue)}</td>
            <td style="text-align:right;color:#15803d;font-weight:bold;">₹${String.format(Locale.US, "%,.2f", r.netProfit)}</td>
            <td style="text-align:center;">${r.unitsSold}</td>
            <td style="text-align:center;">${r.unitsPurchased}</td>
            <td style="text-align:right;">₹${String.format(Locale.US, "%,.2f", r.purchaseSpend)}</td>
            <td style="text-align:center;font-weight:bold;">${r.currentStock}</td>
        </tr>"""
    }

    return """<!DOCTYPE html><html><head><meta charset="utf-8">
    <style>
        body { font-family: sans-serif; margin: 20px; color: #1e293b; }
        h2 { color: #0f1b2b; }
        table { width: 100%; border-collapse: collapse; font-size: 13px; margin-top: 10px; }
        th { background: #0f1b2b; color: white; padding: 8px; text-align: left; }
        td { padding: 8px; border-bottom: 1px solid #e2e8f0; }
        tr:nth-child(even) { background-color: #f8fafc; }
    </style></head><body>
    <h2>StockSaathi - Product Velocity & Sales Performance ($filter)</h2>
    <p>Total Sales: <strong>₹${String.format(Locale.US, "%,.2f", totalSales)}</strong> | Net Profit: <strong>₹${String.format(Locale.US, "%,.2f", totalProfit)}</strong> | Zero Sales SKUs: <strong>$zeroCount</strong></p>
    <table>
        <thead><tr><th>Product Description</th><th style="text-align:right;">Sales</th><th style="text-align:right;">Profit</th><th>Sold</th><th>Bought</th><th style="text-align:right;">Buy Spend</th><th>Current Stock</th></tr></thead>
        <tbody>$tableRows</tbody>
    </table>
    </body></html>"""
}

// ─────────────────────────────────────────────────────────────────────────────
// TAB 4: PROFIT & LOSS AND CASH FLOW STATEMENT (Verified Calculations)
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun PandLTab(
    transactions: List<com.example.data.model.TransactionEntity>,
    transactionItems: List<com.example.data.model.TransactionItemEntity>,
    expenses: List<com.example.data.model.ExpenseEntity>
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val stmt = remember(transactions, transactionItems, expenses) {
        com.example.util.FinancialMath.calculateFinancials(transactions, transactionItems, expenses)
    }

    androidx.compose.foundation.lazy.LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Actions: Export & Print
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Accounting & Income Statement",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = NavyDark
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedButton(
                        onClick = {
                            val csv = """
"Metric","Amount (INR)"
"Gross Sales","${stmt.grossSales}"
"Item Discounts","${stmt.itemDiscounts}"
"Bill Discounts","${stmt.billDiscounts}"
"Sales Returns","${stmt.salesReturns}"
"Net Sales","${stmt.netSales}"
"Cost of Goods Sold (COGS)","${stmt.cogs}"
"Gross Realized Profit","${stmt.grossProfit}"
"Gross Margin %","${stmt.grossMarginPercent}%"
"Operating Expenses","${stmt.operatingExpenses}"
"Net Profit (मुनाफा)","${stmt.netProfit}"
"Net Margin %","${stmt.netMarginPercent}%"
"GST Collected (Tax Liability)","${stmt.gstCollected}"
"Cash Inflow","${stmt.cashInflow}"
"Cash Outflow","${stmt.cashOutflow}"
"Net Cash Flow","${stmt.netCashFlow}"
                            """.trimIndent()
                            com.example.util.ExportUtils.saveCsvReport(context, "StockSaathi_PnL_Statement.csv", csv)
                        },
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Excel", fontSize = 11.sp)
                    }

                    OutlinedButton(
                        onClick = {
                            val html = """<!DOCTYPE html><html><head><meta charset="utf-8">
                            <style>body{font-family:sans-serif;margin:20px;color:#1e293b;}h2{color:#0f1b2b;}table{width:100%;border-collapse:collapse;font-size:14px;margin-top:10px;}td,th{padding:10px;border-bottom:1px solid #e2e8f0;}</style>
                            </head><body>
                            <h2>StockSaathi - Comprehensive Profit & Loss Statement</h2>
                            <table>
                            <tr><td>Gross Sales</td><td style="text-align:right;">₹${stmt.grossSales}</td></tr>
                            <tr><td>Total Discounts</td><td style="text-align:right;color:#dc2626;">-₹${stmt.totalDiscounts}</td></tr>
                            <tr><td>Sales Returns</td><td style="text-align:right;color:#dc2626;">-₹${stmt.salesReturns}</td></tr>
                            <tr style="font-weight:bold;background:#f1f5f9;"><td>Net Sales (Turnover)</td><td style="text-align:right;">₹${stmt.netSales}</td></tr>
                            <tr><td>Cost of Goods Sold (COGS from historical basis)</td><td style="text-align:right;color:#dc2626;">-₹${stmt.cogs}</td></tr>
                            <tr style="font-weight:bold;background:#e0f2fe;"><td>Gross Profit (Margin: ${stmt.grossMarginPercent}%)</td><td style="text-align:right;">₹${stmt.grossProfit}</td></tr>
                            <tr><td>Operating Expenses (Rent, Salary, Bills)</td><td style="text-align:right;color:#dc2626;">-₹${stmt.operatingExpenses}</td></tr>
                            <tr style="font-weight:bold;background:#dcfce7;font-size:16px;"><td>Net Profit / शुद्ध मुनाफा (Margin: ${stmt.netMarginPercent}%)</td><td style="text-align:right;color:#15803d;">₹${stmt.netProfit}</td></tr>
                            <tr><td>GST Collected (Tax Liability, not business income)</td><td style="text-align:right;">₹${stmt.gstCollected}</td></tr>
                            <tr style="font-weight:bold;background:#f8fafc;"><td>Net Cash Flow</td><td style="text-align:right;">₹${stmt.netCashFlow}</td></tr>
                            </table></body></html>"""
                            com.example.util.ExportUtils.printHtml(context, "PnL_Statement", html)
                        },
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Print", fontSize = 11.sp)
                    }
                }
            }
        }

        // Net Profit Highlight Card
        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = EmeraldGreenLight),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Realized Net Profit (शुद्ध लाभ)", style = MaterialTheme.typography.labelMedium, color = EmeraldGreenDark)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = com.example.util.FinancialMath.formatInr(stmt.netProfit),
                            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                            color = EmeraldGreenDark
                        )
                        Text(
                            text = "Net Margin: ${stmt.netMarginPercent}%",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = EmeraldGreenDark
                        )
                    }
                    Text(
                        text = "Calculated after full deduction of COGS (₹${stmt.cogs}) and Shop Operating Expenses (₹${stmt.operatingExpenses})",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = NavyDark
                    )
                }
            }
        }

        // P&L Ledger Lines Card
        item {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    PnLLineRow("Gross Sales (MRP/Billing Total)", stmt.grossSales, isDeduction = false)
                    PnLLineRow("Sales Discounts Given", stmt.totalDiscounts, isDeduction = true)
                    PnLLineRow("Sales Returns / Refunds", stmt.salesReturns, isDeduction = true)
                    HorizontalDivider()
                    PnLLineRow("Net Revenue (Turnover)", stmt.netSales, isDeduction = false, isBold = true)
                    PnLLineRow("Cost of Goods Sold (Historical COGS)", stmt.cogs, isDeduction = true)
                    HorizontalDivider()
                    PnLLineRow("Gross Realized Profit", stmt.grossProfit, isDeduction = false, isBold = true, accentColor = OceanBlue)
                    PnLLineRow("Shop Operating Expenses", stmt.operatingExpenses, isDeduction = true)
                    HorizontalDivider()
                    PnLLineRow("Final Realized Net Profit", stmt.netProfit, isDeduction = false, isBold = true, accentColor = EmeraldGreenDark)
                }
            }
        }

        // Tax Liability & Cash Flow Card
        item {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("GST & Cash Flow Compliance (Strict Tax Isolation)", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                    Text("GST Collected: ${com.example.util.FinancialMath.formatInr(stmt.gstCollected)} (Recorded as Tax Liability, never income)", style = MaterialTheme.typography.bodySmall)
                    Text("Cash Inflow (Counter Collections): ${com.example.util.FinancialMath.formatInr(stmt.cashInflow)}", style = MaterialTheme.typography.bodySmall)
                    Text("Cash Outflow (Cash expenses/procurement): ${com.example.util.FinancialMath.formatInr(stmt.cashOutflow)}", style = MaterialTheme.typography.bodySmall)
                    Text("Net Cash Movement: ${com.example.util.FinancialMath.formatInr(stmt.netCashFlow)}", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
                }
            }
        }
    }
}

@Composable
private fun PnLLineRow(
    label: String,
    amount: Double,
    isDeduction: Boolean = false,
    isBold: Boolean = false,
    accentColor: Color? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = if (isBold) MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold) else MaterialTheme.typography.bodySmall,
            color = if (isDeduction) CrimsonDanger else MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = (if (isDeduction && amount > 0) "-₹" else "₹") + String.format(Locale.US, "%,.2f", amount),
            style = if (isBold) MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold) else MaterialTheme.typography.bodySmall,
            color = accentColor ?: if (isDeduction && amount > 0) CrimsonDanger else MaterialTheme.colorScheme.onSurface
        )
    }
}

