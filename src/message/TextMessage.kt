package message

import com.alibaba.fastjson.annotation.JSONField

class TextMessage(
    ToUserName: String = "",
    FromUserName: String = "",
    CreateTime: Int = (System.currentTimeMillis() / 1000).toInt(),
    Content: String? = null,
    MsgId: Long = 0
): Message(MessageType.text, ToUserName, FromUserName, CreateTime) {
    @JSONField(name = "MsgId")
    val MsgId = MsgId
    @JSONField(name = "Content")
    var Content = Content

}