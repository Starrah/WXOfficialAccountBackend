package wxrbj

import account.OfficialAccount
import account.OfficialAccountServlet
import utils.MessageDBLogger
import javax.servlet.annotation.WebServlet
import components.MediaOperation
import utils.DBLogger
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

val RBJLOGGER: DBLogger = DBLogger("runningLog", DB)

object AccountRBJ: OfficialAccount(
    "XXXXX",
    RBJLOGGER,
    MessageDBLogger("messages", DB),
    UsersRBJ,
    CONFIG.RBJAPPID,
    CONFIG.RBJAPPSECRET)
{

    init {
        use(MediaOperation(DB.getCollection("medias")))
        use(RegisterReplyer)
        use(BirthdayRemindComponent)
    }

    override val users: UsersRBJ = UsersRBJ
}

@WebServlet("/rbj")
class AccountRBJServlet: OfficialAccountServlet(AccountRBJ, RBJLOGGER){

    override fun init() {
        super.init()
        account.logger?.info("Servlet Init SUCCESS")
    }
}