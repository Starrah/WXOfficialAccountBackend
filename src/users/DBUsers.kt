package users

import com.mongodb.client.MongoCollection
import org.bson.Document
import utils.Saveable
import utils.ToObject

/**
 * 用户信息管理的泛型类定义。
 *
 * 用于实现从数据库中取出某一集合，利用它们的信息实例化用户对象和保存操作等，不提供其他功能。
 *
 * 使用方法：对于一个特定群体的用户，继承XXXUser类后，再定义单例XXXUsers: DBUsers<XXXUser>(collection, XXXUser::class.java)
 */
open class DBUsers <T: DBUser> (
    /** 用户信息对应的数据库集合 */ val collection: MongoCollection<Document>,
    /** 被泛型实例化的类对象 */clazz: Class<T>
): Users<T>(clazz), Saveable
{
    init {
        load()
    }

    override fun load() {
        _openIdMap.clear()
        _usersList.clear()
        for (document in collection.find()) {
            val user = document.ToObject(clazz)
            if(user != null) {
                _usersList.add(user)
                user.collection = collection
                user.openId?.let { _openIdMap.put(it, user) }
            }
        }
    }

    /**
     * 保存所有用户的数据，即对所有用户调用Save方法。
     */
    override fun save() {
        for(user in _usersList){
            user.save()
        }
    }

    override fun addUser(user: T) {
        super.addUser(user)
        user.save()
    }

    override fun removeUser(user: T): Boolean {
        val res = super.removeUser(user)
        if(res) {
            val doc: Document?
            if (user.openId != null) doc = Document().append("openId", user.openId)
            else if (user.name != null) doc = Document().append("name", user.name)
            else doc = null
            doc?.let { collection.deleteOne(it) }
        }
        return res
    }
}