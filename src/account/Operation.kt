package account

import com.alibaba.fastjson.JSON
import com.alibaba.fastjson.JSONObject

typealias OperationHandler = (JSONObject) -> JSONObject?

class Operation(val account: OfficialAccount){
    private val handlers =  mutableMapOf<String, MutableList<OperationHandler>>()

    fun registerHandler(oper: String, handler: OperationHandler) {
        handlersOfOper(oper).add(handler)
    }

    fun handlersOfOper(oper: String): MutableList<OperationHandler>{
        if(handlers[oper] == null){
            handlers[oper] = ArrayList<OperationHandler>()
        }
        return handlers[oper]!!
    }

    private fun invokeHandlers(oper: String, para: JSONObject): JSONObject?{
        var res: JSONObject? = JSONObject().apply { plusAssign(mapOf("errmsg" to "No such operation")) }
        var exception: Exception? = null
        for((index, handler) in handlersOfOper(oper).withIndex()){
            try {
                if (index == 0) res = handler(para)
                else handler(para)
            }catch (e: Exception){
                if (index == 0) exception = e
            }
        }
        if(exception != null)throw exception
        return res
    }

    fun reqArrive(query: Map<String, String>, body: String?): JSONObject?{
        val oper = query["oper"]
        if(oper != null){
            val jsonObj = body?.let { JSON.parseObject(it) } ?: JSONObject()
            jsonObj += query
            return invokeHandlers(oper, jsonObj)
        }else return null
    }

    companion object {
        fun assertIsOperation(query: Map<String, String>): Boolean {
            return query["oper"] != null
        }
    }

    fun verifyTokenThrow(req: JSONObject){
        if(!verifyTokenBoolean(req))throw Exception("token错误！")
    }

    fun verifyTokenBoolean(req: JSONObject): Boolean{
        val temp = req["token"]
        return temp != null && temp is String && temp == account.operToken
    }

}