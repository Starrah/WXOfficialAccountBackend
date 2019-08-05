package account

import message.Message

abstract class MessageReplyer <T: OfficialAccount> (val account: T, val factor: Double, nameInLog: String? = null): AccountComponent {
    abstract fun reply(message: Message): Message?;

    val nameInLog: String

    init{
        this.nameInLog = nameInLog?:this::class.simpleName?:"(None)"
    }

    override fun registerTo(account: OfficialAccount) {
        account.dispatcher.registerDispatcher(this)
        account.logger?.info("Replyer ${this.nameInLog} Register SUCCESS")
    }

    fun forceInit(): Int{
        return 1
    }
}