package users

import com.alibaba.fastjson.annotation.JSONField
import com.mongodb.client.MongoCollection
import com.mongodb.client.model.ReplaceOptions
import org.bson.Document
import utils.Saveable
import utils.ToBSONDoc

/**
 * 以MongoDB数据库作为存储支持的用户类。
 */
open class DBUser (openId: String?, name: String?, collection: MongoCollection<Document>? = null): User(openId, name), Saveable{

    @Suppress("CanBePrimaryConstructorProperty")
    @JSONField(serialize = false)
    var collection: MongoCollection<Document>? = collection

    /**
     * 生成日志记录时所需的文档。
     *
     * 通常只包括openId和名字两个字段。
     */
    open fun logDoc(): Document? {
        val document = Document()
        openId?.let { document["openId"] = it }
        name?.let { document["name"] = it }
        return document
    }

    /**
     * 将对象中的所有信息同步到数据库里面。
     */
    override fun save() {
        if(collection != null){
            val queryDoc = Document()
            if(openId != null)queryDoc["openId"] = openId
            else if(name != null)queryDoc["name"] = name
            else throw Exception("不能保存openId和名字都为空的用户！")
            collection!!.replaceOne(
                Document().append("openId", openId).append("name", name),
                this.ToBSONDoc()!!,
                ReplaceOptions().upsert(true)
            )
        }
    }

}