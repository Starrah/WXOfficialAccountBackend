package cn.starrah.wxoabkd.components

import cn.starrah.wxoabkd.account.MessageReplyer
import cn.starrah.wxoabkd.account.OfficialAccount
import cn.starrah.wxoabkd.message.Message
import cn.starrah.wxoabkd.message.TextMessage

class SimpleTextReplyer<T: OfficialAccount>(account: T, var textToReply: String, factor: Double = 0.9, nameInLog: String? = null):
    MessageReplyer<T>(account, factor, nameInLog) {

    override fun reply(message: Message): Message? {
        return TextMessage(message.FromUserName, message.ToUserName, Content = textToReply)
    }

}