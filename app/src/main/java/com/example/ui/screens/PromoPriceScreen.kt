package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
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
import com.example.viewmodel.WmsViewModel

@Composable
fun PromoPriceScreen(viewModel: WmsViewModel) {
    val products by viewModel.products.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var editingPromoProduct by remember { mutableStateOf<ProductEntity?>(null) }

    val filteredProducts = remember(searchQuery, products) {
        if (searchQuery.isBlank()) products
        else {
            val q = searchQuery.trim().lowercase()
            products.filter {
                it.itemName.lowercase().contains(q) ||
                        it.itemCode.lowercase().contains(q) ||
                        it.familyCode.lowercase().contains(q)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Pengaturan Harga Promo Produk",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Atur harga khusus diskon/promosi untuk setiap produk secara terpisah dari form input master produk.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier.fillMaxWidth().testTag("search_promo_field"),
            placeholder = { Text("Cari produk SKU, nama, family...") },
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

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            items(filteredProducts, key = { it.itemCode }) { product ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { editingPromoProduct = product },
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
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = product.itemCode,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                                Text(
                                    text = product.familyCode,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = product.itemName,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text(
                                    text = "Harga Normal: ${viewModel.formatRupiah(product.netPrice)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "Promo: ${if (product.promoPrice > 0) viewModel.formatRupiah(product.promoPrice) else "-"}",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (product.isPromoActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                                )
                            }
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Switch(
                                checked = product.isPromoActive,
                                onCheckedChange = { active ->
                                    viewModel.updatePromoPrice(product.itemCode, product.promoPrice, active)
                                }
                            )
                            Text(
                                text = if (product.isPromoActive) "Promo Aktif" else "Nonaktif",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (product.isPromoActive) Color(0xFF2E7D32) else MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                }
            }
        }
    }

    editingPromoProduct?.let { prod ->
        EditPromoDialog(
            product = prod,
            viewModel = viewModel,
            onDismiss = { editingPromoProduct = null },
            onSave = { newPromoPrice, isPromoActive ->
                viewModel.updatePromoPrice(prod.itemCode, newPromoPrice, isPromoActive) {
                    editingPromoProduct = null
                }
            }
        )
    }
}

@Composable
fun EditPromoDialog(
    product: ProductEntity,
    viewModel: WmsViewModel,
    onDismiss: () -> Unit,
    onSave: (Double, Boolean) -> Unit
) {
    var promoPriceStr by remember { mutableStateOf(product.promoPrice.toLong().toString()) }
    var isPromoActive by remember { mutableStateOf(product.isPromoActive) }

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
                        text = "Setting Harga Promo",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, contentDescription = "Tutup") }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(product.itemName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                Text("${product.itemCode} • Harga Normal: ${viewModel.formatRupiah(product.netPrice)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = promoPriceStr,
                    onValueChange = { promoPriceStr = it },
                    label = { Text("Harga Promo Baru (Rp)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth().testTag("input_promo_price_val"),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Status Promo Aktif:", fontWeight = FontWeight.SemiBold)
                    Switch(checked = isPromoActive, onCheckedChange = { isPromoActive = it })
                }

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
                            val price = promoPriceStr.toDoubleOrNull() ?: 0.0
                            onSave(price, isPromoActive)
                        },
                        modifier = Modifier.weight(1f).testTag("btn_save_promo_price"),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text("Simpan Promo")
                    }
                }
            }
        }
    }
}
