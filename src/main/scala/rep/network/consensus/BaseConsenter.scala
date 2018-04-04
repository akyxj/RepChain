package rep.network.consensus

/**
  * Created by User on 2017/9/23.
  */
trait BaseConsenter {
  /**
    * 共识出初始化
    */
  def init()

  /**
    * 初始化完成，开始同步
    */
  def initFinished()


  def start()

  /**
    * 下一次共识
    */
  def nextConsensus()
}
