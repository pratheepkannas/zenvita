package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class AuthViewModel(application: Application) : AndroidViewModel(application) {

    init {
        try {
            if (FirebaseApp.getApps(application).isEmpty()) {
                val options = FirebaseOptions.Builder()
                    .setProjectId("healthcare-0059")
                    .setApplicationId(BuildConfig.FIREBASE_APP_ID)
                    .setApiKey(BuildConfig.FIREBASE_API_KEY)
                    .build()
                FirebaseApp.initializeApp(application, options)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private val db: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }

    private val _authState = MutableStateFlow<AuthState>(AuthState.Initial)
    val authState = _authState.asStateFlow()

    private val _userProfile = MutableStateFlow<UserProfile?>(null)
    val userProfile = _userProfile.asStateFlow()

    fun fetchUserProfile() {
        val uid = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            try {
                val document = db.collection("users").document(uid).get().await()
                if (document.exists()) {
                    val idName = document.getString("idName") ?: ""
                    val firstName = document.getString("firstName") ?: ""
                    val lastName = document.getString("lastName") ?: ""
                    val email = document.getString("email") ?: auth.currentUser?.email ?: ""
                    val number = document.getString("number") ?: ""
                    val profileImageUri = document.getString("profileImageUri")
                    _userProfile.value = UserProfile(idName, firstName, lastName, email, number, profileImageUri)
                } else {
                    _userProfile.value = UserProfile(email = auth.currentUser?.email ?: "")
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _userProfile.value = UserProfile(email = auth.currentUser?.email ?: "")
            }
        }
    }

    fun updateProfileImage(uri: String) {
        val uid = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            try {
                db.collection("users").document(uid).update("profileImageUri", uri).await()
                _userProfile.value = _userProfile.value?.copy(profileImageUri = uri)
            } catch (e: Exception) {
                e.printStackTrace()
                // Update locally anyway if firestore fails
                _userProfile.value = _userProfile.value?.copy(profileImageUri = uri)
            }
        }
    }

    fun logout() {
        auth.signOut()
        _userProfile.value = null
        _authState.value = AuthState.Unauthenticated
    }

    fun checkLoginStatus() {
        if (auth.currentUser != null) {
            if (auth.currentUser?.isEmailVerified == true) {
                _authState.value = AuthState.Authenticated
            } else {
                _authState.value = AuthState.NeedsVerification
            }
        } else {
            _authState.value = AuthState.Unauthenticated
        }
    }

    fun login(email: String, pass: String) {
        if (email.isBlank() || pass.isBlank()) {
            _authState.value = AuthState.Error("Please fill all fields")
            return
        }
        _authState.value = AuthState.Loading
        viewModelScope.launch {
            try {
                val result = auth.signInWithEmailAndPassword(email, pass).await()
                if (result.user?.isEmailVerified == true) {
                    _authState.value = AuthState.Authenticated
                } else {
                    _authState.value = AuthState.NeedsVerification
                }
            } catch (e: Exception) {
                _authState.value = AuthState.Error(e.message ?: "Login failed")
            }
        }
    }

    fun resendVerificationEmail() {
        viewModelScope.launch {
            try {
                auth.currentUser?.sendEmailVerification()?.await()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun checkEmailVerification() {
        viewModelScope.launch {
            try {
                auth.currentUser?.reload()?.await()
                if (auth.currentUser?.isEmailVerified == true) {
                    _authState.value = AuthState.Authenticated
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun sendPasswordResetEmail(email: String) {
        if (email.isBlank()) return
        viewModelScope.launch {
            try {
                auth.sendPasswordResetEmail(email).await()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun signUp(
        idName: String,
        firstName: String,
        lastName: String,
        email: String,
        number: String,
        pass: String,
        confirmPass: String
    ) {
        if (idName.isBlank() || firstName.isBlank() || lastName.isBlank() || email.isBlank() || number.isBlank() || pass.isBlank()) {
            _authState.value = AuthState.Error("Please fill all fields")
            return
        }
        if (pass != confirmPass) {
            _authState.value = AuthState.Error("Passwords do not match")
            return
        }

        _authState.value = AuthState.Loading
        viewModelScope.launch {
            try {
                val result = auth.createUserWithEmailAndPassword(email, pass).await()
                result.user?.sendEmailVerification()?.await()
                result.user?.uid?.let { uid ->
                    val userMap = hashMapOf(
                        "idName" to idName,
                        "firstName" to firstName,
                        "lastName" to lastName,
                        "email" to email,
                        "number" to number
                    )
                    try {
                        db.collection("users").document(uid).set(userMap).await()
                    } catch (e: Exception) {
                        e.printStackTrace()
                        _authState.value = AuthState.Error("Account created, but database write failed.\nPlease update your Firestore Security Rules.\nError: ${e.message}")
                        return@launch
                    }
                }
                _authState.value = AuthState.NeedsVerification
            } catch (e: Exception) {
                _authState.value = AuthState.Error(e.message ?: "Sign up failed")
            }
        }
    }

    fun resetState() {
        if (auth.currentUser != null) {
            auth.signOut()
        }
        _authState.value = AuthState.Unauthenticated
    }
}

sealed class AuthState {
    object Initial : AuthState()
    object Loading : AuthState()
    object Authenticated : AuthState()
    object Unauthenticated : AuthState()
    object NeedsVerification : AuthState()
    data class Error(val message: String) : AuthState()
}
