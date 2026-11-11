package com.jigneshgaming.micai.ai

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Weather via Open-Meteo (free, no API key required). Geocoding + forecast
 * are two plain HTTPS calls. Returns a human sentence or null when offline.
 */
class WeatherClient {

    private val http = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    suspend fun weatherFor(city: String): String? = withContext(Dispatchers.IO) {
        try {
            val geoUrl = "https://geocoding-api.open-meteo.com/v1/search" +
                "?name=${java.net.URLEncoder.encode(city, "UTF-8")}&count=1&language=en&format=json"
            val geoText = http.newCall(Request.Builder().url(geoUrl).build())
                .execute().use { if (it.isSuccessful) it.body?.string() else null }
                ?: return@withContext null
            val results = JSONObject(geoText).optJSONArray("results")
                ?: return@withContext null
            if (results.length() == 0) return@withContext "I could not find a place called $city."
            val place = results.getJSONObject(0)
            val lat = place.getDouble("latitude")
            val lon = place.getDouble("longitude")
            val name = place.optString("name", city)
            val country = place.optString("country", "")

            val wxUrl = "https://api.open-meteo.com/v1/forecast" +
                "?latitude=$lat&longitude=$lon&current=temperature_2m,relative_humidity_2m," +
                "apparent_temperature,weather_code,wind_speed_10m&timezone=auto"
            val wxText = http.newCall(Request.Builder().url(wxUrl).build())
                .execute().use { if (it.isSuccessful) it.body?.string() else null }
                ?: return@withContext null
            val current = JSONObject(wxText).optJSONObject("current")
                ?: return@withContext null
            val temp = current.optDouble("temperature_2m", Double.NaN)
            val feels = current.optDouble("apparent_temperature", Double.NaN)
            val humidity = current.optInt("relative_humidity_2m", -1)
            val wind = current.optDouble("wind_speed_10m", Double.NaN)
            val code = current.optInt("weather_code", -1)
            val where = if (country.isNotBlank()) "$name, $country" else name
            buildString {
                append("In $where it is ${describe(code)}")
                if (!temp.isNaN()) append(", about ${temp.toInt()} degrees")
                if (!feels.isNaN()) append(" (feels like ${feels.toInt()})")
                if (humidity >= 0) append(", humidity $humidity percent")
                if (!wind.isNaN()) append(", wind ${wind.toInt()} kilometres per hour")
                append(".")
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun describe(code: Int): String = when (code) {
        0 -> "clear sky"
        1, 2, 3 -> "partly cloudy"
        45, 48 -> "foggy"
        51, 53, 55, 56, 57 -> "drizzling"
        61, 63, 65, 66, 67, 80, 81, 82 -> "raining"
        71, 73, 75, 77, 85, 86 -> "snowing"
        95, 96, 99 -> "thunderstorms"
        else -> "changing weather"
    }
}
