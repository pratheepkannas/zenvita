package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MetricDao {
    @Query("SELECT * FROM metric_records ORDER BY timestamp DESC")
    fun getAllRecords(): Flow<List<MetricRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecord(record: MetricRecord)
}
