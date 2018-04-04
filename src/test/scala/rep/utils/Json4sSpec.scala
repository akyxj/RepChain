package rep.utils
import rep.sc.Shim.Oper

import org.scalatest._
import prop._
import scala.collection.immutable._

import org.scalatest._
import prop._
import scala.collection.immutable._
import scala.math.BigInt

class Json4sSpec extends PropSpec with TableDrivenPropertyChecks with Matchers {
  val ol1 = List(Oper("key1",null,BigInt(8).toByteArray),Oper("key1",BigInt(8).toByteArray,BigInt(8).toByteArray))
  val ol2 = List(Oper("key2",null,BigInt(8).toByteArray),Oper("key2",BigInt(8).toByteArray,BigInt(8).toByteArray))
  
  val examples =
    Table(
    "olist",  // First tuple defines column names
    ol1,ol2
   )
  property("Oper list can be convert to json and from json") {
    forAll(examples) { olist =>
      val jstr = Json4s.compactJson(olist)
      val jobj = Json4s.parseJson[List[Oper]](jstr)
      jobj.head.key should be( olist.head.key)
      jobj.head.newValue should be (olist.head.newValue)
   }
  }
}