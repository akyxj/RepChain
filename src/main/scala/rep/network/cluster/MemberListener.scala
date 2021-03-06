package rep.network.cluster

import akka.actor.{Actor, Address}
import akka.cluster.ClusterEvent._
import akka.cluster.pubsub.DistributedPubSub
import akka.cluster.{Cluster, MemberStatus}
import rep.app.conf.TimePolicy
import rep.network.Topic
import rep.network.base.ModuleHelper
import rep.network.cluster.MemberListener.{MemberDown, Recollection}
import rep.network.module.ModuleManager.ClusterJoined
import rep.network.tools.PeerExtension
import rep.utils.GlobalUtils.ActorType
import rep.utils.{RepLogging, TimeUtils}

import scala.collection.mutable


/**
  * Cluster节点状态监听模块
  *
  * @author shidianyue
  * @version 1.0
  * @since 1.0
  **/
object MemberListener {

  //断网消息
  case class MemberDown(address: Address)
  //稳定节点回收请求
  case object Recollection

}
/**
  * Cluster节点状态监听类
  *
  * @author shidianyue
  * @version 1.0
  * @since 1.0
  **/

class MemberListener extends Actor with ClusterActor with ModuleHelper with RepLogging {

  import context.dispatcher

  import scala.concurrent.duration._

  val addr_self = akka.serialization.Serialization.serializedActorPath(self)

  val cluster = Cluster(context.system)

  var preloadNodesMap = mutable.HashMap[ Address, Long ]()

  def scheduler = context.system.scheduler


  override def preStart(): Unit =
    super.preStart()

  cluster.subscribe(self, classOf[ MemberEvent ])
  context.system.eventStream.subscribe(self, classOf[ akka.remote.DisassociatedEvent ])

  SubscribeTopic(mediator, self, addr_self, Topic.Event, false)

  /**
    * 节点状态是否稳定
    * @param srcTime
    * @param dur
    * @return
    */
  def isStableNode(srcTime: Long, dur: Long): Boolean = {
    (TimeUtils.getCurrentTime() - srcTime) > dur
  }


  override def postStop(): Unit =
    cluster unsubscribe self

  //无序，暂时为动态的第一个（可变集合是否是安全的，因为并不共享。如果多个System会共存副本的话，同样需要验证一致性）
  //必须缓存，如果memActor跪了则每次出块就会出问题
  //同步的时候一定要把nodes也同步
  var nodes = Set.empty[ Address ]

  def receive = {

    //系统初始化时状态
    case state: CurrentClusterState =>
      println("Member call first time")
      nodes = state.members.collect {
        case m if m.status == MemberStatus.Up => m.address
      }
      pe.resetNodes(nodes)
      pe.resetStableNodes(nodes)
      if (!nodes.isEmpty) {
        pe.resetSeedNode(nodes.head)
        getActorRef(ActorType.MODULE_MANAGER) ! ClusterJoined
      }

      //成员入网
    case MemberUp(member) =>
      nodes += member.address
      log.info("Member is Up: {}. {} nodes in cluster",
        member.address, nodes.size)
      if (nodes.size == 1) pe.resetSeedNode(member.address)
      pe.putNode(member.address)
      preloadNodesMap.put(member.address, TimeUtils.getCurrentTime())
      scheduler.scheduleOnce(TimePolicy.getSysNodeStableDelay millis,
        self, Recollection)
      //判断自己是否已经join到网络中
      addr_self.contains(member.address.toString) match {
        case true =>
          getActorRef(ActorType.MODULE_MANAGER) ! ClusterJoined
        case false => //ignore
      }
      //成员离网
    case MemberRemoved(member, _) =>
      nodes -= member.address
      log.info("Member is Removed: {}. {} nodes cluster",
        member.address, nodes.size)
      preloadNodesMap.remove(member.address)
      pe.removeNode(member.address)
      pe.removeStableNode(member.address)
      //Tell itself voter actor to judge if the downer is blocker or not
      getActorRef(ActorType.VOTER_MODULE) ! MemberDown(member.address)

    // For test
//    case Event(addr, topic, action, blk) =>
//      topic match {
//        case Topic.Block =>
//          action match {
//            case Event.Action.BLOCK_SYNC_SUC =>
//              println(s"$addr sync sucess, ${pe.getSysName}")
//            case _ => //ignore
//          }
//        case _ => //ignore
//      }

      //稳定节点收集
    case Recollection =>
      Thread.sleep(TimePolicy.getStableTimeDur) //给一个延迟量
      println(pe.getSysTag + " MemberListening recollection")
      preloadNodesMap.foreach(node => {
        if (isStableNode(node._2, TimePolicy.getSysNodeStableDelay)) {
          pe.putStableNode(node._1)
        }
      })
      if (preloadNodesMap.size > 0) pe.getStableNodes.foreach(node => {
        if (preloadNodesMap.contains(node)) preloadNodesMap.remove(node)
      })
      if (preloadNodesMap.size > 0) self ! Recollection

    case event: akka.remote.DisassociatedEvent => //ignore

    case _: MemberEvent => // ignore
  }
}