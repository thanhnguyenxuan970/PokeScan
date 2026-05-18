package com.snapdex.app.data.remote

import com.snapdex.app.data.local.SecureStorage
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import javax.inject.Inject

class AuthAuthenticator @Inject constructor(
    private val secureStorage: SecureStorage,
    private val authEventBus: AuthEventBus,
) : Authenticator {

    override fun authenticate(route: Route?, response: Response): Request? {
        if (response.request.url.encodedPath.contains("/auth/")) return null

        val requestToken = response.request.header("Authorization")
            ?.removePrefix("Bearer ")
            ?.takeIf { it.isNotEmpty() }
        val currentToken = secureStorage.getToken()

        return when {
            currentToken == null -> null
            requestToken == currentToken -> {
                secureStorage.clearToken()
                authEventBus.emitUnauthorized()
                null
            }
            else -> {
                // Stale request from previous session — retry transparently with fresh token
                response.request.newBuilder()
                    .header("Authorization", "Bearer $currentToken")
                    .build()
            }
        }
    }
}
