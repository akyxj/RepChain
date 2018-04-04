package rep.storage
import rep.utils.RepLogging
import rep.protos._


class FakeStorage() extends Storage[String,Array[Byte]] with RepLogging { 
  import FakeStorage._
  
  private val m = scala.collection.mutable.Map[Key,Value]()
  
  override def set(key:Key, value:Value): Unit = {
    val me = this
    log.info(s"set state:$key $value $me")
    m.put(key,value)
  }
  override def get(key:Key): Option[Value] = {
    m.get(key)
  }  
  override def commit(): Unit={    
  }
  override def merkle():Array[Byte]={
    null
  }
}

object FakeStorage {
 type Key = String  
 type Value = Array[Byte]
}
