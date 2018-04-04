package rep.crypto

trait CryptographicHash {

  type Digest = Array[Byte]
  type Message = Array[Byte]

  val DigestSize: Int // in bytes

  def apply(input: Message): Digest = hash(input)

  def apply(input: String): Digest = hash(input.getBytes)

  def hash(input: Message): Digest

  def hash(input: String): Digest = hash(input.getBytes)
}

