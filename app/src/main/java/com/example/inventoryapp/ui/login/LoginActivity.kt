package com.example.inventoryapp.ui.login

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import com.example.inventoryapp.R
import com.example.inventoryapp.ui.main.MainActivity
import com.example.inventoryapp.utils.generateSecureNonce
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import androidx.core.content.edit
import com.example.inventoryapp.ui.register.RegisterActivity
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException

class LoginActivity : AppCompatActivity() {

    private lateinit var credentialManager: CredentialManager

    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        credentialManager = CredentialManager.create(this)

        val btnGoogleLogin = findViewById<LinearLayout>(R.id.btnLoginWithGoogle)
        btnGoogleLogin.setOnClickListener {
            signInWithGoogle()
        }

        val toggleAuthText = findViewById<TextView>(R.id.toggleAuthText)
        toggleAuthText.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
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
        val displayName = credential.displayName

        saveUserSession(idToken, displayName)
        showWelcomeAndRedirect(displayName)
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

    private fun saveUserSession(idToken: String?, displayName: String?) {
        getSharedPreferences("app_prefs", MODE_PRIVATE).edit {
            putString("token", idToken)
            putString("display_name", displayName)
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