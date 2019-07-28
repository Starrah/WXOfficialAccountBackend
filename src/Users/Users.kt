package Users

import Utils.CalendarUtil
import java.util.*

enum class Gender{
    GIRL,
    BOY
}

enum class BirthdayType{
    UNKNOWN,
    SOLAR,
    LUNAR,
}

class User(val id: String,
           val name: String,
           val gender: Gender,
           val classNo: Int,
           var room: String?,
           var phone: String?,
           var email: String?,
           var birthdayType: BirthdayType,
           val standardBirthday: String,
           var solarMonth: Int?,
           var solarDay: Int?,
           var lunarMonth: Int?,
           var lunarDay: Int?,
           var lunarLeap: Boolean = false) {

    var openId: String? = null

    var nextBirthday: Calendar

    init {
        val standardDate = Calendar.getInstance().apply {
            set(standardBirthday.substring(0,4).toInt(), standardBirthday.substring(4,6).toInt(), standardBirthday.substring(6,8).toInt())
        }!!
        val birthYear = standardDate[Calendar.YEAR]
        val calcuBirthday = _calBirthdayInYear(birthYear)
        if(calcuBirthday != standardDate){
            throw Exception("assertion birthday fail!$name")
        }
        nextBirthday = _calNextBirthday()
    }

    private fun _calNextBirthday(): Calendar{
        val now = Calendar.getInstance()
        var thisYearDate = _calBirthdayInYear(now[Calendar.YEAR])
        if(thisYearDate < now){
            thisYearDate = _calBirthdayInYear(now[Calendar.YEAR] + 1)
        }
        return thisYearDate
    }

    private fun _calBirthdayInYear(year: Int): Calendar{
        when(birthdayType){
            BirthdayType.UNKNOWN -> {
                val calendar = Calendar.getInstance()
                calendar.set(year, standardBirthday.substring(4,6).toInt(), standardBirthday.substring(6,8).toInt())
                return calendar
            }
            BirthdayType.SOLAR -> {
                val calendar = Calendar.getInstance()
                calendar.set(year, solarMonth!!, solarDay!!)
                return calendar
            }
            BirthdayType.LUNAR -> {
                val solarStr = CalendarUtil.lunarToSolar(year.toString()+String.format("%02d",lunarMonth)+String.format("%02d", lunarDay), lunarLeap)
                return Calendar.getInstance().apply {
                    set(solarStr.substring(0,4).toInt(), solarStr.substring(4,6).toInt(),solarStr.substring(6,8).toInt())
                }
            }
        }
    }
}