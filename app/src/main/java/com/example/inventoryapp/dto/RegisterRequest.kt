package com.example.inventoryapp.dto

data class RegisterRequest(
    val name: String,
    val email: String,
    val password: String
)