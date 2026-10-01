package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
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
import com.example.data.entity.StockAdjustmentEntity
import com.example.ui.components.SearchableProductDialog
import com.example.viewmodel.WmsViewModel

@Composable
fun StockAdjustmentScreen(viewModel: WmsViewModel) {
    val adjustments by viewModel.adjustments.collectAsState()
    val products by viewModel.products.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddDialog = true },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Koreksi Stock (Adjustment)") },
                modifier = Modifier.testTag("fab_new_adjustment"),
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Histori Stock Adjustment",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Koreksi stock manual wajib mencatat alasan dan otomatis tercatat di Stock Ledger.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            if (adjustments.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                    Text("Belum ada histori koreksi stock.", style = MaterialTheme.typography.bodyMedium)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(adjustments, key = { it.adjNumber }) { adj ->
                        val isIncrease = adj.adjustmentType == "INCREASE"
                        Card(
                            modifier = Modifier.fillMaxWidth(),
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
                                        text = adj.adjNumber,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Surface(
                                        color = if (isIncrease) Color(0xFFE8F5E9) else Color(0xFFFFEBEE),
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            text = if (isIncrease) "+${adj.adjustmentQty} (${adj.adjustmentType})" else "-${adj.adjustmentQty} (${adj.adjustmentType})",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isIncrease) Color(0xFF2E7D32) else Color(0xFFC62828),
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                Text(
                                    text = adj.itemName,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "${adj.itemCode} • Tanggal: ${adj.date} • Oleh: ${adj.user}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                Spacer(modifier = Modifier.height(4.dp))

                                Text(
                                    text = "Stock: ${adj.previousStock} → ${adj.newStock}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Alasan: ${adj.reason}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                if (adj.notes.isNotBlank()) {
                                    Text(
                                        text = "Catatan: ${adj.notes}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        CreateAdjustmentDialog(
            products = products,
            onDismiss = { showAddDialog = false },
            onSave = { prod, type, qty, reason, notes ->
                viewModel.processStockAdjustment(prod, type, qty, reason, notes) {
                    showAddDialog = false
                }
            }
        )
    }
}

@Composable
fun CreateAdjustmentDialog(
    products: List<ProductEntity>,
    onDismiss: () -> Unit,
    onSave: (ProductEntity, String, Int, String, String) -> Unit
) {
    var selectedProduct by remember { mutableStateOf<ProductEntity?>(null) }
    var adjustmentType by remember { mutableStateOf("INCREASE") }
    var qtyStr by remember { mutableStateOf("1") }
    var reason by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var showProductPicker by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Koreksi Stock (Adjustment)",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, contentDescription = "Tutup") }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Select Product via Search Bar
                Text("Pilih Produk *", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showProductPicker = true }
                        .testTag("btn_select_product_adj"),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = selectedProduct?.itemName ?: "Cari Produk (Search Bar)...",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (selectedProduct != null) FontWeight.Bold else FontWeight.Normal
                            )
                            if (selectedProduct != null) {
                                Text(
                                    text = "${selectedProduct!!.itemCode} • Stock Saat Ini: ${selectedProduct!!.stock} pcs",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Type selector: Increase or Decrease
                Text("Tipe Koreksi:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    FilterChip(
                        selected = adjustmentType == "INCREASE",
                        onClick = { adjustmentType = "INCREASE" },
                        label = { Text("Increase (+ Penambahan)") },
                        modifier = Modifier.weight(1f).testTag("chip_adj_increase")
                    )
                    FilterChip(
                        selected = adjustmentType == "DECREASE",
                        onClick = { adjustmentType = "DECREASE" },
                        label = { Text("Decrease (- Pengurangan)") },
                        modifier = Modifier.weight(1f).testTag("chip_adj_decrease")
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = qtyStr,
                    onValueChange = { qtyStr = it },
                    label = { Text("Jumlah Koreksi (Qty) *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth().testTag("input_adj_qty"),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    label = { Text("Alasan Koreksi (Wajib) *") },
                    placeholder = { Text("Contoh: Selisih stock opname, salah hitung") },
                    modifier = Modifier.fillMaxWidth().testTag("input_adj_reason"),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Catatan Tambahan (Opsional)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(18.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                        Text("Batal")
                    }
                    Button(
                        onClick = {
                            val prod = selectedProduct ?: return@Button
                            val qty = qtyStr.toIntOrNull() ?: 0
                            if (qty > 0 && reason.isNotBlank()) {
                                onSave(prod, adjustmentType, qty, reason.trim(), notes.trim())
                            }
                        },
                        modifier = Modifier.weight(1f).testTag("btn_confirm_adj"),
                        enabled = selectedProduct != null && (qtyStr.toIntOrNull() ?: 0) > 0 && reason.isNotBlank()
                    ) {
                        Text("Proses Koreksi")
                    }
                }
            }
        }
    }

    if (showProductPicker) {
        SearchableProductDialog(
            products = products,
            onDismiss = { showProductPicker = false },
            onProductSelected = {
                selectedProduct = it
                showProductPicker = false
            }
        )
    }
}
