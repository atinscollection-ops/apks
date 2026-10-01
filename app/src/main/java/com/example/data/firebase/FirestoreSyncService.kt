package com.example.data.firebase

import android.util.Log
import com.example.data.database.AppDatabase
import com.example.data.entity.*
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FirestoreSyncService(private val database: AppDatabase) {

    private val tag = "FirestoreSyncService"

    private val firestore: FirebaseFirestore? by lazy {
        try {
            FirebaseFirestore.getInstance()
        } catch (e: Exception) {
            Log.w(tag, "Firebase Firestore not initialized: ${e.message}")
            null
        }
    }

    suspend fun syncProductToCloud(product: ProductEntity) = withContext(Dispatchers.IO) {
        val fs = firestore ?: return@withContext
        try {
            val data = hashMapOf(
                "itemCode" to product.itemCode,
                "familyCode" to product.familyCode,
                "itemName" to product.itemName,
                "lastReceiving" to product.lastReceiving,
                "stock" to product.stock,
                "purchasingPrice" to product.purchasingPrice,
                "netPrice" to product.netPrice,
                "promoPrice" to product.promoPrice,
                "isPromoActive" to product.isPromoActive,
                "isActive" to product.isActive,
                "updatedAt" to product.updatedAt
            )
            fs.collection("products").document(product.itemCode)
                .set(data, SetOptions.merge())
        } catch (e: Exception) {
            Log.w(tag, "Failed to sync product to cloud: ${e.message}")
        }
    }

    suspend fun deleteProductFromCloud(itemCode: String) = withContext(Dispatchers.IO) {
        val fs = firestore ?: return@withContext
        try {
            fs.collection("products").document(itemCode).delete()
        } catch (e: Exception) {
            Log.w(tag, "Failed to delete product from cloud: ${e.message}")
        }
    }

    suspend fun syncCustomerToCloud(customer: CustomerEntity) = withContext(Dispatchers.IO) {
        val fs = firestore ?: return@withContext
        try {
            val data = hashMapOf(
                "id" to customer.id,
                "customerName" to customer.customerName,
                "phoneNumber" to customer.phoneNumber,
                "address" to customer.address,
                "createdAt" to customer.createdAt
            )
            val docId = if (customer.phoneNumber.isNotBlank()) customer.phoneNumber else "cust_${customer.id}"
            fs.collection("customers").document(docId).set(data, SetOptions.merge())
        } catch (e: Exception) {
            Log.w(tag, "Failed to sync customer to cloud: ${e.message}")
        }
    }

    suspend fun deleteCustomerFromCloud(customer: CustomerEntity) = withContext(Dispatchers.IO) {
        val fs = firestore ?: return@withContext
        try {
            val docId = if (customer.phoneNumber.isNotBlank()) customer.phoneNumber else "cust_${customer.id}"
            fs.collection("customers").document(docId).delete()
        } catch (e: Exception) {
            Log.w(tag, "Failed to delete customer from cloud: ${e.message}")
        }
    }

    suspend fun syncSOToCloud(so: SalesOrderEntity, items: List<SalesOrderItemEntity>) = withContext(Dispatchers.IO) {
        val fs = firestore ?: return@withContext
        try {
            val soData = hashMapOf(
                "soNumber" to so.soNumber,
                "soDate" to so.soDate,
                "customerName" to so.customerName,
                "customerPhone" to so.customerPhone,
                "customerAddress" to so.customerAddress,
                "status" to so.status,
                "totalAmount" to so.totalAmount,
                "notes" to so.notes,
                "createdAt" to so.createdAt,
                "items" to items.map {
                    mapOf(
                        "itemCode" to it.itemCode,
                        "itemName" to it.itemName,
                        "qty" to it.qty,
                        "unitPrice" to it.unitPrice,
                        "subtotal" to it.subtotal
                    )
                }
            )
            fs.collection("sales_orders").document(so.soNumber).set(soData, SetOptions.merge())
        } catch (e: Exception) {
            Log.w(tag, "Failed to sync SO to cloud: ${e.message}")
        }
    }

    suspend fun syncDOToCloud(deliveryOrder: DeliveryOrderEntity, items: List<DeliveryOrderItemEntity>) = withContext(Dispatchers.IO) {
        val fs = firestore ?: return@withContext
        try {
            val doData = hashMapOf(
                "doNumber" to deliveryOrder.doNumber,
                "soNumber" to deliveryOrder.soNumber,
                "doDate" to deliveryOrder.doDate,
                "customerName" to deliveryOrder.customerName,
                "customerPhone" to deliveryOrder.customerPhone,
                "customerAddress" to deliveryOrder.customerAddress,
                "shippingExpedition" to deliveryOrder.shippingExpedition,
                "status" to deliveryOrder.status,
                "createdAt" to deliveryOrder.createdAt,
                "items" to items.map {
                    mapOf(
                        "itemCode" to it.itemCode,
                        "itemName" to it.itemName,
                        "qty" to it.qty,
                        "unitPrice" to it.unitPrice,
                        "subtotal" to it.subtotal
                    )
                }
            )
            fs.collection("delivery_orders").document(deliveryOrder.doNumber).set(doData, SetOptions.merge())
        } catch (e: Exception) {
            Log.w(tag, "Failed to sync DO to cloud: ${e.message}")
        }
    }

    suspend fun syncUserToCloud(user: UserEntity) = withContext(Dispatchers.IO) {
        val fs = firestore ?: return@withContext
        try {
            val data = hashMapOf(
                "userId" to user.userId,
                "fullName" to user.fullName,
                "password" to user.password,
                "role" to user.role,
                "isActive" to user.isActive,
                "createdAt" to user.createdAt
            )
            fs.collection("users").document(user.userId).set(data, SetOptions.merge())
        } catch (e: Exception) {
            Log.w(tag, "Failed to sync user to cloud: ${e.message}")
        }
    }

    suspend fun deleteUserFromCloud(userId: String) = withContext(Dispatchers.IO) {
        val fs = firestore ?: return@withContext
        try {
            fs.collection("users").document(userId).delete()
        } catch (e: Exception) {
            Log.w(tag, "Failed to delete user from cloud: ${e.message}")
        }
    }

    suspend fun syncMovementToCloud(movement: StockMovementEntity) = withContext(Dispatchers.IO) {
        val fs = firestore ?: return@withContext
        try {
            val data = hashMapOf(
                "movementId" to movement.movementId,
                "date" to movement.date,
                "itemCode" to movement.itemCode,
                "itemName" to movement.itemName,
                "type" to movement.type,
                "referenceId" to movement.referenceId,
                "qtyIn" to movement.qtyIn,
                "qtyOut" to movement.qtyOut,
                "previousStock" to movement.previousStock,
                "newStock" to movement.newStock,
                "reason" to movement.reason,
                "user" to movement.user,
                "timestamp" to movement.timestamp
            )
            fs.collection("stock_movements").document(movement.movementId).set(data, SetOptions.merge())
        } catch (e: Exception) {
            Log.w(tag, "Failed to sync movement to cloud: ${e.message}")
        }
    }

    // REAL-TIME SYNC ENGINE: Listen directly to Firestore changes in real-time
    fun startRealtimeSync(scope: CoroutineScope) {
        val fs = firestore ?: return
        try {
            // 1. Real-time Products Sync
            fs.collection("products").addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener
                scope.launch(Dispatchers.IO) {
                    for (doc in snapshot.documents) {
                        val itemCode = doc.getString("itemCode") ?: doc.id
                        val familyCode = doc.getString("familyCode") ?: "FAM01"
                        val itemName = doc.getString("itemName") ?: itemCode
                        val lastReceiving = doc.getString("lastReceiving") ?: ""
                        val stock = doc.getLong("stock")?.toInt() ?: 0
                        val purchasingPrice = doc.getDouble("purchasingPrice") ?: 0.0
                        val netPrice = doc.getDouble("netPrice") ?: 0.0
                        val promoPrice = doc.getDouble("promoPrice") ?: 0.0
                        val isPromoActive = doc.getBoolean("isPromoActive") ?: false
                        val isActive = doc.getBoolean("isActive") ?: true
                        val updatedAt = doc.getLong("updatedAt") ?: System.currentTimeMillis()

                        val existing = database.productDao().getProductByItemCode(itemCode)
                        if (existing == null) {
                            database.productDao().insert(
                                ProductEntity(
                                    itemCode = itemCode,
                                    familyCode = familyCode,
                                    itemName = itemName,
                                    lastReceiving = lastReceiving,
                                    stock = stock,
                                    purchasingPrice = purchasingPrice,
                                    netPrice = netPrice,
                                    promoPrice = promoPrice,
                                    isPromoActive = isPromoActive,
                                    isActive = isActive,
                                    updatedAt = updatedAt
                                )
                            )
                        } else if (updatedAt > existing.updatedAt || stock != existing.stock) {
                            database.productDao().update(
                                existing.copy(
                                    familyCode = familyCode,
                                    itemName = itemName,
                                    lastReceiving = lastReceiving,
                                    stock = stock,
                                    purchasingPrice = purchasingPrice,
                                    netPrice = netPrice,
                                    promoPrice = promoPrice,
                                    isPromoActive = isPromoActive,
                                    isActive = isActive,
                                    updatedAt = updatedAt
                                )
                            )
                        }
                    }
                }
            }

            // 2. Real-time Customers Sync
            fs.collection("customers").addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener
                scope.launch(Dispatchers.IO) {
                    for (doc in snapshot.documents) {
                        val name = doc.getString("customerName") ?: return@launch
                        val phone = doc.getString("phoneNumber") ?: ""
                        val address = doc.getString("address") ?: ""
                        val createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis()
                        val existingList = database.customerDao().getAllCustomersSync()
                        val existing = existingList.find { it.phoneNumber == phone || it.customerName.equals(name, true) }
                        if (existing == null) {
                            database.customerDao().insert(
                                CustomerEntity(
                                    customerName = name,
                                    phoneNumber = phone,
                                    address = address,
                                    createdAt = createdAt
                                )
                            )
                        }
                    }
                }
            }

            // 3. Real-time Users Sync
            fs.collection("users").addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener
                scope.launch(Dispatchers.IO) {
                    for (doc in snapshot.documents) {
                        val uid = doc.getString("userId") ?: doc.id
                        val fullName = doc.getString("fullName") ?: uid
                        val password = doc.getString("password") ?: ""
                        val role = doc.getString("role") ?: "STAFF"
                        val isActive = doc.getBoolean("isActive") ?: true
                        val createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis()

                        val existing = database.userDao().getUserByUserId(uid)
                        if (existing == null) {
                            database.userDao().insert(
                                UserEntity(
                                    userId = uid,
                                    fullName = fullName,
                                    password = password,
                                    role = role,
                                    isActive = isActive,
                                    createdAt = createdAt
                                )
                            )
                        } else if (password != existing.password || role != existing.role || isActive != existing.isActive) {
                            database.userDao().update(
                                existing.copy(fullName = fullName, password = password, role = role, isActive = isActive)
                            )
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(tag, "Failed to attach Firestore listeners: ${e.message}")
        }
    }
}
