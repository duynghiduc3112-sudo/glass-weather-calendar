package com.example.glassweather

import androidx.compose.ui.graphics.Color
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

enum class WidgetStyle(val label: String) {
    TRANSPARENT("Trong suốt hoàn toàn"),
    LIQUID_LIGHT("Kính mờ sáng"),
    LIQUID_DARK("Kính mờ tối")
}

enum class AppThemeMode(val label: String) {
    SYSTEM("Theo hệ thống"),
    LIGHT("Nền sáng"),
    DARK("Nền tối")
}

enum class AppAccentColor(
    val label: String,
    val primaryLight: Color,
    val primaryDark: Color
) {
    OCEAN("Xanh đại dương", Color(0xFF0284C7), Color(0xFF38BDF8)),
    EMERALD("Xanh ngọc", Color(0xFF059669), Color(0xFF34D399)),
    SUNSET("Cam hoàng hôn", Color(0xFFEA580C), Color(0xFFFB923C)),
    IRIS("Tím oải hương", Color(0xFF7C3AED), Color(0xFFA78BFA)),
    CORAL("Hồng san hô", Color(0xFFE11D48), Color(0xFFFB7185)),
    AMBER("Vàng hổ phách", Color(0xFFD97706), Color(0xFFFBBF24)),
    CRIMSON("Đỏ ruby", Color(0xFFDC2626), Color(0xFFF87171)),
    MINT("Xanh bạc hà", Color(0xFF0D9488), Color(0xFF2DD4BF)),
    SLATE("Than chì tối giản", Color(0xFF475569), Color(0xFFCBD5E1))
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
    val themeMode: AppThemeMode = AppThemeMode.SYSTEM,
    val accentColor: AppAccentColor = AppAccentColor.OCEAN,
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
