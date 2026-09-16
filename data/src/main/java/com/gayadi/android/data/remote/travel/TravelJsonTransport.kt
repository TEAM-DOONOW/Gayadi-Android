package com.gayadi.android.data.remote.travel

import com.gayadi.android.data.datasource.GayadiApiClient
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

internal fun jsonArrayFromBody(body: String): JSONArray {
    val trimmed = body.trim()
    if (trimmed.startsWith("[")) return JSONArray(trimmed)
    val obj = JSONObject(trimmed)
    return obj.optJSONArray("items") ?: obj.optJSONArray("data") ?: JSONArray()
}

internal class GayadiTravelJsonTransport(private val client: GayadiApiClient) : TravelJsonTransport {
    private fun url(path: String, query: Map<String, String?>): String {
        val params = query.filterValues { it != null }.entries.joinToString("&") {
            java.net.URLEncoder.encode(it.key, "UTF-8") + "=" + java.net.URLEncoder.encode(it.value, "UTF-8")
        }
        return path + if (params.isEmpty()) "" else "?$params"
    }
    override suspend fun getObject(path: String, query: Map<String, String?>) = JSONObject(client.request("GET", url(path, query)))
    override suspend fun getArray(path: String, query: Map<String, String?>) =
        jsonArrayFromBody(client.request("GET", url(path, query)))
    override suspend fun postObject(path: String, body: JSONObject) = JSONObject(client.request("POST", path, body.toString()))
    override suspend fun putObject(path: String, body: JSONObject) = JSONObject(client.request("PUT", path, body.toString()))
    override suspend fun patchObject(path: String, body: JSONObject) = JSONObject(client.request("PATCH", path, body.toString()))
    override suspend fun patchArray(path: String, body: JSONObject) = JSONArray(client.request("PATCH", path, body.toString()))
    override suspend fun delete(path: String) { client.request("DELETE", path) }
}
