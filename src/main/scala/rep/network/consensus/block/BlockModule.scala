package rep.network.consensus.block

import akka.actor.{ActorRef, Address, Props}
import akka.cluster.pubsub.DistributedPubSubMediator.Publish
import com.google.protobuf.ByteString
import rep.app.conf.{SystemProfile, TimePolicy}
import rep.crypto.Sha256
import rep.network.consensus.vote.CRFDVoterModule.NextVote
import rep.network._
import rep.network.base.ModuleBase
import rep.network.consensus.block.BlockModule._
import rep.network.cluster.ClusterHelper
import rep.network.consensus.CRFD.CRFD_STEP
import rep.network.persistence.PersistenceModule
import rep.network.persistence.PersistenceModule.{BlockRestore, BlockSrc}
import rep.network.sync.SyncModule.SyncResult
import rep.network.consensus.transaction.PreloadTransactionModule.PreTransFromType
import rep.protos.peer._
import rep.storage.ImpDataAccess
import rep.utils.GlobalUtils.{ActorType, BlockEvent, EventType}
import scala.collection.mutable

/**
  * 出块模块伴生对象
  *
  * @author shidianyue
  * @version 1.0
  * @since 1.0
  **/
object BlockModule {

  def props(name: String): Props = Props(classOf[ BlockModule ], name)

  //打包待预执行块
  case class PreTransBlock(blc: Block, from: Int)

  //块预执行结果
  case class PreTransBlockResult(blc: Block, result: Boolean, merk: String, errorType: Int, error: String)

  //创世块预执行结果
  case class PreTransBlockResultGensis(blc: Block, result: Boolean, merk: String, errorType: Int, error: String)

  //打包块待背书
  case class PrimaryBlock(blc: Block, blocker: Address,voteinedx:Int)
  
  case class PrimaryBlock4Cache(blc: Block, blocker: Address,voteinedx:Int,actRef: ActorRef)
  
  //case class PrimaryBlock1(blc: Block, blocker: Address,iscache:Boolean)

  //块背书结果
  case class EndorsedBlock(isSuccess: Boolean, blc: Block, endor: Endorsement)

  //正式块
  case class ConfirmedBlock(blc: Block, height: Long, actRef: ActorRef)

  //块链数据
  case class BlockChainData(bc: mutable.HashMap[ String, Block ])

  //出块超时
  case object CreateBlockTimeOut

  //出块请求
  case object NewBlock

  //创世块请求
  case object GenesisBlock

  //出块模块初始化完成
  case object BlockModuleInitFinished
  
  case object ResendEndorseInfo

}

/**
  * 出块模块
  *
  * @author shidianyue
  * @version 1.0
  * @since 1.0
  * @param moduleName 模块名称
  **/
class BlockModule(moduleName: String) extends ModuleBase(moduleName) {

  import context.dispatcher
  import scala.concurrent.duration._
  import akka.actor.ActorSelection 

  val dataaccess: ImpDataAccess = ImpDataAccess.GetDataAccess(pe.getSysTag)

  //缓存当前块
  var blc = new Block()
  //modify by jiangbuyun two var transfer to peerextension
  //背书状态
  //var endorState = false
  //出块状态
  //var isBlocking = false
  private val recvedEndorseAddr = new mutable.HashMap[ String, String ]()
  
  var schedulerLink1: akka.actor.Cancellable = null

  def scheduler1 = context.system.scheduler
  def clearSched1() = {
    if (schedulerLink1 != null) schedulerLink1.cancel()
    null
  }
  
  var isGensis = false

  /**
    * Clear the cache for new block
    */
  def clearCache(): Unit = {
    blc = new Block()
    //pe.setBlk(new Block())
    pe.setEndorState(false)
    pe.setIsBlocking(false)
    recvedEndorseAddr.clear()
    schedulerLink1 = clearSched1()
    //isBlocking = false
    //endorState = false
  }

  def visitService(sn : Address, actorName:String,p:PrimaryBlock) = {  
        try {  
          val  selection : ActorSelection  = context.actorSelection(toAkkaUrl(sn , actorName));  
          selection ! p 
        } catch  {  
             case e: Exception => e.printStackTrace()
        }  
    }  
  
  def addEndoserNode(endaddr:String,actorName:String)={
    if(endaddr.indexOf(actorName)>0){
      val addr = endaddr.substring(0, endaddr.indexOf(actorName))
      if(!recvedEndorseAddr.contains(addr) ){
          recvedEndorseAddr += addr -> ""
      }
    }
  }
  
  def resendEndorser(actorName:String,p:PrimaryBlock)={
    pe.getStableNodes.foreach(sn=>{
            if(!recvedEndorseAddr.contains(sn.toString)){
              visitService(sn , "/user/moduleManager/consensusManager/consensus-CRFD/endorse",PrimaryBlock(blc, pe.getBlocker,pe.getBlker_index))
            }
       })
  }
  
    def toAkkaUrl(sn : Address, actorName:String):String = {  
        return sn.toString + "/"  + actorName;  
    }  
  
  override def preStart(): Unit = {
    logMsg(LOG_TYPE.INFO, "Block module start")
    //TODO kami 这里值得注意：整个订阅过s程也是一个gossip过程，并不是立即生效。需要等到gossip一致性成功之后才能够receive到注册信息。
    SubscribeTopic(mediator, self, selfAddr, Topic.Block, true)
    scheduler.scheduleOnce(TimePolicy.getStableTimeDur millis, context.parent, BlockModuleInitFinished)
  }

  override def receive = {
    //创建块请求（给出块人）
    case (BlockEvent.CREATE_BLOCK, _) =>
      //触发原型块生成
      pe.getIsSync() match {
        case false =>
          //isBlocking match {
          pe.getIsBlocking() match {
            case false =>
              //清空缓存
              schedulerLink = clearSched()
              clearCache()
              //准备出块
              logMsg(LOG_TYPE.INFO, "Create Block start in BlockEvent.CREATE_BLOCK")
              logTime("Create Block start", CRFD_STEP._3_BLK_CREATE_START, getActorRef(ActorType.STATISTIC_COLLECTION))
              //From memberListener itself
              //发送候选人事件
              sendEvent(EventType.PUBLISH_INFO, mediator, selfAddr, Topic.Block, Event.Action.CANDIDATOR)
              blc = BlockHelper.createPreBlock(pe.getCurrentBlockHash, pe.getTransListClone(SystemProfile.getLimitBlockTransNum))
              //pe.setBlk(BlockHelper.createPreBlock(pe.getCurrentBlockHash, pe.getTransListClone(SystemProfile.getLimitBlockTransNum)))
              
              logTime("Create Block end", CRFD_STEP._4_BLK_CREATE_END, getActorRef(ActorType.STATISTIC_COLLECTION))
              logMsg(LOG_TYPE.INFO, "Create Block end in BlockEvent.CREATE_BLOCK")
              //endorState = false
              //isBlocking = true
              pe.setEndorState(false)
              pe.setIsBlocking(true)
              getActorRef(pe.getSysTag, ActorType.PRELOADTRANS_MODULE) ! PreTransBlock(blc, PreTransFromType.BLOCK_CREATOR)
              //getActorRef(pe.getSysTag, ActorType.PRELOADTRANS_MODULE) ! PreTransBlock(pe.getBlk, PreTransFromType.BLOCK_CREATOR)
              //New block timer
              schedulerLink = scheduler.scheduleOnce(TimePolicy.getTimeOutBlock seconds, self, CreateBlockTimeOut)
            case true =>
              logMsg(LOG_TYPE.INFO, "Blocking, waiting for next block request in BlockEvent.CREATE_BLOCK")
              getActorRef(pe.getSysTag, ActorType.VOTER_MODULE) ! NextVote(true,0)
          }
        case true =>
          logMsg(LOG_TYPE.INFO, "Syncing, waiting for next block request")
          getActorRef(pe.getSysTag, ActorType.VOTER_MODULE) ! NextVote(true,0)
      }
    //系统开始出块通知（给非出块人）
    case (BlockEvent.BLOCKER, addr) =>
      pe.getIsSync() match {
        case false =>
          //isBlocking match {
          pe.getIsBlocking() match {
            case false =>
              //背书获取当前出块人（更新出块状态）
              //isBlocking = true
              pe.setIsBlocking(true)
              //添加出块定时器
              schedulerLink = scheduler.scheduleOnce(TimePolicy.getTimeOutBlock seconds, self, CreateBlockTimeOut)
            case true =>
              logMsg(LOG_TYPE.INFO, "Blocking, waiting for next block request in BlockEvent.BLOCKER")
              getActorRef(pe.getSysTag, ActorType.VOTER_MODULE) ! NextVote(true,0)
          }
        case true =>
          logMsg(LOG_TYPE.INFO, "Syncing, waiting for next block request")
          getActorRef(pe.getSysTag, ActorType.VOTER_MODULE) ! NextVote(true,0)

      }
    //出块预执行结果
    case PreTransBlockResult(blk, result, merk, errorType, error) =>
      result match {
        case true =>
          blc.previousBlockHash == blk.previousBlockHash match {
          //pe.getBlk.previousBlockHash == blk.previousBlockHash match {
            case true =>
              logTime("Endorsement publish", CRFD_STEP._7_ENDORSE_PUB, getActorRef(ActorType.STATISTIC_COLLECTION))
              logMsg(LOG_TYPE.INFO, "PreTransBlockResult ... ")
              blc = blk.withStateHash(ByteString.copyFromUtf8(merk))
              //pe.setBlk(blk.withStateHash(ByteString.copyFromUtf8(merk)))
              //Endorsement for block（sign for hash）--including the endorsement of itself
              blc = blc.withConsensusMetadata(Seq(BlockHelper.endorseBlock(Sha256.hash(blc.toByteArray), pe.getSysTag)))
              //pe.setBlk(pe.getBlk.withConsensusMetadata(Seq(BlockHelper.endorseBlock(Sha256.hash(pe.getBlk.toByteArray), pe.getSysTag))))
              //Broadcast the Block
              //这个块经过预执行之后已经包含了预执行结果和状态
              //mediator ! Publish(BlockEvent.BLOCK_ENDORSEMENT, PrimaryBlock(blc, pe.getBlocker))
              schedulerLink1 = scheduler1.scheduleOnce(15 seconds, self, ResendEndorseInfo)
              pe.getStableNodes.foreach(sn=>{
                visitService(sn , "/user/moduleManager/consensusManager/consensus-CRFD/endorse",PrimaryBlock(blc, pe.getBlocker,pe.getBlker_index))
              })
              
             // pe.getStableNodes.foreach(sn=>{
             //   visitService(sn , "/user/moduleManager/consensusManager/consensus-CRFD/endorse",PrimaryBlock(pe.getBlk, pe.getBlocker,false))
             // })
              //Endorsement require event
              sendEvent(EventType.PUBLISH_INFO, mediator, selfAddr, Topic.Endorsement, Event.Action.BLOCK_ENDORSEMENT)
            case false => logMsg(LOG_TYPE.INFO, "Receives a wrong trans preload result with timeout error")
          }
        case false => logMsg(LOG_TYPE.INFO, s"Block preload trans failed, error type : ${errorType} ~ ${error}")

      }
    case ResendEndorseInfo =>
      resendEndorser("/user/moduleManager/consensusManager/consensus-CRFD/endorse",PrimaryBlock(blc, pe.getBlocker,pe.getBlker_index))
    //块背书结果
    case EndorsedBlock(isSuccess, blc_new, endor) =>
      var log_msg = ""
      logMsg(LOG_TYPE.WARN, s"recv endorsed from ${sender().toString()}")
      ClusterHelper.isCandidateNow(sender().toString(), pe.getCandidator) match {
        case true =>
          //endorState match {
          pe.getEndorState() match {
            case false =>
              isSuccess match {
                case true =>
                  //Endorsement collection
                  val blcEn = blc.withConsensusMetadata(Seq())
                  //val blcEn = pe.getBlk.withConsensusMetadata(Seq())
                  //TODO kami 类似于MD5验证，是否是同一个blk（可以进一步的完善，存在效率问题？）
                  blc_new.hashCode() == blcEn.hashCode() match {
                    case true =>
                      BlockHelper.checkBlockContent(endor, Sha256.hash(blc_new.toByteArray)) match {
                        case true =>
                          blc = blc.withConsensusMetadata(blc.consensusMetadata.+:(endor))
                          //pe.setBlk( pe.getBlk.withConsensusMetadata(pe.getBlk.consensusMetadata.+:(endor)))
                          addEndoserNode(sender().toString(),"/user/moduleManager/consensusManager/consensus-CRFD/endorse")
                          if (BlockHelper.checkCandidate(blc.consensusMetadata.length, pe.getCandidator.size)) {
                          //if (BlockHelper.checkCandidate(pe.getBlk.consensusMetadata.length, pe.getCandidator.size)) {
                            schedulerLink1 = clearSched1()
                            self ! NewBlock
                            //endorState = true
                            pe.setEndorState(true)
                            logTime("Endorsement collect end", CRFD_STEP._10_ENDORSE_COLLECTION_END,
                              getActorRef(ActorType.STATISTIC_COLLECTION))
                          }
                          //广播收到背书信息的事件
                          sendEvent(EventType.RECEIVE_INFO, mediator, selfAddr, Topic.Block,
                            Event.Action.ENDORSEMENT)
                        case false => logMsg(LOG_TYPE.INFO, "Block endorse request data checked by cert failed")
                      }
                    case false => logMsg(LOG_TYPE.INFO, "Drop a endorsement by wrong block")
                  }
                case false => logMsg(LOG_TYPE.INFO, "Endorsement failed by trans checking")
              }
            case true => logMsg(LOG_TYPE.INFO, "Endorsement of the block is enough, drop it")
          }
        case false => logMsg(LOG_TYPE.INFO, s"The sender is not a candidate this time, drop the endorsement form it. Sender:${sender()}")
      }

    //正式出块
    case NewBlock =>
      //广播这个block
      mediator ! Publish(Topic.Block, new ConfirmedBlock(blc, dataaccess.getBlockChainInfo().height + 1,
        getActorRef(ActorType.MODULE_MANAGER)))
      //mediator ! Publish(Topic.Block, new ConfirmedBlock(pe.getBlk, dataaccess.getBlockChainInfo().height + 1,
      //  getActorRef(ActorType.MODULE_MANAGER)))
      //背书成功后发布新块的event
      //c4w Topic.xxx 用于代表实时图上的几个点，为指导绘图而设
      mediator ! Publish(Topic.Event, new Event(selfAddr, Topic.Block, Event.Action.BLOCK_NEW, Some(blc)))
      logTime("New block publish", CRFD_STEP._11_NEW_BLK_PUB, getActorRef(ActorType.STATISTIC_COLLECTION))
      //mediator ! Publish(Topic.Event, new Event(selfAddr, Topic.Block, Event.Action.BLOCK_NEW, Some(pe.getBlk)))
      //logTime("New block publish", CRFD_STEP._11_NEW_BLK_PUB, getActorRef(ActorType.STATISTIC_COLLECTION))

    //接收广播的正式块数据
    case ConfirmedBlock(blk, height, actRef) =>
      //确认，接受新块（满足最基本的条件）
      logTime("New block get and check", CRFD_STEP._12_NEW_BLK_GET_CHECK,
        getActorRef(ActorType.STATISTIC_COLLECTION))
      val endors = blk.consensusMetadata
      val blkOutEndorse = blk.withConsensusMetadata(Seq())
      if (BlockHelper.checkCandidate(endors.size, pe.getCandidator.size)) {
        var isEndorsed = true
        for (endorse <- endors) {
          //TODO kami 这是一个非常耗时的工作？后续需要完善
          if (!BlockHelper.checkBlockContent(endorse, Sha256.hash(blkOutEndorse.toByteArray))) isEndorsed = false
        }
        if (isEndorsed) {
          logTime("New block, start to store", CRFD_STEP._13_NEW_BLK_START_STORE,
            getActorRef(ActorType.STATISTIC_COLLECTION))
          //c4w 广播接收到block事件
          sendEvent(EventType.RECEIVE_INFO, mediator, selfAddr, Topic.Block, Event.Action.BLOCK_NEW)
          getActorRef(pe.getSysTag, ActorType.PERSISTENCE_MODULE) ! BlockRestore(blk, height, BlockSrc.CONFIRMED_BLOCK, actRef)
          getActorRef(pe.getSysTag, ActorType.PERSISTENCE_MODULE) ! PersistenceModule.LastBlock(BlockHelper.getBlkHash(blk), 0,
            BlockSrc.CONFIRMED_BLOCK, self)
          //pe.addCacheBlkNum()
          //pe.addCacheHeight()
          if (isThisAddr(selfAddr, pe.getBlocker.toString)) {
            //Thread.sleep(500)
            logMsg(LOG_TYPE.INFO, "block store finish,start new vote ...")
          }else{
            logMsg(LOG_TYPE.INFO, s"blocker=${pe.getBlocker.toString},self=${selfAddr}")
          }
          //接收到新块之后重新vote
          schedulerLink = clearSched()
          //状态设置改到持久化之后
          //clearCache()
          logMsg(LOG_TYPE.INFO, "New block, store opt over ...")
          //perhaps question
          //getActorRef(pe.getSysTag, ActorType.VOTER_MODULE) ! NextVote(true)
          logTime("New block, store opt over", CRFD_STEP._14_NEW_BLK_STORE_END,
            getActorRef(ActorType.STATISTIC_COLLECTION))
          
        }
        else logMsg(LOG_TYPE.WARN, s"The block endorsement info is wrong. Sender : ${sender()}")
      }
      else logMsg(LOG_TYPE.WARN, s"The num of  endorsement in block is not enough. Sender : ${sender()}")

    //创世块创建
    case GenesisBlock =>
      logMsg(LOG_TYPE.INFO, "Create genesis block")
      blc = BlockHelper.genesisBlockCreator()
      //pe.setBlk(BlockHelper.genesisBlockCreator())
      isGensis = true
      getActorRef(pe.getSysTag, ActorType.PRELOADTRANS_MODULE) ! PreTransBlock(blc, PreTransFromType.BLOCK_GENSIS)
      //getActorRef(pe.getSysTag, ActorType.PRELOADTRANS_MODULE) ! PreTransBlock(pe.getBlk, PreTransFromType.BLOCK_GENSIS)

    //创世块预执行结果
    case PreTransBlockResultGensis(blk, result, merk, errorType, error) =>
      if (result) {
        blc = blk.withStateHash(ByteString.copyFromUtf8(merk))
        //pe.setBlk(pe.getBlk.withStateHash(ByteString.copyFromUtf8(merk)))
        //According the genesis block to init the shared storage
        try {
          println(logPrefix + s"${pe.getSysTag} ~ Merk(Before Gensis): " + pe.getMerk)
          dataaccess.restoreBlocks(Array(blc))
          //dataaccess.restoreBlocks(Array(pe.getBlk))
          //pe.addCacheHeight()
          pe.setCacheHeight(dataaccess.getBlockHeight())
          pe.setMerk(dataaccess.GetComputeMerkle4String)
          pe.setCurrentBlockHash(dataaccess.getBlockChainInfo().currentBlockHash.toStringUtf8)
          println(logPrefix + s"${pe.getSysTag} ~ Merk(After Gensis): " + dataaccess.GetComputeMerkle4String)
          println("Gensis Block size is " + blc.toByteArray.size)
          //println("Gensis Block size is " + pe.getBlk.toByteArray.size)
          //创世块创建成功
          sendEvent(EventType.PUBLISH_INFO, mediator, selfAddr, Topic.Block, Event.Action.BLOCK_SYNC_SUC)
          //c4w Topic.xxx 用于代表实时图上的几个点，为指导绘图而设
          mediator ! Publish(Topic.Event, new Event(selfAddr, Topic.Block, Event.Action.BLOCK_NEW, Some(blc)))
          //mediator ! Publish(Topic.Event, new Event(selfAddr, Topic.Block, Event.Action.BLOCK_NEW, Some(pe.getBlk)))
          isGensis = false
          clearCache()
          getActorRef(pe.getSysTag, ActorType.VOTER_MODULE) ! NextVote(true,0)
        }
        catch {
          case e: Exception =>
            logMsg(LOG_TYPE.ERROR, s"Commit error : ${e.toString}")
            logMsg(LOG_TYPE.ERROR, "GensisBlock create failed, preload transaction error")
            clearCache()
            self ! GenesisBlock
          case _ => logMsg(LOG_TYPE.ERROR, "Commit Error : Unknown")
        }
        finally {}
      }
      else {
        logMsg(LOG_TYPE.WARN, "GensisBlock create failed, preload transaction error")
        self ! GenesisBlock
      }

    //同步结果
    case SyncResult(result, isStart, isStartNode, blkHash) =>
      isStart match {
        case true =>
          sendEvent(EventType.PUBLISH_INFO, mediator, selfAddr, Topic.Block, Event.Action.BLOCK_SYNC_SUC)
          isStartNode match {
            case true =>
              pe.getCacheHeight() match {
                case 0 =>
                  self ! GenesisBlock
                case _ =>
                  getActorRef(pe.getSysTag, ActorType.VOTER_MODULE) ! NextVote(true,0)
              }
            case false =>
              if (!result) logMsg(LOG_TYPE.WARN,
                "System isn't start with sync successfully! Please shutdown and check it")
              else getActorRef(pe.getSysTag, ActorType.VOTER_MODULE) ! NextVote(true,0)
          }
        case false => //ignore,目前没有这种情况
      }
    //出块超时
    case CreateBlockTimeOut =>
      //isBlocking match {
      pe.getIsBlocking() match {
        case true =>
          logMsg(LOG_TYPE.WARN, "Create new block timeout check, failed")
          clearCache()
          getActorRef(pe.getSysTag, ActorType.VOTER_MODULE) ! NextVote(false,0)
        case false => logMsg(LOG_TYPE.INFO, "Create block timeout check. successfully")
      }

    case _ => //ignore
  }

}