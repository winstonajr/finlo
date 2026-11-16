package com.seunome.finlo.presentation.lock

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.seunome.finlo.core.security.PinManager
import com.seunome.finlo.core.security.SecureStorage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class LockState {
    object Locked : LockState()
    object Unlocked : LockState()
    object Error : LockState()
}

@HiltViewModel
class PinLockViewModel @Inject constructor(
    private val pinManager: PinManager,
    private val secureStorage: SecureStorage
) : ViewModel() {

    private val _lockState = MutableStateFlow<LockState>(LockState.Locked)
    val lockState: StateFlow<LockState> = _lockState.asStateFlow()

    fun verifyPin(pin: String) {
        viewModelScope.launch {
            val valid = pinManager.verifyPin(pin)
            if (valid) {
                _lockState.value = LockState.Unlocked
            } else {
                _lockState.value = LockState.Error
            }
        }
    }

    fun unlockByBiometrics() {
        _lockState.value = LockState.Unlocked
    }

    suspend fun isLockRequired(): Boolean {
        return secureStorage.isPinEnabledFlow.first()
    }
}
