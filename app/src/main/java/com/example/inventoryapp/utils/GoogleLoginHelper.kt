package com.example.inventoryapp.utils

import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import com.example.inventoryapp.R
import com.example.inventoryapp.data.api.ApiClient
import com.example.inventoryapp.model.User
import com.example.inventoryapp.ui.main.MainActivity
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object GoogleLoginHelper {

    fun signInWithGoogle(context: Context, credentialManager: CredentialManager) {
        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(context.getString(R.string.default_web_client_id))
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
                    context = context
                )
                handleSignInResult(context, result)
            } catch (e: Exception) {
                Log.e("GoogleLoginError", "Error al obtener credenciales: ${e.message}", e)
                Toast.makeText(
                    context,
                    "Error de autenticación. Intenta nuevamente.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun handleSignInResult(context: Context, result: GetCredentialResponse) {
        when (val credential = result.credential) {
            is GoogleIdTokenCredential -> {
                processGoogleIdTokenCredential(context, credential)
            }

            is androidx.credentials.CustomCredential -> {
                processCustomCredential(context, credential)
            }

            else -> {
                Log.w(
                    "GoogleLoginWarning",
                    "Tipo inesperado de credencial: ${credential::class.qualifiedName}"
                )
                Toast.makeText(context, "Tipo de credencial no compatible.", Toast.LENGTH_SHORT)
                    .show()
            }
        }
    }

    private fun processGoogleIdTokenCredential(
        context: Context,
        credential: GoogleIdTokenCredential
    ) {
        val idToken = credential.idToken

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val authService = ApiClient.getAuthService()
                val response = authService.registerUserWithGoogleToken(idToken)
                if (response.isSuccessful) {
                    val user = response.body()
                    if (user != null) {
                        saveUserSession(context, user)
                        redirectToMainActivity(context, user.name)
                    }
                } else {
                    Log.e(
                        "GoogleLoginError",
                        "Error en el backend: ${response.code()} - ${response.message()}"
                    )
                    CoroutineScope(Dispatchers.Main).launch {
                        Toast.makeText(
                            context,
                            "Error al registrar usuario. Intenta nuevamente.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            } catch (e: Exception) {
                Log.e("GoogleLoginError", "Error al realizar la petición: ${e.message}", e)
                CoroutineScope(Dispatchers.Main).launch {
                    Toast.makeText(context, "Error de red. Intenta nuevamente.", Toast.LENGTH_SHORT)
                        .show()
                }
            }
        }
    }

    private fun processCustomCredential(
        context: Context,
        credential: androidx.credentials.CustomCredential
    ) {
        if (credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
            try {
                val parsedCredential = GoogleIdTokenCredential.createFrom(credential.data)
                processGoogleIdTokenCredential(context, parsedCredential)
            } catch (e: GoogleIdTokenParsingException) {
                Log.e("GoogleLoginError", "Error al parsear GoogleIdTokenCredential", e)
                Toast.makeText(context, "Token de Google inválido.", Toast.LENGTH_SHORT).show()
            }
        } else {
            Log.w("GoogleLoginWarning", "CustomCredential no reconocida: ${credential.type}")
            Toast.makeText(context, "Credencial personalizada no soportada.", Toast.LENGTH_SHORT)
                .show()
        }
    }

    private fun saveUserSession(context: Context, user: User) {
        SessionManager.saveUserSession(context, user)
    }

    private fun redirectToMainActivity(context: Context, displayName: String?) {
        CoroutineScope(Dispatchers.Main).launch {
            Toast.makeText(context, "Bienvenido, $displayName", Toast.LENGTH_SHORT).show()
            val intent = Intent(context, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            context.startActivity(intent)
        }
    }
}