package com.gayadi.android.ui.screens

import android.annotation.SuppressLint
import android.net.Uri
import android.util.Log
import android.webkit.ConsoleMessage
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.AltRoute
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.gayadi.android.ui.theme.PrimaryAction
import com.gayadi.android.ui.theme.TextSecondary
import org.json.JSONArray

@Composable
@SuppressLint("SetJavaScriptEnabled")
internal fun TravelRoutePreview(
    plans: List<HomeTravelPlan>,
    javaScriptKey: String,
    baseUrl: String,
    onClick: () -> Unit,
) {
    if (javaScriptKey.isBlank()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .background(Color(0xFFE9E9ED))
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            Text("카카오맵 키를 설정해 주세요", fontSize = 13.sp, color = TextSecondary)
        }
        return
    }

    val placeNamesJson = JSONArray(plans.map { it.title })
        .toString()
        .replace("<", "\\u003c")
        .replace(">", "\\u003e")
        .replace("\u2028", "\\u2028")
        .replace("\u2029", "\\u2029")
    val secureBaseUrl = runCatching {
        Uri.parse(baseUrl).buildUpon().scheme("https").build().toString()
    }.getOrDefault(baseUrl)
    val allowedBaseHost = Uri.parse(secureBaseUrl).host
    val html = """
        <!doctype html>
        <html><head><meta charset="utf-8"/>
        <title>Kakao 지도 시작하기</title>
        <meta name="viewport" content="width=device-width,initial-scale=1,maximum-scale=1,user-scalable=no">
        <style>
          html,body{width:100%;height:100%;margin:0;padding:0}
          #map{width:100%;height:200px;background:#e9e9ed}
          #error{display:none;position:absolute;inset:0;align-items:center;justify-content:center;
            padding:24px;box-sizing:border-box;text-align:center;color:#666;font:13px sans-serif;background:#e9e9ed}
        </style>
        <script type="text/javascript" src="https://dapi.kakao.com/v2/maps/sdk.js?appkey=$javaScriptKey&libraries=services&autoload=false"
          onerror="showError()"></script>
        </head><body><div id="map"></div><div id="error">카카오맵을 불러오지 못했어요.<br>JavaScript SDK 허용 도메인을 확인해 주세요.</div><script>
        function showError() {
          document.getElementById('error').style.display = 'flex';
        }
        console.log('Gayadi Kakao map page started');
        if (!window.kakao || !window.kakao.maps) {
          console.error('Gayadi Kakao SDK unavailable after script load');
          showError();
        } else {
        kakao.maps.load(function() {
        console.log('Gayadi Kakao SDK initialized');
          var container = document.getElementById('map');
          var options = {
            center: new kakao.maps.LatLng(33.450701, 126.570667), level: 3
          };
          var map = new kakao.maps.Map(container, options);
          console.log('Gayadi Kakao map instance created');
          kakao.maps.event.addListener(map, 'tilesloaded', function() {
            console.log('Gayadi Kakao map tiles loaded');
          });
          window.setTimeout(function() {
            map.relayout();
            map.setCenter(options.center);
          }, 300);
          var names = $placeNamesJson;
          if (!names.length) return;
          var places = new kakao.maps.services.Places();
          var points = new Array(names.length);
          var remaining = names.length;
          names.forEach(function(name, index) {
            places.keywordSearch(name, function(result, status) {
              if (status === kakao.maps.services.Status.OK && result.length) {
                var point = new kakao.maps.LatLng(Number(result[0].y), Number(result[0].x));
                points[index] = point;
                new kakao.maps.Marker({ map: map, position: point });
              }
              remaining--;
              if (remaining === 0) {
                var route = points.filter(Boolean);
                if (!route.length) return;
                if (route.length > 1) new kakao.maps.Polyline({
                  map: map, path: route, strokeWeight: 5,
                  strokeColor: '#343548', strokeOpacity: 0.9, strokeStyle: 'solid'
                });
                var bounds = new kakao.maps.LatLngBounds();
                route.forEach(function(point) { bounds.extend(point); });
                map.setBounds(bounds);
              }
            });
          });
        });
        }
        </script></body></html>
    """.trimIndent()

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp),
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
        Button(
            onClick = onClick,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(12.dp)
                .height(48.dp),
            shape = RoundedCornerShape(24.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = PrimaryAction,
                contentColor = Color.White,
            ),
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.AltRoute,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text("전체 동선 보기", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}
