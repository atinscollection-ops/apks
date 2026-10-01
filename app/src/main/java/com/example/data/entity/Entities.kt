package com.example.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "users",
    indices = [Index(value = ["userId"], unique = true)]
)
data class UserEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: String,
    val fullName: String,
    val password: String,
    val role: String = "STAFF", // ROOT_ADMIN, ADMIN, STAFF
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "products",
    indices = [
        Index(value = ["itemCode"], unique = true),
        Index(value = ["itemName"]),
        Index(value = ["familyCode"])
    ]
)
data class ProductEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val itemCode: String,
    val familyCode: String,
    val itemName: String,
    val lastReceiving: String = "-",
    val stock: Int = 0,
    val purchasingPrice: Double = 0.0, // Harga Beli / HPP
    val netPrice: Double = 0.0,        // Harga Jual Standar
    val promoPrice: Double = 0.0,      // Harga Promo
    val isPromoActive: Boolean = false,
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "customers",
    indices = [
        Index(value = ["customerName"]),
        Index(value = ["phoneNumber"])
    ]
)
data class CustomerEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val customerName: String,
    val phoneNumber: String,
    val address: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "shipping_services")
data class ShippingServiceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val isActive: Boolean = true
)

@Entity(
    tableName = "purchase_orders",
    indices = [Index(value = ["poNumber"], unique = true)]
)
data class PurchaseOrderEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val poNumber: String,
    val poDate: String,
    val supplierName: String,
    val notes: String = "",
    val status: String = "PENDING", // PENDING, COMPLETED, CANCELLED
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "purchase_order_items")
data class PurchaseOrderItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val poNumber: String,
    val itemCode: String,
    val itemName: String,
    val qtyOrdered: Int,
    val qtyReceived: Int = 0,
    val unitPrice: Double = 0.0,
    val totalPrice: Double = 0.0
)

@Entity(
    tableName = "receivings",
    indices = [Index(value = ["rcvNumber"], unique = true)]
)
data class ReceivingEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val rcvNumber: String,
    val poNumber: String,
    val rcvDate: String,
    val notes: String = "",
    val user: String = "Admin",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "receiving_items")
data class ReceivingItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val rcvNumber: String,
    val itemCode: String,
    val itemName: String,
    val qtyReceived: Int
)

@Entity(
    tableName = "sales_orders",
    indices = [Index(value = ["soNumber"], unique = true)]
)
data class SalesOrderEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val soNumber: String,
    val soDate: String,
    val customerName: String,
    val customerPhone: String = "",
    val customerAddress: String = "",
    val status: String = "CONFIRMED", // CONFIRMED, READY_TO_DELIVER, FULFILLED, CANCELLED
    val totalAmount: Double = 0.0,
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "sales_order_items")
data class SalesOrderItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val soNumber: String,
    val itemCode: String,
    val itemName: String,
    val qty: Int,
    val unitPrice: Double,
    val discount: Double = 0.0,
    val subtotal: Double
)

@Entity(
    tableName = "delivery_orders",
    indices = [
        Index(value = ["doNumber"], unique = true),
        Index(value = ["soNumber"])
    ]
)
data class DeliveryOrderEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val doNumber: String,
    val soNumber: String,
    val doDate: String,
    val customerName: String,
    val customerPhone: String = "",
    val customerAddress: String = "",
    val shippingExpedition: String,
    val status: String = "CONFIRMED", // CONFIRMED, CANCELLED
    val isDeducted: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "delivery_order_items")
data class DeliveryOrderItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val doNumber: String,
    val itemCode: String,
    val itemName: String,
    val qty: Int,
    val unitPrice: Double = 0.0,
    val subtotal: Double = 0.0
)

@Entity(
    tableName = "stock_adjustments",
    indices = [Index(value = ["adjNumber"], unique = true)]
)
data class StockAdjustmentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val adjNumber: String,
    val date: String,
    val itemCode: String,
    val itemName: String,
    val previousStock: Int,
    val adjustmentQty: Int,
    val adjustmentType: String, // INCREASE, DECREASE
    val newStock: Int,
    val reason: String,
    val notes: String = "",
    val user: String = "Admin",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "returns",
    indices = [Index(value = ["retNumber"], unique = true)]
)
data class ReturnEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val retNumber: String,
    val date: String,
    val itemCode: String,
    val itemName: String,
    val quantity: Int,
    val returnType: String, // RETURN_TO_STOCK, NON_STOCK
    val reason: String,
    val notes: String = "",
    val user: String = "Admin",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "breakages",
    indices = [Index(value = ["brkNumber"], unique = true)]
)
data class BreakageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val brkNumber: String,
    val date: String,
    val itemCode: String,
    val itemName: String,
    val quantity: Int,
    val reason: String,
    val notes: String = "",
    val user: String = "Admin",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "stock_movements",
    indices = [
        Index(value = ["movementId"], unique = true),
        Index(value = ["itemCode"]),
        Index(value = ["referenceId"]),
        Index(value = ["timestamp"])
    ]
)
data class StockMovementEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val movementId: String,
    val date: String,
    val itemCode: String,
    val itemName: String,
    val type: String, // RECEIVING, ADJUSTMENT, DELIVERY, RETURN, BREAKAGE
    val referenceId: String,
    val qtyIn: Int,
    val qtyOut: Int,
    val previousStock: Int,
    val newStock: Int,
    val reason: String = "",
    val user: String = "Admin",
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "app_settings")
data class AppSettingsEntity(
    @PrimaryKey val id: Int = 1,
    val storeName: String = "Atin's Collection",
    val storePhone: String = "0899-7884-435",
    val storeAddress: String = "Jl. Sukajadi No. 42, Bandung",
    val labelSize: String = "80mm", // 58mm, 80mm, 100mm
    val currentUser: String = "Admin Atin"
)
