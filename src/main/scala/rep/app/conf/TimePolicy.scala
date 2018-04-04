package rep.app.conf

import com.typesafe.config.Config

/**
  * 时间策略相关的配置信息类
  * @author shidianyue
  * @version	1.0
  * @since	1.0
  * */

object TimePolicy {

  private[this] var _TIMEOUT_BLOCK: Int = 0//出块超时
  private[this] var _TIMEOUT_ENDORSE: Int = 0//背书超时
  private[this] var _TIMEOUT_PRELOAD_TRANS: Int = 0//预执行超时
  private[this] var _TIMEOUT_SYNC_CHAIN: Int = 0//同步超时
  private[this] var _VOTE_RETYR_DELAY: Long = 0//投票延迟
  private[this] var _SYS_NODE_STABLE_DELAY: Long = 0//节点稳定延迟
  private[this] var _STABLE_TIME_DUR: Int = 0
  private[this] var _VOTE_WAITING_DELAY: Long = 0//投票长时等待

  def getVoteWaitingDelay = VOTE_WAITING_DELAY

  def VOTE_WAITING_DELAY: Long = _VOTE_WAITING_DELAY

  def VOTE_WAITING_DELAY_=(value: Long): Unit = {
    _VOTE_WAITING_DELAY = value
  }

  private def TIMEOUT_BLOCK: Int = _TIMEOUT_BLOCK

  private def TIMEOUT_BLOCK_=(value: Int): Unit = {
    _TIMEOUT_BLOCK = value
  }

  def getTimeOutBlock = TIMEOUT_BLOCK

  private def TIMEOUT_ENDORSE: Int = _TIMEOUT_ENDORSE

  private def TIMEOUT_ENDORSE_=(value: Int): Unit = {
    _TIMEOUT_ENDORSE = value
  }

  def getTimeoutEndorse = TIMEOUT_ENDORSE

  private def TIMEOUT_PRELOAD_TRANS: Int = _TIMEOUT_PRELOAD_TRANS

  private def TIMEOUT_PRELOAD_TRANS_=(value: Int): Unit = {
    _TIMEOUT_PRELOAD_TRANS = value
  }

  def getTimeoutPreload = TIMEOUT_PRELOAD_TRANS

  private def TIMEOUT_SYNC_CHAIN: Int = _TIMEOUT_SYNC_CHAIN

  private def TIMEOUT_SYNC_CHAIN_=(value: Int): Unit = {
    _TIMEOUT_SYNC_CHAIN = value
  }

  def getTimeoutSync = TIMEOUT_SYNC_CHAIN

  private def VOTE_RETYR_DELAY: Long = _VOTE_RETYR_DELAY

  private def VOTE_RETYR_DELAY_=(value: Long): Unit = {
    _VOTE_RETYR_DELAY = value
  }

  def getVoteRetryDelay = VOTE_RETYR_DELAY

  private def SYS_NODE_STABLE_DELAY: Long = _SYS_NODE_STABLE_DELAY

  private def SYS_NODE_STABLE_DELAY_=(value: Long): Unit = {
    _SYS_NODE_STABLE_DELAY = value
  }

  def getSysNodeStableDelay = SYS_NODE_STABLE_DELAY

  private def STABLE_TIME_DUR: Int = _STABLE_TIME_DUR

  private def STABLE_TIME_DUR_=(value: Int): Unit = {
    _STABLE_TIME_DUR = value
  }

  def getStableTimeDur = STABLE_TIME_DUR

  /**
    * 初始化时间相关策略
    * @param config
    */
  def initTimePolicy(config: Config): Unit = {
    VOTE_RETYR_DELAY = config.getLong("system.time.block.vote_retry_delay")
    VOTE_WAITING_DELAY = config.getLong("system.time.block.waiting_delay")
    SYS_NODE_STABLE_DELAY = config.getLong("system.cluster.node_stable_delay")
    STABLE_TIME_DUR = config.getInt("system.time.stable_time_dur")
    val policyType = config.getInt("system.time.timeout_policy_type")

    policyType match {
      case PolicyType.MANUAL =>
        TIMEOUT_BLOCK = config.getInt("system.time.timeout.block")
        TIMEOUT_ENDORSE = config.getInt("system.time.timeout.endorse")
        TIMEOUT_PRELOAD_TRANS = config.getInt("system.time.timeout.transaction_preload")
        TIMEOUT_SYNC_CHAIN = config.getInt("system.time.timeout.sync_chain")
      case PolicyType.AUTO =>
        //这里我们根据经验设定算法，通过基准时间（一个出块时间），来配置其他的超时时间
        //类似于默认
        val basePre = config.getInt("system.time.timeout.base_preload")
        val baseSync = config.getInt("system.time.timeout.base_sync")
        val baseAdd = config.getInt("system.time.timeout.base_addition")
        TIMEOUT_PRELOAD_TRANS = basePre
        TIMEOUT_ENDORSE = basePre*2
        TIMEOUT_BLOCK = (3 * basePre + baseAdd)
        TIMEOUT_SYNC_CHAIN = baseSync
    }
  }
}

/**
  * 时间策略类型
  */
case object PolicyType {
  val MANUAL = 1//手动调整
  val AUTO = 0//自动配置（推荐）
}