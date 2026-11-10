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
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.example.data.model.SupplierEntity
import com.example.ui.MainViewModel
import com.example.ui.PurchaseCartItem
import com.example.ui.components.AddProductDialog
import com.example.ui.components.CameraBarcodeScannerDialog
import com.example.ui.components.ProductPickerDialog
import com.example.ui.theme.CrimsonDanger
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.EmeraldGreenDark
import com.example.ui.theme.EmeraldGreenLight
import com.example.ui.theme.NavyDark
import com.example.ui.theme.OceanBlue
import com.example.ui.theme.OceanBlueLight
import com.example.util.FinancialMath
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun PurchaseScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val products by viewModel.allProducts.collectAsStateWithLifecycle()
    val suppliers by viewModel.allSuppliers.collectAsStateWithLifecycle()
    val purchaseCartItems by viewModel.purchaseCartItems.collectAsStateWithLifecycle()
    val selectedSupplier by viewModel.selectedPurchaseSupplier.collectAsStateWithLifecycle()
    val purchasePaymentMethod by viewModel.purchasePaymentMethod.collectAsStateWithLifecycle()

    var showCameraScanner by remember { mutableStateOf(false) }
    var showProductPicker by remember { mutableStateOf(false) }
    var showSupplierPicker by remember { mutableStateOf(false) }
    var showAddSupplierDialog by remember { mutableStateOf(false) }

    var showAddNewProductDialog by remember { mutableStateOf(false) }
    var prefilledBarcodeForAdd by remember { mutableStateOf("") }
    var unknownBarcodePrompt by remember { mutableStateOf<String?>(null) }

    var editingCostPriceItem by remember { mutableStateOf<PurchaseCartItem?>(null) }
    var costPriceInput by remember { mutableStateOf("") }

    var editingBatchItem by remember { mutableStateOf<PurchaseCartItem?>(null) }
    var batchInput by remember { mutableStateOf("") }

    val cartList = remember(purchaseCartItems) { purchaseCartItems.values.toList() }
    val totalUnits = remember(cartList) { cartList.sumOf { it.quantity } }
    val subtotal = remember(cartList) { cartList.sumOf { it.quantity * it.unitCostPrice } }
    val totalDiscounts = remember(cartList) { cartList.sumOf { it.discount } }
    val totalTax = remember(cartList) {
        cartList.sumOf {
            val taxable = maxOf(0.0, (it.quantity * it.unitCostPrice) - it.discount)
            (taxable * it.taxPercent) / 100.0
        }
    }
    val finalTotal = remember(subtotal, totalDiscounts, totalTax) {
        FinancialMath.roundMoney(maxOf(0.0, (subtotal - totalDiscounts) + totalTax))
    }

    val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(14.dp)
            .testTag("purchase_screen")
    ) {
        // Top Header: Supplier Selector & Entry Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Supplier Selector Chip
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = if (selectedSupplier != null) OceanBlueLight else MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .clickable { showSupplierPicker = true }
                    .testTag("supplier_picker_button")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = null,
                        tint = if (selectedSupplier != null) OceanBlue else Color.Gray,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = selectedSupplier?.name ?: "Direct / Cash Vendor",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = if (selectedSupplier != null) OceanBlue else MaterialTheme.colorScheme.onSurface
                    )
                    if (selectedSupplier != null) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Clear",
                            modifier = Modifier
                                .size(14.dp)
                                .clickable { viewModel.setSelectedPurchaseSupplier(null) }
                        )
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                // Method A: Manual Product Entry
                OutlinedButton(
                    onClick = { showProductPicker = true },
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier.height(34.dp).testTag("purchase_search_product_button")
                ) {
                    Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Search", fontSize = 11.sp)
                }

                // Method B: Barcode Scanning
                Button(
                    onClick = { showCameraScanner = true },
                    colors = ButtonDefaults.buttonColors(containerColor = OceanBlue),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier.height(34.dp).testTag("purchase_scan_barcode_button")
                ) {
                    Icon(Icons.Default.QrCodeScanner, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Scan", fontSize = 11.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Procurement Cart Items List
        if (cartList.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.ShoppingCart, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(32.dp))
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text("Procurement Cart is Empty", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                    Text("Search products or scan barcodes to build purchase invoice.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(14.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedButton(onClick = { showProductPicker = true }) {
                            Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Manual Search")
                        }
                        Button(
                            onClick = { showCameraScanner = true },
                            colors = ButtonDefaults.buttonColors(containerColor = OceanBlue)
                        ) {
                            Icon(Icons.Default.QrCodeScanner, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Camera Scanner")
                        }
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 4.dp)
            ) {
                items(cartList, key = { it.product.id }) { item ->
                    Card(
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth().testTag("purchase_cart_item_${item.product.id}")
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Top
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.product.name,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                                    )
                                    Text(
                                        text = "${item.product.category} • SKU: ${item.product.barcode} • Stock: ${item.product.currentStock} ${item.product.unit}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Text(
                                    text = "₹" + String.format(Locale.US, "%.2f", item.totalAmount),
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = OceanBlue)
                                )
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            // Quantity Stepper + Editable Unit Cost Price
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    IconButton(
                                        onClick = { viewModel.updatePurchaseCartItemQuantity(item.product.id, item.quantity - 1) },
                                        modifier = Modifier.size(28.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surface)
                                    ) {
                                        Icon(Icons.Default.Remove, contentDescription = "Decrease", modifier = Modifier.size(14.dp))
                                    }

                                    Text(
                                        text = "${item.quantity} ${item.product.unit}",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                        modifier = Modifier.padding(horizontal = 6.dp)
                                    )

                                    IconButton(
                                        onClick = { viewModel.updatePurchaseCartItemQuantity(item.product.id, item.quantity + 1) },
                                        modifier = Modifier.size(28.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surface)
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = "Increase", modifier = Modifier.size(14.dp))
                                    }
                                }

                                // Editable Cost Rate Chip
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = MaterialTheme.colorScheme.surface,
                                    modifier = Modifier
                                        .clickable {
                                            editingCostPriceItem = item
                                            costPriceInput = String.format(Locale.US, "%.2f", item.unitCostPrice)
                                        }
                                        .padding(4.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "Cost: ₹${String.format(Locale.US, "%.2f", item.unitCostPrice)}",
                                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Icon(Icons.Default.Edit, contentDescription = "Edit cost", modifier = Modifier.size(12.dp), tint = Color.Gray)
                                    }
                                }

                                IconButton(
                                    onClick = { viewModel.updatePurchaseCartItemQuantity(item.product.id, 0) },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "Remove", tint = CrimsonDanger, modifier = Modifier.size(16.dp))
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            // Batch and Expiry row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Batch Number chip
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = MaterialTheme.colorScheme.surface,
                                    modifier = Modifier.clickable {
                                        editingBatchItem = item
                                        batchInput = item.batchNumber ?: ""
                                    }
                                ) {
                                    Text(
                                        text = if (item.batchNumber.isNullOrBlank()) "Add Batch" else "Batch: ${item.batchNumber}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (item.batchNumber.isNullOrBlank()) Color.Gray else MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }

                                // Expiry Date Picker button
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = MaterialTheme.colorScheme.surface,
                                    modifier = Modifier.clickable {
                                        val cal = Calendar.getInstance()
                                        if (item.expiryDate != null) cal.timeInMillis = item.expiryDate!!
                                        DatePickerDialog(
                                            context,
                                            { _, y, m, d ->
                                                val chosen = Calendar.getInstance().apply { set(y, m, d, 0, 0, 0) }
                                                viewModel.updatePurchaseCartItemExpiry(item.product.id, chosen.timeInMillis)
                                            },
                                            cal.get(Calendar.YEAR),
                                            cal.get(Calendar.MONTH),
                                            cal.get(Calendar.DAY_OF_MONTH)
                                        ).show()
                                    }
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Icon(Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(12.dp), tint = OceanBlue)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = if (item.expiryDate != null) "Exp: ${dateFormat.format(Date(item.expiryDate!!))}" else "Set Expiry",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (item.expiryDate != null) OceanBlue else Color.Gray
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Procurement Invoicing Summary Card
        if (cartList.isNotEmpty()) {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("Payment Method:", style = MaterialTheme.typography.labelSmall)
                    Spacer(modifier = Modifier.height(4.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("BANK_TRANSFER", "CASH", "UPI", "CREDIT").forEach { method ->
                            item {
                                FilterChip(
                                    selected = purchasePaymentMethod == method,
                                    onClick = { viewModel.setPurchasePaymentMethod(method) },
                                    label = { Text(if (method == "BANK_TRANSFER") "BANK" else method, fontSize = 10.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = OceanBlueLight
                                    )
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Subtotal ($totalUnits units):", style = MaterialTheme.typography.bodySmall)
                        Text(FinancialMath.formatInr(subtotal), style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold))
                    }

                    if (totalDiscounts > 0) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Procurement Discounts:", style = MaterialTheme.typography.bodySmall, color = CrimsonDanger)
                            Text("-${FinancialMath.formatInr(totalDiscounts)}", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold, color = CrimsonDanger))
                        }
                    }

                    if (totalTax > 0) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Applicable Tax / GST:", style = MaterialTheme.typography.bodySmall)
                            Text("+${FinancialMath.formatInr(totalTax)}", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold))
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Total Purchase Invoice", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                            if (purchasePaymentMethod == "CREDIT" && selectedSupplier != null) {
                                Text("Added to Supplier Payable", style = MaterialTheme.typography.labelSmall, color = CrimsonDanger)
                            }
                        }
                        Text(
                            text = FinancialMath.formatInr(finalTotal),
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, color = OceanBlue)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = { viewModel.clearPurchaseCart() },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Clear")
                        }

                        Button(
                            onClick = { viewModel.prepareMultiItemPurchaseConfirmation() },
                            colors = ButtonDefaults.buttonColors(containerColor = OceanBlue),
                            modifier = Modifier.weight(2f).testTag("generate_purchase_invoice_button")
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Save Invoice")
                        }
                    }
                }
            }
        }
    }

    // Camera Barcode Scanner Dialog (Method B)
    if (showCameraScanner) {
        CameraBarcodeScannerDialog(
            title = "Continuous Procurement Scanner",
            subtitle = "Scan products to add to purchase invoice",
            continuousMode = true,
            onBarcodeScanned = { scannedCode ->
                viewModel.handleScannedBarcodeForPurchase(
                    barcode = scannedCode,
                    onProductNotFound = { missingCode ->
                        unknownBarcodePrompt = missingCode
                    }
                )
            },
            onDismiss = { showCameraScanner = false }
        )
    }

    // Manual Product Picker Dialog (Method A)
    if (showProductPicker) {
        ProductPickerDialog(
            title = "Select Product to Procure",
            products = products,
            isPurchaseMode = true,
            onProductSelected = { prod ->
                viewModel.scanOrAddProductToPurchaseCart(prod)
                showProductPicker = false
            },
            onAddNewProductRequested = {
                prefilledBarcodeForAdd = ""
                showAddNewProductDialog = true
                showProductPicker = false
            },
            onDismiss = { showProductPicker = false }
        )
    }

    // Unknown Barcode Scanned Prompt Dialog
    if (unknownBarcodePrompt != null) {
        val missingCode = unknownBarcodePrompt!!
        AlertDialog(
            onDismissRequest = { unknownBarcodePrompt = null },
            title = { Text("Barcode Not Registered") },
            text = {
                Text("Barcode '$missingCode' is not registered. Register this product now to add it to this purchase invoice?")
            },
            confirmButton = {
                Button(onClick = {
                    prefilledBarcodeForAdd = missingCode
                    showAddNewProductDialog = true
                    unknownBarcodePrompt = null
                }) {
                    Text("Register Product")
                }
            },
            dismissButton = {
                TextButton(onClick = { unknownBarcodePrompt = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Register New Product Dialog
    if (showAddNewProductDialog) {
        val categories = remember(products) { products.map { it.category }.distinct().sorted() }
        AddProductDialog(
            existingCategories = categories,
            suppliers = suppliers,
            initialBarcode = prefilledBarcodeForAdd,
            onDismiss = { showAddNewProductDialog = false },
            onSave = { newProd ->
                viewModel.addNewProduct(newProd) { success ->
                    if (success) {
                        viewModel.scanOrAddProductToPurchaseCart(newProd)
                        showAddNewProductDialog = false
                    }
                }
            }
        )
    }

    // Edit Cost Price Dialog
    if (editingCostPriceItem != null) {
        val editItem = editingCostPriceItem!!
        AlertDialog(
            onDismissRequest = { editingCostPriceItem = null },
            title = { Text("Modify Purchase Price: ${editItem.product.name}") },
            text = {
                OutlinedTextField(
                    value = costPriceInput,
                    onValueChange = { costPriceInput = it },
                    label = { Text("Unit Cost Price (₹)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("edit_cost_price_input")
                )
            },
            confirmButton = {
                Button(onClick = {
                    val newPrice = costPriceInput.toDoubleOrNull() ?: editItem.unitCostPrice
                    viewModel.updatePurchaseCartItemCostPrice(editItem.product.id, newPrice)
                    editingCostPriceItem = null
                }) {
                    Text("Update")
                }
            },
            dismissButton = {
                TextButton(onClick = { editingCostPriceItem = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Edit Batch Number Dialog
    if (editingBatchItem != null) {
        val editItem = editingBatchItem!!
        AlertDialog(
            onDismissRequest = { editingBatchItem = null },
            title = { Text("Set Batch Number: ${editItem.product.name}") },
            text = {
                OutlinedTextField(
                    value = batchInput,
                    onValueChange = { batchInput = it },
                    label = { Text("Batch / Lot Number") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.updatePurchaseCartItemBatch(editItem.product.id, batchInput.trim().ifBlank { null })
                    editingBatchItem = null
                }) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { editingBatchItem = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Supplier Picker Dialog
    if (showSupplierPicker) {
        var supplierSearch by remember { mutableStateOf("") }
        val filteredSuppliers = remember(suppliers, supplierSearch) {
            if (supplierSearch.isBlank()) suppliers
            else suppliers.filter { it.name.contains(supplierSearch, ignoreCase = true) || it.contactNumber.contains(supplierSearch) }
        }

        AlertDialog(
            onDismissRequest = { showSupplierPicker = false },
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Select Supplier", fontWeight = FontWeight.Bold)
                    OutlinedButton(
                        onClick = {
                            showSupplierPicker = false
                            showAddSupplierDialog = true
                        },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier.height(30.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("New", fontSize = 11.sp)
                    }
                }
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = supplierSearch,
                        onValueChange = { supplierSearch = it },
                        label = { Text("Search supplier name or phone") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Button(
                        onClick = {
                            viewModel.setSelectedPurchaseSupplier(null)
                            showSupplierPicker = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Direct / Cash Vendor", color = MaterialTheme.colorScheme.onSurface)
                    }

                    LazyColumn(modifier = Modifier.height(200.dp)) {
                        items(filteredSuppliers, key = { it.id }) { sup ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        viewModel.setSelectedPurchaseSupplier(sup)
                                        showSupplierPicker = false
                                    }
                                    .padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(sup.name, fontWeight = FontWeight.Bold)
                                    Text(sup.contactNumber, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                }
                                if (sup.outstandingPayable > 0) {
                                    Text("Payable: ₹${sup.outstandingPayable}", color = CrimsonDanger, fontSize = 11.sp)
                                }
                            }
                            HorizontalDivider()
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { showSupplierPicker = false }) { Text("Close") } }
        )
    }

    // Add New Supplier Dialog
    if (showAddSupplierDialog) {
        var supName by remember { mutableStateOf("") }
        var supPhone by remember { mutableStateOf("") }
        var supAddress by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddSupplierDialog = false },
            title = { Text("Register New Supplier") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = supName,
                        onValueChange = { supName = it },
                        label = { Text("Supplier Name *") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = supPhone,
                        onValueChange = { supPhone = it },
                        label = { Text("Contact Number") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = supAddress,
                        onValueChange = { supAddress = it },
                        label = { Text("Address") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (supName.isNotBlank()) {
                            val newSup = SupplierEntity(
                                name = supName.trim(),
                                contactNumber = supPhone.trim(),
                                address = supAddress.trim()
                            )
                            viewModel.addNewSupplier(newSup) { success ->
                                if (success) {
                                    viewModel.setSelectedPurchaseSupplier(newSup)
                                    showAddSupplierDialog = false
                                }
                            }
                        }
                    },
                    enabled = supName.isNotBlank()
                ) {
                    Text("Save Supplier")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddSupplierDialog = false }) { Text("Cancel") }
            }
        )
    }
}
