package com.example.inventoryapp.utils

import java.util.UUID

fun generateSecureNonce(): String {
    return UUID.randomUUID().toString()
}