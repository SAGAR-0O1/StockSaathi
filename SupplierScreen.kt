package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import com.example.data.model.SupplierEntity
import com.example.ui.MainViewModel
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.CrimsonDanger
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.NavyDark
import com.example.ui.theme.OceanBlue
import com.example.ui.theme.OceanBlueLight
import com.example.util.FinancialMath

@Composable
fun SupplierScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val suppliers by viewModel.allSuppliers.collectAsStateWithLifecycle()
    var searchQuery by remember { mutableStateOf("") }
    var showAddDialog by remember { mutableStateOf(false) }
    var paymentSupplier by remember { mutableStateOf<SupplierEntity?>(null) }

    val filteredSuppliers = remember(suppliers, searchQuery) {
        if (searchQuery.isBlank()) suppliers
        else suppliers.filter {
            it.name.contains(searchQuery, ignoreCase = true) || it.contactNumber.contains(searchQuery)
        }
    }

    val totalPayable = remember(suppliers) {
        FinancialMath.roundMoney(suppliers.sumOf { it.outstandingPayable })
    }

    Scaffold(
        modifier = modifier.fillMaxSize().testTag("supplier_screen"),
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = NavyDark,
                contentColor = Color.White,
                modifier = Modifier.testTag("add_supplier_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Supplier")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            // Total Payables Summary Banner
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = if (totalPayable > 0) AmberWarning.copy(alpha = 0.1f) else OceanBlueLight),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Total Supplier Payables (Vendor Outstandings)",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = FinancialMath.formatInr(totalPayable),
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = if (totalPayable > 0) CrimsonDanger else NavyDark
                        )
                    }
                    Text(
                        text = "${suppliers.size} registered vendors",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Search supplier by name or phone") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("search_supplier_field")
            )

            Spacer(modifier = Modifier.height(12.dp))

            if (filteredSuppliers.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = if (searchQuery.isBlank()) "No suppliers added yet. Tap + to register vendor." else "No supplier matches '$searchQuery'",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(filteredSuppliers, key = { it.id }) { supplier ->
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background(NavyDark.copy(alpha = 0.1f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Business, contentDescription = null, tint = NavyDark)
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = supplier.name,
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                    )
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Call, contentDescription = null, modifier = Modifier.size(12.dp), tint = Color.Gray)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(text = supplier.contactNumber, style = MaterialTheme.typography.bodySmall)
                                    }
                                    if (supplier.address.isNotBlank()) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.LocationOn, contentDescription = null, modifier = Modifier.size(12.dp), tint = Color.Gray)
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(text = supplier.address, style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp))
                                        }
                                    }
                                    Text(
                                        text = "Terms: ${supplier.paymentTerms}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = OceanBlue
                                    )
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = FinancialMath.formatInr(supplier.outstandingPayable),
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = if (supplier.outstandingPayable > 0) CrimsonDanger else EmeraldGreen
                                        )
                                    )
                                    Text(
                                        text = if (supplier.outstandingPayable > 0) "Payable Due" else "Settled",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (supplier.outstandingPayable > 0) CrimsonDanger else EmeraldGreen
                                    )
                                    if (supplier.outstandingPayable > 0) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Button(
                                            onClick = { paymentSupplier = supplier },
                                            modifier = Modifier.height(28.dp),
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                        ) {
                                            Text("Pay Due", fontSize = 10.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddSupplierDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { name, contact, address, terms ->
                viewModel.addNewSupplier(
                    SupplierEntity(
                        name = name.trim(),
                        contactNumber = contact.trim(),
                        address = address.trim(),
                        paymentTerms = terms.trim()
                    )
                )
                showAddDialog = false
            }
        )
    }

    paymentSupplier?.let { sup ->
        PaySupplierDialog(
            supplier = sup,
            onDismiss = { paymentSupplier = null },
            onConfirm = { amount, method, notes ->
                viewModel.recordSupplierPayment(sup.id, amount, method, notes)
                paymentSupplier = null
            }
        )
    }
}

@Composable
private fun AddSupplierDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, contact: String, address: String, terms: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var contact by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var terms by remember { mutableStateOf("Net 30") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Register Supplier") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Supplier / Company Name *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = contact,
                    onValueChange = { contact = it },
                    label = { Text("Contact Phone *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("Warehouse / City Address") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = terms,
                    onValueChange = { terms = it },
                    label = { Text("Payment Terms (e.g. Net 30, Cash On Delivery)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) onConfirm(name, contact, address, terms)
                },
                enabled = name.isNotBlank()
            ) {
                Text("Save Supplier")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun PaySupplierDialog(
    supplier: SupplierEntity,
    onDismiss: () -> Unit,
    onConfirm: (amount: Double, method: String, notes: String?) -> Unit
) {
    var amountText by remember { mutableStateOf(supplier.outstandingPayable.toString()) }
    var selectedMethod by remember { mutableStateOf("BANK_TRANSFER") }
    var notes by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Pay Supplier: ${supplier.name}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Current Outstanding Payable: ${FinancialMath.formatInr(supplier.outstandingPayable)}")
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Amount Paid (₹) *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("BANK_TRANSFER", "UPI", "CASH").forEach { m ->
                        Button(
                            onClick = { selectedMethod = m },
                            colors = if (selectedMethod == m) androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = NavyDark)
                            else androidx.compose.material3.ButtonDefaults.outlinedButtonColors(),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(m.replace("_", " "), fontSize = 9.sp)
                        }
                    }
                }
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("UTR / Cheque / Ref Number") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amt = amountText.toDoubleOrNull() ?: 0.0
                    if (amt > 0.0) onConfirm(amt, selectedMethod, notes.ifBlank { null })
                },
                enabled = (amountText.toDoubleOrNull() ?: 0.0) > 0.0
            ) {
                Text("Record Payment")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
