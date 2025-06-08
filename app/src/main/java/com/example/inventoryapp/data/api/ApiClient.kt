package com.example.inventoryapp.data.api

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object ApiClient {
    private const val BASE_URL = "http://10.0.2.2:8080/api/v1/"

    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    fun getAuthService(): AuthService = retrofit.create(AuthService::class.java)
    fun getProductService(): ProductService = retrofit.create(ProductService::class.java)
}
