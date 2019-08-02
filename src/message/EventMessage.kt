package message

class EventMessage (
    ToUserName: String = "",
    FromUserName: String = "",
    CreateTime: Int = (System.currentTimeMillis() / 1000).toInt(),
    val Event: String = ""
): Message(MessageType.event, ToUserName, FromUserName, CreateTime) {

}