package rep.utils

/**
  * 时间相关工具
  * Created by User on 2017/5/17.
  */
object TimeUtils {

  def getCurrentTime():Long ={
    var time = System.currentTimeMillis()
    //中国时区+8
    time += 8*3600*1000
    time
  }

  def getNextTargetTimeDur(targetTime:Long): Long ={
    println("Time is : " + targetTime)
    val time = getCurrentTime()
    val result = targetTime - time%targetTime
    println("Time is : " + result)
    result
  }

  def getNextTargetTimeDurMore(targetTime:Long): Long ={
    val time = getCurrentTime()
    val result = targetTime - time%targetTime
    println("Time is : " + (result+targetTime))
    result+targetTime
  }
}
