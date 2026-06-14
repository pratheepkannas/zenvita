package com.example.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    authViewModel: AuthViewModel,
    themeViewModel: ThemeViewModel,
    onBack: () -> Unit
) {
    val userProfile by authViewModel.userProfile.collectAsState()
    val isDarkTheme by themeViewModel.isDarkMode.collectAsState()
    
    var showTermsDialog by remember { mutableStateOf(false) }
    var showPrivacyDialog by remember { mutableStateOf(false) }
    var showChangePasswordDialog by remember { mutableStateOf(false) }
    var passwordResetSent by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        authViewModel.fetchUserProfile()
    }

    if (showTermsDialog) {
        val scrollState = rememberScrollState()
        AlertDialog(
            onDismissRequest = { showTermsDialog = false },
            title = { Text("Terms and Conditions") },
            text = { 
                Column(modifier = Modifier.verticalScroll(scrollState).padding(4.dp)) {
                    Text("""1. Acceptance of Terms

By downloading, accessing, or using the application, the user agrees to these Terms and Conditions.

2. Eligibility
Users must be at least 18 years old.
Minors may use the application only with parental or guardian consent.

3. Medical Disclaimer
The application is intended for informational and monitoring purposes only.
The application does not provide medical diagnosis, treatment, or emergency services.
Users should consult qualified healthcare professionals before making medical decisions.

4. User Account
Users must provide accurate information.
Users are responsible for maintaining account confidentiality.
Users are responsible for all activities performed through their accounts.

5. Health Data Collection
The application may collect:
Name, age, gender
Heart rate readings
Temperature readings
Medical history
Device information

Collected data is used only to provide healthcare-related services and improve application performance.

6. Privacy and Security
User health information is encrypted and securely stored.
Data will not be shared with third parties without user consent except when required by law.
Users may request account deletion and data removal.

7. Bluetooth and IoT Devices
The application may connect to wearable devices and sensors such as ESP32, pulse oximeters, heart rate sensors, and temperature sensors.
Users are responsible for ensuring proper device operation.
The company is not responsible for incorrect readings caused by hardware failures.

8. User Responsibilities
Users agree not to:
Provide false information.
Attempt unauthorized access.
Modify or reverse-engineer the application.
Use the application for illegal purposes.

9. Emergency Situations
The application is not intended for emergency medical care.
In case of emergency, users should immediately contact local emergency services or healthcare providers.

10. Limitation of Liability
The company shall not be liable for:
Medical decisions made by users.
Loss caused by inaccurate sensor data.
Service interruptions.
Device malfunctions.

11. Intellectual Property
All application content, logos, software, and designs remain the property of the company and may not be copied without permission.

12. Termination
The company reserves the right to suspend or terminate accounts that violate these terms.

13. Changes to Terms
The company may update these Terms and Conditions at any time. Continued use of the application indicates acceptance of the revised terms.""")
                }
            },
            confirmButton = { TextButton(onClick = { showTermsDialog = false }) { Text("OK") } }
        )
    }

    if (showPrivacyDialog) {
        val scrollState = rememberScrollState()
        AlertDialog(
            onDismissRequest = { showPrivacyDialog = false },
            title = { Text("Data Privacy") },
            text = { 
                Column(modifier = Modifier.verticalScroll(scrollState).padding(4.dp)) {
                    Text("""# Data Privacy Policy

## Data Collection

Our application may collect personal and health-related information, including but not limited to:

* Name, email address, and contact details
* Heart rate measurements
* Body temperature readings
* Device and application usage information

## Data Protection

We are committed to protecting your privacy. All user data is stored securely using industry-standard security measures, including encryption and secure authentication mechanisms.

## Data Sharing

We do not sell, rent, trade, or share your personal information or health data with any third-party companies, advertisers, marketing agencies, or external organizations for commercial purposes.

Your data will only be accessed:

* To provide and improve the services offered by the application.
* With your explicit consent.
* When required by applicable laws, regulations, or legal processes.

## Third-Party Services

If the application uses services such as Firebase or cloud hosting providers, these services process data solely for application functionality and are bound by their own privacy and security obligations.

## User Rights

Users have the right to:

* Access their personal data.
* Request correction of inaccurate information.
* Request deletion of their account and associated data.
* Withdraw consent for data processing where applicable.

## Data Retention

User data is retained only for as long as necessary to provide services or comply with legal obligations. Upon account deletion, personal data will be removed within a reasonable period unless otherwise required by law.

By using this application, you acknowledge and agree to this Data Privacy Policy.""")
                }
            },
            confirmButton = { TextButton(onClick = { showPrivacyDialog = false }) { Text("OK") } }
        )
    }

    if (showChangePasswordDialog) {
        AlertDialog(
            onDismissRequest = { 
                showChangePasswordDialog = false 
                passwordResetSent = false
            },
            title = { Text("Change Password") },
            text = { 
                if (passwordResetSent) {
                    Text("A verification code and password reset link have been sent to ${userProfile?.email}. Please check your inbox.")
                } else {
                    Text("Are you sure you want to change your password? We will send a verification link to your email id: ${userProfile?.email}")
                }
            },
            confirmButton = { 
                if (!passwordResetSent) {
                    TextButton(onClick = { 
                        authViewModel.sendPasswordResetEmail(userProfile?.email ?: "")
                        passwordResetSent = true
                    }) { 
                        Text("Send Email") 
                    }
                } else {
                    TextButton(onClick = { 
                        showChangePasswordDialog = false
                        passwordResetSent = false
                    }) { 
                        Text("OK", color = MaterialTheme.colorScheme.primary) 
                    }
                }
            },
            dismissButton = {
                if (!passwordResetSent) {
                    TextButton(onClick = { showChangePasswordDialog = false }) { 
                        Text("Cancel") 
                    }
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.Rounded.ArrowBack, contentDescription = "Back")
            }
            Text("Settings", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        }

        Divider()

        Text("Theme Preferences", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Dark Mode", fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
            Switch(
                checked = isDarkTheme,
                onCheckedChange = { themeViewModel.setDarkMode(it) }
            )
        }

        Divider()

        Text("User Details", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Name: ${userProfile?.firstName} ${userProfile?.lastName}", fontSize = 16.sp)
            Text("Email: ${userProfile?.email}", fontSize = 16.sp)
            Text("Phone: ${userProfile?.number}", fontSize = 16.sp)
            
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(
                onClick = { showChangePasswordDialog = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Change Password")
            }
        }

        Divider()

        Text("About & Legal", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(
                "Terms and Conditions",
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showTermsDialog = true }
                    .padding(vertical = 8.dp)
            )
            Text(
                "Data Privacy",
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showPrivacyDialog = true }
                    .padding(vertical = 8.dp)
            )
        }
    }
}
