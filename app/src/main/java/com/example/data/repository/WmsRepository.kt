package com.example.data.repository

import androidx.room.withTransaction
import com.example.data.database.AppDatabase
import com.example.data.entity.*
import com.example.data.firebase.FirestoreSyncService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.atomic.AtomicInteger

class WmsRepository(private val database: AppDatabase) {

    private val userDao = database.userDao()
    private val productDao = database.productDao()
    private val customerDao = database.customerDao()
    private val shippingDao = database.shippingServiceDao()
    private val poDao = database.purchaseOrderDao()
    private val rcvDao = database.receivingDao()
    private val soDao = database.salesOrderDao()
    private val doDao = database.deliveryOrderDao()
    private val adjDao = database.stockAdjustmentDao()
    private val retDao = database.returnDao()
    private val brkDao = database.breakageDao()
    private val movementDao = database.stockMovementDao()
    private val settingsDao = database.appSettingsDao()

    val firestoreSync = FirestoreSyncService(database)

    private val counter = AtomicInteger((System.currentTimeMillis() % 9000).toInt() + 1000)

    private fun getTodayDateString(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    }

    private fun getCompactDateString(): String {
        return SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date())
    }

    fun generateTransactionNumber(prefix: String): String {
        val seq = counter.incrementAndGet() % 9000 + 1000
        return "$prefix-${getCompactDateString()}-$seq"
    }

    // --- USERS & AUTHENTICATION ---
    val allUsers: Flow<List<UserEntity>> = userDao.getAllUsers()

    suspend fun authenticate(userId: String, pass: String): Result<UserEntity> = withContext(Dispatchers.IO) {
        try {
            val user = userDao.authenticate(userId.trim(), pass.trim())
            if (user != null) {
                Result.success(user)
            } else {
                Result.failure(Exception("ID Pengguna atau Password salah!"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun saveUser(user: UserEntity): Result<Long> = withContext(Dispatchers.IO) {
        try {
            if (user.userId.isBlank()) {
                return@withContext Result.failure(Exception("User ID tidak boleh kosong."))
            }
            if (user.password.isBlank()) {
                return@withContext Result.failure(Exception("Password tidak boleh kosong."))
            }
            val existing = userDao.getUserByUserId(user.userId)
            if (user.id == 0L && existing != null) {
                return@withContext Result.failure(Exception("User ID '${user.userId}' sudah terdaftar."))
            }

            val id = if (user.id == 0L) {
                userDao.insert(user)
            } else {
                userDao.update(user)
                user.id
            }
            firestoreSync.syncUserToCloud(user)
            Result.success(id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteUser(user: UserEntity): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (user.userId.equals("admin", ignoreCase = true)) {
                return@withContext Result.failure(Exception("Root Admin tidak dapat dihapus!"))
            }
            userDao.delete(user)
            firestoreSync.deleteUserFromCloud(user.userId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // --- AUTOMATIC ITEM CODE GENERATION (SKU-001, SKU-002, ...) ---
    suspend fun generateNextItemCode(): String = withContext(Dispatchers.IO) {
        val count = productDao.count()
        var nextNum = count + 1
        var candidate = String.format(Locale.US, "SKU-%03d", nextNum)
        while (productDao.getProductByItemCode(candidate) != null) {
            nextNum++
            candidate = String.format(Locale.US, "SKU-%03d", nextNum)
        }
        candidate
    }

    // --- PRODUCTS ---
    val allProducts: Flow<List<ProductEntity>> = productDao.getAllProducts()
    val activeProducts: Flow<List<ProductEntity>> = productDao.getActiveProducts()

    fun searchProducts(query: String): Flow<List<ProductEntity>> = productDao.searchProducts(query)

    suspend fun getProductByItemCode(itemCode: String): ProductEntity? = withContext(Dispatchers.IO) {
        productDao.getProductByItemCode(itemCode)
    }

    suspend fun saveProduct(product: ProductEntity): Result<Long> = withContext(Dispatchers.IO) {
        try {
            val itemCode = if (product.itemCode.isBlank()) {
                generateNextItemCode()
            } else {
                product.itemCode
            }
            val finalProduct = product.copy(itemCode = itemCode)

            val existing = productDao.getProductByItemCode(finalProduct.itemCode)
            if (finalProduct.id == 0L && existing != null) {
                return@withContext Result.failure(Exception("Item Code '${finalProduct.itemCode}' sudah digunakan."))
            }

            val savedId = if (finalProduct.id == 0L) {
                val id = productDao.insert(finalProduct)
                if (finalProduct.stock > 0) {
                    val movId = generateTransactionNumber("MOV")
                    val mov = StockMovementEntity(
                        movementId = movId,
                        date = getTodayDateString(),
                        itemCode = finalProduct.itemCode,
                        itemName = finalProduct.itemName,
                        type = "RECEIVING",
                        referenceId = "INIT-STOCK",
                        qtyIn = finalProduct.stock,
                        qtyOut = 0,
                        previousStock = 0,
                        newStock = finalProduct.stock,
                        reason = "Stock Awal Produk",
                        user = "Admin"
                    )
                    movementDao.insertMovement(mov)
                    firestoreSync.syncMovementToCloud(mov)
                }
                id
            } else {
                productDao.update(finalProduct.copy(updatedAt = System.currentTimeMillis()))
                finalProduct.id
            }

            firestoreSync.syncProductToCloud(finalProduct)
            Result.success(savedId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updatePromoPrice(
        itemCode: String,
        promoPrice: Double,
        isPromoActive: Boolean
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val now = System.currentTimeMillis()
            productDao.updatePromoPrice(itemCode, promoPrice, isPromoActive, now)
            val updated = productDao.getProductByItemCode(itemCode)
            if (updated != null) {
                firestoreSync.syncProductToCloud(updated)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteProduct(product: ProductEntity) = withContext(Dispatchers.IO) {
        productDao.delete(product)
        firestoreSync.deleteProductFromCloud(product.itemCode)
    }

    // --- CUSTOMERS ---
    val allCustomers: Flow<List<CustomerEntity>> = customerDao.getAllCustomers()

    fun searchCustomers(query: String): Flow<List<CustomerEntity>> = customerDao.searchCustomers(query)

    suspend fun saveCustomer(customer: CustomerEntity): Result<Long> = withContext(Dispatchers.IO) {
        try {
            if (customer.customerName.isBlank()) {
                return@withContext Result.failure(Exception("Nama customer wajib diisi."))
            }
            val id = if (customer.id == 0L) {
                customerDao.insert(customer)
            } else {
                customerDao.update(customer)
                customer.id
            }
            firestoreSync.syncCustomerToCloud(customer.copy(id = id))
            Result.success(id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteCustomer(customer: CustomerEntity) = withContext(Dispatchers.IO) {
        customerDao.delete(customer)
        firestoreSync.deleteCustomerFromCloud(customer)
    }

    // --- EXPEDITIONS ---
    val allShippingServices: Flow<List<ShippingServiceEntity>> = shippingDao.getAllShippingServices()
    val activeShippingServices: Flow<List<ShippingServiceEntity>> = shippingDao.getActiveShippingServices()

    suspend fun saveShippingService(service: ShippingServiceEntity): Result<Long> = withContext(Dispatchers.IO) {
        try {
            if (service.name.isBlank()) {
                return@withContext Result.failure(Exception("Nama ekspedisi wajib diisi."))
            }
            val id = if (service.id == 0L) {
                shippingDao.insert(service)
            } else {
                shippingDao.update(service)
                service.id
            }
            Result.success(id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteShippingService(service: ShippingServiceEntity) = withContext(Dispatchers.IO) {
        shippingDao.delete(service)
    }

    // --- PURCHASE ORDERS ---
    val allPurchaseOrders: Flow<List<PurchaseOrderEntity>> = poDao.getAllPurchaseOrders()
    val pendingPurchaseOrders: Flow<List<PurchaseOrderEntity>> = poDao.getPendingPurchaseOrders()

    fun getItemsForPO(poNumber: String): Flow<List<PurchaseOrderItemEntity>> = poDao.getItemsForPO(poNumber)

    suspend fun getItemsForPOSync(poNumber: String): List<PurchaseOrderItemEntity> = withContext(Dispatchers.IO) {
        poDao.getItemsForPOSync(poNumber)
    }

    suspend fun createPurchaseOrder(
        po: PurchaseOrderEntity,
        items: List<PurchaseOrderItemEntity>
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            if (items.isEmpty()) {
                return@withContext Result.failure(Exception("PO harus memiliki minimal 1 item produk."))
            }
            database.withTransaction {
                poDao.insertPO(po)
                poDao.insertPOItems(items)
            }
            Result.success(po.poNumber)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun cancelPurchaseOrder(poNumber: String) = withContext(Dispatchers.IO) {
        poDao.updatePOStatus(poNumber, "CANCELLED")
    }

    // --- RECEIVING ---
    val allReceivings: Flow<List<ReceivingEntity>> = rcvDao.getAllReceivings()

    fun getItemsForReceiving(rcvNumber: String): Flow<List<ReceivingItemEntity>> = rcvDao.getItemsForReceiving(rcvNumber)

    suspend fun processReceiving(
        rcv: ReceivingEntity,
        items: List<ReceivingItemEntity>
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            if (items.isEmpty()) {
                return@withContext Result.failure(Exception("Receiving harus memiliki minimal 1 item."))
            }

            val poItems = poDao.getItemsForPOSync(rcv.poNumber)
            val poItemCodes = poItems.map { it.itemCode }.toSet()

            for (item in items) {
                if (!poItemCodes.contains(item.itemCode)) {
                    return@withContext Result.failure(
                        Exception("Item ${item.itemName} (${item.itemCode}) tidak terdapat pada Purchase Order ini.")
                    )
                }
                if (item.qtyReceived <= 0) {
                    return@withContext Result.failure(
                        Exception("Quantity received untuk ${item.itemName} harus lebih dari 0.")
                    )
                }
            }

            database.withTransaction {
                rcvDao.insertReceiving(rcv)
                rcvDao.insertReceivingItems(items)

                val now = System.currentTimeMillis()

                for (item in items) {
                    val currentProd = productDao.getProductByItemCode(item.itemCode)
                    val prevStock = currentProd?.stock ?: 0
                    val newStock = prevStock + item.qtyReceived

                    productDao.updateStockAndLastReceiving(
                        itemCode = item.itemCode,
                        newStock = newStock,
                        lastReceiving = rcv.rcvDate,
                        updatedAt = now
                    )

                    poDao.incrementPOItemReceivedQty(rcv.poNumber, item.itemCode, item.qtyReceived)

                    val movId = generateTransactionNumber("MOV")
                    val mov = StockMovementEntity(
                        movementId = movId,
                        date = rcv.rcvDate,
                        itemCode = item.itemCode,
                        itemName = item.itemName,
                        type = "RECEIVING",
                        referenceId = rcv.rcvNumber,
                        qtyIn = item.qtyReceived,
                        qtyOut = 0,
                        previousStock = prevStock,
                        newStock = newStock,
                        reason = "Penerimaan PO ${rcv.poNumber}",
                        user = rcv.user,
                        timestamp = now
                    )
                    movementDao.insertMovement(mov)
                    firestoreSync.syncMovementToCloud(mov)

                    currentProd?.let {
                        firestoreSync.syncProductToCloud(
                            it.copy(stock = newStock, lastReceiving = rcv.rcvDate, updatedAt = now)
                        )
                    }
                }

                val updatedPoItems = poDao.getItemsForPOSync(rcv.poNumber)
                val isAllFulfilled = updatedPoItems.all { it.qtyReceived >= it.qtyOrdered }
                if (isAllFulfilled) {
                    poDao.updatePOStatus(rcv.poNumber, "COMPLETED")
                }
            }

            Result.success(rcv.rcvNumber)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // --- SALES ORDERS ---
    val allSalesOrders: Flow<List<SalesOrderEntity>> = soDao.getAllSalesOrders()
    val readyToDeliverSalesOrders: Flow<List<SalesOrderEntity>> = soDao.getReadyToDeliverSalesOrders()
    val confirmedSalesOrders: Flow<List<SalesOrderEntity>> = soDao.getConfirmedSalesOrders()

    fun getItemsForSO(soNumber: String): Flow<List<SalesOrderItemEntity>> = soDao.getItemsForSO(soNumber)

    suspend fun getItemsForSOSync(soNumber: String): List<SalesOrderItemEntity> = withContext(Dispatchers.IO) {
        soDao.getItemsForSOSync(soNumber)
    }

    suspend fun createSalesOrder(
        so: SalesOrderEntity,
        items: List<SalesOrderItemEntity>
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            if (so.customerName.isBlank()) {
                return@withContext Result.failure(Exception("Customer wajib dipilih."))
            }
            if (items.isEmpty()) {
                return@withContext Result.failure(Exception("Sales Order harus memiliki minimal 1 item."))
            }
            for (item in items) {
                if (item.qty <= 0) {
                    return@withContext Result.failure(Exception("Quantity item ${item.itemName} harus > 0."))
                }
            }

            val finalSO = so.copy(status = "CONFIRMED")
            database.withTransaction {
                soDao.insertSO(finalSO)
                soDao.insertSOItems(items)
            }
            firestoreSync.syncSOToCloud(finalSO, items)
            Result.success(finalSO.soNumber)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // UPDATE SO STATUS TO READY TO DELIVER (WITH ITEM ADD/REDUCE OPTION)
    suspend fun updateSOToReadyToDeliver(
        soNumber: String,
        updatedItems: List<SalesOrderItemEntity>
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (updatedItems.isEmpty()) {
                return@withContext Result.failure(Exception("Item Sales Order tidak boleh kosong."))
            }
            for (item in updatedItems) {
                if (item.qty <= 0) {
                    return@withContext Result.failure(Exception("Quantity item ${item.itemName} harus > 0."))
                }
            }

            val newTotal = updatedItems.sumOf { it.subtotal }
            database.withTransaction {
                soDao.deleteSOItems(soNumber)
                soDao.insertSOItems(updatedItems)
                soDao.updateSOStatusAndTotal(soNumber, "READY_TO_DELIVER", newTotal)
            }

            val so = soDao.getSalesOrderByNumber(soNumber)
            if (so != null) {
                firestoreSync.syncSOToCloud(so, updatedItems)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // --- DELIVERY ORDERS ---
    val allDeliveryOrders: Flow<List<DeliveryOrderEntity>> = doDao.getAllDeliveryOrders()

    fun getItemsForDO(doNumber: String): Flow<List<DeliveryOrderItemEntity>> = doDao.getItemsForDO(doNumber)

    suspend fun getItemsForDOSync(doNumber: String): List<DeliveryOrderItemEntity> = withContext(Dispatchers.IO) {
        doDao.getItemsForDOSync(doNumber)
    }

    suspend fun getDeliveryOrderByNumber(doNumber: String): DeliveryOrderEntity? = withContext(Dispatchers.IO) {
        doDao.getDeliveryOrderByNumber(doNumber)
    }

    suspend fun processDeliveryOrder(
        deliveryOrder: DeliveryOrderEntity,
        items: List<DeliveryOrderItemEntity>
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val existingDO = doDao.getDeliveryOrderByNumber(deliveryOrder.doNumber)
            if (existingDO != null) {
                return@withContext Result.failure(Exception("Delivery Order ${deliveryOrder.doNumber} sudah pernah diproses!"))
            }

            if (items.isEmpty()) {
                return@withContext Result.failure(Exception("Delivery Order harus memiliki minimal 1 item."))
            }

            // CRITICAL VALIDATION: Check actual stock for each item before deducting!
            for (item in items) {
                val product = productDao.getProductByItemCode(item.itemCode)
                val availableStock = product?.stock ?: 0
                if (item.qty > availableStock) {
                    val shortage = item.qty - availableStock
                    return@withContext Result.failure(
                        Exception(
                            "Stock tidak mencukupi untuk '${item.itemName}' (${item.itemCode}).\n" +
                                    "Stock Tersedia: $availableStock\n" +
                                    "Jumlah Diminta: ${item.qty}\n" +
                                    "Kekurangan (Shortage): $shortage"
                        )
                    )
                }
            }

            database.withTransaction {
                doDao.insertDO(deliveryOrder)
                doDao.insertDOItems(items)

                val now = System.currentTimeMillis()

                for (item in items) {
                    val product = productDao.getProductByItemCode(item.itemCode)!!
                    val prevStock = product.stock
                    val newStock = prevStock - item.qty

                    productDao.updateStock(item.itemCode, newStock, now)

                    val movId = generateTransactionNumber("MOV")
                    val mov = StockMovementEntity(
                        movementId = movId,
                        date = deliveryOrder.doDate,
                        itemCode = item.itemCode,
                        itemName = item.itemName,
                        type = "DELIVERY",
                        referenceId = deliveryOrder.doNumber,
                        qtyIn = 0,
                        qtyOut = item.qty,
                        previousStock = prevStock,
                        newStock = newStock,
                        reason = "Pengiriman DO ${deliveryOrder.doNumber} (SO ${deliveryOrder.soNumber})",
                        user = "Admin",
                        timestamp = now
                    )
                    movementDao.insertMovement(mov)
                    firestoreSync.syncMovementToCloud(mov)

                    firestoreSync.syncProductToCloud(
                        product.copy(stock = newStock, updatedAt = now)
                    )
                }

                // Update SO status to FULFILLED
                soDao.updateSOStatus(deliveryOrder.soNumber, "FULFILLED")
            }

            firestoreSync.syncDOToCloud(deliveryOrder, items)
            Result.success(deliveryOrder.doNumber)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // --- STOCK ADJUSTMENT ---
    val allAdjustments: Flow<List<StockAdjustmentEntity>> = adjDao.getAllAdjustments()

    suspend fun processStockAdjustment(adj: StockAdjustmentEntity): Result<String> = withContext(Dispatchers.IO) {
        try {
            if (adj.adjustmentQty <= 0) {
                return@withContext Result.failure(Exception("Adjustment quantity harus lebih dari 0."))
            }
            if (adj.reason.isBlank()) {
                return@withContext Result.failure(Exception("Alasan adjustment wajib diisi."))
            }

            val product = productDao.getProductByItemCode(adj.itemCode)
                ?: return@withContext Result.failure(Exception("Produk tidak ditemukan."))

            val prevStock = product.stock
            val newStock = if (adj.adjustmentType == "INCREASE") {
                prevStock + adj.adjustmentQty
            } else {
                if (prevStock < adj.adjustmentQty) {
                    return@withContext Result.failure(Exception("Stock tidak mencukupi untuk pengurangan adjustment (Stock saat ini: $prevStock)."))
                }
                prevStock - adj.adjustmentQty
            }

            val finalAdj = adj.copy(previousStock = prevStock, newStock = newStock)

            database.withTransaction {
                adjDao.insertAdjustment(finalAdj)
                val now = System.currentTimeMillis()
                productDao.updateStock(adj.itemCode, newStock, now)

                val movId = generateTransactionNumber("MOV")
                val mov = StockMovementEntity(
                    movementId = movId,
                    date = adj.date,
                    itemCode = adj.itemCode,
                    itemName = adj.itemName,
                    type = "ADJUSTMENT",
                    referenceId = adj.adjNumber,
                    qtyIn = if (adj.adjustmentType == "INCREASE") adj.adjustmentQty else 0,
                    qtyOut = if (adj.adjustmentType == "DECREASE") adj.adjustmentQty else 0,
                    previousStock = prevStock,
                    newStock = newStock,
                    reason = "${adj.adjustmentType}: ${adj.reason}",
                    user = adj.user,
                    timestamp = now
                )
                movementDao.insertMovement(mov)
                firestoreSync.syncMovementToCloud(mov)

                firestoreSync.syncProductToCloud(
                    product.copy(stock = newStock, updatedAt = now)
                )
            }

            Result.success(adj.adjNumber)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // --- RETURN ---
    val allReturns: Flow<List<ReturnEntity>> = retDao.getAllReturns()

    suspend fun processReturn(ret: ReturnEntity): Result<String> = withContext(Dispatchers.IO) {
        try {
            if (ret.quantity <= 0) {
                return@withContext Result.failure(Exception("Quantity return harus lebih dari 0."))
            }
            if (ret.reason.isBlank()) {
                return@withContext Result.failure(Exception("Alasan return wajib diisi."))
            }

            val product = productDao.getProductByItemCode(ret.itemCode)
                ?: return@withContext Result.failure(Exception("Produk tidak ditemukan."))

            val prevStock = product.stock
            val isReturnToStock = ret.returnType == "RETURN_TO_STOCK"
            val newStock = if (isReturnToStock) prevStock + ret.quantity else prevStock

            database.withTransaction {
                retDao.insertReturn(ret)
                val now = System.currentTimeMillis()

                if (isReturnToStock) {
                    productDao.updateStock(ret.itemCode, newStock, now)

                    val movId = generateTransactionNumber("MOV")
                    val mov = StockMovementEntity(
                        movementId = movId,
                        date = ret.date,
                        itemCode = ret.itemCode,
                        itemName = ret.itemName,
                        type = "RETURN",
                        referenceId = ret.retNumber,
                        qtyIn = ret.quantity,
                        qtyOut = 0,
                        previousStock = prevStock,
                        newStock = newStock,
                        reason = "Return to stock: ${ret.reason}",
                        user = ret.user,
                        timestamp = now
                    )
                    movementDao.insertMovement(mov)
                    firestoreSync.syncMovementToCloud(mov)

                    firestoreSync.syncProductToCloud(
                        product.copy(stock = newStock, updatedAt = now)
                    )
                }
            }

            Result.success(ret.retNumber)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // --- BREAKAGE ---
    val allBreakages: Flow<List<BreakageEntity>> = brkDao.getAllBreakages()

    suspend fun processBreakage(brk: BreakageEntity): Result<String> = withContext(Dispatchers.IO) {
        try {
            if (brk.quantity <= 0) {
                return@withContext Result.failure(Exception("Quantity breakage harus lebih dari 0."))
            }
            if (brk.reason.isBlank()) {
                return@withContext Result.failure(Exception("Alasan barang rusak wajib diisi."))
            }

            val product = productDao.getProductByItemCode(brk.itemCode)
                ?: return@withContext Result.failure(Exception("Produk tidak ditemukan."))

            val prevStock = product.stock
            if (prevStock < brk.quantity) {
                return@withContext Result.failure(
                    Exception("Stock tidak cukup untuk breakage. Stock saat ini: $prevStock, Diminta: ${brk.quantity}")
                )
            }
            val newStock = prevStock - brk.quantity

            database.withTransaction {
                brkDao.insertBreakage(brk)
                val now = System.currentTimeMillis()
                productDao.updateStock(brk.itemCode, newStock, now)

                val movId = generateTransactionNumber("MOV")
                val mov = StockMovementEntity(
                    movementId = movId,
                    date = brk.date,
                    itemCode = brk.itemCode,
                    itemName = brk.itemName,
                    type = "BREAKAGE",
                    referenceId = brk.brkNumber,
                    qtyIn = 0,
                    qtyOut = brk.quantity,
                    previousStock = prevStock,
                    newStock = newStock,
                    reason = "Barang Rusak: ${brk.reason}",
                    user = brk.user,
                    timestamp = now
                )
                movementDao.insertMovement(mov)
                firestoreSync.syncMovementToCloud(mov)

                firestoreSync.syncProductToCloud(
                    product.copy(stock = newStock, updatedAt = now)
                )
            }

            Result.success(brk.brkNumber)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // --- STOCK MOVEMENTS ---
    val allMovements: Flow<List<StockMovementEntity>> = movementDao.getAllMovements()

    // --- APP SETTINGS ---
    val appSettings: Flow<AppSettingsEntity?> = settingsDao.getSettings()

    suspend fun saveSettings(settings: AppSettingsEntity) = withContext(Dispatchers.IO) {
        settingsDao.insertOrUpdate(settings)
    }

    suspend fun getSettingsSync(): AppSettingsEntity {
        return settingsDao.getSettingsSync() ?: AppSettingsEntity()
    }

    // --- SYNC GETTERS FOR EXCEL REPORTING ---
    suspend fun getAllProductsSync(): List<ProductEntity> = withContext(Dispatchers.IO) {
        productDao.getAllProductsSync()
    }

    suspend fun getAllDOItemsSync(): List<DeliveryOrderItemEntity> = withContext(Dispatchers.IO) {
        doDao.getAllDOItemsSync()
    }

    suspend fun getAllDeliveryOrdersSync(): List<DeliveryOrderEntity> = withContext(Dispatchers.IO) {
        doDao.getAllDeliveryOrdersSync()
    }

    suspend fun getAllAdjustmentsSync(): List<StockAdjustmentEntity> = withContext(Dispatchers.IO) {
        adjDao.getAllAdjustmentsSync()
    }

    suspend fun getAllReceivingItemsSync(): List<ReceivingItemEntity> = withContext(Dispatchers.IO) {
        rcvDao.getAllReceivingItemsSync()
    }

    suspend fun getAllReceivingsSync(): List<ReceivingEntity> = withContext(Dispatchers.IO) {
        rcvDao.getAllReceivingsSync()
    }

    suspend fun getAllReturnsSync(): List<ReturnEntity> = withContext(Dispatchers.IO) {
        retDao.getAllReturnsSync()
    }

    suspend fun getAllBreakagesSync(): List<BreakageEntity> = withContext(Dispatchers.IO) {
        brkDao.getAllBreakagesSync()
    }

    suspend fun syncAllLocalDataToCloud(): Result<Int> = withContext(Dispatchers.IO) {
        try {
            var count = 0
            val products = productDao.getAllProductsSync()
            for (p in products) {
                firestoreSync.syncProductToCloud(p)
                count++
            }
            val customers = customerDao.getAllCustomersSync()
            for (c in customers) {
                firestoreSync.syncCustomerToCloud(c)
                count++
            }
            val users = userDao.getAllUsersSync()
            for (u in users) {
                firestoreSync.syncUserToCloud(u)
                count++
            }
            Result.success(count)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
