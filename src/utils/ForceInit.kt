package utils

/**
 * 由于tomcat的糟糕特性，一个类只有在被显式引用的时候才会被加载。为解决此问题，实现了此接口的类可以传入OfficialAccount类的构造函数，从而被显式的加载其类。
 */
interface ForceInit {}