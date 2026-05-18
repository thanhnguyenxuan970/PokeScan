package com.snapdex.app.data.remote

import com.snapdex.app.data.local.SecureStorage
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class AuthAuthenticatorTest {

    private val secureStorage = mockk<SecureStorage>(relaxed = true)
    private val authEventBus = mockk<AuthEventBus>(relaxed = true)
    private val authenticator = AuthAuthenticator(secureStorage, authEventBus)

    private fun makeResponse(requestToken: String?, path: String = "/api/cards"): Response {
        val requestBuilder = Request.Builder().url("http://test.com$path")
        if (requestToken != null) requestBuilder.header("Authorization", "Bearer $requestToken")
        return Response.Builder()
            .request(requestBuilder.build())
            .protocol(Protocol.HTTP_1_1)
            .code(401)
            .message("Unauthorized")
            .body("{}".toResponseBody("application/json".toMediaType()))
            .build()
    }

    @Test
    fun `stale request — current token differs — retry with current token`() {
        every { secureStorage.getToken() } returns "acc2-token"
        val response = makeResponse(requestToken = "acc1-token")

        val retryRequest = authenticator.authenticate(null, response)

        assertNotNull(retryRequest)
        assertEquals("Bearer acc2-token", retryRequest!!.header("Authorization"))
        verify(exactly = 0) { secureStorage.clearToken() }
        verify(exactly = 0) { authEventBus.emitUnauthorized() }
    }

    @Test
    fun `fresh 401 — request token matches current token — clear and emit unauthorized`() {
        every { secureStorage.getToken() } returns "current-token"
        val response = makeResponse(requestToken = "current-token")

        val retryRequest = authenticator.authenticate(null, response)

        assertNull(retryRequest)
        verify { secureStorage.clearToken() }
        verify { authEventBus.emitUnauthorized() }
    }

    @Test
    fun `no current token — signed out — return null no retry`() {
        every { secureStorage.getToken() } returns null
        val response = makeResponse(requestToken = "old-token")

        val retryRequest = authenticator.authenticate(null, response)

        assertNull(retryRequest)
        verify(exactly = 0) { secureStorage.clearToken() }
    }

    @Test
    fun `no auth header on request — current token present — retry with current token`() {
        every { secureStorage.getToken() } returns "current-token"
        val response = makeResponse(requestToken = null)

        val retryRequest = authenticator.authenticate(null, response)

        assertNotNull(retryRequest)
        assertEquals("Bearer current-token", retryRequest!!.header("Authorization"))
        verify(exactly = 0) { secureStorage.clearToken() }
        verify(exactly = 0) { authEventBus.emitUnauthorized() }
    }

    @Test
    fun `auth endpoint 401 — skip — return null`() {
        every { secureStorage.getToken() } returns "some-token"
        val response = makeResponse(requestToken = "some-token", path = "/auth/google")

        val retryRequest = authenticator.authenticate(null, response)

        assertNull(retryRequest)
        verify(exactly = 0) { secureStorage.clearToken() }
        verify(exactly = 0) { authEventBus.emitUnauthorized() }
    }
}
