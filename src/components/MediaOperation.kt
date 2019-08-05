package components

import account.AccountComponent
import account.OfficialAccount
import com.alibaba.fastjson.JSON
import com.alibaba.fastjson.JSONObject
import message.ImageMessage
import utils.assertBlank

enum class MediaType(clazz: Class<*>?){
    image(ImageMessage::class.java),
}

class Media()
{
    var description: String? = null
    var key: String = ""
    var mediaId: String = ""
    var type: MediaType = MediaType.image

    constructor(type: MediaType,
                mediaId: String,
                key: String,
                description: String?): this()
    {
        this.type = type
        this.mediaId = mediaId
        this.key = key
        this.description = description
    }

    fun assertValidation(){
        if(mediaId == "" || key == "")throw Exception("Media信息无效！")
    }


}

/**
 * 不严谨的注释 先记下来之后再改
 *
 * 上传由用户通过curl自行。
 *
 * 接口：(token=令牌&oper=media&kind=接口名)
 *
 * WXAPIUrl：无参，指示上传方法；
 *
 * add：用户上传到微信服务器后存下来MediaId，然后自行调用本接口，参数为type；mediaID；key（可随意指定；任何地方先得到account的MediaOperation组件，再通过MediaOperation.get即可拿到Media对象，里面有MediaId既可用于创建Message了）；描述性文字（可选，人看的）
 *
 * list：无参全部素材列表
 *
 * remove：参数key，要删掉的素材的key
 */
class MediaOperation(): AccountComponent{
    override fun registerTo(account: OfficialAccount) {
        account.operation.registerHandler("media"){ obj->
            account.operation.verifyTokenThrow(obj)
            val kind = obj["kind"]
            val res = when(kind){
                "add"->{
                    val mediaObj = JSON.parseObject(obj.toJSONString(), Media::class.java).apply { assertValidation() }
                    set(mediaObj.key, mediaObj)
                    JSONObject().apply { put("success", true) }
                }
                "remove"->{
                    val key = (obj["key"] as String?).assertBlank()?:throw Exception("不合法的key")
                    val res = remove(key)
                    JSONObject().apply {
                        put("success", res != null)
                        if(res != null)put("media", res)
                    }
                }
                "list"->{
                    JSONObject().apply {
                        put("medias", mediaList())
                    }
                }
                "WXAPIUrl"->{
                    val WXPermenantMediaAPIUrl1 = "https://api.weixin.qq.com/cgi-bin/material/add_material?access_token="
                    val WXPermenantMediaAPIUrl2 = "&type=请填写"
                    val WXPermenantMediaAPIUrl0 = "curl \""
                    val WXPermenantMediaAPIUrl3 = "，如image等\" -F media=@请填入文件名.xxx"
                    val pureUrl = WXPermenantMediaAPIUrl1 + account.accessToken + WXPermenantMediaAPIUrl2
                    JSONObject().apply {
                        put("请求Url：",  pureUrl)
                        put("使用curl命令的请求示例：", WXPermenantMediaAPIUrl0 + pureUrl + WXPermenantMediaAPIUrl3)
                    }
                }
                else->{
                    throw Exception("kind类型不支持！")
                }
            }
            res
        }
    }

    private val mediaMap = mutableMapOf<String, Media>()

    fun set(key: String, media: Media){
        mediaMap[key] = media
    }

    fun remove(key: String): Media?{
        val media = mediaMap[key]
        mediaMap.remove(key)
        return media
    }

    fun mediaList(): List<Media>{
        val list = mutableListOf<Media>()
        for((_, value) in mediaMap){
            list.add(value)
        }
        return list
    }

    fun get(key: String): Media?{
        return mediaMap[key]
    }

}