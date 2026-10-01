package com.example.data.dao

import androidx.room.*
import com.example.data.entity.*
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Query("SELECT * FROM users ORDER BY id ASC")
    fun getAllUsers(): Flow<List<UserEntity>>

    @Query("SELECT * FROM users WHERE userId = :userId LIMIT 1")
    suspend fun getUserByUserId(userId: String): UserEntity?

    @Query("SELECT * FROM users WHERE userId = :userId AND password = :password AND isActive = 1 LIMIT 1")
    suspend fun authenticate(userId: String, password: String): UserEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(user: UserEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(users: List<UserEntity>)

    @Update
    suspend fun update(user: UserEntity)

    @Delete
    suspend fun delete(user: UserEntity)

    @Query("SELECT COUNT(*) FROM users")
    suspend fun count(): Int

    @Query("SELECT * FROM users")
    suspend fun getAllUsersSync(): List<UserEntity>
}

@Dao
interface ProductDao {
    @Query("SELECT * FROM products ORDER BY id DESC")
    fun getAllProducts(): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products WHERE isActive = 1 ORDER BY itemName ASC")
    fun getActiveProducts(): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products WHERE itemCode LIKE '%' || :query || '%' OR itemName LIKE '%' || :query || '%' OR familyCode LIKE '%' || :query || '%' ORDER BY itemName ASC")
    fun searchProducts(query: String): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products WHERE itemCode = :itemCode LIMIT 1")
    suspend fun getProductByItemCode(itemCode: String): ProductEntity?

    @Query("SELECT * FROM products WHERE id = :id LIMIT 1")
    suspend fun getProductById(id: Long): ProductEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(product: ProductEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(products: List<ProductEntity>)

    @Update
    suspend fun update(product: ProductEntity)

    @Query("UPDATE products SET stock = :newStock, lastReceiving = :lastReceiving, updatedAt = :updatedAt WHERE itemCode = :itemCode")
    suspend fun updateStockAndLastReceiving(itemCode: String, newStock: Int, lastReceiving: String, updatedAt: Long)

    @Query("UPDATE products SET stock = :newStock, updatedAt = :updatedAt WHERE itemCode = :itemCode")
    suspend fun updateStock(itemCode: String, newStock: Int, updatedAt: Long)

    @Query("UPDATE products SET promoPrice = :promoPrice, isPromoActive = :isPromoActive, updatedAt = :updatedAt WHERE itemCode = :itemCode")
    suspend fun updatePromoPrice(itemCode: String, promoPrice: Double, isPromoActive: Boolean, updatedAt: Long)

    @Delete
    suspend fun delete(product: ProductEntity)

    @Query("SELECT COUNT(*) FROM products")
    suspend fun count(): Int

    @Query("SELECT MAX(id) FROM products")
    suspend fun getMaxId(): Long?

    @Query("SELECT * FROM products")
    suspend fun getAllProductsSync(): List<ProductEntity>
}

@Dao
interface CustomerDao {
    @Query("SELECT * FROM customers ORDER BY customerName ASC")
    fun getAllCustomers(): Flow<List<CustomerEntity>>

    @Query("SELECT * FROM customers WHERE customerName LIKE '%' || :query || '%' OR phoneNumber LIKE '%' || :query || '%' ORDER BY customerName ASC")
    fun searchCustomers(query: String): Flow<List<CustomerEntity>>

    @Query("SELECT * FROM customers WHERE id = :id LIMIT 1")
    suspend fun getCustomerById(id: Long): CustomerEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(customer: CustomerEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(customers: List<CustomerEntity>)

    @Update
    suspend fun update(customer: CustomerEntity)

    @Delete
    suspend fun delete(customer: CustomerEntity)

    @Query("SELECT COUNT(*) FROM customers")
    suspend fun count(): Int

    @Query("SELECT * FROM customers")
    suspend fun getAllCustomersSync(): List<CustomerEntity>
}

@Dao
interface ShippingServiceDao {
    @Query("SELECT * FROM shipping_services ORDER BY name ASC")
    fun getAllShippingServices(): Flow<List<ShippingServiceEntity>>

    @Query("SELECT * FROM shipping_services WHERE isActive = 1 ORDER BY name ASC")
    fun getActiveShippingServices(): Flow<List<ShippingServiceEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(service: ShippingServiceEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(services: List<ShippingServiceEntity>)

    @Update
    suspend fun update(service: ShippingServiceEntity)

    @Delete
    suspend fun delete(service: ShippingServiceEntity)

    @Query("SELECT COUNT(*) FROM shipping_services")
    suspend fun count(): Int

    @Query("SELECT * FROM shipping_services")
    suspend fun getAllShippingServicesSync(): List<ShippingServiceEntity>
}

@Dao
interface PurchaseOrderDao {
    @Query("SELECT * FROM purchase_orders ORDER BY id DESC")
    fun getAllPurchaseOrders(): Flow<List<PurchaseOrderEntity>>

    @Query("SELECT * FROM purchase_orders WHERE poNumber = :poNumber LIMIT 1")
    suspend fun getPurchaseOrderByNumber(poNumber: String): PurchaseOrderEntity?

    @Query("SELECT * FROM purchase_orders WHERE status = 'PENDING' ORDER BY id DESC")
    fun getPendingPurchaseOrders(): Flow<List<PurchaseOrderEntity>>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertPO(po: PurchaseOrderEntity): Long

    @Update
    suspend fun updatePO(po: PurchaseOrderEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPOItems(items: List<PurchaseOrderItemEntity>)

    @Query("SELECT * FROM purchase_order_items WHERE poNumber = :poNumber")
    fun getItemsForPO(poNumber: String): Flow<List<PurchaseOrderItemEntity>>

    @Query("SELECT * FROM purchase_order_items WHERE poNumber = :poNumber")
    suspend fun getItemsForPOSync(poNumber: String): List<PurchaseOrderItemEntity>

    @Query("UPDATE purchase_order_items SET qtyReceived = qtyReceived + :qtyReceivedIncrement WHERE poNumber = :poNumber AND itemCode = :itemCode")
    suspend fun incrementPOItemReceivedQty(poNumber: String, itemCode: String, qtyReceivedIncrement: Int)

    @Query("UPDATE purchase_orders SET status = :status WHERE poNumber = :poNumber")
    suspend fun updatePOStatus(poNumber: String, status: String)

    @Query("SELECT COUNT(*) FROM purchase_orders")
    suspend fun count(): Int

    @Query("SELECT * FROM purchase_orders")
    suspend fun getAllPurchaseOrdersSync(): List<PurchaseOrderEntity>
}

@Dao
interface ReceivingDao {
    @Query("SELECT * FROM receivings ORDER BY id DESC")
    fun getAllReceivings(): Flow<List<ReceivingEntity>>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertReceiving(rcv: ReceivingEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReceivingItems(items: List<ReceivingItemEntity>)

    @Query("SELECT * FROM receiving_items WHERE rcvNumber = :rcvNumber")
    fun getItemsForReceiving(rcvNumber: String): Flow<List<ReceivingItemEntity>>

    @Query("SELECT * FROM receivings WHERE rcvDate = :date")
    suspend fun getReceivingsByDate(date: String): List<ReceivingEntity>

    @Query("SELECT * FROM receiving_items")
    suspend fun getAllReceivingItemsSync(): List<ReceivingItemEntity>

    @Query("SELECT * FROM receivings")
    suspend fun getAllReceivingsSync(): List<ReceivingEntity>
}

@Dao
interface SalesOrderDao {
    @Query("SELECT * FROM sales_orders ORDER BY id DESC")
    fun getAllSalesOrders(): Flow<List<SalesOrderEntity>>

    @Query("SELECT * FROM sales_orders WHERE status = 'READY_TO_DELIVER' ORDER BY id DESC")
    fun getReadyToDeliverSalesOrders(): Flow<List<SalesOrderEntity>>

    @Query("SELECT * FROM sales_orders WHERE status = 'CONFIRMED' ORDER BY id DESC")
    fun getConfirmedSalesOrders(): Flow<List<SalesOrderEntity>>

    @Query("SELECT * FROM sales_orders WHERE soNumber = :soNumber LIMIT 1")
    suspend fun getSalesOrderByNumber(soNumber: String): SalesOrderEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertSO(so: SalesOrderEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSOItems(items: List<SalesOrderItemEntity>)

    @Query("SELECT * FROM sales_order_items WHERE soNumber = :soNumber")
    fun getItemsForSO(soNumber: String): Flow<List<SalesOrderItemEntity>>

    @Query("SELECT * FROM sales_order_items WHERE soNumber = :soNumber")
    suspend fun getItemsForSOSync(soNumber: String): List<SalesOrderItemEntity>

    @Query("DELETE FROM sales_order_items WHERE soNumber = :soNumber")
    suspend fun deleteSOItems(soNumber: String)

    @Query("UPDATE sales_orders SET status = :status WHERE soNumber = :soNumber")
    suspend fun updateSOStatus(soNumber: String, status: String)

    @Query("UPDATE sales_orders SET status = :status, totalAmount = :totalAmount WHERE soNumber = :soNumber")
    suspend fun updateSOStatusAndTotal(soNumber: String, status: String, totalAmount: Double)

    @Query("SELECT COUNT(*) FROM sales_orders")
    suspend fun count(): Int

    @Query("SELECT * FROM sales_orders")
    suspend fun getAllSalesOrdersSync(): List<SalesOrderEntity>
}

@Dao
interface DeliveryOrderDao {
    @Query("SELECT * FROM delivery_orders ORDER BY id DESC")
    fun getAllDeliveryOrders(): Flow<List<DeliveryOrderEntity>>

    @Query("SELECT * FROM delivery_orders WHERE doNumber = :doNumber LIMIT 1")
    suspend fun getDeliveryOrderByNumber(doNumber: String): DeliveryOrderEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertDO(deliveryOrder: DeliveryOrderEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDOItems(items: List<DeliveryOrderItemEntity>)

    @Query("SELECT * FROM delivery_order_items WHERE doNumber = :doNumber")
    fun getItemsForDO(doNumber: String): Flow<List<DeliveryOrderItemEntity>>

    @Query("SELECT * FROM delivery_order_items WHERE doNumber = :doNumber")
    suspend fun getItemsForDOSync(doNumber: String): List<DeliveryOrderItemEntity>

    @Query("SELECT * FROM delivery_order_items")
    suspend fun getAllDOItemsSync(): List<DeliveryOrderItemEntity>

    @Query("SELECT * FROM delivery_orders")
    suspend fun getAllDeliveryOrdersSync(): List<DeliveryOrderEntity>

    @Query("SELECT * FROM delivery_orders WHERE doDate = :date")
    suspend fun getDOByDateSync(date: String): List<DeliveryOrderEntity>
}

@Dao
interface StockAdjustmentDao {
    @Query("SELECT * FROM stock_adjustments ORDER BY id DESC")
    fun getAllAdjustments(): Flow<List<StockAdjustmentEntity>>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertAdjustment(adj: StockAdjustmentEntity): Long

    @Query("SELECT * FROM stock_adjustments")
    suspend fun getAllAdjustmentsSync(): List<StockAdjustmentEntity>
}

@Dao
interface ReturnDao {
    @Query("SELECT * FROM returns ORDER BY id DESC")
    fun getAllReturns(): Flow<List<ReturnEntity>>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertReturn(ret: ReturnEntity): Long

    @Query("SELECT * FROM returns")
    suspend fun getAllReturnsSync(): List<ReturnEntity>

    @Query("SELECT * FROM returns WHERE date = :date")
    suspend fun getReturnsByDate(date: String): List<ReturnEntity>
}

@Dao
interface BreakageDao {
    @Query("SELECT * FROM breakages ORDER BY id DESC")
    fun getAllBreakages(): Flow<List<BreakageEntity>>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertBreakage(brk: BreakageEntity): Long

    @Query("SELECT * FROM breakages")
    suspend fun getAllBreakagesSync(): List<BreakageEntity>

    @Query("SELECT * FROM breakages WHERE date = :date")
    suspend fun getBreakagesByDate(date: String): List<BreakageEntity>
}

@Dao
interface StockMovementDao {
    @Query("SELECT * FROM stock_movements ORDER BY id DESC")
    fun getAllMovements(): Flow<List<StockMovementEntity>>

    @Query("SELECT * FROM stock_movements WHERE itemCode = :itemCode ORDER BY id DESC")
    fun getMovementsForItem(itemCode: String): Flow<List<StockMovementEntity>>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertMovement(movement: StockMovementEntity): Long

    @Query("SELECT * FROM stock_movements")
    suspend fun getAllMovementsSync(): List<StockMovementEntity>
}

@Dao
interface AppSettingsDao {
    @Query("SELECT * FROM app_settings WHERE id = 1 LIMIT 1")
    fun getSettings(): Flow<AppSettingsEntity?>

    @Query("SELECT * FROM app_settings WHERE id = 1 LIMIT 1")
    suspend fun getSettingsSync(): AppSettingsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(settings: AppSettingsEntity)
}
