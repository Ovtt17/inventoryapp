package com.example.inventoryapp.data.api

import com.example.inventoryapp.model.User
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthService {
    @POST("auth/google")
    suspend fun registerUserWithGoogleToken(@Body token: String): Response<User>
}
