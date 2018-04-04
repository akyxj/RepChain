package rep.network.cluster

import akka.actor.Address
import rep.protos.peer.BlockchainInfo

import scala.collection.mutable



/**
  * 提供集群相关功能组件伴生对象
  *
  * @author shidianyue
  * @version 1.0
  * @since 1.0
  **/
object ClusterHelper {

  /**
    * 是否是当前候选人
    * @param nodePath
    * @param candidates
    * @return
    */
  def isCandidateNow(nodePath: String, candidates: Set[ Address ]): Boolean = {
    candidates.exists(node => nodePath.indexOf(node.toString) != -1)
  }

  /**
    * 是否是当前出块人
    * @param nodePath
    * @param blockerPath
    * @return
    */
  def isBlocker(nodePath: String, blockerPath: String): Boolean = {
    nodePath.indexOf(blockerPath) != -1
  }

  /**
    * System唯一性key
    *
    * @param ip
    * @param port
    * @return
    */
  def getNodeSystemUniqueId(ip: String, port: String): String = {
    ip + ":" + port
  }

  /**
    * 获得多数一致性节点地址
    * 地址：对象
    * @param nodes
    * @param clusterInfo
    * @return
    */
  def filtWithMajorStatusForClusterNodes(nodes: Set[ Address ], clusterInfo: Map[ String, BlockchainInfo ]): Set[ Address ] = {
    val usableNode = getMajorNodes(clusterInfo)
    println("Filted size :" + usableNode.size)
    nodes.filter(node => {
      val nodepath = node.toString
      usableNode.contains(nodepath.substring(nodepath.indexOf("@") + 1))
    })
  }

  /**
    * 获得多数一致性节点的地址
    * 地址：Ip - port
    * @param clusterInfo
    * @return
    */
  def getMajorNodes(clusterInfo: Map[ String, BlockchainInfo ]): Set[ String ] = {
    val diffInfoMap = mutable.HashMap[ String, Int ]()
    var max = 0
    var maxMerk = ""
    println("Total info size: " + clusterInfo.size)
    clusterInfo.foreach(info => {
      val id = info._2.currentWorldStateHash.toStringUtf8
      if (diffInfoMap.contains(id)) {
        diffInfoMap.put(id, diffInfoMap.get(id).get + 1)
        if (max < diffInfoMap.get(id).get) {
          max = diffInfoMap.get(id).get
          if (!maxMerk.equals(id)) maxMerk = id
        }
      }
      else diffInfoMap.put(id, 1)
    })
    clusterInfo.filter(info => info._2.currentWorldStateHash.toStringUtf8.equals(maxMerk)).keySet
  }

  /**
    * 判断节点间chain状态是否相同
    * @param src
    * @param target
    * @return
    */
  def isSameChainStatus(src: BlockchainInfo, target: BlockchainInfo): Boolean = {
    //TODO kami 其实currentB
    if (src.currentWorldStateHash.toStringUtf8 == target.currentWorldStateHash.toStringUtf8) true
    else false
  }

  /**
    * 判断是否是种子节点
    * 目前并不完善
    * @param sysName
    * @return
    */
  def isSeedNode(sysName:String):Boolean ={
    sysName == "1"
  }
}
