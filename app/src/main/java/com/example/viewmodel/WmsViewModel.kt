package com.example.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.database.AppDatabase
import com.example.data.entity.*
import com.example.data.repository.WmsRepository
import com.example.excel.ExcelExporter
import com.example.printing.ShippingLabelData
import com.example.printing.ThermalLabelPrinter
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class WmsViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application, viewModelScope)
    val repository = WmsRepository(database)

    init {
        repository.firestoreSync.startRealtimeSync(viewModelScope)
    }

    // Current Logged-in User
    private val _currentUser = MutableStateFlow<UserEntity?>(null)
    val currentUser: StateFlow<UserEntity?> = _currentUser.asStateFlow()

    // Users List for Admin Management
    val users: StateFlow<List<UserEntity>> = repository.allUsers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val products: StateFlow<List<ProductEntity>> = repository.allProducts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val customers: StateFlow<List<CustomerEntity>> = repository.allCustomers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val shippingServices: StateFlow<List<ShippingServiceEntity>> = repository.allShippingServices
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeShippingServices: StateFlow<List<ShippingServiceEntity>> = repository.activeShippingServices
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val purchaseOrders: StateFlow<List<PurchaseOrderEntity>> = repository.allPurchaseOrders
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val pendingPurchaseOrders: StateFlow<List<PurchaseOrderEntity>> = repository.pendingPurchaseOrders
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val receivings: StateFlow<List<ReceivingEntity>> = repository.allReceivings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val salesOrders: StateFlow<List<SalesOrderEntity>> = repository.allSalesOrders
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val readyToDeliverSalesOrders: StateFlow<List<SalesOrderEntity>> = repository.readyToDeliverSalesOrders
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val confirmedSalesOrders: StateFlow<List<SalesOrderEntity>> = repository.confirmedSalesOrders
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val deliveryOrders: StateFlow<List<DeliveryOrderEntity>> = repository.allDeliveryOrders
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val adjustments: StateFlow<List<StockAdjustmentEntity>> = repository.allAdjustments
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val returns: StateFlow<List<ReturnEntity>> = repository.allReturns
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val breakages: StateFlow<List<BreakageEntity>> = repository.allBreakages
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val stockMovements: StateFlow<List<StockMovementEntity>> = repository.allMovements
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val appSettings: StateFlow<AppSettingsEntity> = repository.appSettings
        .map { it ?: AppSettingsEntity() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppSettingsEntity())

    // Feedback messages (snackbars / dialogs)
    private val _userMessage = MutableSharedFlow<String>()
    val userMessage = _userMessage.asSharedFlow()

    private val _errorMessage = MutableSharedFlow<String>()
    val errorMessage = _errorMessage.asSharedFlow()

    fun showMessage(msg: String) {
        viewModelScope.launch { _userMessage.emit(msg) }
    }

    fun showError(err: String) {
        viewModelScope.launch { _errorMessage.emit(err) }
    }

    fun formatRupiah(amount: Double): String {
        val format = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
        return format.format(amount)
    }

    fun getTodayString(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    }

    // --- AUTHENTICATION & ROLES ---
    fun login(userId: String, pass: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            val result = repository.authenticate(userId, pass)
            result.onSuccess { user ->
                _currentUser.value = user
                showMessage("Selamat datang, ${user.fullName} (${user.role})")
                onSuccess()
            }.onFailure {
                showError(it.message ?: "Login gagal.")
            }
        }
    }

    fun logout() {
        _currentUser.value = null
        showMessage("Anda telah logout.")
    }

    fun isRootAdmin(): Boolean {
        return _currentUser.value?.role == "ROOT_ADMIN"
    }

    fun isAdminOrAbove(): Boolean {
        val r = _currentUser.value?.role
        return r == "ROOT_ADMIN" || r == "ADMIN"
    }

    // --- USER MANAGEMENT (ROOT ADMIN ONLY) ---
    fun saveUser(user: UserEntity, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            if (!isRootAdmin()) {
                showError("Hanya Root Admin yang memiliki otorisasi mendaftarkan/mengubah user!")
                return@launch
            }
            val result = repository.saveUser(user)
            result.onSuccess {
                showMessage("User '${user.userId}' berhasil disimpan.")
                onSuccess()
            }.onFailure {
                showError(it.message ?: "Gagal menyimpan user.")
            }
        }
    }

    fun deleteUser(user: UserEntity) {
        viewModelScope.launch {
            if (!isRootAdmin()) {
                showError("Hanya Root Admin yang dapat menghapus user!")
                return@launch
            }
            val result = repository.deleteUser(user)
            result.onSuccess {
                showMessage("User '${user.userId}' telah dihapus.")
            }.onFailure {
                showError(it.message ?: "Gagal menghapus user.")
            }
        }
    }

    // --- PRODUCT ACTIONS ---
    fun getNextItemCode(onResult: (String) -> Unit) {
        viewModelScope.launch {
            val code = repository.generateNextItemCode()
            onResult(code)
        }
    }

    fun saveProduct(
        product: ProductEntity,
        onSuccess: () -> Unit = {}
    ) {
        viewModelScope.launch {
            val result = repository.saveProduct(product)
            result.onSuccess {
                showMessage("Produk '${product.itemName}' (${product.itemCode}) berhasil disimpan.")
                onSuccess()
            }.onFailure {
                showError(it.message ?: "Gagal menyimpan produk.")
            }
        }
    }

    fun updatePromoPrice(
        itemCode: String,
        promoPrice: Double,
        isPromoActive: Boolean,
        onSuccess: () -> Unit = {}
    ) {
        viewModelScope.launch {
            val result = repository.updatePromoPrice(itemCode, promoPrice, isPromoActive)
            result.onSuccess {
                val statusText = if (isPromoActive) "diaktifkan (${formatRupiah(promoPrice)})" else "dinonaktifkan"
                showMessage("Harga promo produk $itemCode berhasil $statusText.")
                onSuccess()
            }.onFailure {
                showError(it.message ?: "Gagal mengupdate harga promo.")
            }
        }
    }

    fun deleteProduct(product: ProductEntity) {
        viewModelScope.launch {
            if (!isAdminOrAbove()) {
                showError("Hanya Admin yang berwenang menghapus produk!")
                return@launch
            }
            repository.deleteProduct(product)
            showMessage("Produk '${product.itemName}' telah dihapus.")
        }
    }

    // --- CUSTOMER ACTIONS ---
    fun saveCustomer(
        customer: CustomerEntity,
        onSuccess: () -> Unit = {}
    ) {
        viewModelScope.launch {
            val result = repository.saveCustomer(customer)
            result.onSuccess {
                showMessage("Customer '${customer.customerName}' berhasil disimpan.")
                onSuccess()
            }.onFailure {
                showError(it.message ?: "Gagal menyimpan customer.")
            }
        }
    }

    fun deleteCustomer(customer: CustomerEntity) {
        viewModelScope.launch {
            repository.deleteCustomer(customer)
            showMessage("Customer '${customer.customerName}' telah dihapus.")
        }
    }

    // --- EXPEDITIONS ACTIONS ---
    fun saveShippingService(
        service: ShippingServiceEntity,
        onSuccess: () -> Unit = {}
    ) {
        viewModelScope.launch {
            val result = repository.saveShippingService(service)
            result.onSuccess {
                showMessage("Ekspedisi '${service.name}' berhasil disimpan.")
                onSuccess()
            }.onFailure {
                showError(it.message ?: "Gagal menyimpan ekspedisi.")
            }
        }
    }

    fun toggleShippingServiceStatus(service: ShippingServiceEntity) {
        viewModelScope.launch {
            repository.saveShippingService(service.copy(isActive = !service.isActive))
            showMessage("Status ekspedisi '${service.name}' diperbarui.")
        }
    }

    // --- PURCHASE ORDER ACTIONS ---
    fun createPurchaseOrder(
        supplierName: String,
        notes: String,
        items: List<PurchaseOrderItemEntity>,
        onSuccess: () -> Unit = {}
    ) {
        viewModelScope.launch {
            val poNumber = repository.generateTransactionNumber("PO")
            val po = PurchaseOrderEntity(
                poNumber = poNumber,
                poDate = getTodayString(),
                supplierName = supplierName,
                notes = notes,
                status = "PENDING"
            )
            val finalItems = items.map { it.copy(poNumber = poNumber) }
            val result = repository.createPurchaseOrder(po, finalItems)
            result.onSuccess {
                showMessage("Purchase Order $poNumber berhasil dibuat.")
                onSuccess()
            }.onFailure {
                showError(it.message ?: "Gagal membuat Purchase Order.")
            }
        }
    }

    // --- RECEIVING ACTIONS ---
    fun processReceiving(
        poNumber: String,
        notes: String,
        items: List<ReceivingItemEntity>,
        onSuccess: () -> Unit = {}
    ) {
        viewModelScope.launch {
            val rcvNumber = repository.generateTransactionNumber("RCV")
            val currentUserName = _currentUser.value?.fullName ?: appSettings.value.currentUser
            val rcv = ReceivingEntity(
                rcvNumber = rcvNumber,
                poNumber = poNumber,
                rcvDate = getTodayString(),
                notes = notes,
                user = currentUserName
            )
            val finalItems = items.map { it.copy(rcvNumber = rcvNumber) }
            val result = repository.processReceiving(rcv, finalItems)
            result.onSuccess {
                showMessage("Receiving $rcvNumber berhasil! Stock produk telah ditambahkan.")
                onSuccess()
            }.onFailure {
                showError(it.message ?: "Gagal memproses penerimaan.")
            }
        }
    }

    // --- SALES ORDER ACTIONS ---
    // RULE SO: When created, status is CONFIRMED and CANNOT make DO yet!
    fun createSalesOrder(
        customer: CustomerEntity,
        items: List<SalesOrderItemEntity>,
        notes: String,
        onSuccess: () -> Unit = {}
    ) {
        viewModelScope.launch {
            val soNumber = repository.generateTransactionNumber("SO")
            val totalAmount = items.sumOf { it.subtotal }
            val so = SalesOrderEntity(
                soNumber = soNumber,
                soDate = getTodayString(),
                customerName = customer.customerName,
                customerPhone = customer.phoneNumber,
                customerAddress = customer.address,
                status = "CONFIRMED", // NEW RULE: starts at CONFIRMED
                totalAmount = totalAmount,
                notes = notes
            )
            val finalItems = items.map { it.copy(soNumber = soNumber) }
            val result = repository.createSalesOrder(so, finalItems)
            result.onSuccess {
                showMessage("Sales Order $soNumber berhasil dibuat (Status: Confirmed).")
                onSuccess()
            }.onFailure {
                showError(it.message ?: "Gagal membuat Sales Order.")
            }
        }
    }

    // RULE SO: Update to READY_TO_DELIVER with option to add/reduce items
    fun updateSOToReadyToDeliver(
        soNumber: String,
        updatedItems: List<SalesOrderItemEntity>,
        onSuccess: () -> Unit = {}
    ) {
        viewModelScope.launch {
            val result = repository.updateSOToReadyToDeliver(soNumber, updatedItems)
            result.onSuccess {
                showMessage("Status SO $soNumber diubah ke 'Ready to Deliver'. DO sekarang dapat diterbitkan.")
                onSuccess()
            }.onFailure {
                showError(it.message ?: "Gagal memperbarui status SO.")
            }
        }
    }

    // --- DELIVERY ORDER ACTIONS ---
    // RULE DO: Only possible if SO is READY_TO_DELIVER
    fun createDeliveryOrder(
        so: SalesOrderEntity,
        soItems: List<SalesOrderItemEntity>,
        shippingExpedition: String,
        onSuccess: (DeliveryOrderEntity) -> Unit = {}
    ) {
        viewModelScope.launch {
            if (so.status != "READY_TO_DELIVER") {
                showError("Gagal: Status SO harus 'Ready to Deliver' terlebih dahulu untuk membuat DO!")
                return@launch
            }

            val doNumber = repository.generateTransactionNumber("DO")
            val deliveryOrder = DeliveryOrderEntity(
                doNumber = doNumber,
                soNumber = so.soNumber,
                doDate = getTodayString(),
                customerName = so.customerName,
                customerPhone = so.customerPhone,
                customerAddress = so.customerAddress,
                shippingExpedition = shippingExpedition,
                status = "CONFIRMED",
                isDeducted = true
            )
            val doItems = soItems.map {
                DeliveryOrderItemEntity(
                    doNumber = doNumber,
                    itemCode = it.itemCode,
                    itemName = it.itemName,
                    qty = it.qty,
                    unitPrice = it.unitPrice,
                    subtotal = it.subtotal
                )
            }

            val result = repository.processDeliveryOrder(deliveryOrder, doItems)
            result.onSuccess {
                showMessage("Delivery Order $doNumber berhasil diproses! Stock telah dipotong.")
                onSuccess(deliveryOrder)
            }.onFailure {
                showError(it.message ?: "Gagal memproses Delivery Order.")
            }
        }
    }

    // --- STOCK ADJUSTMENT ACTIONS ---
    fun processStockAdjustment(
        product: ProductEntity,
        adjustmentType: String,
        adjustmentQty: Int,
        reason: String,
        notes: String,
        onSuccess: () -> Unit = {}
    ) {
        viewModelScope.launch {
            val adjNumber = repository.generateTransactionNumber("ADJ")
            val currentUserVal = _currentUser.value?.fullName ?: appSettings.value.currentUser
            val adj = StockAdjustmentEntity(
                adjNumber = adjNumber,
                date = getTodayString(),
                itemCode = product.itemCode,
                itemName = product.itemName,
                previousStock = product.stock,
                adjustmentQty = adjustmentQty,
                adjustmentType = adjustmentType,
                newStock = if (adjustmentType == "INCREASE") product.stock + adjustmentQty else product.stock - adjustmentQty,
                reason = reason,
                notes = notes,
                user = currentUserVal
            )
            val result = repository.processStockAdjustment(adj)
            result.onSuccess {
                showMessage("Adjustment $adjNumber berhasil! Stock produk telah diperbarui.")
                onSuccess()
            }.onFailure {
                showError(it.message ?: "Gagal memproses adjustment.")
            }
        }
    }

    // --- RETURN ACTIONS ---
    fun processReturn(
        product: ProductEntity,
        quantity: Int,
        returnType: String,
        reason: String,
        notes: String,
        onSuccess: () -> Unit = {}
    ) {
        viewModelScope.launch {
            val retNumber = repository.generateTransactionNumber("RET")
            val currentUserVal = _currentUser.value?.fullName ?: appSettings.value.currentUser
            val ret = ReturnEntity(
                retNumber = retNumber,
                date = getTodayString(),
                itemCode = product.itemCode,
                itemName = product.itemName,
                quantity = quantity,
                returnType = returnType,
                reason = reason,
                notes = notes,
                user = currentUserVal
            )
            val result = repository.processReturn(ret)
            result.onSuccess {
                val stockNote = if (returnType == "RETURN_TO_STOCK") " (Stock bertambah +$quantity)" else " (Non-stock)"
                showMessage("Return $retNumber berhasil dicatat$stockNote.")
                onSuccess()
            }.onFailure {
                showError(it.message ?: "Gagal memproses return.")
            }
        }
    }

    // --- BREAKAGE ACTIONS ---
    fun processBreakage(
        product: ProductEntity,
        quantity: Int,
        reason: String,
        notes: String,
        onSuccess: () -> Unit = {}
    ) {
        viewModelScope.launch {
            val brkNumber = repository.generateTransactionNumber("BRK")
            val currentUserVal = _currentUser.value?.fullName ?: appSettings.value.currentUser
            val brk = BreakageEntity(
                brkNumber = brkNumber,
                date = getTodayString(),
                itemCode = product.itemCode,
                itemName = product.itemName,
                quantity = quantity,
                reason = reason,
                notes = notes,
                user = currentUserVal
            )
            val result = repository.processBreakage(brk)
            result.onSuccess {
                showMessage("Breakage $brkNumber berhasil dicatat (Stock berkurang -$quantity).")
                onSuccess()
            }.onFailure {
                showError(it.message ?: "Gagal memproses barang rusak.")
            }
        }
    }

    // --- SETTINGS ACTIONS ---
    fun updateSettings(settings: AppSettingsEntity) {
        viewModelScope.launch {
            repository.saveSettings(settings)
            showMessage("Pengaturan berhasil disimpan.")
        }
    }

    // --- PRINTING HELPER ---
    fun printShippingLabel(context: Context, labelData: ShippingLabelData) {
        ThermalLabelPrinter.printLabel(context, labelData)
    }

    // --- EXCEL EXPORT METHODS WITH MARGIN ANALYSIS ---
    fun exportStockReport(
        context: Context,
        familyFilter: String = "All",
        stockCondition: String = "All",
        searchQuery: String = ""
    ) {
        viewModelScope.launch {
            try {
                val products = repository.getAllProductsSync().filter { p ->
                    val matchFamily = familyFilter == "All" || p.familyCode.equals(familyFilter, ignoreCase = true)
                    val matchQuery = searchQuery.isBlank() ||
                            p.itemName.contains(searchQuery, true) ||
                            p.itemCode.contains(searchQuery, true) ||
                            p.familyCode.contains(searchQuery, true)
                    val matchCondition = when (stockCondition) {
                        "Stock > 0" -> p.stock > 0
                        "Stock = 0" -> p.stock == 0
                        "Stock < 0" -> p.stock < 0
                        else -> true
                    }
                    matchFamily && matchQuery && matchCondition
                }

                val headers = listOf(
                    "Family Code", "Item Code", "Item Name",
                    "Last Receiving", "Current Stock",
                    "Purchasing Price (HPP)", "Net Price (Jual)", "Promo Price", "Status Promo"
                )
                val rows = products.map {
                    listOf(
                        it.familyCode,
                        it.itemCode,
                        it.itemName,
                        it.lastReceiving,
                        it.stock,
                        it.purchasingPrice,
                        it.netPrice,
                        it.promoPrice,
                        if (it.isPromoActive) "Aktif" else "Nonaktif"
                    )
                }

                val fileName = "Stock_Report_${getTodayString()}.xlsx"
                val file = ExcelExporter.createXlsxFile(context, fileName, "Stock Report", headers, rows)
                ExcelExporter.shareFile(context, file, "Stock Report Atin's Collection")
                showMessage("Stock Report berhasil diekspor ke $fileName")
            } catch (e: Exception) {
                showError("Gagal export Stock Report: ${e.message}")
            }
        }
    }

    fun exportSalesDailyReport(
        context: Context,
        dateFrom: String = "",
        dateTo: String = ""
    ) {
        viewModelScope.launch {
            try {
                val deliveryOrders = repository.getAllDeliveryOrdersSync()
                val doItems = repository.getAllDOItemsSync()
                val productsMap = repository.getAllProductsSync().associateBy { it.itemCode }

                val doMap = deliveryOrders.associateBy { it.doNumber }
                val rows = mutableListOf<List<Any>>()

                for (item in doItems) {
                    val parentDO = doMap[item.doNumber] ?: continue
                    val date = parentDO.doDate
                    if (dateFrom.isNotBlank() && date < dateFrom) continue
                    if (dateTo.isNotBlank() && date > dateTo) continue

                    val prod = productsMap[item.itemCode]
                    val hpp = (prod?.purchasingPrice ?: 0.0) * item.qty
                    val grossMargin = item.subtotal - hpp
                    val marginPct = if (item.subtotal > 0) (grossMargin / item.subtotal) * 100 else 0.0

                    rows.add(
                        listOf(
                            date,
                            item.itemCode,
                            item.itemName,
                            item.qty,
                            hpp,
                            item.subtotal,
                            grossMargin,
                            String.format(Locale.US, "%.1f%%", marginPct)
                        )
                    )
                }

                val headers = listOf("Date", "Item Code", "Item Name", "Qty Sold", "Total HPP (Rp)", "Sales Amount (Rp)", "Gross Margin (Rp)", "Margin %")
                val fileName = "Sales_By_Item_Daily_${getTodayString()}.xlsx"
                val file = ExcelExporter.createXlsxFile(context, fileName, "Sales Daily", headers, rows)
                ExcelExporter.shareFile(context, file, "Sales Daily Report")
                showMessage("Laporan Sales Daily berhasil diekspor ke $fileName")
            } catch (e: Exception) {
                showError("Gagal export Sales Daily: ${e.message}")
            }
        }
    }

    fun exportSalesSummaryReport(
        context: Context,
        dateFrom: String = "",
        dateTo: String = ""
    ) {
        viewModelScope.launch {
            try {
                val deliveryOrders = repository.getAllDeliveryOrdersSync()
                val doItems = repository.getAllDOItemsSync()
                val productsMap = repository.getAllProductsSync().associateBy { it.itemCode }
                val doMap = deliveryOrders.associateBy { it.doNumber }

                val summaryMap = mutableMapOf<String, Triple<String, Int, Double>>()

                for (item in doItems) {
                    val parentDO = doMap[item.doNumber] ?: continue
                    val date = parentDO.doDate
                    if (dateFrom.isNotBlank() && date < dateFrom) continue
                    if (dateTo.isNotBlank() && date > dateTo) continue

                    val current = summaryMap[item.itemCode]
                    if (current == null) {
                        summaryMap[item.itemCode] = Triple(item.itemName, item.qty, item.subtotal)
                    } else {
                        summaryMap[item.itemCode] = Triple(
                            current.first,
                            current.second + item.qty,
                            current.third + item.subtotal
                        )
                    }
                }

                val rows = summaryMap.map { (code, data) ->
                    val prod = productsMap[code]
                    val unitHpp = prod?.purchasingPrice ?: 0.0
                    val totalHpp = unitHpp * data.second
                    val grossMargin = data.third - totalHpp
                    val avgPrice = if (data.second > 0) data.third / data.second else 0.0
                    listOf(
                        code,
                        data.first,
                        data.second,
                        totalHpp,
                        data.third,
                        grossMargin,
                        avgPrice
                    )
                }

                val headers = listOf("Item Code", "Item Name", "Total Qty Sold", "Total HPP (Rp)", "Total Sales (Rp)", "Gross Margin (Rp)", "Avg Selling Price (Rp)")
                val fileName = "Sales_By_Item_Summary_${getTodayString()}.xlsx"
                val file = ExcelExporter.createXlsxFile(context, fileName, "Sales Summary", headers, rows)
                ExcelExporter.shareFile(context, file, "Sales Summary Report")
                showMessage("Laporan Sales Summary berhasil diekspor ke $fileName")
            } catch (e: Exception) {
                showError("Gagal export Sales Summary: ${e.message}")
            }
        }
    }

    fun exportAdjustmentReport(context: Context) {
        viewModelScope.launch {
            try {
                val adjs = repository.getAllAdjustmentsSync()
                val headers = listOf(
                    "Date", "Adjustment ID", "Item Code", "Item Name",
                    "Previous Stock", "Adjustment Qty", "Adjustment Type",
                    "New Stock", "Reason", "Notes", "User"
                )
                val rows = adjs.map {
                    listOf(
                        it.date,
                        it.adjNumber,
                        it.itemCode,
                        it.itemName,
                        it.previousStock,
                        it.adjustmentQty,
                        it.adjustmentType,
                        it.newStock,
                        it.reason,
                        it.notes,
                        it.user
                    )
                }
                val fileName = "Adjustment_Report_${getTodayString()}.xlsx"
                val file = ExcelExporter.createXlsxFile(context, fileName, "Adjustments", headers, rows)
                ExcelExporter.shareFile(context, file, "Adjustment Report")
                showMessage("Adjustment Report berhasil diekspor ke $fileName")
            } catch (e: Exception) {
                showError("Gagal export Adjustment Report: ${e.message}")
            }
        }
    }

    fun exportReceivingReport(context: Context) {
        viewModelScope.launch {
            try {
                val rcvs = repository.getAllReceivingsSync()
                val rcvItems = repository.getAllReceivingItemsSync()
                val rcvMap = rcvs.associateBy { it.rcvNumber }

                val headers = listOf("Receiving Number", "PO Number", "Date", "Item Code", "Item Name", "Qty Received", "User")
                val rows = rcvItems.map { item ->
                    val rcv = rcvMap[item.rcvNumber]
                    listOf(
                        item.rcvNumber,
                        rcv?.poNumber ?: "-",
                        rcv?.rcvDate ?: "-",
                        item.itemCode,
                        item.itemName,
                        item.qtyReceived,
                        rcv?.user ?: "Admin"
                    )
                }
                val fileName = "Receiving_Report_${getTodayString()}.xlsx"
                val file = ExcelExporter.createXlsxFile(context, fileName, "Receiving", headers, rows)
                ExcelExporter.shareFile(context, file, "Receiving Report")
                showMessage("Receiving Report berhasil diekspor ke $fileName")
            } catch (e: Exception) {
                showError("Gagal export Receiving Report: ${e.message}")
            }
        }
    }

    fun exportReturnBreakageReport(context: Context) {
        viewModelScope.launch {
            try {
                val returns = repository.getAllReturnsSync()
                val breakages = repository.getAllBreakagesSync()

                val headers = listOf("Date", "Transaction Number", "Type", "Item Code", "Item Name", "Quantity", "Reason", "Notes")
                val rows = mutableListOf<List<Any>>()

                returns.forEach {
                    rows.add(
                        listOf(
                            it.date,
                            it.retNumber,
                            "RETURN (${it.returnType})",
                            it.itemCode,
                            it.itemName,
                            it.quantity,
                            it.reason,
                            it.notes
                        )
                    )
                }

                breakages.forEach {
                    rows.add(
                        listOf(
                            it.date,
                            it.brkNumber,
                            "BREAKAGE",
                            it.itemCode,
                            it.itemName,
                            it.quantity,
                            it.reason,
                            it.notes
                        )
                    )
                }

                val fileName = "Return_Breakage_Report_${getTodayString()}.xlsx"
                val file = ExcelExporter.createXlsxFile(context, fileName, "Return & Breakage", headers, rows)
                ExcelExporter.shareFile(context, file, "Return & Breakage Report")
                showMessage("Return & Breakage Report berhasil diekspor ke $fileName")
            } catch (e: Exception) {
                showError("Gagal export Return/Breakage Report: ${e.message}")
            }
        }
    }

    // --- FIREBASE SYNC ACTION ---
    fun syncAllToFirebase(onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            showMessage("Memulai sinkronisasi data ke Firebase Cloud Firestore...")
            val result = repository.syncAllLocalDataToCloud()
            result.onSuccess { count ->
                showMessage("Berhasil sinkronisasi $count data ke Firestore Cloud!")
                onComplete()
            }.onFailure {
                showError("Gagal sinkronisasi ke Firebase: ${it.localizedMessage ?: "Periksa koneksi internet / database"}")
                onComplete()
            }
        }
    }
}
