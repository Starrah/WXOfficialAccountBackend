package wxTestAccount

import com.alibaba.fastjson.JSON
import com.mongodb.client.MongoCollection
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.bson.Document
import users.DBUser
import users.DBUsers
import utils.WXAPI
import utils.request

class APIUserInfoBean(
    val subsribe: Int = 0,
    val openId: String? = null,
    val nickName: String? = null,
    val sex : Int? = null,
    val language: String? = null,
    val city: String? = null,
    val province: String? = null,
    val country: String? = null,
    val headimgurl: String? = null,
    val subsribe_time: Int? = null,
    val unionid: String? = null,
    val remark: String? = null,
    val groupid: Int? = null,
    val tagid_list: List<Int>? = null,
    val subscribe_scene: String? = null,
    val qr_scene: Int? = null,
    val qr_scene_str: Int? = null
): WXAPI(){}

class UserWXTest(openId: String, collection: MongoCollection<Document>? = null, remark: String? = null): DBUser(openId, null, collection) {
    var remark: String? = remark

    fun updateNameWithWXAPI(){
        if(name == null) {
            val API_URL1 = "https://api.weixin.qq.com/cgi-bin/user/info?access_token="
            val API_URL2 = "&openid="
            val API_URL3 = "&lang=zh_CN"
            GlobalScope.launch {
                val res = request(API_URL1 + AccountWXTest.accessToken + API_URL2 + openId + API_URL3, responseClass = APIUserInfoBean::class.java)
                name = res.resObj?.nickName?:name
                println(JSON.toJSONString(this@UserWXTest))
                this@UserWXTest.save()
            }
        }
    }
}

object UsersWXTest : DBUsers<UserWXTest>(DBWXTest.getCollection("users"), UserWXTest::class.java){
    override fun load() {
        super.load()
        for(user in allUsers()){
            user.updateNameWithWXAPI()
        }
    }
}
