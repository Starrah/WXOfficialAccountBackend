package message

import com.alibaba.fastjson.JSON
import com.alibaba.fastjson.annotation.JSONField
import org.bson.Document
import org.json.XML
import utils.ToBSONDoc

enum class MessageType(val clazz: Class<*>) {
    text(TextMessage::class.java),
    event(EventMessage::class.java),
    image(ImageMessage::class.java),
    no(NoMessage::class.java),
    template(TemplateMessage::class.java),
}

/**
 * 注意！kotlin继承此类时，如果把某个属性定义在主构造函数内部，如果再加@JSONField注解的话，这个注解会被认为是定义在主构造函数的参数上而非属性上，从而不生效。尽管fastjson做了一些适配但并不完善，因此！！不要把带有@JSONField注解的属性定义在主构造函数内部！
 */
abstract class Message(
    MsgType: MessageType,
    ToUserName: String,
    FromUserName: String,
    CreateTime: Int = (System.currentTimeMillis() / 1000).toInt()
) {
    class MessageTypeNotSupportedException: Exception {
        constructor(): super() {}
        constructor(message: String): super(message) {}
    }

    companion object {
        fun parseFromXML(xmlString: String): Message {
            val JSONObj = XML.toJSONObject(xmlString)["xml"] as org.json.JSONObject
            val MsgTypeStr = JSONObj.getString("MsgType")
            try {
                val enumObj = MessageType.valueOf(MsgTypeStr)
                val clazz = enumObj.clazz
                val JSONStr = JSONObj.toString()
                return JSON.parseObject(JSONStr, clazz) as Message
            } catch (e: IllegalArgumentException) {
                throw MessageTypeNotSupportedException("消息类型$MsgTypeStr 尚不被支持。")
            }
        }
    }

    open fun toXML(): String {
        return XML.toString(org.json.JSONObject().put("xml", org.json.JSONObject(JSON.toJSONString(this))))
    }

    protected fun toChildClassDefinedPropertiesJSONObject(): com.alibaba.fastjson.JSONObject {
        val inner = JSON.toJSON(this) as com.alibaba.fastjson.JSONObject
        for (key in LOG_OMIT_KEYS) {
            inner.remove(key)
        }
        inner.remove("MsgType")
        return inner
    }

    open fun toServeJSON(): String {
        val doc = logDoc()
        if (doc == null) throw Exception("不能向用户发送空消息！")
        val inner = toChildClassDefinedPropertiesJSONObject()
        val outter = com.alibaba.fastjson.JSONObject()
        outter["touser"] = ToUserName
        outter["msgtype"] = MsgType.name
        outter[MsgType.name] = inner
        return outter.toJSONString()
    }

    @JSONField(name = "CreateTime")
    val CreateTime = CreateTime
    @JSONField(name = "FromUserName")
    val FromUserName = FromUserName
    @JSONField(name = "ToUserName")
    val ToUserName = ToUserName
    @JSONField(name = "MsgType")
    val MsgType = MsgType

    @JSONField(serialize = false)
    open var LOG_OMIT_KEYS = listOf("ToUserName", "FromUserName", "MsgId", "CreateTime")

    open fun logDoc(): Document? {
        val bson = this.ToBSONDoc();
        if (bson == null) return null
        for (key in LOG_OMIT_KEYS) {
            bson.remove(key)
        }
        return if (bson.keys.size != 0) bson else null
    }
}