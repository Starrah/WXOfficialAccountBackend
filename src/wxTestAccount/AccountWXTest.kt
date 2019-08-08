package wxTestAccount

import account.OfficialAccount
import account.OfficialAccountServlet
import users.Users
import utils.DBLogger
import utils.MessageDBLogger
import wxrbj.MONGO_CLINET
import javax.servlet.annotation.WebServlet

val DBWXTest = MONGO_CLINET.getDatabase("testAccount")

object AccountWXTest: OfficialAccount(
    "XXXXX",
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