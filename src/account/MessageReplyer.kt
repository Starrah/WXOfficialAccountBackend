package account

import message.Message

abstract class MessageReplyer <T: OfficialAccount> (val account: T, val factor: Double, nameInLog: String? = null) {
    abstract fun reply(message: Message): Message?;

    val nameInLog: String

    init{
        this.nameInLog = nameInLog?:this::class.simpleName?:"(None)"
        account.dispatcher.registerDispatcher(this)
    }
}