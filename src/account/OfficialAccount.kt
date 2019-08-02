package account

import com.alibaba.fastjson.JSON
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import users.DBUser
import users.DBUsers
import utils.Logger
import utils.MessageDBLogger
import utils.request
import java.util.*

open class OfficialAccount (val token: String, val logger: Logger? = null, open val msgLogger: MessageDBLogger? = null, open val users: DBUsers<*>?, private val APPID: String? = null, private val APPSECRET: String? = null) {
    var accessToken: String? = null
        private set

    val ACCESS_TOKEN_UPDATE_INTERVAL: Long = 6000000

    class AccessTokenResponseBean {
        var access_token: String = ""
        var expires_in: String = ""
        var errmsg: String? = null
        var errcode: Int = 0
    }

    private fun updateAccessToken() {
        if(APPID == null || APPSECRET == null)return
        GlobalScope.launch {
            val query = mapOf("grant_type" to "client_credential", "appid" to APPID, "secret" to APPSECRET)
            val url = "https://api.weixin.qq.com/cgi-bin/token"
            try {
                val resObj = request(url = url, responseClass = AccessTokenResponseBean::class.java, query = query).resObj!!
                if(resObj.errmsg != null)throw Exception(resObj.errmsg)
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

    init {
        updateAccessToken()
        val timer = Timer()
        timer.schedule(object: TimerTask(){
            override fun run() {
                updateAccessToken()
            }
        }, ACCESS_TOKEN_UPDATE_INTERVAL, ACCESS_TOKEN_UPDATE_INTERVAL)
    }

    val dispatcher = MessageDispatcher(this)

    val operation = Operation()
}
