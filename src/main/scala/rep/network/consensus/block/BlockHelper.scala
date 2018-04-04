package rep.network.consensus.block

import com.google.protobuf.ByteString
import com.google.protobuf.timestamp.Timestamp
import com.trueaccord.scalapb.json.JsonFormat
import rep.app.conf.SystemProfile
import rep.crypto.{ECDSASign, Sha256}
import rep.protos.peer.{Block, Endorsement, Transaction}
import rep.utils.TimeUtils

/**
  *
  * Created by User on 2017/7/3.
  */
/**
  * 出块辅助类
  *
  * @author shidianyue
  * @version 1.0
  * @since 1.0
  **/
object BlockHelper {
  /**
    * 背书块
    * @param blkHash
    * @param alise
    * @return
    */
  def endorseBlock(blkHash:Array[Byte], alise:String):Endorsement ={
    val (priK, pubK, cert) = ECDSASign.getKeyPair(alise)
    Endorsement(ByteString.copyFromUtf8(ECDSASign.getBitcoinAddrByCert(cert)),
      ByteString.copyFrom(ECDSASign.sign(priK, blkHash)))
  }

  /**
    * 对块的信息进行校验
    * @param blkHash
    * @return
    */
  def checkBlockContent(endor:Endorsement, blkHash: Array[Byte]): Boolean = {
    //获取出块人的背书信息
    val certTx = ECDSASign.getCertByBitcoinAddr(endor.endorser.toStringUtf8)
    if(certTx.getOrElse(None)!=None){
      //    val certTx = SerializeUtils.deserialise(endor.endorser.toByteArray).asInstanceOf[Certificate]
      val alias = ECDSASign.getAliasByCert(certTx.get).getOrElse(None)
      if (alias == None) false
      else {
        ECDSASign.verify(endor.signature.toByteArray, blkHash, certTx.get.getPublicKey)
      }
    }
    else false
  }

  /**
    * Collect the trans from cache
    * Limit the size with Config param
    *
    * @param trans
    * @return
    */
  def cutTransaction(trans: Seq[Transaction]): Seq[Transaction] = {
    val result = trans.take(if (SystemProfile.getLimitBlockTransNum > trans.length) trans.length else SystemProfile.getLimitBlockTransNum)
    result
  }

  /**
    * 生成原型块
    *
    * @param preBlkHash
    * @param trans
    * @return
    */
  def createPreBlock(preBlkHash: String, trans: Seq[Transaction]): Block = {
    val millis = TimeUtils.getCurrentTime()
    //TODO kami 不应该一刀切，应该针对不同情况采用不同的整合trans的策略
    //TODO kami 出块的时候需要验证交易是否符合要求么？（在内部节点接收的时候已经进行了验证）
    //先这样做确保出块的时候不超出规格
    val blk = new Block(1, Option(Timestamp(millis / 1000, ((millis % 1000) * 1000000).toInt)),
      trans, _root_.com.google.protobuf.ByteString.EMPTY, _root_.com.google.protobuf.ByteString.EMPTY, Seq())
    blk.withPreviousBlockHash(ByteString.copyFromUtf8(preBlkHash))
  }

  /**
    * Create Genesis Block with config info
    *
    * @return
    */
  def genesisBlockCreator(): Block = {
    //TODO kami priHash Empty
    val blkJson = scala.io.Source.fromFile("json/gensis.json")
    val blkStr = try blkJson.mkString finally blkJson.close()
    val gen_blk = JsonFormat.fromJsonString[Block](blkStr)
    gen_blk
  }

  /**
    * Check the endorsement state
    * Whether its size meet the requirement of candidate
    *
    * @param endorseNum
    * @param candiNum
    * @return
    */
  def checkCandidate(endorseNum: Int, candiNum: Int): Boolean = {
//    if ((endorseNum - 1) > ((candiNum-1) / 2)) true else false
    if ((endorseNum - 1) >= Math.floor(((candiNum)*1.0) / 2)) true else false
  }

  /**
    * 获取块的hash
    * @param blk
    * @return
    */
  def getBlkHash(blk:Block):String = {
    Sha256.hashstr(blk.toByteArray)
  }

}
