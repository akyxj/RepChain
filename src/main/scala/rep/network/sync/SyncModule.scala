package rep.network.sync

import akka.actor.{ ActorRef, Props }
import akka.cluster.pubsub.DistributedPubSubMediator.Publish
import rep.app.conf.TimePolicy
import rep.network.base.ModuleBase
import rep.network.consensus.vote.CRFDVoterModule.SeedNode
import rep.network.consensus.block.BlockHelper
import rep.network.persistence.PersistenceModule
import rep.network.persistence.PersistenceModule.{ BlockRestore, BlockSrc, LastBlock }
import rep.network.sync.SyncModule._
import rep.protos.peer.{ Block, BlockchainInfo }
import rep.storage.ImpDataAccess
//import rep.utils.GlobalUtils.{ ActorType, BlockEvent }
import rep.utils.GlobalUtils.{ ActorType, BlockEvent, EventType }
//import rep.network.Topic
import rep.protos.peer.Event

import scala.collection.mutable
import rep.network.Topic



/**
 * Created by User on 2017/9/22.
 */
object SyncModule {
  def props(name: String): Props = Props(classOf[SyncModule], name)

  case class ChainInfoReq(info: BlockchainInfo, actorAddr: String)

  case class ChainInfoRes(src: BlockchainInfo,
    response: BlockchainInfo, isSame: Boolean,
    addr: String, addActor: ActorRef)

  case class ChainSyncReq(blocker: ActorRef)

  case class ChainDataReq(receiver: ActorRef,
    height: Long, blkType: String)

  case class ChainDataReqSingleBlk(receiver: ActorRef,
    targetHeight: Long)

  case class ChainDataRes(data: Array[Block])

  case class ChainDataResSingleBlk(data: Block, targetHeight: Long)

  case class SyncResult(result: Boolean, isStart: Boolean, isStartNode: Boolean, lastBlkHash: String)

  //同步超时
  case object SyncTimeOut

  //When system setup
  case object SetupSync

}

class SyncModule(moduleName: String) extends ModuleBase(moduleName) {

  import context.dispatcher
  import scala.concurrent.duration._

  val dataaccess: ImpDataAccess = ImpDataAccess.GetDataAccess(pe.getSysTag)

  var isStart = false
  //现阶段出块同步都是向出块人，所以没有用
  //Cache sync result
  var syncResults = mutable.HashMap[Long, Seq[ChainInfoRes]]()
  var syncSameCount = 0
  var syncMaxCount = 0
  var syncMaxHight: Long = 0
  var isSyncReq = false

  override def preStart(): Unit = {
    SubscribeTopic(mediator, self, selfAddr, BlockEvent.CHAIN_INFO_SYNC, false)
  }

  def clearCache(): Unit = {
    syncResults.clear()
    syncMaxHight = 0
    syncSameCount = 0
    syncMaxCount = 0
    isStart = false
  }

  override def receive: Receive = {
    case SetupSync =>
      //系统启动，开始同步      
      logMsg(LOG_TYPE.INFO, moduleName, "Start sync setup", selfAddr)
      val info = dataaccess.getBlockChainInfo()
      pe.setCacheHeight(info.height)
      mediator ! Publish(BlockEvent.CHAIN_INFO_SYNC, ChainInfoReq(info, selfAddr))
      //添加初始化同步定时器
      schedulerLink = scheduler.scheduleOnce(TimePolicy.getTimeoutSync seconds, self, SyncTimeOut)
      isStart = true

    case ChainInfoReq(info, actorAddr) =>
      logMsg(LOG_TYPE.INFO, moduleName, s"Get sync req from $actorAddr", selfAddr)
      val responseInfo = dataaccess.getBlockChainInfo()
      var isSame = false
      if (responseInfo.currentWorldStateHash.toStringUtf8 == info.currentWorldStateHash.toStringUtf8)
        isSame = true
      sender() ! ChainInfoRes(info, responseInfo, isSame, selfAddr, self)

    case ChainInfoRes(src, response, isSame, addr, act) =>
      pe.getIsSync() match {
        case true =>
          sender() == self match {
            case true =>
              logMsg(LOG_TYPE.INFO, moduleName, s"Sync Res from itself", selfAddr)
              pe.getNodes.size match {
                case 1 =>
                  logMsg(LOG_TYPE.INFO, moduleName, s"Just one node in cluster", selfAddr)
                  getActorRef(pe.getSysTag, ActorType.VOTER_MODULE) ! SeedNode
                  getActorRef(pe.getSysTag, ActorType.BLOCK_MODULE) ! SyncResult(true, isStart, true,
                    src.currentBlockHash.toStringUtf8)
                  pe.setCurrentBlockHash(src.currentBlockHash.toStringUtf8)
                  clearCache()
                  pe.setIsSync(false)
                  schedulerLink = clearSched()
                case _ => //ignore

              }
            case false =>
              logMsg(LOG_TYPE.INFO, moduleName, s"Get a chaininfo from $addr", selfAddr)
              syncResults.contains(response.height) match {
                case true =>
                  syncResults.put(response.height,
                    syncResults.get(response.height).get :+ (ChainInfoRes(src, response, isSame, addr, act)))
                case false =>
                  syncResults.put(response.height,
                    Array(ChainInfoRes(src, response, isSame, addr, act)))
              }
              if (isSame) syncSameCount += 1
              if (syncResults.get(response.height).get.size > syncMaxCount) {
                syncMaxHight = response.height
                syncMaxCount = syncResults.get(response.height).get.size
              }
              //选择索要
              (syncSameCount * 2) >= (pe.getNodes.size - 1) match {
                case true =>
                  logMsg(LOG_TYPE.INFO, moduleName, s"The chaininfo is same as most of nodes", selfAddr)
                  getActorRef(ActorType.BLOCK_MODULE) ! SyncResult(true, isStart, false,
                    src.currentBlockHash.toStringUtf8)
                  pe.setCurrentBlockHash(src.currentBlockHash.toStringUtf8)
                  clearCache()
                  pe.setIsSync(false)
                  schedulerLink = clearSched()
                case false =>
                  if ((syncMaxCount * 2) >= (pe.getNodes.size - 1)) {
                    if (!isSyncReq) {
                      isSyncReq = true
                      val re = syncResults(syncMaxHight)
                      re.head.addActor ! ChainDataReq(self,
                        dataaccess.getBlockChainInfo().height, BlockSrc.SYNC_START_BLOCK)
                    }
                  }
              }
          }
        case false =>
          logMsg(LOG_TYPE.INFO, moduleName, s"Sync is over", selfAddr)
      }

    case ChainDataReq(rec, infoH, blkType) =>
      logMsg(LOG_TYPE.INFO, moduleName, s"Get a data request from  $sender", selfAddr)
      // TODO 广播同步 --test
      println("#############")
      println(sender.path.toString());
      println(selfAddr);
      println("############")
      sendEvent(EventType.PUBLISH_INFO, mediator,sender.path.toString(), selfAddr,  Event.Action.BLOCK_SYNC)
      sendEventSync(EventType.PUBLISH_INFO, mediator,sender.path.toString(), selfAddr,  Event.Action.BLOCK_SYNC)
//      sendEvent(EventType.PUBLISH_INFO, mediator, selfAddr, Topic.Event, Event.Action.BLOCK_SYNC)
      val local = dataaccess.getBlockChainInfo()
      var data = Array[Block]()
      if (local.height > infoH) {
        data = dataaccess.getBlocks4ObjectFromHeight((infoH + 1).toInt)
      }
      if (data != null && data.size > 0) {
        blkType match {
          case BlockSrc.SYNC_START_BLOCK =>
            var height = infoH + 1
            data.foreach(blk => {
              rec ! ChainDataResSingleBlk(blk, height)
              height += 1
            })
            rec ! PersistenceModule.LastBlock(BlockHelper.getBlkHash(data(data.length - 1)), local.height,
              BlockSrc.SYNC_START_BLOCK, self)
          case BlockSrc.CONFIRMED_BLOCK =>
            //ignore
            rec ! ChainDataResSingleBlk(data(0), infoH + 1)
        }
        logMsg(LOG_TYPE.INFO, moduleName, s"Sync data send successfully", selfAddr)
      }

    case chainDataReqSB: ChainDataReqSingleBlk =>
      logMsg(LOG_TYPE.INFO, moduleName, s"Get a data request from  $sender", selfAddr)
      val local = dataaccess.getBlockChainInfo()
      var data: Block = null
      if (local.height > chainDataReqSB.targetHeight) {
        data = dataaccess.getBlock4ObjectByHeight(chainDataReqSB.targetHeight.toInt)
      }
      if (data != null) {
        chainDataReqSB.receiver ! ChainDataResSingleBlk(data, chainDataReqSB.targetHeight)
      }
      logMsg(LOG_TYPE.INFO, moduleName, s"Sync data send successfully", selfAddr)

    case ChainDataResSingleBlk(data, targetHeight) =>
      //收到同步区块数据
      println("***********")
      println(sender.path.toString());
      println(selfAddr);
      println(mediator);
      println("***********")
      // TODO 收到同步 --test
      sendEvent(EventType.RECEIVE_INFO, mediator, sender.path.toString(), selfAddr, Event.Action.BLOCK_SYNC)
      sendEventSync(EventType.RECEIVE_INFO, mediator,sender.path.toString(), selfAddr,  Event.Action.BLOCK_SYNC)  
      logMsg(LOG_TYPE.INFO, moduleName, s"Get a data from $sender", selfAddr)
      //TODO kami 验证信息和数据合法性，现阶段不考虑;验证创世块的合法性
      getActorRef(ActorType.PERSISTENCE_MODULE) ! BlockRestore(data,
        targetHeight, BlockSrc.SYNC_START_BLOCK, self)
      //pe.addCacheBlkNum()
      //pe.addCacheHeight()

    case LastBlock(blkHash, height, blockSrc, blker) =>
      getActorRef(ActorType.BLOCK_MODULE) ! SyncResult(true, isStart, false, blkHash)
      if (isStart) clearCache()
      schedulerLink = clearSched()
      pe.setIsSync(false)

    case SyncTimeOut =>
      pe.getIsSync() match {
        case true =>
          logMsg(LOG_TYPE.INFO, moduleName, s"Sync timeout checked, failed", selfAddr)
          //重新请求一遍
          dataaccess.getBlockHeight() == syncMaxHight match {
            case true =>
              getActorRef(ActorType.BLOCK_MODULE) ! SyncResult(true, isStart, false,
                dataaccess.getBlockChainInfo().currentBlockHash.toStringUtf8)
            case false =>
              if ((syncMaxCount * 2) >= (pe.getNodes.size - 1)) {
                val re = syncResults(syncMaxHight)
                re.head.addActor ! ChainDataReq(self, dataaccess.getBlockChainInfo().height,
                  BlockSrc.SYNC_START_BLOCK)
                schedulerLink = scheduler.scheduleOnce(TimePolicy.getTimeoutSync seconds, self, SyncTimeOut)
              } else {
                logMsg(LOG_TYPE.INFO, moduleName, s"System is not stable with useful nodes of chain data", selfAddr)
                getActorRef(ActorType.BLOCK_MODULE) ! SyncResult(false, isStart, false, null)
                if (isStart) clearCache()
              }
          }
        case false =>
          logMsg(LOG_TYPE.INFO, moduleName, s"Sync finished", selfAddr)

      }

    case _ => //ignore
  }

}
