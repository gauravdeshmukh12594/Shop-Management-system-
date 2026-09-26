package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "sales",
    indices = [
        Index(value = ["invoiceNumber"], unique = true)
    ]
)
data class SaleEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val invoiceNumber: String,
    val subtotal: Double,
    val discount: Double = 0.0,
    val totalAmount: Double,
    val paymentMethod: String, // "Cash", "UPI", "Card", "Split"
    val customerName: String = "",
    val customerPhone: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
