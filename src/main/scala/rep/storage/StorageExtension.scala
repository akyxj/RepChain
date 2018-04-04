package rep.storage

import akka.actor.ActorSystem
import akka.actor.Extension
import akka.actor.ExtensionId
import akka.actor.ExtensionIdProvider
import akka.actor.ExtendedActorSystem

 import FakeStorage._
 
class StorageExtensionImpl extends Extension{
  //Since this Extension is a shared instance
  // per ActorSystem we need to be threadsafe
  private val sr = new FakeStorage()
  def set(key:Key, value:Value):Unit={
    sr.set(key, value)
  }
  def get(key:Key): Option[Value] = {
    sr.get(key)
  }
  def commit(): Unit={    
  }
  def merkle():Array[Byte]={
    null
  }
  //need some moew
}

object StorageExtension
  extends ExtensionId[StorageExtensionImpl]
  with ExtensionIdProvider {
  //The lookup method is required by ExtensionIdProvider,
  // so we return ourselves here, this allows us
  // to configure our extension to be loaded when
  // the ActorSystem starts up
  override def lookup = StorageExtension

  //This method will be called by Akka
  // to instantiate our Extension
  override def createExtension(system: ExtendedActorSystem) = new StorageExtensionImpl

  /**
   * Java API: retrieve the Count extension for the given system.
   */
  override def get(system: ActorSystem): StorageExtensionImpl = super.get(system)
}