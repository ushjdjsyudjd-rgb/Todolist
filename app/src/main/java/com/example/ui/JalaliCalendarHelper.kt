package com.example.ui

import java.util.Calendar
import java.util.Date

object JalaliCalendarHelper {
    fun getJalaliDate(timestamp: Long): String {
        val calendar = Calendar.getInstance().apply { timeInMillis = timestamp }
        val gYear = calendar.get(Calendar.YEAR)
        val gMonth = calendar.get(Calendar.MONTH) + 1
        val gDay = calendar.get(Calendar.DAY_OF_MONTH)
        
        val jalali = gregorianToJalali(gYear, gMonth, gDay)
        return String.format("%04d/%02d/%02d", jalali[0], jalali[1], jalali[2])
    }

    private fun gregorianToJalali(gy: Int, gm: Int, gd: Int): IntArray {
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
}
