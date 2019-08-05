package account

/**
 * 不严谨的注释 过后再改
 *
 * 各种Replyer、Operation、Script等都实现此接口；接口内有registerTo(account)函数，组件负责自行实现把自己注册给account的方法；
 *
 * OfficialAccount.use注册组件从而使用它，并定义有components、getComponentByClass等方法获取挂载在某一Account上的组件。
 */
interface AccountComponent{
    fun registerTo(account: OfficialAccount);
}