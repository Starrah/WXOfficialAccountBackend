package users

import com.mongodb.client.MongoCollection
import org.bson.Document
import utils.Saveable
import utils.ToObject

/**
 * 用户信息管理的泛型类定义。
 *
 * 使用方法：对于一个特定群体的用户，继承XXXUser类后，再定义单例XXXUsers: Users<XXXUser>(collection, XXXUser::class.java)
 */
open class Users <T: User> (
    /** 被泛型实例化的类对象 */clazz: Class<T>
)
{
    protected val _usersList = ArrayList<T>()
    protected val _openIdMap = LinkedHashMap<String, T>()

    open fun addUser(user: T){
        _usersList.add(user)
        user.openId?.let { _openIdMap.put(it, user) }
    }

    open fun removeUser(user: T): Boolean{
        val res = _usersList.remove(user);
        val filtered = _openIdMap.filter { (_, value)->value === user }.keys
        val key = if(filtered.isEmpty()) null else filtered.first { true }
        if(key != null)_openIdMap.remove(key)
        return res
    }

    /**
     * 获取全部用户对应的数组。
     */
    fun allUsers(): ArrayList<T> {
        val newList = ArrayList<T>(_usersList)
        return newList
    }

    /**
     * 根据openId查找用户。
     * @param openId
     * @return User对象
     */
    fun byOpenId(openId: String): T? {
        return _openIdMap[openId]
    }
}