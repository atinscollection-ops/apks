package com.example.ui.components

import android.bluetooth.BluetoothDevice
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Print
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.data.entity.DeliveryOrderEntity
import com.example.printing.BluetoothThermalPrinter
import com.example.printing.ShippingLabelData
import com.example.printing.ThermalLabelPrinter
import com.example.viewmodel.WmsViewModel
import kotlinx.coroutines.launch

@Composable
fun ShippingLabelDialog(
    deliveryOrder: DeliveryOrderEntity,
    viewModel: WmsViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val appSettings by viewModel.appSettings.collectAsState()

    var selectedSize by remember { mutableStateOf(appSettings.labelSize) }
    var itemsSummary by remember { mutableStateOf("") }

    var showBluetoothPicker by remember { mutableStateOf(false) }
    var pairedDevices by remember { mutableStateOf<List<BluetoothDevice>>(emptyList()) }
    var isPrintingBt by remember { mutableStateOf(false) }

    LaunchedEffect(deliveryOrder.doNumber) {
        val doItems = viewModel.repository.getItemsForDOSync(deliveryOrder.doNumber)
        val distinctItems = doItems.size
        val totalQty = doItems.sumOf { it.qty }
        itemsSummary = "$distinctItems item / $totalQty pcs"
    }

    val labelData = remember(deliveryOrder, appSettings, selectedSize, itemsSummary) {
        ShippingLabelData(
            senderName = appSettings.storeName,
            senderPhone = appSettings.storePhone,
            recipientName = deliveryOrder.customerName,
            recipientPhone = deliveryOrder.customerPhone,
            recipientAddress = deliveryOrder.customerAddress,
            expedition = deliveryOrder.shippingExpedition,
            doNumber = deliveryOrder.doNumber,
            soNumber = deliveryOrder.soNumber,
            doDate = deliveryOrder.doDate,
            itemsSummary = itemsSummary,
            paperSize = selectedSize
        )
    }

    val labelBitmap = remember(labelData) {
        ThermalLabelPrinter.createLabelBitmap(labelData)
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.95f)
                .clip(RoundedCornerShape(16.dp)),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Label Delivery Order (DO)",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Tutup")
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Paper size selector (58mm, 80mm, 100mm)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Ukuran:",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    listOf("58mm", "80mm", "100mm").forEach { size ->
                        FilterChip(
                            selected = selectedSize == size,
                            onClick = { selectedSize = size },
                            label = { Text(size) },
                            modifier = Modifier.testTag("chip_size_$size")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Preview Box
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFF2F2F2))
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
                        .verticalScroll(rememberScrollState())
                        .padding(12.dp),
                    contentAlignment = Alignment.TopCenter
                ) {
                    Card(
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(0.dp)
                    ) {
                        Image(
                            bitmap = labelBitmap.asImageBitmap(),
                            contentDescription = "Label DO Preview",
                            modifier = Modifier.padding(2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Action Buttons: Direct Bluetooth vs System Print
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            pairedDevices = BluetoothThermalPrinter.getPairedDevices()
                            showBluetoothPicker = true
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("btn_print_bluetooth"),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        enabled = !isPrintingBt
                    ) {
                        if (isPrintingBt) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White)
                        } else {
                            Icon(Icons.Default.Bluetooth, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Print Bluetooth", style = MaterialTheme.typography.labelMedium)
                        }
                    }

                    OutlinedButton(
                        onClick = {
                            viewModel.printShippingLabel(context, labelData)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("btn_print_system")
                    ) {
                        Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Print / PDF", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
    }

    // Bluetooth Device Selection Dialog
    if (showBluetoothPicker) {
        Dialog(onDismissRequest = { showBluetoothPicker = false }) {
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
                            text = "Pilih Printer Bluetooth",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        IconButton(onClick = { showBluetoothPicker = false }) {
                            Icon(Icons.Default.Close, contentDescription = "Tutup")
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    if (pairedDevices.isEmpty()) {
                        Text(
                            text = "Tidak ada printer Bluetooth yang terpasang (paired).\nSilakan pairing printer thermal Anda di Pengaturan Bluetooth Android terlebih dahulu.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Text(
                            text = "Daftar perangkat Bluetooth:",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            pairedDevices.forEach { dev ->
                                OutlinedCard(
                                    modifier = Modifier.fillMaxWidth(),
                                    onClick = {
                                        showBluetoothPicker = false
                                        isPrintingBt = true
                                        coroutineScope.launch {
                                            val res = BluetoothThermalPrinter.printBitmapDirect(dev, labelBitmap)
                                            isPrintingBt = false
                                            res.onSuccess {
                                                viewModel.showMessage("Label berhasil dikirim ke printer '${dev.name}'!")
                                            }.onFailure {
                                                viewModel.showError(it.message ?: "Gagal print Bluetooth")
                                            }
                                        }
                                    }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Icon(Icons.Default.Bluetooth, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                        Column {
                                            Text(dev.name ?: "Unknown Printer", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                            Text(dev.address, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { showBluetoothPicker = false },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Tutup")
                    }
                }
            }
        }
    }
}
