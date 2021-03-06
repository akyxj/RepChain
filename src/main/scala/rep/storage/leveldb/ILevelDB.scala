package rep.storage.leveldb

import scala.collection.mutable

/**
 * @author jiangbuyun
 * @version	1.0
 * @since	2017-09-28
 * @category	接口类，描述公共对外访问的全局方法。
 * */
trait ILevelDB {
    /**
	 * @author jiangbuyun
	 * @version	1.0
	 * @since	2017-09-28
	 * @category	获取指定的键值
	 * @param	key String 指定的键
	 * @return	返回对应键的值 Array[Byte]
	 * */
    def   Get(key : String):Array[Byte]
     /**
	 * @author jiangbuyun
	 * @version	1.0
	 * @since	2017-09-28
	 * @category	存储指定的键和值到数据库
	 * @param	key String 指定的键，bb Array[Byte] 要存储的值
	 * @return	返回成功或者失败 Boolean
	 * */
    def   Put (key : String,bb : Array[Byte]):Boolean
     /**
	 * @author jiangbuyun
	 * @version	1.0
	 * @since	2017-09-28
	 * @category	删除指定的键值
	 * @param	key String 指定的键
	 * @return	返回成功或者失败 Boolean
	 * */
    def   Delete (key : String) : Boolean
     /**
	 * @author jiangbuyun
	 * @version	1.0
	 * @since	2017-09-28
	 * @category	把字节数组转成字符串
	 * @param	b Array[Byte] 待转换字节数组
	 * @return	返回转换结果，String 如果为null 返回空字符串
	 * */
    def   toString(b : Array[Byte]):String
    /**
	 * @author jiangbuyun
	 * @version	1.0
	 * @since	2017-09-28
	 * @category	把字节数组转成长整型
	 * @param	b Array[Byte] 待转换字节数组
	 * @return	返回转换结果，Long  如果为null 返回-1
	 * */
    def   toLong(b : Array[Byte]):Long
     /**
	 * @author jiangbuyun
	 * @version	1.0
	 * @since	2017-09-28
	 * @category	把字节数组转成整型
	 * @param	b Array[Byte] 待转换字节数组
	 * @return	返回转换结果，Int  如果为null 返回-1
	 * */
  	def   toInt(b : Array[Byte]):Int
  	 /**
	 * @author jiangbuyun
	 * @version	1.0
	 * @since	2017-09-28
	 * @category	打印Map中的键值对
	 * @param	map 需要打印的map
	 * @return	无
	 * */
  	def   printlnHashMap(map : mutable.HashMap[String,Array[Byte]])
    /**
	 * @author jiangbuyun
	 * @version	1.0
	 * @since	2017-09-28
	 * @category	获取当前系统的名称
	 * @param	无
	 * @return	返回当前系统的名称 String
	 * */
    def   getSystemName:String
    /**
	 * @author jiangbuyun
	 * @version	1.0
	 * @since	2017-09-28
	 * @category	获取当前实例的名称
	 * @param	无
	 * @return	返回当前实例的名称 String
	 * */
    def   getInstanceName:String
     /**
	 * @author jiangbuyun
	 * @version	1.0
	 * @since	2017-09-28
	 * @category	计算当前WorldState的Merkle的值
	 * @param	无
	 * @return	返回WorldState的Merkle值 Array[Byte]
	 * */
    def   GetComputeMerkle:Array[Byte]
    /**
	 * @author jiangbuyun
	 * @version	1.0
	 * @since	2017-09-28
	 * @category	计算当前WorldState的Merkle的值
	 * @param	无
	 * @return	返回WorldState的Merkle值 String
	 * */
    def   GetComputeMerkle4String:String
}