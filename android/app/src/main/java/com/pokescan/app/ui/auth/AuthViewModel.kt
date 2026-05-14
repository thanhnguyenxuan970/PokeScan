package com.pokescan.app.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.tasks.Task
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

    private val _events = MutableSharedFlow<AuthEvent>(replay = 0)
    val events: SharedFlow<AuthEvent> = _events

    fun handleSignInResult(task: Task<GoogleSignInAccount>) {
        val account = try {
            task.result
        } catch (e: Exception) {
            _state.value = AuthState.Error(e.message ?: "Sign-in failed")
            return
        }
        val idToken = account?.idToken
        if (idToken == null) {
            _state.value = AuthState.Error("Sign-in config error: idToken null. Check Firebase OAuth client setup.")
            return
        }
        viewModelScope.launch {
            _state.value = AuthState.Loading
            try {
                authRepository.signInWithGoogle(idToken)
                _state.value = AuthState.Idle
                _events.emit(AuthEvent.NavigateToScanner)
            } catch (e: Exception) {
                _state.value = AuthState.Error(e.message ?: "Authentication failed")
            }
        }
    }

    fun clearError() {
        _state.value = AuthState.Idle
    }
}
