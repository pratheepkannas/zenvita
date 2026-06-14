package com.example.ui

import android.Manifest
import android.os.Build
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import com.example.ble.BleDevice
import android.bluetooth.BluetoothDevice
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ble.ConnectionState
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.MultiplePermissionsState
import com.google.accompanist.permissions.rememberMultiplePermissionsState

// Theme Colors
val BgMain @Composable get() = MaterialTheme.colorScheme.surface
val TextMain @Composable get() = MaterialTheme.colorScheme.onSurface

val HrBg @Composable get() = MaterialTheme.colorScheme.errorContainer
val HrText @Composable get() = MaterialTheme.colorScheme.onErrorContainer
val HrBorder @Composable get() = MaterialTheme.colorScheme.outlineVariant
val HrIconBg @Composable get() = MaterialTheme.colorScheme.error.copy(alpha = 0.2f)

val BtBg @Composable get() = MaterialTheme.colorScheme.secondaryContainer
val BtText @Composable get() = MaterialTheme.colorScheme.onSecondaryContainer
val BtBorder @Composable get() = MaterialTheme.colorScheme.outlineVariant

val TempBg @Composable get() = MaterialTheme.colorScheme.tertiaryContainer
val TempText @Composable get() = MaterialTheme.colorScheme.onTertiaryContainer
val TempBorder @Composable get() = MaterialTheme.colorScheme.outlineVariant

val WaveBg @Composable get() = MaterialTheme.colorScheme.primaryContainer
val WaveText @Composable get() = MaterialTheme.colorScheme.onPrimaryContainer

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun DashboardScreen(viewModel: DashboardViewModel = viewModel(), authViewModel: AuthViewModel? = null, themeViewModel: ThemeViewModel? = null) {
    val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        listOf(
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
    } else {
        listOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
    }

    val multiplePermissionsState = rememberMultiplePermissionsState(permissions)

    LaunchedEffect(Unit) {
        if (!multiplePermissionsState.allPermissionsGranted) {
            multiplePermissionsState.launchMultiplePermissionRequest()
        }
    }

    val discoveredDevices by viewModel.discoveredDevices.collectAsState()
    val connState by viewModel.connectionState.collectAsState()
    var currentRoute by remember { mutableStateOf("Dashboard") }

    if (connState == ConnectionState.SCANNING) {
        DeviceListDialog(
            devices = discoveredDevices,
            onDeviceSelected = { device ->
                viewModel.connectToDevice(device.bluetoothDevice)
            },
            onDismiss = {
                viewModel.stopScan()
            }
        )
    }

    Scaffold(
        containerColor = BgMain,
        bottomBar = { 
            BottomNavBar(
                currentRoute = currentRoute,
                onRouteSelected = { route -> currentRoute = route }
            ) 
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            AppHeader(viewModel, multiplePermissionsState, onSettingsClick = { currentRoute = "Settings" })
            
            if (currentRoute == "Dashboard") {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Top Row (HR + BT/Temp)
                    Row(
                        modifier = Modifier.weight(7f).fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                            HeartRateCard(viewModel)
                        }
                        
                        Column(
                            modifier = Modifier.weight(1f).fillMaxHeight(),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(modifier = Modifier.weight(3f).fillMaxWidth()) {
                                BluetoothCard(viewModel, multiplePermissionsState)
                            }
                            Box(modifier = Modifier.weight(4f).fillMaxWidth()) {
                                TemperatureCard(viewModel)
                            }
                        }
                    }
                    
                    // Bottom Row (Wave)
                    Box(modifier = Modifier.weight(5f).fillMaxWidth()) {
                        PulseWaveCard(viewModel)
                    }
                }
            } else if (currentRoute == "Reports") {
                ReportsScreen(viewModel)
            } else if (currentRoute == "Alerts") {
                AlertsScreen(viewModel)
            } else if (currentRoute == "Profile") {
                if (authViewModel != null) {
                    ProfileScreen(authViewModel)
                } else {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Profile not available", color = TextMain)
                    }
                }
            } else if (currentRoute == "Settings") {
                if (authViewModel != null && themeViewModel != null) {
                    SettingsScreen(
                        authViewModel = authViewModel,
                        themeViewModel = themeViewModel,
                        onBack = { currentRoute = "Dashboard" }
                    )
                }
            } else {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Coming Soon", color = TextMain)
                }
            }
        }
    }
}

@Composable
fun ProfileScreen(authViewModel: AuthViewModel) {
    val userProfile by authViewModel.userProfile.collectAsState()

    LaunchedEffect(Unit) {
        authViewModel.fetchUserProfile()
    }

    val launcher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri: android.net.Uri? ->
        uri?.let {
            authViewModel.updateProfileImage(it.toString())
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(CircleShape)
                .background(BtBg)
                .border(2.dp, BtBorder, CircleShape)
                .clickable { launcher.launch("image/*") },
            contentAlignment = Alignment.Center
        ) {
            if (userProfile?.profileImageUri.isNullOrEmpty()) {
                Icon(Icons.Rounded.AccountCircle, contentDescription = "Profile Photo", modifier = Modifier.size(80.dp), tint = TextMain.copy(alpha=0.7f))
            } else {
                coil.compose.AsyncImage(
                    model = userProfile?.profileImageUri,
                    contentDescription = "Profile Photo",
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ProfileField("ID Name", userProfile?.idName ?: "-")
            ProfileField("Number", userProfile?.number ?: "-")
            ProfileField("Email ID", userProfile?.email ?: "-")
        }

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = { authViewModel.logout() },
            modifier = Modifier.fillMaxWidth().height(50.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
        ) {
            Text("Logout", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }
    }
}

@Composable
fun ProfileField(label: String, value: String) {
    Column {
        Text(label, fontSize = 12.sp, color = TextMain.copy(alpha = 0.6f))
        Spacer(modifier = Modifier.height(4.dp))
        Text(value, fontSize = 16.sp, fontWeight = FontWeight.Medium, color = TextMain)
        Divider(modifier = Modifier.padding(top = 8.dp), color = Color.LightGray, thickness = 0.5.dp)
    }
}

@Composable
fun AlertsScreen(viewModel: DashboardViewModel) {
    val reminders by viewModel.reminders.collectAsState()
    var showDialog by remember { mutableStateOf(false) }

    if (showDialog) {
        AddReminderDialog(
            onDismiss = { showDialog = false },
            onAdd = { title, time ->
                viewModel.addReminder(title, time)
                showDialog = false
            }
        )
    }

    Box(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        if (reminders.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No reminders set. Add one below.", color = TextMain.copy(alpha = 0.6f))
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(bottom = 80.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(reminders) { reminder ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.dp, HrBorder)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(reminder.time, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = TextMain)
                                Text(reminder.title, fontSize = 14.sp, color = TextMain.copy(alpha = 0.7f))
                            }
                            Switch(
                                checked = reminder.isActive,
                                onCheckedChange = { viewModel.toggleReminder(reminder) },
                                colors = SwitchDefaults.colors(checkedThumbColor = BtBg, checkedTrackColor = BtText)
                            )
                        }
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = { showDialog = true },
            modifier = Modifier.align(Alignment.BottomEnd).padding(bottom = 16.dp),
            containerColor = TempBg,
            contentColor = TempText
        ) {
            Icon(Icons.Rounded.Add, contentDescription = "Add Reminder")
        }
    }
}

@Composable
fun AddReminderDialog(onDismiss: () -> Unit, onAdd: (String, String) -> Unit) {
    var title by remember { mutableStateOf("") }
    var hour by remember { mutableStateOf("08") }
    var minute by remember { mutableStateOf("00") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Reminder", fontWeight = FontWeight.Bold, color = TextMain) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Remind me to...") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = hour,
                        onValueChange = { if (it.length <= 2) hour = it },
                        label = { Text("HH") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    Text(":", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    OutlinedTextField(
                        value = minute,
                        onValueChange = { if (it.length <= 2) minute = it },
                        label = { Text("MM") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onAdd(title.ifBlank { "Check Vitals" }, "$hour:$minute") }) {
                Text("Save", color = BtText, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextMain.copy(alpha = 0.6f))
            }
        },
        containerColor = BgMain
    )
}

@Composable
fun ReportsScreen(viewModel: DashboardViewModel) {
    val records by viewModel.metricRecords.collectAsState()

    if (records.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No records yet. Log an event from the Dashboard.", color = TextMain.copy(alpha = 0.6f))
        }
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(records) { record ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, HrBorder)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            val time = java.text.SimpleDateFormat("MMM dd, yyyy HH:mm", java.util.Locale.getDefault()).format(java.util.Date(record.timestamp))
                            Text(time, fontSize = 12.sp, color = TextMain.copy(alpha = 0.6f))
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Rounded.Favorite, contentDescription = null, tint = HrText, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("${record.heartRate} BPM", fontWeight = FontWeight.Bold, color = TextMain)
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Rounded.Thermostat, contentDescription = null, tint = TempText, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(String.format("%.1f °C", record.temperature), fontWeight = FontWeight.Bold, color = TextMain)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DeviceListDialog(
    devices: List<BleDevice>,
    onDeviceSelected: (BleDevice) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Discovered Devices", fontWeight = FontWeight.Bold, color = TextMain) },
        text = {
            if (devices.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = BtText)
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxWidth()) {
                    items(devices) { device ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clickable { onDeviceSelected(device) },
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(device.name, fontWeight = FontWeight.Bold, color = TextMain)
                                Text(device.address, fontSize = 12.sp, color = TextMain.copy(alpha = 0.6f))
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = BtText)
            }
        },
        containerColor = BgMain
    )
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun AppHeader(viewModel: DashboardViewModel, permissionsState: MultiplePermissionsState, onSettingsClick: () -> Unit) {
    val connState by viewModel.connectionState.collectAsState()
    
    val statusText = when {
        connState == ConnectionState.CONNECTED -> "ESP32 Connected"
        connState == ConnectionState.CONNECTING -> "Connecting..."
        connState == ConnectionState.SCANNING -> "Scanning..."
        else -> "Disconnected"
    }
    
    val statusColor = when {
        connState == ConnectionState.CONNECTED -> Color(0xFF10B981) // Emerald
        connState == ConnectionState.CONNECTING || connState == ConnectionState.SCANNING -> Color(0xFFF59E0B)
        else -> Color(0xFFEF4444) // Red
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 24.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text("BioSync Monitor", fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = TextMain, letterSpacing = (-0.5).sp)
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(statusColor)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(statusText, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = statusColor.copy(alpha = 0.8f))
            }
        }
        
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                    .clickable { onSettingsClick() },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.Settings, contentDescription = "Settings", tint = TextMain.copy(alpha=0.7f))
            }
        }
    }
}

@Composable
fun HeartRateCard(viewModel: DashboardViewModel) {
    val hr by viewModel.heartRate.collectAsState()

    Card(
        modifier = Modifier.fillMaxSize(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = HrBg),
        border = BorderStroke(1.dp, HrBorder)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(20.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(HrIconBg)
                        .padding(10.dp)
                ) {
                    Icon(Icons.Rounded.Favorite, contentDescription = null, tint = HrText, modifier = Modifier.size(32.dp))
                }
                Text("LIVE", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = HrText.copy(alpha = 0.6f), letterSpacing = 1.sp)
            }
            
            Column {
                Text("$hr", fontSize = 60.sp, fontWeight = FontWeight.Bold, color = HrText, letterSpacing = (-2).sp)
                Text("BPM", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = HrText.copy(alpha = 0.8f))
            }
            
            Column {
                Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(HrText.copy(alpha = 0.1f)))
                Spacer(modifier = Modifier.height(16.dp))
                Text("Normal range for\nresting state.", fontSize = 11.sp, fontWeight = FontWeight.Medium, color = HrText.copy(alpha = 0.7f), lineHeight = 14.sp)
            }
        }
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun BluetoothCard(viewModel: DashboardViewModel, permissionsState: MultiplePermissionsState) {
    val connState by viewModel.connectionState.collectAsState()
    
    val statusText = when (connState) {
        ConnectionState.CONNECTED -> "Connected"
        ConnectionState.SCANNING -> "Scanning..."
        ConnectionState.CONNECTING -> "Connecting..."
        else -> "Bluetooth 5.0"
    }

    Card(
        modifier = Modifier.fillMaxSize().clickable {
            if (permissionsState.allPermissionsGranted) {
                if (connState == ConnectionState.SCANNING) {
                    viewModel.stopScan()
                } else if (connState == ConnectionState.DISCONNECTED) {
                    viewModel.startScan()
                } else {
                    viewModel.disconnect()
                }
            } else {
                permissionsState.launchMultiplePermissionRequest()
            }
        },
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = BtBg),
        border = BorderStroke(1.dp, BtBorder)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Filled.Bluetooth, contentDescription = null, tint = BtText, modifier = Modifier.size(24.dp))
                Text(statusText, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = BtText, letterSpacing = 0.5.sp)
            }
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                Text("Connection", fontSize = 12.sp, color = BtText.copy(alpha = 0.7f))
                
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.Bottom) {
                    Box(modifier = Modifier.width(4.dp).height(6.dp).clip(CircleShape).background(BtText))
                    Box(modifier = Modifier.width(4.dp).height(8.dp).clip(CircleShape).background(BtText))
                    Box(modifier = Modifier.width(4.dp).height(12.dp).clip(CircleShape).background(BtText))
                    Box(modifier = Modifier.width(4.dp).height(10.dp).clip(CircleShape).background(BtText.copy(alpha = 0.3f)))
                }
            }
        }
    }
}

@Composable
fun TemperatureCard(viewModel: DashboardViewModel) {
    val temp by viewModel.temperature.collectAsState()

    Card(
        modifier = Modifier.fillMaxSize(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = TempBg),
        border = BorderStroke(1.dp, TempBorder)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(20.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Icon(Icons.Rounded.Thermostat, contentDescription = null, tint = TempText, modifier = Modifier.size(32.dp))
            
            Column {
                Text(String.format("%.1f°C", temp), fontSize = 30.sp, fontWeight = FontWeight.Bold, color = TempText)
                Text("Body Temp", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = TempText.copy(alpha = 0.6f))
            }
        }
    }
}

@Composable
fun PulseWaveCard(viewModel: DashboardViewModel) {
    val currentPulse by viewModel.pulseValue.collectAsState()
    val pulseHistory = remember { mutableStateListOf<Float>() }
    
    LaunchedEffect(currentPulse) {
        if (currentPulse > 0f) {
            pulseHistory.add(currentPulse)
            if (pulseHistory.size > 100) {
                pulseHistory.removeAt(0)
            }
        }
    }

    Card(
        modifier = Modifier.fillMaxSize(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = WaveBg)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Rounded.ShowChart, contentDescription = null, tint = WaveText.copy(alpha = 0.9f), modifier = Modifier.size(20.dp))
                    Text("Pulse Stream", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = WaveText.copy(alpha = 0.9f))
                }
                Box(modifier = Modifier.background(Color.White.copy(alpha = 0.1f), CircleShape).padding(horizontal = 8.dp, vertical = 4.dp)) {
                    Text("Live history", fontSize = 10.sp, color = WaveText.copy(alpha = 0.5f))
                }
            }
            
            Box(modifier = Modifier.weight(1f).fillMaxWidth().padding(vertical = 16.dp), contentAlignment = Alignment.Center) {
                val lineColor = TempBg
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val path = Path()
                    val w = size.width
                    val h = size.height
                    val mid = h / 2f
                    
                    if (pulseHistory.isEmpty()) {
                        path.moveTo(0f, mid)
                        var x = 0f
                        val step = w / 8f
                        while (x < w) {
                            path.lineTo(x + step * 0.2f, mid)
                            path.lineTo(x + step * 0.3f, mid - h * 0.3f)
                            path.lineTo(x + step * 0.5f, mid + h * 0.4f)
                            path.lineTo(x + step * 0.6f, mid - h * 0.1f)
                            path.lineTo(x + step * 0.8f, mid)
                            path.lineTo(x + step, mid)
                            x += step
                        }
                    } else {
                        val min = pulseHistory.minOrNull() ?: 0f
                        val max = pulseHistory.maxOrNull() ?: 1f
                        val diff = if (max - min == 0f) 1f else (max - min)
                        
                        // Let's add some padding to the pulse wave so it doesn't touch the top/bottom exactly
                        val padding = h * 0.1f
                        val drawingHeight = h - padding * 2
                        
                        pulseHistory.forEachIndexed { index, value ->
                            val x = (w * index) / 100f
                            val normalized = (value - min) / diff
                            val y = padding + drawingHeight - (normalized * drawingHeight)
                            if (index == 0) path.moveTo(x, y)
                            else path.lineTo(x, y)
                        }
                    }
                    
                    drawPath(
                        path = path,
                        color = lineColor, // #eaddff
                        style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                    )
                }
            }
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Column {
                        Text("HIGH", fontSize = 10.sp, color = WaveText.copy(alpha = 0.4f), letterSpacing = 1.sp)
                        Text("84", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = WaveText)
                    }
                    Column {
                        Text("LOW", fontSize = 10.sp, color = WaveText.copy(alpha = 0.4f), letterSpacing = 1.sp)
                        Text("72", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = WaveText)
                    }
                }
                
                Button(
                    onClick = { viewModel.logCurrentMetrics() },
                    colors = ButtonDefaults.buttonColors(containerColor = TempBg, contentColor = WaveBg),
                    shape = CircleShape
                ) {
                    Text("LOG EVENT", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun BottomNavBar(currentRoute: String, onRouteSelected: (String) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant)
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically
    ) {
        NavItem(Icons.Rounded.GridView, "Dashboard", isSelected = currentRoute == "Dashboard") { onRouteSelected("Dashboard") }
        NavItem(Icons.Rounded.History, "Reports", isSelected = currentRoute == "Reports") { onRouteSelected("Reports") }
        NavItem(Icons.Rounded.Notifications, "Alerts", isSelected = currentRoute == "Alerts") { onRouteSelected("Alerts") }
        NavItem(Icons.Rounded.AccountCircle, "Profile", isSelected = currentRoute == "Profile") { onRouteSelected("Profile") }
    }
}

@Composable
fun NavItem(icon: ImageVector, label: String, isSelected: Boolean, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.clickable { onClick() }
    ) {
        val iconColor = if (isSelected) BtText else TextMain.copy(alpha = 0.6f)
        val bgColor = if (isSelected) BtBg else Color.Transparent
        
        Box(
            modifier = Modifier
                .clip(CircleShape)
                .background(bgColor)
                .padding(horizontal = 20.dp, vertical = 4.dp)
        ) {
            Icon(icon, contentDescription = label, tint = iconColor, modifier = Modifier.size(24.dp))
        }
        Text(
            label,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) TextMain else TextMain.copy(alpha = 0.6f)
        )
    }
}
