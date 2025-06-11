package com.example.inventoryapp.data.api

import android.content.Context
import com.example.inventoryapp.utils.SessionManager
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object ApiClient {
    private const val BASE_URL = "http://192.168.1.9:8080/api/v1/"

    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    fun getAuthService(): AuthService = retrofit.create(AuthService::class.java)
    fun getProductService(context: Context): ProductService {
        val retrofit = createRetrofit(context)
        return retrofit.create(ProductService::class.java)
    }

    private fun createRetrofit(context: Context): Retrofit {
        val client = OkHttpClient.Builder()
            .addInterceptor { chain ->
                val email = SessionManager.getUserEmail(context)
                val request = chain.request().newBuilder()
                    .addHeader("X-User-Email", email ?: "")
                    .build()
                chain.proceed(request)
            }
            .build()

        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
}
