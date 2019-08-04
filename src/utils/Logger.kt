package utils

import account.MessageReplyer
import com.mongodb.client.MongoCollection
import com.mongodb.client.model.CreateCollectionOptions
import message.Message
import org.bson.Document
import users.DBUser
import java.util.*

enum class LogLevel {
    ERROR,
    WARNING,
    INFO
}

/**
 * 日志记录器接口
 */
interface Logger {
    fun log(message: String, level: LogLevel = LogLevel.INFO);

    fun error(message: String){
        log(message, LogLevel.ERROR)
    }

    fun warn(message: String){
        log(message, LogLevel.WARNING)
    }

    fun info(message: String){
        log(message, LogLevel.INFO)
    }
}

open class DBLogger(collectionName: String) : Logger {

    val collection: MongoCollection<Document>

    init{
        val collections = DB.listCollectionNames()
        if(!collections.contains(collectionName)) {
            DB.createCollection(
                collectionName,
                CreateCollectionOptions().capped(true).maxDocuments(1000000).sizeInBytes(1000000000)
            )
        }
        collection = DB.getCollection(collectionName)
    }

    protected open fun attachTimeLevelMeta(document: Document, level: LogLevel){
        document.append("time", Date()).append("level", level.name)
    }

    override fun log(message: String, level: LogLevel) {
        val document = Document()
        attachTimeLevelMeta(document, level)
        document.append("message", message)
        collection.insertOne(document)
    }

    open fun log(document: Document, level: LogLevel = LogLevel.INFO) {
        attachTimeLevelMeta(document, level)
        collection.insertOne(document)
    }

    open fun error(document: Document){
        log(document, LogLevel.ERROR)
    }

    open fun warn(document: Document){
        log(document, LogLevel.WARNING)
    }

    open fun info(document: Document){
        log(document, LogLevel.INFO)
    }
}

enum class MessageReceiveSendType{
    RECEIVE,
    SEND,
}

open class MessageDBLogger(collectionName: String): DBLogger(collectionName) {
    fun logMessage(reqMessage: Message, resMessage: Message, replyer: MessageReplyer<*>?, userDoc: Document? = null, RSType: MessageReceiveSendType = MessageReceiveSendType.RECEIVE, level: LogLevel = LogLevel.INFO) {
        val document = Document()
        document["type"] = RSType.name
        userDoc?.let { document["user"] = it  }
        reqMessage.logDoc()?.let { document["received"] = it }
        replyer?.let { document["replyer"] = it.nameInLog }
        resMessage.logDoc()?.let { document["sent"] = it }
        log(document, level)
    }
}

val GlobalLogger = DBLogger("runningLogs")