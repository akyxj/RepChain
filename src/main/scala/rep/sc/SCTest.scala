package rep.sc
import javax.script.ScriptEngineManager
import rep.protos.peer._
import delight.nashornsandbox._;


object SCTest {
  def main(args: Array[String]): Unit = {
val se = new ScriptEngineManager()
val engine = se.getEngineByName("scala")

//需要设计的属性
//val settings = engine.asInstanceOf[scala.tools.nsc.interpreter.IMain].settings
//settings.usejavacp.value = true  //使用程序的class path作为engine的class path

val c = new ChaincodeID("pathxxx","name")
val d=c.withName("name777")
c.toString()
//engine.put("c",d)
//engine.eval("val d=c.withName(\"name777\")\n  println(s\"val:$d\")")    

//engine.put("m", 10)
//engine.eval("1 to m.asInstanceOf[Int] foreach println")    

  val sandbox = NashornSandboxes.create()
  //sandbox.allow(ChaincodeID.getClass)
  sandbox.allowPrintFunctions(true)
  sandbox.inject("c", c);
  sandbox.eval("var d=c.withName(\"name999\");  print(d.toString());")    

  }
}