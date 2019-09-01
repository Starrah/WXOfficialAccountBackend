package cn.starrah.wxoabkd.account

import com.alibaba.fastjson.JSON
import com.alibaba.fastjson.JSONObject
import cn.starrah.wxoabkd.message.Message
import org.apache.commons.codec.digest.DigestUtils
import cn.starrah.wxoabkd.utils.Logger
import cn.starrah.wxoabkd.utils.stringToQuery
import java.io.PrintWriter
import java.io.StringWriter
import javax.servlet.http.HttpServlet
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

abstract class OfficialAccountServlet(val account: OfficialAccount, var logger: Logger?): HttpServlet() {

    override fun doGet(req: HttpServletRequest, resp: HttpServletResponse) {
        try {
            req.characterEncoding = "UTF-8"
            resp.characterEncoding = "UTF-8"
            val query = stringToQuery(req.queryString)
            if (Operation.assertIsOperation(query)) {
                try {
                    val res = account.operation.reqArrive(query, null)
                    res?.let { resp.writer.print(it.toJSONString()) }
                    return
                }catch (e: Exception){
                    resp.sendError(400, e.message)
                    return
                }
            }

            if (query["echostr"] != null) {
                val verifyRes = verifyWXSignature(query)
                if (verifyRes) {
                    account.logger?.info("Connect with Wechat server SUCCESS")
                } else {
                    account.logger?.error("Connect With Wechat Server FAILED, due to signature verification failure")
                    resp.status = 403
                }
                resp.writer.println(if (verifyRes) query["echostr"] else "0")
                return
            }

            resp.sendError(400)
        }catch (e: Exception){
            logger?.error(StringWriter().apply { e.printStackTrace(PrintWriter(this)) }.buffer.toString())
            throw e
        }
    }

    override fun doPost(req: HttpServletRequest, resp: HttpServletResponse) {
        try {
            req.characterEncoding = "UTF-8"
            resp.characterEncoding = "UTF-8"
            val query = stringToQuery(req.queryString)
            val body: String = req.reader.readText()
            if(Operation.assertIsOperation(query)){
                try {
                    val res = account.operation.reqArrive(query, null)
                    res?.let { resp.writer.print(it.toJSONString()) }
                    return
                }catch (e: Exception){
                    resp.sendError(400, e.message)
                    return
                }
            }

            if(query["signature"] != null) {
                if (!verifyWXSignature(query)) {
                    account.logger?.warn("Signature verify FAILED, query: ${JSON.toJSONString(query)}, body: ${body}")
                    resp.status = 403
                    resp.writer.println(JSONObject().apply { put("errmag", "签名验证失败！") }.toJSONString())
                    return
                }
                val reqMessage = Message.parseFromXML(body);
                val resMessage = account.dispatcher.dispatchMessage(reqMessage)
                resp.writer.println(resMessage.toXML())
                return
            }

            resp.sendError(400)
        }catch (e: Exception){
            logger?.error(StringWriter().apply { e.printStackTrace(PrintWriter(this)) }.buffer.toString())
            throw e
        }
    }

    private fun verifyWXSignature(query: Map<String, String>): Boolean{
        try {
            val arrString = arrayOf(account.token, query["timestamp"]!!, query["nonce"]!!)
            arrString.sort()
            val resultString = arrString[0] + arrString[1] + arrString[2]
            val hexSha1 = DigestUtils.sha1Hex(resultString)!!
            return hexSha1 == query["signature"]
        }catch (e: Exception){
            return false
        }
    }
}