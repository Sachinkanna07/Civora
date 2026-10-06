package com.sachinkanna.civora.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.sachinkanna.civora.data.model.UserProfile
import com.sachinkanna.civora.data.model.UserRole
import com.sachinkanna.civora.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AuthUiState(val loading: Boolean = true, val profile: UserProfile? = null, val error: String? = null)

class AuthViewModel(private val repository: AuthRepository = AuthRepository()) : ViewModel() {
    private val _state = MutableStateFlow(AuthUiState())
    val state: StateFlow<AuthUiState> = _state.asStateFlow()

    init { sessionCheck() }

    fun sessionCheck() = viewModelScope.launch {
        _state.value = AuthUiState(loading = true)
        runCatching { repository.fetchCurrentProfile() }.onSuccess { _state.value = AuthUiState(false, it) }
            .onFailure { repository.logout(); _state.value = AuthUiState(false, error = message(it)) }
    }
    fun login(email: String, password: String) = submit { repository.login(email, password) }
    fun register(name: String, email: String, password: String, role: UserRole) = submit { repository.register(name, email, password, role) }
    fun logout() { repository.logout(); _state.value = AuthUiState(false) }
    private fun submit(action: suspend () -> UserProfile) = viewModelScope.launch {
        if (_state.value.loading) return@launch
        _state.value = AuthUiState(true)
        runCatching { action() }.onSuccess { _state.value = AuthUiState(false, it) }
            .onFailure { _state.value = AuthUiState(false, error = message(it)) }
    }
    fun clearError() { _state.value = _state.value.copy(error = null) }
    private fun message(t: Throwable) = when (t) {
        is com.google.firebase.auth.FirebaseAuthInvalidUserException, is com.google.firebase.auth.FirebaseAuthInvalidCredentialsException -> "Email or password is incorrect."
        is com.google.firebase.auth.FirebaseAuthUserCollisionException -> "An account already exists for this email."
        is com.google.firebase.auth.FirebaseAuthWeakPasswordException -> "Password is too weak. Use at least 6 characters."
        else -> t.message ?: "Something went wrong. Check your connection and try again."
    }
}
