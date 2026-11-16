package com.seunome.finlo.core.security

import java.security.MessageDigest

class PinManager(private val secureStorage: SecureStorage) {

    private val salt = "finlo_secure_pin_salt_v1"

    suspend fun setPin(pin: String) {
        val hash = hashPin(pin)
        secureStorage.savePin(hash)
    }

    suspend fun verifyPin(pin: String): Boolean {
        val storedHash = secureStorage.getPinHash() ?: return false
        val inputHash = hashPin(pin)
        return storedHash == inputHash
    }

    suspend fun removePin() {
        secureStorage.removePin()
    }

    private fun hashPin(pin: String): String {
        val input = salt + pin
        val md = MessageDigest.getInstance("SHA-256")
        val bytes = md.digest(input.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
