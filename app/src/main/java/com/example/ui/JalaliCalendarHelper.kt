package com.example.ui

import java.util.Calendar

data class JalaliDateTime(
    val year: Int,
    val month: Int,
    val day: Int,
    val hour: Int = 0,
    val minute: Int = 0
)

object JalaliCalendarHelper {
    val JALALI_MONTH_NAMES = arrayOf(
        "فروردین", "اردیبهشت", "خرداد",
        "تیر", "مرداد", "شهریور",
        "مهر", "آبان", "آذر",
        "دی", "بهمن", "اسفند"
    )

    val JALALI_WEEK_DAYS = arrayOf("ش", "ی", "د", "س", "چ", "پ", "ج")

    fun getJalaliDate(timestamp: Long): String {
        val dt = millisToJalali(timestamp)
        return String.format("%04d/%02d/%02d", dt.year, dt.month, dt.day)
    }

    fun getJalaliDateTime(timestamp: Long): String {
        val dt = millisToJalali(timestamp)
        return String.format("%04d/%02d/%02d - %02d:%02d", dt.year, dt.month, dt.day, dt.hour, dt.minute)
    }

    fun millisToJalali(timestamp: Long): JalaliDateTime {
        val calendar = Calendar.getInstance().apply { timeInMillis = timestamp }
        val gYear = calendar.get(Calendar.YEAR)
        val gMonth = calendar.get(Calendar.MONTH) + 1
        val gDay = calendar.get(Calendar.DAY_OF_MONTH)
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)

        val jalali = gregorianToJalali(gYear, gMonth, gDay)
        return JalaliDateTime(
            year = jalali[0],
            month = jalali[1],
            day = jalali[2],
            hour = hour,
            minute = minute
        )
    }

    fun jalaliToMillis(year: Int, month: Int, day: Int, hour: Int = 0, minute: Int = 0): Long {
        val (gy, gm, gd) = jalaliToGregorian(year, month, day)
        val calendar = Calendar.getInstance().apply {
            set(Calendar.YEAR, gy)
            set(Calendar.MONTH, gm - 1)
            set(Calendar.DAY_OF_MONTH, gd)
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return calendar.timeInMillis
    }

    fun gregorianToJalali(gy: Int, gm: Int, gd: Int): IntArray {
        val gDaysInMonth = intArrayOf(0, 31, 59, 90, 120, 151, 181, 212, 243, 273, 304, 335)
        var gDayNo = 365 * (gy - 1600) + (gy - 1597) / 4 - (gy - 1501) / 100 + (gy - 1501) / 400 + gd + gDaysInMonth[gm - 1]
        if (gm > 2 && ((gy % 4 == 0 && gy % 100 != 0) || (gy % 400 == 0))) {
            gDayNo++
        }
        var jDayNo = gDayNo - 79
        val jNp = jDayNo / 12053
        jDayNo %= 12053
        var jy = 979 + 33 * jNp + 4 * (jDayNo / 1461)
        jDayNo %= 1461
        if (jDayNo >= 366) {
            jy += (jDayNo - 1) / 365
            jDayNo = (jDayNo - 1) % 365
        }
        var jm = 0
        for (i in 0..11) {
            val daysInJMonth = if (i < 6) 31 else 30
            if (jDayNo < daysInJMonth) {
                jm = i + 1
                break
            }
            jDayNo -= daysInJMonth
        }
        val jd = jDayNo + 1
        return intArrayOf(jy, jm, jd)
    }

    fun jalaliToGregorian(jy: Int, jm: Int, jd: Int): IntArray {
        val jy1 = jy - 979
        val jm1 = jm - 1
        val jd1 = jd - 1

        var jDayNo = 365 * jy1 + (jy1 / 33) * 8 + ((jy1 % 33) + 3) / 4
        for (i in 0 until jm1) {
            jDayNo += if (i < 6) 31 else 30
        }
        jDayNo += jd1

        var gDayNo = jDayNo + 79

        var gy = 1600 + 400 * (gDayNo / 146097)
        gDayNo %= 146097

        var leap = true
        if (gDayNo >= 36525) {
            gDayNo--
            gy += 100 * (gDayNo / 36524)
            gDayNo %= 36524

            if (gDayNo >= 365) {
                gDayNo++
            } else {
                leap = false
            }
        }

        gy += 4 * (gDayNo / 1461)
        gDayNo %= 1461

        if (gDayNo >= 366) {
            leap = false
            gDayNo--
            gy += gDayNo / 365
            gDayNo %= 365
        }

        val gDaysInMonth = intArrayOf(0, 31, if (leap) 29 else 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31)
        var gm = 0
        while (gDayNo >= gDaysInMonth[gm + 1]) {
            gDayNo -= gDaysInMonth[gm + 1]
            gm++
        }
        val gd = gDayNo + 1
        return intArrayOf(gy, gm + 1, gd)
    }

    fun getDaysInJalaliMonth(jy: Int, jm: Int): Int {
        if (jm in 1..6) return 31
        if (jm in 7..11) return 30
        val (gy, gm, gd) = jalaliToGregorian(jy, 12, 30)
        val check = gregorianToJalali(gy, gm, gd)
        return if (check[0] == jy && check[1] == 12 && check[2] == 30) 30 else 29
    }

    fun getFirstDayOfWeekForJalaliMonth(jy: Int, jm: Int): Int {
        val (gy, gm, gd) = jalaliToGregorian(jy, jm, 1)
        val cal = Calendar.getInstance().apply {
            set(gy, gm - 1, gd)
        }
        val dow = cal.get(Calendar.DAY_OF_WEEK) // 1=Sun, 2=Mon, ..., 7=Sat
        return dow % 7 // 7 -> 0 (Sat), 1 -> 1 (Sun), 2 -> 2 (Mon), ..., 6 -> 6 (Fri)
    }
}
