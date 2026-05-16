package com.pokescan.app.data.remote

import com.pokescan.app.data.local.SecureStorage
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
        if (response.code == 401 && !request.url.encodedPath.contains("auth/")) {
            secureStorage.clearToken()
            authEventBus.emitUnauthorized()
        }
        return response
    }
}
