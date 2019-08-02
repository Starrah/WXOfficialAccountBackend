package message

class TextMessage(
    ToUserName: String = "",
    FromUserName: String = "",
    CreateTime: Int = (System.currentTimeMillis() / 1000).toInt(),
    var Content: String? = null,
    val MsgId: Long = 0
): Message(MessageType.text, ToUserName, FromUserName, CreateTime) {

}