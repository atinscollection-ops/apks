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
import com.example.data.entity.PurchaseOrderEntity
import com.example.data.entity.PurchaseOrderItemEntity
import com.example.data.entity.ReceivingEntity
import com.example.data.entity.ReceivingItemEntity
import com.example.ui.components.DetailItemRow
import com.example.viewmodel.WmsViewModel
import kotlinx.coroutines.launch

@Composable
fun ReceivingScreen(
    viewModel: WmsViewModel,
    presetPONumber: String? = null
) {
    val receivings by viewModel.receivings.collectAsState()
    val pendingPOs by viewModel.pendingPurchaseOrders.collectAsState()

    var showProcessDialog by remember { mutableStateOf(false) }
    var selectedPOForReceiving by remember { mutableStateOf<PurchaseOrderEntity?>(null) }
    var selectedReceivingForDetail by remember { mutableStateOf<ReceivingEntity?>(null) }

    // Auto-open if navigated with presetPONumber
    LaunchedEffect(presetPONumber, pendingPOs) {
        if (!presetPONumber.isNullOrBlank()) {
            val matched = pendingPOs.find { it.poNumber == presetPONumber }
            if (matched != null) {
                selectedPOForReceiving = matched
                showProcessDialog = true
            }
        }
    }

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showProcessDialog = true },
                icon = { Icon(Icons.Default.CallReceived, contentDescription = null) },
                text = { Text("Penerimaan Baru (PO)") },
                modifier = Modifier.testTag("fab_new_receiving"),
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
                text = "Histori Penerimaan Barang (Receiving)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Setiap penerimaan wajib berdasarkan PO dan otomatis menambah stock gudang.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            if (receivings.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.CallReceived,
                            contentDescription = null,
                            modifier = Modifier.size(56.dp),
                            tint = MaterialTheme.colorScheme.outline
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Belum ada histori receiving.", style = MaterialTheme.typography.bodyLarge)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Tekan tombol 'Penerimaan Baru (PO)' untuk memproses PO.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(receivings, key = { it.rcvNumber }) { rcv ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedReceivingForDetail = rcv }
                                .testTag("rcv_card_${rcv.rcvNumber}"),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text(
                                            text = rcv.rcvNumber,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Surface(
                                            color = Color(0xFFE8F5E9),
                                            shape = RoundedCornerShape(4.dp)
                                        ) {
                                            Text(
                                                text = "STOCK +",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF2E7D32),
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Ref PO: ${rcv.poNumber}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = "Tanggal: ${rcv.rcvDate} • Petugas: ${rcv.user}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.outline)
                            }
                        }
                    }
                }
            }
        }
    }

    // Process Receiving Dialog
    if (showProcessDialog) {
        ProcessReceivingDialog(
            pendingPOs = pendingPOs,
            initialPO = selectedPOForReceiving,
            viewModel = viewModel,
            onDismiss = {
                showProcessDialog = false
                selectedPOForReceiving = null
            },
            onConfirm = { poNum, notes, items ->
                viewModel.processReceiving(poNum, notes, items) {
                    showProcessDialog = false
                    selectedPOForReceiving = null
                }
            }
        )
    }

    // Detail Dialog
    selectedReceivingForDetail?.let { rcv ->
        ReceivingDetailDialog(
            rcv = rcv,
            viewModel = viewModel,
            onDismiss = { selectedReceivingForDetail = null }
        )
    }
}

@Composable
fun ProcessReceivingDialog(
    pendingPOs: List<PurchaseOrderEntity>,
    initialPO: PurchaseOrderEntity?,
    viewModel: WmsViewModel,
    onDismiss: () -> Unit,
    onConfirm: (String, String, List<ReceivingItemEntity>) -> Unit
) {
    var selectedPO by remember { mutableStateOf(initialPO ?: pendingPOs.firstOrNull()) }
    var notes by remember { mutableStateOf("") }
    var poItems by remember { mutableStateOf<List<PurchaseOrderItemEntity>>(emptyList()) }
    val receivedQuantities = remember { mutableStateMapOf<String, String>() }

    var alertMessage by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(selectedPO) {
        if (selectedPO != null) {
            val items = viewModel.repository.getItemsForPOSync(selectedPO!!.poNumber)
            poItems = items
            receivedQuantities.clear()
            items.forEach {
                val remaining = (it.qtyOrdered - it.qtyReceived).coerceAtLeast(0)
                receivedQuantities[it.itemCode] = remaining.toString()
            }
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
                    Text(
                        text = "Proses Penerimaan (Receiving)",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Tutup")
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                if (pendingPOs.isEmpty() && initialPO == null) {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text(
                            "Tidak ada Purchase Order berstatus PENDING.\nSilakan buat PO terlebih dahulu.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                } else {
                    // Select PO
                    Text("Pilih Purchase Order:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    var expandedDropdown by remember { mutableStateOf(false) }

                    OutlinedCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { expandedDropdown = true }
                            .testTag("dropdown_select_po"),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = selectedPO?.let { "${it.poNumber} (${it.supplierName})" } ?: "Pilih PO...",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                        }
                    }

                    DropdownMenu(
                        expanded = expandedDropdown,
                        onDismissRequest = { expandedDropdown = false }
                    ) {
                        pendingPOs.forEach { po ->
                            DropdownMenuItem(
                                text = { Text("${po.poNumber} - ${po.supplierName} (${po.poDate})") },
                                onClick = {
                                    selectedPO = po
                                    expandedDropdown = false
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Catatan Penerimaan / No Surat Jalan Supplier") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Item Dalam Purchase Order Ini:",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Item otomatis diverifikasi terhadap PO. Ubah jumlah received bila sebagian/lebih.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(poItems) { item ->
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
                                        Text("${item.itemCode} • Order: ${item.qtyOrdered} | Rcv Lalu: ${item.qtyReceived}", style = MaterialTheme.typography.bodySmall)
                                    }

                                    OutlinedTextField(
                                        value = receivedQuantities[item.itemCode] ?: "0",
                                        onValueChange = { receivedQuantities[item.itemCode] = it },
                                        label = { Text("Qty Masuk") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        modifier = Modifier.width(100.dp).testTag("input_qty_rcv_${item.itemCode}"),
                                        singleLine = true
                                    )
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
                            onClick = {
                                val currentPO = selectedPO
                                if (currentPO == null) {
                                    alertMessage = "Silakan pilih Purchase Order terlebih dahulu."
                                    return@Button
                                }

                                val itemsToReceive = mutableListOf<ReceivingItemEntity>()
                                val poItemCodes = poItems.map { it.itemCode }.toSet()

                                for (item in poItems) {
                                    val qty = receivedQuantities[item.itemCode]?.toIntOrNull() ?: 0
                                    if (qty > 0) {
                                        // CRITICAL BUSINESS RULE 4: Validate item against PO!
                                        if (!poItemCodes.contains(item.itemCode)) {
                                            alertMessage = "Item tidak terdapat pada Purchase Order ini."
                                            return@Button
                                        }
                                        itemsToReceive.add(
                                            ReceivingItemEntity(
                                                rcvNumber = "",
                                                itemCode = item.itemCode,
                                                itemName = item.itemName,
                                                qtyReceived = qty
                                            )
                                        )
                                    }
                                }

                                if (itemsToReceive.isEmpty()) {
                                    alertMessage = "Masukkan minimal 1 item dengan quantity > 0."
                                    return@Button
                                }

                                onConfirm(currentPO.poNumber, notes.trim(), itemsToReceive)
                            },
                            modifier = Modifier.weight(1f).testTag("btn_confirm_receiving"),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Text("Konfirmasi Masuk")
                        }
                    }
                }
            }
        }
    }

    // Alert Dialog for rule violations (e.g. Item not in PO)
    alertMessage?.let { msg ->
        AlertDialog(
            onDismissRequest = { alertMessage = null },
            title = { Text("Peringatan Validasi", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error) },
            text = { Text(msg) },
            confirmButton = {
                Button(onClick = { alertMessage = null }) {
                    Text("OK")
                }
            }
        )
    }
}

@Composable
fun ReceivingDetailDialog(
    rcv: ReceivingEntity,
    viewModel: WmsViewModel,
    onDismiss: () -> Unit
) {
    val itemsFlow = remember(rcv.rcvNumber) { viewModel.repository.getItemsForReceiving(rcv.rcvNumber) }
    val items by itemsFlow.collectAsState(initial = emptyList())

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.fillMaxWidth().fillMaxHeight(0.80f).clip(RoundedCornerShape(16.dp)),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "Detail ${rcv.rcvNumber}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, contentDescription = "Tutup") }
                }

                Spacer(modifier = Modifier.height(8.dp))
                DetailItemRow("No. PO", rcv.poNumber)
                DetailItemRow("Tanggal Masuk", rcv.rcvDate)
                DetailItemRow("Petugas", rcv.user)
                if (rcv.notes.isNotBlank()) DetailItemRow("Catatan", rcv.notes)

                Spacer(modifier = Modifier.height(10.dp))
                Text("Item Diterima (Stock Ditambahkan):", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
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
                                    Text("Kode: ${item.itemCode}", style = MaterialTheme.typography.bodySmall)
                                }
                                Surface(color = Color(0xFFE8F5E9), shape = RoundedCornerShape(4.dp)) {
                                    Text(
                                        "+${item.qtyReceived} pcs",
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF2E7D32),
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                    Text("Tutup")
                }
            }
        }
    }
}
