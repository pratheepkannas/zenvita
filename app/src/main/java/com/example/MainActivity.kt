package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.AuthNavigator
import com.example.ui.AuthViewModel
import com.example.ui.AuthState
import com.example.ui.DashboardScreen
import com.example.ui.theme.MyApplicationTheme

import androidx.activity.viewModels
import com.example.ui.ThemeViewModel

class MainActivity : ComponentActivity() {
  private val themeViewModel: ThemeViewModel by viewModels()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      val isDarkTheme by themeViewModel.isDarkMode.collectAsState()
      MyApplicationTheme(darkTheme = isDarkTheme) {
        AppNavigation(themeViewModel = themeViewModel)
      }
    }
  }
}

@Composable
fun AppNavigation(themeViewModel: ThemeViewModel) {
    val authViewModel: AuthViewModel = viewModel()
    val authState by authViewModel.authState.collectAsState()

    LaunchedEffect(Unit) {
        authViewModel.checkLoginStatus()
    }

    if (authState is AuthState.Authenticated) {
        DashboardScreen(authViewModel = authViewModel, themeViewModel = themeViewModel)
    } else {
        AuthNavigator(authViewModel = authViewModel)
    }
}

