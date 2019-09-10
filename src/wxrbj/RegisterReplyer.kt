package wxrbj

import cn.starrah.wxoabkd.account.MessageReplyer
import cn.starrah.wxoabkd.message.Message
import cn.starrah.wxoabkd.message.TextMessage
import java.util.*

object RegisterReplyer: MessageReplyer<AccountRBJ>(AccountRBJ, 80.0){
    override fun reply(message: Message): Message? {
        if(message is TextMessage && message.Content?.substring(0,2) == "注册"){
            val str = message.Content?.substring(2)?:""
            val scan = Scanner(str)
            val keyWord = if(scan.hasNext())scan.next()!! else return TextMessage(message.FromUserName, message.ToUserName, "请输入合法的姓名")
            val user = AccountRBJ.users.byName(keyWord)?:return TextMessage(message.FromUserName, message.ToUserName, "在数据库中没有找到您的名字。")
            if(user.openId != null){

                if(user.openId == message.FromUserName)return TextMessage(message.FromUserName, message.ToUserName, "当前账号已注册并绑定在此微信号上，无需重复操作。")
                else return TextMessage(message.FromUserName, message.ToUserName, "您的账号已注册并绑定在了另一个微信号上。如非您本人操作，或需要更改绑定过的微信号，请与管理员联系。")
            }
            user.openId = message.FromUserName
            user.save()
            return TextMessage(message.FromUserName, message.ToUserName, "您已注册成功，姓名：${user.name}。\r\n" +
                    "以下是您的生日信息，亦请查看，如有问题请与管理员联系：：\r\n" +
                    "生日类型：${when(user.birthday.type){
                        Birthday.BirthdayType.UNKNOWN->"未选择"
                        Birthday.BirthdayType.SOLAR->"公历${user.birthday.solar?.get(0)}月${user.birthday.solar?.get(1)}日"
                        Birthday.BirthdayType.LUNAR->"农历${user.birthday.lunar?.get(0)}月${user.birthday.lunar?.get(1)}日"
                    }}，\r\n" +
                    "下一生日：${user.nextBirthday}。")
        }else return null
    }

}