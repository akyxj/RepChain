package rep.storage

trait Storage[Key, Value] {

  def set(key: Key, value: Value): Unit

  def get(key: Key): Option[Value]

  def containsKey(key: Key): Boolean = get(key).isDefined
  
  def commit(): Unit
  def merkle():Array[Byte]
}