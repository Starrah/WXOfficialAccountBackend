@file:Suppress("DEPRECATION")

package utils

import java.text.SimpleDateFormat
import java.util.*

fun Date.sameDay(other: Date): Boolean{
    return this.year == other.year && this.month == other.month && this.day == other.day
}

val Date.year4bit: Int
    get() = if (this.year < 70) 2000 + this.year else 1900 + this.year

val YMDFormat = SimpleDateFormat("yyyyMMdd")

fun String.assertBlank(): String?{
    return if (this != "") this else null
}

