package components

import account.MessageReplyer
import account.OfficialAccount
import message.Message
import message.TextMessage

class SimpleTextReplyer<T: OfficialAccount>(account: T, var textToReply: String, factor: Double = 0.9, nameInLog: String? = null):
    MessageReplyer<T>(account, factor, nameInLog) {

    override fun reply(message: Message): Message? {
        return TextMessage(message.FromUserName, message.ToUserName, Content = textToReply)
    }

}