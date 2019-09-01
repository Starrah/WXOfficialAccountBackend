@file:Suppress("DEPRECATION")

package cn.starrah.wxoabkd.utils

import com.alibaba.fastjson.JSON
import kotlinx.coroutines.*
import org.bson.Document
import java.awt.Color
import java.io.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * 比较两个Date对象是否在日的精度下表示相同的日期。
 * @param other
 */
fun Date.sameDay(other: Date): Boolean {
    return this.year == other.year && this.month == other.month && this.day == other.day
}

fun Date.omitTime(): Date {
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
fun String?.assertBlank(): String? {
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
            if (i != 0) sb.append("_")
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

fun assertWXAPIErr(bean: WXAPI?) {
    bean!!
    if (bean.errcode != 0) throw WXAPIException(bean.errcode, bean.errmsg)
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
suspend fun InputStream.pipe(outputStream: OutputStream, bufferSize: Int = 8192) {
    withContext(Dispatchers.IO) {
        if (bufferSize > 0) {
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
        } else {
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
suspend fun Reader.pipe(writer: Writer, bufferSize: Int = 8192) {
    withContext(Dispatchers.IO) {
        if (bufferSize > 0) {
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
        } else {
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

/**
 * 类似于JS的Promise的封装，可以用于把协程结合现有的回调函数模式的库来加以使用。
 *
 * 泛型处填实际需要的返回值类型，如Promise<Int>
 *
 * 不建议在非回调函数的情况下使用本工具类，因为那样的话要么可能可以通过阻塞操作的withContext(Dispatchers.IO)实现，要么可能可以通过正常的协程方法实现。
 *
 * 它提供一个挂起函数run，参数为一个函数（语句块），无参且为Unit返回类型。
 * 这里即为Promise执行的语句主体，在其中可以任意（同一协程内、其他协程中、不同线程中均可）调用resolve/reject方法。
 * resolve/reject方法是Promise对象的成员函数，因此Promise对象可以被在外面存起来并在合适的时候再promise.resolve()
 *
 * run方法被调用时，会立即执行传入的函数语句，并在执行完后挂起，直到resolve/reject方法被调用，则结束挂起并返回值或抛出异常。
 *
 * 对于每个Promise对象，run方法只能被调用一次，否则会抛出异常；
 * resolve/reject方法只能被调用一次，之后的所有调用均会抛出异常；
 * 如果语句块内发生未处理的异常，当它被从语句块对应的函数中抛出时，视为reject了该异常。
 *
 * 如果在传入的语句块执行结束（从语句块内返回）前就进行了resolve/reject，则run方法不是在resolve/reject调用时立即返回，而要等到语句块执行结束才会返回。
 *
 * 实现注释：用status标记Promise的当前状态，用suspendCoroutine挂起协程；为应对resolve被从不同线程调用情况，检测和修改status时加线程锁以确保一致性。
 *
 * 示例：在挂起函数内部，
 * val result: Int = Promise<Int>.run{
 *   sendHttpRequest(
 *      url = "example.com",
 *      success = {
 *          resolve(it)
 *      },
 *      fail = {
 *          reject(Exception(it.errmsg))
 *      }
 *   )
 * }
 * 泛型处填实际需要的返回值类型。
 * run是一个挂起函数，调用它后协程会被挂起，直到对应的Promise在某处（本线程/其他线程）resolve或reject，且传入的lambda已返回。
 *
 *
 */
class Promise<T> {
    enum class PromiseStatus {
        UNUSED,
        PENDING,
        RESOLVED,
        REJECTED
    }

    var status = PromiseStatus.UNUSED
        private set

    private var cont: Continuation<Unit>? = null

    private var job: Job? = null

    private var res: Any? = null

    fun resolve(value: T) {
        synchronized(this) {
            if (status != PromiseStatus.PENDING) throw java.lang.Exception("promise is resolved/rejected twice")
            status = PromiseStatus.RESOLVED
            res = value
            job?.cancel()
            cont?.resume(Unit)
        }
    }

    fun reject(err: Throwable) {
        synchronized(this) {
            if (status != PromiseStatus.PENDING) throw java.lang.Exception("promise is resolved/rejected twice")
            status = PromiseStatus.REJECTED
            res = err
            job?.cancel()
            cont?.resume(Unit)
        }
    }

    suspend fun run(block: suspend Promise<T>.()->Unit): T {
        if (status != PromiseStatus.UNUSED) throw java.lang.Exception("a promise's run method can only be called once")
        status = PromiseStatus.PENDING

        try {
            job = coroutineScope {
                launch { block() }
            }
        } catch (e: CancellationException) {
        } catch (e: java.lang.Exception) {
            reject(e)
        }

        if (status == PromiseStatus.PENDING) {
            suspendCoroutine<Unit> {
                synchronized(this) {
                    cont = it
                    if (status != PromiseStatus.PENDING) {
                        cont?.resume(Unit) //解决在另一线程resolve，resolve位置恰好在条件判断为PENDING但cont没来得及设置，导致resolve方法没有唤醒协程的情况。
                    }
                }
            }
        }

        if (status == PromiseStatus.RESOLVED) {
            return res as T
        } else if (status == PromiseStatus.REJECTED) {
            throw res as Throwable
        } else throw Error("error")
    }
}