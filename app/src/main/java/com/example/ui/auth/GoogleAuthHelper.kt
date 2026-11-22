package com.example.ui.auth

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.example.R
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

private const val AUTH_TAG = "Auth"

fun isRecentLoginFresh(user: FirebaseUser?, maxAgeMillis: Long = 5 * 60 * 1000L): Boolean {
  val lastSignIn = user?.metadata?.lastSignInTimestamp ?: return false
  if (lastSignIn <= 0L) return false
  return (System.currentTimeMillis() - lastSignIn) <= maxAgeMillis
}

fun attemptAutoSignIn(
  context: Context,
  credentialManager: CredentialManager,
  onAuthSuccess: () -> Unit,
  onUnauthenticated: () -> Unit,
  scope: CoroutineScope
) {
  if (Firebase.auth.currentUser != null) {
    onAuthSuccess()
    return
  }
  val clientId =
    try {
      context.getString(R.string.default_web_client_id)
    } catch (_: Exception) {
      onUnauthenticated()
      return
    }
  val googleIdOption =
    GetGoogleIdOption.Builder()
      .setFilterByAuthorizedAccounts(true)
      .setServerClientId(clientId)
      .setAutoSelectEnabled(true)
      .build()
  val request = GetCredentialRequest.Builder().addCredentialOption(googleIdOption).build()
  scope.launch {
    try {
      val cred = credentialManager.getCredential(context, request).credential
      if (cred is CustomCredential && cred.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
        val token = GoogleIdTokenCredential.createFrom(cred.data).idToken
        Firebase.auth.signInWithCredential(GoogleAuthProvider.getCredential(token, null)).await()
        onAuthSuccess()
      } else {
        onUnauthenticated()
      }
    } catch (_: Exception) {
      onUnauthenticated()
    }
  }
}

fun onGoogleSignInClicked(
  context: Context,
  credentialManager: CredentialManager,
  onAuthSuccess: () -> Unit,
  onAuthError: (String) -> Unit,
  scope: CoroutineScope,
  onAuthCancelled: () -> Unit = {}
) {
  val clientId =
    try {
      context.getString(R.string.default_web_client_id)
    } catch (_: Exception) {
      onAuthError("Google Giriş yapılandırması eksik: default_web_client_id bulunamadı")
      return
    }
  val activity = context as? Activity
  if (activity == null) {
    onAuthError("Oturum açma ekranı başlatılamadı.")
    return
  }
  val signInOption = GetSignInWithGoogleOption.Builder(serverClientId = clientId).build()
  val request = GetCredentialRequest.Builder().addCredentialOption(signInOption).build()
  scope.launch {
    try {
      val cred = credentialManager.getCredential(activity, request).credential
      if (cred is CustomCredential && cred.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
        val token = GoogleIdTokenCredential.createFrom(cred.data).idToken
        Firebase.auth.signInWithCredential(GoogleAuthProvider.getCredential(token, null)).await()
        onAuthSuccess()
      } else {
        onAuthError("Beklenmeyen kimlik doğrulama türü.")
      }
    } catch (e: GetCredentialCancellationException) {
      Log.w(AUTH_TAG, "Google Sign-In flow cancelled or dismissed: ${e.message}", e)
      onAuthCancelled()
    } catch (e: Exception) {
      Log.e(AUTH_TAG, "Google Sign-In failed", e)
      onAuthError(e.localizedMessage ?: "Giriş başarısız oldu.")
    }
  }
}

fun reauthenticateWithGoogle(
  context: Context,
  credentialManager: CredentialManager,
  onReauthSuccess: () -> Unit,
  onReauthError: (String) -> Unit,
  scope: CoroutineScope,
  onReauthCancelled: () -> Unit = {}
) {
  val clientId =
    try {
      context.getString(R.string.default_web_client_id)
    } catch (_: Exception) {
      onReauthError("Google kimlik yapılandırması bulunamadı.")
      return
    }
  val activity = context as? Activity
  if (activity == null) {
    onReauthError("Doğrulama ekranı başlatılamadı.")
    return
  }
  val signInOption = GetSignInWithGoogleOption.Builder(serverClientId = clientId).build()
  val request = GetCredentialRequest.Builder().addCredentialOption(signInOption).build()
  scope.launch {
    try {
      val cred = credentialManager.getCredential(activity, request).credential
      if (cred is CustomCredential && cred.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
        val googleCred = GoogleIdTokenCredential.createFrom(cred.data)
        val currentUser = Firebase.auth.currentUser
        if (currentUser != null) {
          val activeEmail = currentUser.email.orEmpty()
          val selectedEmail = googleCred.id
          if (activeEmail.isNotBlank() &&
            selectedEmail.isNotBlank() &&
            !activeEmail.equals(selectedEmail, ignoreCase = true)
          ) {
            onReauthError("Güvenlik doğrulaması için lütfen aktif hesap ($activeEmail) ile devam edin.")
            return@launch
          }
          currentUser.reauthenticate(GoogleAuthProvider.getCredential(googleCred.idToken, null)).await()
        } else {
          Firebase.auth.signInWithCredential(GoogleAuthProvider.getCredential(googleCred.idToken, null)).await()
        }
        onReauthSuccess()
      } else {
        onReauthError("Beklenmeyen kimlik doğrulama türü.")
      }
    } catch (e: GetCredentialCancellationException) {
      Log.w(AUTH_TAG, "Google re-auth cancelled: ${e.message}", e)
      onReauthCancelled()
    } catch (e: Exception) {
      Log.e(AUTH_TAG, "Google re-auth failed", e)
      onReauthError(e.localizedMessage ?: "Kimlik doğrulama başarısız oldu.")
    }
  }
}

suspend fun clearCredentialStateSafely(credentialManager: CredentialManager) {
  withContext(NonCancellable) {
    try {
      credentialManager.clearCredentialState(ClearCredentialStateRequest())
    } catch (e: Exception) {
      Log.w(AUTH_TAG, "Failed to clear credential state: ${e.message}")
    }
  }
}

fun signOutUser(
  credentialManager: CredentialManager,
  scope: CoroutineScope,
  onSignOutComplete: () -> Unit = {}
) {
  scope.launch {
    withContext(NonCancellable) {
      try {
        credentialManager.clearCredentialState(ClearCredentialStateRequest())
      } catch (e: Exception) {
        Log.w(AUTH_TAG, "Failed to clear credential state: ${e.message}")
      } finally {
        Firebase.auth.signOut()
        onSignOutComplete()
      }
    }
  }
}
