package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.viewmodel.WmsViewModel

@Composable
fun ReportsScreen(viewModel: WmsViewModel) {
    val context = LocalContext.current
    var selectedReportIndex by remember { mutableIntStateOf(0) }

    val reportTitles = listOf(
        "Stock",
        "Sales Daily",
        "Sales Summary",
        "Adjustment",
        "Receiving",
        "Retur & Rusak"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Laporan & Export Excel",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Preview data dan unduh langsung format .xlsx",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        ScrollableTabRow(
            selectedTabIndex = selectedReportIndex,
            edgePadding = 0.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            reportTitles.forEachIndexed { index, title ->
                Tab(
                    selected = selectedReportIndex == index,
                    onClick = { selectedReportIndex = index },
                    text = { Text(title, fontWeight = if (selectedReportIndex == index) FontWeight.Bold else FontWeight.Normal) }
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        when (selectedReportIndex) {
            0 -> StockReportTab(viewModel = viewModel, onExport = { fam, cond, q -> viewModel.exportStockReport(context, fam, cond, q) })
            1 -> SalesDailyReportTab(viewModel = viewModel, onExport = { from, to -> viewModel.exportSalesDailyReport(context, from, to) })
            2 -> SalesSummaryReportTab(viewModel = viewModel, onExport = { from, to -> viewModel.exportSalesSummaryReport(context, from, to) })
            3 -> AdjustmentReportTab(viewModel = viewModel, onExport = { viewModel.exportAdjustmentReport(context) })
            4 -> ReceivingReportTab(viewModel = viewModel, onExport = { viewModel.exportReceivingReport(context) })
            5 -> ReturnBreakageReportTab(viewModel = viewModel, onExport = { viewModel.exportReturnBreakageReport(context) })
        }
    }
}

@Composable
fun StockReportTab(
    viewModel: WmsViewModel,
    onExport: (String, String, String) -> Unit
) {
    val products by viewModel.products.collectAsState()
    var selectedFamily by remember { mutableStateOf("All") }
    var stockCondition by remember { mutableStateOf("All") }

    val families = remember(products) {
        listOf("All") + products.map { it.familyCode }.distinct().sorted()
    }

    val filteredProducts = remember(selectedFamily, stockCondition, products) {
        products.filter { p ->
            val matchFamily = selectedFamily == "All" || p.familyCode.equals(selectedFamily, true)
            val matchCond = when (stockCondition) {
                "Stock > 0" -> p.stock > 0
                "Stock = 0" -> p.stock == 0
                "Stock < 0" -> p.stock < 0
                else -> true
            }
            matchFamily && matchCond
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("All", "Stock > 0", "Stock = 0").forEach { cond ->
                    FilterChip(
                        selected = stockCondition == cond,
                        onClick = { stockCondition = cond },
                        label = { Text(cond, style = MaterialTheme.typography.labelSmall) }
                    )
                }
            }

            Button(
                onClick = { onExport(selectedFamily, stockCondition, "") },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                modifier = Modifier.testTag("btn_export_stock_xlsx")
            ) {
                Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Export .xlsx", style = MaterialTheme.typography.labelMedium)
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(filteredProducts, key = { it.itemCode }) { p ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(8.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(p.itemName, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                            Text("${p.itemCode} • ${p.familyCode} • Rcv: ${p.lastReceiving}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("Net: ${viewModel.formatRupiah(p.netPrice)} | Promo: ${viewModel.formatRupiah(p.promoPrice)}", style = MaterialTheme.typography.labelSmall)
                        }
                        Surface(
                            color = if (p.stock > 0) Color(0xFFE8F5E9) else Color(0xFFFFEBEE),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                "Stock: ${p.stock}",
                                fontWeight = FontWeight.Bold,
                                color = if (p.stock > 0) Color(0xFF2E7D32) else Color(0xFFC62828),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SalesDailyReportTab(
    viewModel: WmsViewModel,
    onExport: (String, String) -> Unit
) {
    val deliveryOrders by viewModel.deliveryOrders.collectAsState()
    val today = remember { viewModel.getTodayString() }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Dasar Transaksi: Delivery Order", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Button(
                onClick = { onExport("", "") },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                modifier = Modifier.testTag("btn_export_sales_daily_xlsx")
            ) {
                Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Export .xlsx", style = MaterialTheme.typography.labelMedium)
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (deliveryOrders.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                Text("Belum ada data penjualan.", style = MaterialTheme.typography.bodyMedium)
            }
        } else {
            LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(deliveryOrders, key = { it.doNumber }) { doItem ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(8.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("${doItem.doNumber} (${doItem.doDate})", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                Text("Customer: ${doItem.customerName}", style = MaterialTheme.typography.bodySmall)
                                Text("Ekspedisi: ${doItem.shippingExpedition}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                            }
                            Surface(color = Color(0xFFE8F5E9), shape = RoundedCornerShape(4.dp)) {
                                Text("TERKIRIM", color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp), style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SalesSummaryReportTab(
    viewModel: WmsViewModel,
    onExport: (String, String) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Rekapitulasi Penjualan Per Produk", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Button(
                onClick = { onExport("", "") },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                modifier = Modifier.testTag("btn_export_sales_summary_xlsx")
            ) {
                Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Export .xlsx", style = MaterialTheme.typography.labelMedium)
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Klik 'Export .xlsx' di atas untuk mengunduh rekap penjualan lengkap",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Kolom: Item Code, Item Name, Total Qty Terjual, Total Penjualan (Rp), Rata-Rata Harga",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
fun AdjustmentReportTab(
    viewModel: WmsViewModel,
    onExport: () -> Unit
) {
    val adjustments by viewModel.adjustments.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Total: ${adjustments.size} transaksi", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Button(
                onClick = onExport,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                modifier = Modifier.testTag("btn_export_adjustment_xlsx")
            ) {
                Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Export .xlsx", style = MaterialTheme.typography.labelMedium)
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(adjustments, key = { it.adjNumber }) { adj ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(8.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(adj.adjNumber, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            Text("${adj.adjustmentType} (${adj.adjustmentQty})", fontWeight = FontWeight.Bold)
                        }
                        Text("${adj.itemName} (${adj.itemCode})", style = MaterialTheme.typography.bodyMedium)
                        Text("Alasan: ${adj.reason} • Tgl: ${adj.date}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
fun ReceivingReportTab(
    viewModel: WmsViewModel,
    onExport: () -> Unit
) {
    val receivings by viewModel.receivings.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Total: ${receivings.size} transaksi", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Button(
                onClick = onExport,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                modifier = Modifier.testTag("btn_export_receiving_xlsx")
            ) {
                Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Export .xlsx", style = MaterialTheme.typography.labelMedium)
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(receivings, key = { it.rcvNumber }) { rcv ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(8.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(rcv.rcvNumber, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            Text(rcv.rcvDate, style = MaterialTheme.typography.bodySmall)
                        }
                        Text("Berdasarkan: ${rcv.poNumber}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                        Text("Petugas: ${rcv.user}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
fun ReturnBreakageReportTab(
    viewModel: WmsViewModel,
    onExport: () -> Unit
) {
    val returns by viewModel.returns.collectAsState()
    val breakages by viewModel.breakages.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Retur (${returns.size}) | Rusak (${breakages.size})", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Button(
                onClick = onExport,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                modifier = Modifier.testTag("btn_export_return_breakage_xlsx")
            ) {
                Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Export .xlsx", style = MaterialTheme.typography.labelMedium)
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(returns, key = { it.retNumber }) { ret ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(8.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(ret.retNumber, fontWeight = FontWeight.Bold, color = Color(0xFF7B1FA2))
                            Text("RETUR (${ret.quantity} pcs)", fontWeight = FontWeight.Bold, color = Color(0xFF7B1FA2))
                        }
                        Text("${ret.itemName} (${ret.itemCode})", style = MaterialTheme.typography.bodyMedium)
                        Text("Alasan: ${ret.reason} • Tgl: ${ret.date}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            items(breakages, key = { it.brkNumber }) { brk ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(8.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(brk.brkNumber, fontWeight = FontWeight.Bold, color = Color(0xFFC62828))
                            Text("RUSAK (-${brk.quantity} pcs)", fontWeight = FontWeight.Bold, color = Color(0xFFC62828))
                        }
                        Text("${brk.itemName} (${brk.itemCode})", style = MaterialTheme.typography.bodyMedium)
                        Text("Keterangan: ${brk.reason} • Tgl: ${brk.date}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}
