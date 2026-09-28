package com.gayadi.android.data

import com.gayadi.android.data.mapper.toWeatherResult
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class WeatherMapperTest {
    @Test fun nestedWeatherIncludesConditionAndRainProbability() {
        val result = JSONObject("""{"weather":{"available":true,"condition":"맑음","temperatureCelsius":23,"precipitationProbability":10}}""").toWeatherResult()
        assertTrue(result.available)
        assertEquals("맑음", result.condition)
        assertEquals("23", result.temperature)
        assertEquals(10, result.precipitationProbability)
    }

    @Test fun unavailableAndMissingValuesArePreserved() {
        val result = JSONObject("""{"available":false,"temperatureCelsius":null,"precipitationProbability":101}""").toWeatherResult()
        assertFalse(result.available)
        assertNull(result.temperature)
        assertNull(result.condition)
        assertNull(result.precipitationProbability)
    }

    @Test fun zeroProbabilityAndLegacyTemperatureRemainValid() {
        val result = JSONObject("""{"temperature":"0","precipitationProbability":0}""").toWeatherResult()
        assertEquals("0", result.temperature)
        assertEquals(0, result.precipitationProbability)
    }
}
