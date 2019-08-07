package utils

import account.MessageReplyer
import com.alibaba.fastjson.JSONObject
import com.mongodb.client.MongoCollection
import com.mongodb.client.MongoDatabase
import com.mongodb.client.model.CreateCollectionOptions
import message.Message
import message.TextMessage
import org.bson.Document
import java.io.File
import java.io.FileWriter
import java.nio.charset.Charset
import java.text.SimpleDateFormat
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
    fun log(message: String, level: LogLevel = LogLevel.INFO)

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

interface MessageLogger{
    fun logMessage(reqMessage: Message, resMessage: Message, replyer: MessageReplyer<*>?, userDoc: JSONObject? = null, RSType: MessageReceiveSendType = MessageReceiveSendType.RECEIVE, level: LogLevel = LogLevel.INFO)
}

open class DBLogger(collectionName: String, database: MongoDatabase) : Logger {

    val collection: MongoCollection<Document>

    init{
        val collections = database.listCollectionNames()
        if(!collections.contains(collectionName)) {
            database.createCollection(
                collectionName,
                CreateCollectionOptions().capped(true).maxDocuments(1000000).sizeInBytes(1000000000)
            )
        }
        collection = database.getCollection(collectionName)
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

open class MessageDBLogger(collectionName: String, database: MongoDatabase): DBLogger(collectionName, database), MessageLogger {
    override fun logMessage(reqMessage: Message, resMessage: Message, replyer: MessageReplyer<*>?, userDoc: JSONObject?, RSType: MessageReceiveSendType, level: LogLevel) {
        val document = Document()
        document["type"] = RSType.name
        userDoc?.let { document["user"] = it  }
        reqMessage.logDoc()?.let { document["received"] = it }
        replyer?.let { document["replyer"] = it.nameInLog }
        resMessage.logDoc()?.let { document["sent"] = it }
        log(document, level)
    }
}

val DefaultLogger: Logger by lazy{
    FileLogger(File("log.log"))
}

open class FileLogger(file: File): Logger{
    val writer: FileWriter
    val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
    init {
        writer = FileWriter(file, Charset.forName("UTF-8"), true)
    }
    override fun log(message: String, level: LogLevel) {
        writer.write(dateFormat.format(Date()) + "," + level.name + "," + message + "\r\n")
    }
}

open class MessageFileLogger(file: File): FileLogger(file), MessageLogger{
    override fun logMessage(
        reqMessage: Message,
        resMessage: Message,
        replyer: MessageReplyer<*>?,
        userDoc: JSONObject?,
        RSType: MessageReceiveSendType,
        level: LogLevel
    ) {
        val openId = (userDoc?.get("openId") as String?)?:""
        val name = (userDoc?.get("name") as String?)?:""
        val req = if(reqMessage is TextMessage) reqMessage.Content else reqMessage.MsgType.name
        val res = if(resMessage is TextMessage) resMessage.Content else resMessage.MsgType.name
        log("$openId,$name,${RSType.name},$req,${replyer?.nameInLog},$res", level)
    }
}