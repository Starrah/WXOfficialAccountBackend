package message

class ImageMessage(
    ToUserName: String = "",
    FromUserName: String = "",
    CreateTime: Int = (System.currentTimeMillis() / 1000).toInt(),
    var PicUrl: String? = null,
    var MediaId: String? = null,
    val MsgId: Long = 0
): Message(MessageType.image, ToUserName, FromUserName, CreateTime) {

}