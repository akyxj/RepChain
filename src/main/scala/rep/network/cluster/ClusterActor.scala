package rep.network.cluster

import akka.actor.{Actor, ActorRef}
import akka.cluster.pubsub.DistributedPubSubMediator.{Publish, Subscribe}
import rep.network.Topic
import rep.protos.peer.Event
import rep.utils.ActorUtils
import rep.utils.GlobalUtils.EventType

/**
  * Akka组网类
  *
  * @author shidianyue
  * @version 1.0
  * @since 1.0
  **/
 trait ClusterActor extends  Actor{
  import akka.cluster.pubsub.DistributedPubSub

  val mediator = DistributedPubSub(context.system).mediator

  /**
    * 根据全网节点的地址（带IP）判断是否属于同一个System
    *
    * @param src
    * @param tar
    * @return
    */
  def isThisAddr(src: String, tar: String): Boolean = {
    src.startsWith(tar)
  }

  /**
    * 广播Event消息
    *
    * @param eventType 发送、接受
    * @param mediator
    * @param addr
    * @param topic
    * @param action
    */
  def sendEvent(eventType: Int, mediator: ActorRef, addr: String, topic: String, action: Event.Action): Unit = {
    eventType match {
      case EventType.PUBLISH_INFO =>
        //publish event(send message)
        val evt = new Event(addr, topic,
          action)
        mediator ! Publish(Topic.Event, evt)
      case EventType.RECEIVE_INFO =>
        //receive event
        val evt = new Event(topic, addr,
          action)
        mediator ! Publish(Topic.Event, evt)
    }

  }
  
  
    /**
    * 广播SyncEvent消息
    *
    * @param eventType 发送、接受
    * @param mediator
    * @param fromAddr
    * @param toAddr
    * @param action
    */
  def sendEventSync(eventType: Int, mediator: ActorRef, fromAddr: String, toAddr: String, action: Event.Action): Unit = {
    eventType match {
      case EventType.PUBLISH_INFO =>
        //publish event(send message)
        val evt = new Event(fromAddr, toAddr,
          action)
        mediator ! Publish(Topic.Event, evt)
      case EventType.RECEIVE_INFO =>
        //receive event
        val evt = new Event(fromAddr, toAddr,
          action)
        mediator ! Publish(Topic.Event, evt)
    }

  }

  /**
    * 获取有完全信息的地址（ip和port）
    * @param ref
    * @return
    */
  def getClusterAddr(ref:ActorRef):String = {
    akka.serialization.Serialization.serializedActorPath(ref)
  }

  /**
    * cluster订阅消息
    *
    * @param mediator
    * @param self
    * @param addr
    * @param topic
    * @param isEvent
    */
  def SubscribeTopic(mediator: ActorRef, self: ActorRef, addr: String, topic: String, isEvent: Boolean) = {
    mediator ! Subscribe(topic, self)
    //广播本次订阅事件
    if (isEvent) sendEvent(EventType.PUBLISH_INFO, mediator, addr, Topic.Event, Event.Action.SUBSCRIBE_TOPIC)
  }

  /**
    * 两个节点是否相同的system
    * （不完全对，有可能是相同IP和port但是UID不同）
    * @param from
    * @param self
    * @return
    */
  def isSameSystem(from:ActorRef, self:ActorRef): Boolean ={
    ActorUtils.getIpAndPort(getClusterAddr(from)) == ActorUtils.getIpAndPort(getClusterAddr(self))
  }

  /**
    * 两个节点是否是相同的system
    * @param from
    * @param self
    * @return
    */
  def isSameSystem(from:ActorRef, self:String) :Boolean = {
    ActorUtils.getIpAndPort(getClusterAddr(from)) == ActorUtils.getIpAndPort(self)
  }

}
