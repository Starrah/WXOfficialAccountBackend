package wxrbj

import account.OfficialAccount
import account.OfficialAccountServlet
import utils.ForceInit
import utils.GlobalLogger
import utils.MessageDBLogger
import javax.servlet.annotation.WebServlet

object AccountRBJ: OfficialAccount(
    "XXXXX",
    GlobalLogger,
    MessageDBLogger("messages"),
    UsersRBJ,
    CONFIG.TESTACCOUNTAPPID,
    CONFIG.TESTACCOUNTAPPSECRET)
{
    override fun forceInits(): List<ForceInit> {
        return listOf(HelloReplyer)
    }

    override val users: UsersRBJ = UsersRBJ
}

@WebServlet("/")
class AccountRBJServlet(): OfficialAccountServlet(){
    override val account: OfficialAccount = AccountRBJ

    override fun init() {
        super.init()
        account.logger?.info("Servlet Init SUCCESS")
    }
}