package com.example.glassweather

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.ActionParameters
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
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
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

private const val PERSONAL_SIGNATURE = "— Gió"

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
    val context = LocalContext.current
    val size = LocalSize.current
    val isCompact4x2 = size.height < 165.dp

    val (weekday, solarDate) = todaySolarFormatted()
    val (weatherIcon, weatherDesc) = weatherDescription(state.weatherCode)

    val currentInt = state.temperatureC.roundToInt()
    val temps = state.hourly.map { it.tempC } + currentInt
    val maxTemp = temps.maxOrNull() ?: (currentInt + 3)
    val minTemp = temps.minOrNull() ?: (currentInt - 3)

    val estimatedHumidity = when (state.weatherCode) {
        in 51..99 -> 86
        45, 48 -> 90
        3 -> 78
        1, 2 -> 68
        else -> 58
    }
    val estimatedWind = when (state.weatherCode) {
        in 80..99 -> 24
        in 51..67 -> 18
        3 -> 14
        else -> 10
    }

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
        WidgetStyle.TRANSPARENT -> Color(0x1F000000)
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

    val openAppIntent = Intent(context, MainActivity::class.java)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)

    val outerPadding = if (isCompact4x2) 10.dp else 14.dp
    val sectionGap = if (isCompact4x2) 6.dp else 10.dp
    val tempSize = if (isCompact4x2) 25.sp else 28.sp
    val iconSize = if (isCompact4x2) 27.sp else 30.sp
    val hourlyItems = if (isCompact4x2) state.hourly.take(4) else state.hourly.take(5)

    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .cornerRadius(26.dp)
            .background(ColorProvider(borderTint))
            .padding(1.5.dp)
            .clickable(actionStartActivity(openAppIntent))
    ) {
        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .cornerRadius(24.dp)
                .background(ColorProvider(glassBg))
                .padding(outerPadding)
        ) {
            Column(
                modifier = GlanceModifier.fillMaxSize(),
                verticalAlignment = Alignment.Top,
                horizontalAlignment = Alignment.Start
            ) {
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
                        modifier = GlanceModifier
                            .cornerRadius(10.dp)
                            .background(ColorProvider(pillGlass))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                            .clickable(actionRunCallback<RefreshWeatherAction>()),
                        style = TextStyle(
                            color = ColorProvider(textSecondary),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    )
                }

                Spacer(modifier = GlanceModifier.height(2.dp))

                Text(
                    text = "${lunar.shortLabel()} • Ngày ${lunar.dayCanChi} • ${lunar.yearCanChi}",
                    style = TextStyle(
                        color = ColorProvider(lunarColor),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                )

                Spacer(modifier = GlanceModifier.height(sectionGap))

                Row(
                    modifier = GlanceModifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = weatherIcon,
                            style = TextStyle(fontSize = iconSize)
                        )
                        Spacer(modifier = GlanceModifier.width(8.dp))
                        Column {
                            Text(
                                text = "$currentInt°C",
                                style = TextStyle(
                                    color = ColorProvider(textPrimary),
                                    fontSize = tempSize,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                            Text(
                                text = "▲ ${max(maxTemp, currentInt)}°  ▼ ${min(minTemp, currentInt)}°",
                                style = TextStyle(
                                    color = ColorProvider(textSecondary),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            )
                        }
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
                                fontSize = 11.sp
                            )
                        )
                        Spacer(modifier = GlanceModifier.height(2.dp))
                        Text(
                            text = "💧 $estimatedHumidity% • 💨 ${estimatedWind}km/h",
                            style = TextStyle(
                                color = ColorProvider(lunarColor),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium
                            )
                        )
                    }
                }

                if (hourlyItems.isNotEmpty()) {
                    Spacer(modifier = GlanceModifier.height(sectionGap))

                    Row(
                        modifier = GlanceModifier
                            .fillMaxWidth()
                            .cornerRadius(14.dp)
                            .background(ColorProvider(pillGlass))
                            .padding(
                                vertical = if (isCompact4x2) 4.dp else 6.dp,
                                horizontal = 8.dp
                            ),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        hourlyItems.forEachIndexed { index, item ->
                            if (index > 0) {
                                Spacer(modifier = GlanceModifier.defaultWeight())
                            }
                            val (hIcon, _) = weatherDescription(item.code)
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = item.hour,
                                    style = TextStyle(
                                        color = ColorProvider(textSecondary),
                                        fontSize = 10.sp
                                    )
                                )
                                Spacer(modifier = GlanceModifier.width(3.dp))
                                Text(
                                    text = hIcon,
                                    style = TextStyle(fontSize = 12.sp)
                                )
                                Spacer(modifier = GlanceModifier.width(2.dp))
                                Text(
                                    text = "${item.tempC}°",
                                    style = TextStyle(
                                        color = ColorProvider(textPrimary),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = GlanceModifier.defaultWeight())

                Row(
                    modifier = GlanceModifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Spacer(modifier = GlanceModifier.defaultWeight())
                    Text(
                        text = PERSONAL_SIGNATURE,
                        style = TextStyle(
                            color = ColorProvider(textSecondary),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium
                        )
                    )
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
