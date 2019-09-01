package wxrbj

import CONFIG
import cn.starrah.wxoabkd.account.OfficialAccount
import cn.starrah.wxoabkd.account.OfficialAccountServlet
import cn.starrah.wxoabkd.utils.MessageDBLogger
import javax.servlet.annotation.WebServlet
import cn.starrah.wxoabkd.components.MediaOperation
import cn.starrah.wxoabkd.utils.DBLogger

val RBJLOGGER: DBLogger = DBLogger("runningLog", DB)

object AccountRBJ: OfficialAccount(
    CONFIG.RBJTOKEN,
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