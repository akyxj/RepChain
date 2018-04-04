package rep.network.persistence

import akka.actor.{ActorRef, Props}
import com.google.protobuf.ByteString
import rep.network.base.ModuleBase
import rep.network.module.ModuleManager.TargetBlock
import rep.network.persistence.PersistenceModule.BlockRestore
import rep.protos.peer.Block
import rep.storage.ImpDataAccess
import rep.network.consensus.vote.CRFDVoterModule.NextVote
import scala.collection.mutable
import rep.utils.GlobalUtils.{ActorType}
/**
  *
  * Created by User on 2017/8/16.
  */

object PersistenceModule {
  def props(name: String): Props = Props(classOf[ PersistenceModule ], name)

  final case class BlockRestore(blk: Block, height: Long, blockSrc: String, blker: ActorRef)

  case object BlockSrc {
    val CONFIRMED_BLOCK = "Confirmed_Block"
    val SYNC_START_BLOCK = "Sync_Start_Block"
    val SYNC_BLOCK = "Sync_Block"
  }

  case class LastBlock(blkHash: String, height: Long, blockSrc: String, blker: ActorRef)

}

class PersistenceModule(moduleName: String) extends ModuleBase(moduleName) {
  import rep.network.persistence.PersistenceModule.{ BlockSrc}
  val dataaccess: ImpDataAccess = ImpDataAccess.GetDataAccess(pe.getSysTag)

  private var retryCount = 0

  //private val precache = new mutable.HashMap[ Long, BlockRestore ]()

  private val cacheSize = 2 //If the data in cache map over the cache size, reask for lost data

  private val retryLimit = 50

  /*def clearCache() = {
    retryCount = 0
    precache.clear()
  }*/

  /*private def getMinHeight(heightC: Long): Long = {
    var minHeight = heightC
    precache.foreach(node => if (node._1 < heightC) minHeight = node._1)
    minHeight
  }*/
  
  def SaveBlock(blkRestore: BlockRestore):Integer={
    var re : Integer = 0
    try {
          logMsg(LOG_TYPE.INFO, moduleName, s"Merk(Before presistence): ${pe.getMerk}", selfAddr)
          dataaccess.restoreBlock(blkRestore.blk)
          RefreshCacheBuffer(blkRestore)
          logMsg(LOG_TYPE.INFO, moduleName, s"Merk(After presistence): ${pe.getMerk}", selfAddr)
          logMsg(LOG_TYPE.INFO, moduleName, s"save block success,height=${pe.getCacheHeight()},hash=${pe.getCurrentBlockHash}", selfAddr)
        }
        catch {
          case e: Exception =>
            re = 1
            logMsg(LOG_TYPE.INFO, moduleName, s"Restore blocks error : ${e.toString}", selfAddr)
          //TODO kami 将来需要处理restore失败的情况
          case _ => //ignore
            re = 2
        }
    re
  }
  
  def RefreshCacheBuffer(blkRestore: BlockRestore)={
    pe.removeTrans(blkRestore.blk.transactions)
    pe.setMerk(dataaccess.GetComputeMerkle4String)
    pe.setCacheHeight(dataaccess.getBlockHeight())
    pe.setCurrentBlockHash(dataaccess.getBlockChainInfo().currentBlockHash.toStringUtf8)
  }
  
  def RestoreBlock(blkRestore: BlockRestore):Integer={
    var re : Integer = 0
    val local = dataaccess.getBlockChainInfo()
    if(local.currentBlockHash != ByteString.EMPTY){
        if(pe.getCurrentBlockHash.equalsIgnoreCase("0")){
          pe.setCurrentBlockHash(local.currentBlockHash.toStringUtf8())
        }
    }
    pe.addCacheBlkNum()
    if (blkRestore.blk.previousBlockHash.toStringUtf8 == pe.getCurrentBlockHash ||
        (local.height == 0 && blkRestore.blk.previousBlockHash == ByteString.EMPTY)) {
      if(SaveBlock(blkRestore) == 0){
        //success
        pe.rmCacheBlkNum()
        NoticeVoteModule()
      }else{
        println("block restor is failed in persistence module,must restart node")
        throw new Exception("block restore is failed")
      }
    }else{
      if(blkRestore.height <= local.height){
        pe.rmCacheBlkNum()
        RefreshCacheBuffer(blkRestore)
        logMsg(LOG_TYPE.INFO, moduleName, s"Block has already been stored", selfAddr)
        NoticeVoteModule()
      }else{
        if(blkRestore.blk.previousBlockHash.toStringUtf8 == local.previousBlockHash.toStringUtf8()){
          pe.rmCacheBlkNum()
          RefreshCacheBuffer(blkRestore)
          logMsg(LOG_TYPE.INFO, moduleName, s"Block create is error,hash value not equal ", selfAddr)
          NoticeVoteModule()
        }else{
          if(blkRestore.height-local.height > 1){
            pe.rmCacheBlkNum()
            val tt = blkRestore.height-local.height
            var i = 0;
            //
            for( i <- 1 to tt.asInstanceOf[Int]){
              context.parent ! TargetBlock(local.height+i, blkRestore.blker)
            }
            
          }else{
            retryCount += 1
            if(retryCount > retryLimit){
              retryCount = 0
              pe.rmCacheBlkNum()
              RefreshCacheBuffer(blkRestore)
              logMsg(LOG_TYPE.INFO, moduleName, s"Block create is error,repeat save error ", selfAddr)
              NoticeVoteModule()
            }else{
              pe.rmCacheBlkNum()
              self ! blkRestore
            }
          }
        }
      }
    }
    re
  }
  
  def NoticeVoteModule()={
    logMsg(LOG_TYPE.INFO, moduleName, s"Merk(After presistence): ${pe.getMerk}", selfAddr)
    if (pe.getCacheBlkNum() == 0){
      logMsg(LOG_TYPE.INFO, moduleName, s"presistence is over", selfAddr)
      if(!pe.getIsSync()){
        logMsg(LOG_TYPE.INFO, moduleName, s"presistence is over,this is startup vote", selfAddr)
        pe.setIsBlocking(false)
        pe.setEndorState(false)
        pe.setIsBlockVote(false)
        getActorRef(pe.getSysTag, ActorType.VOTER_MODULE) ! NextVote(true,0)
      }
    }
  }

  override def receive = {
    case blkRestore: BlockRestore =>
      RestoreBlock(blkRestore)
    case _ => //ignore
  }
}