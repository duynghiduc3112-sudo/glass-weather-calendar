package com.example.glassweather

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

enum class WidgetStyle(val label: String) {
    TRANSPARENT("Trong suốt hoàn toàn"),
    LIQUID_LIGHT("Kính lỏng sáng"),
    LIQUID_DARK("Kính lỏng tối")
}

data class HourlyItem(
    val hour: String,
    val tempC: Int,
    val code: Int
)

data class WeatherState(
    val locationName: String = "TP. Hồ Chí Minh",
    val latitude: Double = 10.8231,
    val longitude: Double = 106.6297,
    val temperatureC: Double = 30.0,
    val weatherCode: Int = 1,
    val updatedAt: String = "--:--",
    val style: WidgetStyle = WidgetStyle.LIQUID_LIGHT,
    val hourly: List<HourlyItem> = emptyList()
)

fun todaySolarFormatted(): Pair<String, String> {
    val today = LocalDate.now()
    val weekdayVi = when (today.dayOfWeek.value) {
        1 -> "Thứ Hai"
        2 -> "Thứ Ba"
        3 -> "Thứ Tư"
        4 -> "Thứ Năm"
        5 -> "Thứ Sáu"
        6 -> "Thứ Bảy"
        7 -> "Chủ Nhật"
        else -> ""
    }
    val dateStr = today.format(DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale("vi", "VN")))
    return weekdayVi to dateStr
}

fun weatherDescription(code: Int): Pair<String, String> = when (code) {
    0 -> "☀️" to "Trời quang"
    1, 2 -> "⛅" to "Ít mây"
    3 -> "☁️" to "Nhiều mây"
    45, 48 -> "🌫️" to "Sương mù"
    in 51..67 -> "🌦️" to "Có mưa"
    in 71..77 -> "❄️" to "Có tuyết"
    in 80..82 -> "🌧️" to "Mưa rào"
    in 95..99 -> "⛈️" to "Dông bão"
    else -> "🌡️" to "Đang cập nhật"
}
