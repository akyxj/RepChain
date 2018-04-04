package rep.network.consensus.transaction

import akka.actor.{Actor, ActorRef, Cancellable, Props}
import com.google.protobuf.ByteString
import com.google.protobuf.timestamp.Timestamp
import rep.app.conf.{SystemProfile, TimePolicy}
import rep.network.base.ModuleBase
import rep.network.consensus.block.BlockModule.{PreTransBlock, PreTransBlockResult, PreTransBlockResultGensis}
import rep.network.tools.PeerExtension
import rep.network.consensus.transaction.PreloadTransactionModule.{PreLoadTransTimeout, PreTransFromType}
import rep.network.Topic
import rep.network.consensus.CRFD.CRFD_STEP
import rep.protos.peer.{Block, NonHashData, OperLog, Transaction}
import rep.sc.TransProcessor.DoTransaction
import rep.sc.{Sandbox, TransProcessor}
import rep.storage.{ImpDataAccess, ImpDataPreload, ImpDataPreloadMgr}
import rep.utils.GlobalUtils.ActorType
import rep.utils._

import scala.collection.mutable

/**
  * Transaction handler
  * Created by kami on 2017/6/6.
  */
object PreloadTransactionModule {
  def props(name: String,transProcessor:ActorRef): Props = Props(classOf[ PreloadTransactionModule ], name,transProcessor)

  case object PreTransFromType {
    val BLOCK_GENSIS = 0
    val BLOCK_CREATOR = 1
    val ENDORSER = 2
  }

  //交易回滚通知
  case object RollBackTrans

  //预执行超时通知
  case object PreLoadTransTimeout

}

class PreloadTransactionModule(moduleName: String, transProcessor:ActorRef) extends ModuleBase(moduleName) {
  import context.dispatcher
  import scala.collection.breakOut
  import scala.concurrent.duration._

  val dataaccess: ImpDataAccess = ImpDataAccess.GetDataAccess(pe.getSysTag)

  var preLoadTrans = mutable.HashMap.empty[ String, Transaction ]
  var transPreloadTime = mutable.HashMap.empty[ String, Long ]
  var transResult = Seq.empty[ rep.protos.peer.TransactionResult ]
  var errorCount = 0
  var blk = rep.protos.peer.Block()

  var preloadFrom = 0
  var isPreload = false

  def clearCache(): Unit = {
    preloadFrom = 0
    errorCount = 0
    preLoadTrans = mutable.HashMap.empty[ String, Transaction ]
    transResult = Seq.empty[ rep.protos.peer.TransactionResult ]
    blk = rep.protos.peer.Block()
  }

  def preLoadFeedBackInfo(resultFlag: Boolean, block: rep.protos.peer.Block, from: Int, merk: String): Unit = {
    preloadFrom match {
      case PreTransFromType.BLOCK_CREATOR =>
        getActorRef(ActorType.BLOCK_MODULE) ! PreTransBlockResult(block,
          resultFlag, merk, 0, "")

      case PreTransFromType.ENDORSER =>
        getActorRef(ActorType.ENDORSE_MODULE) ! PreTransBlockResult(block,
          resultFlag, merk, 0, "")

      case PreTransFromType.BLOCK_GENSIS =>
        getActorRef(ActorType.BLOCK_MODULE) ! PreTransBlockResultGensis(block,
          resultFlag, merk, 0, "")
    }
  }

  def getBlkFromByte(array: Array[Byte]):Block = {
    Block.parseFrom(array)
  }

  override def preStart(): Unit = {
    logMsg(LOG_TYPE.INFO,"PreloadTransaction Module Start")
  }

  override def receive = {

    case PreTransBlock(blc, from) =>
      val preBlk = dataaccess.getBlockByHash(blk.previousBlockHash.toStringUtf8)
      if((preBlk!=null && dataaccess.getBlockChainInfo().currentWorldStateHash == getBlkFromByte(preBlk).stateHash.toStringUtf8)
      || blk.previousBlockHash == ByteString.EMPTY){
        //先清空缓存
        clearCache() //是否是多余的，确保一定执行了
        schedulerLink = clearSched()
        //开始预执行
        logTime("TransPreloadStart",CRFD_STEP._5_PRELOAD_START,getActorRef(ActorType.STATISTIC_COLLECTION))
        //预执行并收集结果
        logMsg(LOG_TYPE.INFO, s"Get a preload req, form ${sender()}")
        preLoadTrans = blc.transactions.map(trans => (trans.txid, trans))(breakOut): mutable.HashMap[ String, Transaction ]
        val preload :ImpDataPreload = ImpDataPreloadMgr.GetImpDataPreload(pe.getDBTag,blc.transactions.head.txid)
        //确保提交的时候顺序执行
        blc.transactions.map(t => {
          transProcessor ! new DoTransaction(t,self,blc.transactions.head.txid)
        })
        blk = blc
        preloadFrom = from
        isPreload = true
        //TODO kami 超时在这里，如果超时算是错误类型中的一种
        schedulerLink = scheduler.scheduleOnce(TimePolicy.getTimeoutPreload seconds, self, PreLoadTransTimeout)
      }
      else logMsg(LOG_TYPE.WARN, "Preload Transcations input consensus failed")

    case Sandbox.DoTransactionResult(t,from, r, merkle, ol, mb, err) =>
      //是否在当前交易列表中
      preLoadTrans.getOrElse(t.txid, None) match {
        case None => logMsg(LOG_TYPE.WARN, s"${t.txid} does exist in the block this time, size is ${preLoadTrans.size}")
        case _ =>
          err match {
            case None =>
              //c4w  7.31
              var olist = new scala.collection.mutable.ArrayBuffer[ OperLog ]()

              for (l <- ol) {
                val bso = l.oldValue match {
                  case null => _root_.com.google.protobuf.ByteString.EMPTY
                  case _ => ByteString.copyFrom(l.oldValue)
                }
                val bsn = l.newValue match {
                  case null => _root_.com.google.protobuf.ByteString.EMPTY
                  case _ => ByteString.copyFrom(l.newValue)
                }
                olist += new OperLog(l.key, bso, bsn)
              }

              val result = new rep.protos.peer.TransactionResult(t.txid,
                olist, 0, "")

              preLoadTrans(t.txid) = t.withMetadata(ByteString.copyFrom(SerializeUtils.serialise(mb)))

              //          println(log_prefix +s"Old trans ${t.txid} size : " + t.toByteArray.size)
              //          println(log_prefix +s"New trans ${t.txid} with res sizs : " + preLoadTrans(t.txid).toByteArray.size)
              //          println(log_prefix +s"${t.txid} result size : " + result.toByteArray.size)

              transResult = (transResult :+ result)
              //          println(s"T:${transResult.size} + $errorCount ? ${blk.transactions.size}")
              //块内所有交易预执行全部成功
              if ((transResult.size + errorCount) == blk.transactions.size) {
                //Preload totally successfully
                var newTranList = mutable.Seq.empty[ Transaction ]
                for (tran <- blk.transactions) {
                  if (preLoadTrans.getOrElse(tran.txid, None) != None)
                    newTranList = newTranList :+ preLoadTrans(tran.txid)
                }

                //            println(logPrefix + " Block total trans : " + transResult.size)
                //            println(log_prefix +"Old block size : " + blk.toByteArray.size)

                blk = blk.withTransactions(newTranList)
                val millis = TimeUtils.getCurrentTime()
                val nohash = NonHashData(Option(Timestamp(millis / 1000, ((millis % 1000) * 1000000).toInt)),
                  transResult)
                blk = blk.withNonHashData(nohash)
                //            println(log_prefix +"New block size : " + blk.toByteArray.size)
                //            pe.setMerk(merkle.getOrElse(""))//get from chen
                //            println(s"${pe.getSysName} =================================================================")
                //            db.printmap(db.FindByLike("c_"))
                //            println("=================================================================")
                println(s"Merk ${pe.getSysTag} : ~ preload after " + merkle.getOrElse(""))
                preLoadFeedBackInfo(true, blk, preloadFrom, merkle.getOrElse(""))

                logTime(s"Trans Preload End, Trans size ${newTranList.size}", CRFD_STEP._6_PRELOAD_END,
                  getActorRef(ActorType.STATISTIC_COLLECTION))
                isPreload = false
                ImpDataPreloadMgr.Free(pe.getDBTag,blk.transactions.head.txid)
                clearCache() //是否是多余的，确保一定执行了
                schedulerLink = clearSched()
              }
            case _ =>
              logMsg(LOG_TYPE.WARN, s"${t.txid} preload error, error: ${err.get}")
              //TODO kami 删除出错的交易，如果全部出错，则返回false
              preLoadTrans.remove(t.txid)
              errorCount += 1
              println("ErrCount:" + errorCount)
              if (preLoadTrans.size <= SystemProfile.getMinBlockTransNum) {
                println(s"Preload rest size: ${preLoadTrans.size}")
                //非容忍性错误（错误数量太多）
                preLoadFeedBackInfo(false, blk, preloadFrom, pe.getMerk)
                ImpDataPreloadMgr.Free(pe.getDBTag,blk.transactions.head.txid)
                isPreload = false
                clearCache() //是否是多余的，确保一定执行了
                schedulerLink = clearSched()
              }
          }
      }

    case PreLoadTransTimeout =>
      isPreload match {
        case true =>
          logMsg(LOG_TYPE.WARN, "Preload trans timeout checked, unfinished")
          preLoadFeedBackInfo(false, blk, preloadFrom, pe.getMerk)
          isPreload = false
          ImpDataPreloadMgr.Free(pe.getDBTag,blk.transactions.head.txid)
          clearCache() //是否是多余的，确保一定执行了
          schedulerLink = clearSched()
        case false => logMsg(LOG_TYPE.INFO, "Preload trans timeout checked, successfully")

      }

    case _ => //ignore
  }
}
