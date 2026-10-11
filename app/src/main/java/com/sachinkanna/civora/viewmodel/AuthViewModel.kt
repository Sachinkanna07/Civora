package com.sachinkanna.civora.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sachinkanna.civora.data.model.UserProfile
import com.sachinkanna.civora.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AuthUiState(
    val loading: Boolean = true,
    val profile: UserProfile? = null,
    val error: String? = null,
    val notice: String? = null,
)

class AuthViewModel(private val repository: AuthRepository = AuthRepository()) : ViewModel() {
    private val _state = MutableStateFlow(AuthUiState())
    val state: StateFlow<AuthUiState> = _state.asStateFlow()

    init {
        sessionCheck()
    }

    fun sessionCheck() = viewModelScope.launch {
        _state.value = AuthUiState(loading = true)
        runCatching { repository.fetchCurrentProfile() }
            .onSuccess {
                _state.value = AuthUiState(false, it)
                if (it != null) registerPush()
            }
            .onFailure {
                repository.logout()
                _state.value = AuthUiState(false, error = message(it))
            }
    }

    fun login(email: String, password: String) = submit { repository.login(email, password) }

    fun register(name: String, email: String, password: String) = submit {
        repository.register(name, email, password)
    }

    fun refreshProfile() = viewModelScope.launch {
        runCatching { repository.refreshVerifiedProfile() }
            .onSuccess { _state.value = _state.value.copy(profile = it); if(it!=null)registerPush() }
            .onFailure {
                _state.value = _state.value.copy(error = "Profile could not be refreshed.")
            }
    }

    fun sendVerificationEmail() = viewModelScope.launch {
        if(_state.value.loading)return@launch
        _state.value=_state.value.copy(loading=true,error=null,notice=null)
        runCatching {repository.sendVerificationEmail()}
            .onSuccess {_state.value=_state.value.copy(loading=false,notice="Verification email sent. Check your inbox and spam folder.")}
            .onFailure {_state.value=_state.value.copy(loading=false,error="Verification email could not be sent. Wait a moment and try again.")}
    }
    fun checkAdmission() = viewModelScope.launch {
        if(_state.value.loading)return@launch
        _state.value=_state.value.copy(loading=true,error=null,notice=null)
        runCatching {repository.refreshVerifiedProfile()}
            .onSuccess {_state.value=AuthUiState(false,it,notice=if(it?.emailVerified==false)"Your email is still unverified. Complete the link in your inbox first."else null); if(it!=null)registerPush()}
            .onFailure {_state.value=_state.value.copy(loading=false,error="Campus access could not be checked. Check your connection and retry.")}
    }
    fun deletePendingAccount(password: String) = viewModelScope.launch {
        if(_state.value.loading)return@launch
        _state.value=_state.value.copy(loading=true,error=null)
        runCatching {repository.deleteAccount(password)}
            .onSuccess {_state.value=AuthUiState(false)}
            .onFailure {_state.value=_state.value.copy(loading=false,error="Account deletion failed. Confirm your password and retry.")}
    }

    fun logout() {
        val context=com.google.firebase.FirebaseApp.getInstance().applicationContext
        com.sachinkanna.civora.data.datasource.TripTrackingService.stop(context)
        context.getSystemService(android.app.NotificationManager::class.java).cancelAll()
        com.sachinkanna.civora.DiagnosticsPreferences.update(context,false)
        com.sachinkanna.civora.AppNavigation.destination.value=null
        viewModelScope.launch {
            runCatching { kotlinx.coroutines.withTimeout(3000) { repository.unregisterDevice() } }
            repository.logout()
            _state.value = AuthUiState(false)
        }
    }

    private fun submit(action: suspend () -> UserProfile) = viewModelScope.launch {
        if (_state.value.loading) return@launch
        _state.value = AuthUiState(true)
        runCatching { action() }
            .onSuccess {
                _state.value = AuthUiState(false, it)
                registerPush()
            }
            .onFailure { _state.value = AuthUiState(false, error = message(it)) }
    }

    private fun registerPush() {
        // FCM has no local emulator. Demo credentials cannot register real push tokens.
        if (com.google.firebase.FirebaseApp.getInstance().options.projectId?.startsWith("demo-") == true) return
        val profile=_state.value.profile ?: return
        val context=com.google.firebase.FirebaseApp.getInstance().applicationContext
        val permitted=android.os.Build.VERSION.SDK_INT<33||androidx.core.content.ContextCompat.checkSelfPermission(context,android.Manifest.permission.POST_NOTIFICATIONS)==android.content.pm.PackageManager.PERMISSION_GRANTED
        if(!profile.campusAdmitted||!profile.notificationsEnabled||!permitted) {
            viewModelScope.launch {runCatching {kotlinx.coroutines.withTimeout(3000){repository.unregisterDevice()}}}
            return
        }
        runCatching {
            val messaging=com.google.firebase.messaging.FirebaseMessaging.getInstance()
            messaging.isAutoInitEnabled=true
            messaging.token.addOnSuccessListener {
                com.sachinkanna.civora.data.datasource.CivoraMessagingService.registerDevice(it)
            }.addOnFailureListener {
                android.util.Log.w("CivoraPush", "Push registration unavailable", it)
            }
        }.onFailure {
            android.util.Log.w("CivoraPush", "Push initialization unavailable", it)
        }
    }

    fun clearError() {
        _state.value = _state.value.copy(error = null)
    }

    private fun message(t: Throwable) =
        when (t) {
            is com.google.firebase.auth.FirebaseAuthInvalidUserException,
            is com.google.firebase.auth.FirebaseAuthInvalidCredentialsException ->
                "Email or password is incorrect."
            is com.google.firebase.auth.FirebaseAuthUserCollisionException ->
                "An account already exists for this email."
            is com.google.firebase.auth.FirebaseAuthWeakPasswordException ->
                "Password is too weak. Use at least 8 characters."
            else -> "Could not connect to Civora. Check your connection and try again."
        }
}
