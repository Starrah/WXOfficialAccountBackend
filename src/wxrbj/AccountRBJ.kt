package wxrbj

import account.OfficialAccount
import account.OfficialAccountServlet
import utils.GlobalLogger
import utils.MessageDBLogger
import javax.servlet.annotation.WebServlet

object AccountRBJ: OfficialAccount(
    "QWERT",
    GlobalLogger,
    MessageDBLogger("messages"),
    UsersRBJ,
    CONFIG.TESTACCOUNTAPPID,
    CONFIG.TESTACCOUNTAPPSECRET)
{
    override val users: UsersRBJ = UsersRBJ

    init {
        logger?.info("AccountRBJ Init SUCCESS")
    }
}

@WebServlet("/")
class AccountRBJServlet(): OfficialAccountServlet(){
    override val account: OfficialAccount = AccountRBJ

    override fun init() {
        super.init()
        account.logger?.info("Servlet Init SUCCESS")
    }
}