package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.data.entity.CustomerEntity
import com.example.data.entity.ProductEntity
import com.example.data.entity.SalesOrderEntity
import com.example.data.entity.SalesOrderItemEntity
import com.example.ui.components.DetailItemRow
import com.example.ui.components.SearchableCustomerDialog
import com.example.ui.components.SearchableProductDialog
import com.example.viewmodel.WmsViewModel

@Composable
fun SalesOrderScreen(
    viewModel: WmsViewModel,
    onNavigateToDelivery: (String) -> Unit = {}
) {
    val salesOrders by viewModel.salesOrders.collectAsState()
    val products by viewModel.products.collectAsState()
    val customers by viewModel.customers.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var showCreateSODialog by remember { mutableStateOf(false) }
    var selectedSOForDetail by remember { mutableStateOf<SalesOrderEntity?>(null) }
    var selectedSOForReadyToDeliver by remember { mutableStateOf<SalesOrderEntity?>(null) }

    val filteredSOs = remember(searchQuery, salesOrders) {
        if (searchQuery.isBlank()) salesOrders
        else {
            val q = searchQuery.trim().lowercase()
            salesOrders.filter {
                it.soNumber.lowercase().contains(q) ||
                        it.customerName.lowercase().contains(q) ||
                        it.soDate.contains(q) ||
                        it.status.lowercase().contains(q)
            }
        }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCreateSODialog = true },
                modifier = Modifier.testTag("fab_create_so"),
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Buat Sales Order")
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

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("search_so_field"),
                placeholder = { Text("Cari SO, Customer, Tanggal, Status...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Surface(
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Alur: SO Confirmed → Naik ke Ready to Deliver (bisa ubah barang) → Buat DO",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (filteredSOs.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.ReceiptLong,
                            contentDescription = null,
                            modifier = Modifier.size(56.dp),
                            tint = MaterialTheme.colorScheme.outline
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Belum ada Sales Order", style = MaterialTheme.typography.bodyLarge)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(filteredSOs, key = { it.soNumber }) { so ->
                        SOCard(
                            so = so,
                            viewModel = viewModel,
                            onClick = { selectedSOForDetail = so },
                            onReadyToDeliver = { selectedSOForReadyToDeliver = so },
                            onCreateDO = { onNavigateToDelivery(so.soNumber) }
                        )
                    }
                }
            }
        }
    }

    if (showCreateSODialog) {
        CreateSODialog(
            products = products,
            customers = customers,
            viewModel = viewModel,
            onDismiss = { showCreateSODialog = false },
            onSave = { customer, items, notes ->
                viewModel.createSalesOrder(customer, items, notes) {
                    showCreateSODialog = false
                }
            }
        )
    }

    selectedSOForDetail?.let { so ->
        SODetailDialog(
            so = so,
            viewModel = viewModel,
            onDismiss = { selectedSOForDetail = null },
            onReadyToDeliver = {
                val s = so
                selectedSOForDetail = null
                selectedSOForReadyToDeliver = s
            },
            onCreateDO = {
                val num = so.soNumber
                selectedSOForDetail = null
                onNavigateToDelivery(num)
            }
        )
    }

    selectedSOForReadyToDeliver?.let { so ->
        ReadyToDeliverDialog(
            so = so,
            products = products,
            viewModel = viewModel,
            onDismiss = { selectedSOForReadyToDeliver = null },
            onConfirm = { updatedItems ->
                viewModel.updateSOToReadyToDeliver(so.soNumber, updatedItems) {
                    selectedSOForReadyToDeliver = null
                }
            }
        )
    }
}

@Composable
fun SOCard(
    so: SalesOrderEntity,
    viewModel: WmsViewModel,
    onClick: () -> Unit,
    onReadyToDeliver: () -> Unit,
    onCreateDO: () -> Unit
) {
    val statusBg = when (so.status) {
        "CONFIRMED" -> Color(0xFFFFF3E0)
        "READY_TO_DELIVER" -> Color(0xFFE3F2FD)
        "FULFILLED" -> Color(0xFFE8F5E9)
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
    val statusColor = when (so.status) {
        "CONFIRMED" -> Color(0xFFE65100)
        "READY_TO_DELIVER" -> Color(0xFF1565C0)
        "FULFILLED" -> Color(0xFF2E7D32)
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val statusLabel = when (so.status) {
        "CONFIRMED" -> "CONFIRMED (Menunggu Siap Kirim)"
        "READY_TO_DELIVER" -> "READY TO DELIVER"
        "FULFILLED" -> "TERKIRIM (DO Diterbitkan)"
        else -> so.status
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("so_card_${so.soNumber}"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = so.soNumber,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Surface(
                    color = statusBg,
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = statusLabel,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = statusColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Customer: ${so.customerName}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "Tanggal: ${so.soDate} • Total: ${viewModel.formatRupiah(so.totalAmount)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                if (so.status == "CONFIRMED") {
                    Button(
                        onClick = onReadyToDeliver,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1565C0)),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.testTag("btn_ready_to_deliver_${so.soNumber}")
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Ready to Deliver", style = MaterialTheme.typography.labelMedium)
                    }
                } else if (so.status == "READY_TO_DELIVER") {
                    Button(
                        onClick = onCreateDO,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                        modifier = Modifier.testTag("btn_create_do_${so.soNumber}")
                    ) {
                        Icon(Icons.Default.LocalShipping, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Buat DO & Kirim", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
    }
}

@Composable
fun ReadyToDeliverDialog(
    so: SalesOrderEntity,
    products: List<ProductEntity>,
    viewModel: WmsViewModel,
    onDismiss: () -> Unit,
    onConfirm: (List<SalesOrderItemEntity>) -> Unit
) {
    val itemsFlow = remember(so.soNumber) { viewModel.repository.getItemsForSO(so.soNumber) }
    val initialItems by itemsFlow.collectAsState(initial = emptyList())
    val workingItems = remember { mutableStateListOf<SalesOrderItemEntity>() }

    var showProductPicker by remember { mutableStateOf(false) }

    LaunchedEffect(initialItems) {
        if (workingItems.isEmpty() && initialItems.isNotEmpty()) {
            workingItems.addAll(initialItems)
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.92f)
                .clip(RoundedCornerShape(16.dp)),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Set ke Ready to Deliver",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1565C0)
                        )
                        Text(
                            text = "SO: ${so.soNumber} • ${so.customerName}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, contentDescription = "Tutup") }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Surface(
                    color = Color(0xFFE3F2FD),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Anda dapat menambah, mengurangi item, atau mengubah qty sebelum pesanan diset ke Ready to Deliver.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF1565C0),
                        modifier = Modifier.padding(8.dp)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Item Pesanan (${workingItems.size})", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                    Button(
                        onClick = { showProductPicker = true },
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Tambah Item", style = MaterialTheme.typography.labelSmall)
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(workingItems) { item ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(item.itemName, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                                    Text("${item.itemCode} • @ ${viewModel.formatRupiah(item.unitPrice)}", style = MaterialTheme.typography.bodySmall)
                                    Text("Subtotal: ${viewModel.formatRupiah(item.subtotal)}", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(
                                        onClick = {
                                            val index = workingItems.indexOf(item)
                                            if (index >= 0) {
                                                if (item.qty > 1) {
                                                    val newQty = item.qty - 1
                                                    workingItems[index] = item.copy(qty = newQty, subtotal = newQty * item.unitPrice)
                                                } else {
                                                    workingItems.removeAt(index)
                                                }
                                            }
                                        }
                                    ) {
                                        Icon(Icons.Default.RemoveCircleOutline, contentDescription = "Kurang", tint = MaterialTheme.colorScheme.error)
                                    }

                                    Text(
                                        "${item.qty}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 4.dp)
                                    )

                                    IconButton(
                                        onClick = {
                                            val index = workingItems.indexOf(item)
                                            if (index >= 0) {
                                                val newQty = item.qty + 1
                                                workingItems[index] = item.copy(qty = newQty, subtotal = newQty * item.unitPrice)
                                            }
                                        }
                                    ) {
                                        Icon(Icons.Default.AddCircleOutline, contentDescription = "Tambah", tint = MaterialTheme.colorScheme.primary)
                                    }
                                }
                            }
                        }
                    }
                }

                val total = workingItems.sumOf { it.subtotal }
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Total Revisi:", fontWeight = FontWeight.Bold)
                    Text(viewModel.formatRupiah(total), fontWeight = FontWeight.Bold, color = Color(0xFF1565C0), style = MaterialTheme.typography.titleMedium)
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                        Text("Batal")
                    }
                    Button(
                        onClick = { onConfirm(workingItems.toList()) },
                        modifier = Modifier.weight(1f).testTag("btn_confirm_ready_to_deliver"),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1565C0)),
                        enabled = workingItems.isNotEmpty()
                    ) {
                        Text("Set Ready to Deliver")
                    }
                }
            }
        }
    }

    if (showProductPicker) {
        SearchableProductDialog(
            products = products,
            onDismiss = { showProductPicker = false },
            onProductSelected = { prod ->
                showProductPicker = false
                val price = if (prod.isPromoActive && prod.promoPrice > 0) prod.promoPrice else prod.netPrice
                val existingIndex = workingItems.indexOfFirst { it.itemCode == prod.itemCode }
                if (existingIndex >= 0) {
                    val curr = workingItems[existingIndex]
                    val newQty = curr.qty + 1
                    workingItems[existingIndex] = curr.copy(qty = newQty, subtotal = newQty * curr.unitPrice)
                } else {
                    workingItems.add(
                        SalesOrderItemEntity(
                            soNumber = so.soNumber,
                            itemCode = prod.itemCode,
                            itemName = prod.itemName,
                            qty = 1,
                            unitPrice = price,
                            discount = 0.0,
                            subtotal = price
                        )
                    )
                }
            }
        )
    }
}

@Composable
fun CreateSODialog(
    products: List<ProductEntity>,
    customers: List<CustomerEntity>,
    viewModel: WmsViewModel,
    onDismiss: () -> Unit,
    onSave: (CustomerEntity, List<SalesOrderItemEntity>, String) -> Unit
) {
    var selectedCustomer by remember { mutableStateOf<CustomerEntity?>(null) }
    var notes by remember { mutableStateOf("") }
    val orderItems = remember { mutableStateListOf<SalesOrderItemEntity>() }

    var showCustomerPicker by remember { mutableStateOf(false) }
    var showProductPicker by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.92f)
                .clip(RoundedCornerShape(16.dp)),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Buat Sales Order",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Tutup")
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text("Customer *", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showCustomerPicker = true }
                        .testTag("btn_select_customer"),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = selectedCustomer?.customerName ?: "Pilih Customer (Search Bar)...",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (selectedCustomer != null) FontWeight.Bold else FontWeight.Normal
                            )
                            if (selectedCustomer != null) {
                                Text(
                                    text = "Telp: ${selectedCustomer!!.phoneNumber} • ${selectedCustomer!!.address}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Catatan Order (Opsional)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Item Order (${orderItems.size})",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Button(
                        onClick = { showProductPicker = true },
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.testTag("btn_add_so_product")
                    ) {
                        Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Cari Produk")
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                if (orderItems.isEmpty()) {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text(
                            "Belum ada item dipilih.\nGunakan tombol 'Cari Produk' di atas.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(orderItems) { item ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(item.itemName, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                                        Text("${item.itemCode} • Qty: ${item.qty} pcs", style = MaterialTheme.typography.bodySmall)
                                        Text("Subtotal: ${viewModel.formatRupiah(item.subtotal)}", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                    }
                                    IconButton(onClick = { orderItems.remove(item) }) {
                                        Icon(Icons.Default.Delete, contentDescription = "Hapus", tint = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                        }
                    }
                }

                val totalAmount = orderItems.sumOf { it.subtotal }
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Total Order:", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        viewModel.formatRupiah(totalAmount),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                        Text("Batal")
                    }
                    Button(
                        onClick = {
                            val cust = selectedCustomer ?: return@Button
                            onSave(cust, orderItems.toList(), notes.trim())
                        },
                        modifier = Modifier.weight(1f).testTag("btn_save_so"),
                        enabled = selectedCustomer != null && orderItems.isNotEmpty()
                    ) {
                        Text("Simpan SO (Confirmed)")
                    }
                }
            }
        }
    }

    if (showCustomerPicker) {
        SearchableCustomerDialog(
            customers = customers,
            onDismiss = { showCustomerPicker = false },
            onCustomerSelected = {
                selectedCustomer = it
                showCustomerPicker = false
            }
        )
    }

    if (showProductPicker) {
        SearchableProductDialog(
            products = products,
            onDismiss = { showProductPicker = false },
            onProductSelected = { prod ->
                showProductPicker = false
                val price = if (prod.isPromoActive && prod.promoPrice > 0) prod.promoPrice else prod.netPrice
                orderItems.add(
                    SalesOrderItemEntity(
                        soNumber = "",
                        itemCode = prod.itemCode,
                        itemName = prod.itemName,
                        qty = 1,
                        unitPrice = price,
                        discount = 0.0,
                        subtotal = price * 1
                    )
                )
            }
        )
    }
}

@Composable
fun SODetailDialog(
    so: SalesOrderEntity,
    viewModel: WmsViewModel,
    onDismiss: () -> Unit,
    onReadyToDeliver: () -> Unit,
    onCreateDO: () -> Unit
) {
    val itemsFlow = remember(so.soNumber) { viewModel.repository.getItemsForSO(so.soNumber) }
    val items by itemsFlow.collectAsState(initial = emptyList())

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.fillMaxWidth().fillMaxHeight(0.85f).clip(RoundedCornerShape(16.dp)),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "Detail ${so.soNumber}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, contentDescription = "Tutup") }
                }

                Spacer(modifier = Modifier.height(8.dp))
                DetailItemRow("Customer", so.customerName)
                DetailItemRow("No. Telepon", so.customerPhone)
                DetailItemRow("Alamat", so.customerAddress)
                DetailItemRow("Tanggal SO", so.soDate)
                DetailItemRow("Status", so.status)
                DetailItemRow("Total Order", viewModel.formatRupiah(so.totalAmount))
                if (so.notes.isNotBlank()) DetailItemRow("Catatan", so.notes)

                Spacer(modifier = Modifier.height(10.dp))
                Text("Item Dipesan:", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))

                LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(items) { item ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(item.itemName, fontWeight = FontWeight.SemiBold)
                                    Text("${item.itemCode} • Qty: ${item.qty} pcs", style = MaterialTheme.typography.bodySmall)
                                }
                                Text(viewModel.formatRupiah(item.subtotal), fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                        Text("Tutup")
                    }
                    if (so.status == "CONFIRMED") {
                        Button(
                            onClick = onReadyToDeliver,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1565C0))
                        ) {
                            Text("Ready to Deliver")
                        }
                    } else if (so.status == "READY_TO_DELIVER") {
                        Button(
                            onClick = onCreateDO,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Text("Buat DO")
                        }
                    }
                }
            }
        }
    }
}
