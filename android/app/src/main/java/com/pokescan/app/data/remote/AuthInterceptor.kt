package com.snapdex.app.data.remote

import com.snapdex.app.data.local.SecureStorage
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

class AuthInterceptor @Inject constructor(
    private val secureStorage: SecureStorage,
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
        return chain.proceed(request)
    }
}
