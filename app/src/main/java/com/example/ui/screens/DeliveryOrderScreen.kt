package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.data.entity.DeliveryOrderEntity
import com.example.data.entity.SalesOrderEntity
import com.example.data.entity.SalesOrderItemEntity
import com.example.ui.components.DetailItemRow
import com.example.ui.components.ShippingLabelDialog
import com.example.viewmodel.WmsViewModel

@Composable
fun DeliveryOrderScreen(
    viewModel: WmsViewModel,
    presetSONumber: String? = null
) {
    val deliveryOrders by viewModel.deliveryOrders.collectAsState()
    val readyToDeliverSalesOrders by viewModel.readyToDeliverSalesOrders.collectAsState()
    val activeShippingServices by viewModel.activeShippingServices.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var showCreateDODialog by remember { mutableStateOf(false) }
    var selectedDOForLabel by remember { mutableStateOf<DeliveryOrderEntity?>(null) }
    var selectedDOForDetail by remember { mutableStateOf<DeliveryOrderEntity?>(null) }

    // Auto-open if navigated with presetSONumber
    var presetSO by remember { mutableStateOf<SalesOrderEntity?>(null) }
    LaunchedEffect(presetSONumber, readyToDeliverSalesOrders) {
        if (!presetSONumber.isNullOrBlank()) {
            val matched = readyToDeliverSalesOrders.find { it.soNumber == presetSONumber }
            if (matched != null) {
                presetSO = matched
                showCreateDODialog = true
            }
        }
    }

    val filteredDOs = remember(searchQuery, deliveryOrders) {
        if (searchQuery.isBlank()) deliveryOrders
        else {
            val q = searchQuery.trim().lowercase()
            deliveryOrders.filter {
                it.doNumber.lowercase().contains(q) ||
                        it.soNumber.lowercase().contains(q) ||
                        it.customerName.lowercase().contains(q) ||
                        it.shippingExpedition.lowercase().contains(q)
            }
        }
    }

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    presetSO = null
                    showCreateDODialog = true
                },
                icon = { Icon(Icons.Default.LocalShipping, contentDescription = null) },
                text = { Text("Proses DO (Kirim)") },
                modifier = Modifier.testTag("fab_create_do"),
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

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("search_do_field"),
                placeholder = { Text("Cari DO Number, SO, Customer, Ekspedisi...") },
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

            Surface(
                color = Color(0xFFE8F5E9),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = Color(0xFF2E7D32)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Aturan WMS: DO memvalidasi stock, memotong stock, & siap cetak label thermal.",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF2E7D32)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (filteredDOs.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.LocalShipping,
                            contentDescription = null,
                            modifier = Modifier.size(56.dp),
                            tint = MaterialTheme.colorScheme.outline
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Belum ada Delivery Order", style = MaterialTheme.typography.bodyLarge)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(filteredDOs, key = { it.doNumber }) { doItem ->
                        DOCard(
                            deliveryOrder = doItem,
                            onClick = { selectedDOForDetail = doItem },
                            onPrintLabel = { selectedDOForLabel = doItem }
                        )
                    }
                }
            }
        }
    }

    // Create DO Dialog
    if (showCreateDODialog) {
        CreateDODialog(
            orderedSalesOrders = readyToDeliverSalesOrders,
            presetSO = presetSO,
            shippingServices = activeShippingServices,
            viewModel = viewModel,
            onDismiss = {
                showCreateDODialog = false
                presetSO = null
            },
            onSuccess = { createdDO ->
                showCreateDODialog = false
                presetSO = null
                // Immediately open shipping label preview!
                selectedDOForLabel = createdDO
            }
        )
    }

    // Detail Dialog
    selectedDOForDetail?.let { doItem ->
        DODetailDialog(
            deliveryOrder = doItem,
            viewModel = viewModel,
            onDismiss = { selectedDOForDetail = null },
            onPrintLabel = {
                selectedDOForDetail = null
                selectedDOForLabel = doItem
            }
        )
    }

    // Thermal Shipping Label Dialog
    selectedDOForLabel?.let { doItem ->
        ShippingLabelDialog(
            deliveryOrder = doItem,
            viewModel = viewModel,
            onDismiss = { selectedDOForLabel = null }
        )
    }
}

@Composable
fun DOCard(
    deliveryOrder: DeliveryOrderEntity,
    onClick: () -> Unit,
    onPrintLabel: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("do_card_${deliveryOrder.doNumber}"),
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
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = deliveryOrder.doNumber,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Surface(
                        color = Color(0xFFE8F5E9),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = "DELIVERY -",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF2E7D32),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = deliveryOrder.shippingExpedition,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Penerima: ${deliveryOrder.customerName} (${deliveryOrder.customerPhone})",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "Ref: ${deliveryOrder.soNumber} • Tanggal: ${deliveryOrder.doDate}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Alamat: ${deliveryOrder.customerAddress}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                OutlinedButton(
                    onClick = onPrintLabel,
                    modifier = Modifier.testTag("btn_print_label_${deliveryOrder.doNumber}"),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Cetak Label Thermal", style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}

@Composable
fun CreateDODialog(
    orderedSalesOrders: List<SalesOrderEntity>,
    presetSO: SalesOrderEntity?,
    shippingServices: List<com.example.data.entity.ShippingServiceEntity>,
    viewModel: WmsViewModel,
    onDismiss: () -> Unit,
    onSuccess: (DeliveryOrderEntity) -> Unit
) {
    var selectedSO by remember { mutableStateOf(presetSO ?: orderedSalesOrders.firstOrNull()) }
    var selectedExpedition by remember { mutableStateOf(shippingServices.firstOrNull()?.name ?: "JNE Express") }
    var soItems by remember { mutableStateOf<List<SalesOrderItemEntity>>(emptyList()) }

    var alertTitle by remember { mutableStateOf<String?>(null) }
    var alertMessage by remember { mutableStateOf<String?>(null) }
    var isSubmitting by remember { mutableStateOf(false) }

    LaunchedEffect(selectedSO) {
        if (selectedSO != null) {
            val items = viewModel.repository.getItemsForSOSync(selectedSO!!.soNumber)
            soItems = items
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
                        text = "Buat Delivery Order (DO)",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Tutup")
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (orderedSalesOrders.isEmpty() && presetSO == null) {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text(
                            "Tidak ada Sales Order yang belum terkirim.\nSilakan buat Sales Order terlebih dahulu.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                } else {
                    // SO Dropdown
                    Text("Pilih Sales Order:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    var expandedSODropdown by remember { mutableStateOf(false) }

                    OutlinedCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { expandedSODropdown = true }
                            .testTag("dropdown_select_so"),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = selectedSO?.let { "${it.soNumber} - ${it.customerName}" } ?: "Pilih SO...",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                        }
                    }

                    DropdownMenu(
                        expanded = expandedSODropdown,
                        onDismissRequest = { expandedSODropdown = false }
                    ) {
                        orderedSalesOrders.forEach { so ->
                            DropdownMenuItem(
                                text = { Text("${so.soNumber} - ${so.customerName} (${viewModel.formatRupiah(so.totalAmount)})") },
                                onClick = {
                                    selectedSO = so
                                    expandedSODropdown = false
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Expedition Selector (from Database!)
                    Text("Pilih Jasa Pengiriman (Ekspedisi):", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    var expandedExpDropdown by remember { mutableStateOf(false) }

                    OutlinedCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { expandedExpDropdown = true }
                            .testTag("dropdown_select_expedition"),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = selectedExpedition,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                        }
                    }

                    DropdownMenu(
                        expanded = expandedExpDropdown,
                        onDismissRequest = { expandedExpDropdown = false }
                    ) {
                        shippingServices.forEach { service ->
                            DropdownMenuItem(
                                text = { Text(service.name) },
                                onClick = {
                                    selectedExpedition = service.name
                                    expandedExpDropdown = false
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Customer Details Preview
                    selectedSO?.let { so ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text("Penerima: ${so.customerName} • ${so.customerPhone}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                Text("Alamat: ${so.customerAddress}", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text("Item Pesanan yang Dikirim & Dipotong:", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))

                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(soItems) { item ->
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
                                        Text("${item.itemCode} • Qty Kirim: ${item.qty} pcs", style = MaterialTheme.typography.bodySmall)
                                    }
                                    Surface(color = Color(0xFFFFEBEE), shape = RoundedCornerShape(4.dp)) {
                                        Text("-${item.qty} pcs", fontWeight = FontWeight.Bold, color = Color(0xFFC62828), modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
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
                            onClick = {
                                val so = selectedSO ?: return@Button
                                if (isSubmitting) return@Button
                                isSubmitting = true

                                viewModel.createDeliveryOrder(
                                    so = so,
                                    soItems = soItems,
                                    shippingExpedition = selectedExpedition,
                                    onSuccess = { createdDO ->
                                        isSubmitting = false
                                        onSuccess(createdDO)
                                    }
                                )
                            },
                            modifier = Modifier.weight(1f).testTag("btn_confirm_do"),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            enabled = selectedSO != null && soItems.isNotEmpty() && !isSubmitting
                        ) {
                            if (isSubmitting) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White)
                            } else {
                                Text("Konfirmasi & Potong")
                            }
                        }
                    }
                }
            }
        }
    }

    alertMessage?.let { msg ->
        AlertDialog(
            onDismissRequest = { alertMessage = null },
            title = { Text(alertTitle ?: "Peringatan", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error) },
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
fun DODetailDialog(
    deliveryOrder: DeliveryOrderEntity,
    viewModel: WmsViewModel,
    onDismiss: () -> Unit,
    onPrintLabel: () -> Unit
) {
    val itemsFlow = remember(deliveryOrder.doNumber) { viewModel.repository.getItemsForDO(deliveryOrder.doNumber) }
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
                    Text(text = "Detail ${deliveryOrder.doNumber}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, contentDescription = "Tutup") }
                }

                Spacer(modifier = Modifier.height(8.dp))
                DetailItemRow("No. SO Referensi", deliveryOrder.soNumber)
                DetailItemRow("Penerima", deliveryOrder.customerName)
                DetailItemRow("No. HP", deliveryOrder.customerPhone)
                DetailItemRow("Alamat Lengkap", deliveryOrder.customerAddress)
                DetailItemRow("Ekspedisi", deliveryOrder.shippingExpedition)
                DetailItemRow("Tanggal Kirim", deliveryOrder.doDate)

                Spacer(modifier = Modifier.height(10.dp))
                Text("Item yang Dikirim:", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
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
                                Text("Diproses", color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
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
                    Button(
                        onClick = onPrintLabel,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Cetak Label")
                    }
                }
            }
        }
    }
}
