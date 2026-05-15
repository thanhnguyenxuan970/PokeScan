package com.pokescan.app.ui.auth

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.tasks.Task
import com.pokescan.app.BuildConfig
import com.pokescan.app.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    data class Error(val message: String) : AuthState()
}

sealed class AuthEvent {
    object NavigateToScanner : AuthEvent()
}

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    val googleSignInClient: GoogleSignInClient
) : ViewModel() {

    private val _state = MutableStateFlow<AuthState>(AuthState.Idle)
    val state: StateFlow<AuthState> = _state

    private val _events = MutableSharedFlow<AuthEvent>(replay = 0, extraBufferCapacity = 1)
    val events: SharedFlow<AuthEvent> = _events

    fun handleSignInResult(task: Task<GoogleSignInAccount>) {
        val account = try {
            task.result
        } catch (e: Exception) {
            Log.e("AuthVM", "task.result threw: ${e.message}")
            val isNetworkError = listOf("CLEARTEXT", "Unable to resolve host",
                "Failed to connect", "timeout", "SocketException")
                .any { e.message?.contains(it, ignoreCase = true) == true }
            _state.value = AuthState.Error(
                if (isNetworkError) "Unable to connect. Check your connection and try again."
                else e.message ?: "Sign-in failed"
            )
            return
        }
        val idToken = account?.idToken
        Log.d("AuthVM", "idToken null=${idToken == null}")
        if (idToken == null) {
            _state.value = AuthState.Error(
                if (BuildConfig.DEBUG) "Firebase not configured — set up google-services.json"
                else "Sign-in failed. Please try again."
            )
            return
        }
        viewModelScope.launch {
            _state.value = AuthState.Loading
            try {
                Log.d("AuthVM", "calling backend signInWithGoogle")
                authRepository.signInWithGoogle(idToken)
                Log.d("AuthVM", "backend OK, emitting NavigateToScanner")
                _state.value = AuthState.Idle
                _events.emit(AuthEvent.NavigateToScanner)
            } catch (e: Exception) {
                Log.e("AuthVM", "backend failed: ${e.message}")
                val isNetworkError = listOf("CLEARTEXT", "Unable to resolve host",
                    "Failed to connect", "timeout", "SocketException")
                    .any { e.message?.contains(it, ignoreCase = true) == true }
                val msg = when {
                    isNetworkError -> "Unable to connect. Check your connection and try again."
                    else -> e.message ?: "Authentication failed"
                }
                _state.value = AuthState.Error(msg)
            }
        }
    }

    fun clearError() {
        _state.value = AuthState.Idle
    }
}
