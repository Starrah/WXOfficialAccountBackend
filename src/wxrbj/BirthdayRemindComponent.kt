package wxrbj

import cn.starrah.wxoabkd.account.AccountComponent
import cn.starrah.wxoabkd.account.OfficialAccount
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.runBlocking
import cn.starrah.wxoabkd.message.SingleBodyTemplateMessage
import cn.starrah.wxoabkd.utils.YMDFormat
import cn.starrah.wxoabkd.utils.omitTime
import wxTestAccount.AccountWXTest
import java.util.*
import kotlin.concurrent.schedule

object BirthdayRemindComponent: AccountComponent {

    val REMINDTIME = arrayOf(15, 20, 0)
    const val REMIND_BEFORE_DAYS = 2
    const val REMIND_INCLUDE_DAYS = 15

    const val MS_IN_DAY = 86400000

    override fun registerTo(account: OfficialAccount) {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, REMINDTIME[0])
            set(Calendar.MINUTE, REMINDTIME[1])
            set(Calendar.SECOND, REMINDTIME[2])
            if ((this.timeInMillis - Calendar.getInstance().timeInMillis) < 60000) set(Calendar.DAY_OF_MONTH,
                get(Calendar.DAY_OF_MONTH) + 1)
        }
        Timer().schedule(
            object: TimerTask() {
                override fun run() {
                    val nowTime = Date().omitTime().time
                    val includedList = mutableListOf<Pair<UserRBJ, String>>()
                    val remindList = mutableListOf<Pair<UserRBJ, String>>()
                    for (user in UsersRBJ.allUsers()) {
                        if (user.nextBirthday != null) {
                            val daysToGo = (Math.round((YMDFormat.parse(user.nextBirthday).time - nowTime).toFloat() / MS_IN_DAY))
                            if (daysToGo in 0..REMIND_INCLUDE_DAYS) includedList.add(Pair(user, user.nextBirthday!!.substring(4)))
                            if (daysToGo == REMIND_BEFORE_DAYS) remindList.add(Pair(user, user.nextBirthday!!.substring(4)))
                            if (daysToGo <= 0) user.calNextBirthday()
                        }
                    }
                    var str1 = "将在${REMIND_BEFORE_DAYS}天后过生日："
                    for ((user, day) in remindList) {
                        str1 += "$day${user.name},"
                    }
                    str1 += "\r\n"
                    var str2 = "将在${REMIND_INCLUDE_DAYS}天内过生日："
                    for ((user, day) in includedList) {
                        str2 += "$day${user.name},"
                    }
                    runBlocking {
                        for (testAccountUser in AccountWXTest.users.allUsers().filter { it.remark?.contains("软叭酱") == true }) {
                            AccountWXTest.sendTemplateMessage(
                                SingleBodyTemplateMessage(
                                    testAccountUser.openId!!,
                                    "4O9VJuHIVL7J3cdkMERn151DQhrvAaUSMXxz1mNWKjg",
                                    "$str1\r\n$str2"
                                )
                            )
                        }
                    }
                    AccountRBJ.logger?.info("birthday remind sent")
                }
            },
            Date(calendar.timeInMillis),
            MS_IN_DAY.toLong()
        )
        println(Date(calendar.timeInMillis))
    }

}