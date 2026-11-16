package com.seunome.finlo.core.network.dto

data class RegisterRequestDto(
    val email: String,
    val password: String
)

data class LoginRequestDto(
    val email: String,
    val password: String
)

data class GoogleLoginRequestDto(
    val idToken: String
)

data class RefreshRequestDto(
    val refreshToken: String
)

data class AuthResponseDto(
    val accessToken: String,
    val refreshToken: String,
    val userId: String,
    val email: String,
    val expiresIn: Long
)
