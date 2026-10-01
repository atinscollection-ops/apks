package com.example.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.dao.*
import com.example.data.entity.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        UserEntity::class,
        ProductEntity::class,
        CustomerEntity::class,
        ShippingServiceEntity::class,
        PurchaseOrderEntity::class,
        PurchaseOrderItemEntity::class,
        ReceivingEntity::class,
        ReceivingItemEntity::class,
        SalesOrderEntity::class,
        SalesOrderItemEntity::class,
        DeliveryOrderEntity::class,
        DeliveryOrderItemEntity::class,
        StockAdjustmentEntity::class,
        ReturnEntity::class,
        BreakageEntity::class,
        StockMovementEntity::class,
        AppSettingsEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun userDao(): UserDao
    abstract fun productDao(): ProductDao
    abstract fun customerDao(): CustomerDao
    abstract fun shippingServiceDao(): ShippingServiceDao
    abstract fun purchaseOrderDao(): PurchaseOrderDao
    abstract fun receivingDao(): ReceivingDao
    abstract fun salesOrderDao(): SalesOrderDao
    abstract fun deliveryOrderDao(): DeliveryOrderDao
    abstract fun stockAdjustmentDao(): StockAdjustmentDao
    abstract fun returnDao(): ReturnDao
    abstract fun breakageDao(): BreakageDao
    abstract fun stockMovementDao(): StockMovementDao
    abstract fun appSettingsDao(): AppSettingsDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "wms_atin_db"
                )
                    .fallbackToDestructiveMigration()
                    .addCallback(AppDatabaseCallback(scope))
                    .build()
                INSTANCE = instance
                scope.launch(Dispatchers.IO) {
                    populateInitialData(instance)
                }
                instance
            }
        }

        suspend fun populateInitialData(database: AppDatabase) {
            try {
                // Seed Root Admin User
                if (database.userDao().count() == 0) {
                    val rootAdmin = UserEntity(
                        userId = "admin",
                        fullName = "Root Administrator",
                        password = "rootadmin",
                        role = "ROOT_ADMIN",
                        isActive = true
                    )
                    database.userDao().insert(rootAdmin)
                }

                if (database.productDao().count() > 0) return

                // Seed Shipping Services
                val shippingServices = listOf(
                    ShippingServiceEntity(name = "SPX Ekspress", isActive = true),
                    ShippingServiceEntity(name = "JNE Express", isActive = true),
                    ShippingServiceEntity(name = "J&T Express", isActive = true),
                    ShippingServiceEntity(name = "SiCepat Ekspres", isActive = true),
                    ShippingServiceEntity(name = "AnterAja", isActive = true),
                    ShippingServiceEntity(name = "POS Indonesia", isActive = true),
                    ShippingServiceEntity(name = "Ninja Xpress", isActive = true),
                    ShippingServiceEntity(name = "GoSend / GrabExpress", isActive = true)
                )
                database.shippingServiceDao().insertAll(shippingServices)

                // Seed Settings
                database.appSettingsDao().insertOrUpdate(
                    AppSettingsEntity(
                        id = 1,
                        storeName = "Atin's Collection",
                        storePhone = "0899-7884-435",
                        storeAddress = "Jl. Sukajadi No. 42, Bandung",
                        labelSize = "80mm",
                        currentUser = "Root Administrator"
                    )
                )

                // Seed Initial Fashion Products for Atin's Collection with SKU-xxx format and purchasingPrice
                val initialProducts = listOf(
                    ProductEntity(
                        itemCode = "SKU-001",
                        familyCode = "GAMIS",
                        itemName = "Gamis Rayon Premium Twill Dusty Pink",
                        lastReceiving = "2026-09-25",
                        stock = 50,
                        purchasingPrice = 95000.0,
                        netPrice = 145000.0,
                        promoPrice = 129000.0,
                        isPromoActive = false
                    ),
                    ProductEntity(
                        itemCode = "SKU-002",
                        familyCode = "GAMIS",
                        itemName = "Gamis Crinkle Airflow Mocca",
                        lastReceiving = "2026-09-26",
                        stock = 35,
                        purchasingPrice = 85000.0,
                        netPrice = 135000.0,
                        promoPrice = 119000.0,
                        isPromoActive = false
                    ),
                    ProductEntity(
                        itemCode = "SKU-003",
                        familyCode = "HIJAB",
                        itemName = "Pashmina Ceruty Baby Doll Rose",
                        lastReceiving = "2026-09-27",
                        stock = 120,
                        purchasingPrice = 18000.0,
                        netPrice = 35000.0,
                        promoPrice = 28000.0,
                        isPromoActive = false
                    ),
                    ProductEntity(
                        itemCode = "SKU-004",
                        familyCode = "HIJAB",
                        itemName = "Hijab Segiempat Paris Premium Mauve",
                        lastReceiving = "2026-09-27",
                        stock = 80,
                        purchasingPrice = 12000.0,
                        netPrice = 25000.0,
                        promoPrice = 20000.0,
                        isPromoActive = false
                    ),
                    ProductEntity(
                        itemCode = "SKU-005",
                        familyCode = "TUNIK",
                        itemName = "Tunik Linen Rami Oversize Broken White",
                        lastReceiving = "2026-09-28",
                        stock = 25,
                        purchasingPrice = 70000.0,
                        netPrice = 115000.0,
                        promoPrice = 99000.0,
                        isPromoActive = false
                    ),
                    ProductEntity(
                        itemCode = "SKU-006",
                        familyCode = "BLOUSE",
                        itemName = "Blouse Silk Satin Floral Dusty Peach",
                        lastReceiving = "2026-09-29",
                        stock = 40,
                        purchasingPrice = 80000.0,
                        netPrice = 125000.0,
                        promoPrice = 110000.0,
                        isPromoActive = false
                    ),
                    ProductEntity(
                        itemCode = "SKU-007",
                        familyCode = "BOTTOMS",
                        itemName = "Rok Plisket Premium A-Line Nude Pink",
                        lastReceiving = "2026-09-29",
                        stock = 60,
                        purchasingPrice = 45000.0,
                        netPrice = 75000.0,
                        promoPrice = 65000.0,
                        isPromoActive = false
                    ),
                    ProductEntity(
                        itemCode = "SKU-008",
                        familyCode = "OUTER",
                        itemName = "Outer Kimono Organza Embroidery",
                        lastReceiving = "2026-09-30",
                        stock = 15,
                        purchasingPrice = 105000.0,
                        netPrice = 165000.0,
                        promoPrice = 149000.0,
                        isPromoActive = false
                    )
                )
                database.productDao().insertAll(initialProducts)

                // Seed Customers
                val initialCustomers = listOf(
                    CustomerEntity(
                        customerName = "Uwa Awat",
                        phoneNumber = "085798088831",
                        address = "Alamat kp gabungan RT 02 RW 02 Desa Sukapura Kec Cidaun Kab Cianjur"
                    ),
                    CustomerEntity(
                        customerName = "Siti Rahmawati",
                        phoneNumber = "0812-3456-7890",
                        address = "Jl. Melati No. 12 RT 03/05, Kec. Coblong, Kota Bandung, Jawa Barat 40132"
                    ),
                    CustomerEntity(
                        customerName = "Dewi Lestari",
                        phoneNumber = "0813-8822-1144",
                        address = "Komplek Griya Indah Blok C2 No. 8, Cilandak, Jakarta Selatan 12430"
                    ),
                    CustomerEntity(
                        customerName = "Andi Pratama",
                        phoneNumber = "0857-1234-9988",
                        address = "Jl. Gejayan Gg. Flamboyan No. 15, Sleman, D.I. Yogyakarta 55281"
                    )
                )
                database.customerDao().insertAll(initialCustomers)

                // Seed initial stock movements
                val now = System.currentTimeMillis()
                val initialDate = "2026-09-30"
                for (p in initialProducts) {
                    database.stockMovementDao().insertMovement(
                        StockMovementEntity(
                            movementId = "INIT-${p.itemCode}",
                            date = initialDate,
                            itemCode = p.itemCode,
                            itemName = p.itemName,
                            type = "RECEIVING",
                            referenceId = "INIT-STOCK",
                            qtyIn = p.stock,
                            qtyOut = 0,
                            previousStock = 0,
                            newStock = p.stock,
                            reason = "Saldo Awal Inventory",
                            user = "System",
                            timestamp = now
                        )
                    )
                }
            } catch (e: Exception) {
                // Ignore seeding errors
            }
        }
    }

    private class AppDatabaseCallback(
        private val scope: CoroutineScope
    ) : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            INSTANCE?.let { database ->
                scope.launch(Dispatchers.IO) {
                    populateInitialData(database)
                }
            }
        }
    }
}
