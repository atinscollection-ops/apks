package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun MoreMenuScreen(onNavigateTo: (String) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Text(
            text = "Menu & Operasional Tambahan",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Akses seluruh modul koreksi inventory, pelaporan, dan pengaturan.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Inventory Operations Card
        Text("Operasional Inventory", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(6.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column {
                MenuNavigationRow(
                    icon = Icons.Default.Tune,
                    title = "Stock Adjustment",
                    subtitle = "Koreksi penambahan atau pengurangan stock manual",
                    onClick = { onNavigateTo("stock_adjustment") },
                    testTag = "menu_stock_adjustment"
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                MenuNavigationRow(
                    icon = Icons.Default.AssignmentReturn,
                    title = "Return Barang",
                    subtitle = "Catat return customer (ke stock / non-stock)",
                    onClick = { onNavigateTo("return") },
                    testTag = "menu_return"
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                MenuNavigationRow(
                    icon = Icons.Default.HeartBroken,
                    title = "Barang Rusak (Breakage)",
                    subtitle = "Catat barang rusak/cacat dan potong stock",
                    onClick = { onNavigateTo("breakage") },
                    testTag = "menu_breakage"
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                MenuNavigationRow(
                    icon = Icons.Default.History,
                    title = "Buku Besar Mutasi Stock",
                    subtitle = "Histori audit lengkap semua mutasi masuk dan keluar",
                    onClick = { onNavigateTo("stock_movement") },
                    testTag = "menu_stock_movement"
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Reports Card
        Text("Laporan & Export", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(6.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column {
                MenuNavigationRow(
                    icon = Icons.Default.Assessment,
                    title = "Laporan & Export Excel (.xlsx)",
                    subtitle = "Stock, Sales Daily, Sales Summary, Adjustment, Receiving, Retur",
                    onClick = { onNavigateTo("reports") },
                    testTag = "menu_reports"
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Master Data & Configuration Card
        Text("Master Data & Konfigurasi", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(6.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column {
                MenuNavigationRow(
                    icon = Icons.Default.Sell,
                    title = "Setting Harga Promo",
                    subtitle = "Atur harga promosi / diskon terpisah per produk",
                    onClick = { onNavigateTo("promo_price") },
                    testTag = "menu_promo_price"
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                MenuNavigationRow(
                    icon = Icons.Default.People,
                    title = "Database Customer",
                    subtitle = "Master data pelanggan dan alamat kirim",
                    onClick = { onNavigateTo("customers") },
                    testTag = "menu_customers"
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                MenuNavigationRow(
                    icon = Icons.Default.LocalShipping,
                    title = "Database Jasa Pengiriman",
                    subtitle = "Master data ekspedisi & kurir",
                    onClick = { onNavigateTo("expeditions") },
                    testTag = "menu_expeditions"
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                MenuNavigationRow(
                    icon = Icons.Default.ManageAccounts,
                    title = "Manajemen Pengguna (User ID)",
                    subtitle = "Daftarkan user ID & password (Khusus Root Admin)",
                    onClick = { onNavigateTo("user_management") },
                    testTag = "menu_user_management"
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                MenuNavigationRow(
                    icon = Icons.Default.Settings,
                    title = "Pengaturan Printer & Toko",
                    subtitle = "Ukuran label (58/80/100mm) & data pengirim",
                    onClick = { onNavigateTo("settings") },
                    testTag = "menu_settings"
                )
            }
        }

        Spacer(modifier = Modifier.height(30.dp))
    }
}

@Composable
fun MenuNavigationRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    testTag: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag(testTag)
            .padding(14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f)
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Column {
                Text(text = title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = MaterialTheme.colorScheme.outline)
    }
}
