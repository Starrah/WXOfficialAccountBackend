package users

import com.alibaba.fastjson.JSON
import com.mongodb.client.model.ReplaceOptions
import org.bson.*
import utils.DB
import utils.assertBlank
import java.io.File
import java.util.*

fun makeUsers(csvFileName: String) {
    val collection = DB.getCollection("users")
    val scan = Scanner(File(csvFileName))
    println(File(csvFileName).absolutePath)
    while (scan.hasNextLine()) {
        val line = scan.nextLine()!!
        val strs = line.split(",")
        try {
            val gender = if (strs[4] == "男") Gender.BOY else Gender.GIRL
            val birthdayType =
                if (strs[10] == "公历") Birthday.BirthdayType.SOLAR else (if (strs[10] == "农历") Birthday.BirthdayType.LUNAR else Birthday.BirthdayType.UNKNOWN)
            val birthday = Birthday(birthdayType, strs[8])
            birthday.passed = strs[15].toInt()
            if (birthdayType == Birthday.BirthdayType.SOLAR) {
                birthday.solar = arrayOf(strs[11].toInt(), strs[12].toInt())
            } else if (birthdayType == Birthday.BirthdayType.LUNAR) {
                birthday.lunar = arrayOf(strs[13].toInt(), strs[14].toInt(), 0)
            }
            val user = User(
                strs[0].assertBlank(),
                strs[1],
                strs[2],
                gender,
                strs[3].toInt(),
                strs[5].assertBlank(),
                strs[6].assertBlank(),
                strs[7].assertBlank(),
                birthday
            )
            val json = JSON.toJSONString(user)
            val bson = Document.parse(json)
            collection.replaceOne(Document().apply {
                append("tHUId", strs[1])
            },
                bson,
                ReplaceOptions().upsert(true)
            )
        }catch (e: Exception){
            System.err.println(e.message)
        }
    }
    collection.createIndex(BsonDocument().apply {
        append("THUId", BsonInt32(1))
    })
    collection.createIndex(BsonDocument().apply {
        append("openId", BsonInt32(1))
    })
}

fun main(){
    makeUsers("users.csv")
}