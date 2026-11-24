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

    val palette = when (state.style) {
        WidgetStyle.TRANSPARENT -> GlassPalette(
            outerBorder = Color(0x00000000),
            innerGlass = Color(0x00000000),
            pillBg = Color(0x2E000000),
            textPrimary = Color.White,
            textSecondary = Color(0xE6FFFFFF),
            lunarAccent = Color(0xFFFFE082)
        )
        WidgetStyle.LIQUID_LIGHT -> GlassPalette(
            outerBorder = Color(0x73FFFFFF),
            innerGlass = Color(0x45FFFFFF),
            pillBg = Color(0x38FFFFFF),
            textPrimary = Color(0xFF111827),
            textSecondary = Color(0xCC1F2937),
            lunarAccent = Color(0xFF9A3412)
        )
        WidgetStyle.LIQUID_DARK -> GlassPalette(
            outerBorder = Color(0x59FFFFFF),
            innerGlass = Color(0x7D111827),
            pillBg = Color(0x29FFFFFF),
            textPrimary = Color.White,
            textSecondary = Color(0xCCF3F4F6),
            lunarAccent = Color(0xFFFCD34D)
        )
    }

    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .cornerRadius(28.dp)
            .background(ColorProvider(palette.outerBorder))
            .padding(2.dp)
            .clickable(actionRunCallback<RefreshWeatherAction>())
    ) {
        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .cornerRadius(26.dp)
                .background(ColorProvider(palette.innerGlass))
                .padding(14.dp)
        ) {
            Column(
                modifier = GlanceModifier.fillMaxSize(),
                verticalAlignment = Alignment.SpaceBetween
            ) {
                Column(modifier = GlanceModifier.fillMaxWidth()) {
                    Row(
                        modifier = GlanceModifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "$weekday • $solarDate",
                            style = TextStyle(
                                color = ColorProvider(palette.textPrimary),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Text(
                            text = "↻ ${state.updatedAt}",
                            style = TextStyle(
                                color = ColorProvider(palette.textSecondary),
                                fontSize = 11.sp
                            )
                        )
                    }

                    Spacer(modifier = GlanceModifier.height(2.dp))

                    Text(
                        text = "${lunar.shortLabel()} • Ngày ${lunar.dayCanChi} • ${lunar.yearCanChi}",
                        style = TextStyle(
                            color = ColorProvider(palette.lunarAccent),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    )
                }

                Row(
                    modifier = GlanceModifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.SpaceBetween,
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
                                color = ColorProvider(palette.textPrimary),
                                fontSize = 26.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = state.locationName,
                            style = TextStyle(
                                color = ColorProvider(palette.textPrimary),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Text(
                            text = weatherDesc,
                            style = TextStyle(
                                color = ColorProvider(palette.textSecondary),
                                fontSize = 11.sp
                            )
                        )
                    }
                }

                if (state.hourly.isNotEmpty()) {
                    Row(
                        modifier = GlanceModifier
                            .fillMaxWidth()
                            .cornerRadius(16.dp)
                            .background(ColorProvider(palette.pillBg))
                            .padding(vertical = 6.dp, horizontal = 8.dp),
                        horizontalAlignment = Alignment.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        state.hourly.forEach { item ->
                            val (hIcon, _) = weatherDescription(item.code)
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = item.hour,
                                    style = TextStyle(
                                        color = ColorProvider(palette.textSecondary),
                                        fontSize = 10.sp
                                    )
                                )
                                Text(
                                    text = hIcon,
                                    style = TextStyle(fontSize = 13.sp)
                                )
                                Text(
                                    text = "${item.tempC}°",
                                    style = TextStyle(
                                        color = ColorProvider(palette.textPrimary),
                                        fontSize = 11.sp,
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

private data class GlassPalette(
    val outerBorder: Color,
    val innerGlass: Color,
    val pillBg: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val lunarAccent: Color
)

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
