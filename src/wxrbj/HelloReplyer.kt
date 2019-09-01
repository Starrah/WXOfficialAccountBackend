package wxrbj

import cn.starrah.wxoabkd.account.MessageReplyer
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import cn.starrah.wxoabkd.message.Message
import cn.starrah.wxoabkd.message.TextMessage
import cn.starrah.wxoabkd.message.SingleBodyTemplateMessage


object HelloReplyer: MessageReplyer<AccountRBJ>(AccountRBJ, 1.0){
    override fun reply(message: Message): Message? {
        val resmessage = TextMessage(message.FromUserName, message.ToUserName, Content = "你好")
        val temp = SingleBodyTemplateMessage(message.FromUserName, "4O9VJuHIVL7J3cdkMERn151DQhrvAaUSMXxz1mNWKjg", "你好\r\n你好好好")
        val str = temp.toTemplateJSON()
        GlobalScope.launch {
            try {
                delay(10000)
                println("startsend")
                account.sendTemplateMessage(temp)
                println("endsend")
            }catch (e: Exception){
                e.printStackTrace()
                throw e
            }
        }
        arrayOf<Int>().forEach{it.toString()}
        return resmessage
    }

}