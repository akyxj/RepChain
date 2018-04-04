package rep.utils

/**
  * Created by User on 2017/6/12.
  */
object ActorUtils {

  /**
    * Get (ip,port) of this system from Remote path
    * Example:
    * clusterPath:akka.ssl.tcp://repChain_@192.168.100.93:53486/user/pm_#-1893758935
    * result:(192.168.100.93,53486)
    * @param clusterPath
    * @return
    */
  def getIpAndPort(clusterPath:String): (String, String) ={
    var str = clusterPath.substring(clusterPath.indexOf("@")+1)
    str = str.substring(0,str.indexOf("/"))
    val re = str.split(":")
    (re(0),re(1))
  }

  def isHelper(path:String):Boolean = {
    path.contains("helper")
  }

  def isAPI(path:String):Boolean = {
    path.contains("api")
  }
}
