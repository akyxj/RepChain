package rep.log

import rep.network.Topic
import rep.protos.peer._
import akka.stream.actor._
import akka.actor.Props
import akka.cluster.pubsub.DistributedPubSub
import akka.cluster.pubsub.DistributedPubSubMediator.{Publish, Subscribe}
import akka.actor.Address
import akka.cluster.Cluster
import akka.cluster.ClusterEvent._
import akka.cluster.MemberStatus
import rep.ui.web.EventServer
import rep.network.tools.PeerExtension
import rep.storage._
import akka.stream.Graph

case class Tick()

object EventActor {
  def props: Props = Props[EventActor]
}

class EventActor extends ActorPublisher[Event] {
  import scala.concurrent.duration._
  
  val cluster = Cluster(context.system)
//  var nodes = Set.empty[Address]  // xg
//  var buffer = Vector.empty[Event]   //xg

  override def preStart(): Unit ={
    cluster.subscribe(self, classOf[MemberEvent])
    val mediator = DistributedPubSub(context.system).mediator
    mediator ! Subscribe(Topic.Event, self)   
    //发送当前出块人
    val pe = PeerExtension(context.system)
    self ! new Event( pe.getBlocker.toString, "", Event.Action.CANDIDATOR) 
    //for test 序列化block细节，强制构造一个block序列化push到web
    /*
    val sr = StorageMgr.GetStorageMgr(pe.getDbRoot);
    val bb = sr.getBlockByHeight(2)
    if(bb!=null)
      self ! new Event( pe.getBlocker.toString, Topic.Block, Event.Action.BLOCK_NEW, Some(Block.parseFrom(bb))) 
     */
//    if(!nodes.isEmpty)  //xg
//      self ! new Event( nodes.head.toString, Topic.Event, Event.Action.BLOCK_SYNC,None)
  }

  override def postStop(): Unit =
    cluster unsubscribe self
    
  var nodes = Set.empty[Address]

  var buffer = Vector.empty[Event]
  
  override def receive: Receive = {
    case evt:Event=> 
      if (buffer.isEmpty && totalDemand > 0) {
        onNext(evt)
      }
      else {
        buffer :+= evt
        if (totalDemand > 0) {
          val (use,keep) = buffer.splitAt(totalDemand.toInt)
          buffer = keep
          use foreach onNext
        }
      }      
    case state: CurrentClusterState =>
      val iter = state.members.iterator;
      iter.foreach { m =>
        if (m.status == MemberStatus.Up){
          self ! new Event( m.address.toString, "", Event.Action.MEMBER_UP)
        }
      }
    case MemberUp(member) =>
      nodes += member.address
      self !new Event( member.address.toString, "", Event.Action.MEMBER_UP)
    case UnreachableMember(member) =>
 //     nodes -= member.address
 //     self ! new Event( member.address.toString,"",Event.Action.MEMBER_DOWN)
    case MemberRemoved(member, _) =>
      val maddr = member.address.toString
      val saddr =  Cluster(context.system).selfAddress.toString
      //println(s"-------$maddr-----$saddr")
      if(maddr == saddr){
        context.system.terminate()
      }else{
        nodes -= member.address
        self ! new Event( member.address.toString,"",Event.Action.MEMBER_DOWN)
      }
    case _: MemberEvent => // ignore
    
  }
}

