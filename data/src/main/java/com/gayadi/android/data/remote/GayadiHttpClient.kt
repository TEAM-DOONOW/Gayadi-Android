package com.gayadi.android.data.remote

import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class ApiError(
    val timestamp: String? = null,
    val status: Int,
    val code: String,
    val message: String,
    val path: String? = null,
    val traceId: String? = null,
    val details: Map<String, String> = emptyMap(),
)

class ApiException(
    val statusCode: Int,
    val error: ApiError,
) : IOException(error.message)

/** Small JSON HTTP client matching the Gayadi server's REST and error contracts. */
class GayadiHttpClient private constructor(
    baseUrl: String,
    private val tokenStore: TokenStore,
    private val connectionFactory: ((URL) -> HttpURLConnection)?,
    private val ioDispatcher: CoroutineDispatcher,
    private val okHttpClient: OkHttpClient,
) {
    constructor(
        baseUrl: String,
        tokenStore: TokenStore,
    ) : this(baseUrl, tokenStore, null, Dispatchers.IO, defaultOkHttpClient())

    internal constructor(
        baseUrl: String,
        tokenStore: TokenStore,
        connectionFactory: (URL) -> HttpURLConnection,
        ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    ) : this(baseUrl, tokenStore, connectionFactory, ioDispatcher, defaultOkHttpClient())

    private val normalizedBaseUrl = baseUrl.trim().trimEnd('/')

    init {
        require(normalizedBaseUrl.isNotEmpty()) { "API base URL must not be blank." }
        val protocol = URL(normalizedBaseUrl).protocol
        require(protocol == "http" || protocol == "https") { "API base URL must use HTTP or HTTPS." }
    }

    suspend fun getObject(
        path: String,
        query: Map<String, String?> = emptyMap(),
        authenticated: Boolean = true,
    ): JSONObject = requestObject("GET", path, query, body = null, authenticated = authenticated)

    suspend fun getArray(
        path: String,
        query: Map<String, String?> = emptyMap(),
        authenticated: Boolean = true,
    ): JSONArray = withContext(ioDispatcher) {
        val response = execute("GET", path, query, body = null, authenticated = authenticated)
        if (response.body.isBlank()) JSONArray() else JSONArray(response.body)
    }

    suspend fun postObject(
        path: String,
        body: JSONObject,
        authenticated: Boolean = true,
    ): JSONObject = requestObject("POST", path, emptyMap(), body, authenticated)

    suspend fun putObject(
        path: String,
        body: JSONObject,
        authenticated: Boolean = true,
    ): JSONObject = requestObject("PUT", path, emptyMap(), body, authenticated)

    suspend fun patchObject(
        path: String,
        body: JSONObject,
        authenticated: Boolean = true,
    ): JSONObject = requestObject("PATCH", path, emptyMap(), body, authenticated)

    suspend fun patchArray(
        path: String,
        body: JSONObject,
        authenticated: Boolean = true,
    ): JSONArray = withContext(ioDispatcher) {
        val response = execute("PATCH", path, emptyMap(), body, authenticated)
        if (response.body.isBlank()) JSONArray() else JSONArray(response.body)
    }

    suspend fun delete(path: String, authenticated: Boolean = true) {
        withContext(ioDispatcher) {
            execute("DELETE", path, emptyMap(), body = null, authenticated = authenticated)
        }
    }

    private suspend fun requestObject(
        method: String,
        path: String,
        query: Map<String, String?>,
        body: JSONObject?,
        authenticated: Boolean,
    ): JSONObject = withContext(ioDispatcher) {
        val response = execute(method, path, query, body, authenticated)
        if (response.body.isBlank()) JSONObject() else JSONObject(response.body)
    }

    private fun execute(
        method: String,
        path: String,
        query: Map<String, String?>,
        body: JSONObject?,
        authenticated: Boolean,
    ): HttpResponse {
        val token = if (authenticated) {
            tokenStore.readAccessToken()
                ?: throw unauthenticatedError(path)
        } else {
            null
        }
        val url = buildUrl(path, query)
        return connectionFactory?.let { factory ->
            executeUrlConnection(method, url, path, body, authenticated, token, factory)
        } ?: executeOkHttp(method, url, path, body, authenticated, token)
    }

    private fun executeOkHttp(
        method: String,
        url: URL,
        requestPath: String,
        body: JSONObject?,
        authenticated: Boolean,
        token: String?,
    ): HttpResponse {
        val mediaType = JSON_CONTENT_TYPE.toMediaType()
        val requestBody = body?.toString()?.toRequestBody(mediaType)
        val request = Request.Builder()
            .url(url)
            .header("Accept", JSON_CONTENT_TYPE)
            .apply {
                token?.let { header("Authorization", "Bearer $it") }
                when (method) {
                    "GET" -> get()
                    "DELETE" -> delete()
                    else -> method(method, requireNotNull(requestBody) { "$method requires a JSON body." })
                }
            }
            .build()
        okHttpClient.newCall(request).execute().use { response ->
            val responseBody = if (response.code == HttpURLConnection.HTTP_NO_CONTENT) {
                ""
            } else {
                response.body?.string().orEmpty()
            }
            if (!response.isSuccessful) {
                val exception = apiException(response.code, requestPath, responseBody)
                if (authenticated && response.code == HttpURLConnection.HTTP_UNAUTHORIZED) {
                    runCatching(tokenStore::clearAccessToken)
                        .exceptionOrNull()
                        ?.let(exception::addSuppressed)
                }
                throw exception
            }
            return HttpResponse(response.code, responseBody)
        }
    }

    private fun executeUrlConnection(
        method: String,
        url: URL,
        requestPath: String,
        body: JSONObject?,
        authenticated: Boolean,
        token: String?,
        factory: (URL) -> HttpURLConnection,
    ): HttpResponse {
        val connection = factory(url).apply {
            requestMethod = method
            connectTimeout = CONNECT_TIMEOUT_MILLIS
            readTimeout = READ_TIMEOUT_MILLIS
            useCaches = false
            setRequestProperty("Accept", JSON_CONTENT_TYPE)
            token?.let { setRequestProperty("Authorization", "Bearer $it") }
        }

        return try {
            body?.let { json ->
                val bytes = json.toString().toByteArray(StandardCharsets.UTF_8)
                connection.doOutput = true
                connection.setRequestProperty("Content-Type", JSON_CONTENT_TYPE)
                connection.setFixedLengthStreamingMode(bytes.size)
                connection.outputStream.use { output -> output.write(bytes) }
            }

            val statusCode = connection.responseCode
            val responseBody = if (statusCode == HttpURLConnection.HTTP_NO_CONTENT) {
                ""
            } else {
                val stream = if (statusCode in 200..299) connection.inputStream else connection.errorStream
                stream?.bufferedReader(StandardCharsets.UTF_8)?.use { it.readText() }.orEmpty()
            }
            if (statusCode !in 200..299) {
                val exception = apiException(statusCode, requestPath, responseBody)
                if (authenticated && statusCode == HttpURLConnection.HTTP_UNAUTHORIZED) {
                    runCatching(tokenStore::clearAccessToken)
                        .exceptionOrNull()
                        ?.let(exception::addSuppressed)
                }
                throw exception
            }
            HttpResponse(statusCode, responseBody)
        } finally {
            connection.disconnect()
        }
    }

    private fun buildUrl(path: String, query: Map<String, String?>): URL {
        val normalizedPath = path.trim()
        require(normalizedPath.isNotEmpty()) { "API path must not be blank." }
        require(!normalizedPath.contains('?') && !normalizedPath.contains('#')) {
            "API path must not contain a query or fragment."
        }
        require(!normalizedPath.startsWith("http://") && !normalizedPath.startsWith("https://")) {
            "API path must be relative to the configured server."
        }

        val queryString = query.entries
            .filter { it.value != null }
            .joinToString("&") { (name, value) ->
                "${name.urlEncoded()}=${value.orEmpty().urlEncoded()}"
            }
        val url = "$normalizedBaseUrl/${normalizedPath.trimStart('/')}" +
            queryString.takeIf(String::isNotEmpty)?.let { "?$it" }.orEmpty()
        return URL(url)
    }

    private fun apiException(statusCode: Int, requestPath: String, body: String): ApiException {
        val json = body.takeIf(String::isNotBlank)?.let { raw ->
            runCatching { JSONObject(raw) }.getOrNull()
        }
        val details = json?.optJSONObject("details")?.let { detailObject ->
            buildMap {
                detailObject.keys().forEach { key ->
                    put(key, detailObject.optString(key))
                }
            }
        }.orEmpty()
        val error = ApiError(
            timestamp = json.optNullableString("timestamp"),
            status = json?.optInt("status", statusCode) ?: statusCode,
            code = json.optNullableString("code") ?: "HTTP_$statusCode",
            message = json.optNullableString("message")
                ?: "요청을 처리하지 못했습니다. (HTTP $statusCode)",
            path = json.optNullableString("path") ?: requestPath,
            traceId = json.optNullableString("traceId"),
            details = details,
        )
        return ApiException(statusCode, error)
    }

    private fun unauthenticatedError(path: String) = ApiException(
        statusCode = HttpURLConnection.HTTP_UNAUTHORIZED,
        error = ApiError(
            status = HttpURLConnection.HTTP_UNAUTHORIZED,
            code = "UNAUTHORIZED",
            message = "로그인이 필요합니다.",
            path = path,
        ),
    )

    private fun String.urlEncoded(): String =
        URLEncoder.encode(this, StandardCharsets.UTF_8.name())

    private fun JSONObject?.optNullableString(name: String): String? =
        this?.takeIf { it.has(name) && !it.isNull(name) }
            ?.optString(name)
            ?.takeIf(String::isNotBlank)

    private data class HttpResponse(val statusCode: Int, val body: String)

    private companion object {
        const val JSON_CONTENT_TYPE = "application/json"
        const val CONNECT_TIMEOUT_MILLIS = 10_000
        const val READ_TIMEOUT_MILLIS = 15_000

        fun defaultOkHttpClient(): OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(CONNECT_TIMEOUT_MILLIS.toLong(), TimeUnit.MILLISECONDS)
            .readTimeout(READ_TIMEOUT_MILLIS.toLong(), TimeUnit.MILLISECONDS)
            .build()
    }
}
