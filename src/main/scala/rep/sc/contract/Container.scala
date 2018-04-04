package rep.sc.contract
import akka.actor.{Actor, ActorRef, Props, actorRef2Scala}
import rep.storage._
import rep.utils._
import rep.api.rest._
import rep.protos.peer._
import rep.crypto.ECDSASign
import rep.network.tools.PeerExtension
import rep.storage.IdxPrefix.WorldStateKeyPreFix
import rep.sc.TransProcessor._
import rep.sc.Sandbox._
import rep.sc.Sandbox
import rep.sc.Shim
import rep.sc.Shim.Oper
import rep.utils.Json4s._
import com.trueaccord.scalapb.json.JsonFormat
import com.google.protobuf.ByteString

import org.json4s._
/**
 * 负责执行scala编写的合约
 */
object Container {
  val log_prefix = "Container-"
  def props(cid:String): Props = Props(new Container(cid))
}

class Container(cid:String) extends Actor with RepLogging{
  //热加载工具，应该使用单例对象
  var cobj:Contract = null
  val pcid = cid
  val pe = PeerExtension(context.system)
  val sTag =pe.getSysTag

  var span_max:Long =0
  val shim = new Shim(context.system, cid)
  val addr_self = akka.serialization.Serialization.serializedActorPath(self)

  //如果function 是deploy，
  def receive = {
    //执行交易
    case  DoTransaction(t:Transaction,from:ActorRef, da:String) =>
      val tr = doTransaction(t,from,da)
      sender ! tr
    //预执行交易，指定接收者
    case  PreTransaction(t:Transaction) =>
      val tr = doTransaction(t,null,null)
      shim.rollback()
      sender ! tr
    //恢复chainCode,不回消息
    case  DeployTransaction(t:Transaction,from:ActorRef, da:String) =>
      val tr = doTransaction(t,from,da)
      shim.rollback()
  }
  
  def doTransaction(t:Transaction,from:ActorRef, da:String):DoTransactionResult ={
   val tm_start = System.currentTimeMillis()
    //上下文可获得交易
    //每次执行脚本之前重置 
    //shim.reset() 由于DoTransactionResult依赖此两项,不能直接clear,要么clone一份给result,
    //要么上一份给result，重新建一份
    shim.sr = ImpDataPreloadMgr.GetImpDataPreload(sTag, da)
    shim.mb = scala.collection.mutable.Map[String,Array[Byte]]()
    shim.ol = scala.collection.mutable.ListBuffer.empty[Oper]
   //构造和传入ctx
   val ctx = new ContractContext(shim,t)
    //如果执行中出现异常,返回异常
    try{
      val cs = t.payload.get
      val cid = cs.chaincodeID.get.name
      val r:JValue = t.`type` match {
        //如果cid对应的合约class不存在，根据code生成并加载该class
        case Transaction.Type.CHAINCODE_DEPLOY => 
          //TODO 热加载code对应的class
          val code = cs.codePackage.toStringUtf8()

          val clazz = Compiler.compilef(code,pcid)
          cobj = clazz.getConstructor().newInstance().asInstanceOf[Contract]
          //cobj = new ContractAssets()
          cobj.init(ctx)
          //deploy返回chancode.name
          //利用kv记住cid对应的txid,并增加kv操作日志,以便恢复deploy时能根据cid找到当时deploy的tx及其代码内容
          val txid = ByteString.copyFromUtf8(t.txid).toByteArray()
          val key = WorldStateKeyPreFix+ cid
          shim.sr.Put(key,txid)
          //ol value改为byte array
          shim.ol.append(new Oper(key, null, txid))
          encodeJson(cid)
         //新建class实例并执行合约,传参为json数据
        case  Transaction.Type.CHAINCODE_INVOKE =>
          //获得合约action
          val action = cs.ctorMsg.get.function
          //获得传入参数
          val data = cs.ctorMsg.get.args
             
        var tm_start1 = System.currentTimeMillis()  
        cobj.onAction(ctx,action,data.head)
        val span1 = System.currentTimeMillis()-tm_start1
        println(s"----container span1:$span1")
        encodeJson(cobj.onAction(ctx,action,data.head))
      }
      val span2 = System.currentTimeMillis()-tm_start
      println(s"container span2:$span2")
      
      //modify by jiangbuyun 20170802
      //TODO 有必要每笔交易都计算Merkle根吗？？？
      val mb = shim.sr.GetComputeMerkle4String//sr.GetComputeMerkle  //mh.computeWorldState4Byte()
      val mbstr = mb match {
        case null => None
        case _ => Option(mb)  //Option(BytesHex.bytes2hex(mb))
      }
      new DoTransactionResult(t,from, r, 
          mbstr,
         shim.ol.toList,shim.mb,None)
    }catch{
      case e: Exception => 
        shim.rollback        
        log.error(t.txid, e)
        //val e1 = new Exception(e.getMessage, e.getCause)
        //akka send 无法序列化原始异常,简化异常信息
        val e1 = new SandboxException(e.getMessage)
        new DoTransactionResult(t,from, null,
           None,
          shim.ol.toList,shim.mb, 
          Option(akka.actor.Status.Failure(e1)))           
    }finally{
      val span = System.currentTimeMillis()-tm_start
        //println(s"Span doTransaction:$span")
        logMsg(LOG_TYPE.INFO, Container.log_prefix, s"Span doTransaction:$span", "")
        log.info(Container.log_prefix +s"Span doTransaction:$span")
      if(span>span_max){
        span_max= span
        //log.info(Sandbox.log_prefix+s"Max_span doTransaction:$span type:${t.`type`}  txid:${t.txid}")
      }
    }
  }
}