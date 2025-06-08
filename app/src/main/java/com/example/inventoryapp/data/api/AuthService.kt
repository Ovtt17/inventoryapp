package com.example.inventoryapp.data.api

import com.example.inventoryapp.dto.LoginRequest
import com.example.inventoryapp.dto.RegisterRequest
import com.example.inventoryapp.model.User
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthService {
    @POST("auth/google")
    suspend fun registerUserWithGoogleToken(@Body token: String): Response<User>

    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): Response<User>

    @POST("auth/register")
    suspend fun register(@Body request: RegisterRequest): Response<User>
}
