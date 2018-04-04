package rep.utils

import java.io.{ByteArrayInputStream, ByteArrayOutputStream, ObjectInputStream, ObjectOutputStream}
import java.security.cert.Certificate

import rep.crypto.ECDSASign
import Json4s._
import rep.crypto.BytesHex._

/**
  * Created by User on 2017/6/9.
  */
object SerializeUtils {
  /**
    * Java 序列化方法
    * @param value
    * @return
    */
  def serialise(value: Any): Array[Byte] = {
    val stream: ByteArrayOutputStream = new ByteArrayOutputStream()
    val oos = new ObjectOutputStream(stream)
    oos.writeObject(value)
    oos.close
    stream.toByteArray
  }

  /**
    * Java 反序列化方法
    * @param bytes
    * @return
    */
  def deserialise(bytes: Array[Byte]): Any = {
    val ois = new ObjectInputStream(new ByteArrayInputStream(bytes))
    val value = ois.readObject
    ois.close
    value
  }

  /**
    * Json对象序列化
    * @param value
    * @return
    */
  def serialiseJson(value: Any): Array[Byte] = {
    val json = compactJson(value) 
    //println(s"json $json")
    json.getBytes
  }

  /**
    * Json对象反序列化
    * @param bytes
    * @return
    */
  def deserialiseJson(bytes: Array[Byte]): Any = {
    if(bytes==null)
      return null;
    val json = new String(bytes)
    parseAny(json)
  }

  def main(args: Array[String]): Unit = {
    import rep.sc.Shim.Oper
    
    ECDSASign.apply("1", "jks/mykeystore_1.jks", "123", "jks/mytruststore.jks", "changeme")
    ECDSASign.preLoadKey("1")
    val c = ECDSASign.getCert("1")
    val cA = serialise(c)
    val cert = deserialise(cA).asInstanceOf[Certificate]
    println(cert.getPublicKey==c.getPublicKey)
  }

}
