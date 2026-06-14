package com.example.ble

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.os.Build
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

// Nordic UART Service UUIDs
val UART_SERVICE_UUID: UUID = UUID.fromString("6e400001-b5a3-f393-e0a9-e50e24dcca9e")
val UART_TX_CHARACTERISTIC_UUID: UUID = UUID.fromString("6e400003-b5a3-f393-e0a9-e50e24dcca9e")
val CCCD_UUID: UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

data class BleDevice(val name: String, val address: String, val bluetoothDevice: BluetoothDevice)

enum class ConnectionState {
    DISCONNECTED, SCANNING, CONNECTING, CONNECTED
}

@SuppressLint("MissingPermission")
class BleManager(private val context: Context) {

    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager.adapter
    private val bluetoothLeScanner = bluetoothAdapter?.bluetoothLeScanner

    private var bluetoothGatt: BluetoothGatt? = null

    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _heartRate = MutableStateFlow(0)
    val heartRate: StateFlow<Int> = _heartRate.asStateFlow()

    private val _temperature = MutableStateFlow(0f)
    val temperature: StateFlow<Float> = _temperature.asStateFlow()
    
    private val _pulseValue = MutableStateFlow(0f)
    val pulseValue: StateFlow<Float> = _pulseValue.asStateFlow()

    private val _deviceName = MutableStateFlow<String?>(null)
    val deviceName: StateFlow<String?> = _deviceName.asStateFlow()

    private val _discoveredDevices = MutableStateFlow<List<BleDevice>>(emptyList())
    val discoveredDevices: StateFlow<List<BleDevice>> = _discoveredDevices.asStateFlow()

    private var dataBuffer = StringBuilder()
    
    var onMeasurementReceived: (() -> Unit)? = null

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            result?.device?.let { device ->
                val name = try { device.name } catch (e: SecurityException) { null }
                if (!name.isNullOrBlank()) {
                    val currentList = _discoveredDevices.value
                    if (currentList.none { it.address == device.address }) {
                        _discoveredDevices.value = currentList + BleDevice(name, device.address, device)
                    }
                }
            }
        }
    }

    fun startScan() {
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled) return
        if (_connectionState.value != ConnectionState.DISCONNECTED) return

        _discoveredDevices.value = emptyList()
        _connectionState.value = ConnectionState.SCANNING
        bluetoothLeScanner?.startScan(scanCallback)
    }

    fun stopScan() {
        if (_connectionState.value == ConnectionState.SCANNING) {
            bluetoothLeScanner?.stopScan(scanCallback)
            _connectionState.value = ConnectionState.DISCONNECTED
        }
    }

    fun connectToDevice(device: BluetoothDevice) {
        stopScan()
        _connectionState.value = ConnectionState.CONNECTING
        _deviceName.value = try { device.name ?: "Unknown Device" } catch (e: SecurityException) { "Unknown Device" }
        
        bluetoothGatt = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            device.connectGatt(context, false, gattCallback, BluetoothDevice.TRANSPORT_LE)
        } else {
            device.connectGatt(context, false, gattCallback)
        }
    }

    fun disconnect() {
        bluetoothGatt?.disconnect()
        bluetoothGatt?.close()
        bluetoothGatt = null
        _connectionState.value = ConnectionState.DISCONNECTED
        _deviceName.value = null
    }

    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                _connectionState.value = ConnectionState.CONNECTED
                gatt.discoverServices()
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                _connectionState.value = ConnectionState.DISCONNECTED
                bluetoothGatt?.close()
                bluetoothGatt = null
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                enableUartNotifications(gatt)
            }
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic
        ) {
            if (characteristic.uuid == UART_TX_CHARACTERISTIC_UUID) {
                val data = characteristic.value
                if (data != null) {
                    var message = String(data)
                    message = message.replace("\\n", "\n")
                    dataBuffer.append(message)
                    
                    var endOfLineIndex = dataBuffer.indexOf("\n")
                    while (endOfLineIndex != -1) {
                        val completeMessage = dataBuffer.substring(0, endOfLineIndex).trim()
                        dataBuffer.delete(0, endOfLineIndex + 1)
                        parseMessage(completeMessage)
                        endOfLineIndex = dataBuffer.indexOf("\n")
                    }
                }
            }
        }
    }

    private fun enableUartNotifications(gatt: BluetoothGatt) {
        val service = gatt.getService(UART_SERVICE_UUID)
        val charTx = service?.getCharacteristic(UART_TX_CHARACTERISTIC_UUID)
        if (charTx != null) {
            gatt.setCharacteristicNotification(charTx, true)
            val descriptor = charTx.getDescriptor(CCCD_UUID)
            descriptor?.let {
                it.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                gatt.writeDescriptor(it)
            }
        }
    }

    private fun parseMessage(message: String) {
        try {
            if (message.startsWith("I:")) {
                val pulseStr = message.substring(2)
                _pulseValue.value = pulseStr.toFloatOrNull() ?: 0f
            } else if (message.startsWith("H:")) {
                val parts = message.split(",")
                for (part in parts) {
                    if (part.startsWith("H:")) {
                        _heartRate.value = part.substring(2).toIntOrNull() ?: 0
                    } else if (part.startsWith("T:")) {
                        _temperature.value = part.substring(2).toFloatOrNull() ?: 0f
                    }
                }
                onMeasurementReceived?.invoke()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
