package com.example.glassweather

import java.time.LocalDate
import java.time.ZoneOffset
import kotlin.math.floor

data class VietnameseLunarDate(
    val day: Int,
    val month: Int,
    val year: Int,
    val isLeapMonth: Boolean,
    val dayCanChi: String,
    val yearCanChi: String
) {
    fun shortLabel(): String {
        val leapTag = if (isLeapMonth) " (nhuận)" else ""
        return "Âm: $day/$month$leapTag"
    }
}

object LunarCalendar {
    private val CAN = arrayOf("Giáp", "Ất", "Bính", "Đinh", "Mậu", "Kỷ", "Canh", "Tân", "Nhâm", "Quý")
    private val CHI = arrayOf("Tý", "Sửu", "Dần", "Mão", "Thìn", "Tỵ", "Ngọ", "Mùi", "Thân", "Dậu", "Tuất", "Hợi")

    private fun jdFromDate(dd: Int, mm: Int, yy: Int): Long {
        val a = (14 - mm) / 12
        val y = yy + 4800 - a
        val m = mm + 12 * a - 3
        var jd = dd.toLong() + (153 * m + 2) / 5 + 365 * y + y / 4 - y / 100 + y / 400 - 32045
        if (jd < 2299161L) {
            jd = dd.toLong() + (153 * m + 2) / 5 + 365 * y + y / 4 - 32083
        }
        return jd
    }

    private fun getNewMoonDay(k: Long, timeZone: Double): Long {
        val t = k / 1236.85
        val t2 = t * t
        val t3 = t2 * t
        val dr = Math.PI / 180.0
        var jd1 = 2415020.75933 + 29.53058868 * k + 0.0001178 * t2 - 0.000000155 * t3
        jd1 += 0.00033 * kotlin.math.sin((166.56 + 132.87 * t - 0.009173 * t2) * dr)
        val m = 359.2242 + 29.10535608 * k - 0.0000333 * t2 - 0.00000347 * t3
        val mpr = 306.0253 + 385.81691806 * k + 0.0107306 * t2 + 0.00001236 * t3
        val f = 21.2964 + 390.67050646 * k - 0.0016528 * t2 - 0.00000239 * t3
        var c1 = (0.1734 - 0.000393 * t) * kotlin.math.sin(m * dr) + 0.0021 * kotlin.math.sin(2 * m * dr)
        c1 -= 0.4068 * kotlin.math.sin(mpr * dr) + 0.0161 * kotlin.math.sin(2 * mpr * dr)
        c1 -= 0.0004 * kotlin.math.sin(3 * mpr * dr)
        c1 += 0.0104 * kotlin.math.sin(2 * f * dr) - 0.0051 * kotlin.math.sin((m + mpr) * dr)
        c1 -= 0.0074 * kotlin.math.sin((m - mpr) * dr) + 0.0004 * kotlin.math.sin((2 * f + m) * dr)
        c1 -= 0.0004 * kotlin.math.sin((2 * f - m) * dr) - 0.0006 * kotlin.math.sin((2 * f + mpr) * dr)
        c1 += 0.0010 * kotlin.math.sin((2 * f - mpr) * dr) + 0.0005 * kotlin.math.sin((mpr + 2 * m) * dr)
        val deltat = if (t < -11.0) {
            0.001 + 0.000839 * t + 0.0002261 * t2 - 0.00000845 * t3 - 0.000000081 * t3 * t
        } else {
            -0.000278 + 0.000265 * t + 0.000262 * t2
        }
        return floor(jd1 + c1 - deltat + 0.5 + timeZone / 24.0).toLong()
    }

    private fun getSunLongitude(dayNumber: Long, timeZone: Double): Int {
        val t = (dayNumber - 0.5 - timeZone / 24.0 - 2451545.0) / 36525.0
        val t2 = t * t
        val dr = Math.PI / 180.0
        val m = 357.52910 + 35999.05030 * t - 0.0001559 * t2 - 0.00000048 * t * t2
        val l0 = 280.46645 + 36000.76983 * t + 0.0003032 * t2
        var dl = (1.914600 - 0.004817 * t - 0.000014 * t2) * kotlin.math.sin(dr * m)
        dl += (0.019993 - 0.000101 * t) * kotlin.math.sin(dr * 2 * m) + 0.000290 * kotlin.math.sin(dr * 3 * m)
        var l = (l0 + dl) * dr
        l -= Math.PI * 2 * floor(l / (Math.PI * 2))
        return floor(l / Math.PI * 6).toInt()
    }

    private fun getLunarMonth11(yy: Int, timeZone: Double): Long {
        val off = jdFromDate(31, 12, yy) - 2415021L
        val k = floor(off / 29.530588853).toLong()
        var nm = getNewMoonDay(k, timeZone)
        val sunLong = getSunLongitude(nm, timeZone)
        if (sunLong >= 9) {
            nm = getNewMoonDay(k - 1, timeZone)
        }
        return nm
    }

    private fun getLeapMonthOffset(a11: Long, timeZone: Double): Int {
        val k = floor((a11 - 2415021.076998695) / 29.530588853 + 0.5).toLong()
        var last = 0
        var i = 1
        var arc = getSunLongitude(getNewMoonDay(k + i, timeZone), timeZone)
        do {
            last = arc
            i++
            arc = getSunLongitude(getNewMoonDay(k + i, timeZone), timeZone)
        } while (arc != last && i < 14)
        return i - 1
    }

    fun fromLocalDate(date: LocalDate, timeZone: Double = 7.0): VietnameseLunarDate {
        val dayNumber = jdFromDate(date.dayOfMonth, date.monthValue, date.year)
        val k = floor((dayNumber - 2415021.076998695) / 29.530588853).toLong()
        var monthStart = getNewMoonDay(k + 1, timeZone)
        if (monthStart > dayNumber) {
            monthStart = getNewMoonDay(k, timeZone)
        }
        var a11 = getLunarMonth11(date.year, timeZone)
        var b11 = a11
        val lunarYear: Int
        if (a11 >= monthStart) {
            lunarYear = date.year
            a11 = getLunarMonth11(date.year - 1, timeZone)
        } else {
            lunarYear = date.year + 1
            b11 = getLunarMonth11(date.year + 1, timeZone)
        }
        val lunarDay = (dayNumber - monthStart + 1).toInt()
        val diff = floor((monthStart - a11) / 29.0).toInt()
        var lunarLeap = false
        var lunarMonth = diff + 11
        if (b11 - a11 > 365) {
            val leapMonthDiff = getLeapMonthOffset(a11, timeZone)
            if (diff >= leapMonthDiff) {
                lunarMonth = diff + 10
                if (diff == leapMonthDiff) {
                    lunarLeap = true
                }
            }
        }
        if (lunarMonth > 12) {
            lunarMonth -= 12
        }

        val dayCan = CAN[((dayNumber + 9) % 10).toInt()]
        val dayChi = CHI[((dayNumber + 1) % 12).toInt()]
        val yearCan = CAN[(lunarYear + 6) % 10]
        val yearChi = CHI[(lunarYear + 8) % 12]

        return VietnameseLunarDate(
            day = lunarDay,
            month = lunarMonth,
            year = lunarYear,
            isLeapMonth = lunarLeap,
            dayCanChi = "$dayCan $dayChi",
            yearCanChi = "$yearCan $yearChi"
        )
    }

    fun todayVietnam(): VietnameseLunarDate {
        val nowVn = LocalDate.now(ZoneOffset.ofHours(7))
        return fromLocalDate(nowVn, 7.0)
    }
}
