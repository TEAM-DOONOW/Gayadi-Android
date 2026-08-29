package com.gayadi.android.data.remote.travel

import com.gayadi.android.data.remote.GayadiHttpClient
import org.json.JSONArray
import org.json.JSONObject

/** Narrow transport boundary that keeps travel gateway tests independent from the HTTP engine. */
interface TravelJsonTransport {
    suspend fun getObject(
        path: String,
        query: Map<String, String?> = emptyMap(),
    ): JSONObject

    suspend fun getArray(
        path: String,
        query: Map<String, String?> = emptyMap(),
    ): JSONArray

    suspend fun postObject(path: String, body: JSONObject): JSONObject
    suspend fun putObject(path: String, body: JSONObject): JSONObject
    suspend fun patchObject(path: String, body: JSONObject): JSONObject
    suspend fun patchArray(path: String, body: JSONObject): JSONArray
    suspend fun delete(path: String)
}

internal class GayadiTravelJsonTransport(
    private val client: GayadiHttpClient,
) : TravelJsonTransport {
    override suspend fun getObject(path: String, query: Map<String, String?>): JSONObject =
        client.getObject(path, query)

    override suspend fun getArray(path: String, query: Map<String, String?>): JSONArray =
        client.getArray(path, query)

    override suspend fun postObject(path: String, body: JSONObject): JSONObject =
        client.postObject(path, body)

    override suspend fun putObject(path: String, body: JSONObject): JSONObject =
        client.putObject(path, body)

    override suspend fun patchObject(path: String, body: JSONObject): JSONObject =
        client.patchObject(path, body)

    override suspend fun patchArray(path: String, body: JSONObject): JSONArray =
        client.patchArray(path, body)

    override suspend fun delete(path: String) = client.delete(path)
}
