package wxTestAccount

import cn.starrah.wxoabkd.account.OfficialAccount
import cn.starrah.wxoabkd.account.OfficialAccountServlet
import cn.starrah.wxoabkd.users.Users
import cn.starrah.wxoabkd.utils.DBLogger
import cn.starrah.wxoabkd.utils.MessageDBLogger
import wxrbj.MONGO_CLINET
import javax.servlet.annotation.WebServlet
import javax.servlet.http.HttpServlet
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

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

@WebServlet("/test/")
class AccountWXTestServlet: OfficialAccountServlet(AccountWXTest, AccountWXTest.logger)

@WebServlet("/test/reloaduser")
class AccountRBJServlet: HttpServlet(){

    override fun doGet(req: HttpServletRequest, resp: HttpServletResponse) {
        UsersWXTest.load()
        resp.writer.write("success")
    }
}