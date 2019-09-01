package cn.starrah.wxoabkd.utils


/**
 * 描述可被保存对象的接口。
 * 其他工具可以通过此接口实现对数据的定时保存等功能。
 *
 * 保存有很多种实现方式，如数据库、文件等，由相关类自行决定，接口不做要求。
 *
 * 包括Save方法（必须实现）、Load方法（可选实现）两种，均为无参、Unit返回的函数。
 */
public interface Saveable{
    fun save();

    fun load(){

    }
}