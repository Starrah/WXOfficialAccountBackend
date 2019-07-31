package utils

import CONFIG
import com.mongodb.MongoClient
import com.mongodb.MongoClientURI
import com.mongodb.client.MongoDatabase

val DB = fun (): MongoDatabase {
    val uri = MongoClientURI(CONFIG.MONGODB_URI)
    val mongoClient = MongoClient(uri)
    return mongoClient.getDatabase(CONFIG.MONGODB_DATABASE_NAME)
}();