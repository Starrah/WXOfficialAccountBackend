package message

import com.alibaba.fastjson.annotation.JSONField

class EventMessage (
    ToUserName: String = "",
    FromUserName: String = "",
    CreateTime: Int = (System.currentTimeMillis() / 1000).toInt(),
    Event: String = ""
): Message(MessageType.event, ToUserName, FromUserName, CreateTime) {
    @JSONField(name = "Event")
    val Event = Event
}