package message

import com.alibaba.fastjson.JSON
import org.bson.Document
import org.json.JSONObject
import org.json.XML
import utils.ToBSONDoc

enum class MessageType(val sampleObj: Class<*>){
    text(TextMessage::class.java),
    event(EventMessage::class.java),
    image(ImageMessage::class.java),
    no(NoMessage::class.java),
}

abstract class Message(
    val MsgType: MessageType,
    val ToUserName: String,
    val FromUserName: String,
    val CreateTime: Int = (System.currentTimeMillis() / 1000).toInt()
    )
{
    class MessageTypeNotSupportedException: Exception {
        constructor(): super() {}
        constructor(message: String): super(message) {}
    }

    companion object{
        fun parseFromXML(xmlString: String): Message{
            val JSONObj = XML.toJSONObject(xmlString)!!
            val MsgTypeStr = JSONObj.getString("MsgType")
            try {
                val enumObj = MessageType.valueOf(MsgTypeStr)
                val clazz= enumObj.sampleObj
                val JSONStr = JSONObj.toString()
                return JSON.parseObject(JSONStr, clazz) as Message
            }catch (e: IllegalArgumentException){
                throw MessageTypeNotSupportedException("消息类型$MsgTypeStr 尚不被支持。")
            }
        }
    }

    open fun toXML(): String{
        return XML.toString(JSONObject(JSON.toJSONString(this)))
    }

    open var LOG_OMIT_KEYS = listOf("ToUserName", "FromUserName", "MsgId", "CreateTime")

    open fun logDoc(): Document?{
        val bson = this.ToBSONDoc();
        var document: Document? = null
        var flag = false
        if(bson != null){
            document = Document()
            for(key in bson.keys){
                if(key !in LOG_OMIT_KEYS){
                    document[key] = bson[key]
                    flag = true
                }
            }
        }
        return if (flag) document else null
    }
}