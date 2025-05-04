package com.example.dephem

//import java.time.LocalDateTime
data class DateTime(val year: Int, val month: Int, val dayOfMonth: Int, val hour: Int, val minute: Int, val second: Int){
    fun dateTimeToJED(): Double {
        val y: Int = if (month < 3) (year - 1) else year;
        val m: Int = if (month < 3) (month + 12) else month;
        val a: Int = y / 100;
        val b: Int = 2 - a + (a / 4);

        val jdn: Int = (365.25 * y).toInt() +
                (30.6001 * (m + 1)).toInt() +
                dayOfMonth + 1720994 + b;
        val jd: Double = jdn + 0.5 + hour.toDouble() / 24.0 +
                minute.toDouble() / 1440.0 +
                second.toDouble() / 86400.0;
        return jd;
    }
}

fun jedToDateTime(jed: Double): DateTime {
    val x = jed + 0.5
    val z: Int = x.toInt()
    val f: Double = x - z
    val q: Int = ((z - 1867216.25) / 36524.25).toInt()
    val a: Int = z + 1 + q - (q / 4)
    val b: Int = a + 1524
    val c: Int = ((b - 122.1) / 365.25).toInt()
    val d: Int = (365.25 * c).toInt()
    val e: Int = ((b - d) / 30.6001).toInt()

    val dayOfMonth = b - d - (30.6001 * e).toInt() + f.toInt()
    val month = if ((e < 13.5)) (e - 1) else (e - 13)
    val year = if ((month < 2.5)) (c - 4715) else (c - 4716)
    val jdn: Int = (jed).toInt()
    val jed_hours: Double = if ((jed - jdn >= 0.5)) (jed - 0.5 - jdn) * 24 else (jed + 0.5 - jdn) * 24
    var hour = (jed_hours).toInt()
    val jed_minutes: Double = (jed_hours - hour) * 60
    var minute = (jed_minutes).toInt()
    val jed_seconds: Double = (jed_minutes - minute) * 60
    var second = (jed_seconds).toInt()
    if (jed_seconds - second >= 0.5) {
        if (second < 59) {
            second += 1
        } else {
            second = 0
            if (minute < 59) {
                minute += 1
            } else {
                minute = 0
                if (hour < 23) {
                    hour += 1
                } else {
                    hour = 0
                }
            }
        }
    }
    val date = DateTime(year, month, dayOfMonth, hour, minute, second)
    return date
}

fun testJEDconverter(datetime: DateTime): Boolean {
    val jed: Double = datetime.dateTimeToJED()
    val datetimeTest: DateTime = jedToDateTime(jed)
    println("Тестовая дата: $datetimeTest")
    println(" (JD $jed)")
    if (datetimeTest.year == datetime.year && datetimeTest.month == datetime.month &&
        datetimeTest.dayOfMonth == datetime.dayOfMonth && datetimeTest.hour == datetime.hour &&
        datetimeTest.minute == datetime.minute && datetimeTest.second == datetime.second) {
        return true
    }
    return false
}
