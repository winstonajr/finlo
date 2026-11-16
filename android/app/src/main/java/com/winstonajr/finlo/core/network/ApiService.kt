package com.seunome.finlo.core.network

import com.seunome.finlo.core.network.dto.AuthResponseDto
import com.seunome.finlo.core.network.dto.GoogleLoginRequestDto
import com.seunome.finlo.core.network.dto.LoginRequestDto
import com.seunome.finlo.core.network.dto.RefreshRequestDto
import com.seunome.finlo.core.network.dto.RegisterRequestDto
import com.seunome.finlo.core.network.dto.SyncPullResponseDto
import com.seunome.finlo.core.network.dto.SyncPushRequestDto
import com.seunome.finlo.core.network.dto.SyncPushResponseDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Query

interface ApiService {
    @POST("v1/auth/register")
    suspend fun register(@Body request: RegisterRequestDto): Response<AuthResponseDto>

    @POST("v1/auth/login")
    suspend fun login(@Body request: LoginRequestDto): Response<AuthResponseDto>

    @POST("v1/auth/refresh")
    suspend fun refresh(@Body request: RefreshRequestDto): Response<AuthResponseDto>

    @POST("v1/auth/google")
    suspend fun googleLogin(@Body request: GoogleLoginRequestDto): Response<AuthResponseDto>

    @POST("v1/sync/push")
    suspend fun pushChanges(
        @Header("Idempotency-Key") idempotencyKey: String,
        @Body request: SyncPushRequestDto
    ): Response<SyncPushResponseDto>

    @GET("v1/sync/pull")
    suspend fun pullChanges(
        @Query("cursor") cursor: Long,
        @Query("limit") limit: Int = 100
    ): Response<SyncPullResponseDto>

    @GET("v1/user/export")
    suspend fun exportUserData(): Response<String>

    @DELETE("v1/user/account")
    suspend fun deleteAccount(): Response<Unit>
}
