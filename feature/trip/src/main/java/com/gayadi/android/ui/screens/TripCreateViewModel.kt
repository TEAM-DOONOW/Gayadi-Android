package com.gayadi.android.ui.screens

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import java.time.LocalDate
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

enum class TripCreateStep { TRAVEL_TYPE, CITY, DETAILS, COMPLETE }

enum class TripTravelType { SOLO, TOGETHER }

enum class TripDateField { START, END }

data class TripCreateUiState(
    val step: TripCreateStep = TripCreateStep.TRAVEL_TYPE,
    val travelType: TripTravelType? = null,
    val selectedCities: List<String> = emptyList(),
    val name: String = "",
    val startDate: LocalDate? = null,
    val endDate: LocalDate? = null,
    val selectingDateField: TripDateField? = null,
    val createdTrip: TripSummary? = null,
    val errorMessage: String? = null,
    val isSubmitting: Boolean = false,
    val editingTripId: String? = null,
) {
    val isEditing: Boolean get() = editingTripId != null
    val isGroupTrip: Boolean get() = travelType == TripTravelType.TOGETHER
}

class TripCreateViewModel(
    private val savedStateHandle: SavedStateHandle,
    initialTrip: TripSummary? = null,
) : ViewModel() {
    private val _uiState = MutableStateFlow(
        if (savedStateHandle.get<Boolean>(INITIALIZED_KEY) == true) restoreState() else initialState(initialTrip),
    )
    val uiState = _uiState.asStateFlow()

    init {
        persist(_uiState.value)
    }

    fun selectTravelType(type: TripTravelType) = update { it.copy(travelType = type) }

    fun showTravelTypeStep() = update { it.copy(step = TripCreateStep.TRAVEL_TYPE) }

    fun showCityStep() = update { it.copy(step = TripCreateStep.CITY) }

    fun showDetailsStep() = update { it.copy(step = TripCreateStep.DETAILS) }

    fun toggleCity(city: String) = update { state ->
        state.copy(
            selectedCities = if (city in state.selectedCities) {
                state.selectedCities - city
            } else {
                state.selectedCities + city
            },
        )
    }

    fun updateName(name: String) = update { it.copy(name = name, errorMessage = null) }

    fun openDatePicker(field: TripDateField) = update { it.copy(selectingDateField = field) }

    fun dismissDatePicker() = update { it.copy(selectingDateField = null) }

    fun selectDate(field: TripDateField, date: LocalDate) = update { state ->
        when (field) {
            TripDateField.START -> state.copy(
                startDate = date,
                endDate = state.endDate?.takeUnless { it.isBefore(date) },
                selectingDateField = null,
                errorMessage = null,
            )
            TripDateField.END -> if (state.startDate == null || !date.isBefore(state.startDate)) {
                state.copy(endDate = date, selectingDateField = null, errorMessage = null)
            } else {
                state.copy(selectingDateField = null)
            }
        }
    }

    fun createDraft(): TripSummary {
        val state = _uiState.value
        return TripSummary(
            id = state.editingTripId ?: UUID.randomUUID().toString(),
            name = state.name.trim(),
            startDate = state.startDate?.format(tripCreateDateFormatter).orEmpty(),
            endDate = state.endDate?.format(tripCreateDateFormatter).orEmpty(),
            cities = state.selectedCities,
            coverImageResList = cityCoverImageResources(state.selectedCities),
            isGroupTrip = state.isGroupTrip,
        )
    }

    fun beginSubmission() = update { it.copy(isSubmitting = true, errorMessage = null) }

    fun submissionFailed(message: String) = update {
        it.copy(isSubmitting = false, errorMessage = message)
    }

    fun finishEditing() = update { it.copy(isSubmitting = false, errorMessage = null) }

    fun complete(trip: TripSummary) = update {
        it.copy(
            step = TripCreateStep.COMPLETE,
            createdTrip = trip,
            isSubmitting = false,
            errorMessage = null,
        )
    }

    private fun update(transform: (TripCreateUiState) -> TripCreateUiState) {
        _uiState.update(transform)
        persist(_uiState.value)
    }

    private fun initialState(initialTrip: TripSummary?): TripCreateUiState = if (initialTrip == null) {
        TripCreateUiState()
    } else {
        TripCreateUiState(
            step = TripCreateStep.DETAILS,
            travelType = if (initialTrip.isGroupTrip) TripTravelType.TOGETHER else TripTravelType.SOLO,
            selectedCities = initialTrip.cities,
            name = initialTrip.name,
            startDate = initialTrip.startDate.toTripCreateDate(),
            endDate = initialTrip.endDate.toTripCreateDate(),
            editingTripId = initialTrip.id,
        )
    }

    private fun restoreState(): TripCreateUiState = TripCreateUiState(
        step = savedStateHandle.get<String>(STEP_KEY)?.toEnumOrNull<TripCreateStep>()
            ?: TripCreateStep.TRAVEL_TYPE,
        travelType = savedStateHandle.get<String>(TRAVEL_TYPE_KEY)?.toEnumOrNull<TripTravelType>(),
        selectedCities = savedStateHandle.get<ArrayList<String>>(CITIES_KEY).orEmpty(),
        name = savedStateHandle[NAME_KEY] ?: "",
        startDate = savedStateHandle.get<String>(START_DATE_KEY).toTripCreateDate(),
        endDate = savedStateHandle.get<String>(END_DATE_KEY).toTripCreateDate(),
        selectingDateField = savedStateHandle.get<String>(DATE_FIELD_KEY)?.toEnumOrNull<TripDateField>(),
        createdTrip = restoreCreatedTrip(),
        errorMessage = savedStateHandle[ERROR_KEY],
        isSubmitting = false,
        editingTripId = savedStateHandle[EDITING_TRIP_ID_KEY],
    )

    private fun restoreCreatedTrip(): TripSummary? {
        val id = savedStateHandle.get<String>(CREATED_TRIP_ID_KEY)?.takeIf(String::isNotBlank) ?: return null
        return TripSummary(
            id = id,
            name = savedStateHandle[CREATED_TRIP_NAME_KEY] ?: "",
            startDate = savedStateHandle[CREATED_TRIP_START_KEY] ?: "",
            endDate = savedStateHandle[CREATED_TRIP_END_KEY] ?: "",
            cities = savedStateHandle.get<ArrayList<String>>(CREATED_TRIP_CITIES_KEY).orEmpty(),
            coverImageResList = savedStateHandle.get<ArrayList<Int>>(CREATED_TRIP_IMAGES_KEY).orEmpty(),
            inviteCode = savedStateHandle[CREATED_TRIP_INVITE_KEY] ?: "",
            isGroupTrip = savedStateHandle[CREATED_TRIP_GROUP_KEY] ?: false,
        )
    }

    private fun persist(state: TripCreateUiState) {
        savedStateHandle[INITIALIZED_KEY] = true
        savedStateHandle[STEP_KEY] = state.step.name
        savedStateHandle[TRAVEL_TYPE_KEY] = state.travelType?.name
        savedStateHandle[CITIES_KEY] = ArrayList(state.selectedCities)
        savedStateHandle[NAME_KEY] = state.name
        savedStateHandle[START_DATE_KEY] = state.startDate?.format(tripCreateDateFormatter)
        savedStateHandle[END_DATE_KEY] = state.endDate?.format(tripCreateDateFormatter)
        savedStateHandle[DATE_FIELD_KEY] = state.selectingDateField?.name
        savedStateHandle[ERROR_KEY] = state.errorMessage
        savedStateHandle[EDITING_TRIP_ID_KEY] = state.editingTripId
        state.createdTrip?.let { trip ->
            savedStateHandle[CREATED_TRIP_ID_KEY] = trip.id
            savedStateHandle[CREATED_TRIP_NAME_KEY] = trip.name
            savedStateHandle[CREATED_TRIP_START_KEY] = trip.startDate
            savedStateHandle[CREATED_TRIP_END_KEY] = trip.endDate
            savedStateHandle[CREATED_TRIP_CITIES_KEY] = ArrayList(trip.cities)
            savedStateHandle[CREATED_TRIP_IMAGES_KEY] = ArrayList(trip.coverImageResList)
            savedStateHandle[CREATED_TRIP_INVITE_KEY] = trip.inviteCode
            savedStateHandle[CREATED_TRIP_GROUP_KEY] = trip.isGroupTrip
        }
    }

    companion object {
        fun factory(initialTrip: TripSummary? = null) = viewModelFactory {
            initializer { TripCreateViewModel(createSavedStateHandle(), initialTrip) }
        }

        private const val INITIALIZED_KEY = "trip_create_initialized"
        private const val STEP_KEY = "trip_create_step"
        private const val TRAVEL_TYPE_KEY = "trip_create_travel_type"
        private const val CITIES_KEY = "trip_create_cities"
        private const val NAME_KEY = "trip_create_name"
        private const val START_DATE_KEY = "trip_create_start_date"
        private const val END_DATE_KEY = "trip_create_end_date"
        private const val DATE_FIELD_KEY = "trip_create_date_field"
        private const val ERROR_KEY = "trip_create_error"
        private const val EDITING_TRIP_ID_KEY = "trip_create_editing_trip_id"
        private const val CREATED_TRIP_ID_KEY = "trip_create_created_id"
        private const val CREATED_TRIP_NAME_KEY = "trip_create_created_name"
        private const val CREATED_TRIP_START_KEY = "trip_create_created_start"
        private const val CREATED_TRIP_END_KEY = "trip_create_created_end"
        private const val CREATED_TRIP_CITIES_KEY = "trip_create_created_cities"
        private const val CREATED_TRIP_IMAGES_KEY = "trip_create_created_images"
        private const val CREATED_TRIP_INVITE_KEY = "trip_create_created_invite"
        private const val CREATED_TRIP_GROUP_KEY = "trip_create_created_group"
    }
}

private inline fun <reified T : Enum<T>> String.toEnumOrNull(): T? =
    runCatching { enumValueOf<T>(this) }.getOrNull()

private fun String?.toTripCreateDate(): LocalDate? =
    this?.takeIf(String::isNotBlank)?.let {
        runCatching { LocalDate.parse(it, tripCreateDateFormatter) }.getOrNull()
    }
