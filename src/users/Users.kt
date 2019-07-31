package users

import utils.*
import java.util.*

enum class Gender{
    GIRL,
    BOY,
}

class Birthday(var type: BirthdayType = BirthdayType.UNKNOWN,
               val standard: String) {
    enum class BirthdayType{
        UNKNOWN,
        SOLAR,
        LUNAR,
    }

    var solar: Array<Int>? = null
    var lunar: Array<Int>? = null
    var passed = 0
}

class User(var openId: String? = null,
           val tHUId: String,
           val name: String,
           val gender: Gender,
           val classNo: Int,
           var room: String?,
           var phone: String?,
           var email: String?,
           var birthday: Birthday) {

    var nextBirthday: String? = null

    init {
        if(birthday.standard != "") {
            val standardDate = YMDFormat.parse(birthday.standard)!!;
            val birthYear = standardDate.year4bit
            val calcuBirthday = _calBirthdayInYear(birthYear)
            if(!calcuBirthday.sameDay(standardDate)){
                //throw Exception("assertion birthday fail!$name${birthday.standard}${birthday.type}${birthday.solar?.let { "" + it[0] + " " + it[1] }?: ""}${birthday.lunar?.let { "" + it[0] + " " + it[1] }?: ""}")
            }
        }
        if(birthday.standard != "" || birthday.type != Birthday.BirthdayType.UNKNOWN) _calNextBirthday()
    }

    private fun _calNextBirthday(): Date{
        val now = Date()
        var thisYearDate = _calBirthdayInYear(now.year4bit)
        if(thisYearDate < now){
            thisYearDate = _calBirthdayInYear(now.year4bit + 1)
        }
        nextBirthday = YMDFormat.format(thisYearDate)
        return thisYearDate
    }

    private fun _calBirthdayInYear(year: Int): Date{
        var dateStr: String = when(birthday.type){
            Birthday.BirthdayType.UNKNOWN -> {
                "" + year + birthday.standard.substring(4,8)
            }
            Birthday.BirthdayType.SOLAR -> {
                "" + year + String.format("%02d", birthday.solar!![0]) + String.format("%02d", birthday.solar!![1])
            }
            Birthday.BirthdayType.LUNAR -> {
                //解决一种极端情况：如2000年腊月廿七转换出的实际是2001年1月某日，这与获得此人在2000年的生日的逻辑相矛盾；
                val lunarStr = "" + year + String.format("%02d", birthday.lunar!![0]) + String.format("%02d", birthday.lunar!![1])
                var tempSolrStr = CalendarUtil.lunarToSolar(lunarStr, birthday.lunar!![2] > 0)!!
                if (YMDFormat.parse(tempSolrStr).year4bit > year) {
                    val lunarStr = "" + (year - 1) + String.format("%02d", birthday.lunar!![0]) + String.format("%02d", birthday.lunar!![1])
                    val tempSolrStr2 = CalendarUtil.lunarToSolar(lunarStr, birthday.lunar!![2] > 0)!!
                    //解决另一种极端情况：在有闰年时，某一公历年份内可能不存在某一农历
                    if (YMDFormat.parse(tempSolrStr2).year4bit == year) tempSolrStr = tempSolrStr2;
                }
                tempSolrStr
            }
        }
        return YMDFormat.parse(dateStr)
    }
}

object Users{
    init {
        val collection = DB.getCollection("users");

    }
}