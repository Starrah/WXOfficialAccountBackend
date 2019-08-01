@file:Suppress("DEPRECATION")

package utils

import com.alibaba.fastjson.JSON
import org.bson.Document
import users.User
import java.text.SimpleDateFormat
import java.util.*

/**
 * 比较两个Date对象是否在日的精度下表示相同的日期。
 * @param other
 */
fun Date.sameDay(other: Date): Boolean{
    return this.year == other.year && this.month == other.month && this.day == other.day
}

/**
 * 日期对应的四位年份（如2019）
 */
val Date.year4bit: Int
    get() = if (this.year < 70) 2000 + this.year else 1900 + this.year

/**
 * yyyyMMdd格式的日期格式表示，例如20190801
 */
val YMDFormat = SimpleDateFormat("yyyyMMdd")

/**
 * 判断字符串是否是null或空串("")。
 * 如果是，返回null；否则返回该字符串本身。
 */
fun String?.assertBlank(): String?{
    return if (this != "") this else null
}

/**
 * 把任意对象序列化为BSON文档。
 * 如果序列化失败，会抛出异常。
 */
fun Any?.ToBSONDoc(): Document? {
    return if (this != null) Document.parse(JSON.toJSONString(this)) else null
}

/**
 * 把BSON文档反序列化为指定类的对象。
 * @param clazz 要反序列化的目标对象对应的类的反射Java Class
 * 例：document.ToObject(String::class.java)
 */
fun <T> Document?.ToObject(clazz: Class<T>): T? {
    return if (this != null) JSON.parseObject(this.toJson(), clazz) else null
}
