package com.example.glassweather

import android.Manifest
import android.annotation.SuppressLint
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    WeatherConfigScreen()
                }
            }
        }
    }
}

@Composable
fun WeatherConfigScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val initial = remember { WeatherPrefs.loadState(context) }

    var locationName by remember { mutableStateOf(initial.locationName) }
    var lat by remember { mutableStateOf(initial.latitude.toString()) }
    var lon by remember { mutableStateOf(initial.longitude.toString()) }
    var selectedStyle by remember { mutableStateOf(initial.style) }
    var statusText by remember { mutableStateOf("Sẵn sàng cập nhật widget") }

    val fetchGpsAndRefresh: () -> Unit = {
        scope.launch {
            statusText = "Đang xác định vị trí GPS..."
            val loc = getBestLastKnownLocation(context)
            if (loc != null) {
                lat = String.format(Locale.US, "%.4f", loc.latitude)
                lon = String.format(Locale.US, "%.4f", loc.longitude)
                val resolvedName = resolvePlaceName(context, loc.latitude, loc.longitude)
                if (!resolvedName.isNullOrBlank()) {
                    locationName = resolvedName
                }
                WeatherPrefs.saveConfig(context, locationName, loc.latitude, loc.longitude, selectedStyle)
                val updated = WeatherRepository.refreshWeather(context)
                updateAllWeatherWidgets(context)
                statusText = "GPS: $locationName • ${updated.temperatureC.toInt()}°C (${updated.updatedAt})"
            } else {
                statusText = "Không lấy được vị trí. Hãy bật GPS trên điện thoại rồi thử lại."
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            fetchGpsAndRefresh()
        } else {
            statusText = "Bạn chưa cấp quyền vị trí."
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Glass Weather & Lunar Widget", style = MaterialTheme.typography.headlineSmall)

        OutlinedButton(
            onClick = {
                val hasFine = ContextCompat.checkSelfPermission(
                    context, Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
                val hasCoarse = ContextCompat.checkSelfPermission(
                    context, Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED

                if (hasFine || hasCoarse) {
                    fetchGpsAndRefresh()
                } else {
                    permissionLauncher.launch(
                        arrayOf(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        )
                    )
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("📍 Lấy vị trí GPS hiện tại")
        }

        OutlinedTextField(
            value = locationName,
            onValueChange = { locationName = it },
            label = { Text("Tên địa điểm") },
            modifier = Modifier.fillMaxWidth()
        )

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedTextField(
                value = lat,
                onValueChange = { lat = it },
                label = { Text("Vĩ độ (Lat)") },
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = lon,
                onValueChange = { lon = it },
                label = { Text("Kinh độ (Lon)") },
                modifier = Modifier.weight(1f)
            )
        }

        Text("Kiểu hiển thị widget", style = MaterialTheme.typography.titleMedium)

        WidgetStyle.entries.forEach { style ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                RadioButton(
                    selected = selectedStyle == style,
                    onClick = { selectedStyle = style }
                )
                Text(style.label)
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Button(
            onClick = {
                val latVal = lat.toDoubleOrNull() ?: 10.8231
                val lonVal = lon.toDoubleOrNull() ?: 106.6297
                WeatherPrefs.saveConfig(context, locationName.ifBlank { "Vị trí" }, latVal, lonVal, selectedStyle)
                scope.launch {
                    statusText = "Đang lấy dữ liệu thời tiết..."
                    val updated = WeatherRepository.refreshWeather(context)
                    updateAllWeatherWidgets(context)
                    statusText = "Đã cập nhật: ${updated.temperatureC.toInt()}°C (${updated.updatedAt})"
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Lưu & Cập nhật Widget")
        }

        Button(
            onClick = {
                val manager = AppWidgetManager.getInstance(context)
                val provider = ComponentName(context, WeatherWidgetReceiver::class.java)
                if (manager.isRequestPinAppWidgetSupported) {
                    manager.requestPinAppWidget(provider, null, null)
                } else {
                    statusText = "Hãy giữ màn hình chính điện thoại để thêm widget"
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Ghim Widget ra màn hình chính")
        }

        Text(statusText, style = MaterialTheme.typography.bodyMedium)
    }
}

@SuppressLint("MissingPermission")
private fun getBestLastKnownLocation(context: Context): Location? {
    val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    val providers = lm.getProviders(true)
    var best: Location? = null
    for (p in providers) {
        val l = lm.getLastKnownLocation(p) ?: continue
        if (best == null || l.accuracy < best.accuracy) {
            best = l
        }
    }
    return best
}

private suspend fun resolvePlaceName(context: Context, lat: Double, lon: Double): String? =
    withContext(Dispatchers.IO) {
        runCatching {
            val geocoder = Geocoder(context, Locale("vi", "VN"))
            @Suppress("DEPRECATION")
            val list = geocoder.getFromLocation(lat, lon, 1)
            val addr = list?.firstOrNull()
            addr?.subAdminArea ?: addr?.locality ?: addr?.adminArea
        }.getOrNull()
    }
