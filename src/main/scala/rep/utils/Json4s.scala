package rep.utils

object Json4s {
  import org.json4s._
  def encodeJson(src: Any): JValue = {
    import org.json4s.JsonDSL.WithDouble._
    import org.json4s.jackson.Serialization
    implicit val formats = Serialization.formats(NoTypeHints)
    Extraction.decompose(src)
  }  
  def compactJson(src: Any): String = {
    import org.json4s.jackson.Serialization
    import org.json4s.JsonDSL._
    import org.json4s.jackson.JsonMethods._

  implicit val formats = Serialization.formats(NoTypeHints)
    compact(render(Extraction.decompose(src)))
  }  
  def parseJson[T: Manifest](src: String):T  = {
    import org.json4s.jackson.JsonMethods._
    implicit val formats = DefaultFormats 
    
    val json = parse(src)
    json.extract[T]
  }  
  def parseAny(src: String):Any  = {
    import org.json4s.jackson.JsonMethods._
    implicit val formats = DefaultFormats 
    try{
      val json = parse(src)
      json.extract[Any]
    }catch{
      case e => src
    }
  }  
  
}