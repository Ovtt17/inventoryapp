package com.example.inventoryapp.ui.register

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.inventoryapp.R
import com.example.inventoryapp.data.api.ApiClient
import com.example.inventoryapp.dto.RegisterRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class RegisterActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        val usernameEditText = findViewById<EditText>(R.id.registerUsername)
        val emailEditText = findViewById<EditText>(R.id.registerEmail)
        val passwordEditText = findViewById<EditText>(R.id.registerPassword)
        val confirmPasswordEditText = findViewById<EditText>(R.id.registerConfirmPassword)
        val registerButton = findViewById<Button>(R.id.registerSubmitButton)

        registerButton.setOnClickListener {
            val username = usernameEditText.text.toString().trim()
            val email = emailEditText.text.toString().trim()
            val password = passwordEditText.text.toString().trim()
            val confirmPassword = confirmPasswordEditText.text.toString().trim()

            if (validateInput(username, email, password, confirmPassword)) {
                registerUser(username, email, password)
            } else {
                Toast.makeText(
                    this,
                    "Por favor, completa todos los campos correctamente.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun validateInput(
        username: String,
        email: String,
        password: String,
        confirmPassword: String
    ): Boolean {
        return username.isNotEmpty() && email.isNotEmpty() && password.length >= 8 && password == confirmPassword
    }

    private fun registerUser(username: String, email: String, password: String) {
        val registerRequest = RegisterRequest(username, email, password)
        val authService = ApiClient.getAuthService()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = authService.register(registerRequest)
                if (response.isSuccessful) {
                    runOnUiThread {
                        Toast.makeText(
                            this@RegisterActivity,
                            "Usuario registrado exitosamente.",
                            Toast.LENGTH_SHORT
                        ).show()
                        finish()
                    }
                } else {
                    runOnUiThread {
                        Toast.makeText(
                            this@RegisterActivity,
                            "Error al registrar usuario: ${response.message()}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(
                        this@RegisterActivity,
                        "Error de red: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }
}