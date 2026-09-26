package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "stock_transactions",
    indices = [
        Index(value = ["productId"]),
        Index(value = ["createdAt"])
    ]
)
data class StockTransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val productId: Long,
    val productName: String,
    val type: String, // "PURCHASE", "SALE", "ADJUSTMENT", "RETURN"
    val quantityChange: Int, // e.g. +10, -1, +2
    val previousStock: Int,
    val newStock: Int,
    val referenceId: String = "", // e.g. "INV-2026-000101" or "Stock In"
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
