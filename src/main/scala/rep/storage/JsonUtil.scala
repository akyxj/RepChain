package rep.storage

import scala.collection.mutable;
import scala.util.parsing.json._

object JsonUtil {
   
  
  
  def map2Json(map : Map[String,Any]) : String = {
    val json = JSONObject(map)
    val jsonString = json.toString()
    jsonString
  }
 
  def json2Map(jsonstr : String) : Map[String,Any] = {
    val json:Option[Any] = JSON.parseFull(jsonstr)
    val map:Map[String,Any] = json.get.asInstanceOf[Map[String, Any]]
    map
  }
  
  def hashmap2Json(map : mutable.HashMap[String,Any]) : String = {
    val jsonString = map2Json(map.toMap)
    jsonString
  }
 
  def json2HashMap(jsonstr : String) : mutable.HashMap[String,Any] = {
    var map2 = new mutable.HashMap[String, Any];
    val json:Option[Any] = JSON.parseFull(jsonstr)
    if(json != null){
      val map:Map[String,Any] = json.get.asInstanceOf[Map[String, Any]]
      map.foreach(f=>{map2.put(f._1, f._2)})
    }
    map2
  }
}