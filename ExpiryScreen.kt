package com.example.ui.screens

import android.app.DatePickerDialog
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
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.EditCalendar
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.example.ui.MainViewModel
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.AmberWarningLight
import com.example.ui.theme.CrimsonDanger
import com.example.ui.theme.CrimsonDangerLight
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.EmeraldGreenDark
import com.example.ui.theme.EmeraldGreenLight
import com.example.ui.theme.NavyDark
import com.example.ui.theme.OceanBlue
import com.example.util.ExportUtils
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

enum class ExpiryFilter(val label: String) {
    EXPIRED("Expired"),
    NEXT_4_DAYS("Next 4 Days"),
    NEXT_7_DAYS("Weekly (7 Days)"),
    NEXT_30_DAYS("Next 30 Days"),
    NOT_SET("Expiry Not Set"),
    ALL_IN_STOCK("All In-Stock")
}

@Composable
fun ExpiryScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val products by viewModel.allProducts.collectAsStateWithLifecycle()

    var selectedFilter by remember { mutableStateOf(ExpiryFilter.EXPIRED) }
    var editingProduct by remember { mutableStateOf<ProductEntity?>(null) }

    val now = System.currentTimeMillis()
    val cal = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    val todayStart = cal.timeInMillis
    val oneDayMs = 86_400_000L

    // Only include products that still have stock (currentStock > 0) per Requirement 5
    val inStockProducts = remember(products) {
        products.filter { it.currentStock > 0 }
    }

    val filteredProducts = remember(inStockProducts, selectedFilter, todayStart) {
        when (selectedFilter) {
            ExpiryFilter.EXPIRED -> inStockProducts.filter {
                it.expiryDate != null && it.expiryDate < todayStart
            }
            ExpiryFilter.NEXT_4_DAYS -> inStockProducts.filter {
                it.expiryDate != null && it.expiryDate in todayStart..(todayStart + (4 * oneDayMs))
            }
            ExpiryFilter.NEXT_7_DAYS -> inStockProducts.filter {
                it.expiryDate != null && it.expiryDate in todayStart..(todayStart + (7 * oneDayMs))
            }
            ExpiryFilter.NEXT_30_DAYS -> inStockProducts.filter {
                it.expiryDate != null && it.expiryDate in todayStart..(todayStart + (30 * oneDayMs))
            }
            ExpiryFilter.NOT_SET -> inStockProducts.filter {
                it.expiryDate == null
            }
            ExpiryFilter.ALL_IN_STOCK -> inStockProducts
        }.sortedBy { it.expiryDate ?: Long.MAX_VALUE }
    }

    val totalItems = filteredProducts.size
    val totalStockUnits = filteredProducts.sumOf { it.currentStock }
    val totalStockValueAtCost = filteredProducts.sumOf { it.currentStock * it.purchaseCostPrice }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(14.dp)
            .testTag("expiry_screen")
    ) {
        // Top Action Bar with Export Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Expiry Management",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = NavyDark
                )
                Text(
                    text = "Loss Prevention & Stock Shelf-Life Audit",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedButton(
                    onClick = {
                        val csv = buildExpiryCsv(selectedFilter.label, filteredProducts, todayStart, oneDayMs)
                        val fileName = "Expiry_Report_${selectedFilter.name.lowercase()}.csv"
                        ExportUtils.saveCsvReport(context, fileName, csv)
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
                        val html = buildExpiryHtml(selectedFilter.label, filteredProducts, totalStockUnits, totalStockValueAtCost, todayStart, oneDayMs)
                        ExportUtils.printHtml(context, "Expiry_Audit_Report", html)
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

        Spacer(modifier = Modifier.height(10.dp))

        // Expiry Filter Chips
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(ExpiryFilter.values()) { f ->
                val isSelected = selectedFilter == f
                val chipColor = when (f) {
                    ExpiryFilter.EXPIRED -> CrimsonDanger
                    ExpiryFilter.NEXT_4_DAYS, ExpiryFilter.NEXT_7_DAYS -> AmberWarning
                    else -> OceanBlue
                }

                FilterChip(
                    selected = isSelected,
                    onClick = { selectedFilter = f },
                    label = { Text(f.label, fontSize = 11.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = chipColor,
                        selectedLabelColor = Color.White
                    ),
                    shape = RoundedCornerShape(14.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Summary Card
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
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Products Listed", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("$totalItems SKUs", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                }
                Column {
                    Text("At-Risk Stock Qty", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("$totalStockUnits units", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = OceanBlue))
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Value at Cost", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        "₹${String.format(Locale.US, "%,.2f", totalStockValueAtCost)}",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = if (selectedFilter == ExpiryFilter.EXPIRED) CrimsonDanger else AmberWarning
                        )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Product Expiry List
        if (filteredProducts.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(EmeraldGreenLight),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Alarm, contentDescription = null, tint = EmeraldGreen, modifier = Modifier.size(28.dp))
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "No Items in this Expiry Category",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "Your on-hand inventory is safe for this filter.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 4.dp)
            ) {
                items(filteredProducts, key = { it.id }) { product ->
                    ExpiryItemCard(
                        product = product,
                        todayStart = todayStart,
                        oneDayMs = oneDayMs,
                        onClick = { editingProduct = product }
                    )
                }
            }
        }
    }

    // Edit Expiry Date Dialog
    editingProduct?.let { prod ->
        EditExpiryDialog(
            product = prod,
            onDismiss = { editingProduct = null },
            onUpdateExpiry = { newExpiry ->
                viewModel.updateProductExpiry(prod.id, newExpiry)
                editingProduct = null
            }
        )
    }
}

@Composable
private fun ExpiryItemCard(
    product: ProductEntity,
    todayStart: Long,
    oneDayMs: Long,
    onClick: () -> Unit
) {
    val expiry = product.expiryDate
    val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

    val (statusLabel, statusColor, statusBg) = when {
        expiry == null -> Triple("Expiry Not Set", Color.Gray, MaterialTheme.colorScheme.surfaceVariant)
        expiry < todayStart -> {
            val daysAgo = ((todayStart - expiry) / oneDayMs).coerceAtLeast(1)
            Triple("EXPIRED $daysAgo DAYS AGO", CrimsonDanger, CrimsonDangerLight)
        }
        else -> {
            val daysLeft = ((expiry - todayStart) / oneDayMs)
            when {
                daysLeft <= 4 -> Triple("Expires in $daysLeft days!", CrimsonDanger, CrimsonDangerLight)
                daysLeft <= 7 -> Triple("Expires in $daysLeft days", AmberWarning, AmberWarningLight)
                daysLeft <= 30 -> Triple("Expires in $daysLeft days", AmberWarning, AmberWarningLight)
                else -> Triple("$daysLeft days left", EmeraldGreen, EmeraldGreenLight)
            }
        }
    }

    val stockValue = product.currentStock * product.purchaseCostPrice

    Card(
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(1.5.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("expiry_product_card_${product.id}")
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(product.name, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                    Text(
                        "Barcode: ${product.barcode} • Cat: ${product.category}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = statusBg
                ) {
                    Text(
                        text = statusLabel,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = statusColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Expiry Date", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        if (expiry != null) dateFormat.format(Date(expiry)) else "Not Specified",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold)
                    )
                }

                Column {
                    Text("Current Stock", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("${product.currentStock} ${product.unit}", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold))
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text("Stock Value (Cost)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("₹${String.format(Locale.US, "%,.2f", stockValue)}", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, color = NavyDark))
                }
            }
        }
    }
}

@Composable
private fun EditExpiryDialog(
    product: ProductEntity,
    onDismiss: () -> Unit,
    onUpdateExpiry: (Long?) -> Unit
) {
    val context = LocalContext.current
    var selectedDateMillis by remember { mutableStateOf(product.expiryDate) }
    val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

    val cal = Calendar.getInstance()
    if (selectedDateMillis != null) {
        cal.timeInMillis = selectedDateMillis!!
    }

    val datePickerDialog = remember {
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val chosen = Calendar.getInstance().apply {
                    set(year, month, dayOfMonth, 0, 0, 0)
                }
                selectedDateMillis = chosen.timeInMillis
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Update Expiry Date", fontWeight = FontWeight.Bold)
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = product.name,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = "Stock: ${product.currentStock} ${product.unit} • SKU: ${product.barcode}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Date Picker trigger button
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { datePickerDialog.show() }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = OceanBlue)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (selectedDateMillis != null) dateFormat.format(Date(selectedDateMillis!!)) else "Tap to Pick Date",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                            )
                        }
                        Text("Change", style = MaterialTheme.typography.labelSmall, color = OceanBlue)
                    }
                }

                // Quick Shortcut Buttons (+7d / +30d / +90d / Remove)
                Text(
                    text = "Quick Expiry Shortcuts:",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            val c = Calendar.getInstance()
                            c.add(Calendar.DAY_OF_YEAR, 7)
                            selectedDateMillis = c.timeInMillis
                        },
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(2.dp)
                    ) {
                        Text("+7 Days", fontSize = 11.sp)
                    }

                    OutlinedButton(
                        onClick = {
                            val c = Calendar.getInstance()
                            c.add(Calendar.DAY_OF_YEAR, 30)
                            selectedDateMillis = c.timeInMillis
                        },
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(2.dp)
                    ) {
                        Text("+30 Days", fontSize = 11.sp)
                    }

                    OutlinedButton(
                        onClick = {
                            val c = Calendar.getInstance()
                            c.add(Calendar.DAY_OF_YEAR, 90)
                            selectedDateMillis = c.timeInMillis
                        },
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(2.dp)
                    ) {
                        Text("+90 Days", fontSize = 11.sp)
                    }
                }

                if (selectedDateMillis != null) {
                    OutlinedButton(
                        onClick = { selectedDateMillis = null },
                        colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(contentColor = CrimsonDanger),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Remove Expiry Date (Clear)", fontSize = 12.sp)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onUpdateExpiry(selectedDateMillis) },
                colors = ButtonDefaults.buttonColors(containerColor = NavyDark)
            ) {
                Text("Save Expiry Date")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

private fun buildExpiryCsv(
    filter: String,
    products: List<ProductEntity>,
    todayStart: Long,
    oneDayMs: Long
): String {
    val sb = StringBuilder()
    val df = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    sb.appendLine("StockSaathi - Expiry Audit Report ($filter)")
    sb.appendLine("Generated At: ${df.format(Date())}")
    sb.appendLine("Product Name,Barcode,Category,Current Stock,Unit Cost (INR),Stock Value (INR),Expiry Date,Days Left / Status")
    for (p in products) {
        val expDateStr = p.expiryDate?.let { df.format(Date(it)) } ?: "NOT SET"
        val daysStatus = when {
            p.expiryDate == null -> "NOT SET"
            p.expiryDate < todayStart -> "EXPIRED"
            else -> "${(p.expiryDate - todayStart) / oneDayMs} days left"
        }
        val stockVal = p.currentStock * p.purchaseCostPrice
        sb.appendLine("\"${p.name}\",\"${p.barcode}\",\"${p.category}\",${p.currentStock},${p.purchaseCostPrice},$stockVal,$expDateStr,\"$daysStatus\"")
    }
    return sb.toString()
}

private fun buildExpiryHtml(
    filter: String,
    products: List<ProductEntity>,
    totalUnits: Int,
    totalStockVal: Double,
    todayStart: Long,
    oneDayMs: Long
): String {
    val df = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    val tableRows = products.joinToString("\n") { p ->
        val expDateStr = p.expiryDate?.let { df.format(Date(it)) } ?: "NOT SET"
        val (daysStatus, color) = when {
            p.expiryDate == null -> Pair("NOT SET", "#64748b")
            p.expiryDate < todayStart -> Pair("EXPIRED", "#dc2626")
            else -> {
                val d = (p.expiryDate - todayStart) / oneDayMs
                Pair("$d days left", if (d <= 7) "#d97706" else "#15803d")
            }
        }
        val stockVal = p.currentStock * p.purchaseCostPrice
        """<tr>
            <td><strong>${p.name}</strong><br><span style="font-size:11px;color:#64748b;">${p.barcode} | ${p.category}</span></td>
            <td style="text-align:center;">${p.currentStock} ${p.unit}</td>
            <td style="text-align:right;">₹${String.format(Locale.US, "%.2f", p.purchaseCostPrice)}</td>
            <td style="text-align:right;">₹${String.format(Locale.US, "%,.2f", stockVal)}</td>
            <td style="text-align:center;">$expDateStr</td>
            <td style="text-align:center;color:$color;font-weight:bold;">$daysStatus</td>
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
    <h2>StockSaathi - Expiry Risk Audit Report ($filter)</h2>
    <p>Total Units: <strong>$totalUnits</strong> | Total Stock Value at Cost: <strong>₹${String.format(Locale.US, "%,.2f", totalStockVal)}</strong></p>
    <table>
        <thead><tr><th>Product Name & Barcode</th><th>Stock</th><th style="text-align:right;">Unit Cost</th><th style="text-align:right;">Stock Value</th><th>Expiry Date</th><th>Status</th></tr></thead>
        <tbody>$tableRows</tbody>
    </table>
    </body></html>"""
}
