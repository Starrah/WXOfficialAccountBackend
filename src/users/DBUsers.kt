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
): Saveable
{
    protected val _usersList = ArrayList<T>()
    private val _openIdMap = LinkedHashMap<String, T>()

    init {
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
     * 获取全部用户对应的数组。
     */
    fun allUsers(): ArrayList<T> {
        val newList = ArrayList<T>(_usersList)
        return newList
    }

    /**
     * 保存所有用户的数据，即对所有用户调用Save方法。
     */
    override fun save() {
        for(user in _usersList){
            user.save()
        }
    }

    /**
     * 根据openId查找用户。
     * @param openId
     * @return User对象
     */
    public fun byOpenId(openId: String): T? {
        return _openIdMap[openId]
    }
}