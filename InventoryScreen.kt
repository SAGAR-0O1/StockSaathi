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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.ProductEntity
import com.example.ui.MainViewModel
import com.example.ui.components.AddProductDialog
import com.example.ui.components.CameraBarcodeScannerDialog
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.AmberWarningLight
import com.example.ui.theme.CrimsonDanger
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.EmeraldGreenLight
import com.example.ui.theme.NavyDark
import com.example.ui.theme.OceanBlue
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun InventoryScreen(
    viewModel: MainViewModel,
    onNavigateToSales: () -> Unit,
    onNavigateToPurchase: () -> Unit,
    initialFilterLowStock: Boolean = false,
    modifier: Modifier = Modifier
) {
    val products by viewModel.allProducts.collectAsStateWithLifecycle()
    val suppliers by viewModel.allSuppliers.collectAsStateWithLifecycle()

    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    var showOnlyLowStock by remember { mutableStateOf(initialFilterLowStock) }

    var showAddDialog by remember { mutableStateOf(false) }
    var prefilledBarcodeForAdd by remember { mutableStateOf("") }
    var showCameraScanner by remember { mutableStateOf(false) }

    val categories = remember(products) {
        products.map { it.category }.distinct().sorted()
    }

    val filteredProducts = remember(products, searchQuery, selectedCategory, showOnlyLowStock) {
        products.filter { p ->
            val matchesQuery = searchQuery.isBlank() ||
                    p.name.contains(searchQuery, ignoreCase = true) ||
                    p.barcode.contains(searchQuery, ignoreCase = true)

            val matchesCat = selectedCategory == null || p.category == selectedCategory
            val matchesLowStock = !showOnlyLowStock || p.currentStock <= p.minStockThreshold

            matchesQuery && matchesCat && matchesLowStock
        }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    prefilledBarcodeForAdd = ""
                    showAddDialog = true
                },
                containerColor = NavyDark,
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier.testTag("add_product_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add New Product")
            }
        },
        modifier = modifier.testTag("inventory_screen")
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // Search and Live Camera Barcode Scan row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search by name or barcode...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("inventory_search_field")
                )

                // Live Camera Barcode Scanner Button
                IconButton(
                    onClick = { showCameraScanner = true },
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(OceanBlue.copy(alpha = 0.12f))
                        .testTag("inventory_barcode_button")
                ) {
                    Icon(Icons.Default.QrCodeScanner, contentDescription = "Live Camera Scan", tint = OceanBlue)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Filters row
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                item {
                    FilterChip(
                        selected = showOnlyLowStock,
                        onClick = { showOnlyLowStock = !showOnlyLowStock },
                        label = { Text("⚠️ Low Stock Alert") },
                        colors = androidx.compose.material3.FilterChipDefaults.filterChipColors(
                            selectedContainerColor = AmberWarning,
                            selectedLabelColor = Color.White
                        ),
                        shape = RoundedCornerShape(16.dp)
                    )
                }

                item {
                    FilterChip(
                        selected = selectedCategory == null && !showOnlyLowStock,
                        onClick = {
                            selectedCategory = null
                            showOnlyLowStock = false
                        },
                        label = { Text("All Products (${products.size})") },
                        shape = RoundedCornerShape(16.dp)
                    )
                }

                items(categories) { cat ->
                    FilterChip(
                        selected = selectedCategory == cat && !showOnlyLowStock,
                        onClick = {
                            selectedCategory = if (selectedCategory == cat) null else cat
                            showOnlyLowStock = false
                        },
                        label = { Text(cat) },
                        shape = RoundedCornerShape(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Count summary header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Showing ${filteredProducts.size} SKUs",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                val totalInventoryUnits = filteredProducts.sumOf { it.currentStock }
                Text(
                    text = "Total: $totalInventoryUnits on-hand units",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Products List
            if (filteredProducts.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.Inventory,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = Color.Gray
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No products found",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "Scan a barcode or tap + to add your first product.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(filteredProducts, key = { it.id }) { prod ->
                        ProductItemCard(
                            product = prod,
                            onQuickSell = {
                                viewModel.scanOrAddProductToCart(prod)
                                onNavigateToSales()
                            },
                            onQuickRestock = {
                                onNavigateToPurchase()
                            }
                        )
                    }
                }
            }
        }
    }

    // Add Product Dialog with optional Expiry Date and Barcode Scanner
    if (showAddDialog) {
        AddProductDialog(
            existingCategories = categories,
            suppliers = suppliers,
            initialBarcode = prefilledBarcodeForAdd,
            onDismiss = { showAddDialog = false },
            onSave = { newProd ->
                viewModel.addNewProduct(newProd) { success ->
                    if (success) showAddDialog = false
                }
            }
        )
    }

    // Camera Live Barcode Scanner (Requirement 1: opens Add Product if not in DB)
    if (showCameraScanner) {
        CameraBarcodeScannerDialog(
            title = "Scan Product Barcode",
            subtitle = "Point camera at barcode to look up or add",
            continuousMode = false,
            onBarcodeScanned = { scannedCode ->
                val match = products.firstOrNull { it.barcode.equals(scannedCode.trim(), ignoreCase = true) }
                if (match != null) {
                    searchQuery = match.barcode
                } else {
                    // Not in database: open Add Product form with that barcode already filled!
                    prefilledBarcodeForAdd = scannedCode.trim()
                    showAddDialog = true
                }
                showCameraScanner = false
            },
            onDismiss = { showCameraScanner = false }
        )
    }
}

@Composable
private fun ProductItemCard(
    product: ProductEntity,
    onQuickSell: () -> Unit,
    onQuickRestock: () -> Unit
) {
    val isOutOfStock = product.currentStock <= 0
    val isLowStock = !isOutOfStock && product.currentStock <= product.minStockThreshold
    val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

    val stockBadgeBg = when {
        isOutOfStock -> CrimsonDanger.copy(alpha = 0.12f)
        isLowStock -> AmberWarningLight
        else -> EmeraldGreenLight
    }

    val stockBadgeColor = when {
        isOutOfStock -> CrimsonDanger
        isLowStock -> AmberWarning
        else -> EmeraldGreen
    }

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("product_card_${product.id}")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = product.name,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${product.category} • SKU: ${product.barcode}",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (product.expiryDate != null) {
                        Text(
                            text = "Exp: ${dateFormat.format(Date(product.expiryDate))}",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = OceanBlue
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = stockBadgeBg
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isLowStock || isOutOfStock) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = stockBadgeColor, modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                        }
                        Text(
                            text = when {
                                isOutOfStock -> "OUT OF STOCK (0)"
                                isLowStock -> "LOW: ${product.currentStock} ${product.unit}"
                                else -> "${product.currentStock} ${product.unit}"
                            },
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = stockBadgeColor
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
            Spacer(modifier = Modifier.height(10.dp))

            // Pricing & Margins
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Cost Rate", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("₹${String.format(Locale.US, "%.2f", product.purchaseCostPrice)}", style = MaterialTheme.typography.bodyMedium)
                }

                Column {
                    Text("Selling Rate", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        "₹${String.format(Locale.US, "%.2f", product.sellingPrice)}",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = EmeraldGreen)
                    )
                }

                Column {
                    val margin = product.sellingPrice - product.purchaseCostPrice
                    val marginPct = if (product.purchaseCostPrice > 0) (margin / product.purchaseCostPrice) * 100 else 0.0
                    Text("Margin", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("+₹${String.format(Locale.US, "%.2f", margin)} (${String.format(Locale.US, "%.0f", marginPct)}%)", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold))
                }

                // Action shortcuts
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedButton(
                        onClick = onQuickRestock,
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Text("+ Buy", fontSize = 12.sp)
                    }

                    Button(
                        onClick = onQuickSell,
                        enabled = !isOutOfStock,
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Icon(Icons.Default.AddShoppingCart, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Sell", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

