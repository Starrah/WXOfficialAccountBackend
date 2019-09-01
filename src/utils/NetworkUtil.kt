package utils

import com.alibaba.fastjson.JSON
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.http.entity.ContentType
import org.apache.http.entity.mime.MultipartEntityBuilder
import org.apache.http.entity.mime.content.FileBody
import java.io.File
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.file.Path
import java.time.Duration

enum class RequestMethod(val allowBody: Boolean) {
    GET(false),
    POST(true)
}

interface NetworkResInterface {}

/**
 * 当网络请求返回值为4XX/5XX或网络连接失败时抛出此异常。
 */
class NetworkBadRespondedException(val statusCode: Int, val resStr: String, val reqUrl: String? = null) : Exception(
        "Request " + (if (reqUrl != null) "to ${reqUrl} " else "") + "responded with status ${statusCode} : " + resStr),
        NetworkResInterface {}

private fun AssertContentTypeJson(headers: Map<String, String>?): Boolean {
    if (headers != null) {
        return headers["Content-Type"]?.contains("application/json") == true ||
                headers["Content-type"]?.contains("application/json") == true ||
                headers["content-Type"]?.contains("application/json") == true ||
                headers["content-type"]?.contains("application/json") == true
    } else return false
}

/**
 * 网络请求成功的返回对象。
 *
 * 如果返回的头中有Content-Type: application/json，并且请求时传入了类对象，则会进行一次解析并存在resObj中。否则resObj一律为null。
 */
class NetworkResponse<T>(
        /** 返回码 */
        val statusCode: Int,
        /** 原始的未经解析的返回字符串 */
        val resStr: String,
        /** 返回的头 */
        val headers: Map<String, String>,
        /** 反序列化目标类Java Class */
        clazz: Class<T>? = null
) : NetworkResInterface
{
    /** 经过JSON解析为T类后的对象 */
    val resObj: T?

    init {
        if (AssertContentTypeJson(headers) && clazz != null) {
            resObj = JSON.parseObject(resStr, clazz)
        } else {
            resObj = null
        }
    }
}

/**
 * 将query的Map表示转换为query字符串，不含?
 *
 * @param query 表示query的Map，例如mapOf("qwq" to "a", "yyy" to b)
 * @return query字符串，例如"qwq=a&yyy=b"
 */
fun queryToString(query: Map<String, String>): String {
    var str = ""
    for ((key, value) in query.entries) {
        str += (key + "=" + value + "&")
    }
    return str.substring(0, str.length - 1)
}

/**
 * 将query字符串转换为query的Map表示
 *
 * @param str query字符串，例如"qwq=a&yyy=b"。前面含不含"?"都可以，或者是整个网址也可以，会忽略?之前的部分
 * @return 表示query的Map，例如mapOf("qwq" to "a", "yyy" to b)
 * @throws Exception 如果字符串中含有多个?，或&、=出现的位置不合适
 */
fun stringToQuery(str: String?): Map<String, String> {
    val map: MutableMap<String, String> = mutableMapOf()
    if (str == null) return map
    val indexOfMark = str.indexOf('?')
    val realStr = if (indexOfMark != -1) str else str.substring(indexOfMark + 1, str.length)
    val pairs = realStr.split('&')
    for (pair in pairs) {
        val pairSplit = pair.split('=')
        map[pairSplit[0]] = pairSplit[1]
    }
    return map
}

/**
 * 网络请求的挂起函数方法。目前只实现了GET和POST两种选项。
 *
 * 在POST方法时，接收body参数。若是String，直接作为请求体；否则转为JSON后作为请求体并自动添加“Content-Type: application/json”头。
 *
 * 会自动跟随3XX重定向。
 *
 * 在GET方法和POST方法时，URL中的query串部分写在query中。
 *
 * 可以在一般的协程作用域中调用函数。对于不处于协程作用域的情况，可以：
 *
 * runBlocking{ request(...) }//阻塞主线程，直到得到结果。
 *
 * 或GlobalScope.async{ request(...) }//异步处理，所在函数会立刻返回。返回值可以被await。
 *
 * @param url 请求的URL。通常只包括URL部分（例如，api.qwq.com/yyy，不含?及其后面的query串）；但当query传null时，本部分也可直接包含带query串的完整请求URL。但注意不能URL中既带?，又在query参数中传入非null值，否则不能正确请求且抛出异常。
 * @param method 请求的方法，默认为GET
 * @param body 请求的消息体，仅限POST等有消息体的方法。可以传入String或其他任何类型。传入String时，直接作为消息体；否则，作为JSON解析后作为消息体，并自动添加“Content-Type: application/json”头。
 * @param responseClass 类对象，如果传入非null值，则返回值中会尝试按照所给的类做一次JSON反序列化。如果传入null值，则返回的NetworkResponse对象中resObj字段将为null。此时如编译器无法推断泛型类型，可以传入Nothing(kotlin)或不作为泛型方法(java)
 * @param query 请求query参数对应的map。会附加在url参数的尾部形成完整的请求URL。
 * @param headers 请求头。会覆盖自动添加的头。
 * @param timeOut 可选，超时时间，单位ms，默认为5000ms
 *
 * @return NetworkResponse<T> 仅在返回码为1XX/2XX/3XX时返回对象。
 * @see NetworkResponse
 * @see NetworkBadRespondedException
 *
 * @throws Exception 如果请求有任何逻辑错误，例如对不应有消息体的请求方法传入了非null的body等。
 * @throws NetworkBadRespondedException 如果网络连接失败或服务器返回4XX/5XX或返回的内容是JSON但JSON反序列化错误
 */
suspend fun <T> request(
        url: String,
        method: RequestMethod = RequestMethod.GET,
        body: Any? = null,
        responseClass: Class<T>? = null,
        query: Map<String, String>? = null,
        headers: Map<String, String>? = null,
        timeOut: Int = 5000
): NetworkResponse<T> {
    var realUrl: String = url
    if (url.contains('?')) {
        if (query != null) throw Exception("不能同时在url中包括?和对query传入非null值！")
    } else {
        if (query != null) {
            realUrl += ("?" + queryToString(query))
        }
    }
    if (body != null && !method.allowBody) throw Exception("${method.name}方法不能含有消息体！")
    var realBody: String = ""
    var jsonParsedFlag = false
    if (body != null) {
        if (body is String) realBody = body
        else {
            realBody = JSON.toJSONString(body)!!
            jsonParsedFlag = true
        }
    }

    val resObj = withContext(Dispatchers.IO) {
        try {
            val client =
                    HttpClient.newBuilder().followRedirects(HttpClient.Redirect.ALWAYS).version(
                            HttpClient.Version.HTTP_1_1).build()!!
            val requestBuilder: HttpRequest.Builder =
                    HttpRequest.newBuilder(URI.create(realUrl)).timeout(Duration.ofMillis(timeOut.toLong()))
            if (jsonParsedFlag) {
                requestBuilder.header("Content-Type", "application/json")
            }
            if (headers != null) {
                for ((key, value) in headers.entries) {
                    requestBuilder.header(key, value)
                }
            }
            when (method) {
                RequestMethod.GET -> {
                    requestBuilder.GET()
                }
                RequestMethod.POST -> {
                    requestBuilder.POST(HttpRequest.BodyPublishers.ofString(realBody))
                }
                else -> {
                    throw NotImplementedError("不支持此方法")
                }
            }
            val request = requestBuilder.build()
            val response = client.send(request, HttpResponse.BodyHandlers.ofString())!!
            val realResMap = mutableMapOf<String, String>()
            for ((key, value) in response.headers().map()) {
                var totalStr: String = ""
                if (value.size == 1) {
                    totalStr = value[0]
                } else if (value.size >= 2) {
                    for (valueStr in value) {
                        totalStr += (valueStr + ";")
                    }
                }
                realResMap[key] = totalStr
            }
            if (response.statusCode() < 400) {
                return@withContext NetworkResponse(response.statusCode(), response.body(), realResMap, responseClass)
            } else {
                throw NetworkBadRespondedException(response.statusCode(), response.body(), url)
            }
        } catch (e: NetworkBadRespondedException) {
            return@withContext e
        } catch (e: Exception) {
            return@withContext NetworkBadRespondedException(0, e.message ?: "", url)
        }
    }

    if (resObj is NetworkBadRespondedException) throw resObj
    else return resObj as NetworkResponse<T>
}

/**
 * 上传文件挂起函数方法。目前只实现了POST选项，以表单(multipart/formdata)形式发送。
 *
 * 会自动跟随3XX重定向。
 *
 * 可以在一般的协程作用域中调用函数。对于不处于协程作用域的情况，可以：
 *
 * runBlocking{ request(...) }//阻塞主线程，直到得到结果。
 *
 * 或GlobalScope.async{ request(...) }//异步处理，所在函数会立刻返回。返回值可以被await。
 *
 * @param url 请求的URL。通常只包括URL部分（例如，api.qwq.com/yyy，不含?及其后面的query串）；但当query传null时，本部分也可直接包含带query串的完整请求URL。但注意不能URL中既带?，又在query参数中传入非null值，否则不能正确请求且抛出异常。
 * @param method 请求的方法，默认为GET
 * @param formKey 文件在表单项中对应的键名
 * @param file 要发送的文件对象
 * @param otherKVPairs 在表单中一并发送的其他字符串格式键值对。
 * @param responseClass 类对象，如果传入非null值，则返回值中会尝试按照所给的类做一次JSON反序列化。如果传入null值，则返回的NetworkResponse对象中resObj字段将为null。此时如编译器无法推断泛型类型，可以传入Nothing(kotlin)或不作为泛型方法(java)
 * @param query 请求query参数对应的map。会附加在url参数的尾部形成完整的请求URL。
 * @param headers 请求头。会覆盖自动添加的头。
 * @param timeOut 可选，超时时间，单位ms，默认为5000ms
 *
 * @return NetworkResponse<T> 仅在返回码为1XX/2XX/3XX时返回对象。
 * @see NetworkResponse
 * @see NetworkBadRespondedException
 *
 * @throws Exception 如果请求有任何逻辑错误，例如对不应有消息体的请求方法传入了非null的body等。
 * @throws NetworkBadRespondedException 如果网络连接失败或服务器返回4XX/5XX
 */
suspend fun <T> requestUploadFile(
        url: String,
        method: RequestMethod = RequestMethod.POST,
        formKey: String,
        file: File,
        otherKVPairs: Map<String, String>? = null,
        responseClass: Class<T>? = null,
        query: Map<String, String>? = null,
        headers: Map<String, String>? = null,
        timeOut: Int = 5000
): NetworkResponse<T> {
    var realUrl: String = url
    if (url.contains('?')) {
        if (query != null) throw Exception("不能同时在url中包括?和对query传入非null值！")
    } else {
        if (query != null) {
            realUrl += ("?" + queryToString(query))
        }
    }
    if (!method.allowBody) throw Exception("${method.name}方法不能含有消息体！")

    val resObj = withContext(Dispatchers.IO) {
        try {
            val multipartFormDataBoundary = "----Java11HttpClientFormBoundary" + System.currentTimeMillis().toString()
            val multipartEntity = MultipartEntityBuilder.create()
            if (otherKVPairs != null) {
                for ((key, value) in otherKVPairs.entries) {
                    multipartEntity.addTextBody(key, value)
                }
            }
            multipartEntity.addPart(formKey, FileBody(file, ContentType.DEFAULT_BINARY))
                    .setBoundary(multipartFormDataBoundary)

            val client =
                    HttpClient.newBuilder().followRedirects(HttpClient.Redirect.ALWAYS).version(
                            HttpClient.Version.HTTP_1_1).build()!!
            val requestBuilder: HttpRequest.Builder =
                    HttpRequest.newBuilder(URI.create(realUrl)).timeout(Duration.ofMillis(timeOut.toLong()))
            requestBuilder.header("Content-Type", "multipart/form-data; boundary=$multipartFormDataBoundary")
            if (headers != null) {
                for ((key, value) in headers.entries) {
                    requestBuilder.header(key, value)
                }
            }
            when (method) {
                RequestMethod.POST -> {
                    requestBuilder.POST(HttpRequest.BodyPublishers.ofInputStream { multipartEntity.build().content })
                }
                else -> {
                    throw NotImplementedError("不支持此方法")
                }
            }
            val request = requestBuilder.build()
            val response = client.send(request, HttpResponse.BodyHandlers.ofString())!!
            val realResMap = mutableMapOf<String, String>()
            for ((key, value) in response.headers().map()) {
                var totalStr: String = ""
                if (value.size == 1) {
                    totalStr = value[0]
                } else if (value.size >= 2) {
                    for (valueStr in value) {
                        totalStr += (valueStr + ";")
                    }
                }
                realResMap[key] = totalStr
            }
            if (response.statusCode() < 400) {
                return@withContext NetworkResponse(response.statusCode(), response.body(), realResMap, responseClass)
            } else {
                throw NetworkBadRespondedException(response.statusCode(), response.body(), url)
            }
        } catch (e: NetworkBadRespondedException) {
            return@withContext e
        } catch (e: Exception) {
            return@withContext NetworkBadRespondedException(0, e.message ?: "", url)
        }
    }

    if (resObj is NetworkBadRespondedException) throw resObj
    else return resObj as NetworkResponse<T>
}

/**
 * 下载文件的挂起函数方法。目前只实现了GET选项。
 *
 * 会自动跟随3XX重定向。
 *
 * 可以在一般的协程作用域中调用函数。对于不处于协程作用域的情况，可以：
 *
 * runBlocking{ request(...) }//阻塞主线程，直到得到结果。
 *
 * 或GlobalScope.async{ request(...) }//异步处理，所在函数会立刻返回。返回值可以被await。
 *
 * @param url 请求的URL。通常只包括URL部分（例如，api.qwq.com/yyy，不含?及其后面的query串）；但当query传null时，本部分也可直接包含带query串的完整请求URL。但注意不能URL中既带?，又在query参数中传入非null值，否则不能正确请求且抛出异常。
 * @param method 请求的方法，默认为GET
 * @param file 目标要保存的文件对象
 * @param body 请求的消息体，仅限POST等有消息体的方法。可以传入String或其他任何类型。传入String时，直接作为消息体；否则，作为JSON解析后作为消息体，并自动添加“Content-Type: application/json”头。
 * @param query 请求query参数对应的map。会附加在url参数的尾部形成完整的请求URL。
 * @param headers 请求头。会覆盖自动添加的头。
 * @param timeOut 可选，超时时间，单位ms，默认为5000ms
 *
 * @return File? 仅在返回码为1XX/2XX/3XX时返回对象。在服务器返回的不是文件时，为null
 * @see NetworkBadRespondedException
 *
 * @throws Exception 如果请求有任何逻辑错误，例如对不应有消息体的请求方法传入了非null的body等。
 * @throws NetworkBadRespondedException 如果网络连接失败或服务器返回4XX/5XX
 */
suspend fun requestDownloadFile(
        url: String,
        method: RequestMethod = RequestMethod.GET,
        file: File,
        body: Any? = null,
        query: Map<String, String>? = null,
        headers: Map<String, String>? = null,
        timeOut: Int = 5000
): File {
    var realUrl: String = url
    if (url.contains('?')) {
        if (query != null) throw Exception("不能同时在url中包括?和对query传入非null值！")
    } else {
        if (query != null) {
            realUrl += ("?" + queryToString(query))
        }
    }
    if (body != null && !method.allowBody) throw Exception("${method.name}方法不能含有消息体！")
    var realBody: String = ""
    var jsonParsedFlag = false
    if (body != null) {
        if (body is String) realBody = body
        else {
            realBody = JSON.toJSONString(body)!!
            jsonParsedFlag = true
        }
    }

    val resObj = withContext(Dispatchers.IO) {
        try {
            val client =
                    HttpClient.newBuilder().followRedirects(HttpClient.Redirect.ALWAYS).version(
                            HttpClient.Version.HTTP_1_1).build()!!
            val requestBuilder: HttpRequest.Builder =
                    HttpRequest.newBuilder(URI.create(realUrl)).timeout(Duration.ofMillis(timeOut.toLong()))
            if (jsonParsedFlag) {
                requestBuilder.header("Content-Type", ": application/json")
            }
            if (headers != null) {
                for ((key, value) in headers.entries) {
                    requestBuilder.header(key, value)
                }
            }
            when (method) {
                RequestMethod.GET -> {
                    requestBuilder.GET()
                }
                else -> {
                    throw NotImplementedError("不支持此方法")
                }
            }
            val request = requestBuilder.build()
            val response = client.send(request, HttpResponse.BodyHandlers.ofFile(Path.of(file.path)))!!
            val realResMap = mutableMapOf<String, String>()
            for ((key, value) in response.headers().map()) {
                var totalStr: String = ""
                if (value.size == 1) {
                    totalStr = value[0]
                } else if (value.size >= 2) {
                    for (valueStr in value) {
                        totalStr += (valueStr + ";")
                    }
                }
                realResMap[key] = totalStr
            }
            if (response.statusCode() < 400) {
                return@withContext if (response.body() != null) File(response.body().toUri()) else null
            } else {
                throw NetworkBadRespondedException(response.statusCode(), response.body().toString(), url)
            }
        } catch (e: NetworkBadRespondedException) {
            return@withContext e
        } catch (e: Exception) {
            return@withContext NetworkBadRespondedException(0, e.message ?: "", url)
        }
    }

    if (resObj is NetworkBadRespondedException) throw resObj
    else return resObj as File
}