package com.example.nova.data

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class UserProfile(
    val uid: String,
    val email: String?,
    val displayName: String?,
    val isAnonymous: Boolean = false,
    val isSignedIn: Boolean = false
)

class AuthManager(private val context: Context) {

    private val _userProfile = MutableStateFlow<UserProfile?>(null)
    val userProfile: StateFlow<UserProfile?> = _userProfile.asStateFlow()

    private val _isFirebaseConfigured = MutableStateFlow(false)
    val isFirebaseConfigured: StateFlow<Boolean> = _isFirebaseConfigured.asStateFlow()

    init {
        checkAuthStatus()
    }

    fun checkAuthStatus() {
        try {
            if (FirebaseApp.getApps(context).isNotEmpty()) {
                _isFirebaseConfigured.value = true
                val auth = FirebaseAuth.getInstance()
                val current = auth.currentUser
                if (current != null) {
                    _userProfile.value = UserProfile(
                        uid = current.uid,
                        email = current.email,
                        displayName = current.displayName ?: current.email?.substringBefore('@') ?: "User",
                        isAnonymous = current.isAnonymous,
                        isSignedIn = true
                    )
                } else {
                    _userProfile.value = null
                }
            } else {
                _isFirebaseConfigured.value = false
            }
        } catch (_: Exception) {
            _isFirebaseConfigured.value = false
        }
    }

    fun signOut() {
        try {
            if (_isFirebaseConfigured.value) {
                FirebaseAuth.getInstance().signOut()
                _userProfile.value = null
            }
        } catch (_: Exception) {}
    }
}
