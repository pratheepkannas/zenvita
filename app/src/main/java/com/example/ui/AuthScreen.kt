package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.i18n.phonenumbers.NumberParseException
import com.google.i18n.phonenumbers.PhoneNumberUtil
import java.util.Locale

data class CountryInfo(val code: String, val name: String, val dialCode: String, val flag: String)

fun getFlagEmoji(countryCode: String): String {
    if (countryCode.length != 2) return "🌐"
    val firstLetter = Character.codePointAt(countryCode, 0) - 0x41 + 0x1F1E6
    val secondLetter = Character.codePointAt(countryCode, 1) - 0x41 + 0x1F1E6
    return String(Character.toChars(firstLetter)) + String(Character.toChars(secondLetter))
}

fun getCountries(phoneUtil: PhoneNumberUtil): List<CountryInfo> {
    return phoneUtil.supportedRegions.map { region ->
        val locale = Locale("", region)
        CountryInfo(
            code = region,
            name = locale.displayCountry,
            dialCode = "+${phoneUtil.getCountryCodeForRegion(region)}",
            flag = getFlagEmoji(region)
        )
    }.sortedBy { it.name }.filter { it.name.isNotEmpty() }
}

@Composable
fun AuthNavigator(authViewModel: AuthViewModel) {
    val authState by authViewModel.authState.collectAsState()
    var isSignUp by remember { mutableStateOf(false) }

    Scaffold(containerColor = MaterialTheme.colorScheme.background) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.Center
        ) {
            when (val state = authState) {
                is AuthState.Initial, is AuthState.Authenticated -> {
                    // Empty or loading state until transition
                }
                is AuthState.Loading -> {
                    CircularProgressIndicator()
                }
                is AuthState.Error -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Text(state.message, color = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { authViewModel.resetState() }) {
                            Text("Go Back")
                        }
                    }
                }
                is AuthState.Unauthenticated -> {
                    if (isSignUp) {
                        SignUpScreen(
                            onSignUp = { idName, firstName, lastName, email, number, pass, confirmPass ->
                                authViewModel.signUp(
                                    idName, firstName, lastName, email, number, pass, confirmPass
                                )
                            },
                            onNavigateToLogin = { isSignUp = false }
                        )
                    } else {
                        LoginScreen(
                            onLogin = { email, pass ->
                                authViewModel.login(email, pass)
                            },
                            onNavigateToSignUp = { isSignUp = true }
                        )
                    }
                }
                is AuthState.NeedsVerification -> {
                    VerificationScreen(
                        onResend = { authViewModel.resendVerificationEmail() },
                        onCheck = { authViewModel.checkEmailVerification() },
                        onBack = { authViewModel.resetState() }
                    )
                }
            }
        }
    }
}

@Composable
fun VerificationScreen(
    onResend: () -> Unit,
    onCheck: () -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Verify Your Email", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Text(
            text = "We have sent a verification code link to your email address. Please verify to continue.",
            fontSize = 16.sp,
            color = Color.Gray,
            modifier = Modifier.padding(bottom = 16.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )

        Button(
            onClick = onCheck,
            modifier = Modifier.fillMaxWidth().height(50.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Text("I've Verified, Continue", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }

        OutlinedButton(
            onClick = onResend,
            modifier = Modifier.fillMaxWidth().height(50.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Resend Email", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        }

        TextButton(onClick = onBack) {
            Text("Back to Login", color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
fun LoginScreen(
    onLogin: (String, String) -> Unit,
    onNavigateToSignUp: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Welcome Back", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Text("Login to continue", fontSize = 16.sp, color = Color.Gray)

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email ID") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
        )

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = { onLogin(email, password) },
            modifier = Modifier.fillMaxWidth().height(50.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Text("Login", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }

        TextButton(onClick = onNavigateToSignUp) {
            Text("Don't have an account? Create one", color = MaterialTheme.colorScheme.primary)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignUpScreen(
    onSignUp: (String, String, String, String, String, String, String) -> Unit,
    onNavigateToLogin: () -> Unit
) {
    var idName by remember { mutableStateOf("") }
    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var number by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    
    val phoneUtil = remember { PhoneNumberUtil.getInstance() }
    val countries = remember { getCountries(phoneUtil) }
    var selectedCountry by remember { mutableStateOf(countries.find { it.code == "US" } ?: countries.firstOrNull() ?: CountryInfo("US", "United States", "+1", "🇺🇸")) }
    var expanded by remember { mutableStateOf(false) }
    var numberError by remember { mutableStateOf<String?>(null) }

    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(scrollState)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Create Account", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Text("Enter your details to register", fontSize = 16.sp, color = Color.Gray)

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = idName,
            onValueChange = { idName = it },
            label = { Text("ID Name") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        
        OutlinedTextField(
            value = firstName,
            onValueChange = { firstName = it },
            label = { Text("First Name") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        OutlinedTextField(
            value = lastName,
            onValueChange = { lastName = it },
            label = { Text("Last Name") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email ID") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
        )

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded },
                modifier = Modifier.weight(0.35f)
            ) {
                OutlinedTextField(
                    value = "${selectedCountry.flag} ${selectedCountry.dialCode}",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Code") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier.menuAnchor(),
                    singleLine = true
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    countries.forEach { selectionOption ->
                        DropdownMenuItem(
                            text = { Text("${selectionOption.flag} ${selectionOption.name} (${selectionOption.dialCode})") },
                            onClick = {
                                selectedCountry = selectionOption
                                expanded = false
                            }
                        )
                    }
                }
            }

            OutlinedTextField(
                value = number,
                onValueChange = { 
                    number = it 
                    numberError = null
                },
                label = { Text("Phone Number") },
                modifier = Modifier.weight(0.65f),
                singleLine = true,
                isError = numberError != null,
                supportingText = { if (numberError != null) Text(numberError!!) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
            )
        }

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
        )
        
        OutlinedTextField(
            value = confirmPassword,
            onValueChange = { confirmPassword = it },
            label = { Text("Confirm Password") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = { 
                var isValid = false
                var fullNumber = number
                try {
                    val parsed = phoneUtil.parse(number, selectedCountry.code)
                    isValid = phoneUtil.isValidNumber(parsed)
                    if (isValid) {
                        fullNumber = phoneUtil.format(parsed, PhoneNumberUtil.PhoneNumberFormat.E164)
                    }
                } catch (e: NumberParseException) {
                    isValid = false
                }
                
                if (!isValid) {
                    numberError = "Invalid number for ${selectedCountry.name}"
                    return@Button
                }
                
                onSignUp(idName, firstName, lastName, email, fullNumber, password, confirmPassword) 
            },
            modifier = Modifier.fillMaxWidth().height(50.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Text("Sign Up", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }

        TextButton(onClick = onNavigateToLogin) {
            Text("Already have an account? Login", color = MaterialTheme.colorScheme.primary)
        }
    }
}
