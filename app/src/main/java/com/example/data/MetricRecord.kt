package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "metric_records")
data class MetricRecord(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val heartRate: Int,
    val temperature: Float,
    val timestamp: Long = System.currentTimeMillis()
)
