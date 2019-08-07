package wxrbj

import account.OfficialAccount
import account.OfficialAccountServlet
import utils.MessageDBLogger
import javax.servlet.annotation.WebServlet
import components.MediaOperation
import utils.DBLogger

val RBJLOGGER: DBLogger = DBLogger("runningLog", DB)

object AccountRBJ: OfficialAccount(
    "XXXXX",
    RBJLOGGER,
    MessageDBLogger("messages", DB),
    UsersRBJ,
    CONFIG.TESTACCOUNTAPPID,
    CONFIG.TESTACCOUNTAPPSECRET)
{

    init {
        use(MediaOperation(DB.getCollection("medias")))
        use(RegisterReplyer)
    }

    override val users: UsersRBJ = UsersRBJ
}

@WebServlet("/")
class AccountRBJServlet: OfficialAccountServlet(RBJLOGGER){
    override val account: OfficialAccount = AccountRBJ

    override fun init() {
        super.init()
        account.logger?.info("Servlet Init SUCCESS")
    }
}