@file:Suppress("RedundantVisibilityModifier", "unused")

package wxrbj

import com.mongodb.client.MongoCollection
import org.bson.Document
import users.DBUser
import users.DBUsers
import utils.*
import java.lang.Exception
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.LinkedHashMap

/**
 * 性别
 */
enum class Gender{
    GIRL,
    BOY,
}

/**
 * 描述生日信息的对象，可被BSON序列化。
 *
 * 包含生日类型、标准生日（从身份证号中提取）、上报的公历生日、农历生日、生日已过次数等信息。
 */
class Birthday(var type: BirthdayType = BirthdayType.UNKNOWN,
               val standard: String)
{

    /**
     * 生日类型
     */
    enum class BirthdayType{
        /** 未知 */UNKNOWN,
        /** 公历 */SOLAR,
        /** 农历 */LUNAR,
    }

    /**
     * 长为2的数组，依次存储上报的公历出生月和日
     */
    var solar: Array<Int>? = null

    /**
     * 长为3的数组，下标0和1依次存储上报的农历出生月和日，下标2存储是否为农历闰月（1表示是，0表示否）
     */
    var lunar: Array<Int>? = null

    /**
     * 生日已过次数
     */
    var passed = 0
}


public class UserRBJ(
    /** openId，可从消息中获取 */openId: String? = null,
    /** 学号 */val tHUId: String,
    /** 姓名 */name: String,
    /** 性别 */val gender: Gender,
    /** 班号（整型，例如81/82/83） */val classNo: Int,
    /** 宿舍（字符串） */var room: String?,
    /** 手机号（字符串） */var phone: String?,
    /** 邮箱 */var email: String?,
    /** 生日信息 */var birthday: Birthday,
    /**  数据库集合对象 */collection: MongoCollection<Document>?
): DBUser(openId, name, collection)
{

    /**
     * 下一个生日；出于便于阅读考量，是八位yyyyMMdd数字；
     *
     * calNextBirthday方法会自动计算并更新这里的值。
     */
    var nextBirthday: String? = null

    /**
     * 用于反序列化的空构造函数
     */
    public constructor(): this(null, "", "",
        Gender.BOY, 0, null, null, null,
        Birthday(Birthday.BirthdayType.UNKNOWN, ""), null
    ) {}

    init {
        if(birthday.standard != "") {
            val standardDate = YMDFormat.parse(birthday.standard)!!
            val birthYear = standardDate.year4bit
            val calcuBirthday = calBirthdayInYear(birthYear)
            if(!calcuBirthday.sameDay(standardDate)){
                //throw Exception("assertion birthday fail!$name${birthday.standard}${birthday.type}${birthday.solar?.let { "" + it[0] + " " + it[1] }?: ""}${birthday.lunar?.let { "" + it[0] + " " + it[1] }?: ""}")
            }
        }
        if(birthday.standard != "" || birthday.type != Birthday.BirthdayType.UNKNOWN) {
            try {
                calNextBirthday()
            }catch (e: Exception){
                System.err.println(e)
            }
        }
    }

    /**
     * 计算下一个生日，并转为字符串格式保存在nextBirthday中，和转为Date对象返回。
     * @return 对应于下一个生日日期的字符串对象
     */
    public fun calNextBirthday(): Date{
        val now = Date()
        var thisYearDate = calBirthdayInYear(now.year4bit)
        if(thisYearDate < now){
            thisYearDate = calBirthdayInYear(now.year4bit + 1)
        }
        nextBirthday = YMDFormat.format(thisYearDate)
        return thisYearDate
    }

    /**
     * 计算该人在公历某一年的生日日期。
     *
     * @param year 年份，四位整数
     * @return 在该年的生日日期
     */
    public fun calBirthdayInYear(year: Int): Date{
        val dateStr: String = when(birthday.type){
            Birthday.BirthdayType.UNKNOWN -> {
                "" + year + birthday.standard.substring(4,8)
            }
            Birthday.BirthdayType.SOLAR -> {
                "" + year + String.format("%02d", birthday.solar!![0]) + String.format("%02d", birthday.solar!![1])
            }
            Birthday.BirthdayType.LUNAR -> {
                //解决一种极端情况：如2000年腊月廿七转换出的实际是2001年1月某日，这与获得此人在2000年的生日的逻辑相矛盾；
                val lunarStr = "" + year + String.format("%02d", birthday.lunar!![0]) + String.format(
                    "%02d",
                    birthday.lunar!![1]
                )
                var solarStr = CalendarUtil.lunarToSolar(lunarStr, birthday.lunar!![2] > 0)!!
                if (YMDFormat.parse(solarStr).year4bit > year) {
                    val lunarStr2 = "" + (year - 1) + String.format("%02d", birthday.lunar!![0]) + String.format(
                        "%02d",
                        birthday.lunar!![1]
                    )
                    val tempSolrStr2 = CalendarUtil.lunarToSolar(lunarStr2, birthday.lunar!![2] > 0)!!
                    //解决另一种极端情况：在有闰年时，某一公历年份内可能不存在某一农历
                    if (YMDFormat.parse(tempSolrStr2).year4bit == year) solarStr = tempSolrStr2
                }
                solarStr
            }
        }
        return YMDFormat.parse(dateStr)
    }
}


/**
 * UsersRBJ单例，管理所有的UserRBJ信息。
 *
 * 在该单例构造时自动从数据库中获取并实例化许多UserRBJ对象（DBUser泛型类提供的功能）。
 *
 * 利用该单例可以通过openId、学号、姓名等方法查找特定用户，亦可获得全部或满足一定条件的所有用户。
 */
public object UsersRBJ: DBUsers<UserRBJ>(DB.getCollection("users"), UserRBJ::class.java) {
    private val _tHUIdMap = LinkedHashMap<String, UserRBJ>()
    private val _nameMap = LinkedHashMap<String, UserRBJ>()

    init {
        load()
    }

    override fun load() {
        super.load()
        _tHUIdMap.clear()
        _nameMap.clear()
        for(user in _usersList){
            _tHUIdMap[user.tHUId] = user
            user.name?.let { _nameMap.put(it, user) }
        }
        RBJLOGGER.info("UsersRBJ Init SUCCESS: users ${_usersList.size}")
    }

    /**
     * 获取部分用户。目前包括班级和性别两个条件，查找满足所有查询条件的用户。
     * @param classNo: Array<Int>? 值为null时，表示查找所有班级的用户；否则，只查找classNo列表中包含的用户。
     * @param gender: Gender 性别。值为null时表示所有性别，否则传入枚举类型表示只查找特定性别。
     * @return ArrayList<UserRBJ> 满足所有条件的用户的列表。
     */
    public fun filterUsers(classNo: List<Int>? = null, gender: Gender? = null): ArrayList<UserRBJ> {
        val newList = ArrayList<UserRBJ>()
        for(user in _usersList){
            if ((classNo?.contains(user.classNo) != false) && (gender?.let { it === user.gender } != false)) {
                newList.add(user)
            }
        }
        return newList
    }

    /**
     * 根据学号查找用户。
     * @param tHUId 学号（字符串）
     * @return User对象
     */
    public fun byTHUId(tHUId: String): UserRBJ? {
        return _tHUIdMap[tHUId]
    }

    /**
     * 根据姓名查找用户。
     * @param name 姓名
     * @return User对象
     */
    public fun byName(name: String): UserRBJ? {
        return _nameMap[name]
    }

}