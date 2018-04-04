package rep.network.tools.register

import akka.actor.ActorRef

import scala.collection.mutable

/**
  * Single System Actor Reference Register
  * Created by User on 2017/9/22.
  */
class ActorRegister {
  private val actorList = mutable.HashMap[Int, ActorRef]()

  def register(actorName:Int, actorRef: ActorRef)={
    actorList.put(actorName,actorRef)
  }

  def getActorRef(actorName:Int) = actorList.get(actorName)

  def unregister(actorName:Int) = actorList.remove(actorName)

}
