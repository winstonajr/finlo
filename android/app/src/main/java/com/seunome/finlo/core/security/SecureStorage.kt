package com.seunome.finlo.core.security

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.UUID

private val Context.dataStore by preferencesDataStore(name = "finlo_secure_prefs")

class SecureStorage(private val context: Context) {

    companion object {
        private val KEY_ACCESS_TOKEN = stringPreferencesKey("access_token")
        private val KEY_REFRESH_TOKEN = stringPreferencesKey("refresh_token")
        private val KEY_USER_ID = stringPreferencesKey("user_id")
        private val KEY_USER_EMAIL = stringPreferencesKey("user_email")
        private val KEY_DEVICE_ID = stringPreferencesKey("device_id")
        private val KEY_SERVER_CURSOR = longPreferencesKey("server_cursor")
        private val KEY_IS_LOCAL_MODE = booleanPreferencesKey("is_local_mode")
        private val KEY_CURRENCY = stringPreferencesKey("currency")
        private val KEY_DATE_FORMAT = stringPreferencesKey("date_format")
        private val KEY_THEME = stringPreferencesKey("theme")
        private val KEY_PIN_HASH = stringPreferencesKey("pin_hash")
        private val KEY_PIN_ENABLED = booleanPreferencesKey("pin_enabled")
        private val KEY_BIOMETRIC_ENABLED = booleanPreferencesKey("biometric_enabled")
        private val KEY_DAILY_REMINDER_ENABLED = booleanPreferencesKey("daily_reminder_enabled")
        private val KEY_BILL_REMINDER_ENABLED = booleanPreferencesKey("bill_reminder_enabled")
    }

    val accessTokenFlow: Flow<String?> = context.dataStore.data.map { it[KEY_ACCESS_TOKEN] }
    val userIdFlow: Flow<String?> = context.dataStore.data.map { it[KEY_USER_ID] }
    val isLocalModeFlow: Flow<Boolean> = context.dataStore.data.map { it[KEY_IS_LOCAL_MODE] ?: true }
    val currencyFlow: Flow<String> = context.dataStore.data.map { it[KEY_CURRENCY] ?: "BRL" }
    val themeFlow: Flow<String> = context.dataStore.data.map { it[KEY_THEME] ?: "SYSTEM" }
    val isPinEnabledFlow: Flow<Boolean> = context.dataStore.data.map { it[KEY_PIN_ENABLED] ?: false }
    val isBiometricEnabledFlow: Flow<Boolean> = context.dataStore.data.map { it[KEY_BIOMETRIC_ENABLED] ?: false }

    suspend fun saveAuthTokens(accessToken: String, refreshToken: String, userId: String, email: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_ACCESS_TOKEN] = accessToken
            prefs[KEY_REFRESH_TOKEN] = refreshToken
            prefs[KEY_USER_ID] = userId
            prefs[KEY_USER_EMAIL] = email
            prefs[KEY_IS_LOCAL_MODE] = false
        }
    }

    suspend fun clearAuth() {
        context.dataStore.edit { prefs ->
            prefs.remove(KEY_ACCESS_TOKEN)
            prefs.remove(KEY_REFRESH_TOKEN)
            prefs.remove(KEY_USER_ID)
            prefs.remove(KEY_USER_EMAIL)
            prefs[KEY_IS_LOCAL_MODE] = true
            prefs[KEY_SERVER_CURSOR] = 0L
        }
    }

    suspend fun getAccessToken(): String? = context.dataStore.data.first()[KEY_ACCESS_TOKEN]

    suspend fun getRefreshToken(): String? = context.dataStore.data.first()[KEY_REFRESH_TOKEN]

    suspend fun getDeviceId(): String {
        val current = context.dataStore.data.first()[KEY_DEVICE_ID]
        if (current != null) return current

        val newId = UUID.randomUUID().toString()
        context.dataStore.edit { it[KEY_DEVICE_ID] = newId }
        return newId
    }

    suspend fun getServerCursor(): Long = context.dataStore.data.first()[KEY_SERVER_CURSOR] ?: 0L

    suspend fun setServerCursor(cursor: Long) {
        context.dataStore.edit { it[KEY_SERVER_CURSOR] = cursor }
    }

    suspend fun setCurrency(currency: String) {
        context.dataStore.edit { it[KEY_CURRENCY] = currency }
    }

    suspend fun setTheme(theme: String) {
        context.dataStore.edit { it[KEY_THEME] = theme }
    }

    suspend fun savePin(pinHash: String) {
        context.dataStore.edit {
            it[KEY_PIN_HASH] = pinHash
            it[KEY_PIN_ENABLED] = true
        }
    }

    suspend fun removePin() {
        context.dataStore.edit {
            it.remove(KEY_PIN_HASH)
            it[KEY_PIN_ENABLED] = false
        }
    }

    suspend fun getPinHash(): String? = context.dataStore.data.first()[KEY_PIN_HASH]

    suspend fun setBiometricEnabled(enabled: Boolean) {
        context.dataStore.edit { it[KEY_BIOMETRIC_ENABLED] = enabled }
    }

    suspend fun setDailyReminder(enabled: Boolean) {
        context.dataStore.edit { it[KEY_DAILY_REMINDER_ENABLED] = enabled }
    }

    suspend fun setBillReminder(enabled: Boolean) {
        context.dataStore.edit { it[KEY_BILL_REMINDER_ENABLED] = enabled }
    }
}
