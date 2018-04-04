package rep.crypto

import java.security._

trait SignFunc {
  def sign(privateKey: PrivateKey, message: Array[Byte]): Array[Byte] 
  def verify(signature: Array[Byte], message: Array[Byte], publicKey: PublicKey): Boolean
}