package com.example.inventoryapp.ui.main

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.inventoryapp.R
import com.example.inventoryapp.ui.login.LoginActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
        val userToken = prefs.getString("token", null)
        val displayName = prefs.getString("display_name", "Usuario")

        if (userToken == null) {
            // No hay sesión, redirigir al login
            startActivity(Intent(this, LoginActivity::class.java))
            return
        }

        setContentView(R.layout.activity_main)

        val welcomeText = findViewById<TextView>(R.id.welcomeText)
        welcomeText.text = getString(R.string.welcome_message, displayName)
    }
}