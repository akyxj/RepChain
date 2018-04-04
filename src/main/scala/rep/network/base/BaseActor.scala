package rep.network.base

import akka.actor.Actor


/**
  * 系统基础Actor封装
  *
  * @author shidianyue
  * @version 1.0
  * @since 1.0
  **/
trait BaseActor extends Actor {
  val selfAddr = akka.serialization.Serialization.serializedActorPath(self)

  var schedulerLink: akka.actor.Cancellable = null

  def scheduler = context.system.scheduler

  /**
    * 清除定时器
    *
    * @return
    */
  def clearSched() = {
    if (schedulerLink != null) schedulerLink.cancel()
    null
  }
}
