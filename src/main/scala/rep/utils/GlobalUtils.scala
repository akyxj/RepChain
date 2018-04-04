package rep.utils

/**
  * 全局变量
  * Created by User on 2017/5/22.
  */
object GlobalUtils {
  case object BlockEvent{
    //同步信息广播
    val CHAIN_INFO_SYNC = "CHAIN_INFO_SYNC"
    //创建block
    val CREATE_BLOCK = "CREATE_BLOCK"
    //出块人
    val BLOCKER = "BLOCKER"
    val BLOCK_HASH = "BLOCK_HASH"
    //创世块
    val GENESIS_BLOCK = "GENESIS_BLOCK"
    //出块成功
    val NEW_BLOCK = "NEW_BLOCK"
    //背书请求
    val BLOCK_ENDORSEMENT = "BLOCK_ENDORSEMENT"
    //背书反馈
    val ENDORSEMENT = "ENDORSEMENT"
    //出块确认
    val ENDORSEMENT_CHECK = "ENDORSEMENT_CHECK"
    //出块确认反馈
    val ENDORSEMENT_RESULT = "ENDORSEMENT_RESULT"
    //同步区块
    val BLOCK_SYNC = "BLOCK_SYNC"
    //同步区块数据
    val BLOCK_CHAIN = "BLOCK_CHAIN"
  }

  case object ActorType{
    val MEMBER_LISTENER = 1
    val MODULE_MANAGER = 2
    val API_MODULE = 3
    val PEER_HELPER = 4
    val BLOCK_MODULE = 5
    val PRELOADTRANS_MODULE = 6
    val ENDORSE_MODULE = 7
    val VOTER_MODULE = 8
    val SYNC_MODULE = 9
    val TRANSACTION_POOL = 10
    val PERSISTENCE_MODULE = 11
    val CONSENSUS_MANAGER = 12
    val STATISTIC_COLLECTION = 13
  }

  case object EventType{
    val PUBLISH_INFO = 1
    val RECEIVE_INFO = 2
  }

  //

  val AppConfigPath = "application.conf"
  val SysConfigPath = "conf/system.conf"


}
