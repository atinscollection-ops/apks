package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.entity.StockMovementEntity
import com.example.ui.components.StatCard
import com.example.viewmodel.WmsViewModel

@Composable
fun DashboardScreen(
    viewModel: WmsViewModel,
    onNavigateTo: (String) -> Unit
) {
    val products by viewModel.products.collectAsState()
    val deliveryOrders by viewModel.deliveryOrders.collectAsState()
    val receivings by viewModel.receivings.collectAsState()
    val returns by viewModel.returns.collectAsState()
    val breakages by viewModel.breakages.collectAsState()
    val movements by viewModel.stockMovements.collectAsState()
    val appSettings by viewModel.appSettings.collectAsState()

    val today = remember { viewModel.getTodayString() }

    // Inventory metrics
    val totalSku = products.size
    val totalStock = products.sumOf { it.stock }
    val outOfStock = products.count { it.stock <= 0 }
    val lowStock = products.count { it.stock in 1..10 }

    // Today's metrics
    val todayDOs = deliveryOrders.filter { it.doDate == today }
    val todayReceivingsCount = receivings.count { it.rcvDate == today }
    val todayReturnsCount = returns.count { it.date == today }
    val todayBreakagesCount = breakages.count { it.date == today }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Welcome Banner
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = appSettings.storeName,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "Warehouse Management System • $today",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                    }
                    Surface(
                        color = MaterialTheme.colorScheme.primary,
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            text = "Live WMS",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }

        // Section: Quick Shortcuts
        item {
            Text(
                text = "Aksi Cepat",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                QuickActionButton(
                    icon = Icons.Default.ShoppingBag,
                    label = "Sales Order",
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigateTo("sales_order") }
                )
                QuickActionButton(
                    icon = Icons.Default.LocalShipping,
                    label = "Kirim DO",
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigateTo("delivery_order") }
                )
                QuickActionButton(
                    icon = Icons.Default.CallReceived,
                    label = "Receiving",
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigateTo("receiving") }
                )
                QuickActionButton(
                    icon = Icons.Default.Tune,
                    label = "Adjustment",
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigateTo("stock_adjustment") }
                )
            }
        }

        // Section: Inventory Stats
        item {
            Text(
                text = "Ringkasan Inventory",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatCard(
                    title = "Total SKU",
                    value = "$totalSku",
                    icon = Icons.Default.Inventory2,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    title = "Total Stock",
                    value = "$totalStock",
                    icon = Icons.Default.AllInbox,
                    color = Color(0xFF1976D2),
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatCard(
                    title = "Habis (OOS)",
                    value = "$outOfStock",
                    icon = Icons.Default.Warning,
                    color = if (outOfStock > 0) Color(0xFFD32F2F) else Color(0xFF388E3C),
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    title = "Stock Menipis",
                    value = "$lowStock",
                    icon = Icons.Default.LowPriority,
                    color = Color(0xFFF57C00),
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Section: Daily Operations
        item {
            Text(
                text = "Aktivitas Hari Ini ($today)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatCard(
                    title = "DO Terkirim",
                    value = "${todayDOs.size}",
                    icon = Icons.Default.LocalShipping,
                    color = Color(0xFF00796B),
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    title = "Receiving PO",
                    value = "$todayReceivingsCount",
                    icon = Icons.Default.Inventory,
                    color = Color(0xFF512DA8),
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatCard(
                    title = "Barang Retur",
                    value = "$todayReturnsCount",
                    icon = Icons.Default.AssignmentReturn,
                    color = Color(0xFFE65100),
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    title = "Barang Rusak",
                    value = "$todayBreakagesCount",
                    icon = Icons.Default.HeartBroken,
                    color = Color(0xFFC2185B),
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Section: Recent Stock Movements
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Mutasi Stock Terakhir",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                TextButton(onClick = { onNavigateTo("stock_movement") }) {
                    Text("Lihat Semua")
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                }
            }
        }

        val recentMovements = movements.take(5)
        if (recentMovements.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Box(modifier = Modifier.padding(24.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text("Belum ada mutasi stock tercatat.", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        } else {
            items(recentMovements, key = { it.movementId }) { movement ->
                MovementItemCard(movement)
            }
        }
    }
}

@Composable
fun QuickActionButton(
    icon: ImageVector,
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .testTag("quick_action_${label.lowercase().replace(" ", "_")}"),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 12.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                icon,
                contentDescription = label,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Medium,
                maxLines = 1
            )
        }
    }
}

@Composable
fun MovementItemCard(movement: StockMovementEntity) {
    val isPositive = movement.qtyIn > 0
    val deltaText = if (isPositive) "+${movement.qtyIn}" else "-${movement.qtyOut}"
    val badgeColor = when (movement.type) {
        "RECEIVING" -> Color(0xFF2E7D32)
        "DELIVERY" -> Color(0xFF1565C0)
        "ADJUSTMENT" -> Color(0xFFE65100)
        "RETURN" -> Color(0xFF7B1FA2)
        "BREAKAGE" -> Color(0xFFC62828)
        else -> MaterialTheme.colorScheme.primary
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(10.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = badgeColor.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = movement.type,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = badgeColor,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Text(
                        text = movement.referenceId,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = movement.date,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = movement.itemName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "${movement.itemCode} • ${movement.reason}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = deltaText,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isPositive) Color(0xFF2E7D32) else Color(0xFFC62828)
                )
                Text(
                    text = "Stock: ${movement.newStock}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
