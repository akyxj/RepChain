
import org.json4s._
import org.json4s.jackson.JsonMethods._
import rep.sc.contract._

/**
 * 资产管理合约
 */

class sha2d85b1d0509c3afe12b0edcd964f7f00a1319727989e81e7131298422f7416de extends IContract{
case class Transfer(from:String, to:String, amount:Int)

  implicit val formats = DefaultFormats
  
    def init(ctx: ContractContext){      
      println(s"tid: $ctx.t.txid")
    }
    
    def set(ctx: ContractContext, data:Map[String,Int]):Object={
      println(s"set data:$data")
      for((k,v)<-data){
        ctx.api.setVal(k, v)
      }
      null
    }
    
    def transfer(ctx: ContractContext, data:Transfer):Object={
      val sfrom =  ctx.api.getVal(data.from)
      var dfrom =sfrom.toString.toInt
      if(dfrom < data.amount)
        throw new Exception("余额不足")
      var dto = ctx.api.getVal(data.to).toString.toInt
      //if(dto==null) dto = 0;
      
      ctx.api.setVal(data.from,dfrom - data.amount)
      ctx.api.setVal(data.to,dto + data.amount)
      "transfer ok"
    }
    /**
     * 根据action,找到对应的method，并将传入的json字符串parse为method需要的传入参数
     */
    def onAction(ctx: ContractContext,action:String, sdata:String ):Object={
      //println(s"onAction---")
      //return "transfer ok"
      val json = parse(sdata)
      
      action match {
        case "transfer" => 
          println(s"transfer oook")
          transfer(ctx,json.extract[Transfer])
        case "set" => 
          println(s"set") 
          set(ctx, json.extract[Map[String,Int]])
      }
    }
    
}