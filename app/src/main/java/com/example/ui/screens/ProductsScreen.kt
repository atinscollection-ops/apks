package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.data.entity.ProductEntity
import com.example.ui.components.DetailItemRow
import com.example.viewmodel.WmsViewModel

@Composable
fun ProductsScreen(viewModel: WmsViewModel) {
    val products by viewModel.products.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var selectedFamilyFilter by remember { mutableStateOf("All") }

    var showAddEditDialog by remember { mutableStateOf(false) }
    var editingProduct by remember { mutableStateOf<ProductEntity?>(null) }
    var selectedProductForDetail by remember { mutableStateOf<ProductEntity?>(null) }
    var productToDelete by remember { mutableStateOf<ProductEntity?>(null) }

    val families = remember(products) {
        listOf("All") + products.map { it.familyCode }.distinct().sorted()
    }

    val filteredProducts = remember(searchQuery, selectedFamilyFilter, products) {
        products.filter { p ->
            val matchFamily = selectedFamilyFilter == "All" || p.familyCode.equals(selectedFamilyFilter, true)
            val matchQuery = if (searchQuery.isBlank()) true else {
                val q = searchQuery.trim().lowercase()
                p.itemName.lowercase().contains(q) ||
                        p.itemCode.lowercase().contains(q) ||
                        p.familyCode.lowercase().contains(q)
            }
            matchFamily && matchQuery
        }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    editingProduct = null
                    showAddEditDialog = true
                },
                modifier = Modifier.testTag("fab_add_product"),
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Tambah Produk")
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

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("search_product_field"),
                placeholder = { Text("Cari SKU, Nama, Family Code...") },
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

            // Family filter chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                families.take(5).forEach { fam ->
                    FilterChip(
                        selected = selectedFamilyFilter == fam,
                        onClick = { selectedFamilyFilter = fam },
                        label = { Text(fam) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Results count
            Text(
                text = "Total Produk: ${filteredProducts.size} item (Kode otomatis SKU-xxx)",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(6.dp))

            if (filteredProducts.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.Inventory2,
                            contentDescription = null,
                            modifier = Modifier.size(56.dp),
                            tint = MaterialTheme.colorScheme.outline
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Tidak ada produk yang cocok",
                            style = MaterialTheme.typography.bodyLarge,
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
                    items(filteredProducts, key = { it.itemCode }) { product ->
                        ProductItemCard(
                            product = product,
                            viewModel = viewModel,
                            onClick = { selectedProductForDetail = product },
                            onEdit = {
                                editingProduct = product
                                showAddEditDialog = true
                            },
                            onDelete = {
                                productToDelete = product
                            }
                        )
                    }
                }
            }
        }
    }

    // Detail Dialog
    selectedProductForDetail?.let { prod ->
        ProductDetailDialog(
            product = prod,
            viewModel = viewModel,
            onDismiss = { selectedProductForDetail = null },
            onEdit = {
                selectedProductForDetail = null
                editingProduct = prod
                showAddEditDialog = true
            }
        )
    }

    // Add / Edit Dialog
    if (showAddEditDialog) {
        ProductAddEditDialog(
            product = editingProduct,
            viewModel = viewModel,
            onDismiss = { showAddEditDialog = false },
            onSave = { productToSave ->
                viewModel.saveProduct(productToSave) {
                    showAddEditDialog = false
                }
            }
        )
    }

    // Delete Confirmation Dialog (Admin only)
    productToDelete?.let { prod ->
        AlertDialog(
            onDismissRequest = { productToDelete = null },
            title = { Text("Hapus Produk", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error) },
            text = { Text("Apakah Anda yakin ingin menghapus produk '${prod.itemName}' (${prod.itemCode})?") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteProduct(prod)
                        productToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Hapus")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { productToDelete = null }) {
                    Text("Batal")
                }
            }
        )
    }
}

@Composable
fun ProductItemCard(
    product: ProductEntity,
    viewModel: WmsViewModel,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("product_card_${product.itemCode}"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = product.itemCode,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                        Text(
                            text = product.familyCode,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (product.isPromoActive) {
                            Surface(
                                color = Color(0xFFFCE4EC),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = "PROMO",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFC2185B),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = product.itemName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row {
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.primary)
                    }
                    if (viewModel.isAdminOrAbove()) {
                        IconButton(onClick = onDelete) {
                            Icon(Icons.Default.Delete, contentDescription = "Hapus", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "HPP: ${viewModel.formatRupiah(product.purchasingPrice)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Jual: ${viewModel.formatRupiah(product.netPrice)}",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    if (product.isPromoActive && product.promoPrice > 0) {
                        Text(
                            text = "Harga Promo: ${viewModel.formatRupiah(product.promoPrice)}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFC2185B)
                        )
                    }
                    Text(
                        text = "Last Rcv: ${product.lastReceiving}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Surface(
                    color = if (product.stock > 10) Color(0xFFE8F5E9)
                    else if (product.stock > 0) Color(0xFFFFF3E0)
                    else Color(0xFFFFEBEE),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "Stock: ${product.stock}",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (product.stock > 10) Color(0xFF2E7D32)
                        else if (product.stock > 0) Color(0xFFE65100)
                        else Color(0xFFC62828),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ProductDetailDialog(
    product: ProductEntity,
    viewModel: WmsViewModel,
    onDismiss: () -> Unit,
    onEdit: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp)),
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
                        text = "Detail Produk",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Tutup")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                DetailItemRow("Family Code", product.familyCode)
                DetailItemRow("Item Code (SKU)", product.itemCode)
                DetailItemRow("Item Name", product.itemName)
                DetailItemRow("Current Stock", "${product.stock} pcs")
                DetailItemRow("Harga Beli (HPP)", viewModel.formatRupiah(product.purchasingPrice))
                DetailItemRow("Harga Jual (Net)", viewModel.formatRupiah(product.netPrice))
                val promoText = if (product.isPromoActive) "${viewModel.formatRupiah(product.promoPrice)} (Aktif)" else "${viewModel.formatRupiah(product.promoPrice)} (Nonaktif)"
                DetailItemRow("Harga Promo", promoText)
                val grossMargin = product.netPrice - product.purchasingPrice
                val marginPct = if (product.netPrice > 0) (grossMargin / product.netPrice) * 100 else 0.0
                DetailItemRow("Est. Gross Margin", "${viewModel.formatRupiah(grossMargin)} (${String.format(java.util.Locale.US, "%.1f%%", marginPct)})")
                DetailItemRow("Last Receiving", product.lastReceiving)

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Tutup")
                    }
                    Button(
                        onClick = onEdit,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Edit Produk")
                    }
                }
            }
        }
    }
}

@Composable
fun ProductAddEditDialog(
    product: ProductEntity?,
    viewModel: WmsViewModel,
    onDismiss: () -> Unit,
    onSave: (ProductEntity) -> Unit
) {
    val isEdit = product != null

    var familyCode by remember { mutableStateOf(product?.familyCode ?: "GAMIS") }
    var autoItemCode by remember { mutableStateOf(product?.itemCode ?: "") }
    var itemName by remember { mutableStateOf(product?.itemName ?: "") }
    var purchasingPriceStr by remember { mutableStateOf(product?.purchasingPrice?.toLong()?.toString() ?: "0") }
    var netPriceStr by remember { mutableStateOf(product?.netPrice?.toLong()?.toString() ?: "0") }
    var initialStockStr by remember { mutableStateOf(product?.stock?.toString() ?: "0") }

    // Auto-generate SKU for new products
    LaunchedEffect(Unit) {
        if (!isEdit && autoItemCode.isBlank()) {
            viewModel.getNextItemCode { code ->
                autoItemCode = code
            }
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp)),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
            ) {
                Text(
                    text = if (isEdit) "Edit Produk" else "Tambah Produk Baru",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Auto Item Code Badge (NO manual input box!)
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Item Code (Otomatis):",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = if (autoItemCode.isNotBlank()) autoItemCode else "Generating...",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = familyCode,
                    onValueChange = { familyCode = it.uppercase() },
                    label = { Text("Family Code (e.g. GAMIS, HIJAB, TUNIK)") },
                    modifier = Modifier.fillMaxWidth().testTag("input_family_code"),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = itemName,
                    onValueChange = { itemName = it },
                    label = { Text("Item Name *") },
                    modifier = Modifier.fillMaxWidth().testTag("input_item_name"),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Purchasing Price (Harga Beli / HPP) & Selling Price (Net Price)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = purchasingPriceStr,
                        onValueChange = { purchasingPriceStr = it },
                        label = { Text("Harga Beli / HPP *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f).testTag("input_purchasing_price"),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = netPriceStr,
                        onValueChange = { netPriceStr = it },
                        label = { Text("Harga Jual Net *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f).testTag("input_net_price"),
                        singleLine = true
                    )
                }

                if (!isEdit) {
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = initialStockStr,
                        onValueChange = { initialStockStr = it },
                        label = { Text("Stock Awal") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth().testTag("input_initial_stock"),
                        singleLine = true
                    )
                } else {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Catatan: Harga Promo diatur pada menu khusus 'Setting Harga Promo'.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Batal")
                    }
                    Button(
                        onClick = {
                            val hpp = purchasingPriceStr.toDoubleOrNull() ?: 0.0
                            val net = netPriceStr.toDoubleOrNull() ?: 0.0
                            val stock = if (!isEdit) initialStockStr.toIntOrNull() ?: 0 else product.stock

                            val entity = ProductEntity(
                                id = product?.id ?: 0L,
                                itemCode = autoItemCode.trim(),
                                familyCode = familyCode.trim(),
                                itemName = itemName.trim(),
                                lastReceiving = product?.lastReceiving ?: "-",
                                stock = stock,
                                purchasingPrice = hpp,
                                netPrice = net,
                                promoPrice = product?.promoPrice ?: 0.0,
                                isPromoActive = product?.isPromoActive ?: false,
                                isActive = true
                            )
                            onSave(entity)
                        },
                        modifier = Modifier.weight(1f).testTag("btn_save_product"),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        enabled = autoItemCode.isNotBlank() && itemName.isNotBlank()
                    ) {
                        Text("Simpan")
                    }
                }
            }
        }
    }
}
