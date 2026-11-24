package com.example.glassweather

import android.content.Context

object WeatherPrefs {
    private const val PREF_NAME = "glass_weather_prefs"

    fun saveConfig(
        context: Context,
        locationName: String,
        lat: Double,
        lon: Double,
        style: WidgetStyle,
        themeMode: AppThemeMode,
        accentColor: AppAccentColor
    ) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).edit()
            .putString("loc_name", locationName)
            .putString("lat", lat.toString())
            .putString("lon", lon.toString())
            .putString("style", style.name)
            .putString("theme_mode", themeMode.name)
            .putString("accent_color", accentColor.name)
            .apply()
    }

    fun saveWeather(
        context: Context,
        tempC: Double,
        code: Int,
        updatedAt: String,
        hourly: List<HourlyItem>
    ) {
        val serializedHourly = hourly.joinToString(";") { "${it.hour}|${it.tempC}|${it.code}" }

        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).edit()
            .putFloat("temp_c", tempC.toFloat())
            .putInt("weather_code", code)
            .putString("updated_at", updatedAt)
            .putString("hourly_data", serializedHourly)
            .apply()
    }

    fun loadState(context: Context): WeatherState {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

        val styleName = prefs.getString("style", WidgetStyle.LIQUID_LIGHT.name)
        val style = runCatching { WidgetStyle.valueOf(styleName!!) }
            .getOrDefault(WidgetStyle.LIQUID_LIGHT)

        val themeName = prefs.getString("theme_mode", AppThemeMode.SYSTEM.name)
        val themeMode = runCatching { AppThemeMode.valueOf(themeName!!) }
            .getOrDefault(AppThemeMode.SYSTEM)

        val accentName = prefs.getString("accent_color", AppAccentColor.OCEAN.name)
        val accentColor = runCatching { AppAccentColor.valueOf(accentName!!) }
            .getOrDefault(AppAccentColor.OCEAN)

        val rawHourly = prefs.getString("hourly_data", "") ?: ""
        val hourlyList = if (rawHourly.isBlank()) {
            emptyList()
        } else {
            rawHourly.split(";").mapNotNull { entry ->
                val parts = entry.split("|")
                if (parts.size == 3) {
                    HourlyItem(
                        hour = parts[0],
                        tempC = parts[1].toIntOrNull() ?: 0,
                        code = parts[2].toIntOrNull() ?: 1
                    )
                } else null
            }
        }

        return WeatherState(
            locationName = prefs.getString("loc_name", "TP. Hồ Chí Minh") ?: "TP. Hồ Chí Minh",
            latitude = prefs.getString("lat", "10.8231")?.toDoubleOrNull() ?: 10.8231,
            longitude = prefs.getString("lon", "106.6297")?.toDoubleOrNull() ?: 106.6297,
            temperatureC = prefs.getFloat("temp_c", 30.0f).toDouble(),
            weatherCode = prefs.getInt("weather_code", 1),
            updatedAt = prefs.getString("updated_at", "Vừa xong") ?: "Vừa xong",
            style = style,
            themeMode = themeMode,
            accentColor = accentColor,
            hourly = hourlyList
        )
    }
}
