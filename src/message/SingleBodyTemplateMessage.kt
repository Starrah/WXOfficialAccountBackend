package message

import com.alibaba.fastjson.JSON
import com.alibaba.fastjson.JSONObject
import com.alibaba.fastjson.annotation.JSONField

class SingleBodyTemplateMessage(
    ToUserName: String,
    template_id: String,
    var body: String,
    bodyNameInTemplate: String? = null,
    url: String? = null
): TemplateMessage(ToUserName, template_id, url) {

    @JSONField(serialize = false)
    val bodyNameInTemplate = bodyNameInTemplate

    override fun toTemplateJSON(): String {
        val superString = super.toTemplateJSON()
        if (bodyNameInTemplate == null || bodyNameInTemplate == "body") return superString
        val obj = JSON.parseObject(superString) as JSONObject
        val data = obj["data"] as JSONObject
        data[bodyNameInTemplate] = data["body"]
        data.remove("body")
        return obj.toJSONString()
    }
}