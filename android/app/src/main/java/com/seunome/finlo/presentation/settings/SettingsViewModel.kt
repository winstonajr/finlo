package com.seunome.finlo.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.seunome.finlo.core.database.AppDatabase
import com.seunome.finlo.core.export.BackupManager
import com.seunome.finlo.core.network.ApiService
import com.seunome.finlo.core.security.PinManager
import com.seunome.finlo.core.security.SecureStorage
import com.seunome.finlo.core.sync.SyncManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val isLocalMode: Boolean = true,
    val currency: String = "BRL",
    val theme: String = "SYSTEM",
    val isPinEnabled: Boolean = false,
    val isBiometricEnabled: Boolean = false,
    val userEmail: String? = null
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val secureStorage: SecureStorage,
    private val pinManager: PinManager,
    private val syncManager: SyncManager,
    private val backupManager: BackupManager,
    private val apiService: ApiService,
    private val database: AppDatabase
) : ViewModel() {

    val uiState: StateFlow<SettingsUiState> = combine(
        secureStorage.isLocalModeFlow,
        secureStorage.currencyFlow,
        secureStorage.themeFlow,
        secureStorage.isPinEnabledFlow,
        secureStorage.isBiometricEnabledFlow,
        secureStorage.userIdFlow
    ) { isLocal, currency, theme, pin, bio, userId ->
        SettingsUiState(
            isLocalMode = isLocal,
            currency = currency,
            theme = theme,
            isPinEnabled = pin,
            isBiometricEnabled = bio,
            userEmail = userId
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SettingsUiState())

    fun setCurrency(currency: String) {
        viewModelScope.launch { secureStorage.setCurrency(currency) }
    }

    fun setTheme(theme: String) {
        viewModelScope.launch { secureStorage.setTheme(theme) }
    }

    fun setPin(pin: String) {
        viewModelScope.launch { pinManager.setPin(pin) }
    }

    fun removePin() {
        viewModelScope.launch { pinManager.removePin() }
    }

    fun setBiometricEnabled(enabled: Boolean) {
        viewModelScope.launch { secureStorage.setBiometricEnabled(enabled) }
    }

    fun triggerSync() {
        viewModelScope.launch { syncManager.sync() }
    }

    fun createBackupJson(onResult: (String) -> Unit) {
        viewModelScope.launch {
            val json = backupManager.createBackupJson()
            onResult(json)
        }
    }

    fun restoreBackupJson(json: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val success = backupManager.restoreBackupJson(json)
            onResult(success)
        }
    }

    fun logout() {
        viewModelScope.launch {
            secureStorage.clearAuth()
        }
    }

    fun deleteAccountAndAllData(onComplete: () -> Unit) {
        viewModelScope.launch {
            try {
                if (!uiState.value.isLocalMode) {
                    apiService.deleteAccount()
                }
            } catch (e: Exception) {
                // Ignore network errors on local wiping
            } finally {
                database.accountDao().deleteAll()
                database.categoryDao().deleteAll()
                database.transactionDao().deleteAll()
                database.goalDao().deleteAll()
                database.recurringRuleDao().deleteAll()
                database.syncChangeDao().clear()
                secureStorage.clearAuth()
                onComplete()
            }
        }
    }
}
