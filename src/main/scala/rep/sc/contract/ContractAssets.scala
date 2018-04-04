
package rep.sc.contract

import org.json4s._
import org.json4s.jackson.JsonMethods._
import rep.sc.contract._

/**
 * 资产管理合约
 */
case class Transfer(from:String, to:String, amount:Int)

class ContractAssets extends IContract{

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
      "transfer ok"
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
        case "set" => 
          println(s"set") 
          set(ctx, json.extract[Map[String,Int]])
      }
    }
    
}
