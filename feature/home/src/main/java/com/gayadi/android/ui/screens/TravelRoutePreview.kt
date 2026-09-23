package com.gayadi.android.ui.screens

import android.annotation.SuppressLint
import android.net.Uri
import android.util.Log
import android.webkit.ConsoleMessage
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.CookieManager
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.gayadi.android.ui.theme.TextSecondary
import java.net.URI
import org.json.JSONArray
import org.json.JSONObject
import com.gayadi.android.ui.theme.SurfaceLight

@Composable
@SuppressLint("SetJavaScriptEnabled")
internal fun TravelRoutePreview(
    plans: List<HomeTravelPlan>,
    javaScriptKey: String,
    baseUrl: String,
) {
    if (javaScriptKey.isBlank()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
                .background(Color(0xFFE9E9ED)),
            contentAlignment = Alignment.Center,
        ) {
            Text("카카오맵 키를 설정해 주세요", fontSize = 13.sp, color = TextSecondary)
        }
        return
    }

    val points = routeMapPoints(plans)
    if (points.isEmpty()) {
        Box(Modifier.fillMaxWidth().height(160.dp).background(SurfaceLight), contentAlignment = Alignment.Center) {
            Text(
                if (plans.isEmpty()) "일정에 장소를 추가하면 지도로 볼 수 있어요." else "표시할 장소 위치가 없어요.",
                color = TextSecondary,
            )
        }
        return
    }
    val pointsJson = JSONArray(points.map { point ->
        JSONObject().put("title", point.title).put("latitude", point.latitude)
            .put("longitude", point.longitude).put("order", point.order).put("date", point.date)
    }).toString().replace("<", "\\u003c").replace(">", "\\u003e")
        .replace("\u2028", "\\u2028").replace("\u2029", "\\u2029")
    val secureBaseUrl = kakaoWebViewOrigin(baseUrl)
    val allowedBaseHost = Uri.parse(secureBaseUrl).host
    val html = """
        <!doctype html>
        <html><head><meta charset="utf-8"/>
        <title>Kakao 지도 시작하기</title>
        <meta name="viewport" content="width=device-width,initial-scale=1,maximum-scale=1,user-scalable=no">
        <style>
          html,body{width:100%;height:100%;margin:0;padding:0}
          #map{width:100%;height:160px;background:#e9e9ed}
          #error{display:none;position:absolute;inset:0;align-items:center;justify-content:center;
            padding:24px;box-sizing:border-box;text-align:center;color:#666;font:13px sans-serif;background:#e9e9ed}
        </style>
        </head><body><div id="map"></div><div id="error">카카오맵을 불러오지 못했어요.<br>JavaScript SDK 허용 도메인을 확인해 주세요.</div><script>
        function showError() {
          document.getElementById('error').style.display = 'flex';
        }
        function initMap() {
        console.log('Gayadi Kakao map page started');
        if (!window.kakao || !window.kakao.maps) {
          console.error('Gayadi Kakao SDK unavailable after script load');
          showError();
          return;
        }
        kakao.maps.load(function() {
        console.log('Gayadi Kakao SDK initialized');
          var stops = $pointsJson;
          var container = document.getElementById('map');
          var options = {
            center: new kakao.maps.LatLng(stops[0].latitude, stops[0].longitude), level: 3
          };
          var map = new kakao.maps.Map(container, options);
          console.log('Gayadi Kakao map instance created');
          kakao.maps.event.addListener(map, 'tilesloaded', function() {
            console.log('Gayadi Kakao map tiles loaded');
          });
          var bounds = new kakao.maps.LatLngBounds();
          var previous = null;
          stops.forEach(function(stop) {
            var point = new kakao.maps.LatLng(stop.latitude, stop.longitude);
            new kakao.maps.Marker({ map: map, position: point, title: stop.order + '. ' + stop.title });
            var label = document.createElement('span');
            label.textContent = String(stop.order);
            label.style.cssText = 'display:block;background:#343548;color:white;border-radius:12px;padding:2px 6px;font:12px sans-serif';
            new kakao.maps.CustomOverlay({ map: map, position: point, content: label, yAnchor: 3 });
            if (previous && previous.stop.date === stop.date && previous.stop.order + 1 === stop.order) {
              new kakao.maps.Polyline({
                map: map, path: [previous.point, point], strokeWeight: 3,
                strokeColor: '#343548', strokeOpacity: 0.7, strokeStyle: 'dash'
              });
            }
            bounds.extend(point);
            previous = { stop: stop, point: point };
          });
          function fitPlaces() {
            map.relayout();
            if (stops.length === 1) map.setCenter(options.center);
            else map.setBounds(bounds);
          }
          fitPlaces();
          window.setTimeout(fitPlaces, 300);

        });
        }
        </script>
        <script type="text/javascript" src="https://dapi.kakao.com/v2/maps/sdk.js?appkey=$javaScriptKey&autoload=false"
          onload="initMap()" onerror="showError()"></script>
        </body></html>
    """.trimIndent()

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp),
    ) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { context ->
                WebView(context).apply {
                    webViewClient = object : WebViewClient() {
                        override fun shouldOverrideUrlLoading(
                            view: WebView?,
                            request: WebResourceRequest?,
                        ): Boolean {
                            if (request?.isForMainFrame != true) return false
                            val host = request.url.host ?: return true
                            val isAllowedHost = host == allowedBaseHost ||
                                host == "kakao.com" || host.endsWith(".kakao.com")
                            return !isAllowedHost
                        }

                        override fun onReceivedError(
                            view: WebView?,
                            request: WebResourceRequest?,
                            error: WebResourceError?,
                        ) {
                            if (request?.isForMainFrame == true || request?.url?.host?.contains("kakao") == true) {
                                Log.e("KakaoMapWebView", "load error ${error?.errorCode}: ${error?.description}")
                            }
                        }
                    }
                    webChromeClient = object : WebChromeClient() {
                        override fun onConsoleMessage(consoleMessage: ConsoleMessage): Boolean {
                            Log.d(
                                "KakaoMapWebView",
                                "${consoleMessage.messageLevel()}: ${consoleMessage.message()}",
                            )
                            return true
                        }
                    }
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                    CookieManager.getInstance().setAcceptCookie(true)
                    CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
                    tag = html
                    loadDataWithBaseURL(secureBaseUrl, html, "text/html", "UTF-8", null)
                }
            },
            update = { webView ->
                if (webView.tag != html) {
                    webView.tag = html
                    webView.loadDataWithBaseURL(secureBaseUrl, html, "text/html", "UTF-8", null)
                }
            },
        )
    }
}

internal fun kakaoWebViewOrigin(baseUrl: String): String {
    val uri = runCatching { URI(baseUrl.trim()) }
        .getOrElse { error -> throw IllegalArgumentException("Invalid Kakao map base URL", error) }
    val scheme = uri.scheme?.lowercase()
    require(scheme == "http" || scheme == "https") {
        "Kakao map base URL must use HTTP or HTTPS."
    }
    val host = requireNotNull(uri.host) { "Kakao map base URL must include a host." }
    return buildString {
        append(scheme)
        append("://")
        append(host)
        if (uri.port != -1) {
            append(':')
            append(uri.port)
        }
    }
}

internal data class RouteMapPoint(
    val title: String,
    val latitude: Double,
    val longitude: Double,
    val date: String,
    val order: Int,
)

internal fun routeMapPoints(plans: List<HomeTravelPlan>): List<RouteMapPoint> = plans.mapIndexedNotNull { index, plan ->
    val latitude = plan.latitude ?: return@mapIndexedNotNull null
    val longitude = plan.longitude ?: return@mapIndexedNotNull null
    if (!latitude.isFinite() || !longitude.isFinite() || latitude !in -90.0..90.0 || longitude !in -180.0..180.0) {
        return@mapIndexedNotNull null
    }
    RouteMapPoint(plan.title, latitude, longitude, plan.date, index + 1)
}
