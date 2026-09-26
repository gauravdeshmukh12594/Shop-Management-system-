package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "products",
    indices = [
        Index(value = ["sku"], unique = true),
        Index(value = ["barcode"], unique = false)
    ]
)
data class ProductEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val sku: String,
    val barcode: String,
    val name: String,
    val category: String, // Saree, Kurti, Shirt, Pant, Lehenga, Dress, Fabric, etc.
    val brand: String,
    val colour: String,
    val fabric: String,
    val purchasePrice: Double, // Buy price
    val sellingPrice: Double,  // Sell price (MRP / retail)
    val stock: Int,
    val lowStockThreshold: Int = 3,
    val imagePath: String? = null,
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
