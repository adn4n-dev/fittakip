package com.fittakip.app.auth

import android.content.Context
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.fittakip.app.BuildConfig
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential

data class GoogleUser(val firstName: String, val fullName: String, val email: String)

sealed interface SignInResult {
    data class Success(val user: GoogleUser) : SignInResult
    data object Cancelled : SignInResult
    data class Error(val message: String) : SignInResult
}

/** Google ile giriş: ad, soyad ve e-postayı alır. Veriler telefonda kalır, sunucuya gitmez. */
object GoogleAuth {
    val configured: Boolean get() = BuildConfig.GOOGLE_WEB_CLIENT_ID.isNotBlank()

    /** [context] bir Activity olmalı (hesap seçici onun üstünde açılır). */
    suspend fun signIn(context: Context): SignInResult {
        if (!configured) return SignInResult.Error("Google girişi bu sürümde henüz ayarlanmadı.")
        val option = GetSignInWithGoogleOption.Builder(BuildConfig.GOOGLE_WEB_CLIENT_ID).build()
        val request = GetCredentialRequest.Builder().addCredentialOption(option).build()
        return try {
            val credential = CredentialManager.create(context).getCredential(context, request).credential
            if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val g = GoogleIdTokenCredential.createFrom(credential.data)
                SignInResult.Success(
                    GoogleUser(
                        firstName = g.givenName ?: g.displayName?.substringBefore(' ') ?: "",
                        fullName = g.displayName ?: listOfNotNull(g.givenName, g.familyName).joinToString(" "),
                        email = g.id
                    )
                )
            } else SignInResult.Error("Beklenmeyen giriş türü.")
        } catch (e: GetCredentialCancellationException) {
            SignInResult.Cancelled
        } catch (e: NoCredentialException) {
            SignInResult.Error("Telefonda Google hesabı bulunamadı. Ayarlar > Hesaplar'dan bir Google hesabı ekle.")
        } catch (e: GetCredentialException) {
            SignInResult.Error("Google girişi başarısız: ${e.message ?: e.type}")
        } catch (e: Exception) {
            SignInResult.Error("Google girişi başarısız: ${e.message}")
        }
    }

    suspend fun signOut(context: Context) {
        runCatching { CredentialManager.create(context).clearCredentialState(ClearCredentialStateRequest()) }
    }
}
