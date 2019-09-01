package wxrbj

import CONFIG
import com.mongodb.MongoClient
import com.mongodb.MongoClientURI
import com.mongodb.client.MongoDatabase


val MONGO_CLINET = MongoClient(MongoClientURI(CONFIG.MONGODB_URI))

/**
 * 基础数据库对象，保存着到程序全局数据库的引用。
 * 可由此数据库对象获得集合，即可进行相关数据库操作。
 *
 * 例：val collection = DB.getCollection("test")//获得集合
 *
 * collection.insertOne(Document().apply { append("test", 0) })//插入文档
 */
val DB: MongoDatabase = MONGO_CLINET.getDatabase(CONFIG.MONGODB_DATABASE_NAME)