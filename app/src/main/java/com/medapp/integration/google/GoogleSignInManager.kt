package com.medapp.integration.google

import android.content.Context
import android.content.Intent
import com.google.android.gms.auth.GoogleAuthUtil
import com.google.android.gms.auth.UserRecoverableAuthException
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GoogleSignInManager(
    context: Context
) {
    val appContext = context.applicationContext
    private val signInClient: GoogleSignInClient

    init {
        val options = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(Scope(TASKS_SCOPE))
            .build()
        signInClient = GoogleSignIn.getClient(appContext, options)
    }

    fun signInIntent(): Intent = signInClient.signInIntent

    fun getAccountFromIntent(data: Intent?): GoogleSignInAccount {
        return GoogleSignIn.getSignedInAccountFromIntent(data).result
    }

    suspend fun fetchAccessToken(account: GoogleSignInAccount): String = withContext(Dispatchers.IO) {
        val email = account.email ?: throw IllegalStateException("Google account email is missing")
        try {
            GoogleAuthUtil.getToken(appContext, email, "oauth2:$TASKS_SCOPE")
        } catch (recoverable: UserRecoverableAuthException) {
            throw IllegalStateException("Google authorization requires additional user consent", recoverable)
        }
    }

    suspend fun signOut() = withContext(Dispatchers.IO) {
        signInClient.signOut()
    }

    companion object {
        const val TASKS_SCOPE = "https://www.googleapis.com/auth/tasks"
    }
}
