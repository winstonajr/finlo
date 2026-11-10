package com.seunome.finlo.presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.seunome.finlo.core.network.ApiService
import com.seunome.finlo.core.network.dto.GoogleLoginRequestDto
import com.seunome.finlo.core.network.dto.LoginRequestDto
import com.seunome.finlo.core.network.dto.RegisterRequestDto
import com.seunome.finlo.core.security.SecureStorage
import com.seunome.finlo.core.sync.SyncManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class AuthUiState {
    object Idle : AuthUiState()
    object Loading : AuthUiState()
    object Success : AuthUiState()
    data class Error(val message: String) : AuthUiState()
}

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val apiService: ApiService,
    private val secureStorage: SecureStorage,
    private val syncManager: SyncManager
) : ViewModel() {

    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun login(email: String, pass: String) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            try {
                val resp = apiService.login(LoginRequestDto(email, pass))
                if (resp.isSuccessful && resp.body() != null) {
                    val auth = resp.body()!!
                    secureStorage.saveAuthTokens(auth.accessToken, auth.refreshToken, auth.userId, auth.email)
                    syncManager.sync()
                    _uiState.value = AuthUiState.Success
                } else {
                    _uiState.value = AuthUiState.Error("Falha no login: credenciais inválidas.")
                }
            } catch (e: Exception) {
                _uiState.value = AuthUiState.Error("Erro de conexão: ${e.message}")
            }
        }
    }

    fun register(email: String, pass: String) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            try {
                val resp = apiService.register(RegisterRequestDto(email, pass))
                if (resp.isSuccessful && resp.body() != null) {
                    val auth = resp.body()!!
                    secureStorage.saveAuthTokens(auth.accessToken, auth.refreshToken, auth.userId, auth.email)
                    syncManager.sync()
                    _uiState.value = AuthUiState.Success
                } else {
                    _uiState.value = AuthUiState.Error("Falha no cadastro. Verifique os dados.")
                }
            } catch (e: Exception) {
                _uiState.value = AuthUiState.Error("Erro de conexão: ${e.message}")
            }
        }
    }

    fun googleLogin(idToken: String) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            try {
                val resp = apiService.googleLogin(GoogleLoginRequestDto(idToken))
                if (resp.isSuccessful && resp.body() != null) {
                    val auth = resp.body()!!
                    secureStorage.saveAuthTokens(auth.accessToken, auth.refreshToken, auth.userId, auth.email)
                    syncManager.sync()
                    _uiState.value = AuthUiState.Success
                } else {
                    _uiState.value = AuthUiState.Error("Falha na autenticação Google.")
                }
            } catch (e: Exception) {
                _uiState.value = AuthUiState.Error("Erro: ${e.message}")
            }
        }
    }

    fun useLocalMode() {
        viewModelScope.launch {
            secureStorage.clearAuth()
            _uiState.value = AuthUiState.Success
        }
    }
}
