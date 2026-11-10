package com.example.ui.components

import android.app.DatePickerDialog
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ProductEntity
import com.example.data.model.SupplierEntity
import com.example.ui.theme.OceanBlue
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

val STANDARD_RETAIL_UNITS = listOf("Piece", "Packet", "Box", "Kg", "Gram", "Litre", "Ml", "Metre", "Dozen", "Custom")

@Composable
fun AddProductDialog(
    existingCategories: List<String> = emptyList(),
    suppliers: List<SupplierEntity> = emptyList(),
    initialBarcode: String = "",
    onDismiss: () -> Unit,
    onSave: (ProductEntity) -> Unit
) {
    val context = LocalContext.current
    var name by remember { mutableStateOf("") }
    var barcode by remember { mutableStateOf(initialBarcode) }
    var category by remember { mutableStateOf(existingCategories.firstOrNull() ?: "General") }
    var costPrice by remember { mutableStateOf("") }
    var sellingPrice by remember { mutableStateOf("") }
    var initialStock by remember { mutableStateOf("0") }
    var threshold by remember { mutableStateOf("10") }
    var selectedUnit by remember { mutableStateOf("Piece") }
    var customUnitText by remember { mutableStateOf("") }
    var batchNumber by remember { mutableStateOf("") }
    var imageUrl by remember { mutableStateOf("") }
    var selectedSupplierId by remember { mutableStateOf<Long?>(null) }
    var selectedSupplierName by remember { mutableStateOf<String?>(null) }
    var mfgDateMillis by remember { mutableStateOf<Long?>(null) }
    var expiryDateMillis by remember { mutableStateOf<Long?>(null) }

    var showCameraScannerForBarcode by remember { mutableStateOf(false) }

    val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

    val calMfg = Calendar.getInstance()
    if (mfgDateMillis != null) calMfg.timeInMillis = mfgDateMillis!!
    val mfgDatePicker = remember {
        DatePickerDialog(
            context,
            { _, year, month, day ->
                val chosen = Calendar.getInstance().apply { set(year, month, day, 0, 0, 0) }
                mfgDateMillis = chosen.timeInMillis
            },
            calMfg.get(Calendar.YEAR),
            calMfg.get(Calendar.MONTH),
            calMfg.get(Calendar.DAY_OF_MONTH)
        )
    }

    val calExp = Calendar.getInstance()
    if (expiryDateMillis != null) calExp.timeInMillis = expiryDateMillis!!
    val expDatePicker = remember {
        DatePickerDialog(
            context,
            { _, year, month, day ->
                val chosen = Calendar.getInstance().apply { set(year, month, day, 0, 0, 0) }
                expiryDateMillis = chosen.timeInMillis
            },
            calExp.get(Calendar.YEAR),
            calExp.get(Calendar.MONTH),
            calExp.get(Calendar.DAY_OF_MONTH)
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Register New Product", fontWeight = FontWeight.Bold)
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Product Name *") },
                    modifier = Modifier.fillMaxWidth().testTag("product_name_input"),
                    singleLine = true
                )

                OutlinedTextField(
                    value = barcode,
                    onValueChange = { barcode = it },
                    label = { Text("Unique Barcode / SKU *") },
                    trailingIcon = {
                        IconButton(
                            onClick = { showCameraScannerForBarcode = true },
                            modifier = Modifier.testTag("scan_field_icon_button")
                        ) {
                            Icon(Icons.Default.QrCodeScanner, contentDescription = "Scan Barcode", tint = OceanBlue)
                        }
                    },
                    modifier = Modifier.fillMaxWidth().testTag("barcode_input"),
                    singleLine = true
                )

                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text("Category") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // Retail Units Selection Chips
                Text("Unit of Measurement:", style = MaterialTheme.typography.labelSmall)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(STANDARD_RETAIL_UNITS) { u ->
                        FilterChip(
                            selected = selectedUnit == u,
                            onClick = { selectedUnit = u },
                            label = { Text(u, fontSize = 11.sp) }
                        )
                    }
                }
                if (selectedUnit == "Custom") {
                    OutlinedTextField(
                        value = customUnitText,
                        onValueChange = { customUnitText = it },
                        label = { Text("Enter Custom Unit (e.g. bundle, roll)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = costPrice,
                        onValueChange = { costPrice = it },
                        label = { Text("Purchase Price (₹) *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f).testTag("cost_price_input"),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = sellingPrice,
                        onValueChange = { sellingPrice = it },
                        label = { Text("Selling Price (₹) *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f).testTag("selling_price_input"),
                        singleLine = true
                    )
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = initialStock,
                        onValueChange = { initialStock = it },
                        label = { Text("Initial Stock") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = threshold,
                        onValueChange = { threshold = it },
                        label = { Text("Min Stock Alert") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }

                // Batch Number
                OutlinedTextField(
                    value = batchNumber,
                    onValueChange = { batchNumber = it },
                    label = { Text("Batch / Lot Number (Optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // Optional Supplier Selection
                if (suppliers.isNotEmpty()) {
                    Text("Associated Supplier (Optional):", style = MaterialTheme.typography.labelSmall)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(suppliers) { sup ->
                            FilterChip(
                                selected = selectedSupplierId == sup.id,
                                onClick = {
                                    if (selectedSupplierId == sup.id) {
                                        selectedSupplierId = null
                                        selectedSupplierName = null
                                    } else {
                                        selectedSupplierId = sup.id
                                        selectedSupplierName = sup.name
                                    }
                                },
                                label = { Text(sup.name, fontSize = 11.sp) }
                            )
                        }
                    }
                }

                // Manufacturing Date
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { mfgDatePicker.show() }
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = OceanBlue, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (mfgDateMillis != null) "Mfg: ${dateFormat.format(Date(mfgDateMillis!!))}" else "Mfg Date (Tap to set)",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        if (mfgDateMillis != null) {
                            IconButton(onClick = { mfgDateMillis = null }, modifier = Modifier.size(22.dp)) {
                                Icon(Icons.Default.Close, contentDescription = "Clear Mfg", modifier = Modifier.size(14.dp))
                            }
                        }
                    }
                }

                // Expiry Date
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { expDatePicker.show() }
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = OceanBlue, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (expiryDateMillis != null) "Expiry: ${dateFormat.format(Date(expiryDateMillis!!))}" else "Expiry Date (Tap to set)",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        if (expiryDateMillis != null) {
                            IconButton(onClick = { expiryDateMillis = null }, modifier = Modifier.size(22.dp)) {
                                Icon(Icons.Default.Close, contentDescription = "Clear Expiry", modifier = Modifier.size(14.dp))
                            }
                        }
                    }
                }

                // Optional Image URL
                OutlinedTextField(
                    value = imageUrl,
                    onValueChange = { imageUrl = it },
                    label = { Text("Product Image URL (Optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val cost = costPrice.toDoubleOrNull() ?: 0.0
                    val sell = sellingPrice.toDoubleOrNull() ?: 0.0
                    val stock = initialStock.toIntOrNull() ?: 0
                    val minAlert = threshold.toIntOrNull() ?: 10
                    val resolvedUnit = if (selectedUnit == "Custom") customUnitText.ifBlank { "pcs" } else selectedUnit

                    if (name.isNotBlank() && barcode.isNotBlank() && sell > 0) {
                        onSave(
                            ProductEntity(
                                name = name.trim(),
                                barcode = barcode.trim(),
                                category = category.trim(),
                                purchaseCostPrice = cost,
                                sellingPrice = sell,
                                currentStock = stock,
                                minStockThreshold = minAlert,
                                unit = resolvedUnit.trim(),
                                updatedAt = System.currentTimeMillis(),
                                expiryDate = expiryDateMillis,
                                supplierId = selectedSupplierId,
                                supplierName = selectedSupplierName,
                                mfgDate = mfgDateMillis,
                                batchNumber = batchNumber.trim().ifBlank { null },
                                imageUrl = imageUrl.trim().ifBlank { null }
                            )
                        )
                    }
                },
                enabled = name.isNotBlank() && barcode.isNotBlank() && (sellingPrice.toDoubleOrNull() ?: 0.0) > 0,
                modifier = Modifier.testTag("save_product_button")
            ) {
                Text("Save Product")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )

    if (showCameraScannerForBarcode) {
        CameraBarcodeScannerDialog(
            title = "Scan Product Barcode",
            subtitle = "Position camera over barcode",
            continuousMode = false,
            onBarcodeScanned = { code ->
                barcode = code.trim()
                showCameraScannerForBarcode = false
            },
            onDismiss = { showCameraScannerForBarcode = false }
        )
    }
}
