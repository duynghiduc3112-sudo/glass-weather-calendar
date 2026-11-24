package com.example.glassweather

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WeatherSyncWorker.schedule(this, intervalHours = 2L)
        enableEdgeToEdge()
        setContent {
            GlassWeatherApp()
        }
    }
}

private data class CityPreset(
    val name: String,
    val lat: Double,
    val lon: Double
)

private val PRESET_CITIES = listOf(
    CityPreset("Nghị Đức", 11.1580, 107.7680),
    CityPreset("TP. Hồ Chí Minh", 10.8231, 106.6297),
    CityPreset("Hà Nội", 21.0285, 105.8542),
    CityPreset("Đà Nẵng", 16.0544, 108.2022),
    CityPreset("Đà Lạt", 11.9404, 108.4583),
    CityPreset("Nha Trang", 12.2388, 109.1967),
    CityPreset("Cần Thơ", 10.0452, 105.7469)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlassWeatherApp() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var state by remember { mutableStateOf(WeatherPrefs.loadState(context)) }
    var latInput by remember { mutableStateOf(state.latitude.toString()) }
    var lonInput by remember { mutableStateOf(state.longitude.toString()) }
    var cityInput by remember { mutableStateOf(state.locationName) }
    var isRefreshing by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }

    val lunar = remember(state.updatedAt) { LunarCalendar.todayVietnam() }
    val (weekday, solarDate) = remember(state.updatedAt) { todaySolarFormatted() }
    val (weatherIcon, weatherDesc) = weatherDescription(state.weatherCode)

    fun refreshAndUpdateWidgets(customMessage: String? = null) {
        scope.launch {
            isRefreshing = true
            statusMessage = null
            val ok = WeatherRepository.refreshWeather(context)
            updateAllWeatherWidgets(context)
            state = WeatherPrefs.loadState(context)
            isRefreshing = false
            statusMessage = customMessage ?: if (ok) {
                "Đã cập nhật thời tiết & widget (${state.updatedAt})"
            } else {
                "Không lấy được dữ liệu mới, đang giữ dữ liệu đã lưu"
            }
        }
    }

    fun applyStateChange(transform: (WeatherState) -> WeatherState) {
        val next = transform(state)
        state = next
        scope.launch {
            updateAllWeatherWidgets(context)
        }
    }

    MaterialTheme {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = "Glass Weather & Lunar",
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Tự động làm mới mỗi 2 giờ",
                                fontSize = 12.sp,
                                color = Color(0xFF94A3B8)
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color(0xFF0B1020),
                        titleContentColor = Color.White
                    )
                )
            },
            containerColor = Color(0xFF070B14)
        ) { innerPadding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    Card(
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF11192E)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    Brush.linearGradient(
                                        listOf(
                                            state.accentColor.primaryDark.copy(alpha = 0.28f),
                                            Color(0xFF131D36)
                                        )
                                    )
                                )
                                .padding(18.dp)
                        ) {
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = "$weekday • $solarDate",
                                            color = Color.White,
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "${lunar.shortLabel()} • Ngày ${lunar.dayCanChi} •${lunar.yearCanChi}",
                                            color = state.accentColor.primaryDark,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }

                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = "$weatherIcon${state.temperatureC.toInt()}°C",
                                            color = Color.White,
                                            fontSize = 24.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text
