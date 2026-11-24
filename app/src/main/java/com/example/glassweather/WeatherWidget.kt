package com.example.glassweather

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.ActionParameters
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider

class RefreshWeatherAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        WeatherRepository.refreshWeather(context)
        WeatherGlanceWidget().update(context, glanceId)
    }
}

class WeatherGlanceWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val state = WeatherPrefs.loadState(context)
        val lunar = LunarCalendar.todayVietnam()
        provideContent {
            CalendarWeatherWidgetContent(state, lunar)
        }
    }
}

@Composable
private fun CalendarWeatherWidgetContent(
    state: WeatherState,
    lunar: VietnameseLunarDate
) {
    val (weekday, solarDate) = todaySolarFormatted()
    val (weatherIcon, weatherDesc) = weatherDescription(state.weatherCode)

    // Dùng kính mờ nhẹ và viền sáng mảnh để giữ độ trong của hình nền, không phủ mảng màu đặc
    val glassBg = when (state.style) {
        WidgetStyle.TRANSPARENT -> Color(0x14000000)
        WidgetStyle.LIQUID_LIGHT -> Color(0x2EFFFFFF)
        WidgetStyle.LIQUID_DARK -> Color(0x4D090D16)
    }
    val borderTint = when (state.style) {
        WidgetStyle.TRANSPARENT -> Color(0x26FFFFFF)
        WidgetStyle.LIQUID_LIGHT -> Color(0x66FFFFFF)
        WidgetStyle.LIQUID_DARK -> Color(0x40FFFFFF)
    }
    val pillGlass = when (state.style) {
        WidgetStyle.TRANSPARENT -> Color(0x1C000000)
        WidgetStyle.LIQUID_LIGHT -> Color(0x38FFFFFF)
        WidgetStyle.LIQUID_DARK -> Color(0x29FFFFFF)
    }
    val textPrimary = when (state.style) {
        WidgetStyle.LIQUID_LIGHT -> Color(0xFF111827)
        WidgetStyle.TRANSPARENT, WidgetStyle.LIQUID_DARK -> Color.White
    }
    val textSecondary = when (state.style) {
        WidgetStyle.LIQUID_LIGHT -> Color(0xDD1F2937)
        WidgetStyle.TRANSPARENT, WidgetStyle.LIQUID_DARK -> Color(0xE6FFFFFF)
    }
    val lunarColor = when (state.style) {
        WidgetStyle.LIQUID_LIGHT -> state.accentColor.primaryLight
        WidgetStyle.TRANSPARENT, WidgetStyle.LIQUID_DARK -> state.accentColor.primaryDark
    }

    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .cornerRadius(26.dp)
            .background(ColorProvider(borderTint))
            .padding(1.5.dp)
            .clickable(actionRunCallback<RefreshWeatherAction>())
    ) {
        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .cornerRadius(24.5.dp)
                .background(ColorProvider(glassBg))
                .padding(14.dp)
        ) {
            Column(
                modifier = GlanceModifier.fillMaxSize(),
                verticalAlignment = Alignment.Top,
                horizontalAlignment = Alignment.Start
            ) {
                Column(modifier = GlanceModifier.fillMaxWidth()) {
                    Row(
                        modifier = GlanceModifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "$weekday • $solarDate",
                            style = TextStyle(
                                color = ColorProvider(textPrimary),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Spacer(modifier = GlanceModifier.defaultWeight())
                        Text(
                            text = "↻ ${state.updatedAt}",
                            style = TextStyle(
                                color = ColorProvider(textSecondary),
                                fontSize = 12.sp
                            )
                        )
                    }

                    Spacer(modifier = GlanceModifier.height(4.dp))

                    Text(
                        text = "${lunar.shortLabel()} • Ngày ${lunar.dayCanChi} • ${lunar.yearCanChi}",
                        style = TextStyle(
                            color = ColorProvider(lunarColor),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }

                Spacer(modifier = GlanceModifier.defaultWeight())

                Row(
                    modifier = GlanceModifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = weatherIcon,
                            style = TextStyle(fontSize = 28.sp)
                        )
                        Spacer(modifier = GlanceModifier.width(8.dp))
                        Text(
                            text = "${state.temperatureC.toInt()}°C",
                            style = TextStyle(
                                color = ColorProvider(textPrimary),
                                fontSize = 26.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }

                    Spacer(modifier = GlanceModifier.defaultWeight())

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = state.locationName,
                            style = TextStyle(
                                color = ColorProvider(textPrimary),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Text(
                            text = weatherDesc,
                            style = TextStyle(
                                color = ColorProvider(textSecondary),
                                fontSize = 12.sp
                            )
                        )
                    }
                }

                if (state.hourly.isNotEmpty()) {
                    Spacer(modifier = GlanceModifier.height(10.dp))
                    Row(
                        modifier = GlanceModifier
                            .fillMaxWidth()
                            .cornerRadius(16.dp)
                            .background(ColorProvider(pillGlass))
                            .padding(vertical = 6.dp, horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        state.hourly.forEachIndexed { index, item ->
                            if (index > 0) {
                                Spacer(modifier = GlanceModifier.defaultWeight())
                            }
                            val (hIcon, _) = weatherDescription(item.code)
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = item.hour,
                                    style = TextStyle(
                                        color = ColorProvider(textSecondary),
                                        fontSize = 11.sp
                                    )
                                )
                                Text(
                                    text = hIcon,
                                    style = TextStyle(fontSize = 14.sp)
                                )
                                Text(
                                    text = "${item.tempC}°",
                                    style = TextStyle(
                                        color = ColorProvider(textPrimary),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

class WeatherWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = WeatherGlanceWidget()
}

suspend fun updateAllWeatherWidgets(context: Context) {
    val manager = GlanceAppWidgetManager(context)
    val ids = manager.getGlanceIds(WeatherGlanceWidget::class.java)
    ids.forEach { id ->
        WeatherGlanceWidget().update(context, id)
    }
}
