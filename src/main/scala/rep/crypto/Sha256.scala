package rep.crypto
import java.security.MessageDigest
import com.google.protobuf.ByteString

object Sha256 extends CryptographicHash{
  override val DigestSize: Int = 32
  def hash(input: Array[Byte]): Array[Byte] = MessageDigest.getInstance("SHA-256").digest(input)
  def hashstr(input: Array[Byte]):String ={
    BytesHex.bytes2hex(hash(input))
  }
  def hashstr(input: String):String ={
    val iptb = ByteString.copyFromUtf8(input)
    BytesHex.bytes2hex(hash(iptb.toByteArray()))
  }

}