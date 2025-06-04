package com.example.inventoryapp

import android.credentials.GetCredentialException
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.credentials.*
import com.example.inventoryapp.utils.generateSecureNonce
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var credentialManager: CredentialManager

    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        credentialManager = CredentialManager.create(this)

        val btnGoogleLogin = findViewById<Button>(R.id.btnLoginWithGoogle)
        btnGoogleLogin.setOnClickListener {
            signInWithGoogle()
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
                    context = this@MainActivity
                )
                handleSignInResult(result)
            } catch (e: GetCredentialException) {
                Log.e("LoginError", "Error al obtener credenciales: ${e.message}", e)
                Toast.makeText(
                    this@MainActivity,
                    "Error de autenticación. Intenta nuevamente.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun handleSignInResult(result: GetCredentialResponse) {
        val credential = result.credential

        if (credential is GoogleIdTokenCredential) {
            val idToken = credential.idToken
            val displayName = credential.displayName
            val email = credential.id

            Toast.makeText(this, "Bienvenido, $displayName", Toast.LENGTH_LONG).show()

        } else {
            Toast.makeText(this, "Credencial no compatible: tipo ${credential.type}", Toast.LENGTH_SHORT).show()
            Log.w("LoginWarning", "Credencial no es GoogleIdTokenCredential, es: ${credential::class.qualifiedName}")
        }
    }
}
