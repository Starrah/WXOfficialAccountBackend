package account

import message.Message
import utils.ForceInit

abstract class MessageReplyer <T: OfficialAccount> (val account: T, val factor: Double, nameInLog: String? = null): ForceInit {
    abstract fun reply(message: Message): Message?;

    val nameInLog: String

    init{
        this.nameInLog = nameInLog?:this::class.simpleName?:"(None)"
        account.dispatcher.registerDispatcher(this)
        account.logger?.info("Replyer ${this.nameInLog} Init SUCCESS")
    }

    fun forceInit(): Int{
        return 1
    }
}