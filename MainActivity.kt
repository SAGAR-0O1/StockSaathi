package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.MainViewModel
import com.example.ui.components.ActionConfirmationDialog
import com.example.ui.components.CashClosingDialog
import com.example.ui.components.TransactionSlipDialog
import com.example.ui.screens.AiAssistantScreen
import com.example.ui.screens.AuditLogsScreen
import com.example.ui.screens.CustomerScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.ExpenseScreen
import com.example.ui.screens.ExpiryScreen
import com.example.ui.screens.InventoryScreen
import com.example.ui.screens.PurchaseScreen
import com.example.ui.screens.ReportsScreen
import com.example.ui.screens.SalesScreen
import com.example.ui.screens.SupplierScreen
import com.example.ui.theme.CrimsonDanger
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.NavyDark
import com.example.ui.theme.OceanBlue
import com.example.util.UserRole
import com.example.util.UserSession
import kotlinx.coroutines.flow.collectLatest

enum class AppScreen(val title: String) {
    DASHBOARD("StockSaathi Insights"),
    INVENTORY("Inventory Management"),
    SALES("Sales POS"),
    PURCHASE("Stock Procurement"),
    CUSTOMERS("Customers & Udhaar"),
    SUPPLIERS("Suppliers & Payables"),
    EXPENSES("Expenses & Accounts"),
    REPORTS("Reports & Deep Analysis"),
    EXPIRY("Expiry & Shelf-Life"),
    AI_ASSISTANT("StockSaathi AI"),
    AUDIT_LOGS("Audit Ledger")
}

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                var currentScreen by remember { mutableStateOf(AppScreen.DASHBOARD) }
                val snackbarHostState = remember { SnackbarHostState() }
                val confirmationData by viewModel.pendingConfirmation.collectAsStateWithLifecycle()
                val activeSlip by viewModel.activeSlipTransaction.collectAsStateWithLifecycle()

                var showDeleteConfirmDialog by remember { mutableStateOf(false) }
                var deleteInputText by remember { mutableStateOf("") }
                var showOverflowMenu by remember { mutableStateOf(false) }
                var showCashClosingDialog by remember { mutableStateOf(false) }
                var currentUserRole by remember { mutableStateOf(UserSession.currentRole) }

                // System back button handling
                if (currentScreen != AppScreen.DASHBOARD) {
                    BackHandler {
                        currentScreen = AppScreen.DASHBOARD
                    }
                }

                LaunchedEffect(Unit) {
                    viewModel.uiEventMessage.collectLatest { msg ->
                        snackbarHostState.showSnackbar(msg)
                    }
                }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    topBar = {
                        TopAppBar(
                            title = {
                                Column {
                                    Text(
                                        text = currentScreen.title,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 17.sp,
                                        color = Color.White
                                    )
                                    Text(
                                        text = "${currentUserRole.displayName} • ${UserSession.currentStoreName}",
                                        fontSize = 10.sp,
                                        color = Color.White.copy(alpha = 0.7f)
                                    )
                                }
                            },
                            actions = {
                                IconButton(
                                    onClick = { currentScreen = if (currentScreen == AppScreen.CUSTOMERS) AppScreen.DASHBOARD else AppScreen.CUSTOMERS },
                                    modifier = Modifier.testTag("topbar_customers_button")
                                ) {
                                    Icon(
                                        Icons.Default.People,
                                        contentDescription = "Customers",
                                        tint = if (currentScreen == AppScreen.CUSTOMERS) OceanBlue else Color.White
                                    )
                                }

                                IconButton(
                                    onClick = { currentScreen = if (currentScreen == AppScreen.EXPENSES) AppScreen.DASHBOARD else AppScreen.EXPENSES },
                                    modifier = Modifier.testTag("topbar_expenses_button")
                                ) {
                                    Icon(
                                        Icons.Default.AccountBalanceWallet,
                                        contentDescription = "Expenses",
                                        tint = if (currentScreen == AppScreen.EXPENSES) OceanBlue else Color.White
                                    )
                                }

                                IconButton(
                                    onClick = { currentScreen = if (currentScreen == AppScreen.REPORTS) AppScreen.DASHBOARD else AppScreen.REPORTS },
                                    modifier = Modifier.testTag("topbar_reports_button")
                                ) {
                                    Icon(
                                        Icons.Default.Assessment,
                                        contentDescription = "Reports",
                                        tint = if (currentScreen == AppScreen.REPORTS) OceanBlue else Color.White
                                    )
                                }

                                Box {
                                    IconButton(onClick = { showOverflowMenu = true }) {
                                        Icon(Icons.Default.MoreVert, contentDescription = "Menu", tint = Color.White)
                                    }
                                    DropdownMenu(
                                        expanded = showOverflowMenu,
                                        onDismissRequest = { showOverflowMenu = false }
                                    ) {
                                        DropdownMenuItem(
                                            text = { Text("Suppliers & Payables") },
                                            leadingIcon = { Icon(Icons.Default.Business, contentDescription = null) },
                                            onClick = {
                                                showOverflowMenu = false
                                                currentScreen = AppScreen.SUPPLIERS
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Daily Cash Drawer Closing") },
                                            leadingIcon = { Icon(Icons.Default.PointOfSale, contentDescription = null) },
                                            onClick = {
                                                showOverflowMenu = false
                                                showCashClosingDialog = true
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Expiry Tracking") },
                                            leadingIcon = { Icon(Icons.Default.Alarm, contentDescription = null) },
                                            onClick = {
                                                showOverflowMenu = false
                                                currentScreen = AppScreen.EXPIRY
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Audit Transaction Ledger") },
                                            leadingIcon = { Icon(Icons.Default.History, contentDescription = null) },
                                            onClick = {
                                                showOverflowMenu = false
                                                currentScreen = AppScreen.AUDIT_LOGS
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Export JSON Backup") },
                                            leadingIcon = { Icon(Icons.Default.CloudDownload, contentDescription = null) },
                                            onClick = {
                                                showOverflowMenu = false
                                                viewModel.createBackup()
                                            }
                                        )
                                        HorizontalDivider()
                                        DropdownMenuItem(
                                            text = { Text("Delete All Store Data", color = CrimsonDanger) },
                                            leadingIcon = { Icon(Icons.Default.DeleteForever, contentDescription = null, tint = CrimsonDanger) },
                                            onClick = {
                                                showOverflowMenu = false
                                                deleteInputText = ""
                                                showDeleteConfirmDialog = true
                                            }
                                        )
                                    }
                                }
                            },
                            colors = TopAppBarDefaults.topAppBarColors(containerColor = NavyDark)
                        )
                    },
                    bottomBar = {
                        NavigationBar(
                            containerColor = MaterialTheme.colorScheme.surface,
                            contentColor = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.testTag("bottom_nav_bar")
                        ) {
                            NavigationBarItem(
                                selected = currentScreen == AppScreen.DASHBOARD,
                                onClick = { currentScreen = AppScreen.DASHBOARD },
                                icon = { Icon(Icons.Default.Dashboard, contentDescription = "Dashboard") },
                                label = { Text("Insights") },
                                colors = NavigationBarItemDefaults.colors(selectedIconColor = Color.White, indicatorColor = NavyDark),
                                modifier = Modifier.testTag("nav_tab_dashboard")
                            )

                            NavigationBarItem(
                                selected = currentScreen == AppScreen.INVENTORY,
                                onClick = { currentScreen = AppScreen.INVENTORY },
                                icon = { Icon(Icons.Default.Inventory, contentDescription = "Inventory") },
                                label = { Text("Stock") },
                                colors = NavigationBarItemDefaults.colors(selectedIconColor = Color.White, indicatorColor = NavyDark),
                                modifier = Modifier.testTag("nav_tab_inventory")
                            )

                            NavigationBarItem(
                                selected = currentScreen == AppScreen.SALES,
                                onClick = { currentScreen = AppScreen.SALES },
                                icon = { Icon(Icons.Default.AddShoppingCart, contentDescription = "Sales") },
                                label = { Text("POS Sale") },
                                colors = NavigationBarItemDefaults.colors(selectedIconColor = Color.White, indicatorColor = NavyDark),
                                modifier = Modifier.testTag("nav_tab_sales")
                            )

                            NavigationBarItem(
                                selected = currentScreen == AppScreen.PURCHASE,
                                onClick = { currentScreen = AppScreen.PURCHASE },
                                icon = { Icon(Icons.Default.ShoppingCart, contentDescription = "Purchase") },
                                label = { Text("Procure") },
                                colors = NavigationBarItemDefaults.colors(selectedIconColor = Color.White, indicatorColor = NavyDark),
                                modifier = Modifier.testTag("nav_tab_purchase")
                            )

                            NavigationBarItem(
                                selected = currentScreen == AppScreen.AI_ASSISTANT,
                                onClick = { currentScreen = AppScreen.AI_ASSISTANT },
                                icon = { Icon(Icons.Default.AutoAwesome, contentDescription = "AI Saathi") },
                                label = { Text("AI Saathi") },
                                colors = NavigationBarItemDefaults.colors(selectedIconColor = Color.White, indicatorColor = NavyDark),
                                modifier = Modifier.testTag("nav_tab_ai")
                            )
                        }
                    },
                    snackbarHost = { SnackbarHost(snackbarHostState) }
                ) { innerPadding ->
                    Box(modifier = Modifier.padding(innerPadding)) {
                        when (currentScreen) {
                            AppScreen.DASHBOARD -> DashboardScreen(
                                viewModel = viewModel,
                                onNavigateToSales = { currentScreen = AppScreen.SALES },
                                onNavigateToPurchase = { currentScreen = AppScreen.PURCHASE },
                                onNavigateToInventory = { currentScreen = AppScreen.INVENTORY },
                                onNavigateToAi = { currentScreen = AppScreen.AI_ASSISTANT },
                                onNavigateToReports = { currentScreen = AppScreen.REPORTS },
                                onNavigateToExpiry = { currentScreen = AppScreen.EXPIRY }
                            )

                            AppScreen.INVENTORY -> InventoryScreen(
                                viewModel = viewModel,
                                onNavigateToSales = { currentScreen = AppScreen.SALES },
                                onNavigateToPurchase = { currentScreen = AppScreen.PURCHASE }
                            )

                            AppScreen.SALES -> SalesScreen(
                                viewModel = viewModel
                            )

                            AppScreen.PURCHASE -> PurchaseScreen(
                                viewModel = viewModel
                            )

                            AppScreen.CUSTOMERS -> CustomerScreen(
                                viewModel = viewModel
                            )

                            AppScreen.SUPPLIERS -> SupplierScreen(
                                viewModel = viewModel
                            )

                            AppScreen.EXPENSES -> ExpenseScreen(
                                viewModel = viewModel
                            )

                            AppScreen.REPORTS -> ReportsScreen(
                                viewModel = viewModel
                            )

                            AppScreen.EXPIRY -> ExpiryScreen(
                                viewModel = viewModel
                            )

                            AppScreen.AI_ASSISTANT -> AiAssistantScreen(
                                viewModel = viewModel
                            )

                            AppScreen.AUDIT_LOGS -> AuditLogsScreen(
                                viewModel = viewModel
                            )
                        }
                    }

                    // Action Confirmation Modal
                    confirmationData?.let { data ->
                        ActionConfirmationDialog(
                            data = data,
                            onDismiss = { viewModel.dismissConfirmation() }
                        )
                    }

                    // Active Transaction Slip Dialog
                    activeSlip?.let { slip ->
                        TransactionSlipDialog(
                            transaction = slip,
                            onDismiss = { viewModel.dismissSlip() }
                        )
                    }

                    // Cash Closing Dialog
                    if (showCashClosingDialog) {
                        CashClosingDialog(
                            onDismiss = { showCashClosingDialog = false },
                            onConfirm = { opening, actual, notes ->
                                viewModel.recordDailyCashClosing(opening, actual, notes, currentUserRole.displayName)
                                showCashClosingDialog = false
                            }
                        )
                    }

                    // Delete All Store Data Confirmation Dialog
                    if (showDeleteConfirmDialog) {
                        AlertDialog(
                            onDismissRequest = { showDeleteConfirmDialog = false },
                            icon = { Icon(Icons.Default.DeleteForever, contentDescription = null, tint = CrimsonDanger) },
                            title = { Text("Delete All Store Data?") },
                            text = {
                                Column {
                                    Text(
                                        "This will permanently erase all inventory products, customers, suppliers, expenses, sales/purchase invoices, and audit ledger entries.\n\n" +
                                        "To confirm this destructive action, please type DELETE below:"
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    OutlinedTextField(
                                        value = deleteInputText,
                                        onValueChange = { deleteInputText = it },
                                        placeholder = { Text("Type DELETE here") },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth().testTag("delete_all_data_input")
                                    )
                                }
                            },
                            confirmButton = {
                                Button(
                                    onClick = {
                                        if (deleteInputText.trim() == "DELETE") {
                                            viewModel.clearAllStoreData()
                                            showDeleteConfirmDialog = false
                                            deleteInputText = ""
                                            currentScreen = AppScreen.DASHBOARD
                                        }
                                    },
                                    enabled = deleteInputText.trim() == "DELETE",
                                    colors = ButtonDefaults.buttonColors(containerColor = CrimsonDanger),
                                    modifier = Modifier.testTag("confirm_delete_all_button")
                                ) {
                                    Text("Erase Everything")
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                                    Text("Cancel")
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}
