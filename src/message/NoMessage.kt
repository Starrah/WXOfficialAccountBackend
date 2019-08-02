package message

import org.bson.Document

class NoMessage(
    ToUserName: String = "",
    FromUserName: String = "",
    CreateTime: Int = (System.currentTimeMillis() / 1000).toInt()
) : Message(MessageType.no, ToUserName, FromUserName, CreateTime) {
    override fun logDoc(): Document? {
        return null
    }

    override fun toXML(): String {
        return "success"
    }
}