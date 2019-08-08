@file:Suppress("DEPRECATION")

package utils

import com.alibaba.fastjson.JSON
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import org.bson.Document
import java.awt.Color
import java.io.*
import java.text.SimpleDateFormat
import java.util.*

/**
 * 比较两个Date对象是否在日的精度下表示相同的日期。
 * @param other
 */
fun Date.sameDay(other: Date): Boolean{
    return this.year == other.year && this.month == other.month && this.day == other.day
}

fun Date.omitTime(): Date{
    time = (time / 86400000) * 86400000
    return this
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

// 驼峰命名转下划线命名 https://blog.csdn.net/sword_out/article/details/51518097
fun camelToUnderline(str: String?): String {
    if (str == null || str.trim { it <= ' ' }.isEmpty()) {
        return ""
    }

    val len = str.length
    val sb = StringBuilder(len)
    for (i in 0 until len) {
        val c = str[i]
        if (Character.isUpperCase(c)) {
            if(i != 0)sb.append("_")
            sb.append(Character.toLowerCase(c))
        } else {
            sb.append(c)
        }
    }
    return sb.toString()
}

open class WXAPI {
    var errmsg: String? = null
    var errcode: Int = 0
}

class WXAPIException(val errcode: Int, errmsg: String?): Exception(errmsg) {}

fun assertWXAPIErr(bean: WXAPI?){
    bean!!
    if(bean.errcode != 0)throw WXAPIException(bean.errcode, bean.errmsg)
}

//Color与颜色16进制字符串互转 https://blog.csdn.net/signsmile/article/details/3899876
fun String2Color(str: String): Color {
    val i = Integer.parseInt(str.substring(1), 16)
    return Color(i)
}

fun Color2String(color: Color): String {
    var R = Integer.toHexString(color.red)
    R = if (R.length < 2) "0$R" else R
    var B = Integer.toHexString(color.blue)
    B = if (B.length < 2) "0$B" else B
    var G = Integer.toHexString(color.green)
    G = if (G.length < 2) "0$G" else G
    return "#$R$B$G"
}

/**
 * 协程异步地把一个InputSteam中的全部数据输出到OutputStream中，并关闭两个流。
 *
 * @param outputStream
 * @param bufferSize 缓冲区大小。非正值则为无缓冲区。
 */
suspend fun InputStream.pipe(outputStream: OutputStream, bufferSize: Int = 8192){
    withContext(Dispatchers.IO){
        if(bufferSize > 0) {
            val bufferIn = BufferedInputStream(this@pipe, bufferSize)
            val bufferOut = BufferedOutputStream(outputStream, bufferSize)
            try {
                while (true) {
                    val bytes = bufferIn.readNBytes(bufferSize)
                    if (bytes.isEmpty()) break
                    bufferOut.write(bytes)
                    bufferOut.flush()
                    yield()
                }
            } finally {
                bufferIn.close()
                bufferOut.close()
            }
        }else{
            try {
                while (true) {
                    val bytes = this@pipe.readNBytes(bufferSize)
                    if (bytes.isEmpty()) break
                    outputStream.write(bytes)
                    outputStream.flush()
                    yield()
                }
            } finally {
                this@pipe.close()
                outputStream.close()
            }
        }
    }
}

/**
 * 协程异步地把一个Reader中的全部数据输出到Writer中，并关闭两个流。
 *
 * @param writer
 * @param bufferSize 缓冲区大小。非正值则为无缓冲区。
 */
suspend fun Reader.pipe(writer: Writer, bufferSize: Int = 8192){
    withContext(Dispatchers.IO){
        if(bufferSize > 0) {
            val bufferIn = BufferedReader(this@pipe, bufferSize)
            val bufferOut = BufferedWriter(writer, bufferSize)
            try {
                while (true) {
                    val bytes = CharArray(bufferSize)
                    val count = bufferIn.read(bytes)
                    if (count < 0) break
                    bufferOut.write(bytes, 0, count)
                    bufferOut.flush()
                    yield()
                }
            } finally {
                bufferIn.close()
                bufferOut.close()
            }
        }else{
            try {
                while (true) {
                    val bytes = CharArray(bufferSize)
                    val count = this@pipe.read(bytes)
                    if (count < 0) break
                    writer.write(bytes, 0, count)
                    writer.flush()
                    yield()
                }
            } finally {
                this@pipe.close()
                writer.close()
            }
        }
    }
}