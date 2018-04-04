package rep.network.consensus.endorse

import akka.actor.{ActorRef,Props,Address}
import rep.crypto.Sha256
import rep.network.base.ModuleBase
import rep.network.consensus.block.BlockHelper
import rep.network.consensus.block.BlockModule._
import rep.network.Topic
import rep.network.cluster.ClusterHelper
import rep.network.consensus.CRFD.CRFD_STEP
import rep.protos.peer.Event
import rep.storage.{ImpDataPreload, ImpDataPreloadMgr}
import rep.utils.GlobalUtils.{ActorType, BlockEvent, EventType}
import com.sun.beans.decoder.FalseElementHandler
import rep.network.consensus.vote.CRFDVoterModule.NextVote

/**
  * Endorsement handler
  * Created by Kami on 2017/6/6.
  */

object EndorsementModule {
  def props(name: String): Props = Props(classOf[ EndorsementModule ], name)

  case object EndorseBlkTimeout

  case class BlockSyncResult(result: Boolean)

}

class EndorsementModule(moduleName: String) extends ModuleBase(moduleName) {

  import context.dispatcher
  import scala.concurrent.duration._
  import rep.protos.peer._

  override def preStart(): Unit = {
    //Subscribe the endorsement message
    SubscribeTopic(mediator, self, selfAddr, BlockEvent.BLOCK_ENDORSEMENT, false)
  }

  private def endorseForWork(blk:Block, actRef: ActorRef)={
      val preload: ImpDataPreload = ImpDataPreloadMgr.GetImpDataPreload(pe.getSysTag,
      blk.transactions.head.txid)
      try {
      val blkData = blk.withConsensusMetadata(Seq())
      val blkInfo = Sha256.hash(blkData.toByteArray)
        preload.VerifyForEndorsement(blk) match {
          case true =>
            if (BlockHelper.checkBlockContent(blk.consensusMetadata.head, blkInfo)) {
              logMsg(LOG_TYPE.WARN, "Block endorse failed")
              actRef ! EndorsedBlock(true, blkData, BlockHelper.endorseBlock(blkInfo, pe.getSysTag))
              //广播发送背书信息的事件(背书成功)
              sendEvent(EventType.PUBLISH_INFO, mediator, selfAddr, Topic.Block, Event.Action.ENDORSEMENT)
            }
            else logMsg(LOG_TYPE.WARN, "Certification vertified failed")
  
          case false =>
            logMsg(LOG_TYPE.WARN, "Block endorse failed")
           
            actRef! EndorsedBlock(false, blkData, BlockHelper.endorseBlock(blkInfo, pe.getSysTag))
        }
        logTime("Endorse end", CRFD_STEP._9_ENDORSE_END, getActorRef(ActorType.STATISTIC_COLLECTION))
         logMsg(LOG_TYPE.WARN, "Endorse end")
      } catch {
      case e: Exception =>
        //todo 背书失败
        e.printStackTrace()
    }
  }
  
  def NoticeVoteModule(voteinedx:Int)={
      pe.setIsBlocking(false)
      pe.setEndorState(false)
      pe.setIsBlockVote(false)
      if(voteinedx>1){
        getActorRef(pe.getSysTag, ActorType.VOTER_MODULE) ! NextVote(false,voteinedx)
      }else{
        getActorRef(pe.getSysTag, ActorType.VOTER_MODULE) ! NextVote(true,0)
      }
  }
  
  private def endorseForCheck(blk:Block, blocker:Address, voteinedx:Int, actRef: ActorRef)={
    if (!pe.getIsSync()) {
      //开始背书
        logTime("Endorse start", CRFD_STEP._8_ENDORSE_START, getActorRef(ActorType.STATISTIC_COLLECTION))
        println(pe.getSysTag + " " + pe.getPort + " request vote result for endorsement")
        //Check it's a candidate or not
        if (ClusterHelper.isCandidateNow(selfAddr, pe.getCandidator)) {
          //Check the blocker
          if (blk.previousBlockHash.toStringUtf8 == pe.getCurrentBlockHash) {
            
              if (!ClusterHelper.isBlocker(selfAddr, pe.getBlocker.toString)) {
              //Block endorsement
              //广播收到背书请求的事件
              sendEvent(EventType.RECEIVE_INFO, mediator, selfAddr, Topic.Endorsement, Event.Action.BLOCK_ENDORSEMENT)
                if (pe.getBlocker == blocker) {
                  endorseForWork(blk, actRef)
                }else {
                  //pe.setTmpEndorse(PrimaryBlock4Cache(blk, blocker,voteinedx,sender()))
                  NoticeVoteModule(voteinedx)
                  logMsg(LOG_TYPE.WARN, "endorse add to cache")
                  logMsg(LOG_TYPE.WARN, s"${blocker} is not the current blocker(${pe.getBlocker})")
                }
              
              }else{ 
                logMsg(LOG_TYPE.WARN, "Endorsement is from itself, dropped")
              }
          }else{
                //clear pe endorse cache
                //pe.setTmpEndorse(PrimaryBlock4Cache(blk, blocker,voteinedx,sender()))
                NoticeVoteModule(voteinedx)
                logMsg(LOG_TYPE.WARN, "endorse add to cache")
                logMsg(LOG_TYPE.WARN, s"Chain in this node is not complete,this current heigh=${pe.getCacheHeight()},before=${blk.previousBlockHash.toStringUtf8},local=${pe.getCurrentBlockHash},blocker=${pe.getBlocker})")
            }
        }
        else logMsg(LOG_TYPE.WARN, "It is not a candidate this time, endorsement requirement dropped")
    }else{
      logMsg(LOG_TYPE.WARN, "Syncing, waiting for next endorsement request")
    }
  }
  
  
  override def receive = {
    //Endorsement block
    case PrimaryBlock(blk, blocker, voteinedx) =>
      endorseForCheck(blk, blocker, voteinedx, sender())

    //case PrimaryBlock4Cache(blk, blocker,voteinedx,actref) =>
    //  endorseForCheck(blk, blocker, voteinedx, actref)
      
      
    case _ => //ignore
  }

}
