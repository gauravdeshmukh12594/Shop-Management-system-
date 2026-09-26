package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.example.data.local.entity.SaleEntity
import com.example.data.local.entity.SaleItemEntity
import kotlinx.coroutines.flow.Flow

data class SaleWithItems(
    val sale: SaleEntity,
    val items: List<SaleItemEntity>
)

@Dao
interface SaleDao {
    @Query("SELECT * FROM sales ORDER BY createdAt DESC")
    fun getAllSales(): Flow<List<SaleEntity>>

    @Query("SELECT * FROM sales WHERE id = :id LIMIT 1")
    fun getSaleById(id: Long): Flow<SaleEntity?>

    @Query("SELECT * FROM sales WHERE id = :id LIMIT 1")
    suspend fun getSaleByIdDirect(id: Long): SaleEntity?

    @Query("SELECT * FROM sales WHERE invoiceNumber = :invoiceNumber LIMIT 1")
    suspend fun getSaleByInvoice(invoiceNumber: String): SaleEntity?

    @Query("SELECT * FROM sales WHERE createdAt >= :startOfDay ORDER BY createdAt DESC")
    fun getSalesToday(startOfDay: Long): Flow<List<SaleEntity>>

    @Query("SELECT COUNT(*) FROM sales WHERE createdAt >= :startOfDay")
    fun getTodaySalesCount(startOfDay: Long): Flow<Int>

    @Query("SELECT SUM(totalAmount) FROM sales WHERE createdAt >= :startOfDay")
    fun getTodaySalesTotal(startOfDay: Long): Flow<Double?>

    @Query("SELECT SUM(totalAmount) FROM sales")
    fun getAllTimeSalesTotal(): Flow<Double?>

    @Query("SELECT COUNT(*) FROM sales")
    fun getAllTimeSalesCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSale(sale: SaleEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSaleItems(items: List<SaleItemEntity>)

    @Query("SELECT * FROM sale_items WHERE saleId = :saleId")
    fun getItemsForSale(saleId: Long): Flow<List<SaleItemEntity>>

    @Query("SELECT * FROM sale_items WHERE saleId = :saleId")
    suspend fun getItemsForSaleDirect(saleId: Long): List<SaleItemEntity>

    @Query("SELECT * FROM sale_items ORDER BY id DESC")
    fun getAllSaleItems(): Flow<List<SaleItemEntity>>
}
