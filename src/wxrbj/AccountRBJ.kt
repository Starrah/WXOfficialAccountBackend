package wxrbj

import account.OfficialAccount
import account.OfficialAccountServlet
import utils.GlobalLogger
import utils.MessageDBLogger
import javax.servlet.annotation.WebServlet
import components.MediaOperation
import utils.DB

object AccountRBJ: OfficialAccount(
    "XXXXX",
    GlobalLogger,
    MessageDBLogger("messages"),
    UsersRBJ,
    CONFIG.TESTACCOUNTAPPID,
    CONFIG.TESTACCOUNTAPPSECRET)
{
//    override fun forceInits(): List<ForceInit> {
//        return listOf(HelloReplyer)
//    }

    init {
        use(MediaOperation(DB.getCollection("medias")))
        use(HelloReplyer)
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