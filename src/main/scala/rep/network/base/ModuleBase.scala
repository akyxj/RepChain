package rep.network.base

import akka.actor.{Actor, Address, ActorRef}
import rep.app.system.ClusterSystem
import rep.network.cluster.ClusterActor
import rep.network.tools.PeerExtension
import rep.network.tools.register.ActorRegister
import rep.utils.{RepLogging, TimeUtils}
import rep.utils.RepLogging.LogTime
import rep.crypto.Sha256
import scala.collection.mutable


/**
  * 模块基础类伴生对象
  *
  * @author shidianyue
  * @version 1.0
  * @since 1.0
  **/
object ModuleBase {
  def registerActorRef(sysTag: String, actorType: Int, actorRef: ActorRef) = {
    ClusterSystem.getActorRegister(sysTag) match {
      case None =>
        val actorRegister = new ActorRegister()
        actorRegister.register(actorType, actorRef)
        ClusterSystem.register(sysTag, actorRegister)
      case actR =>
        actR.get.register(actorType, actorRef)
    }
  }
}

/**
  * 系统模块基础类
  *
  * @author shidianyue
  * @version 1.0
  * @since 1.0
  * @param name 模块名称
  **/

abstract class ModuleBase(name: String) extends Actor with ModuleHelper with ClusterActor with BaseActor with RepLogging {
  val logPrefix = name

  /**
    * 日志封装
    *
    * @param lOG_TYPE
    * @param msg
    */
  def logMsg(lOG_TYPE: Int, msg: String): Unit = {
    super.logMsg(lOG_TYPE, pe.getSysTag + "-" + name, msg, selfAddr)
  }

  /**
    * 事件时间戳封装
    *
    * @param msg
    * @param step
    * @param actorRef
    */
  def logTime(msg: String, step: Int, actorRef: ActorRef): Unit = {
    actorRef ! LogTime(pe.getSysTag + "-" + name, s"Step_${step} - " + msg, TimeUtils.getCurrentTime(), selfAddr)
  }
}

/**
  * 模块帮助功能接口
  *
  * @author shidianyue
  * @version 1.0
  * @since 1.0
  **/
trait ModuleHelper extends Actor {
  val pe = PeerExtension(context.system)

  /**
    * 从注册中心获取actor引用
    * @param sysTag
    * @param actorType
    * @return
    */
  def getActorRef(sysTag: String, actorType: Int): ActorRef = {
    ClusterSystem.getActorRegister(sysTag).getOrElse(None) match {
      case None => self
      case actorReg: ActorRegister => actorReg.getActorRef(actorType).getOrElse(None) match {
        case None => self
        case actorRef: ActorRef => actorRef
      }
    }
  }

  /**
    * 从注册中心获取actor引用
    * @param actorType
    * @return
    */
  def getActorRef(actorType: Int): ActorRef = {
    ClusterSystem.getActorRegister(pe.getSysTag).getOrElse(None) match {
      case None => self
      case actorReg: ActorRegister => actorReg.getActorRef(actorType).getOrElse(None) match {
        case None => self
        case actorRef: ActorRef => actorRef
      }
    }
  }

  /**
    * 向注册中心注册actor引用
    * @param sysTag
    * @param actorType
    * @param actorRef
    * @return
    */
  def registerActorRef(sysTag: String, actorType: Int, actorRef: ActorRef) = {
    ClusterSystem.getActorRegister(sysTag) match {
      case None =>
        val actorRegister = new ActorRegister()
        actorRegister.register(actorType, actorRef)
        ClusterSystem.register(sysTag, actorRegister)
      case actR =>
        actR.get.register(actorType, actorRef)
    }
  }

  /**
    * 向注册中心注册actor引用
    * @param actorType
    * @param actorRef
    * @return
    */
  def registerActorRef(actorType: Int, actorRef: ActorRef) = {
    ClusterSystem.getActorRegister(pe.getSysTag) match {
      case None =>
        val actorRegister = new ActorRegister()
        actorRegister.register(actorType, actorRef)
        ClusterSystem.register(pe.getSysTag, actorRegister)
      case actR =>
        actR.get.register(actorType, actorRef)
    }
  }
  
 private def blocker(nodes: Set[Address], position:Int): Address = {
    if (nodes.nonEmpty&&position<nodes.size) nodes.head else null
  }
  
 private def candidators(nodes: Set[Address], seed: Array[Byte]): Set[Address] = {
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
      var candidate = mutable.Seq.empty[Address]
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
  
  def virtualBallotForBlocker(seed:Array[Byte],position:Int,selfAddress:Address,isSeedNode:Boolean):Address={
    var candidatorCur :Set[Address] = Set.empty[Address]
    candidatorCur = candidators(pe.getStableNodes, Sha256.hash(seed))
    if (!candidatorCur.isEmpty) {
        if(pe.getCacheHeight() == 1 ){
          if(isSeedNode){
           selfAddress
          }else{
            null
          }
        }else{
          var r = blocker(candidatorCur, position)
          r
        }
    }else{
       null
    }
  }
  
  /*def ballotForBlocker(seed:Array[Byte],position:Int,selfAddress:Address,isSeedNode:Boolean):Address={
    var candidatorCur :Set[Address] = Set.empty[Address]
    candidatorCur = candidators(pe.getStableNodes, Sha256.hash(seed))
    if (!candidatorCur.isEmpty) {
        pe.resetCandidator(candidatorCur)
        if(pe.getCacheHeight() == 1 ){
          if(isSeedNode){
           selfAddress
          }else{
            null
          }
        }else{
          var r = blocker(candidatorCur, position)
          if(r != null){
            pe.resetBlocker(r)
          }
          r
        }
    }else{
       null
    }
  }*/
  
  
}