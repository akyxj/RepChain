package rep.sc.contract

import rep.protos.peer.Transaction
import rep.sc.Sandbox.DoTransactionResult
import rep.sc.Shim

class ContractContext(val api:Shim, val t:Transaction)

trait IContract {
  def init(ctx: ContractContext)
  def onAction(ctx: ContractContext,action:String, sdata:String ):Object
}

abstract class Contract {
  def init(ctx: ContractContext)
  def onAction(ctx: ContractContext ,action:String, sdata:String):Object
}