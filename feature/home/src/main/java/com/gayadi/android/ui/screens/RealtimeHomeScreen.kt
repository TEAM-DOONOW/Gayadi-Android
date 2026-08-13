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
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AltRoute
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gayadi.android.feature.home.R
import com.gayadi.android.ui.components.BottomNavBar
import com.gayadi.android.ui.components.BottomTab
import com.gayadi.android.ui.components.UserCharacterAvatar
import com.gayadi.android.ui.theme.AlertBlue
import com.gayadi.android.ui.theme.AlertBlueText
import com.gayadi.android.ui.theme.GayadiTheme
import com.gayadi.android.ui.theme.PrimaryBlue
import com.gayadi.android.ui.theme.PrimaryAction
import com.gayadi.android.ui.theme.TagBlue
import com.gayadi.android.ui.theme.TagBlueText
import com.gayadi.android.ui.theme.TextPrimary
import com.gayadi.android.ui.theme.TextSecondary
import com.gayadi.android.ui.theme.TextTertiary
import org.json.JSONArray
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

data class HomeTravelPlan(
    val title: String,
    val date: String,
    val time: String,
    val isVisited: Boolean,
)

data class HomeTripDay(
    val dayNumber: Int,
    val date: String,
    val dateLabel: String,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RealtimeHomeScreen(
    uiState: RealtimeHomeUiState,
    tripTitle: String,
    travelPlans: List<HomeTravelPlan> = emptyList(),
    tripDays: List<HomeTripDay> = emptyList(),
    participantCount: Int = 0,
    tripStartDate: String = "",
    tripEndDate: String = "",
    tripCountdownText: String = "여행을 준비하고 있어요!",
    tripCoverImageResList: List<Int> = emptyList(),
    kakaoMapJavaScriptKey: String = "",
    kakaoMapBaseUrl: String = "https://localhost",
    friendCharacterKeys: List<String?> = emptyList(),
    onNavigateMyTrip: () -> Unit,
    onNavigateMyPage: () -> Unit,
    onNavigateLedger: () -> Unit = {},
    onNavigatePlaceSearch: () -> Unit,
    onNavigateParticipants: () -> Unit,
    onNavigateSchedule: () -> Unit,
    onNavigateRoutes: () -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF7F7F9)),
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp),
            ) {
                Spacer(modifier = Modifier.height(56.dp))

                Text(
                    text = "두근두근 여행 준비",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF0B263B),
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "$tripTitle 여행일까지",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            lineHeight = 28.sp,
                        )
                        Text(
                            text = buildAnnotatedString {
                                val remainingDays = Regex("^\\d+일").find(tripCountdownText)?.value
                                if (remainingDays != null) {
                                    withStyle(SpanStyle(color = Color(0xFF10395F))) {
                                        append(remainingDays)
                                    }
                                    append(tripCountdownText.removePrefix(remainingDays))
                                } else {
                                    append(tripCountdownText)
                                }
                            },
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            lineHeight = 28.sp,
                        )
                    }
                    tripCoverImageResList.firstOrNull()?.let { imageRes ->
                        Spacer(modifier = Modifier.width(16.dp))
                        Image(
                            painter = painterResource(imageRes),
                            contentDescription = "$tripTitle 대표 사진",
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop,
                        )
                    }
                }
                Spacer(modifier = Modifier.height(28.dp))

                TravelOverviewCard(
                    progress = calculateTripProgress(tripStartDate, tripEndDate),
                    participantCount = participantCount,
                    myCharacterKey = uiState.profile?.characterKey,
                    friendCharacterKeys = friendCharacterKeys,
                    onParticipants = onNavigateParticipants,
                )

                Spacer(modifier = Modifier.height(22.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(Color(0xFFE6E6EA)),
                )
                Spacer(modifier = Modifier.height(22.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(
                        painter = painterResource(R.drawable.map),
                        contentDescription = null,
                        modifier = Modifier.size(28.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("여행 동선", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                }
                Spacer(modifier = Modifier.height(10.dp))
                TravelRoutePreview(
                    plans = travelPlans,
                    javaScriptKey = kakaoMapJavaScriptKey,
                    baseUrl = kakaoMapBaseUrl,
                    onClick = onNavigateRoutes,
                )

                Spacer(modifier = Modifier.height(22.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(Color(0xFFE6E6EA)),
                )
                Spacer(modifier = Modifier.height(22.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(
                        painter = painterResource(R.drawable.calendar),
                        contentDescription = null,
                        modifier = Modifier.size(28.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("여행 계획", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                }
                Spacer(modifier = Modifier.height(14.dp))
                tripDays.forEach { day ->
                    TripDaySection(
                        day = day,
                        plans = travelPlans.filter { it.date == day.date },
                        onAddPlace = onNavigatePlaceSearch,
                        onPlanClick = onNavigateSchedule,
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                }

                Spacer(modifier = Modifier.height(24.dp))
            }

        BottomNavBar(
            currentTab = BottomTab.OUR_TRIP,
            showLedger = true,
            onTabSelected = { tab ->
                when (tab) {
                    BottomTab.MY_TRIP -> onNavigateMyTrip()
                    BottomTab.MY_PAGE -> onNavigateMyPage()
                    BottomTab.LEDGER -> onNavigateLedger()
                    else -> {}
                    }
                },
            )
        }

    }
}

@Composable
private fun TravelOverviewCard(
    progress: Float,
    participantCount: Int,
    myCharacterKey: String?,
    friendCharacterKeys: List<String?>,
    onParticipants: () -> Unit,
) {
    val visibleFriendCharacterKeys = friendCharacterKeys
        .filter { it != myCharacterKey }
        .distinct()
        .take(2)
    val displayedParticipantCount = (1 + visibleFriendCharacterKeys.size)
        .coerceAtMost(participantCount)
    val remainingParticipantCount = (participantCount - displayedParticipantCount)
        .coerceAtLeast(0)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(199.dp),
    ) {
        Image(
            painter = painterResource(R.drawable.gayadi_letter),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.FillBounds,
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 22.dp),
        ) {
            Text(
                "우리 여행 진행률",
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary,
            )
            Spacer(Modifier.height(10.dp))
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(36.dp),
            ) {
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .align(Alignment.Center)
                        .clip(CircleShape),
                    color = PrimaryAction,
                    trackColor = Color(0xFFD9D9DE),
                    drawStopIndicator = {},
                )
                Image(
                    painter = painterResource(R.drawable.car),
                    contentDescription = "여행 진행 위치",
                    modifier = Modifier
                        .size(52.dp)
                        .offset(
                            x = (maxWidth - 52.dp) * progress,
                            y = (-6).dp,
                        ),
                    contentScale = ContentScale.Fit,
                )
            }
            Spacer(Modifier.height(5.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                if (progress > 0f) Spacer(Modifier.weight(progress))
                Text(
                    "${(progress * 100).toInt()}%",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryAction,
                )
                if (progress < 1f) Spacer(Modifier.weight(1f - progress))
            }
            Spacer(Modifier.height(14.dp))
            Column(
                Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onParticipants),
            ) {
                Text(
                    "함께 하는 친구",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary,
                )
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    UserCharacterAvatar(myCharacterKey, "내 캐릭터", Modifier.requiredSize(30.dp))
                    Spacer(Modifier.width(4.dp))
                    visibleFriendCharacterKeys.forEach { key ->
                        UserCharacterAvatar(key, "함께하는 친구", Modifier.requiredSize(30.dp))
                        Spacer(Modifier.width(4.dp))
                    }
                    Box(
                        Modifier
                            .requiredSize(30.dp)
                            .background(Color(0xFFECECF1), CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            if (remainingParticipantCount == 0) "+" else "+$remainingParticipantCount",
                            fontSize = 10.sp,
                            color = TextSecondary,
                        )
                    }
                }
            }
        }
    }
}

internal fun calculateTripProgress(
    startDate: String,
    endDate: String,
    today: LocalDate = LocalDate.now(),
): Float = runCatching {
    val formatter = DateTimeFormatter.ofPattern("yyyy.MM.dd")
    val start = LocalDate.parse(startDate, formatter)
    val end = LocalDate.parse(endDate, formatter)
    when {
        end.isBefore(start) -> 0f
        today.isBefore(start) -> 0f
        !today.isBefore(end) -> 1f
        else -> {
            val totalDays = ChronoUnit.DAYS.between(start, end)
            if (totalDays == 0L) 1f
            else ChronoUnit.DAYS.between(start, today).toFloat() / totalDays
        }
    }
}.getOrDefault(0f)

@Composable
@SuppressLint("SetJavaScriptEnabled")
private fun TravelRoutePreview(
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
        <script type="text/javascript" src="https://dapi.kakao.com/v2/maps/sdk.js?appkey=$javaScriptKey&libraries=services"
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
        (function() {
        console.log('Gayadi Kakao SDK available');
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
        })();
        }
        </script></body></html>
    """.trimIndent()

    AndroidView(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp),
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
}

@Composable
private fun TravelPlanRow(index: Int, plan: HomeTravelPlan, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(0.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE8E8EC)),
    ) {
        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            RouteDot(index.toString(), if (plan.isVisited) PrimaryBlue else PrimaryAction)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(plan.title, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                Text("${plan.date}  ${plan.time}", fontSize = 12.sp, color = TextSecondary)
            }
            Text(if (plan.isVisited) "완료" else "예정", fontSize = 11.sp, color = if (plan.isVisited) PrimaryBlue else TextSecondary)
        }
    }
}

@Composable
private fun TripDaySection(
    day: HomeTripDay,
    plans: List<HomeTravelPlan>,
    onAddPlace: () -> Unit,
    onPlanClick: () -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = "DAY ${day.dayNumber}",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary,
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = day.dateLabel,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = TextSecondary,
        )
    }
    if (plans.isNotEmpty()) {
        Spacer(modifier = Modifier.height(12.dp))
        plans.forEachIndexed { index, plan ->
            TravelPlanRow(index = index + 1, plan = plan, onClick = onPlanClick)
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
    Spacer(modifier = Modifier.height(14.dp))
    Button(
        onClick = onAddPlace,
        modifier = Modifier.fillMaxWidth().height(40.dp),
        shape = RoundedCornerShape(0.dp),
        colors = ButtonDefaults.buttonColors(containerColor = PrimaryAction),
    ) { Text("장소 추가", fontSize = 14.sp, fontWeight = FontWeight.SemiBold) }
}

@Composable
private fun RouteDot(number: String, color: Color) {
    Box(
        modifier = Modifier
            .size(28.dp)
            .background(color = color, shape = CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(number, fontSize = 13.sp, color = Color.White, fontWeight = FontWeight.Bold)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RescheduleBottomSheet(
    onDismiss: () -> Unit,
    onKeep: () -> Unit,
    onAccept: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        containerColor = Color.White,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .width(40.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color(0xFFD0D0D0)),
            )

            Spacer(modifier = Modifier.height(20.dp))

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(AlertBlue)
                    .padding(horizontal = 10.dp, vertical = 4.dp),
            ) {
                Text("실시간 추천 대응", fontSize = 11.sp, color = AlertBlueText, fontWeight = FontWeight.Medium)
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("오후 야외 일정,", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Text("실내로 비꿀까요?", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                }
                Text("🌧️", fontSize = 40.sp)
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text("14-17시 섭지코지 일대 강수 예보", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Color(0xFFE53935))
            Text("야외 유지 시 관람이 어렵고 동선이 꼬여요.", fontSize = 12.sp, color = TextSecondary)

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF8F9FB)),
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("기존", fontSize = 11.sp, color = TextTertiary)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("15:00 섭지코지", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
                        Text("야외 · 우천 영향", fontSize = 11.sp, color = Color(0xFFE53935))
                    }
                }
                Text("→", fontSize = 18.sp, color = TextTertiary, modifier = Modifier.align(Alignment.CenterVertically))
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = AlertBlue),
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("추천", fontSize = 11.sp, color = AlertBlueText)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("15:30 아쿠아플라넷", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
                        Text("실내 · 우천 무관", fontSize = 11.sp, color = AlertBlueText)
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text("실내 대체 장소", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
            Spacer(modifier = Modifier.height(8.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, PrimaryBlue, RoundedCornerShape(12.dp))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFE3F2FD)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("🐠", fontSize = 22.sp)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("아쿠아플라넷 제주", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(TagBlue)
                                    .padding(horizontal = 6.dp, vertical = 2.dp),
                            ) {
                                Text("실내", fontSize = 10.sp, color = TagBlueText)
                            }
                        }
                        Text("실내 · ★4.5 · 차로 8분", fontSize = 11.sp, color = TextSecondary)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Button(
                    onClick = onKeep,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = RoundedCornerShape(0.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = TextPrimary,
                    ),
                ) {
                    Text("유지", fontSize = 15.sp, fontWeight = FontWeight.Medium)
                }
                Button(
                    onClick = onAccept,
                    modifier = Modifier
                        .weight(1.5f)
                        .height(48.dp),
                    shape = RoundedCornerShape(0.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryAction),
                ) {
                    Text("AI 추천대로 변경", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun RealtimeHomePreview() {
    GayadiTheme {
        RealtimeHomeScreen(
            uiState = RealtimeHomeUiState(),
            tripTitle = "제주 여행",
            onNavigateMyTrip = {},
            onNavigateMyPage = {},
            onNavigatePlaceSearch = {},
            onNavigateParticipants = {},
            onNavigateSchedule = {},
            onNavigateRoutes = {},
        )
    }
}
