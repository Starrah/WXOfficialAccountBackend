package wxTestAccount

import cn.starrah.wxoabkd.account.OfficialAccount
import cn.starrah.wxoabkd.account.OfficialAccountServlet
import cn.starrah.wxoabkd.users.Users
import cn.starrah.wxoabkd.utils.DBLogger
import cn.starrah.wxoabkd.utils.MessageDBLogger
import wxrbj.MONGO_CLINET
import javax.servlet.annotation.WebServlet

val DBWXTest = MONGO_CLINET.getDatabase("testAccount")

object AccountWXTest: OfficialAccount(
    CONFIG.TESTACCOUNTTOKEN,
    DBLogger("runningLogs", DBWXTest),
    MessageDBLogger("messages", DBWXTest),
    UsersWXTest,
    CONFIG.TESTACCOUNTAPPID,
    CONFIG.TESTACCOUNTAPPSECRET
) {
    override val users: UsersWXTest = UsersWXTest
}

@WebServlet("/test")
class AccountWXTestServlet: OfficialAccountServlet(AccountWXTest, AccountWXTest.logger)