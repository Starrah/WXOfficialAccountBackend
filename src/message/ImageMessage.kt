package message

import com.alibaba.fastjson.annotation.JSONField

class ImageMessage(
    ToUserName: String = "",
    FromUserName: String = "",
    CreateTime: Int = (System.currentTimeMillis() / 1000).toInt(),
    PicUrl: String? = null,
    MediaId: String? = null,
    MsgId: Long = 0
): Message(MessageType.image, ToUserName, FromUserName, CreateTime) {
    @JSONField(name = "MsgId")
    val MsgId = MsgId
    @JSONField(name = "MediaId")
    var MediaId = MediaId
    @JSONField(name = "PicUrl")
    var PicUrl = PicUrl

}