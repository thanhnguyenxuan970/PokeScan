package com.snapdex.app.data.remote

import com.snapdex.app.data.local.SecureStorage
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

class AuthInterceptor @Inject constructor(
    private val secureStorage: SecureStorage,
    private val authEventBus: AuthEventBus,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val token = secureStorage.getToken()
        val request = if (token != null && !chain.request().url.encodedPath.contains("/auth/")) {
            chain.request().newBuilder()
                .header("Authorization", "Bearer $token")
                .build()
        } else {
            chain.request()
        }
        val response = chain.proceed(request)
        if (response.code == 401 && !request.url.encodedPath.contains("/auth/")) {
            val currentToken = secureStorage.getToken()
            if (token != null && token == currentToken) {
                secureStorage.clearToken()
                authEventBus.emitUnauthorized()
            }
            // else: stale request from previous session — do not disrupt current session
        }
        return response
    }
}
