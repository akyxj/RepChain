
import rep.sc.contract._
import rep.protos.peer.Transaction
import rep.sc.Sandbox.DoTransactionResult
import org.json4s._
import org.json4s.jackson.JsonMethods._

class sha5aaa0bfec7fa62c7c58affc77442f7a7a60054cdcdcf7e664fb60fc5552c1c7f extends IContract{
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