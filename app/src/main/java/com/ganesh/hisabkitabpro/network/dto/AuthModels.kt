package com.ganesh.hisabkitabpro.network.dto

data class LoginRequest(
    val email: String,
    val password: String
)

data class RegisterRequest(
    val name: String,
    val email: String,
    val password: String,
    val businessName: String? = null
)

data class AuthResponse(
    val token: String,
    val userId: String,
    val email: String,
    val name: String
)