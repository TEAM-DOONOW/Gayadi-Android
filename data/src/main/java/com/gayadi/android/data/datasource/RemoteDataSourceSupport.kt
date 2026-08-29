package com.gayadi.android.data.datasource

import com.gayadi.android.data.remote.GayadiHttpClient
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

internal interface RemoteJsonClient {
    suspend fun getObject(
        path: String,
        query: Map<String, String?> = emptyMap(),
        authenticated: Boolean = true,
    ): JSONObject

    suspend fun getArray(
        path: String,
        query: Map<String, String?> = emptyMap(),
        authenticated: Boolean = true,
    ): JSONArray

    suspend fun postObject(
        path: String,
        body: JSONObject,
        authenticated: Boolean = true,
    ): JSONObject
}

internal class GayadiRemoteJsonClient(
    private val httpClient: GayadiHttpClient,
) : RemoteJsonClient {
    override suspend fun getObject(
        path: String,
        query: Map<String, String?>,
        authenticated: Boolean,
    ): JSONObject = httpClient.getObject(path, query, authenticated)

    override suspend fun getArray(
        path: String,
        query: Map<String, String?>,
        authenticated: Boolean,
    ): JSONArray = httpClient.getArray(path, query, authenticated)

    override suspend fun postObject(
        path: String,
        body: JSONObject,
        authenticated: Boolean,
    ): JSONObject = httpClient.postObject(path, body, authenticated)
}

internal fun defaultRemoteDataSourceScope(): CoroutineScope =
    CoroutineScope(SupervisorJob() + Dispatchers.IO)

internal fun <T> CoroutineScope.launchRemoteRequest(
    callback: (Result<T>) -> Unit,
    request: suspend () -> T,
) {
    launch {
        val result = try {
            Result.success(request())
        } catch (error: CancellationException) {
            throw error
        } catch (error: Throwable) {
            Result.failure(error)
        }
        callback(result)
    }
}
