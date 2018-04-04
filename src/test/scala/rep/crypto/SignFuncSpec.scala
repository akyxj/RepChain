package rep.crypto

import org.scalatest.prop.{GeneratorDrivenPropertyChecks, PropertyChecks}
import org.scalatest.{Matchers, PropSpec}
import rep.crypto._
import java.io._

class SignFuncSpec extends PropSpec
with PropertyChecks
with GeneratorDrivenPropertyChecks
with Matchers {

  property("signed message should be verifiable with appropriate public key") {
    forAll { (seed1: Array[Byte], seed2: Array[Byte],
              message1: Array[Byte], message2: Array[Byte]) =>
      whenever(!seed1.sameElements(seed2) && !message1.sameElements(message2)) {
        //c4w for keypair from jks
        val (skey1,pkey1) = ECDSASign.getKeyPairFromJKS(new File("jks/mykeystore_1.jks"),"123","1")
        val (skey2,pkey2) = ECDSASign.getKeyPairFromJKS(new File("jks/mytruststore.jks"),"changeme","1")
        val (skey3,pkey3) = ECDSASign.getKeyPairFromJKS(new File("jks/mytruststore.jks"),"changeme","2")
        
        val sig = ECDSASign.sign(skey1, message1)
        
        ECDSASign.verify(sig, message1, pkey1) should be (true)
        ECDSASign.verify(sig, message1, pkey2) should be (true)
        ECDSASign.verify(sig, message2, pkey3) shouldNot be (true)
        (pkey1 == pkey2) should be (true)
        (pkey1 == pkey3) shouldNot be (true)

      }
    }
  }
}