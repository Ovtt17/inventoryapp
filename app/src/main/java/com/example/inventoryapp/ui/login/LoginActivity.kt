package com.example.inventoryapp.ui.login

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import com.example.inventoryapp.R
import com.example.inventoryapp.data.api.ApiClient
import com.example.inventoryapp.dto.LoginRequest
import com.example.inventoryapp.ui.main.MainActivity
import com.example.inventoryapp.ui.register.RegisterActivity
import com.example.inventoryapp.utils.SessionManager.saveUserSession
import com.example.inventoryapp.utils.generateSecureNonce
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
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
            signInWithGoogle()
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

    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    private fun signInWithGoogle() {
        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(getString(R.string.default_web_client_id))
            .setAutoSelectEnabled(false)
            .setNonce(generateSecureNonce())
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        CoroutineScope(Dispatchers.Main).launch {
            try {
                val result = credentialManager.getCredential(
                    request = request,
                    context = this@LoginActivity
                )
                handleSignInResult(result)
            } catch (e: Exception) {
                Log.e("LoginError", "Error al obtener credenciales: ${e.message}", e)
                Toast.makeText(
                    this@LoginActivity,
                    "Error de autenticación. Intenta nuevamente.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun handleSignInResult(result: GetCredentialResponse) {
        when (val credential = result.credential) {
            is GoogleIdTokenCredential -> {
                processGoogleIdTokenCredential(credential)
            }

            is androidx.credentials.CustomCredential -> {
                processCustomCredential(credential)
            }

            else -> {
                Log.w(
                    "LoginWarning",
                    "Tipo inesperado de credencial: ${credential::class.qualifiedName}"
                )
                showToast("Tipo de credencial no compatible.")
            }
        }
    }

    private fun processGoogleIdTokenCredential(credential: GoogleIdTokenCredential) {
        val idToken = credential.idToken

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val authService = ApiClient.getAuthService()
                val response = authService.registerUserWithGoogleToken(idToken)
                if (response.isSuccessful) {
                    val user = response.body()
                    if (user != null) {
                        runOnUiThread {
                            saveUserSession(this@LoginActivity, user)
                            showWelcomeAndRedirect(user.name)
                        }
                    }
                } else {
                    Log.e(
                        "LoginError",
                        "Error en el backend: ${response.code()} - ${response.message()}"
                    )
                    runOnUiThread {
                        showToast("Error al registrar usuario. Intenta nuevamente.")
                    }
                }
            } catch (e: Exception) {
                Log.e("LoginError", "Error al realizar la petición: ${e.message}", e)
                runOnUiThread {
                    showToast("Error de red. Intenta nuevamente.")
                }
            }
        }
    }

    private fun processCustomCredential(credential: androidx.credentials.CustomCredential) {
        if (credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
            try {
                val parsedCredential = GoogleIdTokenCredential.createFrom(credential.data)
                processGoogleIdTokenCredential(parsedCredential)
            } catch (e: GoogleIdTokenParsingException) {
                Log.e("LoginError", "Error al parsear GoogleIdTokenCredential", e)
                showToast("Token de Google inválido.")
            }
        } else {
            Log.w("LoginWarning", "CustomCredential no reconocida: ${credential.type}")
            showToast("Credencial personalizada no soportada.")
        }
    }

    private fun showWelcomeAndRedirect(displayName: String?) {
        showToast("Bienvenido, $displayName")
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}