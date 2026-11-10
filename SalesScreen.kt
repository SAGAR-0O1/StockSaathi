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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Discount
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PauseCircle
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayCircle
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.CustomerEntity
import com.example.data.model.HeldBillEntity
import com.example.data.model.ProductEntity
import com.example.data.model.SmartOffer
import com.example.ui.CartItem
import com.example.ui.MainViewModel
import com.example.ui.components.AddProductDialog
import com.example.ui.components.CameraBarcodeScannerDialog
import com.example.ui.components.ProductPickerDialog
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.CrimsonDanger
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.EmeraldGreenDark
import com.example.ui.theme.EmeraldGreenLight
import com.example.ui.theme.OceanBlue
import com.example.ui.theme.OceanBlueLight
import com.example.util.FinancialMath
import java.util.Locale

@Composable
fun SalesScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val products by viewModel.allProducts.collectAsStateWithLifecycle()
    val customers by viewModel.allCustomers.collectAsStateWithLifecycle()
    val suppliers by viewModel.allSuppliers.collectAsStateWithLifecycle()
    val cartItems by viewModel.cartItems.collectAsStateWithLifecycle()
    val selectedCustomer by viewModel.selectedCustomer.collectAsStateWithLifecycle()
    val billDiscount by viewModel.billDiscount.collectAsStateWithLifecycle()
    val paymentMethod by viewModel.paymentMethod.collectAsStateWithLifecycle()
    val heldBills by viewModel.allHeldBills.collectAsStateWithLifecycle()
    val smartOffers by viewModel.activeSmartOffers.collectAsStateWithLifecycle()

    var showBarcodeScanner by remember { mutableStateOf(false) }
    var showProductPicker by remember { mutableStateOf(false) }
    var showCustomerPicker by remember { mutableStateOf(false) }
    var showHeldBillsDialog by remember { mutableStateOf(false) }
    var showHoldPrompt by remember { mutableStateOf(false) }
    var holdLabel by remember { mutableStateOf("") }

    var showAddNewProductDialog by remember { mutableStateOf(false) }
    var prefilledBarcodeForAdd by remember { mutableStateOf("") }
    var unknownBarcodePrompt by remember { mutableStateOf<String?>(null) }

    var editingQuantityProduct by remember { mutableStateOf<CartItem?>(null) }
    var manualQuantityInput by remember { mutableStateOf("") }

    val cartList = remember(cartItems) { cartItems.values.toList() }
    val totalUnits = remember(cartList) { cartList.sumOf { it.quantity } }
    val subtotal = remember(cartList) { cartList.sumOf { it.quantity * it.unitPrice } }
    val itemDiscounts = remember(cartList) { cartList.sumOf { it.discount } }
    val finalTotal = remember(subtotal, itemDiscounts, billDiscount) {
        FinancialMath.roundMoney(maxOf(0.0, (subtotal - (itemDiscounts + billDiscount))))
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(14.dp)
            .testTag("sales_screen")
    ) {
        // POS Header Row: Customer selector & Action Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Customer Selector Chip
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = if (selectedCustomer != null) OceanBlueLight else MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .clickable { showCustomerPicker = true }
                    .testTag("customer_picker_button")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = null,
                        tint = if (selectedCustomer != null) OceanBlue else Color.Gray,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = selectedCustomer?.name ?: "Walk-in Customer",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = if (selectedCustomer != null) OceanBlue else MaterialTheme.colorScheme.onSurface
                    )
                    if (selectedCustomer != null) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Clear",
                            modifier = Modifier
                                .size(14.dp)
                                .clickable { viewModel.setSelectedCustomer(null) }
                        )
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (heldBills.isNotEmpty()) {
                    OutlinedButton(
                        onClick = { showHeldBillsDialog = true },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Icon(Icons.Default.PlayCircle, contentDescription = null, modifier = Modifier.size(14.dp), tint = AmberWarning)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("(${heldBills.size})", fontSize = 11.sp)
                    }
                }

                if (cartList.isNotEmpty()) {
                    OutlinedButton(
                        onClick = { showHoldPrompt = true },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Icon(Icons.Default.PauseCircle, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Hold", fontSize = 11.sp)
                    }
                }

                // Method A: Manual Product Entry Button
                OutlinedButton(
                    onClick = { showProductPicker = true },
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier.height(34.dp).testTag("manual_product_search_button")
                ) {
                    Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Search", fontSize = 11.sp)
                }

                // Method B: Camera Barcode Scanner
                Button(
                    onClick = { showBarcodeScanner = true },
                    colors = ButtonDefaults.buttonColors(containerColor = OceanBlue),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier.height(34.dp).testTag("scan_sale_barcode_button")
                ) {
                    Icon(Icons.Default.QrCodeScanner, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Scan", fontSize = 11.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Cart Items List
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
                    Text("Sales Cart is Empty", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                    Text("Search products or scan barcodes to begin invoice.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(14.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedButton(onClick = { showProductPicker = true }) {
                            Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Manual Search")
                        }
                        Button(
                            onClick = { showBarcodeScanner = true },
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
                        modifier = Modifier.fillMaxWidth().testTag("cart_item_${item.product.id}")
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
                                        text = "₹${item.unitPrice} / ${item.product.unit} • Avail: ${item.product.currentStock} ${item.product.unit} • SKU: ${item.product.barcode}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Text(
                                    text = "₹" + String.format(Locale.US, "%.2f", item.totalAmount),
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = EmeraldGreenDark)
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Quantity Stepper & Tap to Edit
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    IconButton(
                                        onClick = { viewModel.updateCartItemQuantity(item.product.id, item.quantity - 1) },
                                        modifier = Modifier.size(28.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surface)
                                    ) {
                                        Icon(Icons.Default.Remove, contentDescription = "Decrease", modifier = Modifier.size(14.dp))
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = MaterialTheme.colorScheme.surface,
                                        modifier = Modifier
                                            .clickable {
                                                editingQuantityProduct = item
                                                manualQuantityInput = item.quantity.toString()
                                            }
                                            .padding(horizontal = 4.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = "${item.quantity}",
                                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                                            )
                                            Spacer(modifier = Modifier.width(3.dp))
                                            Icon(Icons.Default.Edit, contentDescription = "Edit quantity", modifier = Modifier.size(11.dp), tint = Color.Gray)
                                        }
                                    }

                                    IconButton(
                                        onClick = { viewModel.updateCartItemQuantity(item.product.id, item.quantity + 1) },
                                        enabled = item.quantity < item.product.currentStock,
                                        modifier = Modifier.size(28.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surface)
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = "Increase", modifier = Modifier.size(14.dp))
                                    }
                                }

                                // Line discount and remove
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    if (item.discount > 0) {
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = CrimsonDanger.copy(alpha = 0.12f)
                                        ) {
                                            Text(
                                                text = "-₹${item.discount.toInt()}",
                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                color = CrimsonDanger,
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                    IconButton(
                                        onClick = { viewModel.updateCartItemQuantity(item.product.id, 0) },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "Remove", tint = CrimsonDanger, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }
                }

                // AI-POWERED SMART OFFERS CARD
                if (smartOffers.isNotEmpty()) {
                    item {
                        Card(
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)),
                            modifier = Modifier.fillMaxWidth().testTag("smart_offers_section")
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = OceanBlue, modifier = Modifier.size(18.dp))
                                    Text(
                                        text = "AI Smart Offers & Benefits",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = OceanBlue)
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))

                                smartOffers.forEach { offer ->
                                    SmartOfferItemRow(
                                        offer = offer,
                                        onApply = { viewModel.applySmartOffer(offer) },
                                        onDismiss = { viewModel.dismissSmartOffer(offer) }
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Checkout & Billing Footer Card
        if (cartList.isNotEmpty()) {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    // Payment Method selector chips
                    Text("Payment Method:", style = MaterialTheme.typography.labelSmall)
                    Spacer(modifier = Modifier.height(4.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("CASH", "UPI", "CARD", "CREDIT", "SPLIT").forEach { method ->
                            item {
                                FilterChip(
                                    selected = paymentMethod == method,
                                    onClick = { viewModel.setPaymentMethod(method) },
                                    label = { Text(if (method == "CREDIT") "UDHAAR" else method, fontSize = 10.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = if (method == "CREDIT") CrimsonDanger.copy(alpha = 0.2f) else EmeraldGreenLight
                                    )
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Calculation breakdown
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Subtotal ($totalUnits units):", style = MaterialTheme.typography.bodySmall)
                        Text(FinancialMath.formatInr(subtotal), style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold))
                    }

                    if (itemDiscounts + billDiscount > 0) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Total Discount:", style = MaterialTheme.typography.bodySmall, color = CrimsonDanger)
                            Text("-${FinancialMath.formatInr(itemDiscounts + billDiscount)}", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold, color = CrimsonDanger))
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Net Bill Amount", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                            if (paymentMethod == "CREDIT") {
                                Text("Customer Udhaar Credit", style = MaterialTheme.typography.labelSmall, color = CrimsonDanger)
                            }
                        }
                        Text(
                            text = FinancialMath.formatInr(finalTotal),
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, color = EmeraldGreenDark)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = { viewModel.clearCart() },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Clear")
                        }

                        Button(
                            onClick = { viewModel.prepareMultiItemSaleConfirmation() },
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen),
                            modifier = Modifier.weight(2f).testTag("checkout_sale_button")
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Checkout Bill")
                        }
                    }
                }
            }
        }
    }

    // Method B: Camera Barcode Scanner Dialog
    if (showBarcodeScanner) {
        CameraBarcodeScannerDialog(
            title = "Continuous POS Scanner",
            subtitle = "Scan product barcode to add to bill",
            continuousMode = true,
            onBarcodeScanned = { scannedCode ->
                viewModel.handleScannedBarcodeForSales(
                    barcode = scannedCode,
                    onProductNotFound = { missingBarcode ->
                        unknownBarcodePrompt = missingBarcode
                    }
                )
            },
            onDismiss = { showBarcodeScanner = false }
        )
    }

    // Method A: Manual Product Entry Picker Dialog
    if (showProductPicker) {
        ProductPickerDialog(
            title = "Select Product to Sell",
            products = products,
            isPurchaseMode = false,
            onProductSelected = { prod ->
                viewModel.scanOrAddProductToCart(prod)
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
            title = { Text("Barcode Not Found") },
            text = {
                Text("Barcode '$missingCode' is not registered in the database. Would you like to register this product now?")
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

    // Add New Product Dialog
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
                        viewModel.scanOrAddProductToCart(newProd)
                        showAddNewProductDialog = false
                    }
                }
            }
        )
    }

    // Manual Quantity Editing Dialog
    if (editingQuantityProduct != null) {
        val editingItem = editingQuantityProduct!!
        AlertDialog(
            onDismissRequest = { editingQuantityProduct = null },
            title = { Text("Edit Quantity: ${editingItem.product.name}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Available stock: ${editingItem.product.currentStock} ${editingItem.product.unit}")
                    OutlinedTextField(
                        value = manualQuantityInput,
                        onValueChange = { manualQuantityInput = it },
                        label = { Text("Quantity") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("manual_quantity_field")
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    val newQty = manualQuantityInput.toIntOrNull() ?: editingItem.quantity
                    viewModel.updateCartItemQuantity(editingItem.product.id, newQty)
                    editingQuantityProduct = null
                }) {
                    Text("Update")
                }
            },
            dismissButton = {
                TextButton(onClick = { editingQuantityProduct = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Customer Selection Modal
    if (showCustomerPicker) {
        var customerSearch by remember { mutableStateOf("") }
        val filteredList = remember(customers, customerSearch) {
            if (customerSearch.isBlank()) customers
            else customers.filter { it.name.contains(customerSearch, ignoreCase = true) || it.mobileNumber.contains(customerSearch) }
        }

        AlertDialog(
            onDismissRequest = { showCustomerPicker = false },
            title = { Text("Select Customer for Billing") },
            text = {
                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = customerSearch,
                        onValueChange = { customerSearch = it },
                        label = { Text("Search customer name or mobile") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Button(
                        onClick = {
                            viewModel.setSelectedCustomer(null)
                            showCustomerPicker = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Walk-in Customer (General)", color = MaterialTheme.colorScheme.onSurface)
                    }

                    LazyColumn(modifier = Modifier.height(200.dp)) {
                        items(filteredList, key = { it.id }) { cust ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        viewModel.setSelectedCustomer(cust)
                                        showCustomerPicker = false
                                    }
                                    .padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(cust.name, fontWeight = FontWeight.Bold)
                                    Text(cust.mobileNumber, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                }
                                if (cust.outstandingBalance > 0) {
                                    Text("Due: ₹${cust.outstandingBalance}", color = CrimsonDanger, fontSize = 11.sp)
                                }
                            }
                            HorizontalDivider()
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { showCustomerPicker = false }) { Text("Close") } }
        )
    }

    // Hold Bill Prompt
    if (showHoldPrompt) {
        AlertDialog(
            onDismissRequest = { showHoldPrompt = false },
            title = { Text("Hold Current Bill") },
            text = {
                OutlinedTextField(
                    value = holdLabel,
                    onValueChange = { holdLabel = it },
                    label = { Text("Label / Customer Note (Optional)") },
                    placeholder = { Text("e.g. Table 2 / Ramesh Ji") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.holdCurrentBill(holdLabel)
                        holdLabel = ""
                        showHoldPrompt = false
                    }
                ) {
                    Text("Save & Hold")
                }
            },
            dismissButton = {
                TextButton(onClick = { showHoldPrompt = false }) { Text("Cancel") }
            }
        )
    }

    // Held Bills List
    if (showHeldBillsDialog) {
        AlertDialog(
            onDismissRequest = { showHeldBillsDialog = false },
            title = { Text("Resume Held Bill") },
            text = {
                LazyColumn(modifier = Modifier.fillMaxWidth()) {
                    items(heldBills, key = { it.id }) { bill ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.resumeBill(bill)
                                    showHeldBillsDialog = false
                                }
                                .padding(vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(bill.billLabel, fontWeight = FontWeight.Bold)
                                Text("${bill.itemsCount} items • ${FinancialMath.formatInr(bill.totalAmount)}", style = MaterialTheme.typography.bodySmall)
                            }
                            Button(onClick = {
                                viewModel.resumeBill(bill)
                                showHeldBillsDialog = false
                            }, modifier = Modifier.height(30.dp)) {
                                Text("Resume", fontSize = 11.sp)
                            }
                        }
                        HorizontalDivider()
                    }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { showHeldBillsDialog = false }) { Text("Close") } }
        )
    }
}

@Composable
fun SmartOfferItemRow(
    offer: SmartOffer,
    onApply: () -> Unit,
    onDismiss: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = when (offer.offerType) {
                            "DISCOUNT" -> EmeraldGreenLight
                            "FREE_GIFT" -> OceanBlueLight
                            "EXPIRY_CLEARANCE" -> AmberWarning.copy(alpha = 0.2f)
                            else -> MaterialTheme.colorScheme.surfaceVariant
                        }
                    ) {
                        Text(
                            text = offer.badgeText,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = when (offer.offerType) {
                                "DISCOUNT" -> EmeraldGreenDark
                                "FREE_GIFT" -> OceanBlue
                                "EXPIRY_CLEARANCE" -> AmberWarning
                                else -> MaterialTheme.colorScheme.onSurface
                            },
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Text(
                        text = offer.title,
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold)
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = offer.subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (offer.reason.isNotBlank()) {
                    Text(
                        text = "Why: ${offer.reason}",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = Color.Gray
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                if (offer.offerType != "NUDGE") {
                    Button(
                        onClick = onApply,
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier.height(28.dp)
                    ) {
                        Text("Apply", fontSize = 10.sp)
                    }
                }
                IconButton(onClick = onDismiss, modifier = Modifier.size(26.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Dismiss offer", modifier = Modifier.size(14.dp), tint = Color.Gray)
                }
            }
        }
    }
}
