package cn.starrah.wxoabkd.message

import com.alibaba.fastjson.JSON
import com.alibaba.fastjson.JSONObject
import com.alibaba.fastjson.annotation.JSONField
import cn.starrah.wxoabkd.utils.Color2String
import java.awt.Color

abstract class TemplateMessage(
    ToUserName: String,
    template_id: String,
    url: String? = null
): Message(MessageType.template, ToUserName, "")
{
    @JSONField(serialize = false)
    val url = url
    @JSONField(serialize = false)
    val template_id = template_id
    @JSONField(serialize = false)
    val colors = mutableMapOf<String, Color>()

    class MiniProgramInfo(val appid: String, val pagepath: String) {}

    @JSONField(serialize = false)
    var miniProgramInfo : MiniProgramInfo? = null

    @JSONField(serialize = false)
    var defaultColor = Color.BLACK

    open fun toTemplateJSON(): String{
        val origin = toChildClassDefinedPropertiesJSONObject()
        val inner = JSONObject()
        for((key, value) in origin.entries){
            inner[key] = mapOf("value" to value, "color" to Color2String(colors[key] ?: defaultColor))
        }
        val outter = JSONObject()
        outter["touser"] = ToUserName
        outter["template_id"] = template_id
        if(url != null) outter["url"] = url
        if(miniProgramInfo != null) outter["miniprogram"] = (JSON.toJSON(miniProgramInfo) as JSONObject)
        outter["data"] = inner
        return outter.toJSONString()
    }
}