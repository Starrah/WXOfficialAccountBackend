package cn.starrah.wxoabkd.components

import cn.starrah.wxoabkd.account.AccountComponent
import cn.starrah.wxoabkd.account.OfficialAccount
import cn.starrah.wxoabkd.utils.Saveable
import com.alibaba.fastjson.JSONObject

class SaveLoadOperation(vararg val toLoadObjects: Saveable): AccountComponent, Saveable{

    lateinit var account: OfficialAccount

    fun saveAll(): JSONObject{
        var flag = 0
        for(obj in toLoadObjects) {
            obj.save()
            flag++
        }
        return JSONObject().apply { put("result", "saved ${flag} which are all of the specified Saveable objects") }
    }

    fun loadAll(): JSONObject{
        var flag = 0
        for(obj in toLoadObjects) {
            obj.load()
            flag++
        }
        return JSONObject().apply { put("result", "loaded ${flag} which are all of the specified Saveable objects") }
    }

    fun saveByName(className: String): JSONObject{
        var flag = 0
        val predicate = { it:Any -> it::class.java.name.substringAfterLast('.') == className }
        for(obj in toLoadObjects.filter(predicate) ) {
            obj.save()
            flag++
        }
        if(flag > 0){
            return JSONObject().apply { put("result", "saved ${flag} in specified Saveable objects") }
        }
        val users = account.users
        if(users is Saveable && predicate(users)){
            users.save()
            flag++
        }
        if(flag > 0){
            return JSONObject().apply { put("result", "saved ${flag} users") }
        }
        for(component in account.components){
            if(component is Saveable){
                component.save()
                flag++
            }
        }
        return JSONObject().apply { put("result", "saved ${flag} in all account components") }
    }

    fun loadByName(className: String): JSONObject{
        var flag = 0
        val predicate = { it:Any -> it::class.java.name.substringAfterLast('.') == className }
        for(obj in toLoadObjects.filter(predicate) ) {
            obj.load()
            flag++
        }
        if(flag > 0){
            return JSONObject().apply { put("result", "loaded ${flag} in specified Saveable objects") }
        }
        val users = account.users
        if(users is Saveable && predicate(users)){
            users.load()
            flag++
        }
        if(flag > 0){
            return JSONObject().apply { put("result", "loaded ${flag} users") }
        }
        for(component in account.components){
            if(component is Saveable){
                component.load()
                flag++
            }
        }
        return JSONObject().apply { put("result", "loaded ${flag} in all account components") }
    }

    fun saveAllInAccount(): JSONObject{
        var flag = 0
        val users = account.users
        if (users is Saveable){
            users.save()
            flag++
        }
        for(component in account.components){
            if(component is Saveable){
                component.save()
                flag++
            }
        }
        return JSONObject().apply { put("result", "saved ${flag} objects") }
    }

    fun loadAllInAccount(): JSONObject{
        var flag = 0
        val users = account.users
        if (users is Saveable){
            users.load()
            flag++
        }
        for(component in account.components){
            if(component is Saveable){
                component.load()
                flag++
            }
        }
        return JSONObject().apply { put("result", "loaded ${flag} objects") }
    }

    override fun load() {
        loadAll()
    }

    override fun save() {
        saveAll()
    }

    override fun registerTo(account: OfficialAccount) {
        this.account = account
        account.operation.registerHandler("saveByName"){
            val param = it["className"]
            if(param is String) {
                saveByName(param)
            }else{
                JSONObject().apply{ put("result", "fail: invalid parameter") }
            }
        }
        account.operation.registerHandler("loadByName"){
            val param = it["className"]
            if(param is String) {
                loadByName(param)
            }else{
                JSONObject().apply{ put("result", "fail: invalid parameter") }
            }
        }
        account.operation.registerHandler("loadAllInAccount"){
            loadAllInAccount()
        }
        account.operation.registerHandler("saveAllInAccount"){
            saveAllInAccount()
        }
        account.operation.registerHandler("loadAll"){
            loadAll()
        }
        account.operation.registerHandler("saveAll"){
            saveAll()
        }
        account.operation.registerHandler("load"){
            loadAll()
        }
        account.operation.registerHandler("save"){
            saveAll()
        }

    }

}