package rep.network.consensus.vote

import rep.crypto.Sha256

import scala.collection.mutable

/**
  * 系统默认
  * 候选人竞争实现
  * 出块人竞争实现
  * Created by User on 2017/5/15.
  */
//TODO kami 应该在init的时候载入一个实现函数或者类。然后调用方法。写的更通用一些
trait CRFDVoter extends VoterBase {

  /**
    * 获取出块人
    * @param nodes
    * @param position
    * @tparam T
    * @return
    */
  override def blocker[T](nodes: Set[T], position:Int): Option[T] = {
    if (nodes.nonEmpty&&position<nodes.size) Option(nodes.head) else None
  }

  /**
    * 获取候选人
    * @param nodes
    * @param seed 随机种子(这里写的不太好，应该改一下）
    * @tparam T
    * @return
    */
  override def candidators[T](nodes: Set[T], seed: Array[Byte]): Set[T] = {
    var nodesSeq = nodes.toSeq
    var len = nodes.size / 2 + 1
    val min_len = 4
    len = if(len<min_len){
      if(nodes.size < min_len) nodes.size
      else min_len
    }
    else len
    if(len<4){
      Set.empty
    }
    else{
      var candidate = mutable.Seq.empty[T]
      var index = 0
      var hashSeed = seed

      while (candidate.size < len) {
        if (index >= hashSeed.size) {
          hashSeed = Sha256.hash(hashSeed)
          index = 0
        }
        //应该按位来计算
        if ((hashSeed(index) & 1) == 1) {
          candidate = (candidate :+ nodesSeq(index % (nodesSeq.size)))
          nodesSeq = (nodesSeq.toSet - nodesSeq(index % (nodesSeq.size))).toSeq
        }
        index += 1
      }
      candidate.toSet
    }
  }

  /**
    * 根据候选人序列check背书是都完成
    * @param candidators
    * @param number
    * @tparam T
    * @return
    */
  /*def checkEndorsement[T](candidators: Set[T], number: Int): Boolean = {
    if (number > candidators.size / 2) true else false
  }*/

}
