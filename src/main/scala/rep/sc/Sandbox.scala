package rep.sc

import akka.actor.{Actor, ActorRef, Props, actorRef2Scala}
import rep.utils._
import rep.api.rest._
import rep.protos.peer._
import delight.nashornsandbox._
import java.util.concurrent.Executors
import java.lang.Exception
import java.lang.Thread._
import java.io.File._

import org.json4s.{DefaultFormats, Formats, jackson}
import de.heikoseeberger.akkahttpjson4s.Json4sSupport
import org.json4s._
import com.trueaccord.scalapb.json.JsonFormat
import com.google.protobuf.ByteString
import akka.util.Timeout
import Shim._
import rep.crypto.BytesHex
import rep.crypto.ECDSASign
import rep.network.tools.PeerExtension
import rep.storage.IdxPrefix.WorldStateKeyPreFix

/** 合约容器的抽象类伴生对象,定义了交易执行结果的case类
 * 
 */
object Sandbox {
  //日志前缀
  val log_prefix = "Sandbox-"
  //t:中含txid可以找到原始交易; r:执行结果; merkle:执行完worldstate的hash; err:执行异常
  /** 交易执行结果类
   * @param t 传入交易实例
   * @param from 来源actor指向
   * @param r 执行结果,任意类型
   * @param merkle 交易执行后worldState的merkle结果，用于验证和达成输出共识
   * @param ol 合约执行中对worldState的写入操作
   * @param mb 合约执行涉及的key-value集合
   * @param err 执行中抛出的异常信息
   */
  case class DoTransactionResult(t:Transaction,from:ActorRef, r:JValue,merkle:Option[String],
    ol:List[Oper],
    mb:scala.collection.mutable.Map[String,Array[Byte]],
    err:Option[akka.actor.Status.Failure])
    
  /** 合约执行异常类
   *  @param message 异常信息的文本描述
   *  @param cause 导致异常的原因
   */
  case class SandboxException(private val message: String = "", 
                           private val cause: Throwable = None.orNull)
                      extends Exception(message, cause) 

}

/** 合约容器的抽象类，提供与底层进行API交互的shim实例，用于与存储交互的实例pe
 * 
 * 目前已实现的合约容器包括SandboxJS—以javascript作为合约脚本语言,不支持debug,性能较低;
 * 另一个实现是以scala作为合约脚本语言的SandboxScalax，支持debug,性能较高
 * 
 * @constructor 以合约在区块链上的链码id作为合约容器id建立实例
 * @param cid 链码id
 */
abstract class Sandbox(cid:String) extends Actor with RepLogging{
  import TransProcessor._
  import Sandbox._
  import spray.json._
  //与存储交互的实例
  val pe = PeerExtension(context.system)
  val sTag =pe.getSysTag

  //与底层交互的api实例
  val shim = new Shim(context.system, cid)
  val addr_self = akka.serialization.Serialization.serializedActorPath(self)

  /** 消息处理主流程,包括对交易处理请求、交易的预执行处理请求、从存储恢复合约的请求
   * 
   */
  def receive = {
    //交易处理请求
    case  DoTransaction(t:Transaction,from:ActorRef, da:String) =>
      val tr = doTransaction(t,from,da)
      sender ! tr
    //交易预处理请求，指定接收者
    case  PreTransaction(t:Transaction) =>
      val tr = doTransaction(t,null,null)
      shim.rollback()
      sender ! tr
    //恢复chainCode,不回消息
    case  DeployTransaction(t:Transaction,from:ActorRef, da:String) =>
      val tr = doTransaction(t,from,da)
      shim.rollback()
  }
  /** 交易处理抽象方法，接受待处理交易，返回处理结果
   *  @param t 待处理交易
   *  @param from 发出交易请求的actor
   * 	@param da 存储访问标示
   *  @return 交易执行结果
   */
  def doTransaction(t:Transaction,from:ActorRef, da:String):DoTransactionResult 
  
}