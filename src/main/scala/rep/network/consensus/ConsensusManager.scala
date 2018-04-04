package rep.network.consensus

import akka.actor.{Actor, ActorRef, Props}
import com.typesafe.config.Config
import rep.network.consensus.CRFD.{ConsensusInitFinish, InitCRFD}
import rep.network.consensus.ConsensusManager.ConsensusType

/**
  * Created by User on 2017/9/23.
  */

object ConsensusManager {
  def props(name:String, config: Config): Props = Props(classOf[ ConsensusManager ],name, config)
  case object ConsensusType {
    val CRFD = 1
    val OTHER = 2
  }

}

class ConsensusManager(name:String, conf: Config) extends Actor{

  private val consensusType = init(conf)
  private var consensusActor:ActorRef = null

  generateConsensus()

  initConsensus()

  def init(config: Config): Int = {
    val typeConsensus = config.getString("system.consensus.type")
    typeConsensus match {
      case "CRFD" =>
        ConsensusManager.ConsensusType.CRFD
      case _ =>
        //ignore
        ConsensusManager.ConsensusType.OTHER
    }
  }

  def generateConsensus() = {
    consensusType match {
      case ConsensusType.CRFD =>
        consensusActor = context.actorOf(CRFD.props("consensus-CRFD"),"consensus-CRFD")
      case ConsensusType.OTHER => //ignore
    }
  }

  def initConsensus() ={
    consensusType match {
      case ConsensusType.CRFD =>
        consensusActor ! InitCRFD
    }
  }

  def startConsensus = ???

  override def receive: Receive = {

    case ConsensusInitFinish =>
      context.parent ! ConsensusInitFinish

    case _ => //ignore
  }
}
