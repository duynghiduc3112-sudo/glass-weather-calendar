package com.example.glassweather

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object WeatherRepository {
    suspend fun refreshWeather(context: Context): WeatherState = withContext(Dispatchers.IO) {
        val current = WeatherPrefs.loadState(context)
        try {
            val urlStr =
                "https://api.open-meteo.com/v1/forecast?latitude=${current.latitude}&longitude=${current.longitude}" +
                        "&current=temperature_2m,weather_code" +
                        "&hourly=temperature_2m,weather_code" +
                        "&forecast_days=2&timezone=auto"

            val conn = (URL(urlStr).openConnection() as HttpURLConnection).apply {
                connectTimeout = 8000
                readTimeout = 8000
            }
            val body = conn.inputStream.bufferedReader().use { it.readText() }
            conn.disconnect()

            val root = JSONObject(body)
            val currentJson = root.getJSONObject("current")
            val temp = currentJson.getDouble("temperature_2m")
            val code = currentJson.getInt("weather_code")
            val currentIsoTime = currentJson.getString("time")

            val hourlyJson = root.getJSONObject("hourly")
            val times = hourlyJson.getJSONArray("time")
            val temps = hourlyJson.getJSONArray("temperature_2m")
            val codes = hourlyJson.getJSONArray("weather_code")

            val nextHours = mutableListOf<HourlyItem>()
            val currentHourPrefix = currentIsoTime.substringBefore(":")

            var startIndex = 0
            for (i in 0 until times.length()) {
                if (times.getString(i).startsWith(currentHourPrefix)) {
                    startIndex = i + 1
                    break
                }
            }

            for (i in startIndex until minOf(startIndex + 4, times.length())) {
                val rawTime = times.getString(i)
                val hourLabel = rawTime.substringAfter("T")
                val hTemp = temps.getDouble(i).toInt()
                val hCode = codes.getInt(i)
                nextHours.add(HourlyItem(hourLabel, hTemp, hCode))
            }

            val timeStr = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
            WeatherPrefs.saveWeather(context, temp, code, timeStr, nextHours)
        } catch (_: Exception) {
            // Giữ lại dữ liệu cũ nếu mất mạng
        }
        WeatherPrefs.loadState(context)
    }
}
