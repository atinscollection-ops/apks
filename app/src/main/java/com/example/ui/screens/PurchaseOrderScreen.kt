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
import com.example.data.entity.ProductEntity
import com.example.data.entity.PurchaseOrderEntity
import com.example.data.entity.PurchaseOrderItemEntity
import com.example.ui.components.DetailItemRow
import com.example.ui.components.SearchableProductDialog
import com.example.viewmodel.WmsViewModel

@Composable
fun PurchaseOrderScreen(
    viewModel: WmsViewModel,
    onNavigateToReceiving: (String) -> Unit = {}
) {
    val poList by viewModel.purchaseOrders.collectAsState()
    val products by viewModel.products.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var showCreateDialog by remember { mutableStateOf(false) }
    var selectedPOForDetail by remember { mutableStateOf<PurchaseOrderEntity?>(null) }

    val filteredPOs = remember(searchQuery, poList) {
        if (searchQuery.isBlank()) poList
        else {
            val q = searchQuery.trim().lowercase()
            poList.filter {
                it.poNumber.lowercase().contains(q) ||
                        it.supplierName.lowercase().contains(q) ||
                        it.poDate.contains(q)
            }
        }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCreateDialog = true },
                modifier = Modifier.testTag("fab_create_po"),
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Buat PO Baru")
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
                    .testTag("search_po_field"),
                placeholder = { Text("Cari PO Number, Supplier, Tanggal...") },
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

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "Total PO: ${filteredPOs.size} order",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (filteredPOs.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.ShoppingBag,
                            contentDescription = null,
                            modifier = Modifier.size(56.dp),
                            tint = MaterialTheme.colorScheme.outline
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Belum ada Purchase Order", style = MaterialTheme.typography.bodyLarge)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(filteredPOs, key = { it.poNumber }) { po ->
                        POCard(
                            po = po,
                            onClick = { selectedPOForDetail = po },
                            onReceive = { onNavigateToReceiving(po.poNumber) }
                        )
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        CreatePODialog(
            products = products,
            viewModel = viewModel,
            onDismiss = { showCreateDialog = false },
            onSave = { supplier, notes, items ->
                viewModel.createPurchaseOrder(supplier, notes, items) {
                    showCreateDialog = false
                }
            }
        )
    }

    selectedPOForDetail?.let { po ->
        PODetailDialog(
            po = po,
            viewModel = viewModel,
            onDismiss = { selectedPOForDetail = null },
            onReceive = {
                val num = po.poNumber
                selectedPOForDetail = null
                onNavigateToReceiving(num)
            }
        )
    }
}

@Composable
fun POCard(
    po: PurchaseOrderEntity,
    onClick: () -> Unit,
    onReceive: () -> Unit
) {
    val statusColor = when (po.status) {
        "COMPLETED" -> Color(0xFF2E7D32)
        "CANCELLED" -> Color(0xFFC62828)
        else -> Color(0xFFE65100) // PENDING
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("po_card_${po.poNumber}"),
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
                    text = po.poNumber,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Surface(
                    color = statusColor.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = po.status,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = statusColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Supplier: ${po.supplierName}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "Tanggal: ${po.poDate}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (po.notes.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Catatan: ${po.notes}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (po.status == "PENDING") {
                Spacer(modifier = Modifier.height(10.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    Button(
                        onClick = onReceive,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.CallReceived, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Proses Receiving", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
    }
}

@Composable
fun CreatePODialog(
    products: List<ProductEntity>,
    viewModel: WmsViewModel,
    onDismiss: () -> Unit,
    onSave: (String, String, List<PurchaseOrderItemEntity>) -> Unit
) {
    var supplierName by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    val orderItems = remember { mutableStateListOf<PurchaseOrderItemEntity>() }

    var showProductPicker by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.90f)
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
                        text = "Buat Purchase Order",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Tutup")
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = supplierName,
                    onValueChange = { supplierName = it },
                    label = { Text("Nama Supplier *") },
                    modifier = Modifier.fillMaxWidth().testTag("input_po_supplier"),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Catatan PO (Opsional)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Item Header + Add Item Button (Search Bar based product selection!)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Daftar Item (${orderItems.size})",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Button(
                        onClick = { showProductPicker = true },
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.testTag("btn_add_po_item")
                    ) {
                        Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Cari Produk")
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                if (orderItems.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "Belum ada item ditambahkan.\nGunakan tombol 'Cari Produk' di atas.",
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
                                        Text("${item.itemCode} • Qty: ${item.qtyOrdered} pcs", style = MaterialTheme.typography.bodySmall)
                                        Text("Harga Satuan: ${viewModel.formatRupiah(item.unitPrice)}", style = MaterialTheme.typography.labelSmall)
                                    }
                                    IconButton(onClick = { orderItems.remove(item) }) {
                                        Icon(Icons.Default.Delete, contentDescription = "Hapus", tint = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                        }
                    }
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
                        onClick = { onSave(supplierName.trim(), notes.trim(), orderItems.toList()) },
                        modifier = Modifier.weight(1f).testTag("btn_save_po"),
                        enabled = supplierName.isNotBlank() && orderItems.isNotEmpty()
                    ) {
                        Text("Simpan PO")
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
                // Add item with default qty 10 and net price
                orderItems.add(
                    PurchaseOrderItemEntity(
                        poNumber = "",
                        itemCode = prod.itemCode,
                        itemName = prod.itemName,
                        qtyOrdered = 10,
                        unitPrice = prod.netPrice,
                        totalPrice = 10 * prod.netPrice
                    )
                )
            }
        )
    }
}

@Composable
fun PODetailDialog(
    po: PurchaseOrderEntity,
    viewModel: WmsViewModel,
    onDismiss: () -> Unit,
    onReceive: () -> Unit
) {
    val itemsFlow = remember(po.poNumber) { viewModel.repository.getItemsForPO(po.poNumber) }
    val items by itemsFlow.collectAsState(initial = emptyList())

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .clip(RoundedCornerShape(16.dp)),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Detail ${po.poNumber}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Tutup")
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                DetailItemRow("Supplier", po.supplierName)
                DetailItemRow("Tanggal PO", po.poDate)
                DetailItemRow("Status", po.status)
                if (po.notes.isNotBlank()) DetailItemRow("Catatan", po.notes)

                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Item Pesanan:",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(items) { item ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(item.itemName, fontWeight = FontWeight.SemiBold)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Kode: ${item.itemCode}", style = MaterialTheme.typography.bodySmall)
                                    Text("Order: ${item.qtyOrdered} | Rcv: ${item.qtyReceived}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                                }
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
                    if (po.status == "PENDING") {
                        Button(
                            onClick = onReceive,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Text("Receiving")
                        }
                    }
                }
            }
        }
    }
}
