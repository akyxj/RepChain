package rep.utils

import spray.json._

object JsonSpray {
  implicit object AnyJsonFormat extends JsonFormat[Any] {
      def write(x: Any) = x match {
        case null => JsNull
        case None => JsNull
        case n: Int => JsNumber(n)
        case s: String => JsString(s)
        case b: Boolean if b == true => JsTrue
        case b: Boolean if b == false => JsFalse
      }
      def read(value: JsValue) = value match {
        case JsNumber(n) => n.intValue()
        case JsString(s) => s
        case JsTrue => true
        case JsFalse => false
      }
  }
}