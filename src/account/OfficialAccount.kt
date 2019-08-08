package account

import com.alibaba.fastjson.JSON
import com.alibaba.fastjson.JSONObject
import kotlinx.coroutines.*
import message.Message
import message.TemplateMessage
import users.Users
import utils.*
import java.util.*

abstract class OfficialAccount (val token: String, val logger: Logger? = null, open val msgLogger: MessageDBLogger? = null, open val users: Users<*>?, private val APPID: String? = null, private val APPSECRET: String? = null, val operToken: String = token) {
    var accessToken: String? = null
        private set

    val ACCESS_TOKEN_UPDATE_INTERVAL: Long = 6000000

    class AccessTokenResponseBean: WXAPI() {
        var access_token: String = ""
        var expires_in: String = ""
    }

    private fun updateAccessTokenAsync(): Deferred<Unit> {
        if(APPID == null || APPSECRET == null)return GlobalScope.async {  }
        return GlobalScope.async {
            val query = mapOf("grant_type" to "client_credential", "appid" to APPID, "secret" to APPSECRET)
            val url = "https://api.weixin.qq.com/cgi-bin/token"
            try {
                val resObj = request(url = url, responseClass = AccessTokenResponseBean::class.java, query = query).resObj!!
                assertWXAPIErr(resObj)
                accessToken = resObj.access_token
                logger?.info("Access Token Success")
                println(JSON.toJSONString(resObj))
            }catch (e: Exception){
                accessToken = null
                logger?.error("Access Token Fail: ${e.message}")
                System.err.println(e)
            }
        }
    }

    val dispatcher = MessageDispatcher(this)

    val operation = Operation(this)

    val SERVE_MESSAGE_URL_PREFIX = "https://api.weixin.qq.com/cgi-bin/message/custom/send?access_token="

    /**
     * 在非协程块内的调用方法：
     *
     * 异步调用（缺点：不能得到执行结果）：GlobalScope.launch{ sendServeMessage() }
     *
     * 同步阻塞调用（缺点：阻塞当前线程）：runBlocking{ sendServeMessage() }
     */
    suspend fun sendServeMessage(message: Message){
        val url = SERVE_MESSAGE_URL_PREFIX + accessToken
        val res = request(url, RequestMethod.POST, message.toServeJSON(), WXAPI::class.java)
        assertWXAPIErr(res.resObj)
    }

    /**
     * 功能同sendServeMessage(message)。考虑到Java的调用需求，提供此函数，会进行阻塞线程的调用，调用者应自行处理阻塞问题。
     * @see sendServeMessage
     */
    fun sendServeMessageBlocking(message: Message){
        runBlocking { sendServeMessage(message) }
    }

//    /**
//     * 由于tomcat的糟糕特性，一个类在被引用之前不会被加载。因此必须手动引用才能保证定义的Replyer注册成功。
//     *
//     * 于是，任何OfficialAccount类的子类必须重载此方法，并返回一个List<ForceInit>。程序保证在第一个请求被Servlet处理之前，所有此函数返回的ForceInit对象均被初始化成功，并正常注册。
//     */
//    abstract fun forceInits(): List<ForceInit>;

    val TEMPLATE_MESSAGE_URL_PREFIX = "https://api.weixin.qq.com/cgi-bin/message/template/send?access_token="
    /**
     * 在非协程块内的调用方法：
     *
     * 异步调用（缺点：不能得到执行结果）：GlobalScope.launch{ sendTemplateMessage() }
     *
     * 同步阻塞调用（缺点：阻塞当前线程）：runBlocking{ sendTemplateMessage() }
     */
    suspend fun sendTemplateMessage(message: TemplateMessage){
        val url = TEMPLATE_MESSAGE_URL_PREFIX + accessToken
        val res = request(url, RequestMethod.POST, message.toTemplateJSON(), WXAPI::class.java)
        assertWXAPIErr(res.resObj)
    }

    /**
     * 功能同sendTemplateMessage(message)。考虑到Java的调用需求，提供此函数，会进行阻塞线程的调用，调用者应自行处理阻塞问题。
     * @see sendTemplateMessage
     */
    fun sendTemplateMessageBlocking(message: TemplateMessage){
        runBlocking { sendTemplateMessage(message) }
    }

    private val _components = mutableListOf<AccountComponent>()

    val components: List<AccountComponent>
        get(): List<AccountComponent> = _components

    fun getComponentByClass(clazz: Class<*>): AccountComponent?{
        val temp = _components.filter { it::class.java == clazz }
        return if(temp.isNotEmpty()) temp[temp.size-1] else null
    }

    fun use(component: AccountComponent){
        _components.add(component)
        component.registerTo(this)
    }

    init {
        runBlocking {
            updateAccessTokenAsync().await()
        }
        val timer = Timer()
        timer.schedule(object: TimerTask(){
            override fun run() {
                updateAccessTokenAsync()
            }
        }, ACCESS_TOKEN_UPDATE_INTERVAL, ACCESS_TOKEN_UPDATE_INTERVAL)
        operation.registerHandler("accessToken") { obj->
            operation.verifyTokenThrow(obj)
            JSONObject().apply { put("accessToken", accessToken) }
        }
    }
}
