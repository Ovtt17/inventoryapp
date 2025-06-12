package com.example.inventoryapp.utils

import android.content.Context
import com.example.inventoryapp.model.User
import com.google.gson.Gson
import androidx.core.content.edit

object SessionManager {
    private const val PREFS_NAME = "app_prefs"
    private const val USER_KEY = "user"

    fun saveUserSession(context: Context, user: User) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val userJson = Gson().toJson(user)
        prefs.edit {
            putString(USER_KEY, userJson)
        }
    }

    fun getUserSession(context: Context): User? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val userJson = prefs.getString(USER_KEY, null)
        return if (userJson != null) {
            Gson().fromJson(userJson, User::class.java)
        } else {
            null
        }
    }

    fun clearUserSession(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit {
            remove(USER_KEY)
        }
    }

    fun getUserId(context: Context): String? {
        val user = getUserSession(context)
        return user?.id
    }
}