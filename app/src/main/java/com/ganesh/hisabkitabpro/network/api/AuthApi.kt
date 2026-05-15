package com.ganesh.hisabkitabpro.network.api

import com.ganesh.hisabkitabpro.network.dto.LoginRequest
import com.ganesh.hisabkitabpro.network.dto.RegisterRequest
import com.ganesh.hisabkitabpro.network.dto.AuthResponse
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthApi {
    @POST("auth/register")
    suspend fun register(@Body request: RegisterRequest): AuthResponse

    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): AuthResponse
}