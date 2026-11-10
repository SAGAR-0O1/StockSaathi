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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import com.example.ui.MainViewModel
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.CrimsonDanger
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.NavyDark
import com.example.ui.theme.OceanBlue
import com.example.ui.theme.OceanBlueLight
import com.example.util.FinancialMath

@Composable
fun CustomerScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val customers by viewModel.allCustomers.collectAsStateWithLifecycle()
    var searchQuery by remember { mutableStateOf("") }
    var showAddDialog by remember { mutableStateOf(false) }
    var paymentCustomer by remember { mutableStateOf<CustomerEntity?>(null) }

    val filteredCustomers = remember(customers, searchQuery) {
        if (searchQuery.isBlank()) customers
        else customers.filter {
            it.name.contains(searchQuery, ignoreCase = true) || it.mobileNumber.contains(searchQuery)
        }
    }

    val totalUdhaar = remember(customers) {
        FinancialMath.roundMoney(customers.sumOf { it.outstandingBalance })
    }

    Scaffold(
        modifier = modifier.fillMaxSize().testTag("customer_screen"),
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = OceanBlue,
                contentColor = Color.White,
                modifier = Modifier.testTag("add_customer_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Customer")
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

            // Total Udhaar / Receivables Summary Banner
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = if (totalUdhaar > 0) CrimsonDanger.copy(alpha = 0.08f) else OceanBlueLight),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Total Customer Udhaar (Receivables)",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = FinancialMath.formatInr(totalUdhaar),
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = if (totalUdhaar > 0) CrimsonDanger else NavyDark
                        )
                    }
                    Text(
                        text = "${customers.count { it.outstandingBalance > 0 }} with pending balance",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Search customer by name or mobile") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("search_customer_field")
            )

            Spacer(modifier = Modifier.height(12.dp))

            if (filteredCustomers.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (searchQuery.isBlank()) "No customers registered yet. Tap + to add one." else "No customer matches '$searchQuery'",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(filteredCustomers, key = { it.id }) { customer ->
                        CustomerCard(
                            customer = customer,
                            onReceivePaymentClick = { paymentCustomer = customer }
                        )
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddCustomerDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { name, mobile, address, creditLimit ->
                viewModel.addNewCustomer(
                    CustomerEntity(
                        name = name.trim(),
                        mobileNumber = mobile.trim(),
                        address = address.trim().ifBlank { null },
                        creditLimit = creditLimit
                    )
                )
                showAddDialog = false
            }
        )
    }

    paymentCustomer?.let { cust ->
        ReceiveCustomerPaymentDialog(
            customer = cust,
            onDismiss = { paymentCustomer = null },
            onConfirm = { amount, method, notes ->
                viewModel.recordCustomerPayment(cust.id, amount, method, notes)
                paymentCustomer = null
            }
        )
    }
}

@Composable
private fun CustomerCard(
    customer: CustomerEntity,
    onReceivePaymentClick: () -> Unit
) {
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
                    .background(OceanBlueLight),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Person, contentDescription = null, tint = OceanBlue)
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = customer.name,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Call, contentDescription = null, modifier = Modifier.size(12.dp), tint = Color.Gray)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = customer.mobileNumber,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (customer.loyaltyPoints > 0) {
                        Spacer(modifier = Modifier.width(10.dp))
                        Icon(Icons.Default.Star, contentDescription = null, modifier = Modifier.size(12.dp), tint = AmberWarning)
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            text = "${customer.loyaltyPoints} pts",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                            color = AmberWarning
                        )
                    }
                }
                if (!customer.address.isNullOrBlank()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.LocationOn, contentDescription = null, modifier = Modifier.size(12.dp), tint = Color.Gray)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = customer.address,
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = FinancialMath.formatInr(customer.outstandingBalance),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = if (customer.outstandingBalance > 0) CrimsonDanger else EmeraldGreen
                    )
                )
                Text(
                    text = if (customer.outstandingBalance > 0) "Udhaar Due" else "No Due",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (customer.outstandingBalance > 0) CrimsonDanger else EmeraldGreen
                )
                if (customer.outstandingBalance > 0) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Button(
                        onClick = onReceivePaymentClick,
                        modifier = Modifier.height(28.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text("Pay Udhaar", fontSize = 10.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun AddCustomerDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, mobile: String, address: String, creditLimit: Double) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var mobile by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var creditLimit by remember { mutableStateOf("5000") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Register New Customer") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Customer Full Name *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = mobile,
                    onValueChange = { mobile = it },
                    label = { Text("Mobile Number (10 digits) *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("Address / Colony (Optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = creditLimit,
                    onValueChange = { creditLimit = it },
                    label = { Text("Credit / Udhaar Limit (₹)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank() && mobile.isNotBlank()) {
                        onConfirm(name, mobile, address, creditLimit.toDoubleOrNull() ?: 5000.0)
                    }
                },
                enabled = name.isNotBlank() && mobile.isNotBlank()
            ) {
                Text("Save Customer")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun ReceiveCustomerPaymentDialog(
    customer: CustomerEntity,
    onDismiss: () -> Unit,
    onConfirm: (amount: Double, method: String, notes: String?) -> Unit
) {
    var amountText by remember { mutableStateOf(customer.outstandingBalance.toString()) }
    var selectedMethod by remember { mutableStateOf("CASH") }
    var notes by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Receive Payment: ${customer.name}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Current Outstanding Due: ${FinancialMath.formatInr(customer.outstandingBalance)}")
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Amount Received (₹) *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("CASH", "UPI", "CARD").forEach { method ->
                        Button(
                            onClick = { selectedMethod = method },
                            colors = if (selectedMethod == method) androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = OceanBlue)
                            else androidx.compose.material3.ButtonDefaults.outlinedButtonColors(),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(method, fontSize = 11.sp)
                        }
                    }
                }
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Reference / Notes (Optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amt = amountText.toDoubleOrNull() ?: 0.0
                    if (amt > 0.0) {
                        onConfirm(amt, selectedMethod, notes.ifBlank { null })
                    }
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
