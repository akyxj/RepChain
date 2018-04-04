package rep.sc.contract
import scala.reflect.runtime.currentMirror
import scala.tools.reflect.ToolBox
import org.json4s._
import org.json4s.jackson.JsonMethods._


object ContractTest {
  implicit val formats = DefaultFormats
  def main(args: Array[String]) {
    //在运行期编译和加载scala类，并调用其重载方法
    val s1 =
      """
import rep.sc.contract._
import rep.protos.peer.Transaction
import rep.sc.Sandbox.DoTransactionResult
import org.json4s._
import org.json4s.jackson.JsonMethods._

class ContractAssets2 extends Contract{
  implicit val formats = DefaultFormats
  
    def init(ctx: ContractContext){      
      println(s"tid: $ctx.t.txid")
    }
    
    def set(ctx: ContractContext, data:Map[String,Int]):Object={
      println(s"set data:$data")
      null
    }
    
    def transfer(ctx: ContractContext, data:Transfer):Object={
      println(s"from:$data.from  to:$data.to amount:$data.amount")
      null
    }
    /**
     * 根据action,找到对应的method，并将传入的json字符串parse为method需要的传入参数
     */
    def onAction(ctx: ContractContext,action:String, sdata:String ):Object={
      println(s"apply---")
      val json = parse(sdata)
      
      action match {
        case "transfer" => 
          println(s"transfer")
          transfer(ctx,json.extract[Transfer])
          println(s"transfer")
        case "set" => 
          println(s"set") 
          set(ctx, json.extract[Map[String,Int]])
      }
      null
    }
    
}
scala.reflect.classTag[ContractAssets2].runtimeClass
      """
       val data1 = Transfer("1MH9xedPTkWThJUgT8ZYehiGCM7bEZTVGN","12kAzqqhuq7xYgm9YTph1b9zmCpZPyUWxf",50)
       val jdata1 = Extraction.decompose(data1)
       val sdata1 = pretty(render(jdata1))
       val sc1  =new ContractAssets
       //sc1.apply(null,"transfer",sdata1)
       
       val data2 : Map[String,Int] = Map("1MH9xedPTkWThJUgT8ZYehiGCM7bEZTVGN" -> 1000000,
        "12kAzqqhuq7xYgm9YTph1b9zmCpZPyUWxf" -> 1000000,"1GvvHCFZPajq5yVY44n7bdmSfv2MJ5LyLs" -> 1000000,
        "1AqZs6vhcLiiTvFxqS5CEqMw6xWuX9xqyi" -> 1000000,"16SrzMbzdLyGEUKY5FsdE8SVt5tQV1qmBY" -> 1000000)
       
       val jdata2 = Extraction.decompose(data2)
       val sdata2 = pretty(render(jdata2))
       
    //val sc1 = Eval.load[Contract](s1)
    //val tb = universe.runtimeMirror(getClass.getClassLoader).mkToolBox()
    val tb = currentMirror.mkToolBox()
    val clazz = tb.compile(tb.parse(s1))().asInstanceOf[Class[_]]
    val ctor = clazz.getDeclaredConstructors()(0)
    val instance = ctor.newInstance().asInstanceOf[Contract]
    instance.onAction(null,"set",sdata2)
    instance.onAction(null,"transfer",sdata1)
    //println(s"ffff$instance")
  }
}