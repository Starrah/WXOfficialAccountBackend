package users

import com.alibaba.fastjson.JSONObject

/**
 * 用户抽象类
 */
abstract class User (open var openId: String?, open var name: String?) {

    /**
     * 生成日志记录时所需的文档。
     *
     * 通常只包括openId和名字两个字段。
     */
    open fun logDoc(): JSONObject? {
        val document = JSONObject()
        openId?.let { document["openId"] = it }
        name?.let { document["name"] = it }
        return document
    }
}