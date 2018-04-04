package rep.sc.contract


import scala.tools.nsc.{Global, Settings}
import scala.reflect.internal.util.BatchSourceFile
import tools.nsc.io.{VirtualDirectory, AbstractFile}
import scala.reflect.internal.util.AbstractFileClassLoader
import java.security.MessageDigest
import java.math.BigInteger
import collection.mutable
import java.io._
import org.json4s._
import org.json4s.jackson.JsonMethods._
import rep.crypto.Sha256

import scala.reflect.runtime.currentMirror
import scala.tools.reflect.ToolBox

object CompileTest {
  case class Transfer(from:String, to:String, amount:Int)
  //是否调试模式
  val b_debug = true
  implicit val formats = DefaultFormats
    
  //调试模式下将合约代码写入source路径以支持IDE下进行debug, 运行模式下只写入内存目录
  //val compiler = new Compiler(None,true)

  def main(args: Array[String]) {
    
    val s1 =
      """
import rep.sc.contract._
import rep.protos.peer.Transaction
import rep.sc.Sandbox.DoTransactionResult
import org.json4s._
import org.json4s.jackson.JsonMethods._

class ContractAssets2 extends IContract{
  implicit val formats = DefaultFormats
  
    def init(ctx: ContractContext){      
      println(s"tid: $ctx.t.txid")
    }
    
    def set(ctx: ContractContext, data:Map[String,Int]):DoTransactionResult={
      println(s"set data:$data")
      null
    }
    
    def transfer(ctx: ContractContext, data:Transfer):DoTransactionResult={
      println(s"from:$data.from  to:$data.to amount:$data.amount")
      null
    }
    /**
     * 根据action,找到对应的method，并将传入的json字符串parse为method需要的传入参数
     */
    def onAction(ctx: ContractContext,action:String, sdata:String ):DoTransactionResult={
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
      """
    //构造json数据
       val data1 = Transfer("Alice","Bob",50)
       val jdata1 = Extraction.decompose(data1)
       val sdata1 = pretty(render(jdata1))
    
    val clazz = Compiler.compilef(s1,null)
    val cl = clazz.getConstructor().newInstance().asInstanceOf[IContract]
    cl.onAction(null,"transfer",sdata1)
    
  }
}

object Compiler{
  val cp = new Compiler(None, true)
  def compilef(pcode: String, cid: String): Class[_]= {
    cp.compilef(pcode, cid)
  }
}

/**
 * targetDir: 动态编译并加载的class路径
 * bDebug: 是否调试模式
 */
class Compiler(targetDir: Option[File], bDebug:Boolean) {
  val PRE_CLS_NAME = "sha"
  val tb = currentMirror.mkToolBox()
  val path_source = if(bDebug) getSourcePath else null
  val target = targetDir match {
    case Some(dir) => AbstractFile.getDirectory(dir)
    case None => new VirtualDirectory("(memory)", None)
  }

  val classCache = mutable.Map[String, Class[_]]()

  private val settings = new Settings()
  settings.deprecation.value = true // enable detailed deprecation warnings
  settings.unchecked.value = true // enable detailed unchecked warnings
  settings.outputDirs.setSingleOutput(target)
  settings.usejavacp.value = true
  settings.classpath.append(getSourcePath())

  private val global = new Global(settings)
  private lazy val run = new global.Run

  val classLoader = new AbstractFileClassLoader(target, this.getClass.getClassLoader)

  /**
   * 根据运行路径获得源代码路径
   */
  def getSourcePath()={
    //工程根路径
    val path_source_root = "RepchainNew/repchain"       
    //获得class路径
    val rpath = getClass.getResource("").getPath
    //获得source路径
    val p0 = rpath.indexOf(path_source_root)
    val sr = Array(rpath.substring(0,p0+ path_source_root.length()),"src","main","scala")
    val spath = sr.mkString(File.separator)
    spath
  }
  /**
   * 保存代码文件
   */
  def saveCode(fn:String, code:String)={
    val fout = new FileWriter(path_source+File.separator+fn+".scala") 
    fout.write(code)
    fout.close()   
  }
  /**Compiles the code as a class into the class loader of this compiler.
   *
   * @param code
   * @return
   */
  def compile(code: String) = {
    //如果制定了类名,直接采用指定类名，否则运算生成类名
    val className = classNameForCode(code)
    findClass(className).getOrElse {
      val sourceFiles = List(new BatchSourceFile("(inline)", wrapCodeInClass(className, code)))
      run.compileSources(sourceFiles)
      findClass(className).get
    }
  }

  /**
   * 编译完整文件的代码，只替换className
   * 将code封装文件写入source路径,重新启动时无需再从区块中获得代码加载，为防止擅自修改代码，应进行代码的hash检查
   */
  def compilef(pcode: String, cid: String): Class[_]= {
    val p1 = pcode.indexOf("extends IContract{")
    val p2 = pcode.lastIndexOf("}")
    val p3 = pcode.lastIndexOf("class ",p1)
    
//    val code = pcode.substring(p1+17, p2-1)
    val className = if(cid!=null) PRE_CLS_NAME+cid else classNameForCode(pcode)
    try{
      val cl = Class.forName(className)
      cl      
    }catch {
      case e:Throwable =>     
        findClass(className).getOrElse {
        //替换类名为 hash256
        val ncode = pcode.substring(0,p3) + "class "+className+ " "+pcode.substring(p1,p2+1)
        //+"\nscala.reflect.classTag[ContractAssets2].runtimeClass"
        if(path_source!=null)
          saveCode(className,ncode)
      
        //val sourceFiles = List(new BatchSourceFile("(inline)", ncode))
        //un.compileSources(sourceFiles)
        //val cls = findClass(className).get
        val cls =   tb.compile(tb.parse(ncode +"\nscala.reflect.classTag["
          +className
          +"].runtimeClass"))().asInstanceOf[Class[_]]
        classCache(className) = cls
        cls
      }

    }
  }

  /** Compiles the source string into the class loader and
   * evaluates it.
   *
   * @param code
   * @tparam T
   * @return
   */
  def eval[T](code: String): T = {
    val cls = compile(code)
    val r = cls.getConstructor().newInstance().asInstanceOf[() => Any].apply().asInstanceOf[T]
    r
  }

  def findClass(className: String): Option[Class[_]] = {
    synchronized {
      classCache.get(className).orElse {
        try {
          val cls = classLoader.loadClass(className)
          classCache(className) = cls
          Some(cls)
        } catch {
          case e: ClassNotFoundException => None
        }
      }
    }
  }

  protected def classNameForCode(code: String): String = {
    PRE_CLS_NAME + Sha256.hashstr(code)
  }

  /*
  * Wrap source code in a new class with an apply method.
  */
  //TODO extends Contract
  private def wrapCodeInClass(className: String, code: String) = {
    "class " + className + " extends (() => Any) {\n" +
      "  def apply() = {\n" +
      code + "\n" +
      "  }\n" +
      "}\n"
  }
}