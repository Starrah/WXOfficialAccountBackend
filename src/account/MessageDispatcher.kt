package account

import message.Message
import message.NoMessage
import org.bson.Document
import java.lang.Exception

open class MessageDispatcher(val account: OfficialAccount){
    open fun dispatchMessage(reqMessage: Message): Message{
        var resMessage: Message? = null
        var chosenReplyer: MessageReplyer<*>? = null
        for(replyer in replyers){
            val reply = replyer.reply(reqMessage)
            if(reply != null){
                resMessage = reply
                chosenReplyer = replyer
                break
            }
        }
        if(resMessage == null)resMessage = NoMessage(reqMessage.FromUserName, reqMessage.ToUserName)
        logMessage(reqMessage, resMessage, chosenReplyer)
        return resMessage
    }

    protected open fun logMessage(reqMessage: Message, resMessage: Message, replyer: MessageReplyer<*>?){
        val userDoc = account.users?.byOpenId(reqMessage.FromUserName)?.logDoc()?: (Document().append("openId", reqMessage.FromUserName)!!)
        account.msgLogger?.logMessage(reqMessage, resMessage, replyer, userDoc)
    }

    private val replyers = ArrayList<MessageReplyer<*>>()

    fun registerDispatcher(replyer: MessageReplyer<*>){
        if(replyer.account !== account)throw Exception("被添加的消息回复器必须和消息分发器归属于同一个公众号实例！")
        replyers.add(replyer)
        replyers.sortByDescending { it.factor }
    }
}