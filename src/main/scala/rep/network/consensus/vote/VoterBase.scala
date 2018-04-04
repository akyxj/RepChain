package rep.network.consensus.vote




/**
  * 特质:全局节点控制
  * Created by User on 2017/5/15.
  */
trait VoterBase {

  
  
  
  /**
    * 获取出块人（竞争胜出者）
    * @param nodes
    * @tparam T
    * @return
    */
  def blocker[T](nodes:Set[T], position:Int):Option[T]

  /**
    * 获取候选人节点
    * @param nodes
    * @tparam T
    * @param seed 随机种子(这里写的不太好，应该改一下）
    * @return
    */
  def candidators[T](nodes:Set[T], seed:Array[Byte]):Set[T]
}
