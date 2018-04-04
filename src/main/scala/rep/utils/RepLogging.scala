package rep.utils

import org.slf4j.LoggerFactory
import rep.network.cluster.ClusterActor

object RepLogging{
  case class LogTime(module:String, msg:String, time:Long, cluster_addr:String)
}

trait RepLogging {

  case object LOG_TYPE{
    val INFO = 1
    val DEBUG =2
    val WARN = 3
    val ERROR = 4
  }

  protected def log = LoggerFactory.getLogger(this.getClass)

  /**
    * 记录操作时间相关的日志
    * @param sysname
    * @param log_prefix
    * @param tag
    */
  @deprecated
  def logTime(sysname:String, log_prefix:String, tag:String):Unit = {
    log.info(log_prefix + " : " + sysname + " " + tag + " ~ " + TimeUtils.getCurrentTime())
  }

  /**
    * 记录当前操作时间（鼓励使用）
    * @param module
    * @param msg
    * @param time
    * @param cluster_addr
    */
  def logTime(module:String, msg:String, time:Long, cluster_addr:String) = {
    log.info(module + " ~ Opt Time ~ " + msg + " ~  " + time + " ~ " + cluster_addr)
  }

  def logMsg(lOG_TYPE: Int, module:String, msg:String, cluster_addr:String) = {

    lOG_TYPE match {
      case LOG_TYPE.INFO =>
        log.info(module + " ~ " + msg + " ~ " + cluster_addr)
      case LOG_TYPE.DEBUG =>
        log.debug(module + " ~ " + msg + " ~ " + cluster_addr)
      case LOG_TYPE.WARN =>
        log.warn(module + " ~ " + msg + " ~ " + cluster_addr)
      case LOG_TYPE.ERROR =>
        log.error(module + " ~ " + msg + " ~ " + cluster_addr)
    }
  }

}