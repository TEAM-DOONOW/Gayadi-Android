package com.gayadi.android.data.remote.ranking

import com.gayadi.android.data.datasource.GayadiApiClient
import com.gayadi.android.domain.model.RankingItem
import com.gayadi.android.domain.model.RankingList
import com.gayadi.android.domain.model.RankingType
import com.gayadi.android.domain.repository.RankingGateway
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import org.json.JSONObject

class ServerRankingGateway(
    private val api: GayadiApiClient,
) : RankingGateway {
    override suspend fun getRankings(type: RankingType, region: String, limit: Int): RankingList {
        val query = buildList {
            add("type=${type.name}")
            region.trim().takeIf(String::isNotEmpty)?.let { add("region=${encode(it)}") }
            add("limit=${limit.coerceIn(1, 20)}")
        }.joinToString("&")
        return JSONObject(api.request("GET", "/api/v1/rankings?$query")).toRankingList(type)
    }
}

internal fun JSONObject.toRankingList(requested: RankingType): RankingList {
    val array = optJSONArray("items")
    val items = buildList {
        if (array != null) {
            for (index in 0 until array.length()) {
                val item = array.optJSONObject(index) ?: continue
                val title = item.optString("title").trim()
                if (title.isEmpty()) continue
                add(
                    RankingItem(
                        rank = item.optInt("rank", index + 1),
                        title = title,
                        subtitle = item.optCleanString("subtitle"),
                        imageUrl = item.optCleanString("imageUrl"),
                        latitude = item.optFiniteDouble("latitude"),
                        longitude = item.optFiniteDouble("longitude"),
                        placeId = if (item.isNull("placeId")) null else item.opt("placeId")?.toString(),
                        contentId = item.optCleanString("contentId").ifEmpty { null },
                        metricLabel = item.optCleanString("metricLabel"),
                    ),
                )
            }
        }
    }
    return RankingList(
        type = optString("type").let { value -> RankingType.entries.firstOrNull { it.name == value } } ?: requested,
        region = optCleanString("region"),
        basePeriod = optCleanString("basePeriod"),
        providerDataAvailable = optBoolean("providerDataAvailable", false),
        items = items,
    )
}

private fun JSONObject.optCleanString(name: String): String =
    if (isNull(name)) "" else optString(name).trim()

private fun JSONObject.optFiniteDouble(name: String): Double? =
    if (isNull(name)) null else optDouble(name).takeIf { it.isFinite() }

private fun encode(value: String): String = URLEncoder.encode(value, StandardCharsets.UTF_8.name())
