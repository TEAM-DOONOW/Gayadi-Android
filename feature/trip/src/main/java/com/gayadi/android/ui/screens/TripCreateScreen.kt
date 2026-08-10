package com.gayadi.android.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gayadi.android.feature.trip.R
import com.gayadi.android.ui.theme.PretendardFontFamily
import com.gayadi.android.ui.theme.PretendardSemiBoldFontFamily
import com.gayadi.android.ui.theme.TextPrimary
import com.gayadi.android.ui.components.GayadiCompactTextField
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

private enum class DateField { START, END }
private enum class CreateStep { CITY, DETAILS, COMPLETE }

private data class CityOption(val name: String, val areas: String, val imageRes: Int)

private val tripDateFormatter = DateTimeFormatter.ofPattern("yyyy.MM.dd")
private val tripCreateAccentColor = Color(0xFF343548)
private val domesticCities = listOf(
    CityOption("서울", "서울", R.drawable.city_seoul),
    CityOption("인천", "인천, 강화도", R.drawable.city_incheon),
    CityOption("수원·용인", "수원, 용인, 화성", R.drawable.city_suwon),
    CityOption("가평·양평", "가평, 양평", R.drawable.city_gapyeong),
    CityOption("파주·고양", "파주, 고양", R.drawable.city_paju),
    CityOption("강릉·속초", "강릉, 속초, 양양", R.drawable.city_gangreung),
    CityOption("춘천·홍천", "춘천, 홍천", R.drawable.city_chuncheon),
    CityOption("평창·정선", "평창, 정선", R.drawable.city_pyeongchang),
    CityOption("동해·삼척", "동해, 삼척", R.drawable.city_donghae),
    CityOption("대전", "대전", R.drawable.city_daejeon),
    CityOption("청주", "청주, 증평", R.drawable.city_cheongju),
    CityOption("충주·제천", "충주, 제천, 단양", R.drawable.city_chungju),
    CityOption("태안·보령", "태안, 보령, 서산", R.drawable.city_taean),
    CityOption("공주·부여", "공주, 부여", R.drawable.city_gongju),
    CityOption("전주", "전주, 완주", R.drawable.city_jeonju),
    CityOption("군산·익산", "군산, 익산", R.drawable.city_gunsan),
    CityOption("광주·담양", "광주, 담양", R.drawable.city_gwangju),
    CityOption("목포·신안", "목포, 신안, 무안", R.drawable.city_mokpo),
    CityOption("경주", "경주", R.drawable.city_gyeongju),
    CityOption("대구", "대구", R.drawable.city_daegu),
    CityOption("안동", "안동, 영주", R.drawable.city_andong),
    CityOption("포항", "포항, 영덕", R.drawable.city_pohang),
    CityOption("부산", "부산", R.drawable.city_busan),
    CityOption("울산", "울산", R.drawable.city_ulsan),
    CityOption("창원", "창원, 마산, 진해", R.drawable.city_changwon),
    CityOption("통영·거제", "통영, 거제, 고성", R.drawable.city_geojae),
    CityOption("남해·사천", "남해, 사천", R.drawable.city_namhae),
    CityOption("여수", "여수, 순천", R.drawable.city_yeosu),
    CityOption("해남·완도", "해남, 완도", R.drawable.city_haenam),
    CityOption("제주", "제주", R.drawable.city_jeju),
    CityOption("서귀포", "서귀포", R.drawable.city_seoguipo),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TripCreateScreen(
    onBack: () -> Unit,
    onCreate: (TripSummary) -> TripSummary,
    onStartTrip: (TripSummary) -> Unit = {},
    onInviteFriend: (TripSummary) -> Unit = {},
    initialTrip: TripSummary? = null,
) {
    var step by remember(initialTrip?.id) {
        mutableStateOf(if (initialTrip == null) CreateStep.CITY else CreateStep.DETAILS)
    }
    val selectedCities = remember(initialTrip?.id) {
        mutableStateListOf<String>().apply { addAll(initialTrip?.cities.orEmpty()) }
    }
    var name by remember(initialTrip?.id) { mutableStateOf(initialTrip?.name.orEmpty()) }
    var startDate by remember(initialTrip?.id) {
        mutableStateOf(initialTrip?.startDate?.let { runCatching { LocalDate.parse(it, tripDateFormatter) }.getOrNull() })
    }
    var endDate by remember(initialTrip?.id) {
        mutableStateOf(initialTrip?.endDate?.let { runCatching { LocalDate.parse(it, tripDateFormatter) }.getOrNull() })
    }
    var selectingField by remember { mutableStateOf<DateField?>(null) }
    var createdTrip by remember { mutableStateOf<TripSummary?>(null) }

    when (step) {
        CreateStep.CITY -> CitySelectionStep(
            selectedCities = selectedCities,
            onBack = onBack,
            onNext = { step = CreateStep.DETAILS },
        )
        CreateStep.DETAILS -> TripDetailsStep(
            isEditing = initialTrip != null,
            name = name,
            onNameChange = { name = it },
            startDate = startDate,
            endDate = endDate,
            onBack = { step = CreateStep.CITY },
            onSelectStart = { selectingField = DateField.START },
            onSelectEnd = { selectingField = DateField.END },
            onCreate = {
                val trip = TripSummary(
                        id = initialTrip?.id ?: java.util.UUID.randomUUID().toString(),
                        name = name.trim(),
                        startDate = startDate!!.format(tripDateFormatter),
                        endDate = endDate!!.format(tripDateFormatter),
                        cities = selectedCities.toList(),
                        coverImageResList = selectedCities.mapNotNull { selectedCity ->
                            domesticCities.firstOrNull { it.name == selectedCity }?.imageRes
                        },
                    )
                val savedTrip = onCreate(trip)
                if (initialTrip == null) {
                    createdTrip = savedTrip
                    step = CreateStep.COMPLETE
                }
            },
        )
        CreateStep.COMPLETE -> createdTrip?.let { trip ->
            TripCreationCompleteStep(
                trip = trip,
                onInviteFriend = { onInviteFriend(trip) },
                onStartTrip = { onStartTrip(trip) },
            )
        }
    }

    selectingField?.let { field ->
        val initialDate = when (field) {
            DateField.START -> startDate
            DateField.END -> endDate ?: startDate
        } ?: LocalDate.now()
        val pickerState = rememberDatePickerState(initialSelectedDateMillis = initialDate.toUtcMillis())

        DatePickerDialog(
            onDismissRequest = { selectingField = null },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.toLocalDate()?.let { selectedDate ->
                        when (field) {
                            DateField.START -> {
                                startDate = selectedDate
                                if (endDate?.isBefore(selectedDate) == true) endDate = null
                            }
                            DateField.END -> if (startDate == null || !selectedDate.isBefore(startDate)) {
                                endDate = selectedDate
                            }
                        }
                    }
                    selectingField = null
                }) { Text("확인") }
            },
            dismissButton = { TextButton(onClick = { selectingField = null }) { Text("취소") } },
        ) { DatePicker(state = pickerState) }
    }
}

@Composable
private fun TripCreationCompleteStep(
    trip: TripSummary,
    onInviteFriend: () -> Unit,
    onStartTrip: () -> Unit,
) {
    val clipboard = androidx.compose.ui.platform.LocalClipboardManager.current
    val daysUntilTrip = remember(trip.startDate) {
        runCatching {
            java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), LocalDate.parse(trip.startDate, tripDateFormatter))
        }.getOrDefault(0).coerceAtLeast(0)
    }

    Column(
        modifier = Modifier.fillMaxSize().background(Color(0xFFF7F7F9)).padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(118.dp))
        Text(
            "새로운 여행이 만들어졌어요!",
            fontFamily = PretendardSemiBoldFontFamily,
            fontSize = 25.sp,
            color = TextPrimary,
        )
        Spacer(Modifier.height(14.dp))
        Box(
            modifier = Modifier.fillMaxWidth().height(340.dp),
        ) {
            Box(
                modifier = Modifier.fillMaxWidth().height(252.dp).align(Alignment.BottomCenter),
            ) {
                Image(
                    painter = painterResource(R.drawable.gayadi_letter),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.FillBounds,
                )
                Column(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Spacer(Modifier.height(22.dp))
                    Text(
                        "D-$daysUntilTrip",
                        modifier = Modifier.background(Color(0xFFFFDDD4)).padding(horizontal = 8.dp, vertical = 2.dp),
                        color = Color(0xFFFF5A36),
                        fontFamily = PretendardSemiBoldFontFamily,
                        fontSize = 12.sp,
                    )
                    Spacer(Modifier.height(10.dp))
                    Text(trip.name, fontFamily = PretendardSemiBoldFontFamily, fontSize = 20.sp, color = TextPrimary)
                    Spacer(Modifier.height(5.dp))
                    Text("${trip.startDate} - ${trip.endDate}", fontFamily = PretendardFontFamily, fontSize = 14.sp, color = Color(0xFF9A9CAB))
                    Spacer(Modifier.weight(0.35f))
                    Text(trip.inviteCode, fontFamily = PretendardSemiBoldFontFamily, fontSize = 25.sp, color = TextPrimary)
                    TextButton(onClick = { clipboard.setText(androidx.compose.ui.text.AnnotatedString(trip.inviteCode)) }) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, tint = Color(0xFF9295A5), modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("초대코드 복사하기", color = Color(0xFF9295A5), fontFamily = PretendardFontFamily)
                    }
                    Spacer(Modifier.weight(0.08f))
                }
            }
            Image(
                painter = painterResource(R.drawable.ganadi_hello),
                contentDescription = "편지 위에서 인사하는 가야디",
                modifier = Modifier.width(280.dp).height(223.dp).align(Alignment.TopCenter).offset(y = (-25).dp),
                contentScale = ContentScale.Fit,
            )
        }
        Spacer(Modifier.weight(1f))
        OutlinedButton(
            onClick = onInviteFriend,
            modifier = Modifier.fillMaxWidth().height(54.dp),
            shape = RoundedCornerShape(6.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE0E1E7)),
        ) { Text("친구에게 초대코드 보내기", color = tripCreateAccentColor, fontFamily = PretendardSemiBoldFontFamily, fontSize = 16.sp) }
        Spacer(Modifier.height(12.dp))
        Button(
            onClick = onStartTrip,
            modifier = Modifier.fillMaxWidth().height(54.dp),
            shape = RoundedCornerShape(6.dp),
            colors = ButtonDefaults.buttonColors(containerColor = tripCreateAccentColor),
        ) { Text("바로 여행 시작하기", fontFamily = PretendardSemiBoldFontFamily, fontSize = 16.sp) }
        Spacer(Modifier.height(20.dp))
    }
}

@Composable
private fun CitySelectionStep(
    selectedCities: MutableList<String>,
    onBack: () -> Unit,
    onNext: () -> Unit,
) {
    var query by remember { mutableStateOf("") }
    val cities = domesticCities.filter {
        query.isBlank() || it.name.contains(query, true) || it.areas.contains(query, true)
    }

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFFFAFAFB))) {
        Spacer(modifier = Modifier.height(42.dp))
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로가기")
            }
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("여행, 어디로 떠나시나요?", fontFamily = PretendardFontFamily) },
                trailingIcon = { Icon(Icons.Default.Search, contentDescription = "도시 검색") },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = Color.Transparent,
                    focusedBorderColor = Color.Transparent,
                ),
            )
        }
        Column(
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                "국내 여행 도시",
                fontFamily = PretendardSemiBoldFontFamily,
                fontSize = 15.sp,
                color = tripCreateAccentColor,
            )
            Spacer(modifier = Modifier.height(14.dp))
            Box(Modifier.fillMaxWidth().height(3.dp).background(tripCreateAccentColor))
        }
        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 20.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            items(cities, key = { it.name }) { city ->
                CityRow(
                    city = city,
                    selected = city.name in selectedCities,
                    onClick = {
                        if (city.name in selectedCities) selectedCities.remove(city.name)
                        else selectedCities.add(city.name)
                    },
                )
            }
        }
        Button(
            onClick = onNext,
            enabled = selectedCities.isNotEmpty(),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).height(52.dp),
            shape = RoundedCornerShape(2.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = tripCreateAccentColor,
                disabledContainerColor = Color(0xFFE8E8EB),
            ),
        ) {
            Text(
                if (selectedCities.isEmpty()) "최소 1개 도시 선택" else "${selectedCities.size}개 도시 선택 완료",
                fontFamily = PretendardSemiBoldFontFamily,
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun CityRow(city: CityOption, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            painter = painterResource(city.imageRes),
            contentDescription = "${city.name} 도시 이미지",
            modifier = Modifier.size(52.dp).clip(CircleShape),
            contentScale = ContentScale.Crop,
        )
        Column(modifier = Modifier.weight(1f).padding(horizontal = 14.dp)) {
            Text(city.name, fontFamily = PretendardSemiBoldFontFamily, fontSize = 16.sp, color = TextPrimary)
            Spacer(modifier = Modifier.height(3.dp))
            Text(city.areas, fontFamily = PretendardFontFamily, fontSize = 13.sp, color = Color(0xFFABABB1))
        }
        Text(
            text = if (selected) "선택됨" else "선택",
            modifier = Modifier
                .background(if (selected) tripCreateAccentColor else Color(0xFFF0F0F2), RoundedCornerShape(18.dp))
                .padding(horizontal = 20.dp, vertical = 8.dp),
            color = if (selected) Color.White else TextPrimary,
            fontFamily = PretendardSemiBoldFontFamily,
            fontSize = 13.sp,
        )
    }
}

@Composable
private fun TripDetailsStep(
    isEditing: Boolean,
    name: String,
    onNameChange: (String) -> Unit,
    startDate: LocalDate?,
    endDate: LocalDate?,
    onBack: () -> Unit,
    onSelectStart: () -> Unit,
    onSelectEnd: () -> Unit,
    onCreate: () -> Unit,
) {
    val canCreate = name.isNotBlank() && startDate != null && endDate != null
    Column(
        modifier = Modifier.fillMaxSize().background(Color(0xFFFAFAFB)).padding(horizontal = 20.dp),
    ) {
        Spacer(modifier = Modifier.height(42.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "도시 선택으로 돌아가기")
            }
            Text(
                if (isEditing) "여행 수정하기" else "새 여행 만들기",
                fontFamily = PretendardSemiBoldFontFamily,
                fontSize = 20.sp,
                color = TextPrimary,
            )
        }
        Spacer(modifier = Modifier.height(28.dp))
        Text("여행 이름", fontFamily = PretendardSemiBoldFontFamily, fontSize = 14.sp)
        Spacer(modifier = Modifier.height(8.dp))
        GayadiCompactTextField(
            label = "여행 이름",
            value = name,
            onValueChange = onNameChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = "예: 여름 제주 여행",
        )
        Spacer(modifier = Modifier.height(22.dp))
        Text("여행 기간", fontFamily = PretendardSemiBoldFontFamily, fontSize = 14.sp)
        Spacer(modifier = Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            DateField(startDate?.format(tripDateFormatter).orEmpty(), "시작일", onSelectStart, Modifier.weight(1f))
            Text("~", Modifier.padding(horizontal = 10.dp), fontFamily = PretendardSemiBoldFontFamily)
            DateField(endDate?.format(tripDateFormatter).orEmpty(), "종료일", onSelectEnd, Modifier.weight(1f))
        }
        Spacer(modifier = Modifier.weight(1f))
        Button(
            onClick = onCreate,
            enabled = canCreate,
            modifier = Modifier.fillMaxWidth().height(50.dp),
            shape = RoundedCornerShape(2.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = tripCreateAccentColor,
                disabledContainerColor = Color(0xFFD5D5DA),
            ),
        ) { Text("여행 저장하기", fontFamily = PretendardSemiBoldFontFamily, fontSize = 15.sp) }
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun DateField(value: String, placeholder: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    GayadiCompactTextField(
        label = placeholder,
        value = value,
        onValueChange = {},
        modifier = modifier,
        placeholder = placeholder,
        onClick = onClick,
        trailingContent = {
            Icon(
                Icons.Default.DateRange,
                contentDescription = "$placeholder 달력 열기",
                tint = tripCreateAccentColor,
                modifier = Modifier.size(18.dp),
            )
        },
    )
}

private fun LocalDate.toUtcMillis(): Long = atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
private fun Long.toLocalDate(): LocalDate = Instant.ofEpochMilli(this).atZone(ZoneOffset.UTC).toLocalDate()
