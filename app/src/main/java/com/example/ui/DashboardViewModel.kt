package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ble.BleManager
import com.example.ble.ConnectionState
import android.bluetooth.BluetoothDevice
import com.example.ble.BleDevice
import com.example.data.AppDatabase
import com.example.data.MetricRecord
import com.example.data.MetricRepository
import com.example.data.Reminder
import com.example.data.ReminderRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class DashboardViewModel(application: Application) : AndroidViewModel(application) {

    private val bleManager = BleManager(application)
    private val repository: MetricRepository
    private val reminderRepository: ReminderRepository

    init {
        val database = AppDatabase.getDatabase(application)
        repository = MetricRepository(database.metricDao())
        reminderRepository = ReminderRepository(database.reminderDao())
        
        bleManager.onMeasurementReceived = {
            logCurrentMetrics()
        }
    }

    val connectionState = bleManager.connectionState
    val heartRate = bleManager.heartRate
    val temperature = bleManager.temperature
    val pulseValue = bleManager.pulseValue
    val deviceName = bleManager.deviceName
    val discoveredDevices = bleManager.discoveredDevices

    val metricRecords: StateFlow<List<MetricRecord>> = repository.allRecords
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val reminders: StateFlow<List<Reminder>> = reminderRepository.allReminders
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun addReminder(title: String, time: String) {
        viewModelScope.launch {
            reminderRepository.insert(Reminder(title = title, time = time))
        }
    }

    fun toggleReminder(reminder: Reminder) {
        viewModelScope.launch {
            reminderRepository.update(reminder.copy(isActive = !reminder.isActive))
        }
    }

    fun deleteReminder(reminder: Reminder) {
        viewModelScope.launch {
            reminderRepository.delete(reminder)
        }
    }

    fun logCurrentMetrics() {
        val currentHr = heartRate.value
        val currentTemp = temperature.value
        if (currentHr > 0 || currentTemp > 0f) {
            viewModelScope.launch {
                repository.insert(MetricRecord(heartRate = currentHr, temperature = currentTemp))
            }
        }
    }

    fun startScan() {
        bleManager.startScan()
    }

    fun stopScan() {
        bleManager.stopScan()
    }

    fun connectToDevice(device: BluetoothDevice) {
        bleManager.stopScan()
        bleManager.connectToDevice(device)
    }

    fun disconnect() {
        bleManager.disconnect()
    }
}
