package com.example.data

import kotlinx.coroutines.flow.Flow

class MetricRepository(private val metricDao: MetricDao) {
    val allRecords: Flow<List<MetricRecord>> = metricDao.getAllRecords()

    suspend fun insert(record: MetricRecord) {
        metricDao.insertRecord(record)
    }
}
