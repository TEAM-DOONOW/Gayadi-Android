package com.gayadi.android.data.remote

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets

internal class RecordingHttpURLConnection(
    url: URL,
    private val statusCode: Int = HTTP_OK,
    private val responseBody: String = "{}",
) : HttpURLConnection(url) {
    private val requestBody = ByteArrayOutputStream()
    private var recordedRequestMethod = "GET"

    var wasDisconnected: Boolean = false
        private set

    val writtenBody: String
        get() = requestBody.toString(StandardCharsets.UTF_8.name())

    fun withUrl(requestedUrl: URL): RecordingHttpURLConnection = apply {
        url = requestedUrl
    }

    override fun connect() = Unit

    override fun setRequestMethod(method: String) {
        recordedRequestMethod = method
    }

    override fun getRequestMethod(): String = recordedRequestMethod

    override fun disconnect() {
        wasDisconnected = true
    }

    override fun usingProxy(): Boolean = false

    override fun getResponseCode(): Int = statusCode

    override fun getInputStream(): InputStream =
        ByteArrayInputStream(responseBody.toByteArray(StandardCharsets.UTF_8))

    override fun getErrorStream(): InputStream? =
        if (statusCode in 200..299) null else inputStream

    override fun getOutputStream(): OutputStream = requestBody
}

internal class RecordingTokenStore(initialToken: String? = null) : TokenStore {
    var token: String? = initialToken
        private set

    var clearCount: Int = 0
        private set

    override fun readAccessToken(): String? = token

    override fun writeAccessToken(accessToken: String) {
        token = accessToken
    }

    override fun clearAccessToken() {
        token = null
        clearCount += 1
    }
}
