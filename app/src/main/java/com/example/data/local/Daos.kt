package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface StationConfigDao {
    @Query("SELECT * FROM station_config WHERE id = 1")
    fun observeConfig(): Flow<StationConfigEntity?>

    @Query("SELECT * FROM station_config WHERE id = 1")
    suspend fun getConfigOnce(): StationConfigEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertConfig(config: StationConfigEntity)
}

@Dao
interface StationLocationDao {
    @Query("SELECT * FROM station_locations ORDER BY isPrimary DESC, id ASC")
    fun observeAllLocations(): Flow<List<StationLocationEntity>>

    @Query("SELECT * FROM station_locations ORDER BY isPrimary DESC, id ASC")
    suspend fun getAllOnce(): List<StationLocationEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLocation(location: StationLocationEntity): Long

    @Update
    suspend fun updateLocation(location: StationLocationEntity)

    @Query("DELETE FROM station_locations WHERE id = :id")
    suspend fun deleteLocationById(id: Int)
}

@Dao
interface TransactionDao {
    @Query("SELECT * FROM recharge_transactions ORDER BY createdAt DESC")
    fun observeAllTransactions(): Flow<List<RechargeTransactionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: RechargeTransactionEntity): Long

    @Query("UPDATE recharge_transactions SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: Int, status: String)

    @Query("DELETE FROM recharge_transactions WHERE id = :id")
    suspend fun deleteById(id: Int)
}
