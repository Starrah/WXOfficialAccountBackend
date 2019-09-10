package cn.starrah.wxoabkd.users

import com.alibaba.fastjson.annotation.JSONField
import com.mongodb.client.MongoCollection
import com.mongodb.client.model.ReplaceOptions
import org.bson.Document
import cn.starrah.wxoabkd.utils.Saveable
import cn.starrah.wxoabkd.utils.ToBSONDoc

/**
 * 以MongoDB数据库作为存储支持的用户类。
 */
open class DBUser (openId: String?, name: String?, collection: MongoCollection<Document>? = null): User(openId, name),
                                                                                                   Saveable {

    @Suppress("CanBePrimaryConstructorProperty")
    @JSONField(serialize = false)
    var collection: MongoCollection<Document>? = collection

    /**
     * 将对象中的所有信息同步到数据库里面。
     */
    override fun save() {
        if(collection != null){
            val queryDoc = Document()
            if(name != null)queryDoc["name"] = name
            else if(openId != null)queryDoc["openId"] = openId
            else throw Exception("不能保存名字和openId都为空的用户！")
            collection!!.replaceOne(
                queryDoc,
                this.ToBSONDoc()!!,
                ReplaceOptions().upsert(true)
            )
        }
    }

}