package wxrbj

import CONFIG
import cn.starrah.wxoabkd.account.OfficialAccount
import cn.starrah.wxoabkd.account.OfficialAccountServlet
import cn.starrah.wxoabkd.utils.MessageDBLogger
import javax.servlet.annotation.WebServlet
import cn.starrah.wxoabkd.components.MediaOperation
import cn.starrah.wxoabkd.utils.DBLogger
import java.io.PrintWriter
import java.io.StringWriter

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

@WebServlet("/")
class AccountRBJServlet: OfficialAccountServlet(AccountRBJ, RBJLOGGER){

    override fun init() {
        super.init()
        account.logger?.info("Servlet Init SUCCESS")
    }
}

val defExcepHdler = fun(t: Thread, e: Throwable){
    System.err.print("Exception in thread \"" + t.getName() + "\" ")
    e.printStackTrace(System.err)
    RBJLOGGER.error(StringWriter().apply { e.printStackTrace(PrintWriter(this)) }.buffer.toString())
}.apply { Thread.setDefaultUncaughtExceptionHandler(this);print("qwqwq") }