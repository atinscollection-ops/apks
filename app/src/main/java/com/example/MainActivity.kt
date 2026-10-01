package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.screens.*
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.WmsViewModel
import kotlinx.coroutines.flow.collectLatest

class MainActivity : ComponentActivity() {

    private val viewModel: WmsViewModel by viewModels()

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                val snackbarHostState = remember { SnackbarHostState() }

                // Current logged-in user
                val currentUser by viewModel.currentUser.collectAsState()
                var showLogoutDialog by remember { mutableStateOf(false) }

                // Navigation State
                var currentScreen by remember { mutableStateOf("dashboard") }
                val navigationBackStack = remember { mutableStateListOf<String>() }

                fun navigateTo(screen: String) {
                    if (currentScreen != screen) {
                        navigationBackStack.add(currentScreen)
                        currentScreen = screen
                    }
                }

                fun navigateBack() {
                    if (navigationBackStack.isNotEmpty()) {
                        currentScreen = navigationBackStack.removeLast()
                    } else if (currentScreen != "dashboard") {
                        currentScreen = "dashboard"
                    }
                }

                // If not logged in, enforce LoginScreen
                if (currentUser == null) {
                    LoginScreen(
                        viewModel = viewModel,
                        onLoginSuccess = {
                            currentScreen = "dashboard"
                        }
                    )
                    return@MyApplicationTheme
                }

                // Preset params for direct navigation
                var presetPONumber by remember { mutableStateOf<String?>(null) }
                var presetSONumber by remember { mutableStateOf<String?>(null) }

                // Listen to ViewModel messages & errors
                LaunchedEffect(Unit) {
                    viewModel.userMessage.collectLatest { msg ->
                        snackbarHostState.showSnackbar(
                            message = msg,
                            duration = SnackbarDuration.Short
                        )
                    }
                }

                LaunchedEffect(Unit) {
                    viewModel.errorMessage.collectLatest { err ->
                        snackbarHostState.showSnackbar(
                            message = "⚠️ $err",
                            duration = SnackbarDuration.Long
                        )
                    }
                }

                BackHandler(enabled = currentScreen != "dashboard") {
                    navigateBack()
                }

                val title = when (currentScreen) {
                    "dashboard" -> "WMS Atin's Coll"
                    "products" -> "Master Produk"
                    "sales_order" -> "Sales Order"
                    "delivery_order" -> "Delivery Order"
                    "purchase_order" -> "Purchase Order"
                    "receiving" -> "Receiving Barang"
                    "stock_adjustment" -> "Stock Adjustment"
                    "return" -> "Return Barang"
                    "breakage" -> "Barang Rusak (Breakage)"
                    "stock_movement" -> "Buku Besar Mutasi"
                    "reports" -> "Laporan & Export Excel"
                    "customers" -> "Database Customer"
                    "expeditions" -> "Jasa Pengiriman"
                    "settings" -> "Pengaturan & Printer"
                    "more" -> "Menu Lainnya"
                    "promo_price" -> "Setting Harga Promo"
                    "user_management" -> "Manajemen Pengguna"
                    else -> "WMS Atin's Coll"
                }

                val isRootTab = currentScreen in listOf("dashboard", "products", "sales_order", "purchase_order", "more")

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    topBar = {
                        TopAppBar(
                            title = {
                                Column {
                                    Text(
                                        text = title,
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                    currentUser?.let { user ->
                                        Text(
                                            text = "${user.fullName} (${user.role})",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            },
                            navigationIcon = {
                                if (!isRootTab) {
                                    IconButton(onClick = { navigateBack() }) {
                                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
                                    }
                                }
                            },
                            actions = {
                                if (currentScreen != "reports") {
                                    IconButton(onClick = { navigateTo("reports") }) {
                                        Icon(Icons.Default.Assessment, contentDescription = "Laporan")
                                    }
                                }
                                if (currentScreen != "settings") {
                                    IconButton(onClick = { navigateTo("settings") }) {
                                        Icon(Icons.Default.Settings, contentDescription = "Pengaturan")
                                    }
                                }
                                IconButton(
                                    onClick = { showLogoutDialog = true },
                                    modifier = Modifier.testTag("btn_topbar_logout")
                                ) {
                                    Icon(
                                        Icons.Default.Logout,
                                        contentDescription = "Keluar / Logout",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = MaterialTheme.colorScheme.surface,
                                titleContentColor = MaterialTheme.colorScheme.onSurface
                            )
                        )
                    },
                    bottomBar = {
                        NavigationBar(
                            containerColor = MaterialTheme.colorScheme.surface,
                            tonalElevation = 4.dp
                        ) {
                            NavigationBarItem(
                                selected = currentScreen == "dashboard",
                                onClick = {
                                    navigationBackStack.clear()
                                    currentScreen = "dashboard"
                                },
                                icon = { Icon(Icons.Default.Dashboard, contentDescription = "Dashboard") },
                                label = { Text("Dashboard") },
                                modifier = Modifier.testTag("nav_tab_dashboard")
                            )
                            NavigationBarItem(
                                selected = currentScreen == "products",
                                onClick = {
                                    navigationBackStack.clear()
                                    currentScreen = "products"
                                },
                                icon = { Icon(Icons.Default.Inventory2, contentDescription = "Produk") },
                                label = { Text("Produk") },
                                modifier = Modifier.testTag("nav_tab_products")
                            )
                            NavigationBarItem(
                                selected = currentScreen in listOf("sales_order", "delivery_order"),
                                onClick = {
                                    navigationBackStack.clear()
                                    currentScreen = "sales_order"
                                },
                                icon = { Icon(Icons.Default.ShoppingBag, contentDescription = "Penjualan") },
                                label = { Text("Penjualan") },
                                modifier = Modifier.testTag("nav_tab_sales")
                            )
                            NavigationBarItem(
                                selected = currentScreen in listOf("purchase_order", "receiving"),
                                onClick = {
                                    navigationBackStack.clear()
                                    currentScreen = "purchase_order"
                                },
                                icon = { Icon(Icons.Default.LocalShipping, contentDescription = "Pembelian") },
                                label = { Text("Pembelian") },
                                modifier = Modifier.testTag("nav_tab_purchasing")
                            )
                            NavigationBarItem(
                                selected = currentScreen in listOf("more", "reports", "settings", "customers", "expeditions", "stock_adjustment", "return", "breakage", "stock_movement"),
                                onClick = {
                                    navigationBackStack.clear()
                                    currentScreen = "more"
                                },
                                icon = { Icon(Icons.Default.Menu, contentDescription = "Menu") },
                                label = { Text("Lainnya") },
                                modifier = Modifier.testTag("nav_tab_more")
                            )
                        }
                    },
                    snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        when (currentScreen) {
                            "dashboard" -> DashboardScreen(
                                viewModel = viewModel,
                                onNavigateTo = { navigateTo(it) }
                            )

                            "products" -> ProductsScreen(viewModel = viewModel)

                            "sales_order" -> {
                                Column(modifier = Modifier.fillMaxSize()) {
                                    // Sub-navigation tab between SO and DO
                                    TabRow(selectedTabIndex = 0) {
                                        Tab(
                                            selected = true,
                                            onClick = {},
                                            text = { Text("Sales Order (Pesanan)") }
                                        )
                                        Tab(
                                            selected = false,
                                            onClick = { currentScreen = "delivery_order" },
                                            text = { Text("Delivery Order (Kirim)") }
                                        )
                                    }
                                    SalesOrderScreen(
                                        viewModel = viewModel,
                                        onNavigateToDelivery = { soNum ->
                                            presetSONumber = soNum
                                            currentScreen = "delivery_order"
                                        }
                                    )
                                }
                            }

                            "delivery_order" -> {
                                Column(modifier = Modifier.fillMaxSize()) {
                                    TabRow(selectedTabIndex = 1) {
                                        Tab(
                                            selected = false,
                                            onClick = { currentScreen = "sales_order" },
                                            text = { Text("Sales Order (Pesanan)") }
                                        )
                                        Tab(
                                            selected = true,
                                            onClick = {},
                                            text = { Text("Delivery Order (Kirim)") }
                                        )
                                    }
                                    DeliveryOrderScreen(
                                        viewModel = viewModel,
                                        presetSONumber = presetSONumber
                                    )
                                }
                            }

                            "purchase_order" -> {
                                Column(modifier = Modifier.fillMaxSize()) {
                                    // Sub-navigation tab between PO and Receiving
                                    TabRow(selectedTabIndex = 0) {
                                        Tab(
                                            selected = true,
                                            onClick = {},
                                            text = { Text("Purchase Order") }
                                        )
                                        Tab(
                                            selected = false,
                                            onClick = { currentScreen = "receiving" },
                                            text = { Text("Penerimaan (Receiving)") }
                                        )
                                    }
                                    PurchaseOrderScreen(
                                        viewModel = viewModel,
                                        onNavigateToReceiving = { poNum ->
                                            presetPONumber = poNum
                                            currentScreen = "receiving"
                                        }
                                    )
                                }
                            }

                            "receiving" -> {
                                Column(modifier = Modifier.fillMaxSize()) {
                                    TabRow(selectedTabIndex = 1) {
                                        Tab(
                                            selected = false,
                                            onClick = { currentScreen = "purchase_order" },
                                            text = { Text("Purchase Order") }
                                        )
                                        Tab(
                                            selected = true,
                                            onClick = {},
                                            text = { Text("Penerimaan (Receiving)") }
                                        )
                                    }
                                    ReceivingScreen(
                                        viewModel = viewModel,
                                        presetPONumber = presetPONumber
                                    )
                                }
                            }

                            "stock_adjustment" -> StockAdjustmentScreen(viewModel = viewModel)
                            "return" -> ReturnScreen(viewModel = viewModel)
                            "breakage" -> BreakageScreen(viewModel = viewModel)
                            "stock_movement" -> StockMovementScreen(viewModel = viewModel)
                            "reports" -> ReportsScreen(viewModel = viewModel)
                            "customers" -> CustomersScreen(viewModel = viewModel)
                            "expeditions" -> ExpeditionsScreen(viewModel = viewModel)
                            "settings" -> SettingsScreen(
                                viewModel = viewModel,
                                onNavigateTo = { navigateTo(it) }
                            )
                            "more" -> MoreMenuScreen(
                                onNavigateTo = { navigateTo(it) }
                            )
                            "promo_price" -> PromoPriceScreen(viewModel = viewModel)
                            "user_management" -> UserManagementScreen(viewModel = viewModel)
                            else -> DashboardScreen(
                                viewModel = viewModel,
                                onNavigateTo = { navigateTo(it) }
                            )
                        }
                    }
                }

                if (showLogoutDialog) {
                    AlertDialog(
                        onDismissRequest = { showLogoutDialog = false },
                        title = { Text("Konfirmasi Logout") },
                        text = { Text("Apakah Anda yakin ingin keluar dari akun '${currentUser?.fullName}'?") },
                        confirmButton = {
                            Button(
                                onClick = {
                                    showLogoutDialog = false
                                    viewModel.logout()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                            ) {
                                Text("Keluar")
                            }
                        },
                        dismissButton = {
                            OutlinedButton(onClick = { showLogoutDialog = false }) {
                                Text("Batal")
                            }
                        }
                    )
                }
            }
        }
    }
}
