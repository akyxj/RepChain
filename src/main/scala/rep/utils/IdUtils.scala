package rep.utils

import java.util.UUID
import com.gilt.timeuuid.TimeUuid

/**
  * 生成ID
  * Created by User on 2017/6/29.
  */
object IdUtils {
  def getUUID(): String = {
    val uuid = TimeUuid()
    uuid.toString
  }

  def getRandomUUID: String = {
    UUID.randomUUID().toString
  }

  def main(args: Array[String]): Unit = {
    //TODO kami 需要进行并发测试
    println(IdUtils.getUUID())
    println(IdUtils.getUUID())
    println(IdUtils.getUUID())
    println(IdUtils.getUUID())
    println(IdUtils.getUUID())

    println(IdUtils.getRandomUUID)
  }

}
