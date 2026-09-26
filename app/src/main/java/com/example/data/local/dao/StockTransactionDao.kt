package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.local.entity.StockTransactionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StockTransactionDao {
    @Query("SELECT * FROM stock_transactions ORDER BY createdAt DESC")
    fun getAllTransactions(): Flow<List<StockTransactionEntity>>

    @Query("SELECT * FROM stock_transactions WHERE productId = :productId ORDER BY createdAt DESC")
    fun getTransactionsForProduct(productId: Long): Flow<List<StockTransactionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: StockTransactionEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransactions(transactions: List<StockTransactionEntity>): List<Long>
}
