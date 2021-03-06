package rep.network

import akka.actor.{Actor, Props}
import com.google.protobuf.ByteString
import com.google.protobuf.timestamp.Timestamp
import rep.app.conf.SystemProfile
import rep.crypto.{ECDSASign, Sha256}
import rep.network.base.ModuleBase
import rep.network.consensus.block.BlockHelper
import rep.network.cluster.ClusterActor
import rep.network.tools.PeerExtension
import rep.protos.peer._
import rep.utils.GlobalUtils.ActorType
import rep.utils.{IdUtils, RepLogging, TimeUtils}

import scala.concurrent.forkjoin.ThreadLocalRandom

/**
  *
  * 代理节点辅助类
  * 用来定时生成Transaction
  * 之后也应该通过其接收前端数据进而生成Transaction
  *
  * Created by kami on 2017/6/6.
  */

object Topic {
  val Transaction = "Transaction"
  val Block = "Block"
  val Event = "Event"
  val Endorsement = "Endorsement"
}

object InnerTopic{
  val BlockRestore = "BlockRestore"
}

object PeerHelper {
  
  def props(name: String): Props = Props(classOf[PeerHelper], name)

  case object Tick
  case object TickInit
  case object TickInvoke
  case object TickQuery

  /**
    * 创建交易的公共该方法
    *
    * @param nodeName 节点名（同db使用相同的id）
    * @param tranType
    * @param chainCodeIdPath
    * @param chaincodeInputFunc
    * @param params
    * @param spcPackage
    * @param chaincodeId
    * @return
    */
  def transactionCreator(nodeName:String,tranType:rep.protos.peer.Transaction.Type,chainCodeIdPath:String,
                         chaincodeInputFunc:String,params:Seq[String], spcPackage:String,chaincodeId:Option[String],
                         ctype:rep.protos.peer.ChaincodeSpec.CodeType= rep.protos.peer.ChaincodeSpec.CodeType.CODE_JAVASCRIPT): 
                         Transaction ={
    val millis = TimeUtils.getCurrentTime()
    //deploy时取脚本内容hash作为 chaincodeId/name
    //invoke时调用者应该知道要调用的 chaincodeId
    val name = chaincodeId match {
      case None =>
        Sha256.hashstr(spcPackage)
      case Some(g) =>
        //此处hash是针对所有code代码内容,动态加载的类也必须遵循此规则
        if(g.trim().equals(""))
          Sha256.hashstr(spcPackage) 
        else 
          g
    }
    //TODO kami name = path.hash（目前是用code package的hash）
    val cid = new ChaincodeID(chainCodeIdPath,name)
    //构建运行代码
    val cip = new ChaincodeInput(chaincodeInputFunc, params)
    //初始化链码
    //TODO kami secureContext 应为 client的 token（也可以是唯一标示，ID等）
    val chaincodeSpec = new ChaincodeSpec(Option(cid), Option(cip), 1000, "secureContext", ByteString.copyFromUtf8(spcPackage),ctype)
        
    var t = new Transaction(tranType,
      ByteString.copyFromUtf8(cid.toString),
      Option(chaincodeSpec),
      ByteString.copyFromUtf8("metaData"),
      "",
      Option(Timestamp(millis/1000 , ((millis % 1000) * 1000000).toInt)),
      ConfidentialityLevel.PUBLIC,
      "confidentialityProtocolVersion-1.0",
      ByteString.copyFromUtf8("nonce"),
      ByteString.copyFromUtf8("toValidators"),
      ByteString.EMPTY,
      ByteString.EMPTY
    )
    var txid = ""
    tranType match {
      case Transaction.Type.CHAINCODE_DEPLOY =>
        //如果是Deploy就是name
        txid = name
      case _ =>
        //其他就是一个随机码，现阶段为RFC 4122 Version 1
        txid = IdUtils.getUUID()
    }
    t = t.withTxid(txid)
    //Signature
    val (priKey, pubKey, cert) = ECDSASign.getKeyPair(nodeName)
    t = t.withCert(ByteString.copyFromUtf8(ECDSASign.getBitcoinAddrByCert(cert)))
    //Get signature with tx hash（include cert）
    val sig = ECDSASign.sign(priKey,getTxHash(t))
    t = t.withSignature(ByteString.copyFrom(sig))
    t
  }

  /**
    * Get hash for Transaction
    *
    * @param t
    * @return
    */
  def getTxHash(t:Transaction) :Array[Byte] ={
    Sha256.hash(t.toByteArray)
  }

}

class PeerHelper(name: String) extends ModuleBase(name) {
  import rep.network.PeerHelper._
  import scala.concurrent.duration._
  import context.dispatcher

  def rnd = ThreadLocalRandom.current

  //temp data
//  val s1 = scala.io.Source.fromFile("scripts/example_deploy.js")
//  val l1 = try s1.mkString finally s1.close()

  val si1 = scala.io.Source.fromFile("scripts/example_invoke_"+ pe.getSysTag +".js")
  val li1 = try si1.mkString finally si1.close()
  val si2 = scala.io.Source.fromFile("scripts/transfer_"+ pe.getSysTag +".json")
  val li2 = try si2.mkString finally si2.close()
  
  var chaincode = ""

  override def preStart(): Unit =
  {
    logMsg(LOG_TYPE.INFO,name,"Transaction Creator Start",selfAddr)
    scheduler.scheduleOnce(5.seconds, self, Tick)
  }

  // override postRestart so we don't call preStart and schedule a new Tick
  override def postRestart(reason: Throwable): Unit = ()

  override def receive = {

    case Tick =>
      val blk = BlockHelper.genesisBlockCreator()
      chaincode = blk.transactions(0).payload.get.getChaincodeID.name
      scheduler.scheduleOnce(10.seconds, self, TickInit)

    case TickInit =>
      if(SystemProfile.getTranCreateDur > 0)
        scheduler.scheduleOnce(SystemProfile.getTranCreateDur.millis, self, TickInvoke)
      //deploy
//      val t1 = transactionCreator(pe.getSysName,rep.protos.peer.Transaction.Type.CHAINCODE_DEPLOY,
//        "", "" ,List(),l1,None)
//      receiver(ActorType.PEER_TRANSACTION, context) ! t1
//
//      scheduler.scheduleOnce(rnd.nextInt(5, 10).seconds, self, TickInvoke(t1))

    case TickInvoke =>
      //invoke
//      val cname = t.payload.get.chaincodeID.get.name
      val t2 = transactionCreator(pe.getSysTag,rep.protos.peer.Transaction.Type.CHAINCODE_INVOKE,
        "", li1 ,List(),"", Option(chaincode),rep.protos.peer.ChaincodeSpec.CodeType.CODE_JAVASCRIPT)
      //getActorRef(ActorType.TRANSACTION_POOL) ! t2  
      val t3 = transactionCreator(pe.getSysTag,rep.protos.peer.Transaction.Type.CHAINCODE_INVOKE,
        "", "transfer" ,Seq(li2),"", Option(chaincode),rep.protos.peer.ChaincodeSpec.CodeType.CODE_JAVASCRIPT)  
      getActorRef(ActorType.TRANSACTION_POOL) ! t3
      scheduler.scheduleOnce(SystemProfile.getTranCreateDur.millis, self, TickInvoke)
  }
}
