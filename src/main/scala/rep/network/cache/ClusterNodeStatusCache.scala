package rep.network.cache

import akka.actor.{Actor, ActorRef, Props}
import akka.cluster.Cluster
import akka.cluster.ddata.{DistributedData, LWWMap, LWWMapKey}
import rep.network.tools.PeerExtension
import rep.utils.RepLogging

import scala.concurrent.forkjoin.ThreadLocalRandom

/**
  * 分布式全局缓存机制实现
  * @deprecated
  * Created by User on 2017/8/10.
  */

object ClusterNodeStatusCache {

  def props(name: String): Props = Props(classOf[ ClusterNodeStatusCache ], name)

  private final case class Request(key: String, replyTo: ActorRef)

  final case class PutInCache(key: String, value: Any)

  final case class GetFromCache(key: String)

  final case class Cached(key: String, value: Option[ Any ])

  final case class Evict(key: String)

  final case class PutInTargetCache(target:String, key: String, value: Any)

  final case class GetFromTargetCache(target:String, key: String)

  final case class TargetEvict(target:String, key: String)

  final case class TargetCached(target:String, value:Any)

  case object TickPut

  case object TickGet

  case object CacheTarget{
    val CHAIN_INFO = "chain_info"
  }

}

class ClusterNodeStatusCache(name: String) extends Actor with RepLogging {

  import ClusterNodeStatusCache._
  import akka.cluster.ddata.Replicator._
  import context.dispatcher

  import scala.concurrent.duration._

  val replicator = DistributedData(context.system).replicator
  implicit val cluster = Cluster(context.system)
  val pe = PeerExtension(context.system)

  def rnd = ThreadLocalRandom.current

  def scheduler = context.system.scheduler

  val log_prefix = "Network-PeerCache~"
  val writeMajority = WriteMajority(5 seconds, 4)
  val readMajority = ReadMajority(5 seconds, 4)

  def cacheHashKey(entryKey: String): LWWMapKey[ String, Any ] =
    LWWMapKey("cache-" + math.abs(entryKey.hashCode) % 100)

  def cacheKey(entryKey: String): LWWMapKey[ String, Any ] =
    LWWMapKey("cache-" + entryKey)

  override def preStart(): Unit = {
//    scheduler.scheduleOnce(10 seconds, self, TickPut)
    scheduler.scheduleOnce(20 seconds, self, TickGet)
    log.warn(log_prefix + " Cache start")
  }

  override def receive: Receive = {

    case PutInCache(key, value) =>
      replicator ! Update(cacheHashKey(key), LWWMap(), writeMajority)(_ + (key -> value))

    case Evict(key) =>
      replicator ! Update(cacheHashKey(key), LWWMap(), writeMajority)(_ - key)

    case GetFromCache(key) =>
      replicator ! Get(cacheHashKey(key), readMajority, Some(Request(key, sender())))

//    case g@GetSuccess(LWWMapKey(_), Some(Request(key, replyTo))) =>
//      g.dataValue match {
//        case data: LWWMap[ _, _ ] =>
//          data.asInstanceOf[ LWWMap[ String, Any ] ].get(key) match {
//            case Some(value) => replyTo ! Cached(key, Some(value))
//            case None => replyTo ! Cached(key, None)
//          }
//      }

    case PutInTargetCache(target, key, value) =>
      replicator ! Update(cacheKey(target), LWWMap(), writeMajority)(_ + (key -> value))

    case TargetEvict(target, key) =>
      replicator ! Update(cacheKey(target), LWWMap(), writeMajority)(_ - key)

    case GetFromTargetCache(target, key) =>
      replicator ! Get(cacheKey(target), readMajority, Some(Request(key, sender())))

    case g@GetSuccess(LWWMapKey(_), Some(Request(key, replyTo))) =>
      g.dataValue match {
        case data: LWWMap[ _, _ ] =>
          val cacheValue = data.asInstanceOf[ LWWMap[ String, Any ] ]
          cacheValue.size match {
            case 0 => replyTo ! TargetCached(key, None)
            case _ => replyTo ! TargetCached(key, cacheValue.entries)
          }
      }

    case NotFound(_, Some(Request(key, replyTo))) =>
      replyTo ! Cached(key, None)

//    case Cached(key, value) =>
//      value match {
//        case None => println("Cache info is none")
//        case _ => println(s"Cache info is ${value.get}")
//      }

    case _: UpdateResponse[ _ ] => // ok
  }
}
