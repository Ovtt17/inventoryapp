package com.example.inventoryapp.ui.login

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.credentials.CredentialManager
import com.example.inventoryapp.R
import com.example.inventoryapp.data.api.ApiClient
import com.example.inventoryapp.dto.LoginRequest
import com.example.inventoryapp.ui.main.MainActivity
import com.example.inventoryapp.ui.register.RegisterActivity
import com.example.inventoryapp.utils.GoogleLoginHelper
import com.example.inventoryapp.utils.SessionManager.saveUserSession
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    private lateinit var credentialManager: CredentialManager

    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        credentialManager = CredentialManager.create(this)

        val emailField = findViewById<EditText>(R.id.emailField)
        val passwordField = findViewById<EditText>(R.id.passwordField)
        val loginButton = findViewById<Button>(R.id.internalLoginButton)
        val btnGoogleLogin = findViewById<LinearLayout>(R.id.btnLoginWithGoogle)
        val toggleAuthText = findViewById<TextView>(R.id.toggleAuthText)

        loginButton.setOnClickListener {
            val email = emailField.text.toString().trim()
            val password = passwordField.text.toString().trim()

            if (validateInput(email, password)) {
                loginUser(email, password)
            } else {
                Toast.makeText(
                    this,
                    "Por favor, completa todos los campos correctamente.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        btnGoogleLogin.setOnClickListener {
            GoogleLoginHelper.signInWithGoogle(this, credentialManager)
        }

        toggleAuthText.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }

    private fun validateInput(email: String, password: String): Boolean {
        return email.isNotEmpty() && password.isNotEmpty()
    }

    private fun loginUser(email: String, password: String) {
        val loginRequest = LoginRequest(email, password)
        val authService = ApiClient.getAuthService()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = authService.login(loginRequest)
                if (response.isSuccessful) {
                    val user = response.body()
                    if (user != null) {
                        runOnUiThread {
                            saveUserSession(this@LoginActivity, user)
                            showWelcomeAndRedirect(user.name)
                        }
                    }
                } else {
                    runOnUiThread {
                        Toast.makeText(
                            this@LoginActivity,
                            "Error al iniciar sesión: ${response.message()}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(
                        this@LoginActivity,
                        "Error de red: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun showWelcomeAndRedirect(displayName: String?) {
        Toast.makeText(this, "Bienvenido, $displayName", Toast.LENGTH_SHORT).show()
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}