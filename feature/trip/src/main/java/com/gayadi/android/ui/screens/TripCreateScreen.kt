package com.gayadi.android.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gayadi.android.feature.trip.R
import com.gayadi.android.ui.theme.PretendardFontFamily
import com.gayadi.android.ui.theme.PretendardSemiBoldFontFamily
import com.gayadi.android.ui.theme.TextPrimary
import com.gayadi.android.ui.components.GayadiCompactTextField
import com.gayadi.android.ui.components.GayadiBackButton
import com.gayadi.android.ui.components.GayadiTopAppBar
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.launch

private data class CityOption(val name: String, val areas: String, val imageRes: Int)

internal val tripCreateDateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy.MM.dd")
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

internal fun cityCoverImageResources(cities: List<String>): List<Int> =
    cities.mapNotNull { city -> domesticCities.firstOrNull { it.name == city }?.imageRes }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TripCreateScreen(
    onBack: () -> Unit,
    onCreate: (TripSummary) -> Result<TripSummary>,
    onPublishInvite: suspend (TripSummary) -> Result<Unit> = { Result.success(Unit) },
    onStartTrip: (TripSummary) -> Unit = {},
    onCoordinateDates: (TripSummary) -> Unit = {},
    initialTrip: TripSummary? = null,
) {
    val viewModel: TripCreateViewModel = viewModel(
        key = "trip-create-${initialTrip?.id ?: "new"}",
        factory = TripCreateViewModel.factory(initialTrip),
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val coroutineScope = rememberCoroutineScope()

    when (uiState.step) {
        TripCreateStep.TRAVEL_TYPE -> TravelTypeSelectionStep(
            selectedType = uiState.travelType,
            onTypeSelected = viewModel::selectTravelType,
            onBack = onBack,
            onNext = viewModel::showCityStep,
        )
        TripCreateStep.CITY -> CitySelectionStep(
            selectedCities = uiState.selectedCities,
            onToggleCity = viewModel::toggleCity,
            onBack = {
                if (uiState.isEditing) onBack() else viewModel.showTravelTypeStep()
            },
            onNext = viewModel::showDetailsStep,
        )
        TripCreateStep.DETAILS -> TripDetailsStep(
            isEditing = uiState.isEditing,
            isGroupTrip = uiState.isGroupTrip,
            name = uiState.name,
            onNameChange = viewModel::updateName,
            startDate = uiState.startDate,
            endDate = uiState.endDate,
            onBack = viewModel::showCityStep,
            onSelectStart = { viewModel.openDatePicker(TripDateField.START) },
            onSelectEnd = { viewModel.openDatePicker(TripDateField.END) },
            errorMessage = uiState.errorMessage,
            isSubmitting = uiState.isSubmitting,
            onCreate = {
                viewModel.beginSubmission()
                val trip = viewModel.createDraft()
                onCreate(trip).fold(
                    onSuccess = { savedTrip ->
                        if (!uiState.isEditing) {
                            coroutineScope.launch {
                                onPublishInvite(savedTrip).fold(
                                    onSuccess = { viewModel.complete(savedTrip) },
                                    onFailure = { error ->
                                        viewModel.submissionFailed(
                                            error.message ?: "초대 코드를 서버에 등록하지 못했어요",
                                        )
                                    },
                                )
                            }
                        } else {
                            viewModel.finishEditing()
                        }
                    },
                    onFailure = { viewModel.submissionFailed(it.message ?: "여행을 만들지 못했어요") },
                )
            },
        )
        TripCreateStep.COMPLETE -> uiState.createdTrip?.let { trip ->
            TripCreationCompleteScreen(
                trip = trip,
                onStartTrip = { onStartTrip(trip) },
                onCoordinateDates = { onCoordinateDates(trip) },
            )
        }
    }

    uiState.selectingDateField?.let { field ->
        val initialDate = when (field) {
            TripDateField.START -> uiState.startDate
            TripDateField.END -> uiState.endDate ?: uiState.startDate
        } ?: LocalDate.now()
        TripDatePickerDialog(
            field = field,
            initialDate = initialDate,
            minimumDate = if (field == TripDateField.END) {
                uiState.startDate ?: LocalDate.now()
            } else {
                LocalDate.now()
            },
            onDismiss = viewModel::dismissDatePicker,
            onConfirm = viewModel::selectDate,
        )
    }
}

@Composable
private fun TripDatePickerDialog(
    field: TripDateField,
    initialDate: LocalDate,
    minimumDate: LocalDate,
    onDismiss: () -> Unit,
    onConfirm: (TripDateField, LocalDate) -> Unit,
) {
    var visibleMonth by remember(field, initialDate) { mutableStateOf(YearMonth.from(initialDate)) }
    var selectedDate by remember(field, initialDate) { mutableStateOf(initialDate) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (field == TripDateField.START) "시작일 선택" else "종료일 선택",
                fontFamily = PretendardSemiBoldFontFamily,
                fontSize = 18.sp,
                color = TextPrimary,
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { visibleMonth = visibleMonth.minusMonths(1) }) {
                        Icon(Icons.Default.ChevronLeft, contentDescription = "이전 달")
                    }
                    Text(
                        text = "${visibleMonth.year}년 ${visibleMonth.monthValue}월",
                        modifier = Modifier.weight(1f),
                        fontFamily = PretendardSemiBoldFontFamily,
                        fontSize = 17.sp,
                        color = TextPrimary,
                        textAlign = TextAlign.Center,
                    )
                    IconButton(onClick = { visibleMonth = visibleMonth.plusMonths(1) }) {
                        Icon(Icons.Default.ChevronRight, contentDescription = "다음 달")
                    }
                }
                Row(Modifier.fillMaxWidth()) {
                    listOf("일", "월", "화", "수", "목", "금", "토").forEach { dayOfWeek ->
                        Text(
                            dayOfWeek,
                            Modifier.weight(1f).padding(vertical = 8.dp),
                            textAlign = TextAlign.Center,
                            color = Color(0xFF777781),
                            fontSize = 12.sp,
                        )
                    }
                }
                val leading = visibleMonth.atDay(1).dayOfWeek.value % 7
                val cells = (0 until 42).map { index ->
                    (index - leading + 1).takeIf { it in 1..visibleMonth.lengthOfMonth() }
                }
                cells.chunked(7).forEach { week ->
                    Row(Modifier.fillMaxWidth()) {
                        week.forEach { day ->
                            val date = day?.let(visibleMonth::atDay)
                            val enabled = date != null && !date.isBefore(minimumDate)
                            val selected = date == selectedDate
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1f)
                                    .padding(vertical = 2.dp)
                                    .clip(CircleShape)
                                    .background(if (selected) tripCreateAccentColor else Color.Transparent)
                                    .clickable(enabled = enabled) { selectedDate = date!! },
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = day?.toString().orEmpty(),
                                    color = when {
                                        selected -> Color.White
                                        !enabled -> Color(0xFFC9C9CE)
                                        else -> TextPrimary
                                    },
                                    fontFamily = if (selected) PretendardSemiBoldFontFamily else PretendardFontFamily,
                                    fontSize = 14.sp,
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(field, selectedDate) }) { Text("확인") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("취소") } },
        containerColor = Color.White,
        shape = RoundedCornerShape(14.dp),
    )
}

@Composable
private fun TravelTypeSelectionStep(
    selectedType: TripTravelType?,
    onTypeSelected: (TripTravelType) -> Unit,
    onBack: () -> Unit,
    onNext: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFAFAFB))
            .padding(horizontal = 20.dp),
    ) {
        GayadiTopAppBar(
            title = "새 여행 만들기",
            onBack = onBack,
            backContentDescription = "뒤로가기",
            containerColor = Color(0xFFFAFAFB),
        )

        Box(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "어떤 여행을 떠나시나요?",
                    fontFamily = PretendardSemiBoldFontFamily,
                    fontSize = 22.sp,
                    color = TextPrimary,
                )
                Spacer(modifier = Modifier.height(32.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    TravelTypeButton(
                        label = "혼자 여행가기",
                        selected = selectedType == TripTravelType.SOLO,
                        onClick = { onTypeSelected(TripTravelType.SOLO) },
                        modifier = Modifier.weight(1f),
                        icon = { tint -> Icon(Icons.Default.Person, null, tint = tint, modifier = Modifier.size(38.dp)) },
                    )
                    TravelTypeButton(
                        label = "같이 여행가기",
                        selected = selectedType == TripTravelType.TOGETHER,
                        onClick = { onTypeSelected(TripTravelType.TOGETHER) },
                        modifier = Modifier.weight(1f),
                        icon = { tint -> Icon(Icons.Default.Groups, null, tint = tint, modifier = Modifier.size(38.dp)) },
                    )
                }
            }
        }

        Button(
            onClick = onNext,
            enabled = selectedType != null,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(2.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = tripCreateAccentColor,
                disabledContainerColor = Color(0xFFE8E8EB),
            ),
        ) {
            Text("다음", fontFamily = PretendardSemiBoldFontFamily, fontSize = 15.sp)
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun TravelTypeButton(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: @Composable (Color) -> Unit,
) {
    val shape = RoundedCornerShape(20.dp)
    val contentColor = if (selected) tripCreateAccentColor else Color(0xFF777781)
    Column(
        modifier = modifier
            .height(152.dp)
            .clip(shape)
            .background(if (selected) Color(0xFFF0F0F7) else Color.White)
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) tripCreateAccentColor else Color(0xFFE1E1E6),
                shape = shape,
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        icon(contentColor)
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = label,
            fontFamily = PretendardSemiBoldFontFamily,
            fontSize = 15.sp,
            color = contentColor,
        )
    }
}

@Composable
fun TripCreationCompleteScreen(
    trip: TripSummary,
    onStartTrip: () -> Unit,
    onCoordinateDates: () -> Unit,
) {
    val clipboard = androidx.compose.ui.platform.LocalClipboardManager.current
    val context = LocalContext.current
    val daysUntilTrip = remember(trip.startDate) {
        runCatching {
            java.time.temporal.ChronoUnit.DAYS.between(
                LocalDate.now(),
                LocalDate.parse(trip.startDate, tripCreateDateFormatter),
            )
        }.getOrDefault(0).coerceAtLeast(0)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF7F7F9)),
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .statusBarsPadding()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(40.dp))
            Text(
                text = "새로운 여행이 만들어졌어요!",
                modifier = Modifier.fillMaxWidth(),
                fontFamily = PretendardSemiBoldFontFamily,
                fontSize = 22.sp,
                color = TextPrimary,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(16.dp))
            Box(modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 116.dp)
                        .heightIn(min = 230.dp),
                ) {
                    Image(
                        painter = painterResource(R.drawable.trip_gayadi_letter),
                        contentDescription = null,
                        modifier = Modifier.matchParentSize(),
                        contentScale = ContentScale.FillBounds,
                    )
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 22.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            text = if (trip.isGroupTrip) "날짜 조율 전" else "D-$daysUntilTrip",
                            modifier = Modifier
                                .background(Color(0xFFFFDDD4))
                                .padding(horizontal = 8.dp, vertical = 2.dp),
                            color = Color(0xFFFF5A36),
                            fontFamily = PretendardSemiBoldFontFamily,
                            fontSize = 12.sp,
                        )
                        Spacer(Modifier.height(10.dp))
                        Text(
                            text = trip.name,
                            modifier = Modifier.fillMaxWidth(),
                            fontFamily = PretendardSemiBoldFontFamily,
                            fontSize = 20.sp,
                            color = TextPrimary,
                            textAlign = TextAlign.Center,
                        )
                        Spacer(Modifier.height(5.dp))
                        Text(
                            text = if (trip.isGroupTrip) {
                                "친구들과 가능한 날짜를 정해보세요"
                            } else {
                                "${trip.startDate} - ${trip.endDate}"
                            },
                            modifier = Modifier.fillMaxWidth(),
                            fontFamily = PretendardFontFamily,
                            fontSize = 14.sp,
                            color = Color(0xFF9A9CAB),
                            textAlign = TextAlign.Center,
                        )
                        Spacer(Modifier.height(24.dp))
                        Text(
                            text = trip.inviteCode,
                            fontFamily = PretendardSemiBoldFontFamily,
                            fontSize = 25.sp,
                            color = TextPrimary,
                        )
                        TextButton(
                            onClick = {
                                clipboard.setText(androidx.compose.ui.text.AnnotatedString(trip.inviteCode))
                            },
                        ) {
                            Icon(
                                Icons.Default.ContentCopy,
                                contentDescription = null,
                                tint = Color(0xFF9295A5),
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                "초대코드 복사하기",
                                color = Color(0xFF9295A5),
                                fontFamily = PretendardFontFamily,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                }
                Image(
                    painter = painterResource(R.drawable.ganadi_hello),
                    contentDescription = "편지 위에서 인사하는 가야디",
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth(0.82f)
                        .widthIn(max = 280.dp)
                        .aspectRatio(280f / 223f),
                    contentScale = ContentScale.Fit,
                )
            }
            Spacer(Modifier.height(24.dp))
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp)
                .padding(top = 12.dp, bottom = 20.dp),
        ) {
            Button(
                onClick = {
                    shareTripInviteToKakao(context, trip.name, trip.cities, trip.inviteCode)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 54.dp),
                shape = RoundedCornerShape(2.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFEE500),
                    contentColor = Color(0xFF191919),
                ),
            ) {
                Image(
                    painter = painterResource(R.drawable.kakaotalk),
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "카카오톡으로 공유하기",
                    color = Color(0xFF191919),
                    fontFamily = PretendardSemiBoldFontFamily,
                    fontSize = 16.sp,
                    textAlign = TextAlign.Center,
                )
            }
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = if (trip.isGroupTrip) onCoordinateDates else onStartTrip,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 54.dp),
                shape = RoundedCornerShape(2.dp),
                colors = ButtonDefaults.buttonColors(containerColor = tripCreateAccentColor),
            ) {
                Text(
                    if (trip.isGroupTrip) "가능한 날짜 정하기" else "바로 여행 시작하기",
                    fontFamily = PretendardSemiBoldFontFamily,
                    fontSize = 16.sp,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun CitySelectionStep(
    selectedCities: List<String>,
    onToggleCity: (String) -> Unit,
    onBack: () -> Unit,
    onNext: () -> Unit,
) {
    var query by remember { mutableStateOf("") }
    val cities = domesticCities.filter {
        query.isBlank() || it.name.contains(query, true) || it.areas.contains(query, true)
    }

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFFFAFAFB)).statusBarsPadding()) {
        Row(
            modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp).padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            GayadiBackButton(onClick = onBack, contentDescription = "뒤로가기")
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
                    onClick = { onToggleCity(city.name) },
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
    isGroupTrip: Boolean,
    name: String,
    onNameChange: (String) -> Unit,
    startDate: LocalDate?,
    endDate: LocalDate?,
    onBack: () -> Unit,
    onSelectStart: () -> Unit,
    onSelectEnd: () -> Unit,
    errorMessage: String?,
    isSubmitting: Boolean,
    onCreate: () -> Unit,
) {
    val canCreate = name.isNotBlank() && (isGroupTrip || startDate != null && endDate != null)
    Column(
        modifier = Modifier.fillMaxSize().background(Color(0xFFFAFAFB)).padding(horizontal = 20.dp),
    ) {
        GayadiTopAppBar(
            title = if (isEditing) "여행 수정하기" else "새 여행 만들기",
            onBack = onBack,
            backContentDescription = "도시 선택으로 돌아가기",
            containerColor = Color(0xFFFAFAFB),
        )
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
        if (isGroupTrip) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "여행방을 만든 뒤 친구들과 가능한 날짜를 조율해요.",
                fontFamily = PretendardFontFamily,
                fontSize = 13.sp,
                color = Color(0xFF9295A5),
            )
        } else {
            Spacer(modifier = Modifier.height(22.dp))
            Text("여행 기간", fontFamily = PretendardSemiBoldFontFamily, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                DateField(
                    startDate?.format(tripCreateDateFormatter).orEmpty(),
                    "시작일",
                    onSelectStart,
                    Modifier.weight(1f),
                )
                Text("~", Modifier.padding(horizontal = 10.dp), fontFamily = PretendardSemiBoldFontFamily)
                DateField(
                    endDate?.format(tripCreateDateFormatter).orEmpty(),
                    "종료일",
                    onSelectEnd,
                    Modifier.weight(1f),
                )
            }
        }
        Spacer(modifier = Modifier.weight(1f))
        errorMessage?.let {
            Text(it, color = Color(0xFFE34D59), fontFamily = PretendardFontFamily, fontSize = 13.sp)
            Spacer(modifier = Modifier.height(10.dp))
        }
        Button(
            onClick = onCreate,
            enabled = canCreate && !isSubmitting,
            modifier = Modifier.fillMaxWidth().height(50.dp),
            shape = RoundedCornerShape(2.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = tripCreateAccentColor,
                disabledContainerColor = Color(0xFFD5D5DA),
            ),
        ) {
            Text(
                when {
                    isSubmitting -> "저장 중..."
                    isGroupTrip -> "여행방 만들기"
                    else -> "여행 저장하기"
                },
                fontFamily = PretendardSemiBoldFontFamily,
                fontSize = 15.sp,
            )
        }
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
